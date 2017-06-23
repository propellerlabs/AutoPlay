package com.agentwaj.autoplay;

import android.view.TextureView;

class VideoState {

    String id;
    String source;
    TextureView textureView;
    boolean prepared;
    boolean shouldPlay;
    boolean isPaused;
    boolean isFullscreen;
    boolean hasOpenedFullscreen;
    int position;

    VideoState(String id, String source, TextureView textureView) {
        this.id = id;
        this.source = source;
        this.textureView = textureView;

        prepared = false;
        shouldPlay = false;
        isPaused = false;
        isFullscreen = false;
        hasOpenedFullscreen = false;
        position = 0;
    }
}
