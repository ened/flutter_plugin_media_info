import 'package:flutter/material.dart';

import 'package:media_info/media_info.dart';

import 'package:file_picker/file_picker.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _file;
  Map<String, String> _mediaInfo;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Media Info App'),
        ),
        body: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            children: <Widget>[
              Spacer(flex: 3),
              Text(_file ?? "Please select a file"),
              Spacer(flex: 1),
              Text(
                (_mediaInfo?.keys ?? [])
                    .map((k) => "$k: ${_mediaInfo[k]}")
                    .join(",\n\n"),
                style: Theme.of(context).textTheme.body2,
              ),
              Spacer(flex: 3),
            ],
          ),
        ),
        bottomNavigationBar: RaisedButton(
          child: Text("Select File"),
          onPressed: () async {
            final mediaFile = await FilePicker.getFilePath();

            if (!mounted) return;

            setState(() {
              _file = mediaFile;
              _mediaInfo = null;
            });

            final mediaInfo = await MediaInfo.getMediaInfo(mediaFile);

            if (!mounted) return;

            setState(() {
              _mediaInfo = mediaInfo;
            });
          },
        ),
      ),
    );
  }
}
