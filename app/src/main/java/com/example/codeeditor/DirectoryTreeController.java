package com.example.codeeditor;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DirectoryTreeController {

    private static final Map<String, Boolean> directoryColapsed = new HashMap<>();
    private static final Map<String, Long> lastClickTimes = new HashMap<>();

    private static int getThemeColor(Context context, int attrId) {
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(attrId, tv, true)) {
            return tv.data;
        }
        return context.getColor(android.R.color.white);
    }

    private static void generateTreeButtons(File currentFile, int depth, MainActivity mainScreen){
        if (currentFile == null) return;
        Button button = new Button(mainScreen);
        String repeated = new String(new char[depth]).replace("\0", "   ");
        String prefix = currentFile.isDirectory() ? "📁 " : "";
        button.setText(repeated + prefix + currentFile.getName());
        button.setBackground(mainScreen.getDrawable(R.drawable.transperent));

        if (currentFile.isDirectory()) {
            button.setTextColor(getThemeColor(mainScreen, androidx.appcompat.R.attr.colorPrimary));
        } else {
            button.setTextColor(getThemeColor(mainScreen, com.google.android.material.R.attr.colorOnSurface));
        }
        button.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);

        button.setGravity(Gravity.LEFT);
        button.setAllCaps(false);
        String pathKey = currentFile.getAbsolutePath();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentFile.isDirectory()) {
                    boolean collapsed = Boolean.TRUE.equals(directoryColapsed.get(pathKey));
                    directoryColapsed.put(pathKey, !collapsed);
                    generateExplorerTree(mainScreen);
                }
                else{
                    long now = System.currentTimeMillis();
                    Long lastTime = lastClickTimes.get(pathKey);
                    if (lastTime != null && (now - lastTime) < 400) {
                        try {
                            String relPath = currentFile.getPath();
                            if (relPath.contains("files/")) {
                                relPath = relPath.substring(relPath.indexOf("files/") + 6);
                            }
                            mainScreen.setCurrentFileName(relPath);
                        } catch (Exception e) {
                            Toast.makeText(mainScreen, currentFile.getPath(), Toast.LENGTH_SHORT).show();
                        }
                        lastClickTimes.remove(pathKey);
                    } else {
                        lastClickTimes.put(pathKey, now);
                        Toast.makeText(mainScreen, "Double-click to open file", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        LinearLayout scroller = mainScreen.findViewById(R.id.ExplorerScrollLayout);
        scroller.addView(button);

        if(currentFile.isDirectory() && Boolean.TRUE.equals(directoryColapsed.get(pathKey))){
            File[] files = currentFile.listFiles();
            if (files != null) {
                Arrays.sort(files, (f1, f2) -> {
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    return f1.getName().compareToIgnoreCase(f2.getName());
                });
                for(File item: files){
                    generateTreeButtons(item, depth + 1, mainScreen);
                }
            }
        }
    }

    public static void generateTreeFromDocumentFile(DocumentFile currentDoc, int depth, MainActivity mainScreen) {
        if (currentDoc == null) return;
        Button button = new Button(mainScreen);
        String repeated = new String(new char[depth]).replace("\0", "   ");
        String prefix = currentDoc.isDirectory() ? "📁 " : "";
        button.setText(repeated + prefix + currentDoc.getName());
        button.setBackground(mainScreen.getDrawable(R.drawable.transperent));

        if (currentDoc.isDirectory()) {
            button.setTextColor(getThemeColor(mainScreen, androidx.appcompat.R.attr.colorPrimary));
        } else {
            button.setTextColor(getThemeColor(mainScreen, com.google.android.material.R.attr.colorOnSurface));
        }
        button.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);

        button.setGravity(Gravity.LEFT);
        button.setAllCaps(false);
        String uriKey = currentDoc.getUri().toString();
        button.setOnClickListener(v -> {
            if (currentDoc.isDirectory()) {
                boolean collapsed = Boolean.TRUE.equals(directoryColapsed.get(uriKey));
                directoryColapsed.put(uriKey, !collapsed);
                refreshDocumentTree(mainScreen, currentDoc);
            } else {
                long now = System.currentTimeMillis();
                Long lastTime = lastClickTimes.get(uriKey);
                if (lastTime != null && (now - lastTime) < 400) {
                    mainScreen.openFileFromUri(currentDoc.getUri());
                    lastClickTimes.remove(uriKey);
                } else {
                    lastClickTimes.put(uriKey, now);
                    Toast.makeText(mainScreen, "Double-click to open file", Toast.LENGTH_SHORT).show();
                }
            }
        });

        LinearLayout scroller = mainScreen.findViewById(R.id.ExplorerScrollLayout);
        scroller.addView(button);

        if (currentDoc.isDirectory() && Boolean.TRUE.equals(directoryColapsed.get(uriKey))) {
            DocumentFile[] files = currentDoc.listFiles();
            if (files != null) {
                Arrays.sort(files, (f1, f2) -> {
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    return f1.getName().compareToIgnoreCase(f2.getName());
                });
                for (DocumentFile item : files) {
                    generateTreeFromDocumentFile(item, depth + 1, mainScreen);
                }
            }
        }
    }

    private static void refreshDocumentTree(MainActivity mainScreen, DocumentFile rootDoc) {
        LinearLayout scroller = mainScreen.findViewById(R.id.ExplorerScrollLayout);
        scroller.removeAllViews();
        DocumentFile parent = rootDoc;
        while (parent.getParentFile() != null) {
            parent = parent.getParentFile();
        }
        generateTreeFromDocumentFile(parent, 0, mainScreen);
    }

    private static void generateExplorerTree(MainActivity mainScreen){
        LinearLayout scroller = mainScreen.findViewById(R.id.ExplorerScrollLayout);
        scroller.removeAllViews();
        File directory = FilesController.getFileByPath(mainScreen.getCurrentProjectPath(), mainScreen);
        if (directory == null) {
            directory = mainScreen.getFilesDir();
        }
        generateTreeButtons(directory, 0, mainScreen);
    }

    public static void setEnabled(MainActivity mainScreen){
        mainScreen.findViewById(R.id.ExplorerLayout).setEnabled(true);
        mainScreen.findViewById(R.id.ExplorerLayout).setVisibility(View.VISIBLE);
        generateExplorerTree(mainScreen);
    }

    public static void setEnabledForDocumentFile(MainActivity mainScreen, DocumentFile docFile) {
        mainScreen.findViewById(R.id.ExplorerLayout).setEnabled(true);
        mainScreen.findViewById(R.id.ExplorerLayout).setVisibility(View.VISIBLE);
        refreshDocumentTree(mainScreen, docFile);
    }

    public static void setDisabled(MainActivity mainScreen){
        mainScreen.findViewById(R.id.ExplorerLayout).setEnabled(false);
        mainScreen.findViewById(R.id.ExplorerLayout).setVisibility(View.INVISIBLE);
        generateExplorerTree(mainScreen);
    }
}
