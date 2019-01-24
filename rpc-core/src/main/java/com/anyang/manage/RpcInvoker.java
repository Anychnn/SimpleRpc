package com.anyang.manage;

import java.util.concurrent.Future;

public class RpcInvoker {

    private static RpcInvoker rpcInvoker;

    public static RpcInvoker newInstance() {
        if (rpcInvoker == null) {
            synchronized (RpcInvoker.class) {
                if (rpcInvoker == null) {
                    rpcInvoker = new RpcInvoker();
                }
            }
        }
        return rpcInvoker;
    }

    Future invokeMethod(String methodName) {

        return null;
    }
}
