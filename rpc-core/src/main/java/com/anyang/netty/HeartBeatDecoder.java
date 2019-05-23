package com.anyang.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class HeartBeatDecoder extends MessageToByteEncoder<HeartBeat> {
    @Override
    protected void encode(ChannelHandlerContext ctx, HeartBeat msg, ByteBuf out) throws Exception {

    }
}
