package com.netflix.sse.server;

import com.netflix.custom.protocol.CustomProtocolDecoder;
import com.netflix.custom.protocol.CustomProtocolEncoder;
import com.netflix.custom.server.ServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Nitesh Kant
 */
public class SSEServer {

    private static final Logger logger = LoggerFactory.getLogger(SSEServer.class);
    public static final String HTTP_DECODER_HANDLER_NAME = "decoder";
    public static final String HTTP_CONTENT_CONVERTER_HANDLER_NAME = "http_content_converter";
    public static final String HTTP_ENCODER_HANDLER_NAME = "http_encoder";
    public static final String BYTE_BUF_HTTP_RESPONSE_ENCODER_HANDLER_NAME = "byte_buf_http_response_encoder";

    private final ServerBootstrap bootstrap;

    public SSEServer(ChannelInitializer<SocketChannel> channelInitializer) {
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
                  .addLast("logger", new LoggingHandler(LogLevel.DEBUG))
                  .addLast(HTTP_DECODER_HANDLER_NAME, new HttpRequestDecoder())
                  .addLast(HTTP_CONTENT_CONVERTER_HANDLER_NAME, new HttpContentToByteBufDecoder())
                  .addLast("sse_decoder", new CustomProtocolDecoder())
                  .addLast(HTTP_ENCODER_HANDLER_NAME, new HttpResponseEncoder())
                  .addLast(BYTE_BUF_HTTP_RESPONSE_ENCODER_HANDLER_NAME, new ByteBufToHttpResponseEncoder())
                  .addLast("sse_encoder", new CustomProtocolEncoder())
                  .addLast("server_handler", new ServerHandler());
            }
        };
    }

    public static void removeHttpSpecificHandlers(ChannelPipeline pipeline) {
        pipeline.remove(HTTP_DECODER_HANDLER_NAME);
        pipeline.remove(HTTP_CONTENT_CONVERTER_HANDLER_NAME);
        pipeline.remove(HTTP_ENCODER_HANDLER_NAME);
        pipeline.remove(BYTE_BUF_HTTP_RESPONSE_ENCODER_HANDLER_NAME);
    }

    public static void main(String[] args) throws InterruptedException {
        SSEServer server = new SSEServer(defaultInitializer());
        server.start(8099);
    }
}
