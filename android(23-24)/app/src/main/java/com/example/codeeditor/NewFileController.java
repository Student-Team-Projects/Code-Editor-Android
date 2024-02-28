package com.example.codeeditor;

import android.view.View;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class NewFileController {

    public static void newFileInitialization(MainActivity mainScreen) {
        mainScreen.findViewById(R.id.NewFileButtonSubmit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fileName = NewFileController.getEnteredText(mainScreen);
                try{
                    FilesController.createFile(fileName, mainScreen);
                } catch (Exception e) {
                    setError(e.getMessage(), mainScreen);
                    return;
                }
                try{
                    mainScreen.setCurrentFileName(fileName);
                } catch (Exception e) {
                    setError(e.getMessage(), mainScreen);
                    return;
                }
                NewFileController.setDisabled(mainScreen);
                mainScreen.enableMainLayout();
            }
        });

        mainScreen.findViewById(R.id.NewFileClose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NewFileController.setDisabled(mainScreen);
                mainScreen.enableMainLayout();
            }
        });
    }

    public static void setDisabled(MainActivity mainScreen){
        mainScreen.findViewById(R.id.NewFileLayout).setEnabled(false);
        mainScreen.findViewById(R.id.NewFileLayout).setVisibility(View.INVISIBLE);

        mainScreen.findViewById(R.id.NewFileEnterBackground).setEnabled(false);
        mainScreen.findViewById(R.id.NewFileEnterBackground).setVisibility(View.INVISIBLE);

        mainScreen.findViewById(R.id.NewFileEnterField).setEnabled(false);
        mainScreen.findViewById(R.id.NewFileEnterField).setVisibility(View.INVISIBLE);

        mainScreen.findViewById(R.id.NewFileButtonSubmit).setEnabled(false);
        mainScreen.findViewById(R.id.NewFileButtonSubmit).setVisibility(View.INVISIBLE);

        mainScreen.findViewById(R.id.NewFileError).setVisibility(View.INVISIBLE);
        mainScreen.findViewById(R.id.NewFileTitle).setVisibility(View.INVISIBLE);

        mainScreen.findViewById(R.id.NewFileClose).setEnabled(false);
        mainScreen.findViewById(R.id.NewFileClose).setVisibility(View.INVISIBLE);
    }

    public static void setEnabled(MainActivity mainScreen){
        mainScreen.findViewById(R.id.NewFileLayout).setEnabled(true);
        mainScreen.findViewById(R.id.NewFileLayout).setVisibility(View.VISIBLE);

        mainScreen.findViewById(R.id.NewFileEnterBackground).setEnabled(true);
        mainScreen.findViewById(R.id.NewFileEnterBackground).setVisibility(View.VISIBLE);

        mainScreen.findViewById(R.id.NewFileEnterField).setEnabled(true);
        mainScreen.findViewById(R.id.NewFileEnterField).setVisibility(View.VISIBLE);

        mainScreen.findViewById(R.id.NewFileButtonSubmit).setEnabled(true);
        mainScreen.findViewById(R.id.NewFileButtonSubmit).setVisibility(View.VISIBLE);

        mainScreen.findViewById(R.id.NewFileError).setVisibility(View.VISIBLE);
        mainScreen.findViewById(R.id.NewFileTitle).setVisibility(View.VISIBLE);

        mainScreen.findViewById(R.id.NewFileClose).setEnabled(true);
        mainScreen.findViewById(R.id.NewFileClose).setVisibility(View.VISIBLE);
    }

    public static void setError(String error, MainActivity mainScreen) {
        TextView errorText = mainScreen.findViewById(R.id.NewFileError);
        errorText.setText(error);
    }

    public static String getEnteredText(MainActivity mainScreen){
        TextInputEditText textInputer = mainScreen.findViewById(R.id.NewFileEnterField);
        return Objects.requireNonNull(textInputer.getText()).toString();
    }
}
