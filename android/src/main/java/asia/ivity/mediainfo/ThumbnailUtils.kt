package asia.ivity.mediainfo

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutionException

/**
 */
object ThumbnailUtils {
    fun generateVideoThumbnail(context: Context, path: String, width: Int, height: Int): File? {
        val cacheFile = getTempFile(context,
                String.format(Locale.US, "%s_%s_%s", path.hashCode(), width, height))

        if (cacheFile.exists()) {
            return cacheFile
        }

        val futureTarget = Glide.with(context)
                .asBitmap()
                .apply(RequestOptions.fitCenterTransform())
                .load(File(path))
                .submit(width, height)

        var fos: FileOutputStream? = null
        try {
            val bitmap = futureTarget.get() ?: return null

            fos = FileOutputStream(cacheFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
            fos.close()

            return cacheFile
        } catch (e: InterruptedException) {
            return null
        } catch (e: ExecutionException) {
            return null
        } catch (e: IOException) {
            return null
        } finally {
            if (fos != null) {
                try {
                    fos.close()
                } catch (ignored: IOException) {
                }
            }
        }
    }

    private fun getTempFile(context: Context, cacheKey: String): File {
        return File(context.externalCacheDir, "$cacheKey.tmp")
    }

}
