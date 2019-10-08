package asia.ivity.mediainfo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import asia.ivity.mediainfo.util.OutputSurface;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Player.EventListener;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoListener;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java9.util.concurrent.CompletableFuture;

public class MediaInfoPlugin implements MethodCallHandler {

  private static final String NAMESPACE = "asia.ivity.flutter";
  private static final String TAG = "MediaInfoPlugin";

  private static final boolean USE_EXOPLAYER = true;

  public static void registerWith(Registrar registrar) {
    final MethodChannel channel =
        new MethodChannel(registrar.messenger(), NAMESPACE + "/media_info");
    channel.setMethodCallHandler(new MediaInfoPlugin(registrar.context()));
  }

  private ExecutorService thumbnailExecutor;
  private Handler mainThreadHandler;

  private final Context context;

  private MediaInfoPlugin(Context context) {
    this.context = context;
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (thumbnailExecutor == null) {
      if (USE_EXOPLAYER) {
        thumbnailExecutor = Executors.newSingleThreadExecutor();
      } else {
        thumbnailExecutor =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
      }
    }

    if (mainThreadHandler == null) {
      mainThreadHandler = new Handler(Looper.myLooper());
    }

    if (call.method.equalsIgnoreCase("getMediaInfo")) {
      String path = (String) call.arguments;

      handleMediaInfo(context, path, result);
    } else if (call.method.equalsIgnoreCase("generateThumbnail")) {
      handleThumbnail(
          context,
          call.argument("path"),
          call.argument("target"),
          call.argument("width"),
          call.argument("height"),
          result,
          mainThreadHandler);
    }
  }

  private void handleMediaInfo(Context context, String path, Result result) {
    if (USE_EXOPLAYER) {
      handleMediaInfoExoPlayer(context, path, result);
    } else {
      handleMediaInfoMediaStore(path, result);
    }
  }

  private void handleMediaInfoExoPlayer(Context context, String path, Result result) {
    DefaultTrackSelector selector = new DefaultTrackSelector();
    SimpleExoPlayer exoPlayer = ExoPlayerFactory.newSimpleInstance(context, selector);

    int indexOfAudioRenderer = -1;
    for (int i = 0; i < exoPlayer.getRendererCount(); i++) {
      if (exoPlayer.getRendererType(i) == C.TRACK_TYPE_AUDIO) {
        indexOfAudioRenderer = i;
        break;
      }
    }

    selector.setRendererDisabled(indexOfAudioRenderer, true);

    exoPlayer.setPlayWhenReady(false);
    exoPlayer.addListener(
        new EventListener() {
          @Override
          public void onTracksChanged(
              TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

            if (trackSelections.length == 0 || trackSelections.get(0) == null) {
              result.error("MediaInfo", "TracksUnreadable", null);
              return;
            }

            Format format = trackSelections.get(0).getSelectedFormat();

            VideoDetail info =
                new VideoDetail(
                    format.width,
                    format.height,
                    format.frameRate,
                    exoPlayer.getDuration(),
                    (short) trackGroups.length,
                    format.sampleMimeType);
            result.success(info.toMap());
            Log.d(TAG, "releASE!!");
            exoPlayer.release();
          }

          @Override
          public void onPlayerError(ExoPlaybackException error) {
            Log.e(TAG, "Player Error for this file", error);
            result.error("MediaInfo", "TracksUnreadable", null);
            exoPlayer.release();
          }
        });
    DataSource.Factory dataSourceFactory =
        new DefaultDataSourceFactory(context, Util.getUserAgent(context, "media_info"));
    exoPlayer.prepare(
        new ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(Uri.fromFile(new File(path))));
  }

  private void handleMediaInfoMediaStore(String path, Result result) {
    VideoDetail info = VideoUtils.readVideoDetail(new File(path));

    if (info != null) {
      result.success(info.toMap());
    } else {
      result.error("MediaInfo", "InvalidFile", null);
    }
  }

  OutputSurface surface;

