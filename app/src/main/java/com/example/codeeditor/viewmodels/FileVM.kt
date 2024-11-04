package com.example.codeeditor.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class FileVM: ViewModel() {
    val fileUri = MutableStateFlow<Uri>(Uri.EMPTY)
}