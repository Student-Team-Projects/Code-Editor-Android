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

/**
    Function setting layout of main screen of app.
    @param codeVM: reference to CodeVM object
    @param openFile: action triggering process of opening file
    @param save: action triggering process of saving file
    @param saveAs: action triggering saving new file
    @param openDirectory: action opening directory in app
    @param directoryVM: reference to directoryVM object
    @param onEntryCLicked: action triggered when entry in directory tree is clicked
    @param createFile: action triggering process of creating new file
    @param exitApp: action exiting app
    @param mainActivity: reference to MainActivity of app
 */

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

/**
    Function setting layout of screen: part of app with CodeArea and menu.
    @param codeVM: reference to object of class CodeVM
    @param currentColorGroup: String setting current color mode of app
    @param open: action triggering process of opening file
    @param save: action triggering process of saving file
    @param saveAs: action triggering process of saving new file
    @param updateAutosaveState: action changing state of autosave
    @param exitApp: action exiting app
    @param createFile: action triggering process of creating file
    @param mainActivity: reference to MainActivity of app
    @param defaultAutosave: Boolean setting default autosave state
    @param textColor
    @param modeChange: action triggering change of app color mode
 */

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

/**
    Function managing layout of menu, currently three states: HIDE, SHOW and SETTINGS.
    @param codeVM: reference to object of CodeVM class
    @param currentColorMode: String setting current color mode of ButtonRow
    @param save: action triggering process of saving file
    @param saveAs: action triggering process of saving new file
    @param updateAutosaveState: action changing autosave state
    @param exitApp: action closing app
    @param createFile: action triggering process of creating file
    @param mainActivity: reference to MainActivity of app
    @param defaultAutosave: Boolean setting default state of autosave
    @param textColor
    @param modeChange: action changing color mode of app
 */

@Composable
fun ButtonRow(codeVM: CodeVM, currentColorMode: String,open: ()->Unit, save:() -> Unit, saveAs: () -> Unit,
              updateAutosaveState: ()->Unit, exitApp: () -> Unit, createFile: () -> Unit,
              mainActivity: MainActivity, defaultAutosave: Boolean, textColor: Color,
              modeChange: () -> Unit) {
    //var setting visibility of menu, changing it changes menu mode
    var menuVisibility by remember { mutableStateOf(ButtonRowState.HIDE) }
    //var setting state of autosave
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

/**
    Function setting button showing/hiding menu.
    @param menuVisibility: enum setting mode of menu, when MenuToggleButton is visible,
                            menu can be in mode HIDE or SHOW
    @param onToggle: action triggering change of visibility of menu
 */

@Composable
private fun MenuToggleButton(menuVisibility: ButtonRowState, onToggle: () -> Unit) {
    Button(onClick = onToggle) {
        Text(
            text = if (menuVisibility == ButtonRowState.HIDE) "Show Menu" else "Hide Menu",
            fontSize = if (menuVisibility == ButtonRowState.SHOW) 14.sp else 10.sp
        )
    }
}
/**
    Function setting layout of setting menu.
    @param autosaveAction: function triggered when autosave mode is changed
    @param currentColorMode: String setting current color mode of app
    @param autosaveState: Boolean setting current autosave state
    @param back: function triggering going back to main menu
 */

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

/**
    function building and showing dialog window with two buttons:
    @param context: Determines in which context window is shown
    @param message: String which is shown in window
    @param positiveButtonText: String which is shown on button triggering positiveAction
    @param negativeButtonText: String which is shown on button triggering negativeAction
    @param positiveAction: Function triggered when button with positiveButtonText is clicked
    @param negativeAction: Function triggered when button with negativeButtonText is clicked
 */
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

/**
    HIDE - ButtonRow is hidden, only toggle button visible
    SHOW - ButtonRow is shown
    SETTINGS - SettingMenu is shown
 */
enum class ButtonRowState {
    HIDE,
    SHOW,
    SETTINGS
}