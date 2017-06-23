package com.agentwaj.autoplay;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import com.danikula.videocache.HttpProxyCacheServer;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView list = (ListView) findViewById(R.id.list);
        HttpProxyCacheServer proxy = new HttpProxyCacheServer(getApplicationContext());
        List<String> items = getItems();

        list.setAdapter(new MyAdapter(list, proxy, items));
    }

    private List<String> getItems() {
        List<String> items = new ArrayList<>();

        for (int i = 0; i < 46; i++) {
            items.add("Item " + i);
        }

        return items;
    }
}
