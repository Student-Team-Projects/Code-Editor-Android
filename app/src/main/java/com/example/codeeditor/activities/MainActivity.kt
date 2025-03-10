package com.example.codeeditor.activities

import DirectoryTreeVMFactory
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import kotlin.system.exitProcess
import android.provider.Settings
import androidx.core.app.ActivityCompat
import android.Manifest



class MainActivity : ComponentActivity() {

    var showGitMenu by mutableStateOf(false)
    var currentLanguage by mutableStateOf("English")

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ requires MANAGE_EXTERNAL_STORAGE
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        } else {
            // Request WRITE_EXTERNAL_STORAGE for Android 10 and below
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                1
            )
        }
    }

    private val codeVM: CodeVM by viewModels()
    val fileVM: FileVM by viewModels()
    private val directoryVM: DirectoryTreeVM by viewModels{DirectoryTreeVMFactory{
        uri -> DocumentFile.fromTreeUri(this, uri) ?: run {
            Toast.makeText(this,
                LanguageMap[currentLanguage]!!.failedToOpenDirectoryText+uri, Toast.LENGTH_LONG).show()
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
            directoryVM.update()
        }
    }
    private val createEmptyFile = registerForActivityResult(
        ActivityResultContracts.CreateDocument(saveAsDefaultExtension)
    ){
        directoryVM.update()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestStoragePermissions()
        setContent {
            CodeEditorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(codeVM=codeVM, openFile=::openDocumentPicker,
                        saveAs=::createFilePicker, save=::saveCodeToFile,
                        directoryVM=directoryVM, openDirectory=::openDirectoryPicker,
                        onEntryClicked=::onEntryClicked, createFile=::createFile,
                        exitApp=::exitApp,
                        currentLanguage = currentLanguage,
                        mainActivity = this
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

    private fun createFile() {
        createEmptyFile.launch("newFile.txt")
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
                Toast.makeText(this, LanguageMap[currentLanguage]!!.failedToOpenFileText,
                    Toast.LENGTH_SHORT).show()
            }
        } catch (e: FileNotFoundException) {
            Toast.makeText(this, LanguageMap[currentLanguage]!!.fileNotFoundText,
                Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, LanguageMap[currentLanguage]!!.failedToSaveFileText,
                    Toast.LENGTH_SHORT).show()
            }
        } catch (e: FileNotFoundException) {
            Toast.makeText(this, LanguageMap[currentLanguage]!!.fileNotFoundText,
                Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun exitApp() {
        finishAndRemoveTask()
        exitProcess(-1)
    }
}
