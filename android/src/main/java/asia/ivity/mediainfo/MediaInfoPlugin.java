package asia.ivity.mediainfo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player.EventListener;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
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

    private static final String FORMAT_SS = "ss";
    private static final String FORMAT_DASH = "dash";
    private static final String FORMAT_HLS = "hls";
    private static final String FORMAT_OTHER = "other";

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
                        //            exoPlayer.release();
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
                        .createMediaSource(Uri.fromFile(new File(path))));
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
//    ensureExoPlayer();
//    ensureSurface(width, height);

        LoadControl loadControl = new DefaultLoadControl
                .Builder()
                .setBufferDurationsMs(5, 10, 5, 5)
                .build();

        exoPlayer = new SimpleExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .build();


        exoPlayer.setPlayWhenReady(false);
      exoPlayer.stop(true);

        Uri uri = Uri.parse(path);
//      Uri uri = Uri.parse("https://flutter.github.io/assets-for-api-docs/assets/videos/butterfly.mp4");

        String userAgent = Util.getUserAgent(context, "media_info");
        DataSource.Factory dataSourceFactory;
        if (isHTTP(uri)) {
            dataSourceFactory =
                    new DefaultHttpDataSourceFactory(
                            userAgent,
                            null,
                            DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                            DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                            true);
        } else {
            dataSourceFactory = new DefaultDataSourceFactory(context, userAgent);
        }

        long startTime = System.currentTimeMillis();
        long currentTime;

        MediaSource mediaSource = buildMediaSource(uri, dataSourceFactory, context);

        final EventListener eventListener =
                new EventListener() {
                    @Override
                    public void onPlayerError(ExoPlaybackException error) {
                        Log.d("XYZ", "completeExceptionally");

                        future.completeExceptionally(error);
                    }
                };

        exoPlayer.addListener(eventListener);

        future.whenComplete(
                (s, throwable) -> {
                    Log.d("XYZ", "future.whenComplete");
                    exoPlayer.removeListener(eventListener);
                });

        currentTime = System.currentTimeMillis();
        Log.d("XYZ", "0. " + (currentTime - startTime));


        currentTime = System.currentTimeMillis();
        Log.d("XYZ", "1. " + (currentTime - startTime));
        ensureSurface(width, height);
        currentTime = System.currentTimeMillis();
        Log.d("XYZ", "2. " + (currentTime - startTime));
        surface.setFrameFinished(
                () -> {
                    long newTime;
                    newTime = System.currentTimeMillis();
                    Log.d("XYZ", "11. " + (newTime - startTime));
                    try {

                        surface.awaitNewImage(500);
                        newTime = System.currentTimeMillis();
                        Log.d("XYZ", "12. " + (newTime - startTime));
                    } catch (Exception e) {
                        //
                    }

                    surface.drawImage();

                    newTime = System.currentTimeMillis();
                    Log.d("XYZ", "13. " + (newTime - startTime));

                    try {
                        final Bitmap bitmap = surface.saveFrame();
                        newTime = System.currentTimeMillis();
                        Log.d("XYZ", "14. " + (newTime - startTime));
                        bitmap.compress(CompressFormat.JPEG, 90, new FileOutputStream(target));
                        newTime = System.currentTimeMillis();
                        Log.d("XYZ", "15. " + (newTime - startTime));
                        bitmap.recycle();
                        newTime = System.currentTimeMillis();
                        Log.d("XYZ", "16. " + (newTime - startTime));

                        Log.d("XYZ", "complete");
                        future.complete(target.getAbsolutePath());
                        newTime = System.currentTimeMillis();
                        Log.d("XYZ", "17. " + (newTime - startTime));
                        releaseExoPlayerAndResources();
                    } catch (IOException e) {
                        Log.d("XYZ", "IOException");
                        future.completeExceptionally(e);
                        newTime = System.currentTimeMillis();
                        Log.d("XYZ", "18. " + (newTime - startTime));
                    }
                });
        currentTime = System.currentTimeMillis();
        Log.d("XYZ", "3. " + (currentTime - startTime));
        exoPlayer.setMediaSource(mediaSource);


        exoPlayer.seekTo(positionMs);
        currentTime = System.currentTimeMillis();
        Log.d("XYZ", "4. " + (currentTime - startTime));
        exoPlayer.prepare(); // todo
        currentTime = System.currentTimeMillis();
        Log.d("XYZ", "5. " + (currentTime - startTime));


    }

    private static boolean isHTTP(Uri uri) {
        if (uri == null || uri.getScheme() == null) {
            return false;
        }
        String scheme = uri.getScheme();
        return scheme.equals("http") || scheme.equals("https");
    }

    private MediaSource buildMediaSource(
            Uri uri, DataSource.Factory mediaDataSourceFactory, Context context) {
        int type = Util.inferContentType(uri.getLastPathSegment());

        switch (type) {
            case C.TYPE_SS:
                return new SsMediaSource.Factory(
                        new DefaultSsChunkSource.Factory(mediaDataSourceFactory),
                        new DefaultDataSourceFactory(context, null, mediaDataSourceFactory))
                        .createMediaSource(MediaItem.fromUri(uri));
            case C.TYPE_DASH:
                return new DashMediaSource.Factory(
                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory),
                        new DefaultDataSourceFactory(context, null, mediaDataSourceFactory))
                        .createMediaSource(MediaItem.fromUri(uri));
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(mediaDataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(uri));
            case C.TYPE_OTHER:
                return new ProgressiveMediaSource.Factory(mediaDataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(uri));
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
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
