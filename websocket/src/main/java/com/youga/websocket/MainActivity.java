package com.youga.websocket;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;
import com.youga.websocket.core.WebSocketClient;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Logger.addLogAdapter(new AndroidLogAdapter() {
            @Override
            public boolean isLoggable(int priority, String tag) {
                return BuildConfig.DEBUG;
            }
        });

        String wsUrl = "ws://www.huoqiushu.com:2346/";
        WebSocketClient socketClient = WebSocketClient.getInstance(wsUrl);

    }
}
