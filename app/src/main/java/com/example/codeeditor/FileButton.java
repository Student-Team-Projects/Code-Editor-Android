package com.example.codeeditor;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;

public class FileButton {

    public static Button getFileButton(MainActivity mainScreen){
        View view = mainScreen.findViewById(R.id.hamburger_menu);
        if (view instanceof Button) {
            Button fileButton = (Button) view;
            fileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Context wrapper = new ContextThemeWrapper(mainScreen, R.style.CustomPopupMenu);
                    PopupMenu popup = new PopupMenu(wrapper, v);
                    popup.getMenuInflater().inflate(R.menu.menu_file, popup.getMenu());

                    if (mainScreen.getCurrentFileName() == null) {
                        MenuItem saveItem = popup.getMenu().findItem(R.id.action_save_file);
                        if (saveItem != null) {
                            saveItem.setEnabled(false);
                        }
                    }

                    popup.getMenu().setGroupDividerEnabled(true);
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
                            return onOptionsItemSelected(item, mainScreen);
                        }
                    });
                    popup.show();
                }
            });
            return fileButton;
        }
        return null;
    }

    static public boolean onOptionsItemSelected(MenuItem item, MainActivity mainScreen) {
        int id = item.getItemId();

        if (id == R.id.action_open_file) {
            openFile(mainScreen);
            return true;
        } else if (id == R.id.action_new_file) {
            createNewFile(mainScreen);
            return true;
        } else if (id == R.id.action_close_file) {
            closeCurrentFile(mainScreen);
            return true;
        } else if (id == R.id.action_save_file) {
            saveFile(mainScreen);
            return true;
        } else if (id == R.id.action_open_project) {
            openProject(mainScreen);
            return true;
        } else if (id == R.id.action_close_project) {
            closeProject(mainScreen);
            return true;
        } else {
            return false;
        }
    }

    static private void openFile(MainActivity mainScreen) {
        mainScreen.disableMainLayout();
        OpenFileController.setEnabled(mainScreen);
    }

    static private void createNewFile(MainActivity mainScreen) {
        mainScreen.disableMainLayout();
        if(mainScreen.getCurrentProjectPath() != null) {
            try {
                mainScreen.setCurrentProjectPath(mainScreen.getCurrentProjectPath());
            } catch (Exception e) {
                return;
            }
        }
        NewFileController.setEnabled(mainScreen);
    }

    static private void closeCurrentFile(MainActivity mainScreen) {
        if(mainScreen.getCurrentFileName() == null) {
            return;
        }
        saveFile(mainScreen);
        try {
            mainScreen.setCurrentFileName(null);
        } catch (Exception e) {
            return;
        }
    }

    static private void saveFile(MainActivity mainScreen){
        String fileName = mainScreen.getCurrentFileName();
        CodeEditorController.getCode(mainScreen, content -> {
            FilesController.saveFile(fileName, content, mainScreen);
        });
    }

    static private void openProject(MainActivity mainScreen) {
        mainScreen.disableMainLayout();
        OpenProjectController.setEnabled(mainScreen);
    }

    static private void closeProject(MainActivity mainScreen){
        try {
            mainScreen.setCurrentProjectPath(null);
        } catch (Exception e) {
            return;
        }
    }
}
