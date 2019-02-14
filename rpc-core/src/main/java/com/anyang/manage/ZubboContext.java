package com.anyang.manage;

import com.anyang.invoke.RpcFuture;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Zubbo应用的上下文 存储一些变量
 * 单例
 * 用于解耦
 */
public class ZubboContext {


    private static ZubboContext instance = null;

    public static ZubboContext getInstance() {
        if (instance == null) {
            synchronized (ZubboContext.class) {
                if (instance == null) {
                    instance = new ZubboContext();
                }
            }
        }
        return instance;
    }

    private ZubboContext() {
    }

    //发起的请求
    //requestId RpcFuture
    //每发起一次 存入一个pending状态请求  请求完成 清除
    public ConcurrentHashMap<String, RpcFuture> pending = new ConcurrentHashMap<>();

    //类名,RpcHandler  每个RpcHandler代表一个netty连接
    public ConcurrentHashMap<String, List<RpcHandler>> handlerMap = new ConcurrentHashMap<>();

    //providers map 跟zookeeper保持一致
    //key->com.anyang.CountService  value->localhost:3000
    public ConcurrentHashMap<String, List<String>> serviceMap = new ConcurrentHashMap<>();

    //用于保存zookeeper节点下有哪些服务
    public ConcurrentSkipListSet<String> listeningServices = new ConcurrentSkipListSet<>();

    //server 中的服务  key->com.anyang.CountService  value->CountServiceImpl
    public Map<String, Object> serviceBeanMap = new HashMap<>();

}
