/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.nitesh.netty.snoop.client;

import com.nitesh.netty.snoop.server.HttpSnoopServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.ClientCookieEncoder;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;

import java.net.URI;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple HTTP client that prints out the content of the HTTP response to
 * {@link System#out} to test {@link HttpSnoopServer}.
 */
public class HttpSnoopClient {

    private final URI uri;
    private final int requestCount;
    public static LinkedBlockingQueue<Object> requestQueue = new LinkedBlockingQueue<Object>(1);

    private AtomicInteger requestSentCount = new AtomicInteger();
    private String host;
    private String scheme;
    private Channel ch;

    public HttpSnoopClient(URI uri, int requestCount) {
        this.uri = uri;
        this.requestCount = requestCount;
    }

    public void run() throws Exception {
        scheme = uri.getScheme() == null? "http" : uri.getScheme();
        host = uri.getHost() == null? "localhost" : uri.getHost();
        int port = uri.getPort();
        if (port == -1) {
            if ("http".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("https".equalsIgnoreCase(scheme)) {
                port = 443;
            }
        }

        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            System.err.println("Only HTTP(S) is supported.");
            return;
        }

        boolean ssl = "https".equalsIgnoreCase(scheme);

        // Configure the client.
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSocketChannel.class)
             .handler(new HttpSnoopClientInitializer(ssl));

            // Make the connection attempt.
            ch = b.connect(host, port).sync().channel();
            sendRequest();

            // Wait for the server to close the connection.
            ch.closeFuture().sync();
        } finally {
            // Shut down executor threads to exit.
            group.shutdownGracefully();
        }
    }

    private void sendRequest() {
        // Prepare the HTTP request.
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.getRawPath());
        //HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.getRawPath());
        request.headers().set(HttpHeaders.Names.HOST, host);
        request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);
        request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, 0);

        // Set some example cookies.
        request.headers().set(
                HttpHeaders.Names.COOKIE,
                ClientCookieEncoder.encode(
                        new DefaultCookie("my-cookie", "foo"),
                        new DefaultCookie("another-cookie", "bar")));

        // Send the HTTP request.
        ch.writeAndFlush(request).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                System.out.println("Request sent. Success? " + future.isSuccess());
                if (!future.isSuccess()) {
                    future.cause().printStackTrace(System.out);
                }
            }
        });
    }

    public static void main(String[] args) throws Exception {

        String url = "http://localhost:8089/";

        if (args.length > 0) {
            url = args[0];
        }

        URI uri = new URI(url);

        int maxRequests = 2;
        if (args.length > 1) {
            maxRequests = Integer.parseInt(args[1]);
        }
        final HttpSnoopClient httpSnoopClient = new HttpSnoopClient(uri, maxRequests);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        requestQueue.take();
                        if (httpSnoopClient.requestSentCount.incrementAndGet() >= httpSnoopClient.requestCount) {
                            System.out.println("Sent " + httpSnoopClient.requestSentCount.get()
                                               + " requests. Stopping the client.");
                            break;
                        }
                        System.out.println("Sending a new request!!!");
                        httpSnoopClient.sendRequest();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
        httpSnoopClient.run();
    }
}
