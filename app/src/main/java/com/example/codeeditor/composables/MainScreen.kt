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
    var currentColorMode by remember { mutableStateOf("Normal") }
    var autosaveState = true

    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(directoryTreeWidthFraction)
        ) {
            DirectoryTreeMenu(openDirectory, currentColorMode, directoryVM) { d ->
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
                .background(ColorGroups[currentColorMode]!!.backgroundColor)
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f-directoryTreeWidthFraction)
        ) {
            ScreenLayout(codeVM, currentColorMode,openFile, save, {
                saveAs.invoke()
            }, {autosaveState = !autosaveState}, {
                if(autosaveState) save.invoke()
                exitApp.invoke() },
                {
                    createFile.invoke()
                }, mainActivity, autosaveState, ColorGroups[currentColorMode]!!.textColor,
                { currentColorMode = if (currentColorMode == "Normal") "Dark" else
                "Normal"})
        }
    }
}

@Composable
fun ScreenLayout(codeVM: CodeVM, currentColorGroup: String, open: () -> Unit, save:() -> Unit, saveAs: () -> Unit,
                 updateAutosaveState: () -> Unit, exitApp: () -> Unit, createFile: () -> Unit,
                 mainActivity: MainActivity, defaultAutosave: Boolean, textColor: Color,
                 modeChange: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorGroups[currentColorGroup]!!.backgroundColor),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CodeArea(modifier= Modifier.weight(1.0f), currentColorGroup, codeVM)
        Spacer(modifier = Modifier.height(ButtonTextFieldSpacing))
        ButtonRow(codeVM, currentColorGroup,open, save, saveAs, updateAutosaveState,
            exitApp, createFile, mainActivity, defaultAutosave, textColor, modeChange)
    }
}


@Composable
fun ButtonRow(codeVM: CodeVM, currentColorMode: String,open: ()->Unit, save:() -> Unit, saveAs: () -> Unit,
              updateAutosaveState: ()->Unit, exitApp: () -> Unit, createFile: () -> Unit,
              mainActivity: MainActivity, defaultAutosave: Boolean, textColor: Color,
              modeChange: () -> Unit) {
    var menuVisibility by remember { mutableStateOf(ButtonRowState.HIDE) }
    var isAutosaveOn by remember { mutableStateOf(defaultAutosave) }

    val targetHeight = when(menuVisibility){
        ButtonRowState.HIDE -> 30.dp
        ButtonRowState.SHOW -> 230.dp
        ButtonRowState.SETTINGS -> 180.dp
    }

    Column(
        modifier = Modifier
            .height(targetHeight)
            .background(ColorGroups[currentColorMode]!!.backgroundColor),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when(menuVisibility){
            ButtonRowState.HIDE -> {
                MenuToggleButton(menuVisibility = menuVisibility) {
                    menuVisibility = ButtonRowState.SHOW
                }
            }
            ButtonRowState.SHOW -> {
                MenuToggleButton(menuVisibility = menuVisibility) {
                    menuVisibility = ButtonRowState.HIDE
                }
                MenuActions(open, save, saveAs,
                    { menuVisibility = ButtonRowState.SETTINGS },
                    exitApp, createFile, mainActivity
                    )
            }
            ButtonRowState.SETTINGS -> {
                SettingMenu({
                    updateAutosaveState.invoke()
                    isAutosaveOn = !isAutosaveOn
                            },
                    currentColorMode,
                    isAutosaveOn,
                    { menuVisibility = ButtonRowState.SHOW  },
                    modeChange)
            }
        }
    }
}

@Composable
private fun MenuToggleButton(menuVisibility: ButtonRowState, onToggle: () -> Unit) {
    Button(onClick = onToggle) {
        Text(
            text = if (menuVisibility == ButtonRowState.HIDE) "Show Menu" else "Hide Menu",
            fontSize = if (menuVisibility == ButtonRowState.SHOW) 14.sp else 10.sp
        )
    }
}

@Composable
private fun SettingMenu(autosaveAction: () -> Unit, currentColorMode: String,
                            autosaveState: Boolean, back: () -> Unit,
                        modeChange: () -> Unit){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ButtonRowPadding),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ){
        Text(text = "Autosave:", fontSize = 20.sp, color = ColorGroups[currentColorMode]!!.textColor)
        Button(onClick = autosaveAction){
            Text(if (autosaveState) "Switch Off" else "Switch On")
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ButtonRowPadding),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ){
        Text(text = "Darkmode", fontSize = 20.sp, color = ColorGroups[currentColorMode]!!.textColor)
        Button(onClick = modeChange) {
            Text(if(currentColorMode == "Normal") "Switch On" else "Switch Off")
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

enum class ButtonRowState {
    HIDE,
    SHOW,
    SETTINGS
}