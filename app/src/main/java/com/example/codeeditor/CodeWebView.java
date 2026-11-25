package com.example.codeeditor;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.webkit.WebView;

/**
 * The WebView hosting Monaco. Undo and redo shortcuts are taken before the soft keyboard sees
 * them: Gboard handles Ctrl+Z as its own undo of the text it committed, which reaches the page
 * only as a backspace, so Monaco would never undo.
 */
public class CodeWebView extends WebView {

    public CodeWebView(Context context) {
        super(context);
    }

    public CodeWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CodeWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent event) {
        String command = editorCommandFor(event);
        if (command == null) return super.dispatchKeyEventPreIme(event);
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            evaluateJavascript(command, null);
        }
        return true;
    }

    private static String editorCommandFor(KeyEvent event) {
        if (!event.isCtrlPressed() || event.isAltPressed() || event.isMetaPressed()) return null;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_Z:
                return event.isShiftPressed() ? "editorRedo();" : "editorUndo();";
            case KeyEvent.KEYCODE_Y:
                return event.isShiftPressed() ? null : "editorRedo();";
            default:
                return null;
        }
    }
}
