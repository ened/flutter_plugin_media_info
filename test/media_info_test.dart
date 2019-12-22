import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:media_info/media_info.dart';
import 'package:mockito/mockito.dart';

void main() {
  MockMethodChannel methodChannel;
  MediaInfo mediaInfo;

  setUp(() {
    methodChannel = MockMethodChannel();
    mediaInfo = MediaInfo.private(methodChannel);
  });

  test('getMediaInfo', () async {
    when(methodChannel.invokeMapMethod<String, dynamic>(
      'getMediaInfo',
      any,
    )).thenAnswer(
      (Invocation invoke) => Future<Map<String, dynamic>>.value(
        {'width': 1024},
      ),
    );

    expect(await mediaInfo.getMediaInfo('/path'), {'width': 1024});
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

class MockMethodChannel extends Mock implements MethodChannel {}
