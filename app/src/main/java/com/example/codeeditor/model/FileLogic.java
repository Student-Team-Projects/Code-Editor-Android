package com.example.codeeditor.model;

import android.content.Context;
import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.util.Set;


public class FileLogic {

    /**
     * Initialize a new Git repository in the specified directory.
     */
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

            // Check if already a git repo
            File gitDir = new File(repoDir, ".git");
            if (gitDir.exists() && gitDir.isDirectory()) {
                return "Repository already exists at: " + repoDir.getAbsolutePath();
            }

            Git git = Git.init()
                    .setDirectory(repoDir)
                    .call();
            git.close();

            return "Git repository initialized at: " + repoDir.getAbsolutePath();

        } catch (GitAPIException e) {
            e.printStackTrace();
            String msg = e.getMessage();
            return "Git init error: " + (msg != null ? msg : e.getClass().getSimpleName());
        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage();
            return "Git init exception: " + (msg != null ? msg : e.getClass().getSimpleName());
        }
    }

    /**
     * Check if a directory is a Git repository.
     */
    public static boolean isGitRepository(Context context, Uri directoryUri) {
        try {
            File repoDir = getFileFromUri(context, directoryUri);
            if (repoDir == null) return false;
            
            File gitDir = new File(repoDir, ".git");
            return gitDir.exists() && gitDir.isDirectory();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Find the Git repository root by walking up the directory tree.
     */
    private static File findGitRoot(File startDir) {
        File current = startDir;
        while (current != null) {
            File gitDir = new File(current, ".git");
            if (gitDir.exists() && gitDir.isDirectory()) {
                return current;
            }
            current = current.getParentFile();
        }
        return null;
    }

    /**
     * Add a specific file to the Git staging area.
     */
    public static String gitAdd(Context context, Uri directoryUri, Uri fileUri) {
        try {
            File repoDir = getFileFromUri(context, directoryUri);
            if (repoDir == null) {
                return "Could not resolve repository path.";
            }
            
            // Find actual git root (in case user opened a subdirectory)
            File gitRoot = findGitRoot(repoDir);
            if (gitRoot == null) {
                return "Not a Git repository. Please initialize first.";
            }
            
            File fileToAdd = getFileFromUri(context, fileUri);
            if (fileToAdd == null) {
                return "Could not resolve file path.";
            }

            if (!fileToAdd.exists()) {
                return "File does not exist: " + fileToAdd.getName();
            }

            Git git = Git.open(gitRoot);

            String filePattern = getRelativePath(gitRoot, fileToAdd);
            if (filePattern.isEmpty()) {
                git.close();
                return "File is outside the repository.";
            }
            
            git.add()
                    .addFilepattern(filePattern)
                    .call();

            git.close();
            return "Added to staging: " + filePattern;

        } catch (IOException e) {
            e.printStackTrace();
            String msg = e.getMessage();
            return "Git add error: " + (msg != null ? msg : e.getClass().getSimpleName());
        } catch (GitAPIException e) {
            e.printStackTrace();
            String msg = e.getMessage();
            return "Git add error: " + (msg != null ? msg : e.getClass().getSimpleName());
        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage();
            return "Git add exception: " + (msg != null ? msg : e.getClass().getSimpleName());
        }
    }

    /**
     * Add all changed files to the Git staging area.
     */
    public static String gitAddAll(Context context, Uri directoryUri) {
        try {
            File repoDir = getFileFromUri(context, directoryUri);
            if (repoDir == null) {
                return "Could not resolve repository path.";
            }
            
            File gitRoot = findGitRoot(repoDir);
            if (gitRoot == null) {
                return "Not a Git repository. Please initialize first.";
            }

            Git git = Git.open(gitRoot);

            // Add all new and modified files
            git.add()
                    .addFilepattern(".")
                    .call();
            
            // Also stage deleted files
            git.add()
                    .addFilepattern(".")
                    .setUpdate(true)
                    .call();

            git.close();
            return "All changes staged for commit.";

        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
            String msg = e.getMessage();
            return "Git add all error: " + (msg != null ? msg : e.getClass().getSimpleName());
        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage();
            return "Git add all exception: " + (msg != null ? msg : e.getClass().getSimpleName());
        }
    }

    /**
     * Get the status of the Git repository.
     */
    public static String gitStatus(Context context, Uri directoryUri) {
        try {
            File repoDir = getFileFromUri(context, directoryUri);
            if (repoDir == null) {
                return "Could not resolve repository path.";
            }
            
            File gitRoot = findGitRoot(repoDir);
            if (gitRoot == null) {
                return "Not a Git repository.";
            }

            Git git = Git.open(gitRoot);
            Status status = git.status().call();

            StringBuilder sb = new StringBuilder();
            
            Set<String> staged = status.getAdded();
            Set<String> changed = status.getChanged();
            Set<String> removed = status.getRemoved();
            Set<String> modified = status.getModified();
            Set<String> untracked = status.getUntracked();

            int stagedCount = staged.size() + changed.size() + removed.size();
            int unstagedCount = modified.size();
            int untrackedCount = untracked.size();
            
            if (stagedCount == 0 && unstagedCount == 0 && untrackedCount == 0) {
                git.close();
                return "Working tree clean. Nothing to commit.";
            }

            if (stagedCount > 0) {
                sb.append("Staged: ").append(stagedCount).append(" file(s)\n");
            }
            if (unstagedCount > 0) {
                sb.append("Modified: ").append(unstagedCount).append(" file(s)\n");
            }
            if (untrackedCount > 0) {
                sb.append("Untracked: ").append(untrackedCount).append(" file(s)");
            }

            git.close();
            return sb.toString().trim();

        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
            String msg = e.getMessage();
            return "Git status error: " + (msg != null ? msg : e.getClass().getSimpleName());
        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage();
            return "Git status exception: " + (msg != null ? msg : e.getClass().getSimpleName());
        }
    }

    /**
     * Commit staged changes with the given message.
     */
    public static String gitCommit(Context context, Uri directoryUri, String message) {
        return gitCommit(context, directoryUri, message, "Code Editor User", "user@codeeditor.app");
    }
    
    /**
     * Commit staged changes with the given message and author info.
     */
    public static String gitCommit(Context context, Uri directoryUri, String message, 
                                   String authorName, String authorEmail) {
        try {
            if (directoryUri == null) {
                return "No directory selected. Open a folder first.";
            }
            
            if (message == null || message.trim().isEmpty()) {
                return "Commit message cannot be empty.";
            }
            
            File repoDir = getFileFromUri(context, directoryUri);
            if (repoDir == null) {
                return "Could not resolve repository path.";
            }

            File gitRoot = findGitRoot(repoDir);
            if (gitRoot == null) {
                return "Not a Git repository. Please initialize first.";
            }

            Git git = Git.open(gitRoot);
            
            // Check if there are staged changes
            Status status = git.status().call();
            if (status.getAdded().isEmpty() && 
                status.getChanged().isEmpty() && 
                status.getRemoved().isEmpty()) {
                git.close();
                return "Nothing to commit. Stage files first using 'Git Add'.";
            }
            
            // Set author and committer info (required by JGit)
            git.commit()
                    .setMessage(message.trim())
                    .setAuthor(authorName, authorEmail)
                    .setCommitter(authorName, authorEmail)
                    .call();

            git.close();
            return "Committed: " + message.trim();

        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
            String errorMsg = e.getMessage();
            return "Git commit error: " + (errorMsg != null ? errorMsg : e.getClass().getSimpleName());
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = e.getMessage();
            return "Git commit exception: " + (errorMsg != null ? errorMsg : e.getClass().getSimpleName());
        }
    }

    /**
     * Push commits to the remote repository.
     */
    public static String gitPush(Context context, Uri directoryUri, String username, String password) {
        try {
            if (directoryUri == null) {
                return "No directory selected. Open a folder first.";
            }
            
            File repoDir = getFileFromUri(context, directoryUri);
            if (repoDir == null) {
                return "Could not resolve repository path.";
            }

            File gitRoot = findGitRoot(repoDir);
            if (gitRoot == null) {
                return "Not a Git repository.";
            }

            Git git = Git.open(gitRoot);
            
            // Check if remote is configured
            if (git.getRepository().getRemoteNames().isEmpty()) {
                git.close();
                return "No remote configured. Add a remote first.";
            }

            // Push with or without credentials
            if (username != null && !username.isEmpty() && 
                password != null && !password.isEmpty()) {
                CredentialsProvider cp = new UsernamePasswordCredentialsProvider(username, password);
                git.push()
                        .setCredentialsProvider(cp)
                        .call();
            } else {
                git.push().call();
            }
            
            git.close();
            return "Push successful!";

        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
            String msg = e.getMessage();
            if (msg != null && msg.contains("not authorized")) {
                return "Authentication failed. Check username/token.";
            }
            return "Git push error: " + (msg != null ? msg : e.getClass().getSimpleName());
        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage();
            return "Git push exception: " + (msg != null ? msg : e.getClass().getSimpleName());
        }
    }
    
    /**
     * Push commits to remote without credentials (for SSH or cached credentials).
     */
    public static String gitPush(Context context, Uri directoryUri) {
        return gitPush(context, directoryUri, null, null);
    }

    /**
     * Add a remote repository URL.
     */
    public static String gitAddRemote(Context context, Uri directoryUri, String remoteName, String remoteUrl) {
        try {
            if (directoryUri == null) {
                return "No directory selected.";
            }
            
            if (remoteName == null || remoteName.trim().isEmpty()) {
                return "Remote name cannot be empty.";
            }
            
            if (remoteUrl == null || remoteUrl.trim().isEmpty()) {
                return "Remote URL cannot be empty.";
            }
            
            File repoDir = getFileFromUri(context, directoryUri);
            if (repoDir == null) {
                return "Could not resolve repository path.";
            }

            File gitRoot = findGitRoot(repoDir);
            if (gitRoot == null) {
                return "Not a Git repository.";
            }

            Git git = Git.open(gitRoot);
            
            // Check if remote already exists
            if (git.getRepository().getRemoteNames().contains(remoteName.trim())) {
                git.close();
                return "Remote '" + remoteName + "' already exists.";
            }
            
            git.remoteAdd()
                    .setName(remoteName.trim())
                    .setUri(new org.eclipse.jgit.transport.URIish(remoteUrl.trim()))
                    .call();

            git.close();
            return "Remote '" + remoteName + "' added: " + remoteUrl;

        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage();
            return "Add remote error: " + (msg != null ? msg : e.getClass().getSimpleName());
        }
    }

    /**
     * Converts a SAF (Storage Access Framework) content URI to a File object.
     * 
     * This works when the app has MANAGE_EXTERNAL_STORAGE permission (Android 11+)
     * or appropriate storage permissions on older versions.
     * 
     * Content URIs from the external storage provider follow patterns like:
     * - content://com.android.externalstorage.documents/tree/primary:FolderPath
     * - content://com.android.externalstorage.documents/document/primary:FilePath
     * 
     * @param context The application context
     * @param uri The content URI to convert
     * @return A File object representing the actual filesystem path, or null if conversion fails
     */
    private static File getFileFromUri(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }

        String uriString = uri.toString();
        
        // Handle file:// URIs directly
        if ("file".equals(uri.getScheme())) {
            return new File(uri.getPath());
        }

        // Handle content:// URIs from external storage provider
        if ("content".equals(uri.getScheme()) && 
            uriString.contains("com.android.externalstorage.documents")) {
            
            // Extract the path portion (e.g., "primary:FolderName" or "XXXX-XXXX:FolderName")
            String docId = null;
            
            // Try to get the document ID from the URI path
            String path = uri.getPath();
            if (path != null) {
                // Handle /tree/primary:path or /document/primary:path formats
                if (path.contains("/tree/")) {
                    int treeIndex = path.indexOf("/tree/");
                    String afterTree = path.substring(treeIndex + 6);
                    // If there's also a /document/ part, use that for the actual path
                    if (afterTree.contains("/document/")) {
                        int docIndex = afterTree.indexOf("/document/");
                        docId = afterTree.substring(docIndex + 10);
                    } else {
                        docId = afterTree;
                    }
                } else if (path.contains("/document/")) {
                    int docIndex = path.indexOf("/document/");
                    docId = path.substring(docIndex + 10);
                }
            }

            if (docId != null) {
                // Decode URL encoding (e.g., %20 -> space)
                try {
                    docId = java.net.URLDecoder.decode(docId, "UTF-8");
                } catch (Exception e) {
                    // Continue with encoded version if decoding fails
                }

                // Split into volume and path (e.g., "primary:FolderName")
                String[] split = docId.split(":", 2);
                String volumeId = split[0];
                String relativePath = split.length > 1 ? split[1] : "";

                // Map volume ID to actual storage path
                String basePath;
                if ("primary".equalsIgnoreCase(volumeId)) {
                    // Primary external storage
                    basePath = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
                } else {
                    // Secondary storage (SD card, USB, etc.)
                    basePath = "/storage/" + volumeId;
                }

                // Construct the full path
                if (relativePath.isEmpty()) {
                    return new File(basePath);
                } else {
                    return new File(basePath, relativePath);
                }
            }
        }

        // Fallback: try the path directly (may not work for content URIs)
        String path = uri.getPath();
        if (path != null) {
            return new File(path);
        }

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
