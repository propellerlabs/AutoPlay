package com.agentwaj.autoplay;

import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.danikula.videocache.HttpProxyCacheServer;

import java.util.List;

import static com.agentwaj.autoplay.VideoManager.log;

class MyAdapter extends BaseAdapter {

    private static final int VIEW_TYPE_VIDEO = 0;
    private static final int VIEW_TYPE_ARTICLE = 1;
    private static final int VIEW_TYPE_COUNT = 2;

    private List<String> items;
    private VideoManager videoManager;

    MyAdapter(ViewGroup container, HttpProxyCacheServer proxy, List<String> items) {
        this.items = items;
        videoManager = new VideoManager(container, proxy);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        log("Item " + position);
        int viewType = getItemViewType(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(getLayout(viewType), parent, false);
            convertView.setTag(viewType);
        } else {
            TextureView textureView = (TextureView) convertView.findViewById(R.id.video);
            videoManager.stopTracking(textureView);
            if (!convertView.getTag().equals(viewType)) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(getLayout(viewType), parent, false);
                convertView.setTag(viewType);
            }
        }

        switch (viewType) {
            case VIEW_TYPE_VIDEO:
                String id = "Item " + position;
                ((TextView) convertView.findViewById(R.id.description)).setText(id);

                TextureView textureView = (TextureView) convertView.findViewById(R.id.video);
                textureView.setSurfaceTextureListener(videoManager);
                String source = position % 2 == 0 ?
                        "http://mirrors.standaloneinstaller.com/video-sample/lion-sample.3gp" :
                        "http://mirrors.standaloneinstaller.com/video-sample/page18-movie-4.3gp";
                videoManager.startTracking(id, source, textureView);
                break;
            default:
                ((TextView) convertView.findViewById(android.R.id.text1)).setText(items.get(position));
                break;
        }

        return convertView;
    }

    private int getLayout(int viewType) {
        switch (viewType) {
            case VIEW_TYPE_VIDEO:
                return R.layout.list_item_video;
            default:
                return android.R.layout.simple_list_item_1;
        }
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        return position % 15 == 0 ? VIEW_TYPE_VIDEO : VIEW_TYPE_ARTICLE;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    @Override
    public int getCount() {
        return items.size();
    }
}
