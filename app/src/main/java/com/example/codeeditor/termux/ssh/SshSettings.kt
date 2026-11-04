package com.example.codeeditor.termux.ssh

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.codeeditor.termux.ConnectionState
import com.example.codeeditor.termux.TermuxConnection
import com.example.codeeditor.termux.TermuxException

/**
    Connection section for the SSH backend: builds an [SshTermuxConnection] from its fields.
 */
@Composable
fun SshSettings(
    state: ConnectionState,
    hostKeys: HostKeyStore,
    onConnect: (TermuxConnection) -> Unit,
    onDisconnect: () -> Unit,
) {
    var host by rememberSaveable { mutableStateOf(SshConfig.DEFAULT_HOST) }
    var port by rememberSaveable { mutableStateOf(SshConfig.DEFAULT_PORT.toString()) }
    var username by rememberSaveable { mutableStateOf(SshConfig.DEFAULT_USERNAME) }
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
                label = { Text("Password") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.weight(0.6f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val busy = state is ConnectionState.Connecting
            Button(
                enabled = !busy && host.isNotEmpty() && username.isNotEmpty() && port.toIntOrNull() != null,
                onClick = {
                    resetNote = null
                    onConnect(SshTermuxConnection(SshConfig(host, port.toInt(), username, password), hostKeys))
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
    is TermuxException.AuthFailed -> "Wrong user or password, or no password set yet: check `whoami` and run `passwd` in Termux."
    is TermuxException.Untrusted ->
        "The stored host key doesn't match (${error.detail}). If Termux was reinstalled, reset the stored host key."
    is TermuxException.Failed -> "Unexpected error" + (error.cause?.let { " (${it::class.java.simpleName})" } ?: "") + "."
}

private val setupSteps = listOf(
    "Install Termux from F-Droid." to null,
    "Install OpenSSH:" to "pkg install openssh",
    "Set a password (without one, password login is refused):" to "passwd",
    "Restrict sshd to the phone itself (otherwise it accepts logins from the Wi-Fi network):" to
        "echo \"ListenAddress 127.0.0.1\" >> \$PREFIX/etc/ssh/sshd_config",
    "Start it (listens on port 8022):" to "sshd",
    "Print the user name to log in with:" to "whoami",
    "Optional, to use a working directory under /storage/emulated/0:" to "termux-setup-storage",
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
