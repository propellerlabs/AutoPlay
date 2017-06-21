package com.agentwaj.autoplay;

import android.view.TextureView;

class VideoState {

    TextureView textureView;
    String description;
    boolean prepared;
    int position;

    VideoState(TextureView textureView, String description) {
        this.textureView = textureView;
        this.description = description;

        prepared = false;
        position = 0;
    }
}
