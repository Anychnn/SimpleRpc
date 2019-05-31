package com.anyang.netty;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class ChannelSelectorUtil {

    /**
     * select EpollEventLoopGroup on Linux or else NioEventLoopGroup
     */
    public static EventLoopGroup selectEventLoopGroupByOS() {
        if (isLinux()) {
            return new EpollEventLoopGroup();
        }
        return new NioEventLoopGroup();
    }

    public static EventLoopGroup selectEventLoopGroupByOS(int nEventLoops) {
        if (isLinux()) {
            return new EpollEventLoopGroup(nEventLoops);
        }
        return new NioEventLoopGroup(nEventLoops);
    }

    /**
     * selsect EpollServerSocketChannel.class on Linux or else NioServerSocketChannel.class
     *
     * @return
     */
    public static Class selectServerChannelClassByOS() {
        if (isLinux()) {
            return EpollServerSocketChannel.class;
        }
        return NioServerSocketChannel.class;
    }


    public static Class selectSocketChannelClassByOS(){
        if (isLinux()) {
            return EpollSocketChannel.class;
        }
        return NioSocketChannel.class;
    }

    private static boolean isLinux() {
        if ("Linux".equals(System.getProperty("os.name"))) {
            return true;
        }

        return false;
    }

}
