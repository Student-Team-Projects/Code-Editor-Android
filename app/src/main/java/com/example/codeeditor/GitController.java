package com.example.codeeditor;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.TagOpt;

import java.io.File;

public class GitController {

    public static void gitClone(String gitLink, MainActivity mainScreen) throws Exception {
        if(gitLink.lastIndexOf('/') == -1 || gitLink.lastIndexOf('.') == -1) throw new Exception("Invalid format for git link");
        String gitName = gitLink.substring(gitLink.lastIndexOf('/') + 1, gitLink.lastIndexOf('.'));

        try {
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... voids) {
                    File directory = mainScreen.getFilesDir();
                    File newDirectory = new File(directory, "Projects");
                    File newProjectDirectory = new File(newDirectory, gitName);
                    try {
                        Git git = Git.cloneRepository()
                                .setDirectory(newProjectDirectory)
                                .setURI(gitLink)
                                .setCloneAllBranches(true)
                                .setTagOption(TagOpt.FETCH_TAGS)
                                .call();
                    } catch (Exception e) {
                        return e.getMessage();
                    }
                    return "Git Clone Succeeded";
                }

                @Override
                protected void onPostExecute(String result) {
                    Toast.makeText(mainScreen, result, Toast.LENGTH_SHORT).show();
                    if(result.equals("Git Clone Succeeded")) {
                        try {
                            mainScreen.setCurrentProjectPath("Projects/" + gitName);
                        } catch (Exception e) {
                            return;
                        }
                    }
                }
            }.execute();
        }catch (Exception e) {
            Toast.makeText(mainScreen, "Clone Error", Toast.LENGTH_SHORT).show();
        }
    }

    public static void gitFetch(String gitLink, MainActivity mainScreen){

    }
}
