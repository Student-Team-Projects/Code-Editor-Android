import 'package:flutter/material.dart';

class DummyWidget extends StatefulWidget {
  const DummyWidget({Key? key, required this.notifyApp}) : super(key: key);

  final VoidCallback notifyApp;

  @override
  State<DummyWidget> createState() => _DummyWidgetState();
}

class _DummyWidgetState extends State<DummyWidget> {

  @override
  Widget build(BuildContext context) {
    return Container();
  }

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((timeStamp) {
      widget.notifyApp.call();
    });
  }
}
