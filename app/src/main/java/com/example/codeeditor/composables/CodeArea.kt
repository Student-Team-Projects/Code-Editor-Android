package com.example.codeeditor.composables

import android.annotation.SuppressLint
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.codeeditor.constants.BorderThickness
import com.example.codeeditor.constants.ButtonRowPadding
import com.example.codeeditor.constants.ColorGroups
import com.example.codeeditor.constants.CornerRadius
import com.example.codeeditor.viewmodels.CodeVM

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun CodeArea(
    modifier: Modifier,
    currentColorMode: String,
    currentLanguage: String,
    codeVM: CodeVM,
    fileExtension: String = ""
) {
    var isEditorReady by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    val clipboardManager = LocalClipboardManager.current

    val currentCode = codeVM.text.value

    LaunchedEffect(currentCode, isEditorReady, fileExtension) {
        if (isEditorReady && webViewInstance != null) {
            val encodedCode = Base64.encodeToString(currentCode.toByteArray(), Base64.NO_WRAP)
            webViewInstance?.evaluateJavascript("javascript:setEditorValueFromAndroid('$encodedCode', '$fileExtension');", null)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ButtonRowPadding)
            .clip(RoundedCornerShape(CornerRadius))
            .border(BorderThickness, ColorGroups[currentColorMode]!!.borderColor, RoundedCornerShape(CornerRadius))
            .background(Color(0xFF1E1E1E))
    ) {
        // Compact Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(Color(0xFF2D2D2D)),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val btnColors = ButtonDefaults.textButtonColors(contentColor = Color.LightGray)
            val compactPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)

            TextButton(
                onClick = { webViewInstance?.evaluateJavascript("javascript:editorUndo();", null) },
                colors = btnColors,
                contentPadding = compactPadding
            ) { Text("Undo", fontSize = 12.sp) }

            TextButton(
                onClick = { webViewInstance?.evaluateJavascript("javascript:editorRedo();", null) },
                colors = btnColors,
                contentPadding = compactPadding
            ) { Text("Redo", fontSize = 12.sp) }

            TextButton(
                onClick = { webViewInstance?.evaluateJavascript("javascript:editorSelectAll();", null) },
                colors = btnColors,
                contentPadding = compactPadding
            ) { Text("Select All", fontSize = 12.sp) }

            TextButton(
                onClick = { webViewInstance?.evaluateJavascript("javascript:editorCopy();", null) },
                colors = btnColors,
                contentPadding = compactPadding
            ) { Text("Copy", fontSize = 12.sp) }

            TextButton(
                onClick = {
                    val text = clipboardManager.getText()?.text ?: ""
                    val encoded = Base64.encodeToString(text.toByteArray(), Base64.NO_WRAP)
                    webViewInstance?.evaluateJavascript("javascript:editorPaste('$encoded');", null)
                },
                colors = btnColors,
                contentPadding = compactPadding
            ) { Text("Paste", fontSize = 12.sp) }
        }

        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    isFocusable = true
                    isFocusableInTouchMode = true

                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    @Suppress("DEPRECATION")
                    settings.allowFileAccessFromFileURLs = true
                    @Suppress("DEPRECATION")
                    settings.allowUniversalAccessFromFileURLs = true

                    setOnTouchListener { v, event ->
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            if (!v.hasFocus()) {
                                v.requestFocus()
                            }
                        }
                        false
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            Log.d("MonacoJS", "${consoleMessage?.message()} -- Line ${consoleMessage?.lineNumber()}")
                            return true
                        }
                    }

                    webViewClient = WebViewClient()

                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onEditorReady() {
                            post {
                                isEditorReady = true
                                val encodedCode = Base64.encodeToString(codeVM.text.value.toByteArray(), Base64.NO_WRAP)
                                evaluateJavascript("javascript:setEditorValueFromAndroid('$encodedCode', '$fileExtension');", null)
                            }
                        }

                        @JavascriptInterface
                        fun onTextChanged(currentText: String) {
                            post {
                                if (currentText != codeVM.text.value) {
                                    codeVM.updateText(currentText)
                                }
                            }
                        }

                        @JavascriptInterface
                        fun onCopyRequested(text: String) {
                            post {
                                clipboardManager.setText(AnnotatedString(text))
                            }
                        }
                    }, "AndroidBridge")

                    loadUrl("file:///android_asset/index.html")
                    webViewInstance = this
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}