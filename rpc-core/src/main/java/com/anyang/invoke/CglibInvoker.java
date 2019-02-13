package com.anyang.invoke;

import com.anyang.protocal.RpcRequest;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;

import java.lang.reflect.InvocationTargetException;

@Slf4j
public class CglibInvoker implements Invoker {

    @Override
    public Object invoke(Object serviceBean, RpcRequest request) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        // 避免使用 Java 反射带来的性能问题，我们使用 CGLib 提供的反射 API
        FastClass serviceFastClass = FastClass.create(serviceBean.getClass());
        FastMethod serviceFastMethod = serviceFastClass.getMethod(request.getMethodName(), request.getParameterTypes());
        Object result = serviceFastMethod.invoke(serviceBean, request.getParameters());
        log.info("result:" + result);
        return result;
    }
}
