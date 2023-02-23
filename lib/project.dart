import 'dart:io';
import 'package:code_text_field/code_text_field.dart';
import 'package:flutter_highlight/themes/monokai-sublime.dart';
import 'package:visual_studio_for_android/language_map.dart';

class Project {
  bool _empty = true;
  List<ProjectEntity> _entities = [];
  ProjectFile? currentFile;
  List<ProjectFile> openFiles = [];

  bool get empty => _empty;
  List<ProjectEntity> get entities => _entities;

  Future<void> initializeOpenSingleFile(String filePath) async {
    var file = await ProjectFile.create(filePath);
    _empty = false;
    _entities.add(file);
    currentFile = file;
    openFiles.add(file);
  }

  Future<void> initializeNewSingleFile(String filePath) async {
    var file = await ProjectFile.create(filePath);
    _empty = false;
    _entities.add(file);
    currentFile = file;
    openFiles.add(file);
  }

  Future<void> initializeDirectory(String directoryPath) async {
    final dir = Directory(directoryPath);
    _entities = await _getEntities(dir);
    _empty = false;
  }

  // recursive function to build the project structure based on chosen directory. Returns a sorted list of project entities
  Future<List<ProjectEntity>> _getEntities(Directory dir) async {
    var list = <ProjectEntity>[];

    final List<FileSystemEntity> dirEntities = await dir.list().toList();
    for(var entity in dirEntities) {
      if(entity is File) {
        list.add(await ProjectFile.create(entity.path));
      } else {
        list.add(ProjectDirectory(entity.path, await _getEntities(entity as Directory)));
      }
    }

    list.sort();  //ProjectEntity implements Comparable<ProjectEntity>
    return list;
  }

  void switchToFile(ProjectFile file) {
    currentFile = file;
    if(!openFiles.contains(file)) {
      openFiles.add(file);
    }
  }

  void closeFile(ProjectFile file) {
    openFiles.remove(file);
    if(currentFile == file) {
      currentFile = openFiles.isEmpty ? null : openFiles.first;
    }
  }

  void saveCurrentFile() {
    if(currentFile != null) currentFile!.save();
  }

  void close() {
    _empty = true;
    _entities.clear();
    currentFile = null;
    openFiles.clear();
  }
}

class ProjectFile extends ProjectEntity {
  @override
  late String _name;
  @override
  final bool _isDir = false;
  @override
  late String _path;
  @override
  List<ProjectEntity> _subEntities = <ProjectEntity>[];
  late final CodeController _codeController;

  // Private constructor
  ProjectFile._create(String filePath, String source) {
    _path = filePath;
    int lastBackslashIndex = _path.lastIndexOf('/');
    _name = _path.substring(lastBackslashIndex+1);

    var languageMap = LanguageMap();
    var codeLanguage = _name.substring((_name.indexOf('.')+1));

    _codeController = CodeController(
        text: source,
        language: languageMap.language_map[codeLanguage],
        theme: monokaiSublimeTheme
    );
  }

  // Public factory (because constructor can't be async)
  static Future<ProjectFile> create(String filePath) async {

    File file = File(filePath);
    String source = '';
    try {
      source = await file.readAsString();
    } catch(e) {}

    source = source.replaceAll('\r\n', '\n');
    return ProjectFile._create(filePath, source);
  }

  CodeController get codeController => _codeController;

  void save() async {
    File file = File(path);
    await file.writeAsString(codeController.text);
  }
}

class ProjectDirectory extends ProjectEntity {
  @override
  late String _name;
  @override
  final bool _isDir = true;
  @override
  String _path;
  @override
  List<ProjectEntity> _subEntities;

  ProjectDirectory(this._path, this._subEntities) {
    int lastBackslashIndex = _path.lastIndexOf('/');
    _name = _path.substring(lastBackslashIndex+1);
  }
}

abstract class ProjectEntity implements Comparable<ProjectEntity> {
  abstract String _name;
  abstract final bool _isDir;
  abstract String _path;
  abstract List<ProjectEntity> _subEntities;

  String get name => _name;

  bool get isDir => _isDir;

  String get path => _path;

  List<ProjectEntity> get subEntities => _subEntities;

  @override
  int compareTo(ProjectEntity other) {
    // directories are smaller than files
    if(isDir && !other.isDir) {
      return -1;
    }
    if(!isDir && other.isDir) {
      return 1;
    }
    // both are directories or both are files. compare by name
    return name.compareTo(other.name);
  }
}