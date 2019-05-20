import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

public class NettyClientTest {

    @Test
    public void test() throws IOException {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new IdleStateHandler(5, 0, 0, TimeUnit.SECONDS))
                                    .addLast(new LengthFieldPrepender(4))
                                    .addLast(new StringDecoder())
                                    .addLast(new StringEncoder())
                                    .addLast(new MessageToByteEncoder<HeartBeat>() {
                                        @Override
                                        protected void encode(ChannelHandlerContext ctx, HeartBeat msg, ByteBuf out) throws Exception {

                                        }
                                    })
                                    .addLast(new ChannelHandlerAdapter() {

                                        @Override
                                        public void channelActive(ChannelHandlerContext ctx) {
//                                            for (int i = 0; i < 1; i++) {
                                            ctx.writeAndFlush("HelloWorldClientHandler Active");
                                            ctx.writeAndFlush(new HeartBeat());
                                            System.out.println("HelloWorldClientHandler Active");
//                                            }
                                        }

                                        @Override
                                        public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                            System.out.println("HelloWorldClientHandler read Message:" + msg);
                                            ctx.close();
                                        }


                                        @Override
                                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                            cause.printStackTrace();
                                            ctx.close();
                                        }

                                    })
                                    .addLast(new HeartBeatServerHandler());
                        }
                    });

            ChannelFuture future = bootstrap.connect("localhost", 20200).sync();

//            future.channel().writeAndFlush("hello i am a common client");
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
