package com.anyang.manage;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

@Slf4j
public class RpcProxy implements InvocationHandler {

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RpcInvoker.newInstance().invokeMethod(method.getName());
        log.info("method invoked : {}", method.getName());
        return null;
    }

}
