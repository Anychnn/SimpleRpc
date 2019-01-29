package com.anyang;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.*;
import jdk.nashorn.internal.runtime.linker.Bootstrap;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.List;

@Slf4j
public class Server {
    public static void main(String[] args) throws InterruptedException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        EventLoopGroup boss = new NioEventLoopGroup();
        EventLoopGroup worker = new NioEventLoopGroup();
        bootstrap.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline
//                                .addLast(new LengthFieldPrepender(4))
                                .addLast(new ByteToMessageDecoder() {
                                    @Override
                                    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
                                        int length = in.readInt();
                                        byte[] bytes = new byte[length];

                                        in.readBytes()
                                        in.readBytes(bytes);
                                        System.out.println("server received: " + new String(bytes));
                                    }
                                })
                                .addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 0));

                    }
                });

        ChannelFuture future = bootstrap.bind(5050).sync();
        future.channel().closeFuture().sync();
    }
}
