package asia.ivity.mediainfo;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.util.Log;
import java.io.File;

class VideoUtils {

  private static final String TAG = "VideoUtils";

  static VideoDetail readVideoDetail(File file) {

    try {
      final MediaMetadataRetriever retriever = new MediaMetadataRetriever();
      retriever.setDataSource(file.getAbsolutePath());

      int width =
          Integer.parseInt(
              retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
      int height =
          Integer.parseInt(
              retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));

      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
        int rotation = Integer
            .valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));

        // Switch the width/height if video was taken in portrait mode
        if (rotation == 90 || rotation == 270) {
          int temp = width;
          //noinspection SuspiciousNameCombination
          width = height;
          height = temp;
        }
      }

      float frameRate;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        final String str =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE);

        try {
          frameRate = Float.parseFloat(str);
        } catch (Throwable e) {
          frameRate = readFrameRateUsingExtractor(file);
        }

      } else {
        frameRate = readFrameRateUsingExtractor(file);
      }
      final long durationMs =
          Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

      final short numTracks =
          Short.parseShort(
              retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS));

      final String mimeType =
          retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);

      return new VideoDetail(width, height, frameRate, durationMs, numTracks, mimeType);
    } catch (Throwable e) {
      Log.e(TAG, file.getAbsolutePath(), e);
      return null;
    }
  }

  private static float readFrameRateUsingExtractor(File file) {
    final MediaExtractor extractor = new MediaExtractor();
    int frameRate = 30; // may be default
    try {
      // Adjust data source as per the requirement if file, URI, etc.
      extractor.setDataSource(file.getAbsolutePath());
      final int numTracks = extractor.getTrackCount();

      for (int i = 0; i < numTracks; i++) {
        final MediaFormat format = extractor.getTrackFormat(i);

        final String mime = format.getString(MediaFormat.KEY_MIME);
        if (mime.startsWith("video/")) {
          if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
            frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
          }
        }
      }
    } catch (Throwable e) {
      Log.e(TAG, file.getAbsolutePath(), e);
    } finally {
      // Release stuff
      extractor.release();
    }

    return frameRate;
  }
}
