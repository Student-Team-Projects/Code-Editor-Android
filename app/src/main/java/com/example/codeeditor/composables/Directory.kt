package com.example.codeeditor.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.codeeditor.R
import com.example.codeeditor.constants.DirectoryTreePaddingIncrement
import com.example.codeeditor.viewmodels.DirectoryEntry
import com.example.codeeditor.viewmodels.DirectoryTreeVM
import com.example.codeeditor.constants.ColorGroups
import com.example.codeeditor.constants.LanguageMap

@Composable
fun DirectoryTreeMenu(open: () -> Unit, currentColorMode: String, currentLanguage: String,
                      directoryVM: DirectoryTreeVM, onEntryClicked:(DirectoryEntry) -> Unit) {
    val currentDirectory: DirectoryEntry? by directoryVM.currentEntry.collectAsState()
    var registerKey by remember { mutableStateOf(0) }
    directoryVM.register { registerKey++ }
    
    val scrollState = rememberScrollState()
    
    Column(modifier = Modifier
        .fillMaxSize()
        .background(ColorGroups[currentColorMode]!!.backgroundColor)
        .padding(8.dp)
    ) {
        Button(onClick = open) {
            Text(text = LanguageMap[currentLanguage]!!.openDirectoryText)
        }
        
        // Scrollable directory tree
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            currentDirectory?.let {
                key(registerKey){
                    DirectoryTree(it, currentColorMode, onEntryClicked, isRoot = true)
                }
            }
        }
    }
}

@Composable
fun DirectoryTree(
    entry: DirectoryEntry, 
    currentColorMode: String,
    onEntryClicked: (DirectoryEntry) -> Unit, 
    padding: Int = 0,
    isRoot: Boolean = false
) {
    val isDirectory = !entry.isFile()
    
    // State for folder expansion - root folder expanded by default
    var isExpanded by remember { mutableStateOf(isRoot) }
    
    Column(modifier = Modifier.padding(start = padding.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable {
                    if (isDirectory) {
                        isExpanded = !isExpanded
                    } else {
                        onEntryClicked.invoke(entry)
                    }
                }
                .padding(vertical = 4.dp)
        ) {
            // Folder/File indicator
            if (isDirectory) {
                Text(
                    text = if (isExpanded) "▼" else "▶",
                    color = ColorGroups[currentColorMode]!!.textColor,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                // Spacer for files to align with folders
                Text(
                    text = "  ",
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Entry name
            Text(
                text = entry.name(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                color = ColorGroups[currentColorMode]!!.textColor
            )
        }

        // Animated children (only for directories)
        if (isDirectory) {
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    entry.subEntries().forEach { subEntry ->
                        DirectoryTree(
                            subEntry, 
                            currentColorMode, 
                            onEntryClicked, 
                            padding + DirectoryTreePaddingIncrement
                        )
                    }
                }
            }
        }
    }
}