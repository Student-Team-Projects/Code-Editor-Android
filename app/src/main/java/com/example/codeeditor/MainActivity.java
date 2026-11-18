package com.example.codeeditor;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
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
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.ViewModelProvider;

import com.example.codeeditor.activities.TermuxConnectionActivity;
import com.example.codeeditor.composables.TerminalPanelHost;
import com.example.codeeditor.model.FileLogic;
import com.example.codeeditor.termux.ssh.SshProfile;
import com.example.codeeditor.ui.theme.ThemeManager;
import com.example.codeeditor.viewmodels.FileVM;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private ImageButton hamburgerMenu;
    private WebView codeView;
    private ImageButton terminalToggle;
    private View terminalView; // created on first show
    private boolean terminalShown = false;
    private HashMap<Object, Boolean> canBeEnabled;
    private String currentFileName;
    private String currentProjectPath;
    private Uri currentFileUri;
    private Uri currentDirectoryUri;
    private String savedContent;

    public String currentLanguage = "English";
    public boolean showGitMenu = false;
    public boolean showCommitDialog = false;
    public boolean showPushDialog = false;
    public boolean showAddRemoteDialog = false;
    public boolean showAddFilesDialog = false;
    public List<String> availableGitFiles = new ArrayList<>();
    public FileVM fileVM;

    public static class DirItem {
        public final String displayName;
        public final File localFile;
        public final DocumentFile docFile;
        public final Uri uri;

        public DirItem(String displayName, File localFile, DocumentFile docFile, Uri uri) {
            this.displayName = displayName;
            this.localFile = localFile;
            this.docFile = docFile;
            this.uri = uri;
        }

        @NonNull
        @Override
        public String toString() {
            return displayName;
        }
    }

    public List<DirItem> getRecursiveDirectories() {
        List<DirItem> list = new ArrayList<>();
        if (currentDirectoryUri != null) {
            DocumentFile rootDoc = DocumentFile.fromTreeUri(this, currentDirectoryUri);
            if (rootDoc != null && rootDoc.isDirectory()) {
                collectDocumentDirectories(rootDoc, "", list);
            }
        } else {
            File rootDir = null;
            if (currentProjectPath != null && !currentProjectPath.isEmpty()) {
                rootDir = FilesController.getFileByPath(currentProjectPath, this);
            }
            if (rootDir == null || !rootDir.exists()) {
                rootDir = getFilesDir();
            }
            collectLocalDirectories(rootDir, "", list);
        }
        if (list.isEmpty()) {
            list.add(new DirItem("Root", getFilesDir(), null, Uri.fromFile(getFilesDir())));
        }
        return list;
    }

    private void collectLocalDirectories(File dir, String indent, List<DirItem> list) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        String name = indent.isEmpty() ? (dir.getName().isEmpty() ? "Root" : dir.getName()) : indent + dir.getName();
        list.add(new DirItem(name, dir, null, Uri.fromFile(dir)));
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    collectLocalDirectories(child, indent + "   📁 ", list);
                }
            }
        }
    }

    private void collectDocumentDirectories(DocumentFile doc, String indent, List<DirItem> list) {
        if (doc == null || !doc.isDirectory()) return;
        String name = indent.isEmpty() ? (doc.getName() == null ? "Root" : doc.getName()) : indent + doc.getName();
        list.add(new DirItem(name, null, doc, doc.getUri()));
        DocumentFile[] children = doc.listFiles();
        if (children != null) {
            for (DocumentFile child : children) {
                if (child.isDirectory()) {
                    collectDocumentDirectories(child, indent + "   📁 ", list);
                }
            }
        }
    }

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
        if (uri.equals(currentFileUri)) {
            // Reloading it from disk would discard unsaved edits.
            CodeEditorController.EnableCodeEditor(this);
            return;
        }

        try {
            StringBuilder stringBuilder = new StringBuilder();
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
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

            saveOpenFile(false);
            showFile(uri, fileName, content);
        } catch (Exception e) {
            Log.e("MainActivity", "Error opening file from Uri: " + uri, e);
            Toast.makeText(this, "Failed to open file: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** @param uri null for a file under the app's files dir, which {@code name} is then relative to */
    private void showFile(Uri uri, String name, String content) {
        currentFileUri = uri;
        currentFileName = name;
        savedContent = content;
        CodeEditorController.EnableCodeEditor(this);
        CodeEditorController.setCode(content, this);
    }

    /** The open file's content as last read from or written to disk; null when no file is open. */
    public String getSavedContent() {
        return savedContent;
    }

    /**
     * Writes the editor content to where the open file came from. Asynchronous, but it reads the
     * editor before any later {@link CodeEditorController#setCode} replaces it.
     *
     * @param force also write an unchanged file (otherwise its new mtime would make `make` rebuild)
     */
    public void saveOpenFile(boolean force) {
        final Uri uri = currentFileUri;
        final String name = currentFileName;
        final String contentOnDisk = savedContent;
        if (uri == null && name == null) return;
        CodeEditorController.getCode(this, content -> {
            if (content == null) {
                Toast.makeText(this, "Couldn't read the editor, file not saved", Toast.LENGTH_LONG).show();
                return;
            }
            if (!force && content.equals(contentOnDisk)) return;
            boolean saved = uri != null ? writeToUri(uri, content) : FilesController.saveFile(name, content, this);
            boolean stillOpen = Objects.equals(uri, currentFileUri) && Objects.equals(name, currentFileName);
            if (saved && stillOpen) savedContent = content;
        });
    }

    private boolean writeToUri(Uri uri, String content) {
        try (OutputStream os = getContentResolver().openOutputStream(uri, "wt")) {
            if (os == null) throw new IOException("No output stream for " + uri);
            os.write(content.getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, "File saved", Toast.LENGTH_SHORT).show();
            return true;
        } catch (Exception e) {
            Log.e("MainActivity", "Error saving " + uri, e);
            Toast.makeText(this, "Error saving file: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveOpenFile(false);
    }

    private void saveSessionToPrefs() {
        SharedPreferences prefs = getSharedPreferences("app_session", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("last_project_path", currentProjectPath);
        editor.putString("last_directory_uri", currentDirectoryUri != null ? currentDirectoryUri.toString() : null);
        editor.apply();
    }

    public void openDirectoryFromUri(Uri uri) {
        try {
            currentDirectoryUri = uri;
            DocumentFile docFile = DocumentFile.fromTreeUri(this, uri);
            if (docFile != null) {
                currentProjectPath = uri.toString();
                DirectoryTreeController.setEnabledForDocumentFile(this, docFile);
                saveSessionToPrefs();
                updateTerminalProject();
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error opening directory: " + uri, e);
            Toast.makeText(this, "Failed to open directory: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void createNewFileInOpenedFolder(String fileName, DirItem targetDirItem) throws Exception {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new Exception("File name cannot be empty");
        }
        fileName = fileName.trim();

        if (targetDirItem != null && targetDirItem.docFile != null) {
            DocumentFile docDir = targetDirItem.docFile;
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
                if (currentDirectoryUri != null) {
                    DocumentFile rootDoc = DocumentFile.fromTreeUri(this, currentDirectoryUri);
                    if (rootDoc != null) DirectoryTreeController.setEnabledForDocumentFile(this, rootDoc);
                }
                Toast.makeText(this, "File created: " + newDoc.getName(), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        File targetDir = (targetDirItem != null) ? targetDirItem.localFile : null;
        if (targetDir == null || !targetDir.exists()) {
            if (currentProjectPath != null && !currentProjectPath.isEmpty()) {
                targetDir = FilesController.getFileByPath(currentProjectPath, this);
            }
            if (targetDir == null || !targetDir.exists()) {
                targetDir = getFilesDir();
            }
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

        saveOpenFile(false);
        showFile(null, newFile.getName(), templateText);

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

    public void updateThemeColors() {
        boolean isDark = ThemeManager.INSTANCE.isDark(ThemeManager.INSTANCE.getColorMode(this));
        
        int surfaceColor = isDark ? getColor(R.color.purple_0) : getColor(R.color.purple_5);
        int borderColor = getColor(R.color.purple_3);
        int codeBgColor = isDark ? getColor(R.color.darker_dark_gray) : getColor(R.color.white);

        View upperPanel = findViewById(R.id.upperPanelLayout);
        if (upperPanel != null) {
            upperPanel.setBackground(createPanelDrawable(surfaceColor, borderColor, true, false, false, false));
        }

        View explorerLayout = findViewById(R.id.ExplorerLayout);
        if (explorerLayout != null) {
            explorerLayout.setBackground(createPanelDrawable(surfaceColor, borderColor, false, true, false, true));
        }

        View codeTextLayout = findViewById(R.id.CodeTextLayout);
        if (codeTextLayout != null) {
            codeTextLayout.setBackground(createPanelDrawable(codeBgColor, borderColor, false, false, true, false));
        }
    }

    private LayerDrawable createPanelDrawable(int fillColor, int borderColor, boolean bottomShadow, boolean rightShadow, boolean topShadow, boolean leftShadow) {
        ShapeDrawable border = new ShapeDrawable(new RectShape());
        border.getPaint().setColor(borderColor);

        ShapeDrawable fill = new ShapeDrawable(new RectShape());
        fill.getPaint().setColor(fillColor);

        Drawable[] layers = new Drawable[]{border, fill};
        LayerDrawable ld = new LayerDrawable(layers);

        int left = leftShadow ? 3 : 0;
        int top = topShadow ? 3 : 0;
        int right = rightShadow ? 3 : 0;
        int bottom = bottomShadow ? 3 : 0;
        ld.setLayerInset(1, left, top, right, bottom);
        return ld;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentProjectPath", currentProjectPath);
        outState.putString("currentFileName", currentFileName);
        outState.putBoolean("terminalShown", terminalShown);
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

        updateThemeColors();

        fileVM = new ViewModelProvider(this).get(FileVM.class);

        requestStoragePermissions();
        createRequiredFolders();

        codeView = CodeEditorController.setupCodeEditor(this);

        SshProfile.installAsDefault(this);
        terminalToggle = findViewById(R.id.terminal_toggle);
        terminalToggle.setOnClickListener(v -> {
            if (terminalShown) showEditor();
            else showTerminal();
        });

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
                    saveOpenFile(true);
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
                    updateThemeColors();
                    return true;
                } else if (id == R.id.action_termux_connection) {
                    openTermuxConnection();
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
        currentProjectPath = "";
        FilesController.createProjectsDirectory(this);

        boolean resumeSession = getIntent().getBooleanExtra("resume_session", false);
        boolean openFolderPicker = getIntent().getBooleanExtra("open_folder_picker", false);

        if (openFolderPicker) {
            getWindow().getDecorView().post(this::openDirectoryPicker);
        } else if (resumeSession) {
            SharedPreferences prefs = getSharedPreferences("app_session", MODE_PRIVATE);
            String lastProj = prefs.getString("last_project_path", null);
            String lastDirUri = prefs.getString("last_directory_uri", null);
            if (lastDirUri != null) {
                try {
                    openDirectoryFromUri(Uri.parse(lastDirUri));
                } catch (Exception ignored) {}
            } else if (lastProj != null && !lastProj.isEmpty()) {
                try {
                    setCurrentProjectPath(lastProj);
                } catch (Exception ignored) {}
            } else {
                try {
                    setCurrentProjectPath("");
                } catch (Exception ignored) {}
            }
        } else {
            if (savedInstanceState != null) {
                currentProjectPath = savedInstanceState.getString("currentProjectPath");
                String fileName = savedInstanceState.getString("currentFileName");
                String dirUriStr = savedInstanceState.getString("currentDirectoryUri");
                String fileUriStr = savedInstanceState.getString("currentFileUri");
                if (dirUriStr != null) {
                    currentDirectoryUri = Uri.parse(dirUriStr);
                    DocumentFile docFile = DocumentFile.fromTreeUri(this, currentDirectoryUri);
                    if (docFile != null) {
                        DirectoryTreeController.setEnabledForDocumentFile(this, docFile);
                    }
                } else if (currentProjectPath != null && !currentProjectPath.isEmpty()) {
                    try {
                        setCurrentProjectPath(currentProjectPath);
                    } catch (Exception ignored) {}
                } else {
                    try {
                        setCurrentProjectPath("");
                    } catch (Exception ignored) {}
                }
                if (fileUriStr != null) {
                    openFileFromUri(Uri.parse(fileUriStr));
                } else if (fileName != null) {
                    try {
                        setCurrentFileName(fileName);
                    } catch (Exception ignored) {}
                }
            } else {
                try {
                    setCurrentProjectPath("");
                } catch (Exception ignored) {}
            }
        }

        updateTerminalProject();
        if (savedInstanceState != null && savedInstanceState.getBoolean("terminalShown")) {
            showTerminal();
        }
    }

    public boolean isTerminalShown() {
        return terminalShown;
    }

    /** Shows the terminal in place of the editor. The editor is only hidden, so it keeps its state. */
    public void showTerminal() {
        // Commands run in the terminal should see the edits.
        saveOpenFile(false);
        if (terminalView == null) {
            terminalView = TerminalPanelHost.create(this, this::openTermuxConnection);
            FrameLayout container = findViewById(R.id.CodeTextLayout);
            container.addView(terminalView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        codeView.setVisibility(View.GONE);
        terminalView.setVisibility(View.VISIBLE);
        terminalShown = true;
        terminalToggle.setImageResource(R.drawable.ic_code);
        terminalToggle.setContentDescription("Show editor");
    }

    public void showEditor() {
        if (terminalView != null && terminalView.getVisibility() == View.VISIBLE) {
            InputMethodManager imm = getSystemService(InputMethodManager.class);
            if (imm != null) imm.hideSoftInputFromWindow(terminalView.getWindowToken(), 0);
            terminalView.clearFocus();
            terminalView.setVisibility(View.GONE);
        }
        codeView.setVisibility(View.VISIBLE);
        terminalShown = false;
        terminalToggle.setImageResource(R.drawable.ic_terminal);
        terminalToggle.setContentDescription("Show terminal");
    }

    public void openTermuxConnection() {
        startActivity(new Intent(this, TermuxConnectionActivity.class));
    }

    /** Only a folder from the system picker has a path Termux could open. */
    private void updateTerminalProject() {
        boolean pickedFolder = currentDirectoryUri != null
                && currentDirectoryUri.toString().equals(currentProjectPath);
        TerminalPanelHost.setProjectDirectory(this, pickedFolder ? currentDirectoryUri : null);
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

    /** Opens a file under the app's files dir, or closes the open file when {@code fileName} is null. */
    public void setCurrentFileName(String fileName) throws Exception{
        if (fileName == null) {
            saveOpenFile(false);
            currentFileUri = null;
            currentFileName = null;
            savedContent = null;
            CodeEditorController.DisableCodeEditor(this);
            return;
        }
        if (currentFileUri == null && fileName.equals(currentFileName)) {
            // Reloading it from disk would discard unsaved edits.
            CodeEditorController.EnableCodeEditor(this);
            return;
        }
        String content = FilesController.getFilesInside(fileName, this);
        saveOpenFile(false);
        showFile(null, fileName, content);
    }

    public void setCurrentProjectPath(String projectPath) throws Exception{
        currentProjectPath = projectPath;
        DirectoryTreeController.setEnabled(this);
        saveSessionToPrefs();
        updateTerminalProject();
    }

    public String getCurrentFileName(){
        return currentFileName;
    }
    
    public String getCurrentProjectPath(){
        return currentProjectPath;
    }
}
