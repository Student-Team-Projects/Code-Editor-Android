package com.example.codeeditor.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class CodeVM : ViewModel() {
    private val _text = mutableStateOf("// Welcome to Code Editor!\n// Open a file to start editing.")
    val text: State<String> = _text
    fun updateText(newText: String) {
        _text.value = newText
    }
}