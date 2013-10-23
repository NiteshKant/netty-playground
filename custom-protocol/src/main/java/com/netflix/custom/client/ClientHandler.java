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
package com.netflix.custom.client;

import com.netflix.custom.protocol.Attribute;
import com.netflix.custom.server.StreamAttributeHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class ClientHandler extends SimpleChannelInboundHandler<Attribute> {

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private final MessageReceiveCallback callback;
    private AtomicBoolean waitingForReply = new AtomicBoolean();
    private volatile String lastCommandSent;
    private ChannelHandlerContext ctx;

    public ClientHandler(MessageReceiveCallback callback) {
        this.callback = callback;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Attribute msg) throws Exception {
        callback.onNewMessage(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        if (null != lastCommandSent && !lastCommandSent.equals(StreamAttributeHandler.ATTRIBUTE_NAME)) {
            waitingForReply.set(false);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        logger.error("Exception occured on the channel.", cause);
        ctx.close();
    }

    public void sendMessage(Attribute attribute) {
        sendNextMessage(attribute, ctx);
    }

    private void sendNextMessage(Attribute msgToSend, ChannelHandlerContext ctx) {
        if (!waitingForReply.compareAndSet(false, true)) {
            logger.error("Client can not send a command when the response from the last command is not finished. Last command sent: "
                         + lastCommandSent);
            return;
        }
        final String attribName = msgToSend.name();
        lastCommandSent = attribName;
        ctx.writeAndFlush(msgToSend).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    System.out.println("> " + attribName);
                }
                logger.info(String.format("Message sent to the server. Future result, success? %s",
                                          future.isSuccess()));
            }
        });
    }

    public interface MessageReceiveCallback {

        void onNewMessage(Attribute attribute);
    }
}
