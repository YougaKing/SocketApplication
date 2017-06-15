package com.youga.netty.client;

import android.content.ContentResolver;
import android.os.Build;
import android.util.Log;

import com.youga.netty.Message;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import cc.lison.pojo.EchoFile;
import cc.lison.pojo.EchoMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.timeout.IdleStateHandler;

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
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline p = socketChannel.pipeline();
                            p.addLast(new IdleStateHandler(0, 0, 5));
                            p.addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4, -4, 0));
                            p.addLast(new ClientHandler(Client.this));
                            p.addLast(new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.weakCachingConcurrentResolver(null)));
                            p.addLast(new ObjectEncoder());
                        }
                    });
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

                    Message message = new Message(Message.SYSTEM, "连接服务器成功");
                    mCallback.message(message);
                    Log.d(TAG, message.message);

                    sendMessage("Hello,I am Nexus5x");
                } else {
                    Message message = new Message(Message.SYSTEM, "连接服务器失败,5秒后重试");
                    mCallback.message(message);
                    Log.d(TAG, message.message);
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
        Message message = new Message(Message.LOCAL, string);
        mCallback.message(message);
        Log.d(TAG, message.message);

        EchoMessage em = new EchoMessage();
        byte[] b = message.message.getBytes();
        em.setBytes(b);
        em.setSumCountPackage(b.length);
        em.setCountPackage(1);
        em.setSend_time(System.currentTimeMillis());
        mChannel.writeAndFlush(em);
    }


    public void sendPic(InputStream reader) {
        if (mChannel == null) return;

        int dataLength = 1024;
        int sumCountPackage;

        try {
            byte[] bytes = new byte[reader.available()];
            reader.read(bytes);
            reader.close();

            if ((bytes.length % dataLength == 0))
                sumCountPackage = bytes.length / dataLength;
            else
                sumCountPackage = (bytes.length / dataLength) + 1;

            Log.i("TAG", "文件总长度:" + bytes.length);
            EchoFile msgFile = new EchoFile();
            msgFile.setSumCountPackage(sumCountPackage);
            msgFile.setCountPackage(1);

            msgFile.setBytes(bytes);
            msgFile.setFile_name(Build.MANUFACTURER + "-" + UUID.randomUUID() + ".jpg");
            mChannel.writeAndFlush(msgFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {

            byte[] bytes = new byte[reader.available()];
            reader.read(bytes);
            reader.close();

            mChannel.writeAndFlush("fileLength:" + bytes.length + "\r\n");
            mChannel.flush();
            mChannel.read();

            mChannel.writeAndFlush(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mChannel.flush();
            mChannel.read();
        }
    }

    public interface ClientCallback {
        void message(Message message);
    }
}
