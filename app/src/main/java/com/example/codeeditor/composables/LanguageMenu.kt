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
import com.example.codeeditor.activities.MainActivity
import com.example.codeeditor.constants.ButtonRowPadding
import com.example.codeeditor.constants.LanguageMap
import kotlin.collections.take
import kotlin.collections.toMutableSet
import kotlin.math.min

@Composable
fun LanguageMenu(currentLanguage: String, mainActivity: MainActivity, back: () -> Unit){
    var keySet = LanguageMap.keys.toMutableSet()
    while(!keySet.isEmpty()) LanguageRow(keySet, currentLanguage, mainActivity,back)
    Button(onClick = back) {
        Text(LanguageMap[currentLanguage]!!.returnText)
    }
}

@Composable
private fun LanguageRow(set: MutableSet<String>, currentLanguage: String,
                        mainActivity: MainActivity, back: () -> Unit){
    var currentSet = set.take(min(3, set.size))
    var i = 0
    while(i < currentSet.size){
        set.remove(currentSet[i])
        i++
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ButtonRowPadding),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (s in currentSet) {
            Button(onClick = {
                mainActivity.currentLanguage = s
                back.invoke()
            }) {
                Text(s)
            }
        }
    }
}