package com.anyang.registry;

import com.anyang.manage.ZubboContext;
import com.anyang.invoke.InvokerEnum;
import com.anyang.config.ZubboConfig;
import com.anyang.invoke.CglibInvoker;
import com.anyang.invoke.Invoker;
import com.anyang.invoke.JDKInvoker;
import com.anyang.protocal.RpcRequest;
import com.anyang.protocal.RpcResponse;
import com.anyang.util.SerializationUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class ConnectionManager {

    private volatile static ConnectionManager instance = null;

    private String serverAddress;

    public static ConnectionManager getInstance() {
        if (instance == null) {
            synchronized (ConnectionManager.class) {
                if (instance == null) {
                    instance = new ConnectionManager(ZubboConfig.serverAddress);
                }
            }
        }
        return instance;
    }


    private ConnectionManager(String serverAddress) {
        this.serverAddress = serverAddress;
    }


    public void initServer() throws InterruptedException {
        EventLoopGroup worker = new NioEventLoopGroup();
        EventLoopGroup boss = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 0))
                                    .addLast(new MessageToByteEncoder<RpcResponse>() {
                                        @Override
                                        protected void encode(ChannelHandlerContext ctx, RpcResponse msg, ByteBuf out) throws Exception {
                                            byte[] data = SerializationUtil.serialize(msg);
                                            out.writeInt(data.length);
                                            out.writeBytes(data);
                                        }
                                    })
                                    .addLast(new ByteToMessageDecoder() {
                                        @Override
                                        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
                                            int length = in.readInt();
                                            byte[] data = new byte[length];
                                            in.readBytes(data);
                                            RpcRequest rpcRequest = SerializationUtil.deSerialize(data, RpcRequest.class);
                                            if (rpcRequest != null) {
                                                out.add(rpcRequest);
                                            }
                                        }
                                    })
                                    .addLast(new SimpleChannelInboundHandler<RpcRequest>() {

                                        @Override
                                        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                                            super.channelReadComplete(ctx);
                                            //todo
                                        }

                                        @Override
                                        protected void messageReceived(ChannelHandlerContext ctx, RpcRequest msg) throws Exception {
                                            log.info("server received msg:" + msg);
                                            String className = msg.getClassName();
                                            Object serviceBean = ZubboContext.getInstance().serviceBeanMap.get(className);

                                            String methodName = msg.getMethodName();
                                            Object[] params = msg.getParameters();

                                            //执行方法
                                            Object result = selectInvoker().invoke(serviceBean, msg);

                                            RpcResponse response = new RpcResponse();
                                            response.setResult(result);
                                            response.setRequestId(msg.getRequestId());
                                            ctx.writeAndFlush(response)
                                                    .addListener(new ChannelFutureListener() {
                                                        @Override
                                                        public void operationComplete(ChannelFuture future) throws Exception {
                                                            log.info("send response for request: " + msg.getRequestId());
                                                        }
                                                    });

                                        }

                                        @Override
                                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                            cause.printStackTrace();
//                                            super.exceptionCaught(ctx, cause);
                                        }
                                    });
                        }
                    });
            String[] address = serverAddress.split(":");

            ChannelFuture future = bootstrap.bind(address[0], Integer.valueOf(address[1])).sync();
            future.channel().closeFuture().sync();
        } finally {
            worker.shutdownGracefully();
            boss.shutdownGracefully();
        }
    }


    //默认使用jdk
    private Invoker selectInvoker() {
        if (ZubboConfig.invokerEnum == InvokerEnum.JDKInvoker) {
            return new JDKInvoker();
        } else if (ZubboConfig.invokerEnum == InvokerEnum.CGlibInvoker) {
            return new CglibInvoker();
        }
        return new JDKInvoker();
    }
}
