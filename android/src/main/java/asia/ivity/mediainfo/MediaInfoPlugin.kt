package asia.ivity.mediainfo

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * MediaInfoPlugin
 */
class MediaInfoPlugin(private val context: Context?) : MethodCallHandler {
    /**
     * All thumbnails will be generated on a background thread.
     */
    private lateinit var thumbnailExecutor: ExecutorService

    /**
     * Flutter results must be posted back on the main thread though.
     */
    private lateinit var mainThreadHandler: Handler

    override fun onMethodCall(call: MethodCall, result: Result) {
        if (!this::thumbnailExecutor.isInitialized) {
            thumbnailExecutor =
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
        }

        if (!this::mainThreadHandler.isInitialized) {
            mainThreadHandler = Handler(Looper.myLooper())
        }

        return when (call.method) {
            "getMediaInfo" -> {
                val path = call.arguments as String

                handleMediaInfo(path, result)
            }
            "generateThumbnail" -> {
                val args = call.arguments as HashMap<*, *>

                handleThumbnail(args, result, context, mainThreadHandler)
            }
            else -> result.notImplemented()
        }
    }

    private fun handleMediaInfo(path: String, result: Result) {
        val info = VideoUtils.readVideoDetail(File(path))

        if (info != null) {
            result.success(info.toMap())
        } else {
            result.success(null)
        }
    }

    private fun handleThumbnail(args: HashMap<*, *>, result: Result, context: Context?, mainThreadHandler: Handler) {
        thumbnailExecutor.submit {
            val target = File(args["target"] as String)

            if (target.exists()) {
                Log.e(TAG, "Target $target file already exists.")
                mainThreadHandler.post {
                    result.error("MediaInfo", "FileOverwriteDenied", null)
                }
                return@submit
            }

            if (context == null) {
                Log.e(TAG, "Context disappeared")
                mainThreadHandler.post {
                    result.error("MediaInfo", "ContextDisappeared", null)
                }
                return@submit
            }

            val file = ThumbnailUtils.generateVideoThumbnail(context, args["path"] as String,
                                                             args["width"] as Int,
                                                             args["height"] as Int)
            if (file != null && file.renameTo(target)) {
                mainThreadHandler.post {
                    result.success(target.absolutePath)
                }
            } else {
                Log.e(TAG, "File does not generate or does not exist: $file")
                mainThreadHandler.post {
                    result.error("MediaInfo", "FileCreationFailed", null)
                }
            }
        }
    }

    companion object {

        private const val NAMESPACE = "asia.ivity.flutter"
        private const val TAG = "MediaInfoPlugin"

        /**
         * Plugin registration.
         */
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "$NAMESPACE/media_info")
            channel.setMethodCallHandler(MediaInfoPlugin(registrar.context()))
        }
    }
}
