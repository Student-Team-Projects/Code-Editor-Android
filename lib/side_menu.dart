import 'package:flutter/material.dart';
import 'package:visual_studio_for_android/color_palettes.dart';

class EmptyProjectMenu extends StatelessWidget {
  const EmptyProjectMenu({super.key});

  @override
  Widget build(BuildContext context) {
    return Drawer(
      backgroundColor: explorerItemsListBackgroundColor,
      child: Column(
        children: <Widget>[
          Expanded(
            child: ListView(
              padding: EdgeInsets.zero,
              children: const <Widget>[
                DrawerHeader(
                  decoration: BoxDecoration(
                      color: explorerBackgroundColor,
                  ),
                  child: Text(
                    'Menu',
                    style: TextStyle(fontSize: 25),
                  ),
                ),
              ],
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
}
