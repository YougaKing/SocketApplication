package com.youga.netty.client;

import android.util.Log;

import com.youga.netty.Message;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by Youga on 2017/6/15.
 */

public class ClientHandler extends ByteBufHandler {

    Client mClient;

    public ClientHandler(Client client) {
        super(client.getClass().getSimpleName());
        mClient = client;
    }

    @Override
    protected void handleData(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) {
        byte[] data = new byte[byteBuf.readableBytes() - 5];
        byteBuf.skipBytes(5);
        byteBuf.readBytes(data);

        Message message = new Message(Message.REMOTE, new String(data));
        mClient.mCallback.message(message);
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Message message = new Message(Message.SYSTEM, "通道 活动");
        mClient.mCallback.message(message);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Message message = new Message(Message.SYSTEM, "通道 迟顿");
        mClient.mCallback.message(message);
        mClient.doConnect();
        super.channelInactive(ctx);
    }


    @Override
    protected void handleAllIdle(ChannelHandlerContext ctx) {
        super.handleAllIdle(ctx);
        sendPingMsg(ctx);
    }
}
