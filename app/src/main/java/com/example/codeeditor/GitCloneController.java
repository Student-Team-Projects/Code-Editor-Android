package com.example.codeeditor;

import android.view.View;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class GitCloneController {

    public static void gitCloneInitialization(MainActivity mainScreen) {
        mainScreen.findViewById(R.id.GitCloneButtonSubmit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String gitLink = GitCloneController.getEnteredText(mainScreen);
                try {
                    GitController.gitClone(gitLink, mainScreen);
                } catch (Exception e) {
                    GitCloneController.setError(e.getMessage(), mainScreen);
                }
                GitCloneController.setDisabled(mainScreen);
                mainScreen.enableMainLayout();
            }
        });

        mainScreen.findViewById(R.id.GitCloneClose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GitCloneController.setDisabled(mainScreen);
                mainScreen.enableMainLayout();
            }
        });
    }

    public static void setDisabled(MainActivity mainScreen){
        mainScreen.findViewById(R.id.GitCloneLayout).setEnabled(false);
        mainScreen.findViewById(R.id.GitCloneLayout).setVisibility(View.INVISIBLE);

        mainScreen.findViewById(R.id.GitCloneEnterBackground).setEnabled(false);
        mainScreen.findViewById(R.id.GitCloneEnterBackground).setVisibility(View.INVISIBLE);

        mainScreen.findViewById(R.id.GitCloneEnterField).setEnabled(false);
        mainScreen.findViewById(R.id.GitCloneEnterField).setVisibility(View.INVISIBLE);

        mainScreen.findViewById(R.id.GitCloneButtonSubmit).setEnabled(false);
        mainScreen.findViewById(R.id.GitCloneButtonSubmit).setVisibility(View.INVISIBLE);

        mainScreen.findViewById(R.id.GitCloneError).setVisibility(View.INVISIBLE);
        mainScreen.findViewById(R.id.GitCloneTitle).setVisibility(View.INVISIBLE);

        mainScreen.findViewById(R.id.GitCloneClose).setEnabled(false);
        mainScreen.findViewById(R.id.GitCloneClose).setVisibility(View.INVISIBLE);
    }

    public static void setEnabled(MainActivity mainScreen){
        mainScreen.findViewById(R.id.GitCloneLayout).setEnabled(true);
        mainScreen.findViewById(R.id.GitCloneLayout).setVisibility(View.VISIBLE);

        mainScreen.findViewById(R.id.GitCloneEnterBackground).setEnabled(true);
        mainScreen.findViewById(R.id.GitCloneEnterBackground).setVisibility(View.VISIBLE);

        mainScreen.findViewById(R.id.GitCloneEnterField).setEnabled(true);
        mainScreen.findViewById(R.id.GitCloneEnterField).setVisibility(View.VISIBLE);

        mainScreen.findViewById(R.id.GitCloneButtonSubmit).setEnabled(true);
        mainScreen.findViewById(R.id.GitCloneButtonSubmit).setVisibility(View.VISIBLE);

        mainScreen.findViewById(R.id.GitCloneError).setVisibility(View.VISIBLE);
        mainScreen.findViewById(R.id.GitCloneTitle).setVisibility(View.VISIBLE);

        mainScreen.findViewById(R.id.GitCloneClose).setEnabled(true);
        mainScreen.findViewById(R.id.GitCloneClose).setVisibility(View.VISIBLE);
    }

    public static void setError(String error, MainActivity mainScreen) {
        TextView errorText = mainScreen.findViewById(R.id.GitCloneError);
        errorText.setText(error);
    }

    public static String getEnteredText(MainActivity mainScreen){
        TextInputEditText textInputer = mainScreen.findViewById(R.id.GitCloneEnterField);
        return Objects.requireNonNull(textInputer.getText()).toString();
    }
}
