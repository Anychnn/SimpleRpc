package com.anyang.netty;

import com.anyang.config.OutTypeEnum;
import com.anyang.invoke.RpcFuture;
import com.anyang.manage.ZubboContext;
import com.anyang.protocal.RpcResponse;
import com.anyang.util.SerializationUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class RpcResponseDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() <= 0) return;
        int length = in.readInt();
        byte[] data = new byte[length];
        in.readBytes(data);
        RpcResponse response = SerializationUtil.deSerialize(data, RpcResponse.class);
        RpcFuture future = ZubboContext.getInstance().pending.get(response.getRequestId());
        if (future == null) {
            log.error("future is null and requestId is {}", response.getRequestId());
            log.error(ZubboContext.getInstance().pending.toString());
            log.error("future : " + ZubboContext.getInstance().pending.get(response.getRequestId()));
        } else {
            future.done(response);
            log.info("client received from server : {}", response.toString());
        }
    }
}
