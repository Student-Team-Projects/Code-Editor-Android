package com.example.codeeditor.constants

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.codeeditor.ui.theme.Purple0
import com.example.codeeditor.ui.theme.Purple1
import com.example.codeeditor.ui.theme.Purple3
import com.example.codeeditor.ui.theme.Purple4
import com.example.codeeditor.ui.theme.Purple5

data class ColorGroup(
    val backgroundColor: Color,
    val codeAreaBackgroundColor: Color,
    val borderColor: Color,
    val buttonColor: Color,
    val textColor: Color,
    val placeholderTextColor: Color
)

val ColorGroups = mapOf(
    "Normal" to ColorGroup(
        backgroundColor = Purple5,
        codeAreaBackgroundColor = Purple5,
        borderColor = Purple3,
        buttonColor = Purple1,
        textColor = Purple0,
        placeholderTextColor = Purple3
    ),
    "Dark" to ColorGroup(
        backgroundColor = Purple0,
        codeAreaBackgroundColor = Purple0,
        borderColor = Purple3,
        buttonColor = Purple1,
        textColor = Purple5,
        placeholderTextColor = Purple4
    )
)

data class LanguageGroup(
    val showMenuText: String,
    val hideMenuText: String,
    val openFileText: String,
    val saveFileText: String,
    val saveAsText: String,
    val createFileText: String,
    val settingsText: String,
    val exitAppText: String,
    val exitAppQuestionText: String,
    val yesText: String,
    val noText: String,
    val autosaveText: String,
    val switchOnText: String,
    val switchOffText: String,
    val darkmodeText: String,
    val returnText: String,
    val gitInitializeErrorText: String,
    val initializeGitRepoText: String,
    val noFileOpenErrorText: String,
    val gitAddCurrentFileText: String,
    val closeGitMenuText: String,
    val failedToOpenDirectoryText: String,
    val failedToOpenFileText: String,
    val fileNotFoundText: String,
    val failedToSaveFileText: String,
    val emptyCodeAreaText: String,
    val openDirectoryText: String,
    val changeLanguageText: String
)

val LanguageMap = mapOf(
    "English" to LanguageGroup(
        showMenuText = "Show Menu",
        hideMenuText = "Hide Menu",
        openFileText = "Open",
        saveFileText = "Save",
        saveAsText = "Save as",
        createFileText = "Create file",
        settingsText = "Settings",
        exitAppText = "Exit app",
        exitAppQuestionText = "All unsaved changes will be lost without autosave, do you wish to proceed?",
        yesText = "Yes",
        noText = "No",
        autosaveText = "Autosave",
        switchOnText = "Switch On",
        switchOffText = "Switch Off",
        darkmodeText = "Darkmode",
        returnText = "Return",
        gitInitializeErrorText = "You need to open directory first",
        initializeGitRepoText = "Initialize Git Repo",
        noFileOpenErrorText = "No file open to add",
        gitAddCurrentFileText = "Git Add Current File",
        closeGitMenuText = "Close Git Menu",
        failedToOpenDirectoryText = "Failed to open directory: ",
        failedToOpenFileText = "Failed to open file",
        fileNotFoundText = "File not found",
        failedToSaveFileText = "Failed to save file",
        emptyCodeAreaText = "//your code here",
        openDirectoryText = "Open directory",
        changeLanguageText = "Change language"
    ),
    "Polski" to LanguageGroup(
        showMenuText = "Pokaż Menu",
        hideMenuText = "Schowaj Menu",
        openFileText = "Otwórz",
        saveFileText = "Zapisz",
        saveAsText = "Zapisz jako",
        createFileText = "Stwórz plik",
        settingsText = "Ustawienia",
        exitAppText = "Wyjdź",
        exitAppQuestionText = "Wszystkie niezapisane zmiany przepadną bez włączonego autozapisywania, czy chcesz kontynuować?",
        yesText = "Tak",
        noText = "Nie",
        autosaveText = "Autozapis",
        switchOnText = "Włącz",
        switchOffText = "Wyłącz",
        darkmodeText = "Ciemny motyw",
        returnText = "Wróć",
        gitInitializeErrorText = "Musisz najpierw otworzyć folder",
        initializeGitRepoText = "Inicializuj repozytorium Git",
        noFileOpenErrorText = "Nie ma otwartego pliku do dodania",
        gitAddCurrentFileText = "Git Dodaj Otwarty Plik",
        closeGitMenuText = "Zamknij Git Menu",
        failedToOpenDirectoryText = "Nie udało się otworzyć folderu: ",
        failedToOpenFileText = "Nie udało się otworzyć pliku",
        fileNotFoundText = "Nie znaleziono pliku",
        failedToSaveFileText = "Nie udało się zapisać pliku",
        emptyCodeAreaText = "//miejsce na Twój kod",
        openDirectoryText = "Otwórz folder",
        changeLanguageText = "Zmień język"
    )
)

data class LanguageMeasurements(
    val mainMenuMeasurements: Dp,
    val settingsMenuMeasurements: Dp
)

val LanguageMeasurementsMap = mapOf(
    "English" to LanguageMeasurements(
        mainMenuMeasurements = 230.dp,
        settingsMenuMeasurements = 230.dp
    ),
    "Polski" to LanguageMeasurements(
        mainMenuMeasurements = 245.dp,
        settingsMenuMeasurements = 230.dp
    )
)

public val ButtonRowPadding = 8.dp
public val ButtonTextFieldSpacing = 0.dp
public val BorderThickness = 1.dp
public val CornerRadius = 8.dp
public const val DirectoryTreePaddingIncrement = 8
public val searchingExtensions = arrayOf("*/*")
public const val saveAsDefaultExtension = "*/*"
public const val epsilon = 10e-6f
