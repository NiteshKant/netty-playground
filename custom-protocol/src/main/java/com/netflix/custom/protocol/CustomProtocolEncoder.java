package com.netflix.custom.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Nitesh Kant
 */
public class CustomProtocolEncoder extends MessageToMessageEncoder<Attribute> {

    private static final Logger logger = LoggerFactory.getLogger(CustomProtocolEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, Attribute msg, List<Object> out) throws Exception {
        logger.info("Received an attribute with name {} to encode.", null == msg ? "null" : msg.name());

        if (null == msg) {
            return;
        }

/*
        CompositeByteBuf toAdd = ctx.alloc().compositeBuffer(3);
        ByteBuf header = ctx.alloc().buffer(msg.name().length() + 1);
        header.writeBytes(msg.name().getBytes(CustomProtocolConstants.charsetToUse));
        header.writeByte(CustomProtocolConstants.COLON);
        toAdd.addComponent(header);
        toAdd.addComponent(msg.value());
        toAdd.addComponent(ctx.alloc().buffer(1).writeByte(CustomProtocolConstants.CR));
        ByteBuf copy = toAdd.copy();
        byte[] content = new byte[toAdd.readableBytes()];
        while (copy.isReadable()) {
            copy.readBytes(content);
        }
*/
        StringBuilder responseBuilder = new StringBuilder();
        responseBuilder.append(msg.name());
        responseBuilder.append((char)CustomProtocolConstants.COLON);
        String attribValue = msg.valueAsString(CustomProtocolConstants.charsetToUse);
        responseBuilder.append(attribValue); /// TODO: re-use the sent buffer, this is a data copy.

        String response = responseBuilder.toString();
        logger.info("Encoded an attribute to data {}", response);
        ByteBuf toAdd = ctx.alloc().buffer(response.length())
                           .writeBytes(response.getBytes(CustomProtocolConstants.charsetToUse))
                           .writeByte(CustomProtocolConstants.CR);

        out.add(toAdd);
    }
}
