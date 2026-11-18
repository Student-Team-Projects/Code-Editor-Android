package com.example.codeeditor.activities

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.codeeditor.termux.TermuxSession
import com.example.codeeditor.termux.ssh.SshConnectionScreen
import com.example.codeeditor.ui.theme.CodeEditorTheme
import com.example.codeeditor.ui.theme.ThemeManager

/**
    Sets up the app-wide [TermuxSession]: server settings, login key, setup help.
 */
class TermuxConnectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.initTheme(this)
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        setContent {
            CodeEditorTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // The only backend-specific screen.
                    SshConnectionScreen(TermuxSession.shared)
                }
            }
        }
    }
}
