import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.*;

import java.net.InetSocketAddress;
import java.util.List;

public class Client {
    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup worker = null;
        try {
            Bootstrap bootstrap = new Bootstrap();
            worker = new NioEventLoopGroup();
            bootstrap
                    .channel(NioSocketChannel.class)
                    .group(worker)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline()
//                                    .addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 0))
//                                    .addLast(new LengthFieldPrepender(4))
                                    .addLast(new MessageToByteEncoder() {
                                        @Override
                                        protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
                                            if (msg instanceof String) {
                                                byte[] bytes = ((String) msg).getBytes();
                                                out.writeInt(bytes.length);
                                                out.writeBytes(bytes);
                                            }
                                        }

                                        @Override
                                        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                                            super.channelReadComplete(ctx);
                                            System.out.println("channelReadComplete");
                                        }
                                    })
                                    .addLast(new ChannelHandlerAdapter() {
                                        @Override
                                        public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                            ctx.writeAndFlush("hello");
                                        }
                                    });
                        }
                    });
            ChannelFuture future = bootstrap.connect(new InetSocketAddress("localhost", 5050))
                    .sync();

            future.channel().closeFuture().sync();
        } finally {
            if (worker != null) {
                worker.shutdownGracefully();
            }
        }


    }
}
