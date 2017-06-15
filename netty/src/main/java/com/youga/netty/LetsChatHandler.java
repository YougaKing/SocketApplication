package com.youga.netty;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Created by Lison on 5/6/2016.
 */
public class LetsChatHandler extends SimpleChannelInboundHandler<String> {

    private static final String TAG = LetsChatHandler.class.getSimpleName();
    final Handler handler;

    public LetsChatHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) throws Exception {
        Log.d(TAG, "channelRead0():" + s);
        Message message = handler.obtainMessage(0x01);
        message.obj = "[SYSTEM] - " + s;
        message.sendToTarget();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Log.d(TAG, "channelActive()");
        Message message = handler.obtainMessage(0x01);
        message.obj = "[SYSTEM] - CLIENT ACTIVE";
        message.sendToTarget();
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Log.d(TAG, "channelInactive()");
        Message message = handler.obtainMessage(0x01);
        message.obj = "[SYSTEM] - CLIENT INACTIVE";
        message.sendToTarget();
        super.channelInactive(ctx);
    }
}
