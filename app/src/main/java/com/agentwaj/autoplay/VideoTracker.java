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

class VideoTracker implements TextureView.SurfaceTextureListener {

    private static final String TAG = VideoTracker.class.getName();
    private static final int VISIBLE_THRESHOLD = 50;

    private Set<VideoState> trackedViews;
    private HttpProxyCacheServer proxy;
    private MediaPlayer mediaPlayer;

    VideoTracker(ViewGroup container, HttpProxyCacheServer proxy) {
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
                        startMediaPlayer(textureView.getSurfaceTexture());
                    }
                }
            }
        });
    }

    private void startMediaPlayer(SurfaceTexture surfaceTexture) {
        final VideoState videoState = getVideoStateForSurfaceTexture(surfaceTexture);
        if (videoState == null) {
            return;
        }

        String source = "http://mirrors.standaloneinstaller.com/video-sample/lion-sample.3gp";
        Surface surface = new Surface(surfaceTexture);
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(proxy.getProxyUrl(source));
            mediaPlayer.setSurface(surface);
            mediaPlayer.prepareAsync();
            mediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.seekTo(videoState.position);
                    mp.start();
                    videoState.prepared = true;
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

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        startMediaPlayer(surfaceTexture);
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

    void startTracking(String id, TextureView textureView) {
        VideoState videoState = getVideoStateForId(id);
        if (videoState == null) {
            videoState = new VideoState(id, textureView);
            trackedViews.add(videoState);
        } else {
            videoState.textureView = textureView;
        }
        // The SurfaceTexture may be available if the view is being recycled
        if (textureView.isAvailable()) {
            videoState.prepared = true;
        }
        log(trackedViews.size() + " views are being tracked.");
    }

    void stopTracking(final TextureView textureView) {
        for (VideoState videoState : trackedViews) {
            if (videoState.textureView != null && videoState.textureView.equals(textureView)) {
                videoState.textureView = null;
                videoState.prepared = false;
            }
        }
    }

    static void log(String message) {
        Log.d(TAG, message);
    }
}
