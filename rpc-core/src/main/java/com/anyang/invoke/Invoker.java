package com.anyang.invoke;

import com.anyang.protocal.RpcRequest;

import java.lang.reflect.InvocationTargetException;

public interface Invoker {
    Object invoke(Object serviceBean, RpcRequest request) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException;
}
