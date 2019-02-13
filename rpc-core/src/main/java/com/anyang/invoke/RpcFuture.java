package com.anyang.invoke;

import com.anyang.protocal.RpcRequest;
import com.anyang.protocal.SyncRpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public class RpcFuture implements Future<Object> {

    private RpcRequest request;
    private SyncRpcResponse response;
    private long startTime;
    private CountDownLatch latch;


    public RpcFuture(RpcRequest request) {
        this.request = request;
        latch = new CountDownLatch(1);
        startTime = System.currentTimeMillis();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDone() {
        return response != null;
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        latch.await();
        return response.getResult();
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean awaitSuccess = false;
        try {
            awaitSuccess = latch.await(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!awaitSuccess) {
            throw new RuntimeException("rpc timeout exception");
        }
        long useTime = System.currentTimeMillis() - startTime;
        log.info("request id: {} class: {} method: {} useTime {}ms",
                request.getRequestId(), request.getClassName(), request.getMethodName(), useTime);
        return response.getResult();
    }

    public void done(SyncRpcResponse response) {
        this.response = response;
        latch.countDown();
    }
}
