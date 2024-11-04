package com.example.codeeditor.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class CodeVM : ViewModel() {
    val text = MutableStateFlow("// your code here")
    init {
        observeTextChanges()
    }
    private fun observeTextChanges() {
        viewModelScope.launch {
            text.collect { newText ->
                println("Text changed to: $newText")
            }
        }
    }
}