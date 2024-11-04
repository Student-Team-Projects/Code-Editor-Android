package com.example.codeeditor.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

val EmptyDirectory = File(Uri.EMPTY)
open class DirectoryEntry(val name: Uri, val subEntries: List<DirectoryEntry>)
class File(name: Uri) : DirectoryEntry(name, emptyList())

class DirectoryTreeVM: ViewModel() {
    val currentEntry: MutableStateFlow<DirectoryEntry> = MutableStateFlow(EmptyDirectory)
}


