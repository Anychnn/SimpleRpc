import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

import static org.junit.Assert.*;

public class NettyServerTest {

    @Test
    public void test() throws IOException {
        EventLoopGroup boss = new NioEventLoopGroup();
        EventLoopGroup worker = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 4))
//                                    .addLast(new LineBasedFrameDecoder(1024))
//                                    .addLast(new HttpResponseEncoder())
                                    .addLast(new StringDecoder())
                                    .addLast(new StringEncoder())
                                    .addLast(new SimpleChannelInboundHandler<HeartBeat>() {
                                        @Override
                                        protected void messageReceived(ChannelHandlerContext ctx, HeartBeat msg) throws Exception {
                                            ctx.channel().eventLoop();
                                            System.out.println("heart beat received");
                                        }
                                    })
                                    .addLast(new ChannelHandlerAdapter() {
                                        private int tst = 0;

                                        @Override
                                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                            if (msg instanceof String) {
                                                System.out.println("server channelRead..");
                                                tst++;
                                                System.out.println(ctx.channel().remoteAddress() + "->Server " + tst + ":" + msg.toString());
//                                            ctx.write("server write" + msg);


//                                            FullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
//                                                    HttpResponseStatus.OK,
//                                                    Unpooled.copiedBuffer("server write" + msg, CharsetUtil.UTF_8));
//
//                                            resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
//
//                                            ctx.writeAndFlush(resp);
                                            } else if (msg instanceof HeartBeat) {
                                                System.out.println("read heart beat");
                                            }
                                        }

                                        @Override
                                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                            cause.printStackTrace();
                                            ctx.close();
                                        }
                                    });
                        }
                    });

            ChannelFuture future = bootstrap.bind(20200).sync();

            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
