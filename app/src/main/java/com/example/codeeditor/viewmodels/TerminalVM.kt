package com.example.codeeditor.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.codeeditor.terminal.ChunkStyle
import com.example.codeeditor.terminal.CommandHistory
import com.example.codeeditor.terminal.OutputBuffer
import com.example.codeeditor.terminal.OutputChunk
import com.example.codeeditor.terminal.TerminalLine
import com.example.codeeditor.terminal.cdCommand
import com.example.codeeditor.terminal.promptFor
import com.example.codeeditor.terminal.stripAnsi
import com.example.codeeditor.terminal.terminalCommand
import com.example.codeeditor.termux.ConnectionState
import com.example.codeeditor.termux.ExecRequest
import com.example.codeeditor.termux.ExecResult
import com.example.codeeditor.termux.OutputStream
import com.example.codeeditor.termux.TermuxConnection
import com.example.codeeditor.termux.TermuxException
import com.example.codeeditor.termux.TermuxSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
    State of the integrated terminal. Each command is a separate exec on [TermuxSession];
    the working directory is tracked here, so only a bare `cd` changes it.
 */
class TerminalVM(private val session: TermuxSession = TermuxSession.shared) : ViewModel() {
    private val buffer = OutputBuffer(MAX_OUTPUT_CHARS)
    private val history = CommandHistory()
    private var runJob: Job? = null
    private var publishedVersion = -1L
    private var projectDirectory: String? = null
    private var cwdNeedsCheck = false

    val connectionState: StateFlow<ConnectionState> = session.state

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    /** `null` means `$HOME`. */
    private val _cwd = MutableStateFlow<String?>(null)
    val cwd: StateFlow<String?> = _cwd.asStateFlow()

    private val _output = MutableStateFlow<List<OutputChunk>>(emptyList())
    val output: StateFlow<List<OutputChunk>> = _output.asStateFlow()

    private val _outputTruncated = MutableStateFlow(false)
    val outputTruncated: StateFlow<Boolean> = _outputTruncated.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    fun setInput(text: String) {
        _input.value = text
    }

    fun historyPrevious() {
        history.previous(_input.value)?.let { _input.value = it }
    }

    fun historyNext() {
        history.next()?.let { _input.value = it }
    }

    /**
        Moves the terminal into a newly opened project. [path] is a filesystem path, or null if the project
        has none Termux could reach; then the terminal stays where it is.
     */
    fun setProjectDirectory(path: String?) {
        if (path == projectDirectory) return
        projectDirectory = path
        if (path == null) return
        _cwd.value = path
        cwdNeedsCheck = true
    }

    fun clear() {
        buffer.clear()
        publishOutput()
    }

    fun stop() {
        runJob?.cancel()
    }

    fun submit() {
        if (runJob?.isActive == true) return
        val line = _input.value
        _input.value = ""
        history.add(line)
        val parsed = TerminalLine.parse(line)
        if (parsed == TerminalLine.Clear) return clear()
        append(ChunkStyle.COMMAND, "${promptFor(_cwd.value)} $line\n")
        if (line.isBlank()) return publishOutput()

        runJob = viewModelScope.launch {
            _running.value = true
            // Output can arrive faster than the UI can draw it, so publish at a fixed rate.
            val publisher = launch {
                while (true) {
                    delay(PUBLISH_INTERVAL_MS)
                    publishOutput()
                }
            }
            try {
                val connection = session.ensureConnected()
                if (connection == null) {
                    append(ChunkStyle.ERROR, notConnectedMessage())
                    return@launch
                }
                if (cwdNeedsCheck) {
                    cwdNeedsCheck = false
                    checkCwd(connection)
                }
                when (parsed) {
                    is TerminalLine.Cd -> runCd(connection, parsed.args)
                    is TerminalLine.Command -> runCommand(connection, parsed.text)
                    TerminalLine.Clear -> Unit
                }
            } catch (e: CancellationException) {
                endLine()
                append(ChunkStyle.INFO, "stopped\n")
                throw e
            } catch (e: TermuxException) {
                endLine()
                append(ChunkStyle.ERROR, "error: ${e.message}\n")
            } finally {
                publisher.cancel()
                publishOutput()
                _running.value = false
            }
        }
    }

    private suspend fun runCommand(connection: TermuxConnection, command: String) {
        val request = ExecRequest(terminalCommand(command), _cwd.value, timeoutSec = null)
        val result = connection.exec(request) { stream, text ->
            buffer.append(if (stream == OutputStream.STDERR) ChunkStyle.ERROR else ChunkStyle.OUTPUT, stripAnsi(text))
        }
        endLine()
        describe(result)?.let { append(ChunkStyle.INFO, "$it\n") }
    }

    private suspend fun runCd(connection: TermuxConnection, args: String) {
        val stdout = StringBuilder()
        val result = connection.exec(ExecRequest(cdCommand(args), _cwd.value, SHORT_TIMEOUT_SEC)) { stream, text ->
            if (stream == OutputStream.STDOUT) synchronized(stdout) { stdout.append(text) }
            else buffer.append(ChunkStyle.ERROR, stripAnsi(text))
        }
        endLine()
        val newCwd = synchronized(stdout) { stdout.lines().lastOrNull { it.isNotBlank() } }
        if (result.exitCode == 0 && newCwd != null) {
            _cwd.value = newCwd
        } else {
            describe(result)?.let { append(ChunkStyle.INFO, "$it\n") }
        }
    }

    /** A project folder may be unreachable from Termux (no storage permission); fall back to `$HOME`. */
    private suspend fun checkCwd(connection: TermuxConnection) {
        val dir = _cwd.value ?: return
        val errors = StringBuilder()
        val result = connection.exec(ExecRequest("pwd", dir, SHORT_TIMEOUT_SEC)) { stream, text ->
            if (stream == OutputStream.STDERR) synchronized(errors) { errors.append(text) }
        }
        if (result.exitCode == 0) return
        _cwd.value = null
        val reason = synchronized(errors) { errors.toString().trim() }.ifEmpty { "exit ${result.exitCode}" }
        append(
            ChunkStyle.INFO,
            "Can't open the project in Termux ($reason).\n" +
                "Run termux-setup-storage in Termux to allow it. Using ~ for now.\n"
        )
    }

    private fun notConnectedMessage(): String {
        // The connection's own state: session.state may not have caught up yet.
        val error = (session.connection.value?.state?.value as? ConnectionState.Failed)?.error
        return if (error != null) "Not connected: ${error.message}. Open Connection to fix it.\n"
        else "Not connected. Open Connection to set up Termux.\n"
    }

    private fun describe(result: ExecResult): String? = with(result) {
        when {
            timedOut -> "timed out"
            exitCode == 0 -> null
            exitCode != null -> "exit $exitCode"
            exitSignal != null -> "killed by $exitSignal"
            else -> "exit unknown"
        }
    }

    private fun endLine() {
        if (!buffer.endsWithNewline()) buffer.append(ChunkStyle.OUTPUT, "\n")
    }

    private fun append(style: ChunkStyle, text: String) {
        buffer.append(style, text)
    }

    private fun publishOutput() {
        val version = buffer.version
        if (version == publishedVersion) return
        publishedVersion = version
        _output.value = buffer.snapshot()
        _outputTruncated.value = buffer.truncated
    }

    companion object {
        const val MAX_OUTPUT_CHARS = 256 * 1024
        private const val PUBLISH_INTERVAL_MS = 100L
        private const val SHORT_TIMEOUT_SEC = 15L
    }
}
