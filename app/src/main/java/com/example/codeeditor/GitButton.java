package com.example.codeeditor;

import android.content.Context;
import android.view.ContentInfo;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;

import androidx.core.view.MenuCompat;

public class GitButton {

    public static Button getGitButton(MainActivity mainScreen){
        Button gitButton = mainScreen.findViewById(R.id.git_button);

        gitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context wrapper = new ContextThemeWrapper(mainScreen, R.style.CustomPopupMenu);
                PopupMenu popup = new PopupMenu(wrapper, v);
                popup.getMenuInflater().inflate(R.menu.menu_git, popup.getMenu());
                MenuCompat.setGroupDividerEnabled(popup.getMenu(), true);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        return onOptionsItemSelected(item, mainScreen);
                    }
                });
                popup.show();
            }
        });

        return gitButton;
    }

    static public boolean onOptionsItemSelected(MenuItem item, MainActivity mainScreen) {
        switch (item.getItemId()) {
            case R.id.action_commit:
                gitCommit();
                return true;
            case R.id.action_fetch:
                gitFetch();
                return true;
            case R.id.action_push:
                gitPush();
                return true;
            case R.id.action_clone:
                gitClone(mainScreen);
                return true;
            default:
                return false;
        }
    }

    static private void gitCommit() {
    }

    static private void gitFetch() {
    }

    static private void gitPush() {
    }

    static private void gitClone(MainActivity mainScreen) {
        mainScreen.disableMainLayout();
        GitCloneController.setEnabled(mainScreen);
    }
}
