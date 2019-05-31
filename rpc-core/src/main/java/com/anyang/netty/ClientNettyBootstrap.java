package com.anyang.netty;

import com.anyang.config.OutTypeEnum;
import com.anyang.manage.RpcHandler;
import com.anyang.manage.ZubboContext;
import com.anyang.protocal.RpcRequest;
import com.anyang.util.SerializationUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ClientNettyBootstrap {

    private ChannelFuture channelFuture;

    //netty客户端启动
    public void connect(InetSocketAddress socketAddress) {
        String serverNodeAddress = socketAddress.getHostString() + ":" + socketAddress.getPort();
        EventLoopGroup loopGroup = ChannelSelectorUtil.selectEventLoopGroupByOS(4);
        Bootstrap client = new Bootstrap();

        client.group(loopGroup)
                .channel(ChannelSelectorUtil.selectSocketChannelClassByOS())
                .option(ChannelOption.SO_BACKLOG, 128)      //listening状态 的个数
                .option(ChannelOption.SO_KEEPALIVE, true)   //保持连接
                .option(ChannelOption.TCP_NODELAY, true)    //禁用nagel算法
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 0))
                                .addLast(new IdleStateHandler(20, 0, 0, TimeUnit.SECONDS))
                                .addLast(new MessageToByteEncoder<HeartBeat>() {
                                    @Override
                                    protected void encode(ChannelHandlerContext ctx, HeartBeat msg, ByteBuf out) throws Exception {
                                        log.info("encode heart beat");
                                        out.writeInt(OutTypeEnum.HEART_BEAT.getType());
                                        byte[] bytes = SerializationUtil.serialize(msg);
                                        out.writeInt(bytes.length);
                                        out.writeBytes(bytes);
                                        ctx.flush();
                                    }
                                })
                                .addLast(new ChannelHandlerAdapter() {
                                    @Override
                                    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                                        log.info("send heart beat");
                                        HeartBeat heartBeat = new HeartBeat();
                                        ctx.writeAndFlush(heartBeat);
                                        ctx.fireUserEventTriggered(evt);
                                    }
                                })
                                .addLast(new RpcResponseDecoder())
                                .addLast(new RpcHandler() {
                                    @Override
                                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                        super.channelActive(ctx);
                                        this.channel = ctx.channel();
                                        List<RpcHandler> handlers = ZubboContext.getInstance().handlerMap.get(serverNodeAddress);
                                        if (handlers == null) {
                                            handlers = new ArrayList<>();
                                        }
                                        handlers.add(this);
                                        ZubboContext.getInstance().handlerMap.put(serverNodeAddress, handlers);
                                        this.initLatch.countDown();
                                    }

                                    @Override
                                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                        super.channelInactive(ctx);
                                        ZubboContext.getInstance().handlerMap.remove(serverNodeAddress);
                                    }
                                })
                                .addLast(new MessageToByteEncoder<RpcRequest>() {
                                    @Override
                                    protected void encode(ChannelHandlerContext ctx, RpcRequest msg, ByteBuf out) throws Exception {
                                        byte[] data = SerializationUtil.serialize(msg);
                                        out.writeInt(OutTypeEnum.RRC_RESPONSE.getType());
                                        out.writeInt(data.length);
                                        out.writeBytes(data);
                                        System.out.println("client encode:" + msg);
                                    }

                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//                                        super.exceptionCaught(ctx, cause);
                                        log.info("client exception caught");
                                        cause.printStackTrace();
                                    }
                                });
                    }
                });


        channelFuture = client.connect(socketAddress)
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        log.info("success connect to remote server: {}", serverNodeAddress);
                    } else {
                        log.error("connect failed");
                    }
                });
    }

    public void close() {
        channelFuture.channel().close();
    }
}
