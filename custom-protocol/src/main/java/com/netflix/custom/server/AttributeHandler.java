package com.netflix.custom.server;

import com.netflix.custom.protocol.Attribute;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Nitesh Kant
 */
public interface AttributeHandler {

    ChannelFuture handle(Attribute attribute, ChannelHandlerContext ctx);
}
