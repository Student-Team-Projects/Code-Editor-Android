package com.example.codeeditor;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public class OpenProjectController {

    public static void openProjectInitialization(MainActivity mainScreen) {
        mainScreen.findViewById(R.id.OpenProjectButtonSubmit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String filePath = OpenProjectController.getEnteredText(mainScreen);
                if (!FilesController.isDirectory(filePath, mainScreen)){

                    OpenProjectController.setError(filePath, mainScreen);
                    return;
                }
                try {
                    mainScreen.setCurrentProjectPath(filePath);
                } catch (Exception e) {
                    OpenProjectController.setError(e.getMessage(), mainScreen);
                    return;
                }
                OpenProjectController.setDisabled(mainScreen);
                mainScreen.enableMainLayout();
            }
        });

        mainScreen.findViewById(R.id.OpenProjectClose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenProjectController.setDisabled(mainScreen);
                mainScreen.enableMainLayout();
            }
        });
    }

    public static void setDisabled(MainActivity mainScreen){
        mainScreen.findViewById(R.id.OpenProjectLayout).setEnabled(false);
        mainScreen.findViewById(R.id.OpenProjectLayout).setVisibility(View.INVISIBLE);

        mainScreen.findViewById(R.id.OpenProjectEnterBackground).setEnabled(false);
        mainScreen.findViewById(R.id.OpenProjectEnterBackground).setVisibility(View.INVISIBLE);

        mainScreen.findViewById(R.id.OpenProjectEnterField).setEnabled(false);
        mainScreen.findViewById(R.id.OpenProjectEnterField).setVisibility(View.INVISIBLE);

        mainScreen.findViewById(R.id.OpenProjectButtonSubmit).setEnabled(false);
        mainScreen.findViewById(R.id.OpenProjectButtonSubmit).setVisibility(View.INVISIBLE);

        mainScreen.findViewById(R.id.OpenProjectError).setVisibility(View.INVISIBLE);
        mainScreen.findViewById(R.id.OpenProjectTitle).setVisibility(View.INVISIBLE);

        mainScreen.findViewById(R.id.OpenProjectClose).setEnabled(false);
        mainScreen.findViewById(R.id.OpenProjectClose).setVisibility(View.INVISIBLE);

        mainScreen.findViewById(R.id.OpenProjectScroll).setEnabled(false);
        mainScreen.findViewById(R.id.OpenProjectScroll).setVisibility(View.INVISIBLE);
        ViewGroup layout = mainScreen.findViewById(R.id.OpenProjectScrollLayout);
        layout.removeAllViews();

        mainScreen.findViewById(R.id.OpenProjectScrollLayout).setEnabled(false);
        mainScreen.findViewById(R.id.OpenProjectScrollLayout).setVisibility(View.INVISIBLE);
    }

    public static void setEnabled(MainActivity mainScreen){
        mainScreen.findViewById(R.id.OpenProjectLayout).setEnabled(true);
        mainScreen.findViewById(R.id.OpenProjectLayout).setVisibility(View.VISIBLE);

        mainScreen.findViewById(R.id.OpenProjectEnterBackground).setEnabled(true);
        mainScreen.findViewById(R.id.OpenProjectEnterBackground).setVisibility(View.VISIBLE);

        mainScreen.findViewById(R.id.OpenProjectEnterField).setEnabled(true);
        mainScreen.findViewById(R.id.OpenProjectEnterField).setVisibility(View.VISIBLE);

        mainScreen.findViewById(R.id.OpenProjectButtonSubmit).setEnabled(true);
        mainScreen.findViewById(R.id.OpenProjectButtonSubmit).setVisibility(View.VISIBLE);

        mainScreen.findViewById(R.id.OpenProjectError).setVisibility(View.VISIBLE);
        mainScreen.findViewById(R.id.OpenProjectTitle).setVisibility(View.VISIBLE);

        mainScreen.findViewById(R.id.OpenProjectClose).setEnabled(true);
        mainScreen.findViewById(R.id.OpenProjectClose).setVisibility(View.VISIBLE);

        mainScreen.findViewById(R.id.OpenProjectScroll).setEnabled(true);
        mainScreen.findViewById(R.id.OpenProjectScroll).setVisibility(View.VISIBLE);

        mainScreen.findViewById(R.id.OpenProjectScrollLayout).setEnabled(true);
        mainScreen.findViewById(R.id.OpenProjectScrollLayout).setVisibility(View.VISIBLE);
        scrollerRestart(mainScreen);
    }

    public static void setError(String error, MainActivity mainScreen) {
        TextView errorText = mainScreen.findViewById(R.id.OpenProjectError);
        errorText.setText(error);
    }

    public static String getEnteredText(MainActivity mainScreen){
        TextInputEditText textInputer = mainScreen.findViewById(R.id.OpenProjectEnterField);
        return Objects.requireNonNull(textInputer.getText()).toString();
    }

    public static void scrollerRestart(MainActivity mainScreen){
        LinearLayout scroller = mainScreen.findViewById(R.id.OpenProjectScrollLayout);
        scroller.removeAllViews();
        File[] files = FilesController.getAllLocalFiles(mainScreen, "/Projects");
        for (File entry : files) {
            if(entry.isDirectory()) {
                String key = entry.getName();
                scroller.addView(createFileButton(key, mainScreen));
            }
        }
    }

    private static Button createFileButton(String key, MainActivity mainScreen){
        Button button = new Button(mainScreen);
        button.setText("/Projects/" + key);
        button.setBackground(mainScreen.getDrawable(R.drawable.transperent));
        button.setTextColor(mainScreen.getColor(R.color.suggestion_yellow));
        button.setAllCaps(false);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextInputEditText input = mainScreen.findViewById(R.id.OpenProjectEnterField);
                input.setText("/Projects/" + key);
            }
        });
        return button;
    }
}
