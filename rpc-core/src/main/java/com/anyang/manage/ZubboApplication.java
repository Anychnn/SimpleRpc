package com.anyang.manage;

import com.anyang.*;
import com.anyang.invoke.RpcFuture;
import com.anyang.protocal.RpcRequest;
import com.anyang.protocal.RpcResponse;
import com.anyang.registry.ConnectionManager;
import com.anyang.util.SerializationUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.zookeeper.CreateMode;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class ZubboApplication {

    private String zookeeperAddress;
    private String serverAddress;

    public ZookeeperManager zookeeperManager;

    public ZubboApplication(String zookeeperAddress, String serverAddress) throws Exception {
        ZubboConfig.serverAddress = serverAddress;
        ZubboConfig.zookeeperAddress = zookeeperAddress;

        this.zookeeperManager = new ZookeeperManager(zookeeperAddress);
        this.zookeeperAddress = ZubboConfig.zookeeperAddress;
        this.serverAddress = ZubboConfig.serverAddress;
    }


    /**
     * @param clazz the type of service
     * @return
     */
    public <T> T subscribe(Class<T> clazz) throws Exception {
        String serviceName = clazz.getName();
        List<String> providers = zookeeperManager.getChildren("/" + serviceName + "/" + "providers");
        log.info(providers.toString());
        if (CollectionUtils.isEmpty(providers)) {
            throw new RuntimeException("no providers found");
        }

        if (CollectionUtils.isEmpty(ZubboContext.getInstance().handlerMap.get(providers.get(0)))) {

            log.info("从订阅服务 zookeeper: {}", serviceName);

            //创建服务节点 /zubbo/#service  /zubbo/#service/providers  /zubbo/#service/consumers
            zookeeperManager.createServiceNode(serviceName);
            zookeeperManager.createNodeIfNotExist("/" + serviceName + "/" + "consumers" + "/" + serverAddress, CreateMode.EPHEMERAL);

            String[] inets = providers.get(0).split(":");
            InetSocketAddress socketAddress = new InetSocketAddress(inets[0], Integer.valueOf(inets[1]));
            connectToServerNode(socketAddress);
        }

        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new RpcProxy(this, providers.get(0)));
    }


    //netty连接远程服务
    //serverNodeAddress example: localhost:3000
    private void connectToServerNode(InetSocketAddress socketAddress) {
        String serverNodeAddress = socketAddress.getHostString() + ":" + socketAddress.getPort();
        EventLoopGroup loopGroup = new NioEventLoopGroup(4);
        Bootstrap client = new Bootstrap();
        client.group(loopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)      //listening状态 的个数
                .option(ChannelOption.SO_KEEPALIVE, true)   //保持连接
                .option(ChannelOption.TCP_NODELAY, true)    //禁用nagel算法
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 0))
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
                                        out.writeInt(data.length);
                                        out.writeBytes(data);
                                        System.out.println("client encode:" + msg);
                                    }
                                });
                    }
                });

        client.connect(socketAddress)
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        log.info("success connect to remote server: {}", serverNodeAddress);
                    } else {
                        log.error("connect failed");
                    }
                });
    }

    public String getZookeeperAddress() {
        return zookeeperAddress;
    }

    public void setZookeeperAddress(String zookeeperAddress) {
        this.zookeeperAddress = zookeeperAddress;
    }
}
