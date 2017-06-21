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

import java.util.HashMap;
import java.util.Map;

class VideoTracker implements TextureView.SurfaceTextureListener {

    private static final String TAG = VideoTracker.class.getName();

    private Map<String, TextureView> viewsToBeTracked;
    private Map<String, TextureView> viewsBeingTracked;
    private HttpProxyCacheServer proxy;
    private MediaPlayer mediaPlayer;

    VideoTracker(ViewGroup container, HttpProxyCacheServer proxy) {
        this.proxy = proxy;

        viewsToBeTracked = new HashMap<>();
        viewsBeingTracked = new HashMap<>();

        container.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                for (String key : viewsBeingTracked.keySet()) {
                    TextureView textureView = viewsBeingTracked.get(key);

                    Rect viewRect = new Rect();
                    if (!textureView.getLocalVisibleRect(viewRect) || !textureView.isShown()) {
                        log(key + " is not visible");
                        continue;
                    }

                    int visibleWidth = viewRect.right - viewRect.left;
                    int visibleHeight = viewRect.bottom - viewRect.top;
                    int visibleArea = visibleWidth * visibleHeight;
                    int viewArea = textureView.getWidth() * textureView.getHeight();
                    int visibleAreaPercent = viewArea == 0 ? 0 :
                            (int) (((float) visibleArea) / viewArea * 100f);
                    log(key + " is " + visibleAreaPercent + "% visible");
                }
            }
        });
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
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
//            mediaPlayer.setOnBufferingUpdateListener(this);
//            mediaPlayer.setOnCompletionListener(this);
//            mediaPlayer.setOnVideoSizeChangedListener(this);
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });
            mediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
            updateTrackedViews();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        for (String key : viewsToBeTracked.keySet()) {
            viewsBeingTracked.put(key, viewsToBeTracked.remove(key));
        }
    }

    void startTracking(final String key, final TextureView textureView) {
        viewsToBeTracked.put(key, textureView);
    }

    void stopTracking(final TextureView textureView) {
        if (textureView == null) {
            return;
        }
        for (String key : viewsBeingTracked.keySet()) {
            if (viewsBeingTracked.get(key).equals(textureView)) {
                viewsBeingTracked.remove(key);
            }
        }

        for (String key : viewsToBeTracked.keySet()) {
            if (viewsToBeTracked.get(key).equals(textureView)) {
                viewsToBeTracked.remove(key);
            }
        }
    }

    private static void log(String message) {
        Log.d(TAG, message);
    }
}
