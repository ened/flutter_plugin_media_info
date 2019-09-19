package asia.ivity.mediainfo;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

class ThumbnailUtils {

  private static final String TAG = "ThumbnailUtils";

  static File generateVideoThumbnail(Context context, String path, int width, int height) {
    final File cacheFile =
        getTempFile(context, String.format(Locale.US, "%s_%s_%s", path.hashCode(), width, height));

    if (cacheFile.exists()) {
      return cacheFile;
    }

    FutureTarget<Bitmap> futureTarget =
        Glide.with(context)
            .asBitmap()
            .apply(RequestOptions.fitCenterTransform())
            .load(new File(path))
            .submit(width, height);

    FileOutputStream fos = null;
    try {
      final Bitmap bitmap = futureTarget.get();

      if (bitmap == null) {
        return null;
      }

      fos = new FileOutputStream(cacheFile);
      bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
      fos.close();

      return cacheFile;
    } catch (Throwable e) {
      Log.e(TAG, "during thumbnail creation", e);
      return null;
    } finally {
      if (fos != null) {
        try {
          fos.close();
        } catch (IOException ignored) {
        }
      }
    }
  }

  private static File getTempFile(Context context, String cacheKey) {
    return new File(context.getExternalCacheDir(), cacheKey + ".tmp");
  }
}
