package com.example.codeeditor.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.codeeditor.constants.*
import com.example.codeeditor.viewmodels.CodeVM
import com.example.codeeditor.viewmodels.DirectoryEntry
import com.example.codeeditor.viewmodels.DirectoryTreeVM

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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DirectoryTreeMenu(openDirectory, directoryVM, onEntryClicked)
        }
    ) {
        ScreenLayout(codeVM, openFile, save, saveAs)
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