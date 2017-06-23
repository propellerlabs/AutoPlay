package com.agentwaj.autoplay;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.danikula.videocache.HttpProxyCacheServer;

import java.util.HashSet;
import java.util.Set;

class VideoManager implements TextureView.SurfaceTextureListener {

    private static final String TAG = VideoManager.class.getName();
    private static final int VISIBLE_THRESHOLD = 50;

    private Set<VideoState> trackedViews;
    private HttpProxyCacheServer proxy;
    private MediaPlayer mediaPlayer;

    VideoManager(ViewGroup container, HttpProxyCacheServer proxy) {
        this.proxy = proxy;

        trackedViews = new HashSet<>();

        container.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                for (VideoState videoState : trackedViews) {
                    if (!videoState.prepared) {
                        log(videoState.id + " not prepared");
                        continue;
                    }
                    TextureView textureView = videoState.textureView;

                    int visibleAreaPercent = getVisibleAreaPercent(textureView);
                    if (visibleAreaPercent == 0) {
                        log(videoState.id + " is not visible");
                        continue;
                    }

                    log(videoState.id + " is " + visibleAreaPercent + "% visible");

                    if (visibleAreaPercent < VISIBLE_THRESHOLD && mediaPlayer.isPlaying()) {
                        pauseMediaPlayer(videoState);
                    } else if (visibleAreaPercent >= VISIBLE_THRESHOLD && !mediaPlayer.isPlaying() && !videoState.isPaused) {
                        videoState.shouldPlay = true;
                        prepareMediaPlayer(textureView.getSurfaceTexture());
                    }
                }
            }
        });
    }

    private int getVisibleAreaPercent(TextureView textureView) {
        Rect viewRect = new Rect();
        if (!textureView.getLocalVisibleRect(viewRect) || !textureView.isShown()) {
            return 0;
        }

        int visibleWidth = viewRect.right - viewRect.left;
        int visibleHeight = viewRect.bottom - viewRect.top;
        int visibleArea = visibleWidth * visibleHeight;
        int viewArea = textureView.getWidth() * textureView.getHeight();
        return viewArea == 0 ? 0 : (int) (((float) visibleArea) / viewArea * 100f);
    }

    private void prepareMediaPlayer(SurfaceTexture surfaceTexture) {
        final VideoState videoState = getVideoStateForSurfaceTexture(surfaceTexture);
        if (videoState == null) {
            return;
        }

        Surface surface = new Surface(surfaceTexture);
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(proxy.getProxyUrl(videoState.source));
            mediaPlayer.setSurface(surface);
            mediaPlayer.prepareAsync();
            mediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    videoState.prepared = true;
                    mp.seekTo(videoState.position);
                    if (videoState.shouldPlay) {
                        mp.start();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pauseMediaPlayer(VideoState videoState) {
        if (mediaPlayer.isPlaying()) {
            videoState.position = mediaPlayer.getCurrentPosition();
        }
        mediaPlayer.reset();
    }

    private VideoState getVideoStateForId(String id) {
        for (VideoState videoState : trackedViews) {
            if (videoState.id.equals(id)) {
                return videoState;
            }
        }
        return null;
    }

    private VideoState getVideoStateForSurfaceTexture(SurfaceTexture surfaceTexture) {
        for (VideoState videoState : trackedViews) {
            if (videoState.textureView == null) {
                continue;
            }
            SurfaceTexture currentSurfaceTexture = videoState.textureView.getSurfaceTexture();
            if (currentSurfaceTexture == null) {
                continue;
            }
            if (currentSurfaceTexture.equals(surfaceTexture)) {
                return videoState;
            }
        }
        return null;
    }

    void startTracking(String id, String source, TextureView textureView, boolean shouldPlay) {
        VideoState videoState = getVideoStateForId(id);
        if (videoState == null) {
            videoState = new VideoState(id, source, textureView);
            trackedViews.add(videoState);
        } else {
            videoState.textureView = textureView;
        }
        // The SurfaceTexture may be available if the view is being recycled
        if (textureView.isAvailable()) {
            videoState.shouldPlay = shouldPlay;
            prepareMediaPlayer(textureView.getSurfaceTexture());
        }
        log(trackedViews.size() + " views are being tracked.");

        final VideoState finalVideoState = videoState;
        textureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!finalVideoState.isFullscreen) {
                    goFullscreen(v.getContext(), finalVideoState);
                } else if (mediaPlayer.isPlaying()) {
                    finalVideoState.shouldPlay = false;
                    finalVideoState.isPaused = true;
                    pauseMediaPlayer(finalVideoState);
                } else {
                    finalVideoState.shouldPlay = true;
                    finalVideoState.isPaused = false;
                    prepareMediaPlayer(finalVideoState.textureView.getSurfaceTexture());
                }
            }
        });
    }

    void stopTracking(final TextureView textureView) {
        for (VideoState videoState : trackedViews) {
            if (videoState.textureView != null && videoState.textureView.equals(textureView)) {
                videoState.textureView = null;
                videoState.prepared = false;
                videoState.shouldPlay = false;
            }
        }
    }

    private void goFullscreen(Context context, final VideoState videoState) {
        videoState.isFullscreen = true;
        pauseMediaPlayer(videoState);
        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar);
        //noinspection ConstantConditions
        dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
        dialog.setContentView(R.layout.fullscreen_video);
        dialog.show();

        final TextureView previousTextureView = videoState.textureView;
        stopTracking(previousTextureView);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                videoState.isFullscreen = false;
                pauseMediaPlayer(videoState);
                startTracking(videoState.id, videoState.source, previousTextureView, videoState.shouldPlay);
            }
        });

        ((TextView) dialog.findViewById(R.id.description)).setText(videoState.id);

        videoState.shouldPlay = true;
        videoState.isPaused = false;
        TextureView textureView = (TextureView) dialog.findViewById(R.id.video);
        textureView.setSurfaceTextureListener(this);
        startTracking(videoState.id, videoState.source, textureView, true);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        prepareMediaPlayer(surfaceTexture);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    static void log(String message) {
        Log.d(TAG, message);
    }
}
