import 'package:flutter/material.dart';

class SnackBarFactory {
  static SnackBar getInfoSnackBar(String info) {
    return SnackBar(
      content: Text(info, textAlign: TextAlign.center,),
      shape: const StadiumBorder(),
      behavior: SnackBarBehavior.floating,
      width: 200,
      duration: const Duration(seconds: 1),
    );
  }
}