package com.agentwaj.autoplay;

import android.view.TextureView;

class VideoState {

    String id;
    TextureView textureView;
    boolean prepared;
    int position;

    VideoState(String id, TextureView textureView) {
        this.id = id;
        this.textureView = textureView;

        prepared = false;
        position = 0;
    }
}
