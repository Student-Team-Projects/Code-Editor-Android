package com.example.codeeditor.activities
import org.eclipse.jgit.api.Git
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.example.codeeditor.composables.MainScreen
import com.example.codeeditor.constants.*
import com.example.codeeditor.ui.theme.CodeEditorTheme
import com.example.codeeditor.viewmodels.CodeVM
import com.example.codeeditor.viewmodels.DirectoryEntry
import com.example.codeeditor.viewmodels.DirectoryTreeVM
import com.example.codeeditor.viewmodels.FileVM
import java.io.*

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
                    MainScreen(codeVM=codeVM, openFile=::openDocumentPicker,
                        saveAs=::createFilePicker, save=::saveCodeToFile,
                        directoryVM=directoryVM, openDirectory=::openDirectoryPicker,
                        onEntryClicked=::onEntryClicked, gitInit = ::initializeGitRepository,
                        gitAdd = ::gitAdd,
                        gitCommit = ::gitCommit,
                        gitStatus = ::gitStatus

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
        fileVM.updateFileUri(uri)
    }

    private fun updateCurrentDirectory(uri: Uri) {
        directoryVM.currentEntry.value = directoryVM.directoryEntry(uri).getOrNull()
    }

    fun readTextFromUri(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val text = BufferedReader(InputStreamReader(inputStream)).readText()
                codeVM.updateText(text)
            } ?: run {
                Toast.makeText(this, "Failed to open file", Toast.LENGTH_SHORT).show()
            }
        } catch (e: FileNotFoundException) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun onEntryClicked(directoryEntry: DirectoryEntry) {
        if (directoryEntry.isFile()) {
            updateCurrentFile(directoryEntry.uri())
            readTextFromUri(directoryEntry.uri())
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


    private fun initializeGitRepository(uri: Uri) {
        try {
            val currentDirectory = DocumentFile.fromTreeUri(this, uri)
            currentDirectory?.let { dir ->
                val gitDirectoryName = "${dir.name}_git"
                val gitDirectory = dir.createDirectory(gitDirectoryName)

                if (gitDirectory == null) {
                    Toast.makeText(this, "Failed to create Git directory", Toast.LENGTH_SHORT).show()
                    return
                }

                val localGitDir = File(filesDir, gitDirectoryName)
                if (!localGitDir.exists()) localGitDir.mkdirs()

                Git.init().setDirectory(localGitDir).call()

                Toast.makeText(
                    this,
                    "Git repository initialized at: ${localGitDir.absolutePath}",
                    Toast.LENGTH_LONG
                ).show()
            } ?: run {
                Toast.makeText(this, "Invalid directory for Git init", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to initialize Git: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun getLocalGitDirectory(uri: Uri): File {
        val documentFile = DocumentFile.fromTreeUri(this, uri)
        val gitDirName = "${documentFile?.name}_git"

        return File(filesDir, gitDirName)
    }


    private fun gitAdd(uri: Uri) {

    }

    private fun gitCommit(uri: Uri, commitMessage: String) {

    }

    private fun gitStatus(uri: Uri) {
        try {
            val localGitDir = getLocalGitDirectory(uri)
            val git = Git.open(localGitDir)

            val status = git.status().call()

            val statusMessage = StringBuilder()

            if (status.hasUncommittedChanges()) {
                statusMessage.append("Uncommitted Changes:\n")
            }

            if (status.untracked.isNotEmpty()) {
                statusMessage.append("Untracked Files:\n")
                status.untracked.forEach { statusMessage.append("- $it\n") }
            }

            if (status.modified.isNotEmpty()) {
                statusMessage.append("Modified Files:\n")
                status.modified.forEach { statusMessage.append("- $it\n") }
            }

            if (status.added.isNotEmpty()) {
                statusMessage.append("Added Files:\n")
                status.added.forEach { statusMessage.append("- $it\n") }
            }

            if (statusMessage.isEmpty()) {
                statusMessage.append("No changes. Working tree is clean.")
            }

            Toast.makeText(this, statusMessage.toString(), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to check status: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }








}