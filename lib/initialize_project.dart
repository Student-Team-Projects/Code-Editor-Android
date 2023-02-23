import 'dart:io';
import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:visual_studio_for_android/project.dart';
import 'package:filesystem_picker/filesystem_picker.dart';
import 'package:visual_studio_for_android/snackbar_factory.dart';

class InitializeProject extends StatelessWidget {
  const InitializeProject({required this.project, super.key, required this.notifyApp});

  final Project project;
  final VoidCallback notifyApp;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.center,
        children: <Widget>[
          const Spacer(),
          InitializeButton(
              onPressed: () {
                _openFile(context);
              },
              text: const Text("Open File")
          ),
          const Spacer(),
          InitializeButton(
              onPressed: () {
                _newFile(context);
              },
              text: const Text("New File")
          ),
          const Spacer(),
          InitializeButton(
            onPressed: () {
              _openFolder(context);
            },
            text: const Text("Open Folder"),
          ),
          const Spacer(),
        ]
      )
    );
  }

  void _openFile(BuildContext context) async {
    if (await Permission.manageExternalStorage.request().isGranted) {
      Directory rootDir = Directory('/storage/emulated/0');

      String? selectedFilePath = await FilesystemPicker.open(
        title: 'Open file',
        context: context,
        rootDirectory: rootDir,
        fsType: FilesystemType.file,
      );

      // user canceled the picker
      if (selectedFilePath == null) {
        return;
      }

      await project.initializeOpenSingleFile(selectedFilePath);
      notifyApp();
    }
  }

  void _newFile(BuildContext context) async {
    if (await Permission.manageExternalStorage.request().isGranted) {
      Directory rootDir = Directory('/storage/emulated/0');

      String? selectedDirPath = await FilesystemPicker.open(
        title: 'New file',
        context: context,
        rootDirectory: rootDir,
        fsType: FilesystemType.folder,
        pickText: 'Create file in this folder',
      );

      if (selectedDirPath == null) {
        return;
      }

      var fileName = await _getNewFileName(context);

      if(fileName == null || fileName.isEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBarFactory.getInfoSnackBar('File name is not correct'));
        return;
      }
      if(! await _isUnique(fileName, selectedDirPath)) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBarFactory.getInfoSnackBar('File already exists'));
        return;
      }

      String filePath = '$selectedDirPath/$fileName';
      await project.initializeNewSingleFile(filePath);
      notifyApp();
    }
  }

  void _openFolder(BuildContext context) async {

    if (await Permission.manageExternalStorage.request().isGranted) {
      Directory rootDir = Directory('/storage/emulated/0');

      String? selectedDirPath = await FilesystemPicker.open(
        title: 'Open folder',
        context: context,
        rootDirectory: rootDir,
        fsType: FilesystemType.folder,
        pickText: 'Select this folder',
      );

      // user canceled the picker
      if (selectedDirPath == null) {
        return;
      }

      try {
        await project.initializeDirectory(selectedDirPath);
      } catch (e) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBarFactory.getInfoSnackBar('Permission denied'));
      }
      notifyApp();
    }
  }

  Future<String?> _getNewFileName(BuildContext context) async {
    final controller = TextEditingController();
    var result = await _openNewFileNameDialog(context, controller);
    if(result != null) {
      result = result.trim();
    }
    return result;
  }

  Future<bool> _isUnique(String fileName, String directoryPath) async {
    final dir = Directory(directoryPath);
    final List<FileSystemEntity> dirEntities = await dir.list().toList();
    for(var entity in dirEntities) {
      if(entity is File) {
        if(fileName == entity.path.substring(entity.path.lastIndexOf('/')+1)) return false;
      }
    }

    return true;
  }

  Future<String?> _openNewFileNameDialog(BuildContext context, TextEditingController controller) => showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('File name'),
        content: TextField(
          autofocus: true,
          decoration: const InputDecoration(hintText: 'Enter file name'),
          controller: controller,
        ),
        actions: [
          TextButton(
            child: const Text('SUBMIT'),
            onPressed: () {
              _submit(context, controller);
            },
          )
        ],
      )

  );

  void _submit(BuildContext context, TextEditingController controller) {
    Navigator.of(context).pop(controller.text);
  }
}

class InitializeButton extends StatelessWidget {
  const InitializeButton({super.key, required this.onPressed, required this.text});

  final VoidCallback onPressed;
  final Text text;

  @override
  Widget build(BuildContext context) {
    return ElevatedButton(
      onPressed: onPressed,
      child: text
    );
  }
}