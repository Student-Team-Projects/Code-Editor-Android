package com.example.codeeditor;

import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileExplorerActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private FileAdapter fileAdapter;
    private List<File> fileList;
    private TextView currentPathTextView;
    private File currentDirectory;


    private static final int REQUEST_EXTERNAL_STORAGE_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_explorer);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_EXTERNAL_STORAGE_PERMISSION);
        } else {
            setupFileExplorer();
        }
    }


    private void setupFileExplorer(){
        recyclerView = findViewById(R.id.recyclerView);
        currentPathTextView = findViewById(R.id.currentPathTextView);
        Button internalStorageButton = findViewById(R.id.internalStorageButton);
        Button startingPointButton = findViewById(R.id.startingPointButton);
        Button backButton = findViewById(R.id.backButton);

        fileList = new ArrayList<>();
        fileAdapter = new FileAdapter(fileList, new FileAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(File file, int position) {
                if (file.isDirectory()) {
                    currentDirectory = file;
                    updateFileList(currentDirectory);
                } else {
                    copyFileToInternalStorage(file);
                }
            }
        });

        recyclerView.setAdapter(fileAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        currentDirectory = Environment.getExternalStorageDirectory();
        updateFileList(currentDirectory);

        internalStorageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInternalStorage();
            }
        });
        startingPointButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentDirectory = Environment.getExternalStorageDirectory();
                updateFileList(currentDirectory);
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackButtonClick(v);
            }
        });
    }


    private void showInternalStorage() {
        currentDirectory = getFilesDir();
        updateFileList(currentDirectory);
    }

    private void updateFileList(File directory) {
        fileList.clear();
        fileList.addAll(Arrays.asList(directory.listFiles()));
        currentPathTextView.setText(directory.getAbsolutePath());
        fileAdapter.notifyDataSetChanged();
    }

    private void copyFileToInternalStorage(File sourceFile) {
        try {
            File internalFile = new File(getFilesDir(), sourceFile.getName());
            copyFile(sourceFile, internalFile);
            Toast.makeText(this, "File copied to internal storage", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error copying file", Toast.LENGTH_SHORT).show();
        }
    }

    private void copyFile(File sourceFile, File destFile) throws IOException {
        try (InputStream in = new FileInputStream(sourceFile);
             OutputStream out = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }

    public void onBackButtonClick(View view) {
        if (currentDirectory.getParentFile() != null) {
            currentDirectory = currentDirectory.getParentFile();
            updateFileList(currentDirectory);
        }
        else{
            Toast.makeText(this, "You are already at the root directory", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_EXTERNAL_STORAGE_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupFileExplorer();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

}