import 'dart:async';
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

const List<int> _imageWidths = [
  320,
  480,
  640,
  848,
  960,
  1024,
  1200,
  1280,
  1440,
  1600,
  1920,
];

class _MyAppState extends State<MyApp> {
  String _file;
  Map<String, dynamic> _mediaInfoCache;
  final Map<int, Future<String>> _thumbnails = <int, Future<String>>{};

  final MediaInfo _mediaInfo = MediaInfo();

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      showPerformanceOverlay: false,
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Media Info App'),
        ),
        body: SafeArea(
          child: SingleChildScrollView(
            child: Padding(
              padding: const EdgeInsets.all(24.0),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: <Widget>[
                  Text(_file ?? 'Please select a file'),
                  Text(
                    (_mediaInfoCache?.keys ?? <String>[])
                        .map((String k) => '$k: ${_mediaInfoCache[k]}')
                        .join(',\n\n'),
                    style: Theme.of(context).textTheme.body2,
                  ),
                  Builder(
                    builder: (BuildContext context) {
                      if (_thumbnails.isEmpty) {
                        return const SizedBox();
                      }

                      final List<int> listW = _thumbnails.keys.toList();
                      listW.sort();

                      final List<Widget> widgets = <Widget>[];
                      for (final int width in listW) {
                        widgets.addAll([
                          Text('Width: $width'),
                          FutureBuilder<String>(
                            future: _thumbnails[width],
                            builder: (BuildContext context, snapshot) {
                              if (snapshot.hasData) {
                                return Image.file(File(snapshot.data));
                              }
                              if (snapshot.hasError) {
                                return Text(
                                  'E: ${snapshot.error}',
                                  style: TextStyle(color: Colors.red),
                                );
                              }

                              return const SizedBox();
                            },
                          ),
                          Divider(),
                        ]);
                      }

                      return Column(
                        mainAxisSize: MainAxisSize.min,
                        children: widgets,
                      );
                    },
                  ),
                ],
              ),
            ),
          ),
        ),
        floatingActionButton: _buildSelectButton(),
      ),
    );
  }

  Widget _buildSelectButton() {
    return FloatingActionButton(
      child: Icon(Icons.attach_file),
      onPressed: () async {
        final String mediaFile = await FilePicker.getFilePath();

        if (!mounted || mediaFile == null) {
          return;
        }

        setState(() {
          _file = mediaFile;
          _mediaInfoCache = null;
          _thumbnails.clear();
        });

        final Map<String, dynamic> mediaInfo =
            await _mediaInfo.getMediaInfo(mediaFile);

        if (!mounted || mediaInfo == null) {
          return;
        }

        setState(() {
          _mediaInfoCache = mediaInfo;
        });

        final Directory cacheDir = await getTemporaryDirectory();
        final int cacheName = _file.hashCode;

        final int w = mediaInfo['width'];
        final int h = mediaInfo['height'];
        final double ratio = w / h;

        final String mime = mediaInfo['mimeType'];
        if (mime.startsWith("video/")) {
          for (final int width in _imageWidths) {
            final String target =
                File('${cacheDir.path}/$cacheName.$width').path;
            if (File(target).existsSync()) {
              File(target).deleteSync();
            }

            _thumbnails[width] = _mediaInfo.generateThumbnail(
                _file, target, width, width ~/ ratio);
          }
        }

        setState(() {});
      },
    );
  }
}
