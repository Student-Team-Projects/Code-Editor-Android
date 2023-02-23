import 'package:flutter/material.dart';
import 'package:visual_studio_for_android/project.dart';
import 'package:flutter_slidable/flutter_slidable.dart';

import 'color_palettes.dart';

class ProjectMenu extends StatefulWidget {

  final Project project;
  final VoidCallback notifyApp;
  final double recursivePaddingSize = 8;

  const ProjectMenu({required this.project, required this.notifyApp, Key? key}) : super(key: key);

  @override
  State<ProjectMenu> createState() => _ProjectMenuState();
}

class _ProjectMenuState extends State<ProjectMenu> {
  @override
  Widget build(BuildContext context) {
    return Drawer(
        backgroundColor: explorerItemsListBackgroundColor,
        child: Column(
          children: <Widget>[
            Expanded(
              child: ListView(
                padding: EdgeInsets.zero,
                children: _getMenuItems(),
              ),
            ),
            ListTile(
              leading: const Icon(Icons.settings),
              title: const Text('Settings'),
              onTap: () => {Navigator.of(context).pop()},
            ),
          ],
        )
    );
  }

  List<Widget> _getMenuItems() {
    var widgets = <Widget>[];
    widgets.add(_getDrawerHeader());
    widgets.add(_getOpenEditors());
    widgets.add(_getExplorer());
    //widgets.add(_getSearch());

    return widgets;
  }

  Widget _getDrawerHeader() {
    return const DrawerHeader(
      decoration: BoxDecoration(
          color: explorerBackgroundColor,
      ),
      child: Text(
        'Menu',
        style: TextStyle(fontSize: 25),
      ),
    );
  }

  Widget _getOpenEditors() {
    var files = widget.project.openFiles;

    return ExpansionTile(
      collapsedIconColor: Colors.white,
      title: const Text('Open Editors'),
      children: _getOpenEditorsTiles(files),
    );
  }

  List<Widget> _getOpenEditorsTiles(List<ProjectFile> files) {
    List<Widget> result = [];

    for(var file in files) {
      result.add(_getOpenEditorTile(file));
    }

    return result;
  }

  Padding _getOpenEditorTile(ProjectFile file) {
    return Padding(
      padding: EdgeInsets.only(left: widget.recursivePaddingSize),
      child: Slidable(
        endActionPane: ActionPane(
          motion: const ScrollMotion(),
          children: [
            SlidableAction(
              onPressed: (context) {
                widget.project.closeFile(file);
                widget.notifyApp();
              },
              backgroundColor: Colors.red,
              icon: Icons.close,
              label: 'Close',
            ),
          ],
        ),
        child: ListTile(
          title: Text(
              file.name,
              style: file == widget.project.currentFile ? const TextStyle(color: Colors.blueAccent, fontWeight: FontWeight.bold) : const TextStyle(color: Colors.white)
          ),
          onTap: () {
            widget.project.switchToFile(file);
            widget.notifyApp();
            Navigator.of(context).pop();
          },
        ),
      ),
    );
  }

  Widget _getExplorer() {
    var entities = widget.project.entities;

    return ExpansionTile(
      collapsedIconColor: Colors.white,
      title: const Text('Explorer'),
      children: _getExplorerTiles(entities),
    );
  }

  List<Widget> _getExplorerTiles(List<ProjectEntity> entities) {
    var widgets = <Widget>[];

    for(var entity in entities) {
      if(entity is ProjectFile) {
        widgets.add(_getListTile(entity));
      } else if (entity is ProjectDirectory){
        widgets.add(_getExpansionTile(entity));
      }
    }

    return widgets;
  }

  Widget _getListTile(ProjectFile file) {
    return Padding(
      padding: EdgeInsets.only(left: widget.recursivePaddingSize),
      child: ListTile(
        title: Text(file.name),
        onTap: () {
          widget.project.switchToFile(file);
          widget.notifyApp();
          Navigator.of(context).pop();
        },
      ),
    );
  }

  Widget _getExpansionTile(ProjectDirectory directory) {
    return Padding(
      padding: EdgeInsets.only(left: widget.recursivePaddingSize),
      child: ExpansionTile(
        collapsedIconColor: Colors.white,
        title: Text(directory.name),
        children: _getExplorerTiles(directory.subEntities),
      ),
    );
  }

  // Widget _getSearch() {
  //   return const ExpansionTile(
  //     collapsedIconColor: Colors.white,
  //     title: Text('Search'),
  //   );
  // }
}
