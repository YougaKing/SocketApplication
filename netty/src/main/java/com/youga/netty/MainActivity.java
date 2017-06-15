package com.youga.netty;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.youga.netty.client.Client;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import netty.echo.EchoMessage;


public class MainActivity extends Activity implements Client.ClientCallback {

    private static final String TAG = MainActivity.class.getSimpleName();

    Client mClient;
    @BindView(R.id.recyclerView)
    RecyclerView mRecyclerView;
    @BindView(R.id.et_msg)
    EditText mEtMsg;
    @BindView(R.id.btn_send)
    Button mBtnSend;
    @BindView(R.id.btn_pic)
    Button mBtnPic;
    InnerAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);


        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mEtMsg.getText().toString().trim().isEmpty()) {
                    mClient.sendMessage(mEtMsg.getText().toString());
                    mEtMsg.setText("");
                }
            }
        });

        mBtnPic.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, 1);
            }
        });

        mAdapter = new InnerAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mClient = new Client(this);
        new Thread() {
            @Override
            public void run() {
                mClient.start();
            }
        }.start();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Uri uri = data.getData();
            Log.e("uri", uri.toString());

            ContentResolver resolver = getContentResolver();
            try {
                InputStream reader = resolver.openInputStream(uri);
                mClient.sendPic(reader);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void message(final EchoMessage message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.addMessage(message);
            }
        });
    }

    class InnerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        List<EchoMessage> mMessageList = new ArrayList<>();

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = View.inflate(parent.getContext(), R.layout.item_main, null);
            return new ViewHolder(view);
        }

        public void addMessage(EchoMessage message) {
            mMessageList.add(message);
            notifyItemInserted(mMessageList.indexOf(message));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ViewHolder viewHolder = (ViewHolder) holder;
            viewHolder.bindPosition(position);
        }

        @Override
        public int getItemCount() {
            return mMessageList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            @BindView(R.id.textView)
            TextView mTextView;

            ViewHolder(View view) {
                super(view);
                ButterKnife.bind(this, view);
            }

            public void bindPosition(int position) {
                ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
                if (layoutParams == null) {
                    layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                } else {
                    layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                }
                itemView.setLayoutParams(layoutParams);

                EchoMessage message = mMessageList.get(position);
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mTextView.getLayoutParams();
                switch (message.target) {
                    case SYSTEM:
                        params.gravity = Gravity.CENTER;
                        mTextView.setTextColor(Color.RED);
                        mTextView.requestLayout();
                        break;
                    case CLIENT:
                        params.gravity = Gravity.RIGHT;
                        mTextView.setTextColor(Color.LTGRAY);
                        mTextView.requestLayout();
                        break;
                    case SERVER:
                        params.gravity = Gravity.LEFT;
                        mTextView.setTextColor(Color.DKGRAY);
                        mTextView.requestLayout();
                        break;
                }
                mTextView.setText(message.getMessage());
            }
        }
    }
}