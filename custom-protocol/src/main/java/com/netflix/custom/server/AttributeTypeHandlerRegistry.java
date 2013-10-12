package com.netflix.custom.server;

import com.netflix.custom.protocol.Attribute;
import com.netflix.custom.protocol.CustomProtocolConstants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Nitesh Kant
 */
public class AttributeTypeHandlerRegistry {

    private static final Logger logger = LoggerFactory.getLogger(AttributeTypeHandlerRegistry.class);

    private final Map<String, AttributeHandler> attributeNameVsHandlers = new HashMap<String, AttributeHandler>();

    public static final AttributeHandler NOT_KNOWN_ATTRIB_HANDLER = new AttributeHandler() {

        public static final String NOT_HANDLED_PAYLOAD_FORMAT = "I do not understand the attribute %s you have sent me.";

        @Override
        public ChannelFuture handle(Attribute attribute, ChannelHandlerContext ctx) {
            DefaultChannelPromise promise = new DefaultChannelPromise(ctx.channel());
            String errorMsg = String.format(NOT_HANDLED_PAYLOAD_FORMAT, attribute.name());
            ByteBuf content = ctx.alloc().buffer(errorMsg.length())
                                 .writeBytes(errorMsg.getBytes(CustomProtocolConstants.charsetToUse));
            ctx.writeAndFlush(new Attribute("error", content), promise);
            return promise;
        }
    };

    public AttributeTypeHandlerRegistry() {
        attributeNameVsHandlers.put("ping", new AttributeHandler() {
            @Override
            public ChannelFuture handle(Attribute attribute, ChannelHandlerContext ctx) {
                DefaultChannelPromise promise = new DefaultChannelPromise(ctx.channel());
                ctx.writeAndFlush(new Attribute("pong", ctx.alloc().buffer(0)), promise);
                return promise;
            }
        });

        attributeNameVsHandlers.put(StreamAttributeHandler.ATTRIBUTE_NAME, new StreamAttributeHandler());
    }

    @Nullable
    public AttributeHandler handlerForAttribute(String attributeName) {
        return attributeNameVsHandlers.get(attributeName);
    }

}
