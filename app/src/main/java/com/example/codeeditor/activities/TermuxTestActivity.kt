package com.example.codeeditor.activities

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.codeeditor.termux.ConnectionState
import com.example.codeeditor.termux.OutputStream
import com.example.codeeditor.termux.TermuxException
import com.example.codeeditor.termux.ssh.HostKeyStore
import com.example.codeeditor.termux.ssh.SshSettings
import com.example.codeeditor.termux.ssh.SshSetupHelp
import com.example.codeeditor.ui.theme.CodeEditorTheme
import com.example.codeeditor.ui.theme.ThemeManager
import com.example.codeeditor.viewmodels.OutputChunk
import com.example.codeeditor.viewmodels.RunOutcome
import com.example.codeeditor.viewmodels.TermuxTestVM

/**
    Lab screen for Termux communication: connect, run a command, watch its output.
 */
class TermuxTestActivity : AppCompatActivity() {

    private val vm: TermuxTestVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.initTheme(this)
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val hostKeys = HostKeyStore(this)
        setContent {
            CodeEditorTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TermuxTestScreen(vm) { state ->
                        // The only backend-specific UI on this screen.
                        SshSettings(state, hostKeys, onConnect = vm::connect, onDisconnect = vm::disconnect)
                    }
                }
            }
        }
    }
}

@Composable
private fun TermuxTestScreen(vm: TermuxTestVM, settings: @Composable (ConnectionState) -> Unit) {
    val state by vm.connectionState.collectAsState()
    val running by vm.running.collectAsState()
    val output by vm.output.collectAsState()
    val truncated by vm.outputTruncated.collectAsState()
    val lastRun by vm.lastRun.collectAsState()

    var cwd by rememberSaveable { mutableStateOf("") }
    var command by rememberSaveable { mutableStateOf("echo hello") }
    var timeout by rememberSaveable { mutableStateOf("60") }

    val lastError: TermuxException? = (state as? ConnectionState.Failed)?.error
        ?: (lastRun?.outcome as? RunOutcome.Error)?.error
    val needsSetup = lastError is TermuxException.NotReachable || lastError is TermuxException.AuthFailed

    Row(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            modifier = Modifier.weight(0.42f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Termux connection test", style = MaterialTheme.typography.titleLarge)
            settings(state)
            Divider()
            OutlinedTextField(
                value = cwd, onValueChange = { cwd = it },
                label = { Text("Working directory (empty = \$HOME)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = command, onValueChange = { command = it },
                label = { Text("Command") },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = timeout, onValueChange = { timeout = it.filter(Char::isDigit) },
                    label = { Text("Timeout, s") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(120.dp)
                )
                Button(
                    enabled = state is ConnectionState.Connected && !running && command.isNotBlank(),
                    onClick = { vm.run(command, cwd, (timeout.toLongOrNull() ?: 60L).coerceAtLeast(1L)) }
                ) { Text("Run") }
                OutlinedButton(enabled = running, onClick = vm::cancel) { Text("Cancel") }
            }
            SshSetupHelp(autoExpand = needsSetup)
        }
        Column(modifier = Modifier.weight(0.58f).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Card(modifier = Modifier.weight(1f).fillMaxWidth()) {
                OutputArea(output, truncated)
            }
            Text(
                when {
                    running -> "running…"
                    else -> lastRun?.describe() ?: ""
                },
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.heightIn(min = 20.dp)
            )
        }
    }
}

@Composable
private fun OutputArea(output: List<OutputChunk>, truncated: Boolean) {
    val scroll = rememberScrollState()
    // Follow the end of the output as it grows.
    LaunchedEffect(scroll) {
        snapshotFlow { scroll.maxValue }.collect { scroll.scrollTo(it) }
    }
    val errorColor = MaterialTheme.colorScheme.error
    val text = remember(output, truncated, errorColor) {
        buildAnnotatedString {
            if (truncated) {
                withStyle(SpanStyle(color = errorColor)) { append("[earlier output dropped]\n") }
            }
            output.forEach { chunk ->
                if (chunk.stream == OutputStream.STDERR) {
                    withStyle(SpanStyle(color = errorColor)) { append(chunk.text) }
                } else {
                    append(chunk.text)
                }
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize().verticalScroll(scroll).padding(8.dp)) {
        SelectionContainer {
            Text(text, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        }
    }
}
