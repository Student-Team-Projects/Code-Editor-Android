package com.example.codeeditor;

import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class DirectoryTreeController {

    private static Map<String, Boolean> directoryColapsed;

    static {
        directoryColapsed = new HashMap<String, Boolean>();
    }

    private static void generateTreeButtons(File currentFile, int depth, MainActivity mainScreen){
        if (currentFile == null) return;
        Button button = new Button(mainScreen);
        String repeated = new String(new char[depth]).replace("\0", "   ");
        button.setText( repeated + currentFile.getName());
        button.setBackground(mainScreen.getDrawable(R.drawable.transperent));

        if(currentFile.isDirectory()) button.setTextColor(mainScreen.getColor(R.color.cool_blue));
        else button.setTextColor(mainScreen.getColor(R.color.white));

        button.setGravity(Gravity.LEFT);
        button.setAllCaps(false);
        String name = String.valueOf(currentFile.getName());
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentFile.isDirectory()) {
                    Boolean value = true;
                    if(Boolean.TRUE.equals(directoryColapsed.get(name))){
                        value = false;
                    }
                    directoryColapsed.put(name, value);
                    generateExplorerTree(mainScreen);
                }
                else{
                    try {
                        mainScreen.setCurrentFileName(currentFile.getPath().substring(currentFile.getPath().indexOf("files/") + 6));
                    } catch (Exception e) {
                        Toast.makeText(mainScreen, currentFile.getPath(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        LinearLayout scroller = mainScreen.findViewById(R.id.ExplorerScrollLayout);
        scroller.addView(button);

        if(currentFile.isDirectory() && Boolean.TRUE.equals(directoryColapsed.get(currentFile.getName()))){
            File[] files = currentFile.listFiles();
            if (files != null) {
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
        button.setText(repeated + (currentDoc.isDirectory() ? "📁 " : "📄 ") + currentDoc.getName());
        button.setBackground(mainScreen.getDrawable(R.drawable.transperent));

        if (currentDoc.isDirectory()) button.setTextColor(mainScreen.getColor(R.color.cool_blue));
        else button.setTextColor(mainScreen.getColor(R.color.white));

        button.setGravity(Gravity.LEFT);
        button.setAllCaps(false);
        String name = String.valueOf(currentDoc.getName());
        button.setOnClickListener(v -> {
            if (currentDoc.isDirectory()) {
                Boolean value = !Boolean.TRUE.equals(directoryColapsed.get(name));
                directoryColapsed.put(name, value);
                refreshDocumentTree(mainScreen, currentDoc);
            } else {
                mainScreen.openFileFromUri(currentDoc.getUri());
            }
        });

        LinearLayout scroller = mainScreen.findViewById(R.id.ExplorerScrollLayout);
        scroller.addView(button);

        if (currentDoc.isDirectory() && Boolean.TRUE.equals(directoryColapsed.get(name))) {
            DocumentFile[] files = currentDoc.listFiles();
            for (DocumentFile item : files) {
                generateTreeFromDocumentFile(item, depth + 1, mainScreen);
            }
        }
    }

    private static void refreshDocumentTree(MainActivity mainScreen, DocumentFile rootDoc) {
        LinearLayout scroller = mainScreen.findViewById(R.id.ExplorerScrollLayout);
        scroller.removeAllViews();
        generateTreeFromDocumentFile(rootDoc, 0, mainScreen);
    }

    private static void generateExplorerTree(MainActivity mainScreen){
        LinearLayout scroller = mainScreen.findViewById(R.id.ExplorerScrollLayout);
        scroller.removeAllViews();
        File directory = FilesController.getFileByPath(mainScreen.getCurrentProjectPath(), mainScreen);
        generateTreeButtons(directory, 0, mainScreen);
    }

    public static void setEnabled(MainActivity mainScreen){
        mainScreen.findViewById(R.id.ExplorerLayout).setEnabled(true);
        mainScreen.findViewById(R.id.ExplorerLayout).setVisibility(View.VISIBLE);
        directoryColapsed = new HashMap<>();
        generateExplorerTree(mainScreen);
    }

    public static void setEnabledForDocumentFile(MainActivity mainScreen, DocumentFile docFile) {
        mainScreen.findViewById(R.id.ExplorerLayout).setEnabled(true);
        mainScreen.findViewById(R.id.ExplorerLayout).setVisibility(View.VISIBLE);
        directoryColapsed = new HashMap<>();
        refreshDocumentTree(mainScreen, docFile);
    }

    public static void setDisabled(MainActivity mainScreen){
        mainScreen.findViewById(R.id.ExplorerLayout).setEnabled(false);
        mainScreen.findViewById(R.id.ExplorerLayout).setVisibility(View.INVISIBLE);
        generateExplorerTree(mainScreen);
    }
}
