package com.example.codeeditor;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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

    @SuppressWarnings("deprecation")
    @SuppressLint("SetJavaScriptEnabled")
    public static WebView setupCodeEditor(MainActivity mainScreen) {
        WebView codeEditor = mainScreen.findViewById(R.id.codeView);

        codeEditor.setFocusable(true);
        codeEditor.setFocusableInTouchMode(true);
        codeEditor.requestFocus();

        WebSettings settings = codeEditor.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setSupportZoom(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        codeEditor.setLayerType(View.LAYER_TYPE_HARDWARE, null);

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

        // Java-JavaScript bridge. Methods in EditorBridge can be called from HTML/JS.
        codeEditor.addJavascriptInterface(new EditorBridge(mainScreen), "AndroidBridge");

        // Load local HTML from assets
        codeEditor.loadUrl("file:///android_asset/index.html");

        return codeEditor;
    }

    public static void DisableCodeEditor(MainActivity mainScreen) {
        WebView codePanel = mainScreen.findViewById(R.id.codeView);
        if (codePanel != null) {
            codePanel.setEnabled(true);
            codePanel.setVisibility(View.VISIBLE);
        }
        setCode("// Open a file from menu File -> Open File or create new", mainScreen);
    }

    public static void EnableCodeEditor(MainActivity mainScreen) {
        WebView codePanel = mainScreen.findViewById(R.id.codeView);
        if (codePanel != null) {
            codePanel.setEnabled(true);
            codePanel.setVisibility(View.VISIBLE);
            codePanel.requestFocus();
        }
    }

    private static String escapeJs(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("'", "\\'")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    public static void setCode(String code, MainActivity mainScreen) {
        WebView codeEditor = mainScreen.findViewById(R.id.codeView);
        if (codeEditor == null) return;

        String fileName = mainScreen.getCurrentFileName();
        String extension = (fileName != null && fileName.contains("."))
                ? fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase()
                : "";

        String escapedCode = escapeJs(code);

        String jsCommand = "javascript:setEditorValueFromAndroid(\"" + escapedCode + "\", \"" + extension + "\");";
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
            mainScreen.runOnUiThread(() -> {
                // Keep track of text changes if needed
            });
        }

        @JavascriptInterface
        public String getClipboardText() {
            try {
                ClipboardManager clipboard = (ClipboardManager) mainScreen.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null && clipboard.getPrimaryClip().getItemCount() > 0) {
                    CharSequence text = clipboard.getPrimaryClip().getItemAt(0).getText();
                    return text != null ? text.toString() : "";
                }
            } catch (Exception e) {
                Log.e("EditorBridge", "Error reading clipboard", e);
            }
            return "";
        }

        @JavascriptInterface
        public void setClipboardText(String text) {
            mainScreen.runOnUiThread(() -> {
                try {
                    ClipboardManager clipboard = (ClipboardManager) mainScreen.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        ClipData clip = ClipData.newPlainText("Copied Text", text);
                        clipboard.setPrimaryClip(clip);
                    }
                } catch (Exception e) {
                    Log.e("EditorBridge", "Error writing clipboard", e);
                }
            });
        }
    }
}
