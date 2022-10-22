import 'dart:io';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:media_info/media_info.dart';

void main() {
  testWidgets('should perform 25 sequential reads', (tester) async {
    var dir = Directory.systemTemp.createTempSync();
    var temp = File("${dir.path}/video.mp4")..createSync();

    final bytes =
        await rootBundle.load('assets/videos/pexels_videos_2268807.mp4');
    temp.writeAsBytesSync(
        bytes.buffer.asUint8List(bytes.offsetInBytes, bytes.lengthInBytes));

    for (int i = 0; i < 25; i++) {
      final info = await MediaInfo().getMediaInfo(temp.path);

      expect(info['width'], equals(1920));
      expect(info['height'], equals(1080));
    }
  });
}
