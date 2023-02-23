import 'package:flutter/material.dart';
import 'package:visual_studio_for_android/project.dart';
import 'package:visual_studio_for_android/project_menu.dart';
import 'package:visual_studio_for_android/side_menu.dart';
import 'code_editor.dart';
import 'color_palettes.dart';
import 'dummy_widget.dart';
import 'initialize_project.dart';

class WidgetFactory {

  bool dummy = false;

  getDrawer({required Project project, required VoidCallback notifyApp}) {
    if(project.empty) return const EmptyProjectMenu();

    return ProjectMenu(project: project, notifyApp: notifyApp);
  }

  getBody({required Project project, required VoidCallback notifyApp}) {
    if(project.empty) {
      return InitializeProject(project: project, notifyApp: notifyApp);
    }
    return _getBodyChild(project, notifyApp);
  }

  getAppBar({required Project project, required VoidCallback notifyApp}) {
    if(project.empty) {
      return AppBar(
        backgroundColor: appBarColor,
      );
    }

    return AppBar(
      backgroundColor: appBarColor,
      title: Text(_getAppBarMessage(project)),
      actions: [
        IconButton(
          onPressed: () {
            project.saveCurrentFile();
          },
          icon: const Icon(Icons.save)),
        Container(
          margin: const EdgeInsets.only(right: 10),
          child: TextButton(
              onPressed: () {
                project.close();
                notifyApp();
              },
              child: const Text('Exit')
          ),
        )
      ]
    );
  }

  _getBodyChild(Project project, VoidCallback notifyApp) {
    if(project.currentFile == null) {
      return const Center(child: Text('No files opened'));
    } else {
      if(dummy == false) {
        dummy = true;
        return SingleChildScrollView(
          child: Container(
              margin: const EdgeInsets.symmetric(horizontal: 10, vertical: 0),
              child: CodeEditor(file: project.currentFile!)
          ),
        );
      } else {
        dummy = false;
        return DummyWidget(notifyApp: notifyApp);
      }

    }
  }

  String _getAppBarMessage(Project project) {
    if(project.currentFile == null) {
      return '';
    }

    return project.currentFile!.name;
  }
}