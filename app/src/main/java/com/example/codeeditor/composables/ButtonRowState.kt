package com.example.codeeditor.composables

/**
    HIDE - ButtonRow is hidden, only toggle button visible
    SHOW - ButtonRow is shown
    SETTINGS - SettingMenu is shown
 */
enum class ButtonRowState {
    HIDE,
    SHOW,
    SETTINGS,
    LANGUAGE_SETTINGS
}