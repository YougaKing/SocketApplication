package com.youga.websocket.core;

import android.util.Log;

import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.util.Observable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * Created by Youga on 2017/6/19.
 */

public abstract class AbstractBlockingClient extends Observable {

    private static final String TAG = AbstractBlockingClient.class.getSimpleName();
    private ScheduledExecutorService mExecutorService = Executors.newScheduledThreadPool(2);
    private ExecutorService mMessageExecutor = Executors.newCachedThreadPool();
    private OkHttpClient mOkHttpClient;
    private Request mRequest;
    private final AtomicReference<State> mState = new AtomicReference<>(State.STOPPED);

    private enum State {
        STOPPED, STOPPING, RUNNING
    }

    public AbstractBlockingClient(String wsUrl) {
        mOkHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .pingInterval(1, TimeUnit.SECONDS)// websocket 轮训间隔
                .build();

        mRequest = new Request.Builder()
                .url(wsUrl)
                .build();

        newWebSocket();
    }


    void newWebSocket() {
        mOkHttpClient.newWebSocket(mRequest, mWebSocketListener);
    }

    private WebSocket mWebSocket;
    private WebSocketListener mWebSocketListener = new WebSocketListener() {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            super.onOpen(webSocket, response);
            mWebSocket = webSocket;
            if (!mState.compareAndSet(State.STOPPED, State.RUNNING)) return;
            authWrite();
            Logger.d(response.request().headers() + "\n" + response.headers() + "\n" + response);
            //开启消息定时发送
            pingRunnable();
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            super.onMessage(webSocket, text);
            messageReceived(text);
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            super.onMessage(webSocket, bytes);
            messageReceived(bytes);
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            super.onClosing(webSocket, code, reason);
            mState.set(State.STOPPING);
            Log.d(TAG, "onClosing()-->code:" + code + "-->reason:" + reason);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            super.onClosed(webSocket, code, reason);
            mState.set(State.STOPPED);
            Log.d(TAG, "onClosed()-->code:" + code + "-->reason:" + reason);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            super.onFailure(webSocket, t, response);
            Logger.e(t, "onFailure", response);
            mWebSocket.cancel();
            mState.set(State.STOPPED);
            disconnected();
            restart();
        }
    };

    private synchronized Boolean authWrite() {
        // TODO: 2017/6/19

        return true;
    }

    private void restart() {
        setChanged();
        notifyObservers();
    }

    public void sendMessage(final String text) {
        mMessageExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (mState.get() != State.RUNNING) return;
                mWebSocket.send(text);
            }
        });
    }

    private void pingRunnable() {
        mExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                if (mWebSocket == null) return;
//                msgCount++;
//                boolean isSuccessed = mWebSocket.send("msg" + msgCount + "-" + System.currentTimeMillis());
//                mExecutorService.schedule(this, 1, TimeUnit.SECONDS);
            }
        }, 1, TimeUnit.SECONDS);
    }

    public boolean stop() {
        if (mState.compareAndSet(State.RUNNING, State.STOPPING)) {
            mWebSocket.cancel();
            mState.set(State.STOPPED);
            // TODO: 2017/6/19

            return true;
        }
        return false;
    }

    public abstract void messageReceived(ByteString bytes);

    public abstract void messageReceived(String text);

    public abstract void disconnected();
}
