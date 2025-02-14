package com.example.codeeditor.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.AlertDialog
import android.content.Context
import com.example.codeeditor.activities.MainActivity
import com.example.codeeditor.constants.*
import com.example.codeeditor.viewmodels.CodeVM
import com.example.codeeditor.viewmodels.DirectoryEntry
import com.example.codeeditor.viewmodels.DirectoryTreeVM

private const val dragSpeed = 2000f
private const val minDirectoryFraction = epsilon
private const val normalDirectoryFraction = 0.2f
private const val minCodeFraction = 0.35f

@Composable
fun MainScreen(
    codeVM: CodeVM,
    openFile: () -> Unit,
    save: () -> Unit,
    saveAs: () -> Unit,
    openDirectory: () -> Unit,
    directoryVM: DirectoryTreeVM,
    onEntryClicked: (DirectoryEntry) -> Unit,
    createFile: () -> Unit,
    exitApp: () -> Unit,
    mainActivity: MainActivity
) {
    var directoryTreeWidthFraction by remember { mutableFloatStateOf(0.3f) }
    var autosaveState = true

    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(directoryTreeWidthFraction)
        ) {
            DirectoryTreeMenu(openDirectory, directoryVM) { d ->
                if(autosaveState) save.invoke()
                onEntryClicked.invoke(d)
            }
        }

        // slider to manage code and directory ratio
        Box(
            modifier = Modifier
                .width(5.dp)
                .fillMaxHeight()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        directoryTreeWidthFraction = (directoryTreeWidthFraction + dragAmount / dragSpeed).coerceIn(
                            minDirectoryFraction, 1f- minCodeFraction)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            directoryTreeWidthFraction = normalDirectoryFraction
                        }
                    )
                }
                .background(Color.Gray)
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f-directoryTreeWidthFraction)
        ) {
            ScreenLayout(codeVM, openFile, save, {
                saveAs.invoke()
            }, {autosaveState = !autosaveState}, {
                if(autosaveState) save.invoke()
                exitApp.invoke() },
                {
                    createFile.invoke()
                }, mainActivity, autosaveState)
        }
    }
}

@Composable
fun ScreenLayout(codeVM: CodeVM, open: () -> Unit, save:() -> Unit, saveAs: () -> Unit,
                 updateAutosaveState: () -> Unit, exitApp: () -> Unit, createFile: () -> Unit,
                 mainActivity: MainActivity, defaultAutosave: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CodeArea(modifier= Modifier.weight(1.0f), codeVM)
        Spacer(modifier = Modifier.height(ButtonTextFieldSpacing))
        ButtonRow(codeVM, open, save, saveAs, updateAutosaveState,
            exitApp, createFile, mainActivity, defaultAutosave)
    }
}


@Composable
fun ButtonRow(codeVM: CodeVM, open: ()->Unit, save:() -> Unit, saveAs: () -> Unit,
              updateAutosaveState: ()->Unit, exitApp: () -> Unit, createFile: () -> Unit,
              mainActivity: MainActivity, defaultAutosave: Boolean) {
    var menuVisibility by remember { mutableStateOf(buttonRowState.HIDE) }
    var isAutosaveOn by remember { mutableStateOf(defaultAutosave) }

    val targetHeight = when(menuVisibility){
        buttonRowState.HIDE -> 30.dp
        buttonRowState.SHOW -> 230.dp
        buttonRowState.SETTINGS -> 100.dp
    }

    Column(
        modifier = Modifier
            .height(targetHeight)
            .background(BackgroundColor),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when(menuVisibility){
            buttonRowState.HIDE -> {
                MenuToggleButton(menuVisibility = menuVisibility) {
                    menuVisibility = buttonRowState.SHOW
                }
            }
            buttonRowState.SHOW -> {
                MenuToggleButton(menuVisibility = menuVisibility) {
                    menuVisibility = buttonRowState.HIDE
                }
                MenuActions(open, save, saveAs,
                    { menuVisibility = buttonRowState.SETTINGS },
                    exitApp, createFile, mainActivity
                    )
            }
            buttonRowState.SETTINGS -> {
                SettingMenu({
                    updateAutosaveState.invoke()
                    isAutosaveOn = !isAutosaveOn
                            },
                    isAutosaveOn,
                    { menuVisibility = buttonRowState.SHOW  })
            }
        }
    }
}

@Composable
private fun MenuToggleButton(menuVisibility: buttonRowState, onToggle: () -> Unit) {
    Button(onClick = onToggle) {
        Text(
            text = if (menuVisibility == buttonRowState.HIDE) "Show Menu" else "Hide Menu",
            fontSize = if (menuVisibility == buttonRowState.SHOW) 14.sp else 10.sp
        )
    }
}

@Composable
private fun SettingMenu(autosaveAction: () -> Unit,
                            autosaveState: Boolean, back: () -> Unit){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ButtonRowPadding),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ){
        Text(text = "Autosave:", fontSize = 20.sp)
        Button(onClick = autosaveAction){
            Text(if (autosaveState) "Switch Off" else "Switch On")
        }
    }
    Button(onClick = back) {
        Text("Return")
    }
}

@Composable
private fun MenuActions(open: () -> Unit, save: () -> Unit, saveAs: () -> Unit, settings: () -> Unit,
                        exit: () -> Unit, createFile: () -> Unit,mainActivity: MainActivity) {
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ButtonRowPadding),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(onClick = createFile){ Text("Create file") }
        Button(onClick = settings){ Text("Settings") }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ButtonRowPadding),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(onClick = { showDialogWithTwoButtons(context = mainActivity,
        "All unsaved changes will be lost without autosave, do you wish to proceed?",
            "Yes", "No", exit, {})} ){ Text("Exit app") }
    }

}

private fun showDialogWithTwoButtons(context: Context, message: String,
                                     positiveButtonText: String, negativeButtonText: String,
                                     positiveAction: () -> Unit, negativeAction: () -> Unit) {
    AlertDialog.Builder(context)
        .setMessage(message)
        .setPositiveButton(positiveButtonText) { dialog, _ ->
            positiveAction()
            dialog.dismiss()
        }
        .setNegativeButton(negativeButtonText) { dialog, _ ->
            negativeAction()
            dialog.dismiss()
        }
        .show()
}

enum class buttonRowState {
    HIDE,
    SHOW,
    SETTINGS
}