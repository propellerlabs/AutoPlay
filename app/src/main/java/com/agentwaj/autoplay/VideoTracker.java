package com.agentwaj.autoplay;

import android.graphics.Rect;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.VideoView;

import java.util.HashMap;
import java.util.Map;

class VideoTracker {

    private static final String TAG = VideoTracker.class.getName();

    private Map<String, VideoView> viewsBeingTracked;

    VideoTracker(ViewGroup container) {

        viewsBeingTracked = new HashMap<>();

        container.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                for (String key : viewsBeingTracked.keySet()) {
                    VideoView videoView = viewsBeingTracked.get(key);

                    Rect viewRect = new Rect();
                    if (!videoView.getLocalVisibleRect(viewRect) || !videoView.isShown()) {
                        log(key + " is not visible");
                        continue;
                    }

                    int visibleWidth = viewRect.right - viewRect.left;
                    int visibleHeight = viewRect.bottom - viewRect.top;
                    int visibleArea = visibleWidth * visibleHeight;
                    int viewArea = videoView.getWidth() * videoView.getHeight();
                    int visibleAreaPercent = viewArea == 0 ? 0 :
                            (int) (((float) visibleArea) / viewArea * 100f);
                    log(key + " is " + visibleAreaPercent + "% visible");
                }
            }
        });
    }

    void startTracking(String key, VideoView videoView) {
        viewsBeingTracked.put(key, videoView);
    }

    void stopTracking(String key) {
        viewsBeingTracked.remove(key);
    }

    private static void log(String message) {
        Log.d(TAG, message);
    }
}
