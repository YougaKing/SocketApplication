package com.youga.netty.client;

import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import netty.echo.EchoCommon;
import netty.echo.EchoFile;
import netty.echo.EchoMessage;

/**
 * Created by Youga on 2017/6/15.
 */

public class Client {
    public static final String TAG = Client.class.getSimpleName();
    String host = "192.168.0.112";
    int port = 8080;

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

        ChannelFuture future = mBootstrap.connect(host, port);

        future.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture futureListener) throws Exception {
                if (futureListener.isSuccess()) {
                    mChannel = futureListener.channel();


                    EchoMessage message = EchoMessage.buildMessage("连接服务器成功", EchoCommon.SYSTEM);
                    mCallback.message(message);
                    Log.d(TAG, message.getMessage());

                    sendMessage("Hello,I am Nexus5x");
                } else {

                    EchoMessage message = EchoMessage.buildMessage("连接服务器失败,5秒后重试", EchoCommon.SYSTEM);
                    mCallback.message(message);
                    Log.d(TAG, message.getMessage());

                    futureListener.channel().eventLoop().schedule(new Runnable() {
                        @Override
                        public void run() {
                            doConnect();
                        }
                    }, 5, TimeUnit.SECONDS);
                }
            }
        });
    }

    public void sendMessage(String string) {
        if (mChannel == null) return;

        EchoMessage message = EchoMessage.buildMessage(string, EchoCommon.CLIENT);
        mCallback.message(message);
        mChannel.writeAndFlush(message);

        Log.d(TAG, message.getMessage());
    }


    public void sendPic(InputStream reader) {
        if (mChannel == null) return;

//        int dataLength = 1024;
//        int sumCountPackage;
//
//        try {
//            byte[] bytes = new byte[reader.available()];
//            reader.read(bytes);
//            reader.close();
//
//            if ((bytes.length % dataLength == 0))
//                sumCountPackage = bytes.length / dataLength;
//            else
//                sumCountPackage = (bytes.length / dataLength) + 1;
//
//            Log.i("TAG", "文件总长度:" + bytes.length);
//            EchoFile msgFile = new EchoFile();
//            msgFile.setSumCountPackage(sumCountPackage);
//            msgFile.setCountPackage(1);
//
//            msgFile.setBytes(bytes);
//            msgFile.setFile_name(Build.MANUFACTURER + "-" + UUID.randomUUID() + ".jpg");
//            mChannel.writeAndFlush(msgFile);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        try {
//
//            byte[] bytes = new byte[reader.available()];
//            reader.read(bytes);
//            reader.close();
//
//            mChannel.writeAndFlush("fileLength:" + bytes.length + "\r\n");
//            mChannel.flush();
//            mChannel.read();
//
//            mChannel.writeAndFlush(bytes);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            mChannel.flush();
//            mChannel.read();
//        }
    }

    public interface ClientCallback {
        void message(EchoMessage message);
    }
}
