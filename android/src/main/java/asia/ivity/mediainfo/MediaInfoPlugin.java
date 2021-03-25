package asia.ivity.mediainfo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Player.EventListener;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import asia.ivity.mediainfo.util.OutputSurface;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import java9.util.concurrent.CompletableFuture;

public class MediaInfoPlugin implements MethodCallHandler, FlutterPlugin {

  private static final String NAMESPACE = "asia.ivity.flutter";

  private static final boolean USE_EXOPLAYER = true;

  private Context applicationContext;
  private MethodChannel methodChannel;

  public static void registerWith(Registrar registrar) {
    final MediaInfoPlugin instance = new MediaInfoPlugin();
    instance.onAttachedToEngine(registrar.context(), registrar.messenger());
  }

  @Override
  public void onAttachedToEngine(FlutterPluginBinding binding) {
    onAttachedToEngine(binding.getApplicationContext(), binding.getBinaryMessenger());
  }

  private void onAttachedToEngine(Context applicationContext, BinaryMessenger messenger) {
    this.applicationContext = applicationContext;
    methodChannel = new MethodChannel(messenger, NAMESPACE + "/media_info");
    methodChannel.setMethodCallHandler(this);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    applicationContext = null;
    methodChannel.setMethodCallHandler(null);
    methodChannel = null;
  }

  private ThreadPoolExecutor executorService;

  private Handler mainThreadHandler;

  private SimpleExoPlayer exoPlayer;

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (executorService == null) {
      if (USE_EXOPLAYER) {
        executorService =
            new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
      } else {
        executorService =
            (ThreadPoolExecutor)
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
      }
    }

    if (mainThreadHandler == null) {
      mainThreadHandler = new Handler(Looper.myLooper());
    }

