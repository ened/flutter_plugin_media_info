import 'dart:async';

import 'package:flutter/services.dart';

class MediaInfo {
  static const MethodChannel _channel =
      MethodChannel('asia.ivity.flutter/media_info');

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

  /// Generate a thumbnail for a video or image file.
  /// 
  /// The thumbnail will be stored in the file path specified at [target].
  /// 
  /// Additionally, a target width and height should be specified. The thumbnail will be 
  /// centered inside the bounding box.
  /// 
  /// Currently the thumbnail format is JPG.
  static Future<bool> generateThumbnail(
      String path, String target, int width, int height) async {
    final dynamic successful =
        await _channel.invokeMethod('generateThumbnail', <String, dynamic>{
      'path': path,
      'target': target,
      'width': width,
      'height': height,
    });

    return successful;
  }
}
