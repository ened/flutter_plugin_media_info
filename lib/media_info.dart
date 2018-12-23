import 'dart:async';

import 'package:flutter/services.dart';

class MediaInfo {
  static const MethodChannel _channel =
      const MethodChannel('asia.ivity.flutter/media_info');

  /// Utilizes platform methods (which may include a combination of HW and SW decoders)
  /// to analyze the media file at a given path.
  /// 
  /// This will return *null* if the media file is invalid.
  /// 
  /// Valid media files will generate a dictionary with relevant fields set.
  /// For video files, this includes:
  /// - width
  /// - height
  /// - frameRate
  /// - durationMs
  /// - numTracks
  static Future<Map<String, String>> getMediaInfo(String path) async {
    final dynamic version = await _channel.invokeMethod('getMediaInfo', path);
    return Map<String, String>.from(version);
  }
}
