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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.mutableStateListOf
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
    
    // Git dialogs
    GitDialogs(mainActivity = mainActivity, currentLanguage = currentLanguage)
    
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
        Column(modifier = Modifier.fillMaxSize()) {
            // Main content area (directory tree + code editor)
            Row(modifier = Modifier.weight(1f)) {
                // Directory tree panel
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(directoryTreeWidthFraction)
            ) {
                    DirectoryTreeMenu(openDirectory, currentColorMode, currentLanguage, directoryVM) { d ->
                    if (autosaveState) save.invoke()
                    onEntryClicked.invoke(d)
                }
            }
                
                // Resizable divider
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
                
                // Code editor panel
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f - directoryTreeWidthFraction)
            ) {
                    CodeAreaOnly(
                    codeVM = codeVM,
                    currentColorGroup = currentColorMode,
                        currentLanguage = currentLanguage
                    )
                }
            }
            
            // Bottom toolbar - full width
            ButtonRow(
                codeVM = codeVM,
                currentColorMode = currentColorMode,
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
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Code area takes all available space
        CodeArea(
            modifier = Modifier.weight(1.0f), 
            currentColorGroup,
            currentLanguage, 
            codeVM
        )
        
        // Bottom toolbar with hamburger menu
        ButtonRow(
            codeVM, currentColorGroup, currentLanguage, open, save, saveAs, 
            updateAutosaveState, exitApp, createFile, mainActivity, 
            defaultAutosave, textColor, modeChange
        )
    }
}

/**
    Git dialogs for commit, push, add remote, add files.
 */
