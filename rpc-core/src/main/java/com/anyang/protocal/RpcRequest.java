package com.anyang.protocal;

import lombok.Data;

/**
 * rpc请求的实体
 */
@Data
public class RpcRequest {

    private String methodName;

    private Object[] parameters;


}
