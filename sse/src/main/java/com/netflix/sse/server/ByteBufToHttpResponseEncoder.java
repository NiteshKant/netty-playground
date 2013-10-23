package com.netflix.sse.server;

import com.netflix.sse.protocol.SSEProtocolConstants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.Attribute;

import java.util.List;

/**
 * @author Nitesh Kant
 */
public class ByteBufToHttpResponseEncoder extends MessageToMessageEncoder<ByteBuf> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        Attribute<Boolean> responseInitiatedAttr = ctx.channel().attr(SSEProtocolConstants.RESPONSE_INITIATED_KEY);
        responseInitiatedAttr.setIfAbsent(false);
        msg.retain();
        if (responseInitiatedAttr.get()) {
            SSEServer.removeHttpSpecificHandlers(ctx.pipeline());
            out.add(msg);
        } else {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                                                                           msg);
            response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/event-stream");
            responseInitiatedAttr.compareAndSet(false, true);
            out.add(response);
        }
    }
}
