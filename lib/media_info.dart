import 'dart:async';
import 'dart:io';
import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:flutter/foundation.dart';
import 'package:flutter/painting.dart';
import 'package:flutter/services.dart';
import 'package:mime/mime.dart';

/// Media information & basic thumbnail creation methods.
class MediaInfo {
  /// Initializes the plugin and starts listening for potential platform events.
  factory MediaInfo() {
    if (_instance == null) {
      final MethodChannel methodChannel =
          const MethodChannel('asia.ivity.flutter/media_info');
      _instance = MediaInfo.private(methodChannel);
    }
    return _instance;
  }

  /// This constructor is only used for testing and shouldn't be accessed by
  /// users of the plugin. It may break or change at any time.
  @visibleForTesting
  MediaInfo.private(this._methodChannel);

  static MediaInfo _instance;

  final MethodChannel _methodChannel;

  /// Utilizes platform methods (which may include a combination of HW and SW
  /// decoders) to analyze the media file at a given path.
  ///
  /// This method will return a standard [FlutterError] if the decoding failed.
  ///
  /// Valid media files will generate a dictionary with relevant fields set.
  ///
  /// Images are decoded in Dart, while Audio & Video files are processed on the platform itself.
  ///
  /// The returned map contains the following fields, depending on the content parsed.
  ///
  /// | Images   | Videos     | Audio      |
  /// |----------|------------|------------|
  /// | mimeType | mimeType   | mimeType   |
  /// | width    | width      |            |
  /// | height   | height     |            |
  /// |          | frameRate  |            |
  /// |          | durationMs | durationMs |
  /// |          | numTracks  |            |
  /// |          |            | bitrate    |
  Future<Map<String, dynamic>> getMediaInfo(String path) async {
    final RandomAccessFile file = File(path).openSync();
    final Uint8List headerBytes = file.readSync(defaultMagicNumbersMaxLength);
    final String mimeType = lookupMimeType(path, headerBytes: headerBytes);

    if (mimeType?.startsWith('image') == true) {
      Completer<ui.Image> completer = Completer<ui.Image>();

      final stream = FileImage(File(path)).resolve(ImageConfiguration());

      final ImageStreamListener listener =
          ImageStreamListener((ImageInfo image, __) {
        if (!completer.isCompleted) {
          completer.complete(image.image);
        }
      });

      stream.addListener(listener);

      final image = await completer.future.whenComplete(
        () => stream.removeListener(listener),
      );

      return {
        'width': image.width,
        'height': image.height,
        'mimeType': mimeType,
      };
    }

    return _methodChannel.invokeMapMethod<String, dynamic>(
      'getMediaInfo',
      path,
    );
  }

  /// Generate a thumbnail for a video or image file.
  ///
  /// The thumbnail will be stored in the file path specified at [target].
  ///
  /// Additionally, a target width and height should be specified.
  ///
  /// Currently the thumbnail format is JPG, set to image quality 80.
  ///
  /// Errors will be propagated to the consumer of this API and need to be
  /// handled in the onError handler of the returned [Future].
  Future<String> generateThumbnail(
    /// Absolute source file path, without the file:// scheme prepended.
    String path,

    /// Absolute target file path, without the file:// scheme prepended.
    String target,

    /// Target width.
    int width,

    /// Target height.
    /// TODO: Consider to remove the field or specify the fit/crop ratio better.
    int height, {

    /// Position of the video in milliseconds where to generate the image from
    int positionMs = 0,
  }) {
    return _methodChannel.invokeMethod<String>(
      'generateThumbnail',
      <String, dynamic>{
        'path': path,
        'target': target,
        'width': width,
        'height': height,
        'positionMs': positionMs,
      },
    );
  }
}
