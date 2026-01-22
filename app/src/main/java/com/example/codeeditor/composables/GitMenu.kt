package com.example.codeeditor.composables

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.codeeditor.activities.MainActivity
import com.example.codeeditor.constants.ButtonRowPadding
import com.example.codeeditor.constants.LanguageMap
import com.example.codeeditor.model.FileLogic
import com.example.codeeditor.viewmodels.CodeVM
import com.example.codeeditor.viewmodels.DirectoryTreeVM
import com.example.codeeditor.viewmodels.FileVM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun GitMenu(
    directoryVM: DirectoryTreeVM,
    fileVM: FileVM,
    codeVM: CodeVM,
    mainActivity: MainActivity,
    currentLanguage: String,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    
    var showCommitDialog by remember { mutableStateOf(false) }
    var showPushDialog by remember { mutableStateOf(false) }
    var showAddRemoteDialog by remember { mutableStateOf(false) }
    var showAddFilesDialog by remember { mutableStateOf(false) }
    
    var commitMessage by remember { mutableStateOf("") }
    var pushUsername by remember { mutableStateOf("") }
    var pushPassword by remember { mutableStateOf("") }
    var remoteName by remember { mutableStateOf("origin") }
    var remoteUrl by remember { mutableStateOf("") }
    
    // File selection state
    var availableFiles by remember { mutableStateOf<List<String>>(emptyList()) }
    val selectedFiles = remember { mutableStateListOf<String>() }

    // Helper function to run git operations on background thread
    fun runGitOperation(operation: () -> String) {
        scope.launch {
            isLoading = true
            val result = withContext(Dispatchers.IO) {
                operation()
            }
            isLoading = false
            Toast.makeText(mainActivity, result, Toast.LENGTH_LONG).show()
        }
    }

    // Commit Dialog
    if (showCommitDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoading) showCommitDialog = false },
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
                        maxLines = 5,
                        enabled = !isLoading
                    )
                    if (isLoading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val currentDir = directoryVM.currentEntry.value
                        scope.launch {
                            isLoading = true
                            val result = withContext(Dispatchers.IO) {
                                FileLogic.gitCommit(
                                    mainActivity,
                                    currentDir?.uri(),
                                    commitMessage
                                )
                            }
                            isLoading = false
                            Toast.makeText(mainActivity, result, Toast.LENGTH_LONG).show()
                            if (result.startsWith("Committed:")) {
                                commitMessage = ""
                                showCommitDialog = false
                            }
                        }
                    },
                    enabled = !isLoading && commitMessage.isNotBlank()
                ) {
                    Text("Commit")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCommitDialog = false },
                    enabled = !isLoading
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Push Dialog (with optional credentials)
    if (showPushDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoading) showPushDialog = false },
            title = { Text("Git Push") },
            text = {
                Column {
                    Text(
                        "Credentials (optional - for HTTPS):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pushUsername,
                        onValueChange = { pushUsername = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pushPassword,
                        onValueChange = { pushPassword = it },
                        label = { Text("Password / Token") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        enabled = !isLoading
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tip: For GitHub, use a Personal Access Token as password",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (isLoading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val currentDir = directoryVM.currentEntry.value
                        scope.launch {
                            isLoading = true
                            val result = withContext(Dispatchers.IO) {
                                FileLogic.gitPush(
                                    mainActivity,
                                    currentDir?.uri(),
                                    pushUsername.ifEmpty { null },
                                    pushPassword.ifEmpty { null }
                                )
                            }
                            isLoading = false
                            Toast.makeText(mainActivity, result, Toast.LENGTH_LONG).show()
                            if (result.startsWith("Push successful")) {
                                showPushDialog = false
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text("Push")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPushDialog = false },
                    enabled = !isLoading
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Files Dialog (with checkboxes)
    if (showAddFilesDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoading) showAddFilesDialog = false },
            title = { Text("Select Files to Add") },
            text = {
                Column {
                    if (isLoading) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (availableFiles.isEmpty()) {
                        Text(
                            "No files to add. Working tree is clean.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            "${availableFiles.size} file(s) available",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Select All / Deselect All
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { 
                                selectedFiles.clear()
                                selectedFiles.addAll(availableFiles) 
                            }) {
                                Text("Select All")
                            }
                            TextButton(onClick = { selectedFiles.clear() }) {
                                Text("Deselect All")
                            }
                        }
                        
                        // File list with checkboxes
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                        ) {
                            items(availableFiles) { file ->
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
                                            if (checked) {
                                                selectedFiles.add(file)
                                            } else {
                                                selectedFiles.remove(file)
                                            }
                                        }
                                    )
                                    Text(
                                        text = file,
                                        style = MaterialTheme.typography.bodyMedium,
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
                        val currentDir = directoryVM.currentEntry.value
                        scope.launch {
                            isLoading = true
                            val result = withContext(Dispatchers.IO) {
                                FileLogic.gitAddFiles(
                                    mainActivity,
                                    currentDir?.uri(),
                                    selectedFiles.toList()
                                )
                            }
                            isLoading = false
                            Toast.makeText(mainActivity, result, Toast.LENGTH_LONG).show()
                            if (result.contains("Added")) {
                                selectedFiles.clear()
                                showAddFilesDialog = false
                            }
                        }
                    },
                    enabled = !isLoading && selectedFiles.isNotEmpty()
                ) {
                    Text("Add ${selectedFiles.size} file(s)")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showAddFilesDialog = false
                        selectedFiles.clear()
                    },
                    enabled = !isLoading
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Remote Dialog
    if (showAddRemoteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoading) showAddRemoteDialog = false },
            title = { Text("Add Remote") },
            text = {
                Column {
                    OutlinedTextField(
                        value = remoteName,
                        onValueChange = { remoteName = it },
                        label = { Text("Remote Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = remoteUrl,
                        onValueChange = { remoteUrl = it },
                        label = { Text("Remote URL") },
                        placeholder = { Text("https://github.com/user/repo.git") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading
                    )
                    if (isLoading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val currentDir = directoryVM.currentEntry.value
                        scope.launch {
                            isLoading = true
                            val result = withContext(Dispatchers.IO) {
                                FileLogic.gitAddRemote(
                                    mainActivity,
                                    currentDir?.uri(),
                                    remoteName,
                                    remoteUrl
                                )
                            }
                            isLoading = false
                            Toast.makeText(mainActivity, result, Toast.LENGTH_LONG).show()
                            if (result.contains("added")) {
                                remoteUrl = ""
                                showAddRemoteDialog = false
                            }
                        }
                    },
                    enabled = !isLoading && remoteName.isNotBlank() && remoteUrl.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddRemoteDialog = false },
                    enabled = !isLoading
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ButtonRowPadding)
            .verticalScroll(rememberScrollState())
    ) {
        // Loading indicator
        if (isLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                Text("Processing...")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Initialize Git Repository
        Button(
            onClick = {
                val currentDir = directoryVM.currentEntry.value
                if (currentDir == null) {
                    Toast.makeText(
                        mainActivity,
                        LanguageMap[currentLanguage]!!.gitInitializeErrorText,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    runGitOperation {
                        FileLogic.initGitRepository(mainActivity, currentDir.uri())
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(LanguageMap[currentLanguage]!!.initializeGitRepoText)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Git Status
        Button(
            onClick = {
                val currentDir = directoryVM.currentEntry.value
                if (currentDir == null) {
                    Toast.makeText(
                        mainActivity,
                        "Open a folder first",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    runGitOperation {
                        FileLogic.gitStatus(mainActivity, currentDir.uri())
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("Git Status")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Git Add buttons in a row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Git Add Files (with selection dialog)
            Button(
                onClick = {
                    val currentDir = directoryVM.currentEntry.value
                    if (currentDir == null) {
                        Toast.makeText(
                            mainActivity,
                            "Open a folder first",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // Load available files and show dialog
                        scope.launch {
                            isLoading = true
                            val files = withContext(Dispatchers.IO) {
                                FileLogic.getUnstagedFiles(mainActivity, currentDir.uri())
                            }
                            availableFiles = files
                            selectedFiles.clear()
                            isLoading = false
                            showAddFilesDialog = true
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Text("Add Files")
            }

            // Git Add All
            Button(
                onClick = {
                    val currentDir = directoryVM.currentEntry.value
                    if (currentDir == null) {
                        Toast.makeText(
                            mainActivity,
                            "Open a folder first",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        runGitOperation {
                            FileLogic.gitAddAll(mainActivity, currentDir.uri())
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Text("Add All")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Git Commit
        Button(
            onClick = {
                val currentDir = directoryVM.currentEntry.value
                if (currentDir == null) {
                    Toast.makeText(
                        mainActivity,
                        "Open a folder first",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    showCommitDialog = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("Git Commit")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Remote operations row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Add Remote
            Button(
                onClick = {
                    val currentDir = directoryVM.currentEntry.value
                    if (currentDir == null) {
                        Toast.makeText(
                            mainActivity,
                            "Open a folder first",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        showAddRemoteDialog = true
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Text("Add Remote")
            }

            // Git Push
            Button(
                onClick = {
                    val currentDir = directoryVM.currentEntry.value
                    if (currentDir == null) {
                        Toast.makeText(
                            mainActivity,
                            "Open a folder first",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        showPushDialog = true
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Text("Git Push")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Close Button
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            ),
            enabled = !isLoading
        ) {
            Text(LanguageMap[currentLanguage]!!.closeGitMenuText)
        }
    }
}
