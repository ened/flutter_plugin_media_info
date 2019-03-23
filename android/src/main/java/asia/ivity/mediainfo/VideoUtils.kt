package asia.ivity.mediainfo

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Log
import java.io.File
import java.util.*

class VideoUtils {

    data class VideoDetail(
            val width: Int,
            val height: Int,
            val frameRate: Float,
            val durationMs: Long,
            val numTracks: Short,
            val mimeType: String
    ) {
        fun toMap(): HashMap<String, Any> = HashMap<String, Any>().apply {
            this.putAll(mapOf(
                    "width" to width,
                    "height" to height,
                    "frameRate" to frameRate,
                    "durationMs" to durationMs,
                    "numTracks" to numTracks.toInt(),
                    "mimeType" to mimeType
            ))
        }

    }

    companion object {
        private const val TAG = "VideoUtils"

        fun readVideoDetail(file: File): VideoDetail? {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(file.absolutePath)
                val width = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH))
                val height = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT))

                val frameRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val str = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)

                    str?.toFloat() ?: readFrameRateUsingExtractor(file)
                } else {
                    readFrameRateUsingExtractor(file)
                }

                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()

                val numTracks = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS).toShort()

                val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)

                return VideoDetail(width, height, frameRate, durationMs, numTracks, mimeType)
            } catch (e: Exception) {
                Log.e(TAG, file.absolutePath, e)
                return null
            }
        }

        private fun readFrameRateUsingExtractor(file: File): Float {
            val extractor = MediaExtractor()
            var frameRate = 30 //may be default
            try {
                //Adjust data source as per the requirement if file, URI, etc.
                extractor.setDataSource(file.absolutePath)
                val numTracks = extractor.trackCount

                for (i in 0 until numTracks) {
                    val format = extractor.getTrackFormat(i)

                    val mime = format.getString(MediaFormat.KEY_MIME)
                    if (mime.startsWith("video/")) {
                        if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                            frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE)
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, file.absolutePath, e)
            } finally {
                //Release stuff
                extractor.release()
            }

            return frameRate.toFloat()
        }
    }

}
