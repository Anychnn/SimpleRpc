package com.anyang.registry;

import com.anyang.manage.ZubboContext;
import com.anyang.invoke.InvokerEnum;
import com.anyang.config.ZubboConfig;
import com.anyang.invoke.CglibInvoker;
import com.anyang.invoke.Invoker;
import com.anyang.invoke.JDKInvoker;
import com.anyang.netty.ServerNettyBootstrap;
import com.anyang.protocal.RpcRequest;
import com.anyang.protocal.RpcResponse;
import com.anyang.util.SerializationUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class ConnectionManager {

    private volatile static ConnectionManager instance = null;

    private String serverAddress;

    public static ConnectionManager getInstance() {
        if (instance == null) {
            synchronized (ConnectionManager.class) {
                if (instance == null) {
                    instance = new ConnectionManager(ZubboConfig.serverAddress);
                }
            }
        }
        return instance;
    }


    private ConnectionManager(String serverAddress) {
        this.serverAddress = serverAddress;
    }


    public void initServer() throws InterruptedException {
        ServerNettyBootstrap nettyBootstrap = new ServerNettyBootstrap();
        nettyBootstrap.connect(this.serverAddress);
    }

}
