package com.example.codeeditor;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.ViewModelProvider;

import com.example.codeeditor.model.FileLogic;
import com.example.codeeditor.ui.theme.ThemeManager;
import com.example.codeeditor.viewmodels.FileVM;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ImageButton hamburgerMenu;
    private WebView codeView;
    private HashMap<Object, Boolean> canBeEnabled;
    private String currentFileName;
    private String currentProjectPath;
    private Uri currentFileUri;
    private Uri currentDirectoryUri;

    public String currentLanguage = "English";
    public boolean showGitMenu = false;
    public boolean showCommitDialog = false;
    public boolean showPushDialog = false;
    public boolean showAddRemoteDialog = false;
    public boolean showAddFilesDialog = false;
    public List<String> availableGitFiles = new ArrayList<>();
    public FileVM fileVM;

    private final ActivityResultLauncher<String[]> openDocumentLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    try {
                        getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (Exception ignored) {}
                    openFileFromUri(uri);
                }
            }
    );

    private final ActivityResultLauncher<Uri> openDirectoryLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocumentTree(),
            uri -> {
                if (uri != null) {
                    try {
                        getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (Exception ignored) {}
                    openDirectoryFromUri(uri);
                }
            }
    );

    public void openFilePicker() {
        openDocumentLauncher.launch(new String[]{"*/*"});
    }

    public void openDirectoryPicker() {
        openDirectoryLauncher.launch(null);
    }

    public void openFileFromUri(Uri uri) {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                reader.close();
                inputStream.close();
            }
            String content = stringBuilder.toString();
            String fileName = getFileNameFromUri(uri);
            if (fileName == null) fileName = "file.txt";

            currentFileUri = uri;
            currentFileName = fileName;

            CodeEditorController.EnableCodeEditor(this);
            CodeEditorController.setCode(content, this);
        } catch (Exception e) {
            Log.e("MainActivity", "Error opening file from Uri: " + uri, e);
            Toast.makeText(this, "Failed to open file: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void openDirectoryFromUri(Uri uri) {
        try {
            currentDirectoryUri = uri;
            DocumentFile docFile = DocumentFile.fromTreeUri(this, uri);
            if (docFile != null) {
                currentProjectPath = uri.toString();
                DirectoryTreeController.setEnabledForDocumentFile(this, docFile);
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error opening directory: " + uri, e);
            Toast.makeText(this, "Failed to open directory: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void createNewFileInOpenedFolder(String fileName) throws Exception {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new Exception("File name cannot be empty");
        }
        fileName = fileName.trim();

        if (currentDirectoryUri != null) {
            DocumentFile docDir = DocumentFile.fromTreeUri(this, currentDirectoryUri);
            if (docDir != null && docDir.isDirectory()) {
                DocumentFile existing = docDir.findFile(fileName);
                if (existing != null && existing.exists()) {
                    throw new Exception("File " + fileName + " already exists in folder");
                }

                String extension = "";
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    extension = fileName.substring(dotIndex + 1);
                }
                String mimeType = null;
                if (!extension.isEmpty()) {
                    mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
                }
                if (mimeType == null) {
                    mimeType = "*/*";
                }

                DocumentFile newDoc = docDir.createFile(mimeType, fileName);
                if (newDoc != null) {
                    openFileFromUri(newDoc.getUri());
                    DirectoryTreeController.setEnabledForDocumentFile(this, docDir);
                    Toast.makeText(this, "File created: " + newDoc.getName(), Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }

        File targetDir = null;
        if (currentProjectPath != null && !currentProjectPath.isEmpty()) {
            targetDir = FilesController.getFileByPath(currentProjectPath, this);
        }
        if (targetDir == null || !targetDir.exists()) {
            targetDir = getFilesDir();
        }

        File newFile = new File(targetDir, fileName);
        if (newFile.exists()) {
            throw new Exception("File " + fileName + " already exists");
        }

        String templateText = "";
        if (fileName.endsWith(".cpp")) {
            templateText = "#include <iostream>\n\nint main() {\n    std::cout << \"Hello World!\\n\";\n    return 0;\n}";
        } else if (fileName.endsWith(".java")) {
            String className = fileName.contains(".") ? fileName.substring(0, fileName.indexOf('.')) : fileName;
            templateText = "public class " + className + " {\n    public static void main(String[] args) {\n        System.out.println(\"Hello World!\");\n    }\n}";
        }

        FileOutputStream fos = new FileOutputStream(newFile);
        fos.write(templateText.getBytes());
        fos.close();

        currentFileName = newFile.getName();
        CodeEditorController.EnableCodeEditor(this);
        CodeEditorController.setCode(templateText, this);

        if (currentProjectPath != null && !currentProjectPath.isEmpty()) {
            DirectoryTreeController.setEnabled(this);
        } else {
            currentProjectPath = targetDir.getName();
            DirectoryTreeController.setEnabled(this);
        }

        Toast.makeText(this, "File created: " + fileName, Toast.LENGTH_SHORT).show();
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            } catch (Exception ignored) {}
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result;
    }

    private Uri getProjectUri() {
        if (currentDirectoryUri != null) {
            return currentDirectoryUri;
        }
        if (currentProjectPath == null || currentProjectPath.isEmpty()) {
            File defaultDir = getFilesDir();
            return Uri.fromFile(defaultDir);
        }
        File projFile = FilesController.getFileByPath(currentProjectPath, this);
        return projFile != null ? Uri.fromFile(projFile) : Uri.fromFile(getFilesDir());
    }

    public void gitInit() {
        Uri uri = getProjectUri();
        String result = FileLogic.initGitRepository(this, uri);
        Toast.makeText(this, result, Toast.LENGTH_LONG).show();
    }

    public void gitStatus() {
        Uri uri = getProjectUri();
        String result = FileLogic.gitStatus(this, uri);
        Toast.makeText(this, result, Toast.LENGTH_LONG).show();
    }

    public void gitAddFiles() {
        Uri uri = getProjectUri();
        List<String> files = FileLogic.getUnstagedFiles(this, uri);
        if (files.isEmpty()) {
            Toast.makeText(this, "No modified or unstaged files found.", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] items = files.toArray(new String[0]);
        boolean[] checked = new boolean[items.length];
        List<String> selected = new ArrayList<>();

        new AlertDialog.Builder(this)
                .setTitle("Select Files to Add")
                .setMultiChoiceItems(items, checked, (dialog, which, isChecked) -> {
                    if (isChecked) selected.add(items[which]);
                    else selected.remove(items[which]);
                })
                .setPositiveButton("Add", (dialog, which) -> {
                    String res = FileLogic.gitAddFiles(this, uri, selected);
                    Toast.makeText(this, res, Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void gitAddAll() {
        Uri uri = getProjectUri();
        String result = FileLogic.gitAddAll(this, uri);
        Toast.makeText(this, result, Toast.LENGTH_LONG).show();
    }

    public void gitCommit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Git Commit");

        final EditText input = new EditText(this);
        input.setHint("Enter commit message");
        builder.setView(input);

        builder.setPositiveButton("Commit", (dialog, which) -> {
            String msg = input.getText().toString();
            performGitCommit(msg);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    public void gitPush() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Git Push");

        final EditText input = new EditText(this);
        input.setHint("Username or Personal Access Token (optional)");
        builder.setView(input);

        builder.setPositiveButton("Push", (dialog, which) -> {
            String user = input.getText().toString();
            performGitPush(user, "");
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    public void gitAddRemote() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Git Remote");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        final EditText inputName = new EditText(this);
        inputName.setHint("Remote Name (e.g. origin)");
        layout.addView(inputName);

        final EditText inputUrl = new EditText(this);
        inputUrl.setHint("Remote URL (e.g. https://github.com/...)");
        layout.addView(inputUrl);

        builder.setView(layout);
        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = inputName.getText().toString();
            String url = inputUrl.getText().toString();
            performGitAddRemote(name, url);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    public void performGitCommit(String message) {
        Uri uri = getProjectUri();
        String result = FileLogic.gitCommit(this, uri, message);
        Toast.makeText(this, result, Toast.LENGTH_LONG).show();
    }

    public void performGitPush(String username, String password) {
        Uri uri = getProjectUri();
        String result = FileLogic.gitPush(this, uri, username, password);
        Toast.makeText(this, result, Toast.LENGTH_LONG).show();
    }

    public void performGitAddRemote(String name, String url) {
        Uri uri = getProjectUri();
        String result = FileLogic.gitAddRemote(this, uri, name, url);
        Toast.makeText(this, result, Toast.LENGTH_LONG).show();
    }

    public void performGitAddFiles(List<String> files) {
        Uri uri = getProjectUri();
        String result = FileLogic.gitAddFiles(this, uri, files);
        Toast.makeText(this, result, Toast.LENGTH_LONG).show();
    }

    private void requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                1
            );
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentProjectPath", currentProjectPath);
        outState.putString("currentFileName", currentFileName);
        if (currentDirectoryUri != null) {
            outState.putString("currentDirectoryUri", currentDirectoryUri.toString());
        }
        if (currentFileUri != null) {
            outState.putString("currentFileUri", currentFileUri.toString());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.INSTANCE.initTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fileVM = new ViewModelProvider(this).get(FileVM.class);

        requestStoragePermissions();
        createRequiredFolders();

        codeView = CodeEditorController.setupCodeEditor(this);

        CodeEditorController.DisableCodeEditor(this);

        NewFileController.setDisabled(this);
        NewFileController.newFileInitialization(this);
        OpenFileController.setDisabled(this);
        OpenFileController.openFileInitialization(this);
        OpenProjectController.setDisabled(this);
        OpenProjectController.openProjectInitialization(this);
        GitCloneController.setDisabled(this);
        GitCloneController.gitCloneInitialization(this);

        hamburgerMenu = findViewById(R.id.hamburger_menu);
        hamburgerMenu.setOnClickListener(v -> {
            Context wrapper = new ContextThemeWrapper(MainActivity.this, R.style.CustomPopupMenu);
            PopupMenu popup = new PopupMenu(wrapper, v);
            popup.getMenuInflater().inflate(R.menu.menu_main, popup.getMenu());

            if (getCurrentFileName() == null) {
                MenuItem saveItem = popup.getMenu().findItem(R.id.action_save_file);
                if (saveItem != null) saveItem.setEnabled(false);
                MenuItem closeItem = popup.getMenu().findItem(R.id.action_close_file);
                if (closeItem != null) closeItem.setEnabled(false);
            }

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_open_file) {
                    openFilePicker();
                    return true;
                } else if (id == R.id.action_new_file) {
                    disableMainLayout();
                    NewFileController.setEnabled(MainActivity.this);
                    return true;
                } else if (id == R.id.action_open_project) {
                    openDirectoryPicker();
                    return true;
                } else if (id == R.id.action_save_file) {
                    if (getCurrentFileName() != null) {
                        CodeEditorController.getCode(MainActivity.this, content -> {
                            FilesController.saveFile(getCurrentFileName(), content, MainActivity.this);
                        });
                    }
                    return true;
                } else if (id == R.id.action_close_file) {
                    if (getCurrentFileName() != null) {
                        try {
                            setCurrentFileName(null);
                        } catch (Exception ignored) {}
                    }
                    return true;
                } else if (id == R.id.action_close_project) {
                    try {
                        setCurrentProjectPath(null);
                    } catch (Exception ignored) {}
                    return true;
                } else if (id == R.id.action_git_init) {
                    gitInit();
                    return true;
                } else if (id == R.id.action_git_add_all) {
                    gitAddAll();
                    return true;
                } else if (id == R.id.action_git_status) {
                    gitStatus();
                    return true;
                } else if (id == R.id.action_git_commit) {
                    gitCommit();
                    return true;
                } else if (id == R.id.action_git_push) {
                    gitPush();
                    return true;
                } else if (id == R.id.action_git_add_remote) {
                    gitAddRemote();
                    return true;
                } else if (id == R.id.action_clone) {
                    disableMainLayout();
                    GitCloneController.setEnabled(MainActivity.this);
                    return true;
                } else if (id == R.id.action_toggle_theme) {
                    String newTheme = ThemeManager.INSTANCE.toggleTheme(MainActivity.this);
                    boolean isDark = ThemeManager.INSTANCE.isDark(newTheme);
                    if (codeView != null) {
                        String js = "if (window.editor) { monaco.editor.setTheme('" + (isDark ? "vs-dark" : "vs") + "'); }";
                        codeView.evaluateJavascript(js, null);
                    }
                    return true;
                } else if (id == R.id.action_file_explorer) {
                    Intent intent = new Intent(MainActivity.this, InternalStorageActivity.class);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
            popup.show();
        });

        currentFileName = null;
        currentProjectPath = null;
        FilesController.createProjectsDirectory(this);

        if (savedInstanceState != null) {
            currentProjectPath = savedInstanceState.getString("currentProjectPath");
            currentFileName = savedInstanceState.getString("currentFileName");
            String dirUriStr = savedInstanceState.getString("currentDirectoryUri");
            String fileUriStr = savedInstanceState.getString("currentFileUri");
            if (dirUriStr != null) {
                currentDirectoryUri = Uri.parse(dirUriStr);
                DocumentFile docFile = DocumentFile.fromTreeUri(this, currentDirectoryUri);
                if (docFile != null) {
                    DirectoryTreeController.setEnabledForDocumentFile(this, docFile);
                }
            } else if (currentProjectPath != null) {
                try {
                    setCurrentProjectPath(currentProjectPath);
                } catch (Exception ignored) {}
            }
            if (fileUriStr != null) {
                currentFileUri = Uri.parse(fileUriStr);
                openFileFromUri(currentFileUri);
            } else if (currentFileName != null) {
                try {
                    setCurrentFileName(currentFileName);
                } catch (Exception ignored) {}
            }
        }
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

    protected boolean getEditorEnabled(){
        return currentFileName != null;
    }

    public void setCurrentFileName(String fileName) throws Exception{
        if(fileName == null){
            currentFileName = null;
            CodeEditorController.DisableCodeEditor(this);
            return;
        }
        currentFileName = fileName;
        CodeEditorController.EnableCodeEditor(this);
        CodeEditorController.setCode(FilesController.getFilesInside(fileName, this), this);
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
