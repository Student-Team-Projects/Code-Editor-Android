package com.example.codeeditor.composables

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.codeeditor.terminal.ChunkStyle
import com.example.codeeditor.terminal.OutputChunk
import com.example.codeeditor.terminal.filesystemPathOf
import com.example.codeeditor.terminal.promptFor
import com.example.codeeditor.termux.ConnectionState
import com.example.codeeditor.ui.theme.CodeEditorTheme
import com.example.codeeditor.viewmodels.TerminalVM

/** Lets the View-based main screen embed the terminal. */
object TerminalPanelHost {
    @JvmStatic
    fun create(activity: AppCompatActivity, onOpenConnection: Runnable): ComposeView {
        val vm = viewModel(activity)
        return ComposeView(activity).apply {
            setContent {
                CodeEditorTheme(styleWindow = false) {
                    TerminalPanel(vm, onOpenConnection = { onOpenConnection.run() })
                }
            }
        }
    }

    /** [treeUri] is the project folder from the system picker, or null when no such project is open. */
    @JvmStatic
    fun setProjectDirectory(activity: AppCompatActivity, treeUri: Uri?) {
        viewModel(activity).setProjectDirectory(filesystemPathOf(treeUri))
    }

    private fun viewModel(activity: AppCompatActivity) = ViewModelProvider(activity)[TerminalVM::class.java]
}

@Composable
fun TerminalPanel(vm: TerminalVM, onOpenConnection: () -> Unit) {
    val state by vm.connectionState.collectAsState()
    val output by vm.output.collectAsState()
    val truncated by vm.outputTruncated.collectAsState()
    val running by vm.running.collectAsState()
    val cwd by vm.cwd.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Terminal · " + statusText(state),
                style = MaterialTheme.typography.labelLarge,
                color = if (state is ConnectionState.Failed) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = vm::clear) { Text("Clear") }
            TextButton(onClick = onOpenConnection) { Text("Connection") }
        }
        Divider()
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Transcript(output, truncated)
        }
        Divider()
        InputRow(vm, prompt = promptFor(cwd), running = running)
    }
}

private fun statusText(state: ConnectionState): String = when (state) {
    ConnectionState.Disconnected -> "not connected"
    ConnectionState.Connecting -> "connecting…"
    is ConnectionState.Connected -> "connected"
    is ConnectionState.Failed -> state.error.message ?: "failed"
}

@Composable
private fun InputRow(vm: TerminalVM, prompt: String, running: Boolean) {
    val input by vm.input.collectAsState()
    var field by remember { mutableStateOf(TextFieldValue(input)) }
    // History and submit replace the text from the VM; put the cursor at its end.
    // Compare with the VM's current text, not `input`: by the time this runs the user may have typed more.
    LaunchedEffect(input) {
        val current = vm.input.value
        if (field.text != current) field = TextFieldValue(current, TextRange(current.length))
    }
    val mono = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(prompt, style = mono, color = MaterialTheme.colorScheme.primary)
        OutlinedTextField(
            value = field,
            onValueChange = {
                field = it
                vm.setInput(it.text)
            },
            singleLine = true,
            textStyle = mono,
            placeholder = { Text(if (running) "running…" else "command", style = mono) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrect = false,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Send,
            ),
            keyboardActions = KeyboardActions(onSend = { vm.submit() }),
            modifier = Modifier
                .weight(1f)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionUp -> { vm.historyPrevious(); true }
                        Key.DirectionDown -> { vm.historyNext(); true }
                        else -> false
                    }
                }
        )
        IconButton(onClick = vm::historyPrevious) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Previous command")
        }
        IconButton(onClick = vm::historyNext) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Next command")
        }
        if (running) {
            FilledIconButton(
                onClick = vm::stop,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Icon(Icons.Filled.Close, contentDescription = "Stop") }
        } else {
            FilledIconButton(onClick = vm::submit) { Icon(Icons.Filled.Send, contentDescription = "Run") }
        }
    }
}

@Composable
private fun Transcript(output: List<OutputChunk>, truncated: Boolean) {
    val scroll = rememberScrollState()
    // Follow the end while the user is at the bottom; leave them alone when they scrolled up.
    // Also keeps the end in view when the keyboard shrinks the panel.
    LaunchedEffect(scroll) {
        var lastMax = 0
        snapshotFlow { scroll.maxValue }.collect { max ->
            if (scroll.value >= lastMax - FOLLOW_SLACK_PX) scroll.scrollTo(max)
            lastMax = max
        }
    }
    val colors = MaterialTheme.colorScheme
    val text = remember(output, truncated, colors) {
        buildAnnotatedString {
            if (truncated) {
                withStyle(SpanStyle(color = colors.error)) { append("[earlier output dropped]\n") }
            }
            output.forEach { chunk ->
                val style = when (chunk.style) {
                    ChunkStyle.OUTPUT -> null
                    ChunkStyle.ERROR -> SpanStyle(color = colors.error)
                    ChunkStyle.COMMAND -> SpanStyle(color = colors.primary, fontWeight = FontWeight.Bold)
                    ChunkStyle.INFO -> SpanStyle(color = colors.secondary, fontStyle = FontStyle.Italic)
                }
                if (style == null) append(chunk.text) else withStyle(style) { append(chunk.text) }
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize().verticalScroll(scroll).padding(8.dp)) {
        SelectionContainer {
            Text(text, fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = colors.onBackground)
        }
    }
}

private const val FOLLOW_SLACK_PX = 48
