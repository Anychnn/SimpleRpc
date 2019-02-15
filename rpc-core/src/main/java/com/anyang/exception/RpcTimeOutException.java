package com.anyang.exception;

public class RpcTimeOutException extends RuntimeException {

    public RpcTimeOutException() {
    }

    public RpcTimeOutException(String message) {
        super(message);
    }
}
