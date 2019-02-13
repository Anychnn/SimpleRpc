package com.anyang.invoke;

import com.anyang.protocal.RpcRequest;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Slf4j
public class JDKInvoker implements Invoker {

    @Override
    public Object invoke(Object serviceBean, RpcRequest request) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method method = serviceBean.getClass().getMethod(request.getMethodName(), request.getParameterTypes());
        method.setAccessible(true);
        Object result = method.invoke(serviceBean, request.getParameters());
        log.info("result:" + result);
        return result;
    }
}
