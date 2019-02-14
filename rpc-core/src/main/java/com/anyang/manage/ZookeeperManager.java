package com.anyang.manage;

import com.anyang.config.ZubboConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CreateBuilder;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class ZookeeperManager {

    private CuratorFramework client;

    private String zookeeperAddress;

    public ZookeeperManager(String zookeeperAddress) throws Exception {
        this.zookeeperAddress = zookeeperAddress;
        client = CuratorFrameworkFactory.builder()
                .connectString(zookeeperAddress)
                .sessionTimeoutMs(5000)
                .connectionTimeoutMs(3000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .namespace(ZubboConfig.root)
                .build();


        client.start();
        log.info("ZubboApplication connect to zookeeper: {}", this.zookeeperAddress);
        watchProviders();
    }

    public List<String> getChildren(String path) throws Exception {
        return client.getChildren().forPath(path);
    }

    //注册服务到zookeeper
    public void registerService(Object service, String serverAddress) throws Exception {
        Class[] infs = service.getClass().getInterfaces();
        String serviceName = null;
        if (infs == null || infs.length <= 0) {
            log.error("RpcService does not found any interface to delegate, class name: {}", service.getClass().getName());
            throw new IllegalArgumentException(String.format("RpcService does not found any interface to delegate, class name: {}", service.getClass().getName()));
        } else {
            serviceName = infs[0].getTypeName();
        }
        log.info("注册服务到 zookeeper: {}", serviceName);

        createServiceNode(serviceName);

        CreateBuilder createBuilder = client.create();
        createBuilder.withMode(CreateMode.EPHEMERAL);
        createBuilder.creatingParentsIfNeeded().forPath("/" + serviceName + "/" + "providers" + "/" + serverAddress, null);
    }

    public void createServiceNode(String serviceName) throws Exception {
        CreateBuilder createBuilder = client.create();
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

    public void createNodeIfNotExist(String path, CreateMode createMode) throws Exception {
        CreateBuilder createBuilder = client.create();
        createBuilder.withMode(createMode);
        if (client.checkExists().forPath(path) == null) {
            try {
                createBuilder.creatingParentContainersIfNeeded().forPath(path, null);
            } catch (KeeperException.NodeExistsException e) {
                log.warn("node exist : {}", e.toString());
            }
        }
    }


    /**
     * 监听zookeeper
     * 监听 /#service/providers  子节点的变化情况
     *
     * @throws Exception
     */
    public void watchProviders() throws Exception {
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
                                    ZubboContext.getInstance().listeningServices.add(paths[1]);
                                }
                                log.info("NODE_ADDED: " + event.getData().getPath());
                                break;
                            case NODE_REMOVED:
                                paths = event.getData().getPath().split("/");
                                //ex: /com.anyang.CountService/consumers/localhost:3002
                                if (paths.length > 0) {
                                    ZubboContext.getInstance().listeningServices.remove(paths[1]);
                                }
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

}
