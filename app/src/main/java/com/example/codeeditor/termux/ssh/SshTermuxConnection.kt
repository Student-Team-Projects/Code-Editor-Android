package com.example.codeeditor.termux.ssh

import com.example.codeeditor.termux.ConnectionState
import com.example.codeeditor.termux.ExecRequest
import com.example.codeeditor.termux.ExecResult
import com.example.codeeditor.termux.OutputSink
import com.example.codeeditor.termux.OutputStream
import com.example.codeeditor.termux.TermuxConnection
import com.example.codeeditor.termux.TermuxException
import com.example.codeeditor.termux.withCwd
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.connection.channel.direct.Signal
import net.schmizz.sshj.transport.TransportException
import net.schmizz.sshj.common.DisconnectReason
import net.schmizz.sshj.userauth.UserAuthException
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.InputStream
import java.io.InputStreamReader
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.security.Security

/**
    Termux over OpenSSH (`sshd` on port 8022). The only class that uses sshj.
 */
class SshTermuxConnection(
    private val config: SshConfig,
    private val hostKeys: HostKeyStore,
) : TermuxConnection {

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    @Volatile
    private var client: SSHClient? = null

    override suspend fun connect() = withContext(Dispatchers.IO) {
        BouncyCastle.ensureRegistered()
        closeClient()
        _state.value = ConnectionState.Connecting
        val verifier = hostKeys.verifier()
        // DefaultConfig, not AndroidConfig: the latter registers the obsolete SpongyCastle.
        val ssh = SSHClient(DefaultConfig())
        try {
            ssh.connectTimeout = CONNECT_TIMEOUT_MS
            ssh.addHostKeyVerifier(verifier)
            ssh.connect(config.host, config.port)
            ssh.authPassword(config.username, config.password)
        } catch (e: Exception) {
            runCatching { ssh.disconnect() }
            val error = toTermuxException(e, verifier.mismatch)
            _state.value = ConnectionState.Failed(error)
            throw error
        }
        client = ssh
        _state.value = ConnectionState.Connected(
            mapOf(
                "Server" to "${config.username}@${config.host}:${config.port}",
                "Host key" to (verifier.acceptedFingerprint ?: "?"),
            )
        )
    }

    override suspend fun exec(request: ExecRequest, output: OutputSink): ExecResult =
        withContext(Dispatchers.IO) {
            val ssh = client?.takeIf { it.isConnected }
                ?: throw notConnected()
            // sshj sessions can't be reused after exec, so each command gets its own.
            val session = try {
                ssh.startSession()
            } catch (e: Exception) {
                throw failure(e)
            }
            var command: Session.Command? = null
            try {
                val cmd = session.exec(withCwd(request.command, request.cwd))
                command = cmd
                val finished = withTimeoutOrNull(request.timeoutSec * 1000) {
                    coroutineScope {
                        launch { pump(cmd.inputStream, OutputStream.STDOUT, output) }
                        launch { pump(cmd.errorStream, OutputStream.STDERR, output) }
                    }
                    runInterruptible { cmd.join() }
                    true
                } ?: false
                if (finished) {
                    ExecResult(cmd.exitStatus, cmd.exitSignal?.name, timedOut = false)
                } else {
                    kill(cmd)
                    ExecResult(exitCode = null, exitSignal = null, timedOut = true)
                }
            } catch (e: CancellationException) {
                command?.let { kill(it) }
                throw e
            } catch (e: Exception) {
                throw failure(e)
            } finally {
                runCatching { session.close() }
            }
        }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        closeClient()
        _state.value = ConnectionState.Disconnected
    }

    private suspend fun pump(stream: InputStream, kind: OutputStream, output: OutputSink) {
        val reader = InputStreamReader(stream, Charsets.UTF_8)
        val buf = CharArray(8192)
        while (true) {
            val n = runInterruptible { reader.read(buf) }
            if (n < 0) return
            if (n > 0) output.onOutput(kind, String(buf, 0, n))
        }
    }

    // Without a PTY, closing the channel alone doesn't stop the remote process.
    private fun kill(cmd: Session.Command) {
        runCatching { cmd.signal(Signal.KILL) }
    }

    private fun closeClient() {
        client?.let { runCatching { it.disconnect() } }
        client = null
    }

    private fun notConnected(): TermuxException {
        val error = TermuxException.NotReachable(IllegalStateException("Not connected"))
        if (client != null) {
            closeClient()
            _state.value = ConnectionState.Failed(error)
        }
        return error
    }

    private fun failure(e: Exception): TermuxException {
        val error = toTermuxException(e, hostKeyMismatch = null)
        if (client?.isConnected == false) {
            closeClient()
            _state.value = ConnectionState.Failed(TermuxException.NotReachable(e))
        }
        return error
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 10_000
    }
}

// Android registers a cut-down provider as "BC"; sshj would find it and stop there.
private object BouncyCastle {
    init {
        Security.removeProvider("BC")
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }

    fun ensureRegistered() = Unit
}

/** Maps sshj and socket errors to [TermuxException]. [hostKeyMismatch] comes from [HostKeyStore.Verifier]. */
internal fun toTermuxException(e: Throwable, hostKeyMismatch: String?): TermuxException {
    if (e is TermuxException) return e
    if (hostKeyMismatch != null) return TermuxException.Untrusted(hostKeyMismatch)
    val chain = generateSequence(e) { it.cause }.take(10).toList()
    return when {
        chain.any { it is TransportException && it.disconnectReason == DisconnectReason.HOST_KEY_NOT_VERIFIABLE } ->
            TermuxException.Untrusted(e.message ?: "host key rejected")
        chain.any { it is UserAuthException } -> TermuxException.AuthFailed(e)
        chain.any { it is ConnectException || it is NoRouteToHostException } -> TermuxException.NotReachable(e)
        else -> TermuxException.Failed(e)
    }
}
