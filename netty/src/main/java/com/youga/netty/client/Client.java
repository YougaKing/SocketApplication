package com.youga.netty.client;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import netty.echo.EchoCommon;
import netty.echo.EchoCommon.Target;
import netty.echo.EchoFile;
import netty.echo.EchoMessage;

/**
 * Created by Youga on 2017/6/15.
 */

public class Client {
    static final String TAG = Client.class.getSimpleName();
    static final String HOST = "121.42.13.161";
    static final int PORT = 8080;
    // 隔N秒后重连
    private static final int RE_CONN_WAIT_SECONDS = 5;
    private NioEventLoopGroup mWorkGroup = new NioEventLoopGroup(4);
    Bootstrap mBootstrap;
    Channel mChannel;

    ClientCallback mCallback;

    public Client(ClientCallback callback) {
        mCallback = callback;
    }

    public void start() {
        try {
            mBootstrap = new Bootstrap();
            mBootstrap.group(mWorkGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new InitializerPipeline(Client.this));
            doConnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void doConnect() {
        if (mChannel != null && mChannel.isActive()) {
            return;
        }

        ChannelFuture future = mBootstrap.connect(HOST, PORT);

        future.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture futureListener) throws Exception {
                if (futureListener.isSuccess()) {
                    mChannel = futureListener.channel();


                    EchoMessage message = EchoMessage.buildMessage("连接服务器成功", Target.SYSTEM);
                    mCallback.message(message);
                    Log.d(TAG, message.getMessage());

                    authWrite();
//                    sendMessage("Hello,I am Nexus5x");
                } else {

                    EchoMessage message = EchoMessage.buildMessage("连接服务器失败,5秒后重试", Target.SYSTEM);
                    mCallback.message(message);
                    Log.d(TAG, message.getMessage());

                    futureListener.channel().eventLoop().schedule(new Runnable() {
                        @Override
                        public void run() {
                            doConnect();
                        }
                    }, RE_CONN_WAIT_SECONDS, TimeUnit.SECONDS);
                }
            }
        });
    }


    public synchronized Boolean authWrite() throws IOException {
        String msg = "12998" + "," + "game";

        int packLength = msg.length() + 16;
        byte[] message = new byte[4 + 2 + 2 + 4 + 4];

        // package length
        int offset = BruteForceCoding.encodeIntBigEndian(message, packLength, 0, 4 * BruteForceCoding.BSIZE);
        // header lenght
        offset = BruteForceCoding.encodeIntBigEndian(message, 16, offset, 2 * BruteForceCoding.BSIZE);
        // ver
        offset = BruteForceCoding.encodeIntBigEndian(message, 1, offset, 2 * BruteForceCoding.BSIZE);
        // operation
        offset = BruteForceCoding.encodeIntBigEndian(message, 7, offset, 4 * BruteForceCoding.BSIZE);
        // jsonp callback
        offset = BruteForceCoding.encodeIntBigEndian(message, 1, offset, 4 * BruteForceCoding.BSIZE);

        byte[] bytes = BruteForceCoding.add(message, msg.getBytes());

        Log.d(TAG, new String(bytes, Charset.defaultCharset()) + "-->" + Arrays.toString(bytes));

        mChannel.writeAndFlush(bytes);
        return true;
    }

    public void sendMessage(String string) {
        if (mChannel == null) return;

//        EchoMessage message = EchoMessage.buildMessage(string, Target.CLIENT);
//        mCallback.message(message);
//        mChannel.writeAndFlush(message);
//
//        Log.d(TAG, message.getMessage());
    }


    public void sendPic(InputStream reader, String fileName, String filePath) {
        if (mChannel == null) return;
        try {
            byte[] bytes = new byte[reader.available()];
            reader.read(bytes);
            reader.close();
            EchoFile msgFile = EchoFile.buildFile(bytes, fileName, filePath, Target.CLIENT);
            mCallback.message(msgFile);
            mChannel.writeAndFlush(msgFile);

            Log.i(TAG, fileName + "-->文件总长度:" + bytes.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public interface ClientCallback {
        void message(EchoCommon message);
    }
}
