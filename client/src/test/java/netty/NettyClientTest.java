package netty;

import com.anyang.config.OutTypeEnum;
import com.anyang.manage.RpcHandler;
import com.anyang.manage.ZubboContext;
import com.anyang.netty.HeartBeat;
import com.anyang.netty.RpcResponseDecoder;
import com.anyang.protocal.RpcRequest;
import com.anyang.util.SerializationUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NettyClientTest {

    @Test
    public void test() throws InterruptedException {
        String serverNodeAddress = "localhost:8080";
        InetSocketAddress socketAddress = new InetSocketAddress("localhost", 8080);

        EventLoopGroup loopGroup = new NioEventLoopGroup(4);
        Bootstrap client = new Bootstrap();

        client.group(loopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)      //listening状态 的个数
                .option(ChannelOption.SO_KEEPALIVE, true)   //保持连接
                .option(ChannelOption.TCP_NODELAY, true)    //禁用nagel算法
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 0))
//                                .addLast(new IdleStateHandler(20, 0, 0, TimeUnit.SECONDS))
                                .addLast(new MessageToByteEncoder<HeartBeat>() {
                                    @Override
                                    protected void encode(ChannelHandlerContext ctx, HeartBeat msg, ByteBuf out) throws Exception {
                                        log.info("encode heart beat");
                                        out.writeInt(OutTypeEnum.HEART_BEAT.getType());
                                        byte[] bytes = SerializationUtil.serialize(msg);
                                        out.writeInt(bytes.length);
                                        out.writeBytes(bytes);
                                        ctx.flush();
                                    }
                                })
                                .addLast(new ChannelHandlerAdapter() {

                                    @Override
                                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//                                        log.info("send heart beat");
//                                        HeartBeat heartBeat = new HeartBeat();
//                                        ctx.writeAndFlush(heartBeat);
                                        ctx.fireChannelActive();

                                        ctx.executor().scheduleAtFixedRate(new Runnable() {
                                            @Override
                                            public void run() {
                                                log.info("send heart beat");
                                                HeartBeat heartBeat = new HeartBeat();
                                                ctx.writeAndFlush(heartBeat);
                                            }
                                        }, 0, 5, TimeUnit.SECONDS);
                                    }

                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                        super.exceptionCaught(ctx, cause);
                                        cause.printStackTrace();
                                    }
                                });
                    }
                });


        ChannelFuture future1 = client.connect(socketAddress)
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        log.info("success connect to remote server: {}", serverNodeAddress);
                    } else {
                        log.error("connect failed");
                    }
                });


//        Thread.sleep(5000);

         future1.channel().closeFuture().sync();
    }
}
