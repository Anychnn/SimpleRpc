package com.anyang.manage;

import com.anyang.invoke.RpcFuture;
import com.anyang.protocal.RpcRequest;
import com.anyang.protocal.RpcResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.util.StringUtils;

import java.util.concurrent.CountDownLatch;

public class RpcHandler extends SimpleChannelInboundHandler<RpcResponse> {

    protected volatile Channel channel;

    protected CountDownLatch initLatch = new CountDownLatch(1);

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, RpcResponse msg) throws Exception {
        System.out.println("client received :" + msg);
    }

    public RpcFuture sendRequest(RpcRequest request) {
        if (StringUtils.isEmpty(request.getRequestId())) {
            throw new RuntimeException("request id is null");
        }
        RpcFuture future = new RpcFuture(request);
        ZubboContext.getInstance().pending.put(request.getRequestId(), future);
        channel.writeAndFlush(request);
        return future;
    }
}
