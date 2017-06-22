package com.agentwaj.autoplay;

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

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

                    Rect viewRect = new Rect();
                    if (!textureView.getLocalVisibleRect(viewRect) || !textureView.isShown()) {
                        log(videoState.id + " is not visible");
                        continue;
                    }

                    int visibleWidth = viewRect.right - viewRect.left;
                    int visibleHeight = viewRect.bottom - viewRect.top;
                    int visibleArea = visibleWidth * visibleHeight;
                    int viewArea = textureView.getWidth() * textureView.getHeight();
                    int visibleAreaPercent = viewArea == 0 ? 0 :
                            (int) (((float) visibleArea) / viewArea * 100f);
                    log(videoState.id + " is " + visibleAreaPercent + "% visible");

                    if (visibleAreaPercent < VISIBLE_THRESHOLD && mediaPlayer.isPlaying()) {
                        videoState.position = mediaPlayer.getCurrentPosition();
                        mediaPlayer.reset();
                    } else if (visibleAreaPercent >= VISIBLE_THRESHOLD && !mediaPlayer.isPlaying()) {
                        videoState.shouldPlay = true;
                        prepareMediaPlayer(textureView.getSurfaceTexture());
                    }
                }
            }
        });
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

    void startTracking(String id, String source, TextureView textureView) {
        VideoState videoState = getVideoStateForId(id);
        if (videoState == null) {
            videoState = new VideoState(id, source, textureView);
            trackedViews.add(videoState);
        } else {
            videoState.textureView = textureView;
        }
        // The SurfaceTexture may be available if the view is being recycled
        if (textureView.isAvailable()) {
            videoState.shouldPlay = false;
            prepareMediaPlayer(textureView.getSurfaceTexture());
        }
        log(trackedViews.size() + " views are being tracked.");
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
