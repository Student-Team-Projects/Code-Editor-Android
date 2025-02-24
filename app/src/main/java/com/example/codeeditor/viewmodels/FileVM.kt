package com.example.codeeditor.viewmodels

import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class FileVM: ViewModel() {
    private val _fileUri = mutableStateOf(Uri.EMPTY)
    val fileUri: State<Uri> = _fileUri
    fun updateFileUri(newUri: Uri) {
        _fileUri.value = newUri
    }
}