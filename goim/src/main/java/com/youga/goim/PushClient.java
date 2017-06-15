package com.youga.goim;


import android.util.Log;

import java.net.InetAddress;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

public class PushClient extends AbstractBlockingClient {

    private static final String TAG = PushClient.class.getSimpleName();

    public PushClient(InetAddress server, int port, Integer uid, String game) {
        super(server, port, uid, game);
    }

    @Override
    protected void heartBeatReceived() {
        Log.d(TAG, "heartBeatReceived ...");
    }


    @Override
    protected void authSuccess() {
        Log.d(TAG, "authSuccess ...");
    }

    @Override
    protected void messageReceived(Long packageLength, Long headLength, Long version, Long operation, Long sequenceId, String message) {
        String sb = "-----------------------------" + new Date().getTime() + "\n" +
                "headLength:" + headLength + "\n" +
                "version:" + version + "\n" +
                "operation:" + operation + "\n" +
                "sequenceId:" + sequenceId + "\n" +
                "message:" + message + "\n" +
                "-----------------------------";
        Log.d(TAG, sb);

    }

    @Override
    protected void messageReceived(String message) {
        Log.d(TAG, (new Date().getTime() + "," + uid + ",message:" + message));

    }

    @Override
    protected void connected(boolean alreadyConnected) {
        Log.d(TAG, "alreadyConnected is " + alreadyConnected);
    }

    @Override
    protected void disconnected() {
        Log.d(TAG, "disconnected....... ");

    }

    class Listener implements Observer {

        private final String TAG = Listener.class.getSimpleName();

        @Override
        public void update(Observable o, Object arg) {
            Log.d(TAG, "PushClient 死机");
            PushClient pc = new PushClient(getServer(), getPort(), uid, game);
            pc.addObserver(this);
            new Thread(pc).start();
            Log.d(TAG, "PushClient 重启");
        }
    }
}