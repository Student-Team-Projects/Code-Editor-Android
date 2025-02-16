package com.example.codeeditor.constants

import androidx.compose.material3.ButtonColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class ColorGroup(
    val backgroundColor: Color,
    val codeAreaBackgroundColor: Color,
    val borderColor: Color,
    val buttonColor: Color,
    val textColor: Color,
    val placeholderTextColor: Color
)

val ColorGroups = mapOf(
    "Normal" to ColorGroup(
        backgroundColor = Color.White,
        codeAreaBackgroundColor = Color(0xFFFFF9C4),
        borderColor = Color.Gray,
        buttonColor = Color.Blue,
        textColor = Color.Black,
        placeholderTextColor = Color.Gray
    ),
    "Dark" to ColorGroup(
        backgroundColor = Color.DarkGray,
        codeAreaBackgroundColor = Color.Gray,
        borderColor = Color.Gray,
        buttonColor = Color(0xFF4B0082),
        textColor = Color.White,
        placeholderTextColor = Color.Yellow
    )
)

public val ButtonRowPadding = 8.dp
public val ButtonTextFieldSpacing = 0.dp
public val BorderThickness = 1.dp
public val CornerRadius = 8.dp
public const val DirectoryTreePaddingIncrement = 8
public val searchingExtensions = arrayOf("text/plain", "text/x-c", "text/x-java-source", "text/x-python")
public const val saveAsDefaultExtension = "text/plain"
public const val epsilon = 10e-6f