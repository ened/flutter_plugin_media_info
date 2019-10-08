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

class Resolution {
  const Resolution(this.w, this.h);

  final int w;
  final int h;

}

const List<Resolution> _resolutions = [
  Resolution(384, 216),
  Resolution(512, 288),
  Resolution(640, 360),
  Resolution(768, 432),
  Resolution(896, 504),
  Resolution(1024, 576),
  Resolution(1152, 648),
  Resolution(1280, 720),
  Resolution(1408, 792),
  Resolution(1536, 864),
  Resolution(1664, 936),
  Resolution(1792, 1008),
  Resolution(1920, 1080),
//  Resolution(320, 180),
//  Resolution(480, 288),
//  Resolution(640, 360),
//  Resolution(848, 1200),
//  Resolution(960, 1200),
//  Resolution(1024, 1200),
//  Resolution(1200, 1200),
//  Resolution(1280, 1200),
//  Resolution(1440, 1200),
//  Resolution(1600, 1200),
//  Resolution(1920, 1200),
];

class _MyAppState extends State<MyApp> {
  String _file;
  Map<String, dynamic> _mediaInfoCache;
  final Map<String, Future<String>> _thumbnails = <String, Future<String>>{};

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

                      final List<String> listW = _thumbnails.keys.toList();
                      listW.sort((a, b) {
                        final wA = int.parse(a.split('x').first);
                        final wB = int.parse(b.split('x').first);

                        return wA.compareTo(wB);
                      });

                      final List<Widget> widgets = <Widget>[];
                      for (final String res in listW) {
                        widgets.addAll([
                          Text(res),
                          FutureBuilder<String>(
                            future: _thumbnails[res],
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

        final String mime = mediaInfo['mimeType'];
        if (mime.startsWith("video/")) {
          Set<Resolution> resolutions = Set();
//          resolutions.addAll(_resolutions);
          resolutions.add(Resolution(w, h));


          for (final Resolution res in resolutions) {
//            final double ratio = w / width;

            final String target =
                File('${cacheDir.path}/$cacheName.${res.w}').path;
            if (File(target).existsSync()) {
              File(target).deleteSync();
            }

            _thumbnails['${res.w}x${res.h}'] = _mediaInfo.generateThumbnail(
                _file, target, res.w, res.h);
          }
        }

        setState(() {});
      },
    );
  }
}