@Composable
fun GitDialogs(mainActivity: MainActivity, currentLanguage: String) {
    var commitMessage by remember { mutableStateOf("") }
    var pushUsername by remember { mutableStateOf("") }
    var pushPassword by remember { mutableStateOf("") }
    var remoteName by remember { mutableStateOf("origin") }
    var remoteUrl by remember { mutableStateOf("") }
    val selectedFiles = remember { mutableStateListOf<String>() }
    
    // Commit Dialog
    if (mainActivity.showCommitDialog) {
        AlertDialog(
            onDismissRequest = { mainActivity.showCommitDialog = false },
            title = { Text("Git Commit") },
            text = {
                Column {
                    Text("Enter commit message:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = commitMessage,
                        onValueChange = { commitMessage = it },
                        placeholder = { Text("Describe your changes...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mainActivity.performGitCommit(commitMessage)
                        commitMessage = ""
                    },
                    enabled = commitMessage.isNotBlank()
                ) {
                    Text("Commit")
                }
            },
            dismissButton = {
                TextButton(onClick = { mainActivity.showCommitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Push Dialog
    if (mainActivity.showPushDialog) {
        AlertDialog(
            onDismissRequest = { mainActivity.showPushDialog = false },
            title = { Text("Git Push") },
            text = {
                Column {
                    Text("Credentials (optional for HTTPS):", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pushUsername,
                        onValueChange = { pushUsername = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pushPassword,
                        onValueChange = { pushPassword = it },
                        label = { Text("Password / Token") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Text(
                        "Tip: For GitHub use Personal Access Token",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mainActivity.performGitPush(
                            pushUsername.ifEmpty { null },
                            pushPassword.ifEmpty { null }
                        )
                    }
                ) {
                    Text("Push")
                }
            },
            dismissButton = {
                TextButton(onClick = { mainActivity.showPushDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Add Remote Dialog
    if (mainActivity.showAddRemoteDialog) {
        AlertDialog(
            onDismissRequest = { mainActivity.showAddRemoteDialog = false },
            title = { Text("Add Remote") },
            text = {
                Column {
                    OutlinedTextField(
                        value = remoteName,
                        onValueChange = { remoteName = it },
                        label = { Text("Remote Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = remoteUrl,
                        onValueChange = { remoteUrl = it },
                        label = { Text("Remote URL") },
                        placeholder = { Text("https://github.com/user/repo.git") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mainActivity.performGitAddRemote(remoteName, remoteUrl)
                        remoteUrl = ""
                    },
                    enabled = remoteName.isNotBlank() && remoteUrl.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { mainActivity.showAddRemoteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Add Files Dialog
    if (mainActivity.showAddFilesDialog) {
        AlertDialog(
            onDismissRequest = { 
                mainActivity.showAddFilesDialog = false 
                selectedFiles.clear()
            },
            title = { Text("Select Files to Add") },
            text = {
                Column {
                    if (mainActivity.availableGitFiles.isEmpty()) {
                        Text("No files to add. Working tree is clean.")
                    } else {
                        Text("${mainActivity.availableGitFiles.size} file(s) available")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { 
                                selectedFiles.clear()
                                selectedFiles.addAll(mainActivity.availableGitFiles) 
                            }) {
                                Text("Select All")
                            }
                            TextButton(onClick = { selectedFiles.clear() }) {
                                Text("Deselect All")
                            }
                        }
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(mainActivity.availableGitFiles) { file ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (selectedFiles.contains(file)) {
                                                selectedFiles.remove(file)
                                            } else {
                                                selectedFiles.add(file)
                                            }
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selectedFiles.contains(file),
                                        onCheckedChange = { checked ->
                                            if (checked) selectedFiles.add(file)
                                            else selectedFiles.remove(file)
                                        }
                                    )
                                    Text(
                                        text = file,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mainActivity.performGitAddFiles(selectedFiles.toList())
                        selectedFiles.clear()
                    },
                    enabled = selectedFiles.isNotEmpty()
                ) {
                    Text("Add ${selectedFiles.size} file(s)")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    mainActivity.showAddFilesDialog = false
                    selectedFiles.clear()
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
    Code area wrapper for the new layout.
 */
@Composable
fun CodeAreaOnly(
    codeVM: CodeVM,
    currentColorGroup: String,
    currentLanguage: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorGroups[currentColorGroup]!!.backgroundColor)
    ) {
        CodeArea(
            modifier = Modifier.fillMaxSize(),
            currentColorMode = currentColorGroup,
            currentLanguage = currentLanguage,
            codeVM = codeVM
        )
    }
}

/**
    Bottom toolbar with hamburger menu.
 */
@Composable
fun ButtonRow(codeVM: CodeVM, currentColorMode: String, currentLanguage: String, open: ()->Unit, save:() -> Unit,
              saveAs: () -> Unit,
              updateAutosaveState: ()->Unit, exitApp: () -> Unit, createFile: () -> Unit,
              mainActivity: MainActivity, defaultAutosave: Boolean, textColor: Color,
              modeChange: () -> Unit) {
    
    var showMainMenu by remember { mutableStateOf(false) }
    var showLanguageSubmenu by remember { mutableStateOf(false) }
    var showGitSubmenu by remember { mutableStateOf(false) }
    var isAutosaveOn by remember { mutableStateOf(defaultAutosave) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(ColorGroups[currentColorMode]!!.backgroundColor)
            .padding(horizontal = 8.dp)
    ) {
        // Left side - Hamburger Menu
        Box(
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            IconButton(
                onClick = { showMainMenu = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = ColorGroups[currentColorMode]!!.textColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // Main Dropdown Menu - opens from left
            DropdownMenu(
                expanded = showMainMenu,
                onDismissRequest = { 
                    showMainMenu = false 
                    showLanguageSubmenu = false
                    showGitSubmenu = false
                }
            ) {
                // File operations section
                Text(
                    "File",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                DropdownMenuItem(
                    text = { Text(LanguageMap[currentLanguage]!!.openFileText) },
                    onClick = { 
                        showMainMenu = false
                        open()
                    }
                )
                DropdownMenuItem(
                    text = { Text(LanguageMap[currentLanguage]!!.saveFileText) },
                    onClick = { 
                        showMainMenu = false
                        save()
                    }
                )
                DropdownMenuItem(
                    text = { Text(LanguageMap[currentLanguage]!!.saveAsText) },
                    onClick = { 
                        showMainMenu = false
                        saveAs()
                    }
                )
                DropdownMenuItem(
                    text = { Text(LanguageMap[currentLanguage]!!.createFileText) },
                    onClick = { 
                        showMainMenu = false
                        createFile()
                    }
                )
                
                Divider()
                
                // Git submenu - with nested dropdown on the right
                Box {
                    DropdownMenuItem(
                        text = { 
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Git", fontWeight = FontWeight.Medium)
                                Text("▶")
                            }
                        },
                        onClick = { 
                            showGitSubmenu = !showGitSubmenu
                            showLanguageSubmenu = false
                        }
                    )
                    
                    // Git submenu appears to the right (attached to main menu)
                    DropdownMenu(
                        expanded = showGitSubmenu,
                        onDismissRequest = { showGitSubmenu = false },
                        offset = DpOffset(160.dp, (-48).dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text(LanguageMap[currentLanguage]!!.initializeGitRepoText) },
                            onClick = { 
                                showMainMenu = false
                                showGitSubmenu = false
                                mainActivity.gitInit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Git Status") },
                            onClick = { 
                                showMainMenu = false
                                showGitSubmenu = false
                                mainActivity.gitStatus()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add Files") },
                            onClick = { 
                                showMainMenu = false
                                showGitSubmenu = false
                                mainActivity.gitAddFiles()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add All") },
                            onClick = { 
                                showMainMenu = false
                                showGitSubmenu = false
                                mainActivity.gitAddAll()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Commit") },
                            onClick = { 
                                showMainMenu = false
                                showGitSubmenu = false
                                mainActivity.gitCommit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Push") },
                            onClick = { 
                                showMainMenu = false
                                showGitSubmenu = false
                                mainActivity.gitPush()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add Remote") },
                            onClick = { 
                                showMainMenu = false
                                showGitSubmenu = false
                                mainActivity.gitAddRemote()
                            }
                        )
                    }
                }
                
                Divider()
                
                // Settings section
                Text(
                    LanguageMap[currentLanguage]!!.settingsText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Autosave toggle
                DropdownMenuItem(
                    text = { 
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(LanguageMap[currentLanguage]!!.autosaveText)
                            Switch(
                                checked = isAutosaveOn,
                                onCheckedChange = null
                            )
                        }
                    },
                    onClick = { 
                        updateAutosaveState()
                    isAutosaveOn = !isAutosaveOn
                    }
                )
                
                // Dark mode toggle
                DropdownMenuItem(
                    text = { 
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(LanguageMap[currentLanguage]!!.darkmodeText)
                            Switch(
                                checked = currentColorMode == "Dark",
                                onCheckedChange = null
                            )
                        }
                    },
                    onClick = { 
                        modeChange()
                    }
                )
                
                // Language submenu
                DropdownMenuItem(
                    text = { 
                        Text(
                            if (showLanguageSubmenu) 
                                "${LanguageMap[currentLanguage]!!.changeLanguageText} ▼" 
                            else 
                                "${LanguageMap[currentLanguage]!!.changeLanguageText} ▶"
                        ) 
                    },
                    onClick = { 
                        showLanguageSubmenu = !showLanguageSubmenu
                        showGitSubmenu = false
                    }
                )
                
                // Language options
                if (showLanguageSubmenu) {
                    LanguageMap.keys.forEach { lang ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "    $lang",
                                    fontWeight = if (lang == currentLanguage) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = { 
                                mainActivity.currentLanguage = lang
                                showLanguageSubmenu = false
                                showMainMenu = false
                            }
                        )
                    }
                }
                
                Divider()
                
                // Exit
                DropdownMenuItem(
                    text = { 
                        Text(
                            LanguageMap[currentLanguage]!!.exitAppText,
                            color = MaterialTheme.colorScheme.error
                        ) 
                    },
                    onClick = { 
                        showMainMenu = false
                        showDialogWithTwoButtons(
                            context = mainActivity,
                            message = LanguageMap[currentLanguage]!!.exitAppQuestionText,
                            positiveButtonText = LanguageMap[currentLanguage]!!.yesText,
                            negativeButtonText = LanguageMap[currentLanguage]!!.noText,
                            positiveAction = exitApp,
                            negativeAction = {}
                        )
                    }
                )
            }
        }
        
        // Center - Title
        Text(
            text = "Code Editor",
            color = ColorGroups[currentColorMode]!!.textColor,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Center)
        )
        
        // Right side - Quick save
        IconButton(
            onClick = save,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Text(
                text = "💾",
                fontSize = 20.sp
            )
        }
    }
}


