package com.anyang.manage;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class RpcProxy implements InvocationHandler {

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RpcInvoker.newInstance().invokeMethod(method.getName());
        return null;
    }

}
