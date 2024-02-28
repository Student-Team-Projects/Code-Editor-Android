package com.example.codeeditor;

import android.view.View;

import com.amrdeveloper.codeview.CodeView;

import java.util.Objects;

public class CodeEditorController {

    public static CodeView getCodeEditor(MainActivity mainScreen){
        CodeView codeEditor = mainScreen.findViewById(R.id.codeView);
        lineNumberConfiguration(codeEditor, mainScreen);
        return codeEditor;
    }

    public static void DisableCodeEditor(MainActivity mainScreen){
        mainScreen.enableOpenFileHint();
        CodeView codePanel = mainScreen.findViewById(R.id.codeView);
        codePanel.setEnabled(false);
        codePanel.setVisibility(View.INVISIBLE);
    }

    public static void EnableCodeEditor(MainActivity mainScreen){
        mainScreen.disableOpenFileHint();
        CodeView codePanel = mainScreen.findViewById(R.id.codeView);
        codePanel.setEnabled(true);
        codePanel.setVisibility(View.VISIBLE);
    }


    private static void lineNumberConfiguration(CodeView codeEditor, MainActivity mainScreen){
        codeEditor.setEnableLineNumber(true);
        codeEditor.setEnableHighlightCurrentLine(true);
        codeEditor.setHighlightCurrentLineColor(mainScreen.getColor(R.color.dark_gray));
        codeEditor.setLineNumberTextColor(mainScreen.getColor(R.color.line_number_color));
        codeEditor.setLineNumberTextSize(30);
    }

    public static void setCode(String code, MainActivity mainScreen){
        CodeView codeEditor = mainScreen.findViewById(R.id.codeView);
        codeEditor.setText(code);
    }

    public static String getCode(MainActivity mainScreen){
        CodeView codeEditor = mainScreen.findViewById(R.id.codeView);
        return Objects.requireNonNull(codeEditor.getText()).toString();
    }

}
