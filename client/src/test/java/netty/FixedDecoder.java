package netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.TooLongFrameException;

import java.util.List;

public class FixedDecoder extends ByteToMessageDecoder {

    private int frameSize = 3;
    private int maxFrameSize = 4;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int size = in.readableBytes();
        if (size >= maxFrameSize) {
            in.clear();
            throw new TooLongFrameException();
        }

        ByteBuf buf = in.readBytes(size);
        out.add(buf);

    }
}
