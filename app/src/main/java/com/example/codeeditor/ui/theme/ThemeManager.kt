package com.example.codeeditor.ui.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_COLOR_MODE = "color_mode"

    fun getColorMode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_COLOR_MODE, "Dark") ?: "Dark"
    }

    fun setColorMode(context: Context, mode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_COLOR_MODE, mode).apply()
        applyTheme(mode)
    }

    fun initTheme(context: Context) {
        val mode = getColorMode(context)
        applyTheme(mode)
    }

    fun isDark(context: Context): Boolean {
        return getColorMode(context) == "Dark"
    }

    fun isDark(mode: String): Boolean {
        return mode == "Dark"
    }

    fun toggleTheme(context: Context): String {
        val currentMode = getColorMode(context)
        val newMode = if (currentMode == "Normal") "Dark" else "Normal"
        setColorMode(context, newMode)
        return newMode
    }

    private fun applyTheme(mode: String) {
        val nightMode = if (mode == "Dark") {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}
