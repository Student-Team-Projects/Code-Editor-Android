package com.example.codeeditor.viewmodels

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class DirectoryEntry(private val df: DocumentFile) {
    fun name(): String {
        return df.name ?: "error_no_name"
    }

    fun subEntries(): List<DirectoryEntry> {
        return df.listFiles().mapNotNull { df -> DirectoryEntry((df)) }
    }

    fun isFile(): Boolean {
        return df.isFile
    }

    fun uri(): Uri {
        return df.uri
    }
}

class DirectoryTreeVM(private val fileProvider: Function1<Uri, DocumentFile?>): ViewModel() {
    val currentEntry: MutableStateFlow<DirectoryEntry?> = MutableStateFlow(null)
    private val observers = mutableListOf<() -> Unit>()
    fun directoryEntry(uri: Uri):Result<DirectoryEntry> {
        return fileProvider.invoke(uri)?.let {
            Result.success(DirectoryEntry(it))
        } ?: Result.failure(NullPointerException("Directory entry not found: $uri"))
    }
    fun register(f: () -> Unit){
        observers.add(f)
    }
    fun update(){
        for(f in observers){
            f.invoke()
        }
    }
}



