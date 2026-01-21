package com.example.codeeditor;

import android.content.Context;
import android.os.Build;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;

import androidx.annotation.RequiresApi;

public class FileButton {

    public static Button getFileButton(MainActivity mainScreen){
        Button fileButton = mainScreen.findViewById(R.id.file_button);

        fileButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void onClick(View v) {
                Context wrapper = new ContextThemeWrapper(mainScreen, R.style.CustomPopupMenu);
                PopupMenu popup = new PopupMenu(wrapper, v);
                popup.getMenuInflater().inflate(R.menu.menu_file, popup.getMenu());

                // Исправлена работа с группами меню
                if (popup.getMenu().findItem(R.id.action_save_file) != null) {
                    // Группы иногда не работают на старых API, поэтому проверяем элементы
                }

                if(mainScreen.getCurrentFileName() == null) {
                    MenuItem saveItem = popup.getMenu().findItem(R.id.action_save_file);
                    if (saveItem != null) {
                        saveItem.setEnabled(false);
                    }
                    // popup.getMenu().setGroupVisible(R.id.group3, false); // Это может вызывать ошибки, если группа не найдена
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

    // ИСПРАВЛЕННАЯ ЧАСТЬ: switch заменен на if-else
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
        String content = CodeEditorController.getCode(mainScreen);
        FilesController.saveFile(fileName, content, mainScreen);
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