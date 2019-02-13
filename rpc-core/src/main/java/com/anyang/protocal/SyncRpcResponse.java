package com.anyang.protocal;

import lombok.Data;

@Data
public class SyncRpcResponse {
    private String requestId;
    private String error;
    private Object result;
}
