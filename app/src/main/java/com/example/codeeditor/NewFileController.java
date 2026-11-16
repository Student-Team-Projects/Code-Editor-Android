package com.example.codeeditor;

import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.List;
import java.util.Objects;

public class NewFileController {

    public static void newFileInitialization(MainActivity mainScreen) {
        TextInputEditText textInputer = mainScreen.findViewById(R.id.NewFileEnterField);
        if (textInputer != null) {
            textInputer.setSingleLine(true);
            textInputer.setImeOptions(EditorInfo.IME_ACTION_DONE);
            textInputer.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO ||
                            (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                        mainScreen.findViewById(R.id.NewFileButtonSubmit).performClick();
                        return true;
                    }
                    return false;
                }
            });
        }

        mainScreen.findViewById(R.id.NewFileButtonSubmit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fileName = NewFileController.getEnteredText(mainScreen);
                Spinner spinner = mainScreen.findViewById(R.id.NewFileDirectorySpinner);
                MainActivity.DirItem selectedDir = (spinner != null && spinner.getSelectedItem() != null)
                        ? (MainActivity.DirItem) spinner.getSelectedItem()
                        : null;
                try {
                    mainScreen.createNewFileInOpenedFolder(fileName, selectedDir);
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

        Spinner spinner = mainScreen.findViewById(R.id.NewFileDirectorySpinner);
        if (spinner != null) {
            spinner.setEnabled(false);
            spinner.setVisibility(View.INVISIBLE);
        }
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
        mainScreen.findViewById(R.id.NewFileClose).setVisibility(View.INVISIBLE);

        Spinner spinner = mainScreen.findViewById(R.id.NewFileDirectorySpinner);
        if (spinner != null) {
            spinner.setEnabled(true);
            spinner.setVisibility(View.VISIBLE);
            List<MainActivity.DirItem> dirs = mainScreen.getRecursiveDirectories();
            ArrayAdapter<MainActivity.DirItem> adapter = new ArrayAdapter<>(
                    mainScreen, android.R.layout.simple_spinner_item, dirs
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
        }

        TextInputEditText textInputer = mainScreen.findViewById(R.id.NewFileEnterField);
        if (textInputer != null) {
            textInputer.setText("");
            textInputer.requestFocus();
        }
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
