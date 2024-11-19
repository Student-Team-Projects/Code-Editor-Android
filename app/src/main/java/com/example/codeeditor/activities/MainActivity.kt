package com.example.codeeditor.activities

import DirectoryTreeVMFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.example.codeeditor.ui.theme.CodeEditorTheme
import com.example.codeeditor.viewmodels.CodeVM
import com.example.codeeditor.viewmodels.DirectoryEntry
import com.example.codeeditor.viewmodels.DirectoryTreeVM
import com.example.codeeditor.viewmodels.FileVM
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.Optional

private val BackgroundColor = Color.White
private val CodeAreaBackgroundColor = Color(0xFFFFF9C4)
private val BorderColor = Color.Gray
private val ButtonRowPadding = 8.dp
private val ButtonTextFieldSpacing = 0.dp
private val BorderThickness = 1.dp
private val CornerRadius = 8.dp
private const val DirectoryTreePaddingIncrement = 8
private val searchingExtensions = arrayOf("text/plain", "text/x-c", "text/x-java-source", "text/x-python")
private const val saveAsDefaultExtension = "text/plain"

class MainActivity : ComponentActivity() {
    private val codeVM: CodeVM by viewModels()
    private val fileVM: FileVM by viewModels()
    private val directoryVM: DirectoryTreeVM by viewModels{DirectoryTreeVMFactory{
        uri -> DocumentFile.fromTreeUri(this, uri) ?: run {
            Toast.makeText(this, "Failed to open directory: $uri", Toast.LENGTH_LONG).show()
            null
        }
    }}

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            updateCurrentFile(it)
            readTextFromUri(it)
        }
    }

    private val openDirectoryLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            updateCurrentDirectory(it)
        }
    }

    private val saveAs = registerForActivityResult(
        ActivityResultContracts.CreateDocument(saveAsDefaultExtension)
    ) { uri: Uri? ->
        uri?.let {
            updateCurrentFile(it)
            saveCodeToFile()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CodeEditorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(codeVM=codeVM, openFile={openDocumentPicker()},
                        saveAs = {createFilePicker()}, save={saveCodeToFile()},
                        directoryVM=directoryVM, openDirectory = {openDirectoryPicker()}
                    )
                }
            }
        }
    }

    private fun openDocumentPicker() {
        openDocumentLauncher.launch(searchingExtensions)
    }
    private fun openDirectoryPicker() {
        openDirectoryLauncher.launch(Uri.EMPTY)
    }

    private fun createFilePicker() {
        saveAs.launch("newFile.txt")
    }

    private fun updateCurrentFile(uri: Uri) {
        fileVM.fileUri.value = uri
    }
    private fun updateCurrentDirectory(uri: Uri) {
        directoryVM.currentEntry.value = directoryVM.directoryEntry(uri).getOrNull()
    }

    private fun readTextFromUri(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val text = BufferedReader(InputStreamReader(inputStream)).readText()
                codeVM.text.value = text
            } ?: run {
                Toast.makeText(this, "Failed to open file", Toast.LENGTH_SHORT).show()
            }
        } catch (e: FileNotFoundException) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun saveCodeToFile() {
        try {
            contentResolver.openOutputStream(fileVM.fileUri.value)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(codeVM.text.value)
                }
            } ?: run {
                Toast.makeText(this, "Failed to save file", Toast.LENGTH_SHORT).show()
            }
        } catch (e: FileNotFoundException) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
}

@Composable
fun MainScreen(
    codeVM: CodeVM,
    openFile: () -> Unit,
    save: () -> Unit,
    saveAs: () -> Unit,
    openDirectory: () -> Unit,
    directoryVM: DirectoryTreeVM
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DirectoryTreeMenu(open=openDirectory, directoryVM=directoryVM)
        }
    ) {
        ScreenLayout(codeVM, openFile, save, saveAs)
    }
}

@Composable
fun DirectoryTreeMenu(open: () -> Unit, directoryVM: DirectoryTreeVM) {
    val currentDirectory: DirectoryEntry? by directoryVM.currentEntry.collectAsState()
    Column(modifier = Modifier
                .fillMaxHeight()
                .background(BackgroundColor)
                .padding(8.dp)
    ) {
        Button(onClick = open) {
            Text(text = "Open directory")
        }
        currentDirectory?.let {
            DirectoryTree(it)
        }
    }
}
@Composable
fun DirectoryTree(entry: DirectoryEntry, padding: Int = 0) {
    Column(modifier = Modifier.padding(start = padding.dp)) {
        Text(text = entry.name(), style = MaterialTheme.typography.bodyMedium, color = Color.Black)

        entry.subEntries().forEach { subEntry ->
            DirectoryTree(subEntry, padding+DirectoryTreePaddingIncrement)
        }
    }
}

@Composable
fun ScreenLayout(codeVM: CodeVM, open: () -> Unit, save:() -> Unit, saveAs: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CodeArea(modifier=Modifier.weight(1.0f), codeVM)
        Spacer(modifier = Modifier.height(ButtonTextFieldSpacing))
        ButtonRow(codeVM, open, save, saveAs)
    }
}

@Composable
fun ButtonRow(codeVM: CodeVM, open: ()->Unit, save:() -> Unit, saveAs: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ButtonRowPadding),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(onClick = open) { Text("Open") }
        Button(onClick = save) { Text("Save") }
        Button(onClick = saveAs) { Text("Save as") }
    }
}

@Composable
fun CodeArea(modifier: Modifier, codeVM: CodeVM) {
    val text by codeVM.text.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(ButtonRowPadding)
            .border(BorderThickness, BorderColor, RoundedCornerShape(CornerRadius))
            .background(CodeAreaBackgroundColor)
    ) {
        TextField(
            value = text,
            onValueChange = { newText -> codeVM.text.value = newText },
            modifier = Modifier.fillMaxSize(),
            placeholder = {
                Text(text = "// your code here", color= Color.Gray)
            },
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent
            )
        )
    }
}