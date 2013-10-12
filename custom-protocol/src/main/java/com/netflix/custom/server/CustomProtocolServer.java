package com.netflix.custom.server;

import com.netflix.custom.protocol.CustomProtocolDecoder;
import com.netflix.custom.protocol.CustomProtocolEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Nitesh Kant
 */
public class CustomProtocolServer {

    private static final Logger logger = LoggerFactory.getLogger(CustomProtocolServer.class);

    private final ServerBootstrap bootstrap;

    public CustomProtocolServer(ChannelInitializer<SocketChannel> channelInitializer) {
        bootstrap = new ServerBootstrap();
        NioEventLoopGroup group = new NioEventLoopGroup();
        bootstrap.group(group)
                 .channel(NioServerSocketChannel.class)
                 .childHandler(channelInitializer);
    }

    public void start(int port) throws InterruptedException {
        ChannelFuture channelFuture = startWithNoWait(port);
        logger.info("Custom protocol server starter at port {}", port);
        channelFuture.sync();
    }

    public ChannelFuture startWithNoWait(int port) throws InterruptedException {
        return bootstrap.bind(port).sync().channel().closeFuture();
    }

    public static ChannelInitializer<SocketChannel> defaultInitializer() {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline()
                  //.addLast(new LoggingHandler())
                  .addLast("decoder", new CustomProtocolDecoder())
                  .addLast("encoder", new CustomProtocolEncoder())
                  .addLast("server_handler", new ServerHandler());
            }
        };
    }

    public static void main(String[] args) throws InterruptedException {
        CustomProtocolServer server = new CustomProtocolServer(defaultInitializer());
        server.start(8099);
    }
}
