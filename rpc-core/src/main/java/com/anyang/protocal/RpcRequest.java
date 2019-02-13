package com.anyang.protocal;

import lombok.Data;

/**
 * rpc请求的实体
 */
@Data
public class RpcRequest {
    private String requestId;
    private String className;
    private String methodName;
    private Object[] parameters;
    private Class<?>[] parameterTypes;
}
