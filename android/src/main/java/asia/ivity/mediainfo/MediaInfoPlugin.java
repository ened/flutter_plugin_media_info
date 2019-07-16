package asia.ivity.mediainfo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import java.io.File;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MediaInfoPlugin implements MethodCallHandler {

  private static final String NAMESPACE = "asia.ivity.flutter";
  private static final String TAG = "MediaInfoPlugin";

  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(),
        NAMESPACE + "/media_info");
    channel.setMethodCallHandler(new MediaInfoPlugin(registrar.context()));
  }

  private ExecutorService thumbnailExecutor;
  private Handler mainThreadHandler;

  private final Context context;

  private MediaInfoPlugin(Context context) {
    this.context = context;
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    if (thumbnailExecutor == null) {
      thumbnailExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    if (mainThreadHandler == null) {
      mainThreadHandler = new Handler(Looper.myLooper());
    }

    if (call.method.equalsIgnoreCase("getMediaInfo")) {
      String path = (String) call.arguments;

      handleMediaInfo(path, result);
    } else if (call.method.equalsIgnoreCase("generateThumbnail")) {
      HashMap args = (HashMap) call.arguments;
      handleThumbnail(args, result, context, mainThreadHandler);
    }
  }


  private void handleMediaInfo(String path, Result result) {
    VideoDetail info = VideoUtils.readVideoDetail(new File(path));

    if (info != null) {
      result.success(info.toMap());
    } else {
      result.error("MediaInfo", "InvalidFile", null);
    }
  }

  private void handleThumbnail(HashMap args, Result result, Context context,
      Handler mainThreadHandler) {
    thumbnailExecutor.submit(() -> {
      final File target = new File((String) args.get("target"));

      if (target.exists()) {
        Log.e(TAG, "Target $target file already exists.");
        mainThreadHandler.post(() -> result.error("MediaInfo", "FileOverwriteDenied", null));
        return;
      }

      if (context == null) {
        Log.e(TAG, "Context disappeared");
        mainThreadHandler.post(() -> result.error("MediaInfo", "ContextDisappeared", null));

        return;
      }

      File file = ThumbnailUtils.generateVideoThumbnail(context,
          Objects.requireNonNull((String) args.get("path")),
          Objects.<Integer>requireNonNull((Integer) args.get("width")),
          Objects.<Integer>requireNonNull((Integer) args.get("height"))
      );

      if (file != null && file.renameTo(target)) {
        mainThreadHandler.post(() -> result.success(target.getAbsolutePath()));
      } else {
        Log.e(TAG, "File does not generate or does not exist: " + file);
        mainThreadHandler.post(() -> result.error("MediaInfo", "FileCreationFailed", null));
      }
    });
  }
}
