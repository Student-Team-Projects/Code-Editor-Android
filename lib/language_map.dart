import 'package:highlight/highlight.dart';
import 'package:highlight/languages/cpp.dart';
import 'package:highlight/languages/css.dart';
import 'package:highlight/languages/java.dart';
import 'package:highlight/languages/javascript.dart';
import 'package:highlight/languages/kotlin.dart';
import 'package:highlight/languages/python.dart';

class LanguageMap{
  Map language_map = Map<String, Mode>();
  LanguageMap(){
    language_map = {
      "cpp": cpp,
      "css": css,
      "py": python,
      "java": java,
      "js": javascript,
      "kotlin": kotlin,
    };
  }
}