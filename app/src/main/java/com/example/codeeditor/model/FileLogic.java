package com.example.codeeditor.model;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.documentfile.provider.DocumentFile;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;


public class FileLogic {

    /**
     * Initialize a new Git repository in the directory specified by directoryUri.
     */
    public static String initGitRepository(Context context, Uri directoryUri) {
        try {
            System.out.println("Initializing Git repository...");

            if (directoryUri == null) {
                return "Error: directoryUri is NULL!";
            }

            System.out.println("Directory URI received: " + directoryUri.toString());

            File repoDir = getFileFromUri(context, directoryUri);

            if (repoDir == null) {
                return "Could not resolve a filesystem path from directoryUri: " + directoryUri.toString();
            }

            System.out.println("Resolved repo directory: " + repoDir.getAbsolutePath());

            if (!repoDir.exists()) {
                boolean created = repoDir.mkdirs();
                if (!created) {
                    return "Unable to create directory: " + repoDir.getAbsolutePath();
                }
            }

            System.out.println("Creating Git repository in: " + repoDir.getAbsolutePath());

            Git git = Git.init()
                    .setDirectory(repoDir)
                    .call();
            git.close();

            return "Git repository initialized at: " + repoDir.getAbsolutePath();

        } catch (GitAPIException e) {
            e.printStackTrace();
            return "initGitRepository GitAPIException: " + e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            return "initGitRepository exception: " + e.getMessage();
        }
    }




    /**
     * Add the file at fileUri to the Git index.
     */
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

    /**
     * Commit changes in the Git repository with a given commit message.
     */
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

    /**
     * Push changes to remote repository.
     * If your remote requires credentials, you will need to set them here.
     */
    public static String gitPush(Context context, Uri directoryUri) {
        try {
            File repoDir = getFileFromUri(context, directoryUri);
            if (repoDir == null) {
                return "Could not resolve a filesystem path from directoryUri.";
            }

            Git git = Git.open(repoDir);

            // If you need to specify a remote name or credentials:
            // git.push()
            //    .setRemote("origin")
            //    .setCredentialsProvider(
            //         new UsernamePasswordCredentialsProvider("username", "password")
            //    )
            //    .call();

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
        System.out.println("Resolving URI: " + uri.toString());

        if ("file".equalsIgnoreCase(uri.getScheme())) {
            File file = new File(uri.getPath());
            System.out.println("Resolved file (file scheme): " + file.getAbsolutePath());
            return file;
        }

        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {MediaStore.Files.FileColumns.DATA};
            try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
                    String filePath = cursor.getString(columnIndex);
                    System.out.println("Resolved file path (content scheme): " + filePath);
                    return new File(filePath);
                } else {
                    System.out.println("Cursor was empty or invalid.");
                }
            }
        }

        System.out.println("Failed to resolve file from URI.");
        return null;
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
