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
import java.util.Iterator;
import java.util.Set;

class VideoTracker implements TextureView.SurfaceTextureListener {

    private static final String TAG = VideoTracker.class.getName();
    private static final int VISIBLE_THRESHOLD = 70;

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
                        log(videoState.description + " not prepared");
                        continue;
                    }
                    TextureView textureView = videoState.textureView;

                    Rect viewRect = new Rect();
                    if (!textureView.getLocalVisibleRect(viewRect) || !textureView.isShown()) {
                        log(videoState.description + " is not visible");
                        continue;
                    }

                    int visibleWidth = viewRect.right - viewRect.left;
                    int visibleHeight = viewRect.bottom - viewRect.top;
                    int visibleArea = visibleWidth * visibleHeight;
                    int viewArea = textureView.getWidth() * textureView.getHeight();
                    int visibleAreaPercent = viewArea == 0 ? 0 :
                            (int) (((float) visibleArea) / viewArea * 100f);
                    log(videoState.description + " is " + visibleAreaPercent + "% visible");

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
                    updateTrackedViews();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VideoState getVideoStateForSurfaceTexture(SurfaceTexture surfaceTexture) {
        for (VideoState videoState : trackedViews) {
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

    private void updateTrackedViews() {
        for (VideoState videoState : trackedViews) {
            videoState.prepared = true;
        }
    }

    void startTracking(final String description, final TextureView textureView) {
        trackedViews.add(new VideoState(textureView, description));
    }

    void stopTracking(final TextureView textureView) {
        if (textureView == null) {
            return;
        }
        Iterator<VideoState> it = trackedViews.iterator();
        while (it.hasNext()) {
            if (it.next().textureView.equals(textureView)) {
                it.remove();
                break;
            }
        }
    }

    private static void log(String message) {
        Log.d(TAG, message);
    }
}
