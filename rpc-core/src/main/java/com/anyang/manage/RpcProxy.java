package com.anyang.manage;

import com.anyang.RpcHandler;
import com.anyang.invoke.RpcFuture;
import com.anyang.protocal.RpcRequest;
import com.anyang.protocal.SyncRpcResponse;
import com.anyang.registry.ConnectionManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class RpcProxy implements InvocationHandler {

    private ZubboApplication application;
    private String serverAddress;

    private static final long rpcHandlerTimeOut = 5000;

    public RpcProxy(ZubboApplication application, String serverAddress) {
        this.application = application;
        this.serverAddress = serverAddress;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//        SyncRpcResponse response = RpcInvoker.getInstance().invokeMethod(method.getDeclaringClass(), method, args);
        RpcRequest request = new RpcRequest();
        request.setMethodName(method.getName());
        request.setParameters(args);
        request.setParameterTypes(method.getParameterTypes());
        request.setClassName(method.getDeclaringClass().getName());
        request.setRequestId(UUID.randomUUID().toString());

        RpcHandler handler = chooseHandler();
        RpcFuture future = handler.sendRequest(request);
//        SyncRpcResponse response = ConnectionManager.getInstance().invokeMethod(method.getDeclaringClass(), method, args);
        log.info("method invoked : {}", method.getName());
        return future.get();
    }

    /**
     * 选择一个handler  可能handler还未初始化  所以等待
     *
     * @return
     * @throws InterruptedException
     */
    private RpcHandler chooseHandler() throws InterruptedException {
        long wait = 0;
        long interval = 100;
        while (CollectionUtils.isEmpty(this.application.handlerMap.get(serverAddress))) {
            Thread.sleep(interval);
            wait += interval;
            if (wait > rpcHandlerTimeOut) {
                throw new RuntimeException(String.format("rpcHandler init time out ,server address is {}", serverAddress));
            }
        }
        return this.application.handlerMap.get(serverAddress).get(0);
    }

}
