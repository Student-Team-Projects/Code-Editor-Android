package com.example.codeeditor.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class CodeVM : ViewModel() {
    private val _text = mutableStateOf("")
    val text: State<String> = _text
    fun updateText(newText: String) {
        _text.value = newText
    }
}