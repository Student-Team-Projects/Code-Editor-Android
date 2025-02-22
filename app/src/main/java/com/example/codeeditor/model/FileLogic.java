package com.example.codeeditor.model;

import android.content.Context;
import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;


public class FileLogic {


    public static String initGitRepository(Context context, Uri directoryUri) {
        try {
            File repoDir = getFileFromUri(context, directoryUri);
            if (repoDir == null) {
                return "Could not resolve a filesystem path from directoryUri.";
            }

            if (!repoDir.exists()) {
                boolean created = repoDir.mkdirs();
                if (!created) {
                    return "Unable to create directory: " + repoDir.getAbsolutePath();

                }
            }

            Git git = Git.init()
                    .setDirectory(repoDir)
                    .call();
            git.close();

            return "Git repository initialized at: " + repoDir.getAbsolutePath();

        } catch (GitAPIException e) {
            e.printStackTrace();
            return "initGitRepository error: " + e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            return "initGitRepository exception: " + e.getMessage();
        }
    }

    public static String gitAdd(Context context, Uri directoryUri, Uri fileUri) {
        try {
            File repoDir = getFileFromUri(context, directoryUri);
            if (repoDir == null) {
                return "Could not resolve a filesystem path from directoryUri.";
            }
            File fileToAdd = getFileFromUri(context, fileUri);
            if (fileToAdd == null) {
                return "Could not resolve a filesystem path from fileUri.";
            }

            Git git = Git.open(repoDir);


            String filePattern = getRelativePath(repoDir, fileToAdd);
            git.add()
                    .addFilepattern(filePattern)
                    .call();

            git.close();
            return "File added to Git index: " + fileToAdd.getName();

        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
            return "gitAdd error: " + e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            return "gitAdd exception: " + e.getMessage();
        }
    }

    public static String gitCommit(Context context, Uri directoryUri, String message) {
        try {
            File repoDir = getFileFromUri(context, directoryUri);
            if (repoDir == null) {
                return "Could not resolve a filesystem path from directoryUri.";
            }

            Git git = Git.open(repoDir);
            git.commit()
                    .setMessage(message)
                    .call();

            git.close();
            return "Commit succeeded: " + message;

        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
            return "gitCommit error: " + e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            return "gitCommit exception: " + e.getMessage();
        }
    }

    public static String gitPush(Context context, Uri directoryUri) {
        try {
            File repoDir = getFileFromUri(context, directoryUri);
            if (repoDir == null) {
                return "Could not resolve a filesystem path from directoryUri.";
            }

            Git git = Git.open(repoDir);

            git.push().call();
            git.close();

            return "Push to remote succeeded";

        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
            return "gitPush error: " + e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            return "gitPush exception: " + e.getMessage();
        }
    }

    private static File getFileFromUri(Context context, Uri uri) {
        return new File(uri.getPath());
    }


    private static String getRelativePath(File folder, File file) {
        String folderPath = folder.getAbsolutePath();
        String filePath   = file.getAbsolutePath();

        if (!folderPath.endsWith(File.separator)) {
            folderPath += File.separator;
        }

        if (filePath.startsWith(folderPath)) {
            return filePath.substring(folderPath.length());
        } else {
            return file.getName();
        }
    }
}
