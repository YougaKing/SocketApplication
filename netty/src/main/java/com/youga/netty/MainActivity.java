package com.youga.netty;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.youga.netty.client.Client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import netty.echo.EchoCommon;
import netty.echo.EchoFile;
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
                    hideKeyboard(mBtnSend);
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

    protected void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive()) {
            imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Uri uri = data.getData();

            Log.d(TAG, getFilePath(uri) + "-->" + getFileName(uri));

            ContentResolver resolver = getContentResolver();
            try {
                InputStream reader = resolver.openInputStream(uri);
                mClient.sendPic(reader, getFileName(uri), getFilePath(uri));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void message(final EchoCommon message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.addMessage(message);
                mRecyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
            }
        });
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                assert cursor != null;
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public String getFilePath(Uri uri) {
        String result = null;
        if (DocumentsContract.isDocumentUri(this, uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(contentUri, selection, selectionArgs);
            }
        } else if (uri.getScheme().equals("content")) {
            String[] prof = {MediaStore.Audio.Media.DATA};
            Cursor cursor = getContentResolver().query(uri, prof, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                }
            } finally {
                assert cursor != null;
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
        }
        return result;
    }

    public String getDataColumn(Uri uri, String selection,
                                String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    class InnerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        List<EchoCommon> mMessageList = new ArrayList<>();

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = View.inflate(parent.getContext(), R.layout.item_main, null);
            return new ViewHolder(view);
        }

        public void addMessage(EchoCommon message) {
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
            @BindView(R.id.imageView)
            ImageView mImageView;

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

                EchoCommon message = mMessageList.get(position);
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mTextView.getLayoutParams();
                FrameLayout.LayoutParams ivParams = (FrameLayout.LayoutParams) mImageView.getLayoutParams();
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
                        ivParams.gravity = Gravity.RIGHT;
                        mImageView.requestLayout();
                        break;
                    case SERVER:
                        params.gravity = Gravity.LEFT;
                        mTextView.setTextColor(Color.DKGRAY);
                        mTextView.requestLayout();
                        ivParams.gravity = Gravity.LEFT;
                        mImageView.requestLayout();
                        break;
                }
                if (message instanceof EchoMessage) {
                    mTextView.setVisibility(View.VISIBLE);
                    mImageView.setVisibility(View.GONE);
                    mTextView.setText(((EchoMessage) message).getMessage());
                } else {
                    mTextView.setVisibility(View.GONE);
                    mImageView.setVisibility(View.VISIBLE);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(((EchoFile) message).filePath, options);
                    DisplayMetrics metrics = getResources().getDisplayMetrics();
                    if (options.outWidth > metrics.widthPixels / 2f) {
                        options.inDensity = (int) (options.outWidth / metrics.widthPixels / 2f);
                    }
                    options.inJustDecodeBounds = false;
                    Bitmap bitmap = BitmapFactory.decodeFile(((EchoFile) message).filePath, options);
                    mImageView.setImageBitmap(bitmap);
                }
            }
        }
    }
}