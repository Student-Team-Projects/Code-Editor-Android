import 'package:filesystem_picker/filesystem_picker.dart';
import 'package:flutter/material.dart';
import 'package:visual_studio_for_android/project.dart';
import 'color_palettes.dart';
import 'widget_factory.dart';

class App extends StatefulWidget {
  const App({required this.widgetFactory, Key? key}) : super(key: key);

  final WidgetFactory widgetFactory;

  @override
  State<StatefulWidget> createState() => _AppState();
}

class _AppState extends State<App> {

  void _reactToProjectChange() {
    setState(() {});
  }

  final Project _project = Project();

  @override
  Widget build(BuildContext context) {
    return FilesystemPickerDefaultOptions(
      fileTileSelectMode: FileTileSelectMode.wholeTile,
      child: MaterialApp(
        theme: ThemeData.dark(),
        home: Scaffold(
          backgroundColor: mainBackgroundColor,
          appBar: widget.widgetFactory.getAppBar(project: _project, notifyApp: _reactToProjectChange),
          drawer: widget.widgetFactory.getDrawer(project: _project, notifyApp: _reactToProjectChange),
          body: widget.widgetFactory.getBody(project: _project, notifyApp: _reactToProjectChange)
        )
      ),
    );
  }
}