import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;

public class HeartBeatServerHandler extends ChannelHandlerAdapter {

    private int lossConnectCount = 0;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        System.out.println("server heart beat triggered");
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleStateEvent.READER_IDLE_STATE_EVENT.state()) {
                lossConnectCount++;
            }
        }
    }
}
