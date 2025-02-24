package com.example.codeeditor.composables

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.codeeditor.activities.MainActivity
import com.example.codeeditor.constants.ButtonRowPadding
import com.example.codeeditor.constants.LanguageMap
import com.example.codeeditor.model.FileLogic
import com.example.codeeditor.viewmodels.CodeVM
import com.example.codeeditor.viewmodels.DirectoryTreeVM
import com.example.codeeditor.viewmodels.FileVM

@Composable
fun GitMenu(
    directoryVM: DirectoryTreeVM,
    fileVM: FileVM,
    codeVM: CodeVM,
    mainActivity: MainActivity,
    currentLanguage: String,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ButtonRowPadding)
    ) {
        Button(onClick = {
            val currentDir = directoryVM.currentEntry.value
            if (currentDir == null) {
                Toast.makeText(
                    mainActivity,
                    LanguageMap[currentLanguage]!!.gitInitializeErrorText,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val initResult = FileLogic.initGitRepository(mainActivity, currentDir.uri())
                Toast.makeText(mainActivity, initResult, Toast.LENGTH_LONG).show()
            }
        }) {
            Text(LanguageMap[currentLanguage]!!.initializeGitRepoText)
        }

        Button(onClick = {
            val currentFile = fileVM.fileUri.value
            if (currentFile == null || currentFile == Uri.EMPTY) {
                Toast.makeText(
                    mainActivity, LanguageMap[currentLanguage]!!.noFileOpenErrorText,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val addResult = FileLogic.gitAdd(
                    mainActivity,
                    directoryVM.currentEntry.value?.uri(),
                    currentFile
                )
                Toast.makeText(mainActivity, addResult, Toast.LENGTH_LONG).show()
            }
        }) {
            Text(LanguageMap[currentLanguage]!!.gitAddCurrentFileText)
        }

        Button(onClick = {
            val commitResult = FileLogic.gitCommit(
                mainActivity,
                directoryVM.currentEntry.value?.uri(),
                "Commit from app"
            )
            Toast.makeText(mainActivity, commitResult, Toast.LENGTH_LONG).show()
        }) {
            Text("Git Commit")
        }

        Button(onClick = {
            val pushResult = FileLogic.gitPush(mainActivity, directoryVM.currentEntry.value?.uri())
            Toast.makeText(mainActivity, pushResult, Toast.LENGTH_LONG).show()
        }) {
            Text("Git Push")
        }

        Button(onClick = onClose) {
            Text(LanguageMap[currentLanguage]!!.closeGitMenuText)
        }
    }
}