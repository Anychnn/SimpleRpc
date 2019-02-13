package com.anyang;

import com.anyang.manage.ZubboApplication;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class RpcClient implements ApplicationContextAware, InitializingBean {

    private String zookeeperAddress;

    private String serverAdress;

    private ZubboApplication application;

    public RpcClient(String zookeeperAddress, String serverAdress) throws InterruptedException {
        this.zookeeperAddress = zookeeperAddress;
        this.serverAdress = serverAdress;
        application = new ZubboApplication(zookeeperAddress, serverAdress);
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

    }
}
