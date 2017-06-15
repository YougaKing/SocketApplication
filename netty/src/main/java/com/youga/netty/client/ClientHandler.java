package com.youga.netty.client;


import android.util.Log;

import java.util.Arrays;

import io.netty.buffer.ByteBuf;
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
    private static final byte PING_MSG = 1;
    private static final byte PONG_MSG = 2;
    Client mClient;
    private int heartbeatCount;

    public ClientHandler(Client client) {
        mClient = client;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof EchoMessage) {
            EchoMessage message = (EchoMessage) msg;
            mClient.mCallback.message(message);
        } else if (msg instanceof ByteBuf) {
            ByteBuf byteBuf = (ByteBuf) msg;
            Log.e(TAG, Arrays.toString(byteBuf.array()));
            if (byteBuf.getByte(4) == PING_MSG) {
                sendPongMsg(ctx);
            } else if (byteBuf.getByte(4) == PONG_MSG) {
                Log.d(TAG, " get pong msg from " + ctx.channel().remoteAddress());
            } else {
                handleData(byteBuf);
            }
        }
    }

    private void sendPongMsg(ChannelHandlerContext context) {
        ByteBuf buf = context.alloc().buffer(5);
        buf.writeInt(5);
        buf.writeByte(PONG_MSG);
        context.channel().writeAndFlush(buf);
        heartbeatCount++;
        Log.d(TAG, " sent pong msg to " + context.channel().remoteAddress() + ", count: " + heartbeatCount);
    }

    protected void handleData(ByteBuf byteBuf) {
        byte[] data = new byte[byteBuf.readableBytes() - 5];
        byteBuf.skipBytes(5);
        byteBuf.readBytes(data);

        EchoMessage message = EchoMessage.buildMessage(data, Target.SERVER);
        mClient.mCallback.message(message);
        Log.d(TAG, message.getMessage());
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
                    sendPingMsg(ctx);
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
        Log.e(TAG, "---WRITER_IDLE---");
    }

    private void sendPingMsg(ChannelHandlerContext ctx) {
        ByteBuf buf = ctx.alloc().buffer(5);
        buf.writeInt(5);
        buf.writeByte(PING_MSG);
        buf.retain();
        ctx.writeAndFlush(buf);
        heartbeatCount++;
        Log.w(TAG, " sent ping msg to " + ctx.channel().remoteAddress() + ", count: " + heartbeatCount);
    }
}
