package asia.ivity.mediainfo

import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.File

/**
 * MediaInfoPlugin
 */
class MediaInfoPlugin : MethodCallHandler {

    override fun onMethodCall(call: MethodCall, result: Result) {
        if (call.method == "getMediaInfo") {
            val path = call.arguments as String

            val info = VideoUtils.readVideoDetail(File(path))

            if (info != null) {
                result.success(info.toMap())
            } else {
                result.success(null)
            }
        } else {
            result.notImplemented()
        }
    }

    companion object {

        private val NAMESPACE = "asia.ivity.flutter"

        /**
         * Plugin registration.
         */
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(),
                    "$NAMESPACE/media_info")
            channel.setMethodCallHandler(MediaInfoPlugin())
        }
    }
}
