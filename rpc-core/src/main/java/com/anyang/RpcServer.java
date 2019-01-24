package com.anyang;

import com.anyang.annotation.RpcService;
import com.anyang.manage.ZubboApplication;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;

@Slf4j
public class RpcServer implements ApplicationContextAware, InitializingBean {
    private String zookeeperAddress;
    private String serverAddress;

    private ZubboApplication zubboApplication;

    public RpcServer(String zookeeperAddress, String serverAddress) {
        this.zookeeperAddress = zookeeperAddress;
        this.serverAddress = serverAddress;
        zubboApplication = new ZubboApplication(zookeeperAddress);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        log.info("Rpcserver set applicationContext()");
        Map<String, Object> beanMap = applicationContext.getBeansWithAnnotation(RpcService.class);
        if (MapUtils.isNotEmpty(beanMap)) {
            for (Object bean : beanMap.values()) {
                log.info("get bean {}", bean.getClass());
                try {
                    zubboApplication.register(bean, serverAddress);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}
