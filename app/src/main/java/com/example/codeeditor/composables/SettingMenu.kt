package com.example.codeeditor.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.example.codeeditor.constants.ButtonRowPadding
import com.example.codeeditor.constants.ColorGroups
import com.example.codeeditor.constants.LanguageMap

/**
    Function setting layout of setting menu.
    @param autosaveAction: function triggered when autosave mode is changed
    @param currentColorMode: String setting current color mode of app
    @param autosaveState: Boolean setting current autosave state
    @param back: function triggering going back to main menu
 */

@Composable
fun SettingMenu(autosaveAction: () -> Unit, currentColorMode: String,
                        currentLanguage: String,
                        autosaveState: Boolean, languageMenu: () -> Unit, back: () -> Unit,
                        modeChange: () -> Unit){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ButtonRowPadding),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = LanguageMap[currentLanguage]!!.autosaveText, fontSize = 20.sp,
            color = ColorGroups[currentColorMode]!!.textColor
        )
        Button(onClick = autosaveAction) {
            Text(
                if (autosaveState) LanguageMap[currentLanguage]!!.switchOffText else
                    LanguageMap[currentLanguage]!!.switchOnText
            )
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ButtonRowPadding),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = LanguageMap[currentLanguage]!!.darkmodeText,
            fontSize = 20.sp, color = ColorGroups[currentColorMode]!!.textColor
        )
        Button(onClick = modeChange) {
            Text(
                if (currentColorMode == "Normal") LanguageMap[currentLanguage]!!.switchOnText else
                    LanguageMap[currentLanguage]!!.switchOffText
            )
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ButtonRowPadding),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = languageMenu) {
            Text(LanguageMap[currentLanguage]!!.changeLanguageText)
        }
        Button(onClick = back) {
            Text(LanguageMap[currentLanguage]!!.returnText)
        }
    }
}