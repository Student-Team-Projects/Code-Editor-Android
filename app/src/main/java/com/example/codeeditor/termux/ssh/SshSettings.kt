package com.example.codeeditor.termux.ssh

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.codeeditor.termux.ConnectionState
import com.example.codeeditor.termux.ExecRequest
import com.example.codeeditor.termux.OutputStream
import com.example.codeeditor.termux.TermuxException
import com.example.codeeditor.termux.TermuxSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.KeyPair

/**
    Connection screen content for the SSH backend: settings and status on the left,
    the login key and setup help on the right.
 */
@Composable
fun SshConnectionScreen(session: TermuxSession) {
    val context = LocalContext.current
    val hostKeys = remember { HostKeyStore(context) }
    val keys = remember { SshKeyStore(context) }
    val profile = remember { SshProfile(context) }
    val state by session.state.collectAsState()
    var key by remember { mutableStateOf<KeyPair?>(null) }
    LaunchedEffect(Unit) { key = withContext(Dispatchers.IO) { keys.load() } }

    val error = (state as? ConnectionState.Failed)?.error
    val needsSetup = error is TermuxException.NotReachable || error is TermuxException.AuthFailed

    Row(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            modifier = Modifier.weight(0.45f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Termux connection", style = MaterialTheme.typography.titleLarge)
            SshSettings(
                state, hostKeys, profile, key,
                onConnect = { config -> session.connect(SshTermuxConnection(config, hostKeys)) },
                onDisconnect = { session.disconnect() },
            )
        }
        Column(
            modifier = Modifier.weight(0.55f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SshKeySettings(session, keys, key, onKeyChanged = { key = it })
            SshSetupHelp(autoExpand = needsSetup)
        }
    }
}

@Composable
private fun SshSettings(
    state: ConnectionState,
    hostKeys: HostKeyStore,
    profile: SshProfile,
    key: KeyPair?,
    onConnect: (SshConfig) -> Unit,
    onDisconnect: () -> Unit,
) {
    var host by rememberSaveable { mutableStateOf(profile.host) }
    var port by rememberSaveable { mutableStateOf(profile.port.toString()) }
    var username by rememberSaveable { mutableStateOf(profile.username) }
    // Deliberately not saveable: the password is never written anywhere.
    var password by remember { mutableStateOf("") }
    var resetNote by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = host, onValueChange = { host = it.trim() },
                label = { Text("Host") }, singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = port, onValueChange = { port = it.filter(Char::isDigit) },
                label = { Text("Port") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(100.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = username, onValueChange = { username = it.trim() },
                label = { Text("User") }, singleLine = true,
                modifier = Modifier.weight(0.4f)
            )
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text(if (key != null) "Password (optional)" else "Password") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.weight(0.6f)
            )
        }
        Text(
            if (key != null) "Logs in with the app's key, then the password if one is given."
            else "No key yet: logs in with the password.",
            style = MaterialTheme.typography.bodySmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val busy = state is ConnectionState.Connecting
            val portNumber = port.toIntOrNull()
            Button(
                enabled = !busy && host.isNotEmpty() && username.isNotEmpty() && portNumber != null &&
                    (key != null || password.isNotEmpty()),
                onClick = {
                    resetNote = null
                    profile.host = host
                    profile.port = portNumber!!
                    profile.username = username
                    onConnect(SshConfig(host, portNumber, username, password, key))
                }
            ) { Text(if (state is ConnectionState.Connected) "Reconnect" else "Connect") }
            OutlinedButton(
                enabled = state is ConnectionState.Connected || busy,
                onClick = onDisconnect
            ) { Text("Disconnect") }
        }
        TextButton(onClick = {
            val p = port.toIntOrNull() ?: return@TextButton
            hostKeys.reset(host, p)
            resetNote = "Stored host key for $host:$p removed; the next connection trusts the new one."
        }) { Text("Reset stored host key") }
        resetNote?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        ConnectionStatus(state)
    }
}

