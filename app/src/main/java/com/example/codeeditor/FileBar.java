package com.example.codeeditor;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * FileBar is an inheritance class of MiddleBar containing file-related operations.
 *
 * @param <T> generic content parameter
 */
public class FileBar<T> extends MiddleBar<T> {

    public FileBar(@NonNull Context context) {
        super(context);
    }

    public FileBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FileBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init(Context context) {
        inflate(context, R.layout.file_bar, this);

        Button btnNewFile = findViewById(R.id.btnNewFile);
        Button btnOpenFile = findViewById(R.id.btnOpenFile);
        Button btnOpenFolder = findViewById(R.id.btnOpenFolder);
        Button btnSaveFile = findViewById(R.id.btnSaveFile);
        Button btnCloseFile = findViewById(R.id.btnCloseFile);
        Button btnCloseFolder = findViewById(R.id.btnCloseFolder);

        if (context instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) context;

            if (btnNewFile != null) {
                btnNewFile.setOnClickListener(v -> {
                    mainActivity.disableMainLayout();
                    NewFileController.setEnabled(mainActivity);
                });
            }

            if (btnOpenFile != null) {
                btnOpenFile.setOnClickListener(v -> mainActivity.openFilePicker());
            }

            if (btnOpenFolder != null) {
                btnOpenFolder.setOnClickListener(v -> mainActivity.openDirectoryPicker());
            }

            if (btnSaveFile != null) {
                btnSaveFile.setOnClickListener(v -> mainActivity.saveOpenFile(true));
            }

            if (btnCloseFile != null) {
                btnCloseFile.setOnClickListener(v -> {
                    try {
                        mainActivity.setCurrentFileName(null);
                    } catch (Exception ignored) {}
                });
            }

            if (btnCloseFolder != null) {
                btnCloseFolder.setOnClickListener(v -> {
                    try {
                        mainActivity.setCurrentProjectPath(null);
                    } catch (Exception ignored) {}
                });
            }
        }
    }
}
