package com.example.codeeditor.composables

import android.app.AlertDialog
import android.content.Context

/**
    function building and showing dialog window with two buttons:
    @param context: Determines in which context window is shown
    @param message: String which is shown in window
    @param positiveButtonText: String which is shown on button triggering positiveAction
    @param negativeButtonText: String which is shown on button triggering negativeAction
    @param positiveAction: Function triggered when button with positiveButtonText is clicked
    @param negativeAction: Function triggered when button with negativeButtonText is clicked
 */
fun showDialogWithTwoButtons(context: Context, message: String,
                                     positiveButtonText: String, negativeButtonText: String,
                                     positiveAction: () -> Unit, negativeAction: () -> Unit) {
    AlertDialog.Builder(context)
        .setMessage(message)
        .setPositiveButton(positiveButtonText) { dialog, _ ->
            positiveAction()
            dialog.dismiss()
        }
        .setNegativeButton(negativeButtonText) { dialog, _ ->
            negativeAction()
            dialog.dismiss()
        }
        .show()
}