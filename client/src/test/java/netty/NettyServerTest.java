package netty;

import com.anyang.config.OutTypeEnum;
import com.anyang.manage.ZubboContext;
import com.anyang.netty.HeartBeat;
import com.anyang.protocal.RpcRequest;
import com.anyang.protocal.RpcResponse;
import com.anyang.util.SerializationUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.List;

@Slf4j
public class NettyServerTest {

    private String serverAddress="localhost:8080";

    @Test
    public void test() throws InterruptedException {

        EventLoopGroup worker = new NioEventLoopGroup();
        EventLoopGroup boss = new NioEventLoopGroup();
        try {
            io.netty.bootstrap.ServerBootstrap bootstrap = new io.netty.bootstrap.ServerBootstrap();
            bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(1024, 4, 4, 0, 0))
                                    .addLast(new ByteToMessageDecoder() {
                                        @Override
                                        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
                                            if (in.readableBytes() <= 0){
                                                log.warn("channel read rejected, no enough bytes");
                                                return;
                                            }

                                            int type = in.readInt(); //protocal object type :see OutTypeEnum
                                            int length = in.readInt();
                                            byte[] data = new byte[length];
                                            in.readBytes(data);
                                            if (type == OutTypeEnum.HEART_BEAT.getType()) {
                                                HeartBeat heartBeat = SerializationUtil.deSerialize(data, HeartBeat.class);
                                                out.add(heartBeat);
                                            } else if (type == OutTypeEnum.RRC_RESPONSE.getType()) {
                                                RpcRequest rpcRequest = SerializationUtil.deSerialize(data, RpcRequest.class);
                                                out.add(rpcRequest);
                                            }
                                        }
                                    })
                                    .addLast(new SimpleChannelInboundHandler<HeartBeat>() {
                                        //process heart beat
//                                        @Override
//                                        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
//                                            super.channelReadComplete(ctx);
//                                            //todo
//                                        }

                                        @Override
                                        protected void messageReceived(ChannelHandlerContext ctx, HeartBeat msg) throws Exception {
                                            System.out.println("received heart beat:" + msg);
                                        }

                                        @Override
                                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                            cause.printStackTrace();
//                                            super.exceptionCaught(ctx, cause);
                                        }
                                    });
                        }
                    });
            String[] address = serverAddress.split(":");

            ChannelFuture future = bootstrap.bind(address[0], Integer.valueOf(address[1])).sync();
            future.channel().closeFuture().sync();
        } finally {
            worker.shutdownGracefully();
            boss.shutdownGracefully();
        }
    }
}
