package com.example.codeeditor.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.codeeditor.constants.DirectoryTreePaddingIncrement
import com.example.codeeditor.viewmodels.DirectoryEntry
import com.example.codeeditor.viewmodels.DirectoryTreeVM
import com.example.codeeditor.constants.ColorGroups
import com.example.codeeditor.constants.LanguageMap
import kotlinx.coroutines.isActive
import java.nio.file.FileSystems
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds

@Composable
fun DirectoryTreeMenu(open: () -> Unit, currentColorMode: String, currentLanguage: String,
                      directoryVM: DirectoryTreeVM, onEntryClicked:(DirectoryEntry) -> Unit) {
    val currentDirectory: DirectoryEntry? by directoryVM.currentEntry.collectAsState()
    var registerKey by remember { mutableStateOf(0) }
    directoryVM.register { registerKey++ }
    Column(modifier = Modifier
        .fillMaxSize()
        .background(ColorGroups[currentColorMode]!!.backgroundColor)
        .padding(8.dp)
    ) {
        Button(onClick = open) {
            Text(text = LanguageMap[currentLanguage]!!.openDirectoryText)
        }
        currentDirectory?.let {
            key(registerKey){
                DirectoryTree(it, currentColorMode, onEntryClicked)
            }
        }
    }
}

@Composable
fun DirectoryTree(entry: DirectoryEntry, currentColorMode: String,onEntryClicked:(DirectoryEntry) -> Unit, padding: Int = 0) {
    Column(modifier = Modifier.padding(start = padding.dp)) {
        Text(text = entry.name(), style = MaterialTheme.typography.bodyMedium,
            color = ColorGroups[currentColorMode]!!.textColor,
            modifier = Modifier.clickable {
                onEntryClicked.invoke(entry)
            })

        entry.subEntries().forEach { subEntry ->
            DirectoryTree(subEntry, currentColorMode, onEntryClicked, padding+ DirectoryTreePaddingIncrement)
        }
    }
}