import 'dart:io';

import 'package:flutter/material.dart';

import 'package:media_info/media_info.dart';

import 'package:file_picker/file_picker.dart';
import 'package:path_provider/path_provider.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _file;
  Map<String, String> _mediaInfo;
  String _thumbnail;

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
              Spacer(flex: 1),
              AspectRatio(
                aspectRatio: 16 / 9,
                child: _thumbnail != null ? Image.file(File(_thumbnail)) : null,
              ),
              Spacer(flex: 3),
            ],
          ),
        ),
        bottomNavigationBar: RaisedButton(
          child: const Text('Select File'),
          onPressed: () async {
            final String mediaFile = await FilePicker.getFilePath();

            if (!mounted) return;

            setState(() {
              _file = mediaFile;
              _mediaInfo = null;
              _thumbnail = null;
            });

            final Map<String, String> mediaInfo =
                await MediaInfo.getMediaInfo(mediaFile);

            if (!mounted) return;

            setState(() {
              _mediaInfo = mediaInfo;
            });

            final Directory cacheDir = await getTemporaryDirectory();
            final int cacheName = _file.hashCode;
            final String target = File('${cacheDir.path}/$cacheName').path;
            final bool thumbOk =
                await MediaInfo.generateThumbnail(_file, target, 1920, 1080);

            setState(() => _thumbnail = thumbOk ? target : null);
          },
        ),
      ),
    );
  }
}
