package com.anyang;

import com.anyang.invoke.RpcFuture;
import com.anyang.manage.ZubboApplication;
import com.anyang.protocal.RpcRequest;
import com.anyang.protocal.RpcResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.util.StringUtils;

import java.util.concurrent.CountDownLatch;

public class RpcHandler extends SimpleChannelInboundHandler<RpcResponse> {

    private ZubboApplication application;

    protected volatile Channel channel;

    protected CountDownLatch initLatch = new CountDownLatch(1);

    public RpcHandler(ZubboApplication application) {
        this.application = application;
    }

    //    @Override
//    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        super.channelActive(ctx);
//        this.channel=ctx.channel();
//    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, RpcResponse msg) throws Exception {
        System.out.println("client received :" + msg);
    }

    public RpcFuture sendRequest(RpcRequest request) {
        if (StringUtils.isEmpty(request.getRequestId())) {
            throw new RuntimeException("request id is null");
        }
        channel.writeAndFlush(request);
        RpcFuture future = new RpcFuture(request);
        application.pending.put(request.getRequestId(), future);
        return future;
    }
}