    if (call.method.equalsIgnoreCase("getMediaInfo")) {
      String path = (String) call.arguments;

      handleMediaInfo(applicationContext, path, result);
    } else if (call.method.equalsIgnoreCase("generateThumbnail")) {
      Integer width = call.argument("width");
      Integer height = call.argument("height");

      if (width == null || height == null) {
        result.error("MediaInfo", "invalid-dimensions", null);
        return;
      }

      handleThumbnail(
          applicationContext,
          call.argument("path"),
          call.argument("target"),
          width,
          height,
          call.argument("positionMs"),
          result,
          mainThreadHandler);
    }
  }

  private void handleMediaInfo(Context context, String path, Result result) {
    if (USE_EXOPLAYER) {
      final CompletableFuture<MediaDetail> future = new CompletableFuture<>();

      executorService.execute(
          () -> {
            mainThreadHandler.post(() -> handleMediaInfoExoPlayer(context, path, future));

            try {
              MediaDetail info = future.get();
              mainThreadHandler.post(
                  () -> {
                    if (info != null) {
                      result.success(info.toMap());
                    } else {
                      result.error("MediaInfo", "InvalidFile", null);
                    }
                  });

            } catch (InterruptedException e) {
              mainThreadHandler.post(() -> result.error("MediaInfo", e.getMessage(), null));
            } catch (ExecutionException e) {
              mainThreadHandler.post(
                  () -> result.error("MediaInfo", e.getCause().getMessage(), null));
            }

            if (executorService.getQueue().size() < 1) {
              mainThreadHandler.post(this::releaseExoPlayerAndResources);
            }
          });
    } else {
      executorService.execute(
          () -> {
            final VideoDetail info = handleMediaInfoMediaStore(path);

            mainThreadHandler.post(
                () -> {
                  if (info != null) {
                    result.success(info.toMap());
                  } else {
                    result.error("MediaInfo", "InvalidFile", null);
                  }
                });
          });
    }
  }

  private void handleMediaInfoExoPlayer(
      Context context, String path, CompletableFuture<MediaDetail> future) {

    ensureExoPlayer();
    exoPlayer.clearVideoSurface();

    final EventListener listener =
        new EventListener() {
          @Override
          public void onTracksChanged(
              TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

            for (int i = 0; i < trackGroups.length; i++) {
              TrackGroup tg = trackGroups.get(i);
              for (int j = 0; j < tg.length; j++) {
                final Format format = tg.getFormat(j);

                final String mimeType = format.sampleMimeType;
                if (mimeType == null) {
                  continue;
                }

                if (mimeType.contains("video")) {
                  int width = format.width;
                  int height = format.height;
                  int rotation = format.rotationDegrees;

                  // Switch the width/height if video was taken in portrait mode
                  if (rotation == 90 || rotation == 270) {
                    int temp = width;
                    width = height;
                    height = temp;
                  }

                  VideoDetail info =
                      new VideoDetail(
                          width,
                          height,
                          format.frameRate,
                          exoPlayer.getDuration(),
                          (short) trackGroups.length,
                          mimeType);
                  future.complete(info);
                  return;
                } else if (mimeType.contains("audio")) {
                  AudioDetail audio =
                      new AudioDetail(exoPlayer.getDuration(), format.bitrate, mimeType);
                  future.complete(audio);
                  return;
                }
              }
            }

            future.completeExceptionally(new IOException("TracksUnreadable"));
          }

          @Override
          public void onPlayerError(ExoPlaybackException error) {
            future.completeExceptionally(error);
          }
        };

    exoPlayer.addListener(listener);

    future.whenComplete(
        (videoDetail, throwable) -> {
          exoPlayer.removeListener(listener);
        });

    DataSource.Factory dataSourceFactory =
        new DefaultDataSourceFactory(context, Util.getUserAgent(context, "media_info"));
    exoPlayer.prepare(
        new ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(Uri.parse(path)));
  }

  private VideoDetail handleMediaInfoMediaStore(String path) {
    return VideoUtils.readVideoDetail(new File(path));
  }

  private OutputSurface surface;

  private void handleThumbnail(
      Context context,
      String path,
      String targetPath,
      int width,
      int height,
      int positionMs,
      Result result,
      Handler mainThreadHandler) {

    final File target = new File(targetPath);

    executorService.submit(
        () -> {
          if (target.exists()) {
            mainThreadHandler.post(() -> result.error("MediaInfo", "FileOverwriteDenied", null));
            return;
          }

          if (context == null) {
            mainThreadHandler.post(() -> result.error("MediaInfo", "ContextDisappeared", null));

            return;
          }

          if (USE_EXOPLAYER) {
            CompletableFuture<String> future = new CompletableFuture<>();

            mainThreadHandler.post(
                () -> handleThumbnailExoPlayer(context, path, width, height, positionMs, target, future));

            try {
              final String futureResult = future.get();
              mainThreadHandler.post(() -> result.success(futureResult));
            } catch (InterruptedException e) {
              mainThreadHandler.post(() -> result.error("MediaInfo", "Interrupted", null));
            } catch (ExecutionException e) {
              mainThreadHandler.post(() -> result.error("MediaInfo", "Misc", null));
            }

            if (executorService.getQueue().size() < 1) {
              mainThreadHandler.post(this::releaseExoPlayerAndResources);
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
      int positionMs,
      File target,
      CompletableFuture<String> future) {
    ensureExoPlayer();
    ensureSurface(width, height);

    surface.setFrameFinished(
        () -> {
          try {
            surface.awaitNewImage(500);
          } catch (Exception e) {
            //
          }

          surface.drawImage();

          try {
            final Bitmap bitmap = surface.saveFrame();
            bitmap.compress(CompressFormat.JPEG, 90, new FileOutputStream(target));
            bitmap.recycle();

            future.complete(target.getAbsolutePath());
          } catch (IOException e) {
            future.completeExceptionally(e);
          }
        });

    final EventListener eventListener =
        new EventListener() {
          @Override
          public void onPlayerError(ExoPlaybackException error) {
            future.completeExceptionally(error);
          }
        };

    exoPlayer.addListener(eventListener);

    future.whenComplete(
        (s, throwable) -> {
          exoPlayer.removeListener(eventListener);
        });

    DataSource.Factory dataSourceFactory =
        new DefaultDataSourceFactory(context, Util.getUserAgent(context, "media_info"));
      exoPlayer.seekTo(positionMs);
      exoPlayer.prepare(
              new ProgressiveMediaSource.Factory(dataSourceFactory)
                      .createMediaSource(Uri.parse(path)),
              false,
              false);
  }

  private synchronized void ensureExoPlayer() {
    if (exoPlayer == null) {
      DefaultTrackSelector selector = new DefaultTrackSelector();
      exoPlayer = ExoPlayerFactory.newSimpleInstance(applicationContext, selector);

      int indexOfAudioRenderer = -1;
      for (int i = 0; i < exoPlayer.getRendererCount(); i++) {
        if (exoPlayer.getRendererType(i) == C.TRACK_TYPE_AUDIO) {
          indexOfAudioRenderer = i;
          break;
        }
      }

      selector.setParameters(
          selector.getParameters().buildUpon().setRendererDisabled(indexOfAudioRenderer, true));
    }

    exoPlayer.setPlayWhenReady(false);
    exoPlayer.stop(true);
  }

  private void ensureSurface(int width, int height) {
    if (surface == null || surface.getWidth() != width || surface.getHeight() != height) {
      if (surface != null) {
        surface.release();
      }

      surface = new OutputSurface(width, height);
    }

    exoPlayer.setVideoSurface(surface.getSurface());
  }

  private void releaseExoPlayerAndResources() {
    if (exoPlayer != null) {
      exoPlayer.release();
      exoPlayer = null;
    }

    if (surface != null) {
      surface.release();
      surface = null;
    }
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
      mainThreadHandler.post(() -> result.error("MediaInfo", "FileCreationFailed", null));
    }
  }
}
