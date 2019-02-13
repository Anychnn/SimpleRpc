package com.anyang;

import com.anyang.annotation.RpcService;
import com.anyang.manage.ZubboApplication;
import com.anyang.registry.ConnectionManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class RpcServer implements ApplicationContextAware, InitializingBean {
    private String zookeeperAddress;
    private String serverAddress;
    private ZubboApplication zubboApplication;


    public RpcServer(String zookeeperAddress, String serverAddress) throws Exception {
        this.zookeeperAddress = zookeeperAddress;
        this.serverAddress = serverAddress;
        zubboApplication = new ZubboApplication(zookeeperAddress, serverAddress);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        log.info("Rpcserver set applicationContext()");
        Map<String, Object> beanMap = applicationContext.getBeansWithAnnotation(RpcService.class);
        if (MapUtils.isNotEmpty(beanMap)) {
            for (Object bean : beanMap.values()) {
                try {
                    Class[] infs = bean.getClass().getInterfaces();
                    String serviceName = null;
                    if (infs == null || infs.length <= 0) {
                        log.error("RpcService does not found any interface to delegate, class name: {}", bean.getClass().getName());
                        throw new IllegalArgumentException(String.format("RpcService does not found any interface to delegate, class name: {}", bean.getClass().getName()));
                    } else {
                        serviceName = infs[0].getTypeName();
                    }
                    ConnectionManager.getInstance().serviceBeanMap.put(serviceName, bean);

                    zubboApplication.zookeeperManager.registerService(bean, serverAddress);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ConnectionManager connectionManager = ConnectionManager.getInstance();
        connectionManager.initServer();
    }
}
