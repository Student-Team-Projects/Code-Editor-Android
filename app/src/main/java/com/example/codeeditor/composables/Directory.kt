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
import com.example.codeeditor.constants.BackgroundColor
import com.example.codeeditor.constants.DirectoryTreePaddingIncrement
import com.example.codeeditor.viewmodels.DirectoryEntry
import com.example.codeeditor.viewmodels.DirectoryTreeVM
import kotlinx.coroutines.isActive
import java.nio.file.FileSystems
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds

@Composable
fun DirectoryTreeMenu(open: () -> Unit, directoryVM: DirectoryTreeVM, onEntryClicked:(DirectoryEntry) -> Unit) {
    val currentDirectory: DirectoryEntry? by directoryVM.currentEntry.collectAsState()
    var registerKey by remember { mutableStateOf(0) }
    directoryVM.register { registerKey++ }
    Column(modifier = Modifier
        .fillMaxSize()
        .background(BackgroundColor)
        .padding(8.dp)
    ) {
        Button(onClick = open) {
            Text(text = "Open directory")
        }
        currentDirectory?.let {
            key(registerKey){
                DirectoryTree(it, onEntryClicked)
            }
        }
    }
}

@Composable
fun DirectoryTree(entry: DirectoryEntry, onEntryClicked:(DirectoryEntry) -> Unit, padding: Int = 0) {
    Column(modifier = Modifier.padding(start = padding.dp)) {
        Text(text = entry.name(), style = MaterialTheme.typography.bodyMedium, color = Color.Black,
            modifier = Modifier.clickable {
                onEntryClicked.invoke(entry)
            })

        entry.subEntries().forEach { subEntry ->
            DirectoryTree(subEntry, onEntryClicked, padding+ DirectoryTreePaddingIncrement)
        }
    }
}