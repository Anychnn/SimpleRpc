package com.anyang.manage;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CreateBuilder;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.lang.reflect.Proxy;

@Slf4j
public class ZubboApplication {

    private String address;
    private CuratorFramework client;
    public static final String root = "zubbo";

    public ZubboApplication(String address) {
        this.address = address;

        client = CuratorFrameworkFactory.builder()
                .connectString("112.74.62.29:2181")
                .sessionTimeoutMs(5000)
                .connectionTimeoutMs(3000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .namespace(root)
                .build();
        client.start();
        log.info("ZubboApplication connect to zookeeper: {}", address);
    }

    /**
     * @param clazz the type of service
     * @return
     */
    public static <T> T subscribe(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new RpcProxy());
    }

    public void register(Object service,String serverAddress) throws Exception {
        String serviceName = service.getClass().getName();
        log.info("register service:", serviceName);
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
