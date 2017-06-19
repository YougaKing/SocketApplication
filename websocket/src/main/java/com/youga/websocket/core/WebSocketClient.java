package com.youga.websocket.core;

import android.util.Log;

import com.orhanobut.logger.Logger;

import java.util.Observable;
import java.util.Observer;

import okio.ByteString;

/**
 * Created by Youga on 2017/6/19.
 */

public class WebSocketClient extends AbstractBlockingClient {

    private static WebSocketClient mInstance;

    private WebSocketClient(String wsUrl) {
        super(wsUrl);
    }

    public static WebSocketClient getInstance(String wsUrl) {
        if (mInstance == null) {
            synchronized (WebSocketClient.class) {
                if (mInstance == null) {
                    mInstance = new WebSocketClient(wsUrl);
                    mInstance.addObserver(new WebSocketClient.Listener());
                }
            }
        }
        return mInstance;
    }

    @Override
    public void messageReceived(ByteString bytes) {
        Logger.d(bytes);
    }

    @Override
    public void messageReceived(String text) {
        Logger.d(text);
    }

    @Override
    public void disconnected() {

    }

    private static class Listener implements Observer {

        private final String TAG = Listener.class.getSimpleName();

        @Override
        public void update(Observable o, Object arg) {
            Log.d(TAG, "WebSocketClient 断开");
            WebSocketClient webSocketClient = (WebSocketClient) o;
            webSocketClient.newWebSocket();
            Log.d(TAG, "WebSocketClient 重连");
        }
    }
}
