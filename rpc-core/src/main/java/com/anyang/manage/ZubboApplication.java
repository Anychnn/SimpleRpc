package com.anyang.manage;

import com.anyang.config.ZubboConfig;
import com.anyang.netty.ClientNettyBootstrap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.zookeeper.CreateMode;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.List;

@Slf4j
public class ZubboApplication {

    private String zookeeperAddress;
    private String serverAddress;

    public ZookeeperManager zookeeperManager;

    private ClientNettyBootstrap clientNettyBootstrap;

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
        log.info("providers fount : {}", providers.toString());
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

        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new RpcProxy(providers.get(0)));
    }

    //netty连接远程服务
    //serverNodeAddress example: localhost:3000
    private void connectToServerNode(InetSocketAddress socketAddress) {
        clientNettyBootstrap = new ClientNettyBootstrap();
        clientNettyBootstrap.connect(socketAddress);
    }


    public void close() {
        if (clientNettyBootstrap != null) {
            clientNettyBootstrap.close();
        }
    }


    public String getZookeeperAddress() {
        return zookeeperAddress;
    }

    public void setZookeeperAddress(String zookeeperAddress) {
        this.zookeeperAddress = zookeeperAddress;
    }
}
