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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
            // Closing a file while the terminal is shown shouldn't bring the editor back.
            if (!mainScreen.isTerminalShown()) codePanel.setVisibility(View.VISIBLE);
        }
        setCode("// Open a file from menu File -> Open File or create new", mainScreen);
    }

    public static void EnableCodeEditor(MainActivity mainScreen) {
        WebView codePanel = mainScreen.findViewById(R.id.codeView);
        if (codePanel != null) {
            codePanel.setEnabled(true);
            // Opening a file switches back from the terminal.
            mainScreen.showEditor();
            codePanel.requestFocus();
        }
    }

    public static void setCode(String code, MainActivity mainScreen) {
        WebView codeEditor = mainScreen.findViewById(R.id.codeView);
        if (codeEditor == null) return;

        String fileName = mainScreen.getCurrentFileName();
        String extension = (fileName != null && fileName.contains("."))
                ? fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase()
                : "";

        // A JSON string literal is a valid JS string literal.
        String jsCommand = "setEditorValueFromAndroid(" + JSONObject.quote(code) + ", " + JSONObject.quote(extension) + ");";
        codeEditor.evaluateJavascript(jsCommand, null);
    }

    /** Passes the editor content to the callback (asynchronously), or null if it can't be read. */
    public static void getCode(MainActivity mainScreen, ValueCallback<String> callback) {
        WebView codeEditor = mainScreen.findViewById(R.id.codeView);
        if (codeEditor == null) {
            callback.onReceiveValue(null);
            return;
        }

        codeEditor.evaluateJavascript("window.editor ? window.editor.getValue() : null;",
                json -> callback.onReceiveValue(decodeJsonString(json)));
    }

    /** evaluateJavascript hands results back JSON-encoded. */
    private static String decodeJsonString(String json) {
        if (json == null || json.equals("null")) return null;
        try {
            return new JSONArray("[" + json + "]").getString(0);
        } catch (JSONException e) {
            Log.e("CodeEditorController", "Unexpected editor value: " + json, e);
            return null;
        }
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
                String content = mainScreen.getSavedContent();
                CodeEditorController.setCode(content != null ? content
                        : "// Open file from menu File -> Open File or create new", mainScreen);
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
