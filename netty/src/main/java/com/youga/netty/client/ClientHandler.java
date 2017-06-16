package com.youga.netty.client;


import android.util.Log;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import netty.echo.EchoCommon.Target;
import netty.echo.EchoMessage;

/**
 * Created by Youga on 2017/6/15.
 */

public class ClientHandler extends SimpleChannelInboundHandler<Object> {

    protected final String TAG = getClass().getSimpleName();
    // 定义客户端没有收到服务端的pong消息的最大次数
    private static final int MAX_UN_REC_PONG_TIMES = 3;
    // 多长时间未请求后，发送心跳
    static final int WRITE_WAIT_SECONDS = 5;
    // 客户端连续N次没有收到服务端的pong消息  计数器
    private int mUnRecPongTimes = 0;
    Client mClient;

    public ClientHandler(Client client) {
        mClient = client;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof EchoMessage) {
            EchoMessage message = (EchoMessage) msg;
            if (message.target == Target.HEART_BEAT) {
                // 计数器清零
                mUnRecPongTimes = 0;
            } else {
                mClient.mCallback.message(message);
            }
            Log.d(TAG, message.target.getDescribe() + ":" + message.getMessage());
        } else {
            Log.i(TAG, msg.toString());
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        EchoMessage message = EchoMessage.buildMessage("通道 活动", Target.SYSTEM);
        mClient.mCallback.message(message);
        Log.d(TAG, message.getMessage());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        EchoMessage message = EchoMessage.buildMessage("通道 迟顿", Target.SYSTEM);
        mClient.mCallback.message(message);
        Log.d(TAG, message.getMessage());
        mClient.doConnect();
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            switch (e.state()) {
                case READER_IDLE:
                    handleReaderIdle(ctx);
                    break;
                case WRITER_IDLE:
                    handleWriterIdle(ctx);
                    break;
                case ALL_IDLE:
                    handleAllIdle(ctx);
                    break;
                default:
                    break;
            }
        }
    }

    private void handleReaderIdle(ChannelHandlerContext ctx) {
        Log.e(TAG, "---READER_IDLE---");
    }

    private void handleWriterIdle(ChannelHandlerContext ctx) {
        if (mUnRecPongTimes < MAX_UN_REC_PONG_TIMES) {
            EchoMessage message = EchoMessage.buildMessage("HEART_BEAT", Target.HEART_BEAT);
            ctx.writeAndFlush(message);
            mUnRecPongTimes++;
        } else {
            ctx.channel().close();
        }
        Log.e(TAG, "---WRITER_IDLE---mUnRecPongTimes:" + mUnRecPongTimes);
    }

    private void handleAllIdle(ChannelHandlerContext ctx) {
        Log.e(TAG, "---ALL_IDLE---");
    }
}
