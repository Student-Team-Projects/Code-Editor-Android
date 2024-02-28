package com.example.codeeditor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

public class InternalStorageActivity extends AppCompatActivity {
    private String currentDisplayedPath = null;
    private String copyDestinationPath = null;

    private static final int READ_REQUEST_CODE = 42;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;


    Button backButton;
    private LinearLayout layout;
    private LinearLayout layoutSmall;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_internal_storage_activity);

        currentDisplayedPath = getFilesDir().getPath();

        backButton = (Button) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backButtonPressed();
            }
        });

        Button createFolderButton = findViewById(R.id.createFolderButton);
        createFolderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createFolder();
            }
        });

        Button createFileButton = findViewById(R.id.createFileButton);
        createFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreateFileDialog();
            }
        });

        Button deviceButton = findViewById(R.id.createDeviceButton);
        deviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileExplorer();
            }
        });

        Button exitButton = findViewById(R.id.createExitButton);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        layout = findViewById(R.id.filesLayout);
        generateAvailableFiles(new File(getFilesDir().getPath()));
    }

    private void showCreateFileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter File Name:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String fileName = input.getText().toString().trim();
                if (!fileName.isEmpty()) {
                    createFile(fileName);
                } else {
                    Toast.makeText(InternalStorageActivity.this, "Please enter a file name", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
        generateAvailableFiles(new File(currentDisplayedPath));
    }

    private void backButtonPressed() {
        if (currentDisplayedPath.equals(getFilesDir().getPath())) {
            finish();
        } else {
            File currentFile = new File(currentDisplayedPath);
            File parentFile = currentFile.getParentFile();
            currentDisplayedPath = parentFile.getPath();
            generateAvailableFiles(parentFile);
        }
    }

    private void createFolder() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter folder name:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newFolderName = input.getText().toString().trim();
                if (!newFolderName.isEmpty()) {
                    File newFolder = new File(currentDisplayedPath, newFolderName);
                    if (newFolder.mkdir()) {
                        Toast.makeText(InternalStorageActivity.this, "Folder created: " + newFolderName, Toast.LENGTH_SHORT).show();
                        generateAvailableFiles(new File(currentDisplayedPath));
                    } else {
                        Toast.makeText(InternalStorageActivity.this, "Failed to create folder", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(InternalStorageActivity.this, "Please enter a folder name", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
        generateAvailableFiles(new File(currentDisplayedPath));
    }

    public void generateAvailableFiles(File directory) {
        layout.removeAllViews();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if(file.isDirectory()) {
                    addViewToLayout(createFileButton(file.getName(), file));
                }
            }
            for (File file : files) {
                if(!file.isDirectory()) {
                    addViewToLayout(createFileButton(file.getName(), file));
                }
            }
        }
    }

    public void generateAvailableFoldersOnChooseForCopy(File directory) {
        layoutSmall.removeAllViews();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    layoutSmall.addView(createFolderButtonOnChooseForCopy(file));
                }
            }
        }
    }

    private Button createFolderButtonOnChooseForCopy(File file) {
        Button button = new Button(this);
        button.setText(file.getName());

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyDestinationPath = file.getPath();
                generateAvailableFoldersOnChooseForCopy(file);
            }
        });

        return button;
    }

    private void copyFolder(File sourceFolder, File destinationFolder) {
       if (sourceFolder.isDirectory()) {
            File newDestinationFolder = new File(destinationFolder, sourceFolder.getName());
            newDestinationFolder.mkdir();

            String[] files = sourceFolder.list();

            for (String file : files) {
                File srcFile = new File(sourceFolder, file);
                File destFile = new File(newDestinationFolder, file);

                copyFolder(srcFile, destFile);
            }
        } else {
            try {
                if (destinationFolder.isDirectory()) {
                    destinationFolder = new File(destinationFolder, sourceFolder.getName());
                }
                FileInputStream in = new FileInputStream(sourceFolder);
                FileOutputStream out = new FileOutputStream(destinationFolder);

                byte[] buffer = new byte[1024];

                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }

                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Button createFileButton(String fileName, File file) {
        Button button = new Button(this);
        button.setText(fileName);
        button.setAllCaps(false);
        button.setBackgroundResource(android.R.drawable.btn_default);

        if (file.isDirectory()) {
            Drawable contactCardIcon = getResources().getDrawable(android.R.drawable.sym_contact_card);
            contactCardIcon.setBounds(0, 0, contactCardIcon.getIntrinsicWidth(), contactCardIcon.getIntrinsicHeight());
            button.setCompoundDrawablesRelative(contactCardIcon, null, null, null);
        }

        int paddingHorizontal = 12;
        int paddingVertical = 8;
        button.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);

        Paint textPaint = button.getPaint();
        int textWidth = (int) textPaint.measureText(fileName) * 2;
        int extraWidth = 32;
        int buttonWidth = textWidth + extraWidth;

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(buttonWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        button.setLayoutParams(params);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (file.isDirectory()) {
                    currentDisplayedPath = file.getPath();
                    generateAvailableFiles(file);
                } else {
//                    openFileEditor(fileName);
                }
            }
        });

        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showFileOptions(file);
                return true;
            }
        });

        return button;
    }

    private void showFileOptions(final File file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("File Options");
        builder.setItems(new CharSequence[]{"Delete", "Copy"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        deleteFile(file);
                        break;
                    case 1:
                        chooseDestinationDirectoryForCopyOperation(file);
                        break;
                }
            }
        });
        builder.show();
    }



    private void chooseDestinationDirectoryForCopyOperation(File sourse) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Directory");

        copyDestinationPath = getFilesDir().getPath();
        layoutSmall = new LinearLayout(this);
        layoutSmall.setOrientation(LinearLayout.VERTICAL);


        generateAvailableFoldersOnChooseForCopy(new File(copyDestinationPath));

        builder.setView(layoutSmall);
        builder.setNegativeButton("Cancel", null);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(InternalStorageActivity.this, "Copying...", Toast.LENGTH_SHORT).show();
                copyFolder(sourse, new File(copyDestinationPath));
            }
        });
        builder.setNeutralButton("Back", null);
        AlertDialog dialog = builder.create();
        dialog.show();

        Button neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);

        neutralButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Objects.equals(copyDestinationPath, getFilesDir().getPath())) {
                    Toast.makeText(InternalStorageActivity.this, "Back", Toast.LENGTH_SHORT).show();
                    copyDestinationPath = new File(copyDestinationPath).getParent();
                    generateAvailableFoldersOnChooseForCopy(new File(copyDestinationPath));
                }
                else{
                    Toast.makeText(InternalStorageActivity.this, "You are in the root directory", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

//    private void openFileEditor(String fileName) {
//        Intent intent = new Intent(InternalStorageActivity.this, FileEditorActivity.class);
//        intent.putExtra("fileName", fileName);
//        startActivity(intent);
//        Toast.makeText(InternalStorageActivity.this, "TODO", Toast.LENGTH_SHORT).show();
//    }

    private void addViewToLayout(View view) {
        layout.addView(view);
    }

    private void createFile(String fileName) {
        File newFile = new File(currentDisplayedPath, fileName);
        if (newFile.exists()) {
            Toast.makeText(InternalStorageActivity.this, "File already exists", Toast.LENGTH_SHORT).show();
        } else {
            String templateText = "";
            if (fileName.endsWith(".cpp")) {
                templateText = "#include<iostream>\n\nint main(){\n\tstd::cout<<\"Hello World!\\n\";\n\treturn 0;\n}";
            }
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(newFile);
                fileOutputStream.write(templateText.getBytes());
                fileOutputStream.close();
                Toast.makeText(InternalStorageActivity.this, "File created", Toast.LENGTH_SHORT).show();

                String filePath = newFile.getAbsolutePath();
                Log.d("Debug", filePath);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(InternalStorageActivity.this, "Error creating file", Toast.LENGTH_SHORT).show();
            }

            Toast.makeText(InternalStorageActivity.this, "File created", Toast.LENGTH_SHORT).show();
        }
    }
    private void deleteFile(final File file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Deletion");
        builder.setMessage("Are you sure you want to delete this file?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (file.delete()) {
                    Toast.makeText(InternalStorageActivity.this, "File deleted", Toast.LENGTH_SHORT).show();
                    generateAvailableFiles(new File(currentDisplayedPath));
                } else {
                    Toast.makeText(InternalStorageActivity.this, "Failed to delete file", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("No", null);
        builder.show();
    }



    private void openFileExplorer() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, READ_REQUEST_CODE);

        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                copyFileToInternalStorage(uri);
            }
        }
    }

    private void copyFileToInternalStorage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            String fileName = getFileNameFromUri(uri);

            File localFilesFolder = new File(getFilesDir(), "Local Files");
            if (!localFilesFolder.exists()) {
                localFilesFolder.mkdir();
            }
            File internalFile = new File(localFilesFolder.getPath(), fileName);
            OutputStream outputStream = new FileOutputStream(internalFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            inputStream.close();
            outputStream.close();
            Toast.makeText(this, "File copied to internal storage", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error copying file", Toast.LENGTH_SHORT).show();
        }
    }


    private String getFileNameFromUri(Uri uri) {
        Cursor cursor = null;
        try {
            String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
            cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
                return cursor.getString(columnIndex);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }


}
