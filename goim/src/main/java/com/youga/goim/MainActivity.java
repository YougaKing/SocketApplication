package com.youga.goim;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import butterknife.BindView;
import butterknife.ButterKnife;


public class MainActivity extends AppCompatActivity {

    String host = "121.42.13.161";
    int port = 8080;
    @BindView(R.id.recyclerView)
    RecyclerView mRecyclerView;
    @BindView(R.id.et_msg)
    EditText mEtMsg;
    @BindView(R.id.btn_send)
    Button mBtnSend;
    @BindView(R.id.btn_pic)
    Button mBtnPic;
    PushClient mPushClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        try {
            mPushClient = new PushClient(InetAddress.getByName(host), port, 1, "game");
            PushClient.Listener listen = mPushClient.new Listener();
            mPushClient.addObserver(listen);
            Thread t = new Thread(mPushClient);
            t.start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mEtMsg.getText().toString().trim().isEmpty()) {
                    if (mPushClient != null) {
                        mPushClient.sendMessage(mEtMsg.getText().toString());
                    }
                    mEtMsg.setText("");
                    hideKeyboard(mBtnSend);
                }
            }
        });
    }

    protected void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive()) {
            imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
        }
    }
}