@Composable
private fun SshKeySettings(session: TermuxSession, keys: SshKeyStore, key: KeyPair?, onKeyChanged: (KeyPair?) -> Unit) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val state by session.state.collectAsState()
    var note by remember { mutableStateOf<String?>(null) }
    val line = remember(key) { key?.let { SshKeyStore.publicKeyLine(it.public) } }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Login key", style = MaterialTheme.typography.titleSmall)
            if (key == null || line == null) {
                Text("No key. A key lets the app log in without a password, and connect by itself.")
            } else {
                Text(
                    "Fingerprint: ${SshKeyStore.fingerprint(key.public)}",
                    style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace
                )
                Text("To allow it, paste the setup command into Termux once, or install it over a password connection.")
                SelectionContainer {
                    Text(SshKeyStore.authorizeCommand(line), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (key == null || line == null) {
                    Button(onClick = {
                        scope.launch {
                            note = null
                            onKeyChanged(withContext(Dispatchers.IO) { keys.generate() })
                        }
                    }) { Text("Generate key") }
                } else {
                    Button(onClick = {
                        clipboard.setText(AnnotatedString(SshKeyStore.authorizeCommand(line)))
                        note = "Copied. Paste it into Termux and press Enter."
                    }) { Text("Copy setup command") }
                    OutlinedButton(
                        enabled = state is ConnectionState.Connected,
                        onClick = { scope.launch { note = installKey(session, line) } }
                    ) { Text("Install over this connection") }
                    TextButton(onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) { keys.delete() }
                            onKeyChanged(null)
                            note = "Key deleted. Also remove its line (ending in \"${SshKeyStore.KEY_COMMENT}\") " +
                                "from ~/.ssh/authorized_keys in Termux."
                        }
                    }) { Text("Delete key") }
                }
            }
            note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

private suspend fun installKey(session: TermuxSession, line: String): String {
    val connection = session.connection.value ?: return "Not connected."
    val errors = StringBuilder()
    return try {
        val result = connection.exec(ExecRequest(SshKeyStore.authorizeCommand(line), timeoutSec = 15)) { stream, text ->
            if (stream == OutputStream.STDERR) synchronized(errors) { errors.append(text) }
        }
        if (result.exitCode == 0) "Key installed. Later connections can log in without the password."
        else "Installing failed (exit ${result.exitCode ?: "unknown"}): ${errors.toString().trim()}"
    } catch (e: TermuxException) {
        "Installing failed: ${e.message}"
    }
}

@Composable
private fun ConnectionStatus(state: ConnectionState) {
    when (state) {
        ConnectionState.Disconnected -> Text("Disconnected")
        ConnectionState.Connecting -> Text("Connecting…")
        is ConnectionState.Connected -> Column {
            Text("Connected", color = MaterialTheme.colorScheme.primary)
            state.info.forEach { (key, value) ->
                Text("$key: $value", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
        }
        is ConnectionState.Failed -> Column {
            Text(state.error.message ?: "Failed", color = MaterialTheme.colorScheme.error)
            Text(sshAdvice(state.error), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Concrete advice for an error raised by [SshTermuxConnection]. */
fun sshAdvice(error: TermuxException): String = when (error) {
    is TermuxException.NotReachable -> "sshd isn't running: start it in Termux with `sshd`."
    is TermuxException.AuthFailed ->
        "Login refused: check the user with `whoami`, then either install the app's key or set a password with `passwd`."
    is TermuxException.Untrusted ->
        "The stored host key doesn't match (${error.detail}). If Termux was reinstalled, reset the stored host key."
    is TermuxException.Failed -> "Unexpected error" + (error.cause?.let { " (${it::class.java.simpleName})" } ?: "") + "."
}

private val setupSteps = listOf(
    "Install Termux from F-Droid." to null,
    "Install OpenSSH:" to "pkg install openssh",
    "Restrict sshd to the phone itself (otherwise it accepts logins from the Wi-Fi network):" to
        "echo \"ListenAddress 127.0.0.1\" >> \$PREFIX/etc/ssh/sshd_config",
    "Start it (listens on port 8022):" to "sshd",
    "Print the user name to log in with:" to "whoami",
    "Allow a login: generate a key here and paste its setup command, or set a password:" to "passwd",
    "Optional, so the terminal can open projects under /storage/emulated/0:" to "termux-setup-storage",
)

/** Collapsible phone setup steps. Opens by itself whenever [autoExpand] turns true. */
@Composable
fun SshSetupHelp(autoExpand: Boolean) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(autoExpand) { if (autoExpand) expanded = true }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                (if (expanded) "▼ " else "▶ ") + "Termux setup",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
            )
            if (expanded) {
                setupSteps.forEachIndexed { i, (text, command) ->
                    Text("${i + 1}. $text", modifier = Modifier.padding(top = 6.dp))
                    command?.let {
                        Text(it, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        }
    }
}