  private void handleThumbnail(
      Context context,
      String path,
      String targetPath,
      int width,
      int height,
      Result result,
      Handler mainThreadHandler) {

    final File target = new File(targetPath);

    thumbnailExecutor.submit(
        () -> {
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

          if (USE_EXOPLAYER) {
            CompletableFuture<String> future = new CompletableFuture<>();

            mainThreadHandler.post(
                () -> handleThumbnailExoPlayer(context, path, width, height, target, future));

            try {
              final String futureResult = future.get();
              mainThreadHandler.post(() -> result.success(futureResult));
            } catch (InterruptedException e) {
              mainThreadHandler.post(() -> result.error("MediaInfo", "Interrupted", null));
            } catch (ExecutionException e) {
              Log.e(TAG, "Execution exception", e);
              mainThreadHandler.post(() -> result.error("MediaInfo", "Misc", null));
            }
          } else {
            handleThumbnailMediaStore(
                context, path, width, height, result, mainThreadHandler, target);
          }
        });
  }

  private void handleThumbnailExoPlayer(
      Context context,
      String path,
      int width,
      int height,
      File target,
      CompletableFuture<String> future) {

    Log.d(TAG, "Start decoding: " + path + ", in res: " + width + " x " + height);

    DefaultTrackSelector selector = new DefaultTrackSelector();
    SimpleExoPlayer exoPlayer = ExoPlayerFactory.newSimpleInstance(context, selector);
    exoPlayer.setPlayWhenReady(false);

    int indexOfAudioRenderer = -1;
    for (int i = 0; i < exoPlayer.getRendererCount(); i++) {
      if (exoPlayer.getRendererType(i) == C.TRACK_TYPE_AUDIO) {
        indexOfAudioRenderer = i;
        break;
      }
    }

    selector.setRendererDisabled(indexOfAudioRenderer, true);

    surface = new OutputSurface(width, height);

    exoPlayer.setVideoSurface(surface.getSurface());
    exoPlayer.seekTo(1);

    final AtomicBoolean renderedFirstFrame = new AtomicBoolean(false);

    exoPlayer.addVideoListener(
        new VideoListener() {
          @Override
          public void onRenderedFirstFrame() {
            renderedFirstFrame.set(true);
          }
        });

    exoPlayer.addListener(
        new EventListener() {
          @Override
          public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            if (playbackState == ExoPlayer.STATE_READY) {
              if (renderedFirstFrame.get()) {
                try {
                  surface.awaitNewImage(500);
                } catch (Exception e) {
                  //
                }
                exoPlayer.release();

                surface.drawImage();

                try {
                  final Bitmap bitmap = surface.saveFrame();
                  bitmap.compress(CompressFormat.JPEG, 90, new FileOutputStream(target));
                  mainThreadHandler.post(() -> future.complete(target.getAbsolutePath()));
                  exoPlayer.release();
                } catch (IOException e) {
                  Log.e(TAG, "File not found", e);
                  exoPlayer.release();
                  future.completeExceptionally(e);
                }
              }
            }
          }

          @Override
          public void onPlayerError(ExoPlaybackException error) {
            Log.e(TAG, "Player Error for this file", error);
            exoPlayer.release();
            future.completeExceptionally(error);
          }
        });

    DataSource.Factory dataSourceFactory =
        new DefaultDataSourceFactory(context, Util.getUserAgent(context, "media_info"));
    exoPlayer.prepare(
        new ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(Uri.fromFile(new File(path))));
  }

  private void handleThumbnailMediaStore(
      Context context,
      String path,
      int width,
      int height,
      Result result,
      Handler mainThreadHandler,
      File target) {
    File file = ThumbnailUtils.generateVideoThumbnail(context, path, width, height);

    if (file != null && file.renameTo(target)) {
      mainThreadHandler.post(() -> result.success(target.getAbsolutePath()));
    } else {
      Log.e(TAG, "File does not generate or does not exist: " + file);
      mainThreadHandler.post(() -> result.error("MediaInfo", "FileCreationFailed", null));
    }
  }
}
