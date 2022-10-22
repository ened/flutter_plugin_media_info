import 'dart:convert';
import 'dart:io';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:media_info/media_info.dart';
import 'package:mockito/annotations.dart';
import 'package:mockito/mockito.dart';

import 'media_info_test.mocks.dart';

@GenerateMocks([MethodChannel])
void main() {
  const tinyMp4 =
      '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';
  const png1by1 =
      'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==';

  late MockMethodChannel methodChannel;
  late MediaInfo mediaInfo;

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    methodChannel = MockMethodChannel();
    mediaInfo = MediaInfo.private(methodChannel);
  });

  test('getMediaInfo png', () async {
    var dir = Directory.systemTemp.createTempSync();
    var temp = File("${dir.path}/sample.png")..createSync();
    temp.writeAsBytesSync(base64Decode(png1by1));

    expect(
      await mediaInfo.getMediaInfo(temp.path),
      {'width': 1, 'height': 1, 'mimeType': 'image/png'},
    );
  });

  test('getMediaInfo mp4', () async {
    var dir = Directory.systemTemp.createTempSync();
    var temp = File("${dir.path}/sample.mp4")..createSync();
    temp.writeAsBytesSync(base64Decode(tinyMp4));

    when(methodChannel.invokeMapMethod<String, dynamic>(
      'getMediaInfo',
      any,
    )).thenAnswer(
      (Invocation invoke) => Future<Map<String, dynamic>>.value(
        {'width': 192, 'height': 108, 'mimeType': 'video/quicktime'},
      ),
    );

    expect(
      await mediaInfo.getMediaInfo(temp.path),
      {'width': 192, 'height': 108, 'mimeType': 'video/quicktime'},
    );
  });

  test('generateThumbnail', () async {
    when(methodChannel.invokeMethod<String>(
      'generateThumbnail',
      any,
    )).thenAnswer((Invocation invoke) => Future<String>.value('/path'));

    expect(
      await mediaInfo.generateThumbnail('/source', '/path', 1024, 768),
      '/path',
    );
  });
}
