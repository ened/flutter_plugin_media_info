import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:media_info/media_info.dart';
import 'package:e2e/e2e.dart';
import 'package:flutter/services.dart' show ByteData, rootBundle;
import 'package:path_provider/path_provider.dart';

Future<String> copyAsset(String assetPath) async {
  final Directory directory = await getApplicationDocumentsDirectory();
  final String asset = assetPath.split('/').last;

  var copiedPath = '${directory.path}/$asset';
  if (FileSystemEntity.typeSync(copiedPath) == FileSystemEntityType.notFound) {
    ByteData data = await rootBundle.load(assetPath);
    List<int> bytes = data.buffer.asUint8List(
      data.offsetInBytes,
      data.lengthInBytes,
    );
    await File(copiedPath).writeAsBytes(bytes);
  }

  return copiedPath;
}

void main() {
  E2EWidgetsFlutterBinding.ensureInitialized();

  for (String name in ['IMG_6078.MOV']) {
    String fullPath;
    setUp(() async {
      fullPath = await copyAsset('assets/samples/$name');
    });

    testWidgets('Can determine details', (WidgetTester tester) async {
      final MediaInfo mediaInfo = MediaInfo();
      final info = await mediaInfo.getMediaInfo(fullPath);

      expect(info['height'], 1920);
      expect(info['width'], 1080);
    });

    testWidgets('Can generate thumbnail', (WidgetTester tester) async {
      final MediaInfo mediaInfo = MediaInfo();
      final info = await mediaInfo.getMediaInfo(fullPath);

      final tmpDir = await getTemporaryDirectory();
      await mediaInfo.generateThumbnail(
        fullPath,
        '${tmpDir.path}/$name',
        info['width'],
        info['height'],
      );

      print('Thumbnail written to: ${tmpDir.path}/$name');
    });
  }
}
