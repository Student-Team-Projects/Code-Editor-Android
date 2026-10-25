package com.example.codeeditor;

import android.annotation.SuppressLint;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class CodeEditorController {

    @SuppressLint("SetJavaScriptEnabled")
    public static WebView setupCodeEditor(MainActivity mainScreen) {
        WebView codeEditor = mainScreen.findViewById(R.id.codeView);

        WebSettings settings = codeEditor.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);

        codeEditor.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("MonacoJS", consoleMessage.message() + " -- Line "
                        + consoleMessage.lineNumber() + " of "
                        + consoleMessage.sourceId());
                return true;
            }
        });

        codeEditor.setWebViewClient(new WebViewClient());

        // Tworzymy most Java-JavaScript. Metody w EditorBridge mogą być wywoływane z HTML-a.
        codeEditor.addJavascriptInterface(new EditorBridge(mainScreen), "AndroidBridge");

        // Wczytanie lokalnego pliku z katalogu assets
        codeEditor.loadUrl("file:///android_asset/index.html");

        return codeEditor;
    }

    public static void DisableCodeEditor(MainActivity mainScreen) {
        WebView codePanel = mainScreen.findViewById(R.id.codeView);
        if (codePanel != null) {
            codePanel.setEnabled(true);
            codePanel.setVisibility(View.VISIBLE);
        }
        setCode("// Otwórz plik z menu File -> Open File lub utwórz nowy", mainScreen);
    }

    public static void EnableCodeEditor(MainActivity mainScreen) {
        WebView codePanel = mainScreen.findViewById(R.id.codeView);
        if (codePanel != null) {
            codePanel.setEnabled(true);
            codePanel.setVisibility(View.VISIBLE);
        }
    }

    public static void setCode(String code, MainActivity mainScreen) {
        WebView codeEditor = mainScreen.findViewById(R.id.codeView);
        if (codeEditor == null) return;

        String fileName = mainScreen.getCurrentFileName();
        String extension = (fileName != null && fileName.contains("."))
                ? fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase()
                : "";

        String encodedCode = Base64.encodeToString(code.getBytes(), Base64.NO_WRAP);

        String jsCommand = "javascript:setEditorValueFromAndroid('" + encodedCode + "', '" + extension + "');";
        codeEditor.evaluateJavascript(jsCommand, null);
    }

    // acquiring code is async
    public static void getCode(MainActivity mainScreen, ValueCallback<String> callback) {
        WebView codeEditor = mainScreen.findViewById(R.id.codeView);
        if (codeEditor == null) {
            callback.onReceiveValue("");
            return;
        }

        codeEditor.evaluateJavascript("window.editor ? window.editor.getValue() : '';", value -> {
            if (value != null && value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                String unescaped = value.substring(1, value.length() - 1)
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"")
                        .replace("\\'", "'")
                        .replace("\\\\", "\\");
                callback.onReceiveValue(unescaped);
            } else {
                callback.onReceiveValue(value != null ? value : "");
            }
        });
    }

    // listener
    public static class EditorBridge {
        private final MainActivity mainScreen;

        public EditorBridge(MainActivity mainScreen) {
            this.mainScreen = mainScreen;
        }

        @JavascriptInterface
        public void onEditorReady() {
            mainScreen.runOnUiThread(() -> {
                String currentFile = mainScreen.getCurrentFileName();
                if (currentFile != null) {
                    try {
                        CodeEditorController.setCode(FilesController.getFilesInside(currentFile, mainScreen), mainScreen);
                    } catch (Exception e) {
                        Log.e("CodeEditorController", "Error setting code on ready", e);
                    }
                } else {
                    CodeEditorController.setCode("// Open file from menu File -> Open File or create new", mainScreen);
                }
            });
        }

        @JavascriptInterface
        public void onTextChanged(String currentText) {
        }
    }
}