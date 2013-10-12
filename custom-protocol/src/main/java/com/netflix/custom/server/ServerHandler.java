package com.netflix.custom.server;

import com.netflix.custom.protocol.Attribute;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This handler recieves a message of type {@link Attribute}, finds a handler for the {@link Attribute#name()} from
 *
 * @author Nitesh Kant
*/
class ServerHandler extends SimpleChannelInboundHandler<Attribute> {

    private static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);

    public final AttributeTypeHandlerRegistry REGISTRY = new AttributeTypeHandlerRegistry();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, final Attribute attribute) throws Exception {
        logger.info("Received a new attribute with name {}", attribute.name());
        AttributeHandler attributeHandler = REGISTRY.handlerForAttribute(attribute.name());
        if (null == attributeHandler) {
            attributeHandler = AttributeTypeHandlerRegistry.NOT_KNOWN_ATTRIB_HANDLER;
        }
        ChannelFuture handleFuture = attributeHandler.handle(attribute, ctx);
        logger.info("Enqueued the recieved attribute for processing with handler {}", attributeHandler);
        handleFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                logger.info("Handle response sent for attribute name {}", attribute.name());
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ((StreamAttributeHandler)REGISTRY.handlerForAttribute("stream")).stopStream();
    }
}
