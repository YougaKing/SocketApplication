package com.youga.netty.client;

import android.os.Message;
import android.util.Log;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * Created by Youga on 2017/6/15.
 */

public abstract class ByteBufHandler extends SimpleChannelInboundHandler<ByteBuf> {

    protected final String TAG = getClass().getSimpleName();
    private static final byte PING_MSG = 1;
    private static final byte PONG_MSG = 2;
    protected String mName;
    private int heartbeatCount;

    public ByteBufHandler(String name) {
        mName = name;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, ByteBuf byteBuf) throws Exception {
        if (byteBuf.getByte(4) == PING_MSG) {
            sendPongMsg(context);
        } else if (byteBuf.getByte(4) == PONG_MSG) {
            Log.d(TAG, mName + " get pong msg from " + context.channel().remoteAddress());
        } else {
            handleData(context, byteBuf);
        }
    }


    private void sendPongMsg(ChannelHandlerContext context) {
        ByteBuf buf = context.alloc().buffer(5);
        buf.writeInt(5);
        buf.writeByte(PONG_MSG);
        context.channel().writeAndFlush(buf);
        heartbeatCount++;
        Log.d(TAG, mName + " sent pong msg to " + context.channel().remoteAddress() + ", count: " + heartbeatCount);
    }


    protected void sendPingMsg(ChannelHandlerContext context) {
        ByteBuf buf = context.alloc().buffer(5);
        buf.writeInt(5);
        buf.writeByte(PING_MSG);
        buf.retain();
        context.writeAndFlush(buf);
        heartbeatCount++;
        Log.d(TAG, mName + " sent ping msg to " + context.channel().remoteAddress() + ", count: " + heartbeatCount);
    }

    protected abstract void handleData(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf);

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // IdleStateHandler 所产生的 IdleStateEvent 的处理逻辑.
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

    protected void handleReaderIdle(ChannelHandlerContext ctx) {
        Log.e(TAG, "---READER_IDLE---");
    }

    protected void handleWriterIdle(ChannelHandlerContext ctx) {
        Log.e(TAG, "---WRITER_IDLE---");
    }

    protected void handleAllIdle(ChannelHandlerContext ctx) {
        Log.e(TAG, "---ALL_IDLE---");
    }
}
