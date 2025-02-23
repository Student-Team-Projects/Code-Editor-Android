package com.example.codeeditor.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.codeeditor.constants.BorderThickness
import com.example.codeeditor.constants.ButtonRowPadding
import com.example.codeeditor.constants.CornerRadius
import com.example.codeeditor.constants.ColorGroups
import com.example.codeeditor.constants.LanguageMap
import com.example.codeeditor.viewmodels.CodeVM

@Composable
fun CodeArea(modifier: Modifier, currentColorMode: String, currentLanguage: String,
             codeVM: CodeVM) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(ButtonRowPadding)
            .border(BorderThickness, ColorGroups[currentColorMode]!!.borderColor, RoundedCornerShape(CornerRadius))
            .background(ColorGroups[currentColorMode]!!.codeAreaBackgroundColor)
    ) {
        TextField(
            value = codeVM.text.value,
            onValueChange = { newText: String -> codeVM.updateText(newText) },
            modifier = Modifier.fillMaxSize(),
            placeholder = {
                Text(text = LanguageMap[currentLanguage]!!.emptyCodeAreaText,
                    color= ColorGroups[currentColorMode]!!.placeholderTextColor)
            },
            colors = TextFieldDefaults.colors(
                focusedTextColor = ColorGroups[currentColorMode]!!.textColor,
                unfocusedTextColor = ColorGroups[currentColorMode]!!.textColor,
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent
            )
        )
    }
}