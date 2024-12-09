package com.example.codeeditor.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.codeeditor.constants.*
import com.example.codeeditor.viewmodels.CodeVM
import com.example.codeeditor.viewmodels.DirectoryEntry
import com.example.codeeditor.viewmodels.DirectoryTreeVM

private const val dragSpeed = 2000f
private const val minDirectoryFraction = epsilon
private const val normalDirectoryFraction = 0.2f
private const val minCodeFraction = 0.35f

@Composable
fun MainScreen(
    codeVM: CodeVM,
    openFile: () -> Unit,
    save: () -> Unit,
    saveAs: () -> Unit,
    openDirectory: () -> Unit,
    directoryVM: DirectoryTreeVM,
    onEntryClicked: (DirectoryEntry) -> Unit
) {
    var directoryTreeWidthFraction by remember { mutableFloatStateOf(0.3f) }

    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(directoryTreeWidthFraction)
        ) {
            DirectoryTreeMenu(openDirectory, directoryVM, onEntryClicked)
        }

        // slider to manage code and directory ratio
        Box(
            modifier = Modifier
                .width(5.dp)
                .fillMaxHeight()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        directoryTreeWidthFraction = (directoryTreeWidthFraction + dragAmount / dragSpeed).coerceIn(
                            minDirectoryFraction, 1f- minCodeFraction)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            directoryTreeWidthFraction = normalDirectoryFraction
                        }
                    )
                }
                .background(Color.Gray)
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f-directoryTreeWidthFraction)
        ) {
            ScreenLayout(codeVM, openFile, save, saveAs)
        }
    }
}

@Composable
fun ScreenLayout(codeVM: CodeVM, open: () -> Unit, save:() -> Unit, saveAs: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CodeArea(modifier= Modifier.weight(1.0f), codeVM)
        Spacer(modifier = Modifier.height(ButtonTextFieldSpacing))
        ButtonRow(codeVM, open, save, saveAs)
    }
}

@Composable
fun ButtonRow(codeVM: CodeVM, open: ()->Unit, save:() -> Unit, saveAs: () -> Unit) {
    var isMenuVisible by remember { mutableStateOf(false) }

    val targetHeight = if (isMenuVisible) 100.dp else 30.dp

    Column(
        modifier = Modifier
            .height(targetHeight)
            .background(BackgroundColor),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MenuToggleButton(isMenuVisible = isMenuVisible) {
            isMenuVisible = !isMenuVisible
        }

        if (isMenuVisible) {
            MenuActions(open, save, saveAs)
        }
    }
}

@Composable
private fun MenuToggleButton(isMenuVisible: Boolean, onToggle: () -> Unit) {
    Button(onClick = onToggle) {
        Text(
            text = if (isMenuVisible) "Hide Menu" else "Show Menu",
            fontSize = if (isMenuVisible) 14.sp else 10.sp
        )
    }
}

@Composable
private fun MenuActions(open: () -> Unit, save: () -> Unit, saveAs: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ButtonRowPadding),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(onClick = open) { Text("Open") }
        Button(onClick = save) { Text("Save") }
        Button(onClick = saveAs) { Text("Save as") }
    }
}