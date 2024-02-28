package com.example.codeeditor;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

public class FilesController {

    protected static void createFile(String fileName, MainActivity mainScreen) throws Exception {
        File directory = FilesController.getDirectory(fileName, mainScreen);
        String realFileName = FilesController.getFileName(fileName);
        File file = new File(directory, realFileName);
        if (file.exists()) {
            throw new Exception("File " + fileName + " already exists");
        } else {
            String templateText = "";
            if (fileName.endsWith(".cpp")) {
                templateText = "#include<iostream>\n\nint main(){\n\tstd::cout<<\"Hello World!\\n\";\n\treturn 0;\n}";
            }
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(templateText.getBytes());
                fileOutputStream.close();
                Toast.makeText(mainScreen, "File created", Toast.LENGTH_SHORT).show();

                String filePath = new File(mainScreen.getFilesDir(), fileName).getAbsolutePath();
                Log.d("Debug", filePath);
//                /data/user/0/com.example.codeeditor/files/auf
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(mainScreen, "Error creating file", Toast.LENGTH_SHORT).show();
            }
            Toast.makeText(mainScreen, "File created", Toast.LENGTH_SHORT).show();
        }
    }

    public static String getFilesInside(String fileName, MainActivity mainScreen) throws Exception {
        File directory = FilesController.getDirectory(fileName, mainScreen);
        String realFileName = FilesController.getFileName(fileName);
        File file = new File(directory, realFileName);
        if (!file.exists()) {
            throw new Exception("File " + fileName + " do not exists");
        } else
        {
            StringBuilder stringBuilder = new StringBuilder();
            FileInputStream fileInputStream = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(isr);
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append('\n');
            }
            fileInputStream.close();
            return stringBuilder.toString();
        }
    }

    public static void saveFile(String fileName, String content, MainActivity mainScreen) {
        File directory = FilesController.getDirectory(fileName, mainScreen);
        String realFileName = FilesController.getFileName(fileName);
        File file = new File(directory, realFileName);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(content.getBytes());
            fileOutputStream.close();
            Toast.makeText(mainScreen, "File saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(mainScreen, "Error saving file", Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean isFileExist(String fileName, MainActivity mainScreen) {
        File directory = FilesController.getDirectory(fileName, mainScreen);
        if (directory == null) {
            return false;
        }
        String realFileName = FilesController.getFileName(fileName);
        return new File(directory, realFileName).exists();
    }

    public static Map<String, ?> getAllLocalFiles(MainActivity mainScreen) {
        SharedPreferences sharedPreferences = mainScreen.getApplicationContext().getSharedPreferences("MyLocalFiles", MODE_PRIVATE);
        return sharedPreferences.getAll();
    }

    public static File[] getAllLocalFiles(MainActivity mainScreen, String path) {
        File directory = FilesController.getDirectory(path, mainScreen);
        String realFileName = FilesController.getFileName(path);
        File file = new File(directory, realFileName);
        return file.listFiles();
    }

    public static void createProjectsDirectory(MainActivity mainScreen){
        File directory = mainScreen.getFilesDir();
        File newDirectory = new File(directory, "Projects");
        if (!newDirectory.exists()) {
            newDirectory.mkdir();
        }
    }

    public static File getFileByPath(String path, MainActivity mainScreen) {
        File directory = FilesController.getDirectory(path, mainScreen);
        String realFileName = FilesController.getFileName(path);
        File file = new File(directory, realFileName);
        if(file.exists()){
            return file;
        }
        else{
            return null;
        }
    }

    public static boolean isDirectory(String directoryPath, MainActivity mainScreen){
        File directory = FilesController.getDirectory(directoryPath, mainScreen);
        String realFileName = FilesController.getFileName(directoryPath);
        File file = new File(directory, realFileName);
        if(!file.exists() || !file.isDirectory()){
            return false;
        }
        return true;
    }

    private static File getDirectory(String fileName, MainActivity mainScreen) {
        String fileNameCopy = String.copyValueOf(fileName.toCharArray());
        File currentDirectory = mainScreen.getFilesDir();
        while(fileNameCopy.contains("/")){
            String subDirectoryName = fileNameCopy.substring(0, fileNameCopy.indexOf('/'));
            File subDirectory = new File(currentDirectory, subDirectoryName);
            if (!subDirectory.exists()) {
                return null;
            }
            currentDirectory = subDirectory;
            fileNameCopy = fileNameCopy.substring(fileNameCopy.indexOf('/') + 1);;
        }
        return currentDirectory;
    }

    private static String getFileName(String fileName){
        return fileName.substring(fileName.lastIndexOf('/') + 1);
    }
}
