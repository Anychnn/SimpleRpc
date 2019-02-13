package com.anyang.manage;

import com.anyang.RpcHandler;
import com.anyang.ZubboConfig;
import com.anyang.invoke.RpcFuture;
import com.anyang.protocal.RpcRequest;
import com.anyang.protocal.SyncRpcResponse;
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
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CreateBuilder;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

import static org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type.NODE_ADDED;
import static org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type.NODE_REMOVED;
import static org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type.NODE_UPDATED;

@Slf4j
public class ZubboApplication {

    private String zookeeperAddress;
    private String serverAddress;
    private CuratorFramework client;
    private ConnectionManager connectionManager;
    public static final String root = "zubbo";


    public ConcurrentHashMap<String, RpcFuture> pending = new ConcurrentHashMap<>();

    //类名,RpcHandler
    public ConcurrentHashMap<String, List<RpcHandler>> handlerMap = new ConcurrentHashMap<>();

    public ZubboApplication(String zookeeperAddress, String serverAddress) throws InterruptedException {
        ZubboConfig.serverAddress = serverAddress;
        ZubboConfig.zookeeperAddress = zookeeperAddress;
        this.zookeeperAddress = ZubboConfig.zookeeperAddress;
        this.serverAddress = ZubboConfig.serverAddress;

        client = CuratorFrameworkFactory.builder()
                .connectString(zookeeperAddress)
                .sessionTimeoutMs(5000)
                .connectionTimeoutMs(3000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .namespace(root)
                .build();
        client.start();
        log.info("ZubboApplication connect to zookeeper: {}", this.zookeeperAddress);

    }


    /**
     * 监听zookeeper
     *
     * @throws Exception
     */
    public void startWatch() throws Exception {
        /**
         * 在注册监听器的时候，如果传入此参数，当事件触发时，逻辑由线程池处理
         */
        ExecutorService pool = Executors.newFixedThreadPool(2);

        /**
         * 监听子节点的变化情况
         */
//        final PathChildrenCache childrenCache = new PathChildrenCache(client, "/" + serviceName + "/" + "providers", true);
        final TreeCache childrenCache = new TreeCache(client, "/");
        childrenCache.getListenable().addListener(
                new TreeCacheListener() {
                    @Override
                    public void childEvent(CuratorFramework curatorFramework, TreeCacheEvent event) throws Exception {
                        switch (event.getType()) {
                            case NODE_ADDED:
                                String[] paths = event.getData().getPath().split("/");
                                //ex: /com.anyang.CountService/consumers/localhost:3002
                                if (paths.length > 0) {
                                    ConnectionManager.getInstance().listeningServices.add(paths[1]);
                                }
                                log.info("NODE_ADDED: " + event.getData().getPath());
                                break;
                            case NODE_REMOVED:
                                log.info("NODE_REMOVED: " + event.getData().getPath());
                                break;
                            case NODE_UPDATED:
                                log.info("NODE_UPDATED: " + event.getData().getPath());
                                break;
                            default:
                                break;
                        }
                    }

                },
                pool
        );
        childrenCache.start();
    }

    /**
     * @param clazz the type of service
     * @return
     */
    public <T> T subscribe(Class<T> clazz) throws Exception {
        String serviceName = clazz.getName();
        List<String> providers = client.getChildren().forPath("/" + serviceName + "/" + "providers");
        log.info(providers.toString());
        if (CollectionUtils.isEmpty(handlerMap.get(providers.get(0)))) {

            log.info("从订阅服务 zookeeper: {}", serviceName);
            CreateBuilder createBuilder = client.create();
            createServiceNode(client, createBuilder, serviceName);

            createBuilder.withMode(CreateMode.EPHEMERAL);
            if (client.checkExists().forPath("/" + serviceName + "/" + "consumers" + "/" + serverAddress) == null) {
                //节点存在不创建
                createBuilder.creatingParentsIfNeeded().forPath("/" + serviceName + "/" + "consumers" + "/" + serverAddress, null);
            }
            //添加到监听服务队列
            ConnectionManager.getInstance().listeningServices.add(serviceName);


            EventLoopGroup loopGroup = new NioEventLoopGroup(4);
            Bootstrap client = new Bootstrap();
            client.group(loopGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 0))
                                    .addLast(new ByteToMessageDecoder() {
                                        @Override
                                        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
                                            int length = in.readInt();
                                            byte[] data = new byte[length];
                                            in.readBytes(data);
                                            SyncRpcResponse response = SerializationUtil.deSerialize(data, SyncRpcResponse.class);
                                            RpcFuture future = pending.get(response.getRequestId());
                                            future.done(response);
                                            log.info("client received from server : {}", response.toString());
                                        }
                                    })
                                    .addLast(new RpcHandler(ZubboApplication.this) {
                                        @Override
                                        public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                            super.channelActive(ctx);
                                            this.channel = ctx.channel();
                                            List<RpcHandler> handlers = handlerMap.get(providers.get(0));
                                            if (handlers == null) {
                                                handlers = new ArrayList<>();
                                            }
                                            handlers.add(this);
                                            handlerMap.put(providers.get(0), handlers);
                                            this.initLatch.countDown();
                                        }

                                        @Override
                                        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                            super.channelInactive(ctx);
                                            handlerMap.remove(providers.get(0));
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

            String[] inets = providers.get(0).split(":");
            client.connect(new InetSocketAddress(inets[0], Integer.valueOf(inets[1])))
                    .addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (future.isSuccess()) {
                                log.info("success connect to remote server: {}", providers.get(0));
                            } else {
                                log.error("connect failed");
                            }
                        }
                    });
        }

        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new RpcProxy(this, providers.get(0)));
    }

    public void register(Object service, String serverAddress) throws Exception {
        Class[] infs = service.getClass().getInterfaces();
        String serviceName = null;
        if (infs == null || infs.length <= 0) {
            log.error("RpcService does not found any interface to delegate, class name: {}", service.getClass().getName());
            throw new IllegalArgumentException(String.format("RpcService does not found any interface to delegate, class name: {}", service.getClass().getName()));
        } else {
            serviceName = infs[0].getTypeName();
        }
        log.info("注册服务到 zookeeper: {}", serviceName);
        CreateBuilder createBuilder = client.create();
        createServiceNode(client, createBuilder, serviceName);

        createBuilder.withMode(CreateMode.EPHEMERAL);
        createBuilder.creatingParentsIfNeeded().forPath("/" + serviceName + "/" + "providers" + "/" + serverAddress, null);
    }

    private void createServiceNode(CuratorFramework client, CreateBuilder createBuilder, String serviceName) throws Exception {
        createBuilder.withMode(CreateMode.PERSISTENT);
        String consumersPath = "/" + serviceName + "/" + "consumers";
        String providersPath = "/" + serviceName + "/" + "providers";

        //check if exist
        if (client.checkExists().forPath(consumersPath) == null) {
            createBuilder.creatingParentsIfNeeded().forPath("/" + serviceName + "/" + "consumers", null);
        }

        if (client.checkExists().forPath(providersPath) == null) {
            createBuilder.creatingParentsIfNeeded().forPath("/" + serviceName + "/" + "providers", null);
        }
    }

    public String getZookeeperAddress() {
        return zookeeperAddress;
    }

    public void setZookeeperAddress(String zookeeperAddress) {
        this.zookeeperAddress = zookeeperAddress;
    }
}
