package com.netflix.custom.client;

import com.netflix.custom.protocol.Attribute;
import com.netflix.custom.protocol.CustomProtocolConstants;
import com.netflix.custom.protocol.CustomProtocolDecoder;
import com.netflix.custom.protocol.CustomProtocolEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

/**
 * @author Nitesh Kant
 */
public class CustomProtocolClient {

    private static final Logger logger = LoggerFactory.getLogger(CustomProtocolClient.class);

    private static final Pattern ATTRIB_NAME_VALUE_PAIR_SPLIT_PATTERN = Pattern.compile(":");
    private final String host;
    private final int port;

    private final ClientHandler.MessageReceiveCallback callback = new ClientHandler.MessageReceiveCallback() {
        @Override
        public void onNewMessage(Attribute attribute) {
            System.out.println(String.format("< %s : %s",
                                             attribute.name(),
                                             attribute.valueAsString(CustomProtocolConstants.charsetToUse)));

        }
    };
    private EventLoopGroup group;
    private ChannelFuture shutdownFuture;
    private ChannelFuture clientChannel;
    private ClientHandler clientHandler;

    public CustomProtocolClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void run() throws Exception {
        // Configure the client.
        group = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        b.group(group)
         .channel(NioSocketChannel.class)
         .option(ChannelOption.TCP_NODELAY, true)
         .handler(new ChannelInitializer<SocketChannel>() {
             @Override
             public void initChannel(SocketChannel ch) throws Exception {
                 clientHandler = new ClientHandler(callback);
                 ch.pipeline()
                   .addLast("decoder", new CustomProtocolDecoder())
                   .addLast("encoder", new CustomProtocolEncoder())
                   .addLast(new DefaultEventExecutorGroup(1), "client_handler", clientHandler);
             }
         });

        // Start the client.
        clientChannel = b.connect(host, port);
        shutdownFuture = clientChannel.sync();
    }

    public static void main(String[] args) throws Exception {

        final CustomProtocolClient client = new CustomProtocolClient("localhost", 8099);
        client.run();
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                client.group.shutdownGracefully();

            }
        }));

        System.out.println("Enter an attribute to send, with name-value separated by a :");
        BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
        String lineInput;
        while ((lineInput = inReader.readLine()) != null ) {
            String[] nameValuePair = ATTRIB_NAME_VALUE_PAIR_SPLIT_PATTERN.split(lineInput);
            if (nameValuePair.length != 2) {
                System.err.println("Enter a attribute name-value separated by :. You entered: " + lineInput);
                continue;
            } else {
                String value = nameValuePair[1];
                client.clientHandler.sendMessage(new Attribute(nameValuePair[0], Unpooled.buffer(value.length())
                                                                                         .writeBytes(value.getBytes())));
            }
        }
    }
}
