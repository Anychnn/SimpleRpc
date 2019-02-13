package com.anyang.registry;

import com.anyang.ZubboConfig;
import com.anyang.protocal.RpcRequest;
import com.anyang.protocal.SyncRpcResponse;
import com.anyang.util.SerializationUtil;
import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
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

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ConnectionManager {

    private static ConnectionManager instance = null;

    //跟zookeeper保持一致
    public Map<String, List<String>> serviceMap = new ConcurrentHashMap<>();

    public Set<String> listeningServices = new HashSet<>();

    private String serverAddress;

    private boolean nettyInit = false;


    //server
    public Map<String, Object> serviceBeanMap = new HashMap<>();

    public static ConnectionManager getInstance() throws InterruptedException {
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

    public void updateZookeeper(String serviceName, List<String> remoteAddresses) {
        serviceMap.put(serviceName, remoteAddresses);
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
                                    .addLast(new MessageToByteEncoder<SyncRpcResponse>() {
                                        @Override
                                        protected void encode(ChannelHandlerContext ctx, SyncRpcResponse msg, ByteBuf out) throws Exception {
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
                                            out.add(rpcRequest);
                                        }
                                    })
                                    .addLast(new SimpleChannelInboundHandler<RpcRequest>() {
                                        @Override
                                        protected void messageReceived(ChannelHandlerContext ctx, RpcRequest msg) throws Exception {
                                            log.info("server received msg:" + msg);
                                            String className = msg.getClassName();
                                            Object serviceBean = ConnectionManager.getInstance().serviceBeanMap.get(className);

                                            String methodName = msg.getMethodName();
                                            Object[] params = msg.getParameters();
                                            //reflect
                                            Method method = serviceBean.getClass().getMethod(methodName, msg.getParameterTypes());
                                            method.setAccessible(true);
                                            Object result = method.invoke(serviceBean, params);
                                            log.info("result:" + result);

                                            SyncRpcResponse response = new SyncRpcResponse();
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


    public SyncRpcResponse invokeMethod(Class service, Method method, Object[] args) {
        return new SyncRpcResponse();
    }


}
