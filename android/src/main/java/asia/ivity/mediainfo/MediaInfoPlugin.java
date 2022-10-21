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
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player.Listener;
import com.google.android.exoplayer2.Tracks;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.common.collect.ImmutableList;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import java9.util.concurrent.CompletableFuture;

public class MediaInfoPlugin implements MethodCallHandler, FlutterPlugin {

  private static final String TAG = "MediaInfoPlugin";

  private static final String NAMESPACE = "asia.ivity.flutter";

  private static final boolean USE_EXOPLAYER = true;

  private Context applicationContext;
  private MethodChannel methodChannel;

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

    if (executorService != null) {
      executorService.shutdown();
    }

    releaseExoPlayerAndResources();
  }

  private ThreadPoolExecutor executorService;

  private Handler mainThreadHandler;

  private ExoPlayer exoPlayer;

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
      Integer positionMs = call.argument("positionMs");

      if (width == null || height == null) {
        result.error("MediaInfo", "invalid-dimensions", null);
        return;
      }

      if (positionMs == null) {
        positionMs = 0;
      }

      handleThumbnail(
          applicationContext,
          call.argument("path"),
          call.argument("target"),
          width,
          height,
          positionMs,
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

            mainThreadHandler.postDelayed(() -> {
              if (executorService.getQueue().size() < 1) {
                releaseExoPlayerAndResources();
              }
            }, 3000);
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

    final Listener listener =
        new Listener() {
          @Override
          public void onTracksChanged(Tracks tracks) {
            ImmutableList<Tracks.Group> trackGroups = tracks.getGroups();

            if (trackGroups.size() == 0) {
              Log.d(TAG, "Tracks Changed, track groups currently empty");
              return;
            }

            for (int i = 0; i < trackGroups.size(); i++) {
              final Tracks.Group group = trackGroups.get(i);

              if (group.getType() != C.TRACK_TYPE_VIDEO) {
                continue;
              }

              for (int j = 0; j < group.length; j++) {
                final Format format = group.getTrackFormat(j);

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
                    //noinspection SuspiciousNameCombination
                    width = height;
                    height = temp;
                  }

                  VideoDetail info =
                      new VideoDetail(
                          width,
                          height,
                          format.frameRate,
                          exoPlayer.getDuration(),
                          (short) trackGroups.size(),
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
          public void onPlayerError(@NonNull PlaybackException error) {
            future.completeExceptionally(error);
          }
        };

    exoPlayer.addListener(listener);

    future.whenComplete((videoDetail, throwable) -> exoPlayer.removeListener(listener));

    DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(context);
    exoPlayer.setMediaSource(
        new ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(Uri.parse(path))));
    exoPlayer.prepare();
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
                () ->
                    handleThumbnailExoPlayer(
                        context, path, width, height, positionMs, target, future));

            try {
              Log.d(TAG, "Await thumbnail result.");
              final String futureResult = future.get();
              Log.d(TAG, "Received thumbnail result.");
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
          Log.d(TAG, "handleThumbnailExoPlayer::setFrameFinished::init ");

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

    final Listener eventListener =
        new Listener() {
          @Override
          public void onPlayerError(@NonNull PlaybackException error) {
            Log.e(TAG, "Player error", error);
            future.completeExceptionally(error);
          }

          @Override
          public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
            Log.d(TAG, "onPlayWhenReadyChanged");

            exoPlayer.seekTo(positionMs);
            Log.d(TAG, "onPlayWhenReadyChanged - Done seekTo: " + positionMs);
          }
        };

    exoPlayer.addListener(eventListener);

    future.whenComplete((s, throwable) -> exoPlayer.removeListener(eventListener));

    DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(context);

    exoPlayer.setMediaSource(
        new ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(Uri.parse(path))),
        true);
    exoPlayer.prepare();
    Log.d(TAG, "done prepare..");

    exoPlayer.setPlayWhenReady(true);
    Log.d(TAG, "done setPlayWhenReady..");
  }

  private synchronized void ensureExoPlayer() {
    if (exoPlayer == null) {
      DefaultTrackSelector selector = new DefaultTrackSelector(applicationContext);
      exoPlayer = new ExoPlayer.Builder(applicationContext).setTrackSelector(selector).build();

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
    //    exoPlayer.stop(true);
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
