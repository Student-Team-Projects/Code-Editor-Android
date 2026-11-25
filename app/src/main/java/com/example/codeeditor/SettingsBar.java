package com.example.codeeditor;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.webkit.WebView;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.codeeditor.ui.theme.ThemeManager;

/**
 * SettingsBar is an inheritance class of MiddleBar.
 * Consists of two buttons: "switch theme" and "file explorer".
 *
 * @param <T> generic content parameter
 */
public class SettingsBar<T> extends MiddleBar<T> {

    private Button btnSwitchTheme;
    private Button btnFileExplorer;

    public SettingsBar(@NonNull Context context) {
        super(context);
    }

    public SettingsBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SettingsBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init(Context context) {
        inflate(context, R.layout.settings_bar, this);
        btnSwitchTheme = findViewById(R.id.btnSwitchTheme);
        btnFileExplorer = findViewById(R.id.btnFileExplorer);

        if (btnSwitchTheme != null) {
            btnSwitchTheme.setOnClickListener(v -> {
                if (context instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) context;
                    String newTheme = ThemeManager.INSTANCE.toggleTheme(mainActivity);
                    boolean isDark = ThemeManager.INSTANCE.isDark(newTheme);
                    WebView codeView = mainActivity.findViewById(R.id.codeView);
                    if (codeView != null) {
                        String js = "if (window.editor) { monaco.editor.setTheme('" + (isDark ? "vs-dark" : "vs") + "'); }";
                        codeView.evaluateJavascript(js, null);
                    }
                    mainActivity.updateThemeColors();
                } else {
                    ThemeManager.INSTANCE.toggleTheme(context);
                }
            });
        }

        if (btnFileExplorer != null) {
            btnFileExplorer.setOnClickListener(v -> {
                Intent intent = new Intent(context, InternalStorageActivity.class);
                context.startActivity(intent);
            });
        }
    }
}
