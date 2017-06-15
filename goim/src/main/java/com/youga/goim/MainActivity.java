package com.youga.goim;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.net.InetAddress;
import java.net.UnknownHostException;


public class MainActivity extends AppCompatActivity {

    String host = "192.168.0.112";
    int port = 8080;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            PushClient cb = new PushClient(InetAddress.getByName(host), port, 1, "game");
            PushClient.Listener listen = cb.new Listener();
            cb.addObserver(listen);
            Thread t = new Thread(cb);
            t.start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
