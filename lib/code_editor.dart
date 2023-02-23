import 'package:code_text_field/code_text_field.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:visual_studio_for_android/project.dart';

class CodeEditor extends StatelessWidget {
  const CodeEditor({super.key, required this.file});

  final ProjectFile file;

  @override
  Widget build(BuildContext context) {
    return CodeField(
      controller: file.codeController,
      textStyle: GoogleFonts.sourceCodePro(),
    );
  }
}
