package com.example.codeeditor;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;
import com.amrdeveloper.codeview.CodeView;

import java.io.File;
import java.util.HashMap;

import java.io.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;



public class MainActivity extends AppCompatActivity {

    // private Button newFileButton;
    // private Button openFileButton;
    // private Button exitButton;
     private Button fileExplorerButton;
    // private Button openInternalStorageButton;

    //  private static final int READ_REQUEST_CODE = 42;
    //  private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;

    private Button fileButton;
    private Button gitButton;
    private CodeView codeView;
    private EditText openFileHint;
    private HashMap<Object, Boolean> canBeEnabled;
    private View openFileTextInput;
    private String currentFileName;
    private String currentProjectPath;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

         createRequiredFolders();

        fileButton = FileButton.getFileButton(this);
        gitButton = GitButton.getGitButton(this);
        codeView = CodeEditorController.getCodeEditor(this);

        openFileHint = this.findViewById(R.id.openFileHint);
        openFileHint.setEnabled(false);
        CodeEditorController.DisableCodeEditor(this);

        NewFileController.setDisabled(this);
        NewFileController.newFileInitialization(this);
        OpenFileController.setDisabled(this);
        OpenFileController.openFileInitialization(this);
        OpenProjectController.setDisabled(this);
        OpenProjectController.openProjectInitialization(this);
        GitCloneController.setDisabled(this);
        GitCloneController.gitCloneInitialization(this);

        fileExplorerButton = findViewById(R.id.file_explorer);
        fileExplorerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, InternalStorageActivity.class);
                startActivity(intent);
            }
        });

        currentFileName = null;
        currentProjectPath = null;
        FilesController.createProjectsDirectory(this);

    }

    protected void disableOpenFileHint() {
        openFileHint.setVisibility(View.INVISIBLE);
    }

    protected void enableOpenFileHint() {
        openFileHint.setVisibility(View.VISIBLE);
    }

    protected void disableMainLayout(){
        canBeEnabled = new HashMap<>();
        disableLayout(this.findViewById(R.id.subMainLayout));
    }
    private void disableLayout(ViewGroup layout){
        if(layout.isEnabled()) canBeEnabled.put(layout, true);
        layout.setEnabled(false);
        layout.setAlpha(0.9F);
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if(child.isEnabled()) canBeEnabled.put(child, true);
            child.setEnabled(false);
            child.setAlpha(0.9F);
            if (child instanceof ViewGroup) {
                disableLayout((ViewGroup) child);
            }
        }
    }

    protected void enableMainLayout(){
        enableLayout(this.findViewById(R.id.subMainLayout));
    }

    private void enableLayout(ViewGroup layout){
        if(Boolean.TRUE.equals(canBeEnabled.get(layout))) {
            layout.setEnabled(true);
            layout.setAlpha(1f);
        }
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if(Boolean.TRUE.equals(canBeEnabled.get(child))) {
                child.setEnabled(true);
                child.setAlpha(1f);
            }
            if (child instanceof ViewGroup) {
                enableLayout((ViewGroup) child);
            }
        }
    }

    private void createRequiredFolders() {
        File localFilesFolder = new File(getFilesDir(), "Local Files");
        File projectsFolder = new File(getFilesDir(), "Projects");

        if (!localFilesFolder.exists()) {
            if (!localFilesFolder.mkdir()) {
                Log.e("MainActivity", "Failed to create Local Files folder");
            }
        }
        if (!projectsFolder.exists()) {
            if (!projectsFolder.mkdir()) {
                Log.e("MainActivity", "Failed to create Projects folder");
            }
        }
    }
        // fileExplorerButton = findViewById(R.id.file_explorer);
        // fileExplorerButton.setOnClickListener(new View.OnClickListener() {
        //     @Override
        //     public void onClick(View v) {
        //         openFileExplorer();
        //     }
        // });

        // openInternalStorageButton = findViewById(R.id.openInternalStorageButton);
        // openInternalStorageButton.setOnClickListener(new View.OnClickListener() {
        //     @Override
        //     public void onClick(View v) {
        //         Intent intent = new Intent(MainActivity.this, InternalStorageActivity.class);
        //         startActivity(intent);
        //     }
        // });
    protected boolean getEditorEnabled(){
        return !(openFileHint.getVisibility() == View.VISIBLE);
    }

    public void setCurrentFileName(String fileName) throws Exception{
        if(fileName == null){
            currentFileName = null;
            enableOpenFileHint();
            CodeEditorController.setCode("", this);
            CodeEditorController.DisableCodeEditor(this);
            return;
        }
        currentFileName = fileName;
        disableOpenFileHint();
        CodeEditorController.setCode(FilesController.getFilesInside(fileName, this), this);
        CodeEditorController.EnableCodeEditor(this);
    }

    public void setCurrentProjectPath(String projectPath) throws Exception{
        currentProjectPath = projectPath;
        DirectoryTreeController.setEnabled(this);
    }

    public String getCurrentFileName(){
        return currentFileName;
    }
    
    public String getCurrentProjectPath(){
        return currentProjectPath;
    }
}
