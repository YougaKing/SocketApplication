package com.youga.netty.client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.timeout.IdleStateHandler;


/**
 * Created by Lison on 5/6/2016.
 */
public class InitializerPipeline extends ChannelInitializer<SocketChannel> {

    Client mClient;

    public InitializerPipeline(Client client) {
        mClient = client;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new IdleStateHandler(0, ClientHandler.WRITE_WAIT_SECONDS, 0));
//        p.addLast(new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.weakCachingConcurrentResolver(null)));
//        p.addLast(new ObjectEncoder());
        p.addLast(new ClientHandler(mClient));
    }
}
