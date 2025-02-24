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
import androidx.compose.foundation.layout.height
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
    currentLanguage: String,
    mainActivity: MainActivity
) {
    var directoryTreeWidthFraction by remember { mutableFloatStateOf(0.3f) }
    var currentColorMode by remember { mutableStateOf("Normal") }
    var autosaveState = true
    if (mainActivity.showGitMenu) {
        GitMenu(
            directoryVM = directoryVM,
            fileVM = mainActivity.fileVM,
            codeVM = codeVM,
            mainActivity = mainActivity,
            currentLanguage = currentLanguage,
            onClose = { mainActivity.showGitMenu = false }
        )
    } else {

        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(directoryTreeWidthFraction)
            ) {
                DirectoryTreeMenu(openDirectory, currentColorMode, currentLanguage,directoryVM) { d ->
                    if (autosaveState) save.invoke()
                    onEntryClicked.invoke(d)
                }
            }
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            directoryTreeWidthFraction =
                                (directoryTreeWidthFraction + dragAmount / dragSpeed)
                                    .coerceIn(minDirectoryFraction, 1f - minCodeFraction)
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
                    .weight(1f - directoryTreeWidthFraction)
            ) {
                ScreenLayout(
                    codeVM = codeVM,
                    currentColorGroup = currentColorMode,
                    currentLanguage = currentLanguage,
                    open = openFile,
                    save = save,
                    saveAs = { saveAs.invoke() },
                    updateAutosaveState = { autosaveState = !autosaveState },
                    exitApp = {
                        if (autosaveState) save.invoke()
                        exitApp.invoke()
                    },
                    createFile = { createFile.invoke() },
                    mainActivity = mainActivity,
                    defaultAutosave = autosaveState,
                    textColor = ColorGroups[currentColorMode]!!.textColor,
                    modeChange = {
                        currentColorMode = if (currentColorMode == "Normal") "Dark" else "Normal"
                    }
                )
            }
        }
    }
}

/**
    Function setting layout of screen: part of app with CodeArea and menu.
    @param codeVM: reference to object of class CodeVM
    @param currentColorGroup: String setting current color mode of app
    @param currentLanguage: String setting current language
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
fun ScreenLayout(codeVM: CodeVM, currentColorGroup: String, currentLanguage:String, open: () -> Unit, save:() -> Unit, saveAs: () -> Unit,
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
        CodeArea(modifier= Modifier.weight(1.0f), currentColorGroup,
            currentLanguage, codeVM)
        Spacer(modifier = Modifier.height(ButtonTextFieldSpacing))
        ButtonRow(codeVM, currentColorGroup, currentLanguage, open, save, saveAs, updateAutosaveState,
            exitApp, createFile, mainActivity, defaultAutosave, textColor, modeChange)
    }
}

/**
    Function managing layout of menu, currently three states: HIDE, SHOW and SETTINGS.
    @param codeVM: reference to object of CodeVM class
    @param currentColorMode: String setting current color mode of ButtonRow
    @param currentLanguage: String setting current language
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
fun ButtonRow(codeVM: CodeVM, currentColorMode: String, currentLanguage: String, open: ()->Unit, save:() -> Unit,
              saveAs: () -> Unit,
              updateAutosaveState: ()->Unit, exitApp: () -> Unit, createFile: () -> Unit,
              mainActivity: MainActivity, defaultAutosave: Boolean, textColor: Color,
              modeChange: () -> Unit) {
    //var setting visibility of menu, changing it changes menu mode
    var menuVisibility by remember { mutableStateOf(ButtonRowState.HIDE) }
    //var setting state of autosave
    var isAutosaveOn by remember { mutableStateOf(defaultAutosave) }

    val targetHeight = when(menuVisibility){
        ButtonRowState.HIDE -> 30.dp
        ButtonRowState.SHOW -> LanguageMeasurementsMap[currentLanguage]!!.mainMenuMeasurements
        ButtonRowState.SETTINGS -> LanguageMeasurementsMap[currentLanguage]!!.settingsMenuMeasurements
        ButtonRowState.LANGUAGE_SETTINGS -> (50.dp * ((LanguageMap.size/3).toInt()+2))
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
                MenuToggleButton(menuVisibility = menuVisibility, currentLanguage) {
                    menuVisibility = ButtonRowState.SHOW
                }
            }
            ButtonRowState.SHOW -> {
                MenuToggleButton(menuVisibility = menuVisibility, currentLanguage) {
                    menuVisibility = ButtonRowState.HIDE
                }
                MenuActions(open, save, saveAs,
                    { menuVisibility = ButtonRowState.SETTINGS },
                    exitApp, createFile, currentLanguage, mainActivity
                    )
            }
            ButtonRowState.SETTINGS -> {
                SettingMenu({
                    updateAutosaveState.invoke()
                    isAutosaveOn = !isAutosaveOn
                            },
                    currentColorMode,
                    currentLanguage,
                    isAutosaveOn, { menuVisibility = ButtonRowState.LANGUAGE_SETTINGS },
                    { menuVisibility = ButtonRowState.SHOW  },
                    modeChange)
            }
            ButtonRowState.LANGUAGE_SETTINGS -> {
                LanguageMenu(currentLanguage, mainActivity) { menuVisibility = ButtonRowState.SETTINGS }
            }
        }
    }
}

/**
    Function setting button showing/hiding menu.
    @param menuVisibility: enum setting mode of menu, when MenuToggleButton is visible,
                            menu can be in mode HIDE or SHOW
    @param currentLanguage: String setting current language
    @param onToggle: action triggering change of visibility of menu
 */

@Composable
private fun MenuToggleButton(menuVisibility: ButtonRowState, currentLanguage: String, onToggle: () -> Unit) {
    Button(onClick = onToggle) {
        Text(
            text = if (menuVisibility == ButtonRowState.HIDE) LanguageMap[currentLanguage]!!.showMenuText
                else LanguageMap[currentLanguage]!!.hideMenuText,
            fontSize = if (menuVisibility == ButtonRowState.SHOW) 14.sp else 10.sp
        )
    }
}


