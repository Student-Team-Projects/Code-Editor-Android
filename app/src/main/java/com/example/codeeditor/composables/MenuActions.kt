package com.example.codeeditor.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.codeeditor.activities.MainActivity
import com.example.codeeditor.constants.ButtonRowPadding
import com.example.codeeditor.constants.LanguageMap

/**
    Function setting layout of main menu.
    @param open: action triggered when button for opening file is clicked
    @param save: action triggered when button for saving file is clicked
    @param saveAs: action triggered when button for saving new file is clicked
    @param settings: action triggered when button for opening setting menu is clicked
    @param exit: action triggering exiting app
    @param createFile: action triggered when button for creating file is clicked
 */
@Composable
fun MenuActions(
    open: () -> Unit,
    save: () -> Unit,
    saveAs: () -> Unit,
    settings: () -> Unit,
    exit: () -> Unit,
    createFile: () -> Unit,
    currentLanguage: String,
    mainActivity: MainActivity
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ButtonRowPadding),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(onClick = open) { Text(LanguageMap[currentLanguage]!!.openFileText) }
        Button(onClick = save) { Text(LanguageMap[currentLanguage]!!.saveFileText) }
        Button(onClick = saveAs) { Text(LanguageMap[currentLanguage]!!.saveAsText) }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ButtonRowPadding),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(onClick = createFile) { Text(LanguageMap[currentLanguage]!!.createFileText) }
        Button(onClick = settings) { Text(LanguageMap[currentLanguage]!!.settingsText) }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ButtonRowPadding),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(onClick = { mainActivity.showGitMenu = true }) {
            Text("Git")
        }
        Button(
            onClick = {
                showDialogWithTwoButtons(
                    context = mainActivity,
                    message = LanguageMap[currentLanguage]!!.exitAppQuestionText,
                    positiveButtonText = LanguageMap[currentLanguage]!!.yesText,
                    negativeButtonText = LanguageMap[currentLanguage]!!.noText,
                    positiveAction = exit,
                    negativeAction = {}
                )
            }
        ) { Text(LanguageMap[currentLanguage]!!.exitAppText) }
    }
}