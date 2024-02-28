package com.example.codeeditor;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.Map;
import java.util.Objects;

public class OpenFileController {

    public static void openFileInitialization(MainActivity mainScreen) {
        mainScreen.findViewById(R.id.OpenFileButtonSubmit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fileName = OpenFileController.getEnteredText(mainScreen);
                if (!FilesController.isFileExist(fileName, mainScreen)){
                    OpenFileController.setError("File does not exist", mainScreen);
                    return;
                }
                try {
                    mainScreen.setCurrentFileName(fileName);
                } catch (Exception e) {
                    OpenFileController.setError(e.getMessage(), mainScreen);
                    return;
                }
                OpenFileController.setDisabled(mainScreen);
                mainScreen.enableMainLayout();
            }
        });

        mainScreen.findViewById(R.id.OpenFileClose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenFileController.setDisabled(mainScreen);
                mainScreen.enableMainLayout();
            }
        });
    }

    public static void setDisabled(MainActivity mainScreen){
        mainScreen.findViewById(R.id.OpenFileLayout).setEnabled(false);
        mainScreen.findViewById(R.id.OpenFileLayout).setVisibility(View.INVISIBLE);

        mainScreen.findViewById(R.id.OpenFileEnterBackground).setEnabled(false);
        mainScreen.findViewById(R.id.OpenFileEnterBackground).setVisibility(View.INVISIBLE);

        mainScreen.findViewById(R.id.OpenFileEnterField).setEnabled(false);
        mainScreen.findViewById(R.id.OpenFileEnterField).setVisibility(View.INVISIBLE);

        mainScreen.findViewById(R.id.OpenFileButtonSubmit).setEnabled(false);
        mainScreen.findViewById(R.id.OpenFileButtonSubmit).setVisibility(View.INVISIBLE);

        mainScreen.findViewById(R.id.OpenFileError).setVisibility(View.INVISIBLE);
        mainScreen.findViewById(R.id.OpenFileTitle).setVisibility(View.INVISIBLE);

        mainScreen.findViewById(R.id.OpenFileClose).setEnabled(false);
        mainScreen.findViewById(R.id.OpenFileClose).setVisibility(View.INVISIBLE);

        mainScreen.findViewById(R.id.OpenFileScroll).setEnabled(false);
        mainScreen.findViewById(R.id.OpenFileScroll).setVisibility(View.INVISIBLE);
        ViewGroup layout = mainScreen.findViewById(R.id.OpenFileScrollLayout);
        layout.removeAllViews();

        mainScreen.findViewById(R.id.OpenFileScrollLayout).setEnabled(false);
        mainScreen.findViewById(R.id.OpenFileScrollLayout).setVisibility(View.INVISIBLE);
    }

    public static void setEnabled(MainActivity mainScreen){
        mainScreen.findViewById(R.id.OpenFileLayout).setEnabled(true);
        mainScreen.findViewById(R.id.OpenFileLayout).setVisibility(View.VISIBLE);

        mainScreen.findViewById(R.id.OpenFileEnterBackground).setEnabled(true);
        mainScreen.findViewById(R.id.OpenFileEnterBackground).setVisibility(View.VISIBLE);

        mainScreen.findViewById(R.id.OpenFileEnterField).setEnabled(true);
        mainScreen.findViewById(R.id.OpenFileEnterField).setVisibility(View.VISIBLE);

        mainScreen.findViewById(R.id.OpenFileButtonSubmit).setEnabled(true);
        mainScreen.findViewById(R.id.OpenFileButtonSubmit).setVisibility(View.VISIBLE);

        mainScreen.findViewById(R.id.OpenFileError).setVisibility(View.VISIBLE);
        mainScreen.findViewById(R.id.OpenFileTitle).setVisibility(View.VISIBLE);

        mainScreen.findViewById(R.id.OpenFileClose).setEnabled(true);
        mainScreen.findViewById(R.id.OpenFileClose).setVisibility(View.VISIBLE);

        mainScreen.findViewById(R.id.OpenFileScroll).setEnabled(true);
        mainScreen.findViewById(R.id.OpenFileScroll).setVisibility(View.VISIBLE);

        mainScreen.findViewById(R.id.OpenFileScrollLayout).setEnabled(true);
        mainScreen.findViewById(R.id.OpenFileScrollLayout).setVisibility(View.VISIBLE);
        scrollerRestart(mainScreen);
    }

    public static void setError(String error, MainActivity mainScreen) {
        TextView errorText = mainScreen.findViewById(R.id.OpenFileError);
        errorText.setText(error);
    }

    public static String getEnteredText(MainActivity mainScreen){
        TextInputEditText textInputer = mainScreen.findViewById(R.id.OpenFileEnterField);
        return Objects.requireNonNull(textInputer.getText()).toString();
    }

    public static void scrollerRestart(MainActivity mainScreen){
        LinearLayout scroller = mainScreen.findViewById(R.id.OpenFileScrollLayout);
        scroller.removeAllViews();
        Map<String, ?> files = FilesController.getAllLocalFiles(mainScreen);
        for (Map.Entry<String,?> entry : files.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            scroller.addView(createFileButton(key, mainScreen));
        }
    }

    private static Button createFileButton(String key, MainActivity mainScreen){
        Button button = new Button(mainScreen);
        button.setText("/" + key);
        button.setBackground(mainScreen.getDrawable(R.drawable.transperent));
        button.setTextColor(mainScreen.getColor(R.color.suggestion_yellow));
        button.setAllCaps(false);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextInputEditText input = mainScreen.findViewById(R.id.OpenFileEnterField);
                input.setText(key);
            }
        });
        return button;
    }
}
