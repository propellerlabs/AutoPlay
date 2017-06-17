package com.agentwaj.autoplay;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.VideoView;

import java.util.List;

class MyAdapter extends BaseAdapter {

    private static final int VIEW_TYPE_VIDEO = 0;
    private static final int VIEW_TYPE_ARTICLE = 1;
    private static final int VIEW_TYPE_COUNT = 2;

    private List<String> items;

    MyAdapter(List<String> items) {
        this.items = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int viewType = getItemViewType(position);

        if (convertView == null || convertView.getTag() != (viewType + "")) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(getLayout(viewType), parent, false);
            convertView.setTag(viewType + "");
        }

        switch (viewType) {
            case VIEW_TYPE_VIDEO:
                VideoView videoView = (VideoView) convertView.findViewById(R.id.video);
                videoView.setVideoPath("http://www.sample-videos.com/video/3gp/240/big_buck_bunny_240p_5mb.3gp");
                videoView.start();
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
        return position % 10 == 0 ? VIEW_TYPE_VIDEO : VIEW_TYPE_ARTICLE;
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
