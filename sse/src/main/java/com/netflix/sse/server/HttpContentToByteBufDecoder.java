package com.netflix.sse.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Nitesh Kant
 */
public class HttpContentToByteBufDecoder extends MessageToMessageDecoder<HttpObject> {

    private static final Logger logger = LoggerFactory.getLogger(HttpContentToByteBufDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out) throws Exception {
        if (msg instanceof HttpRequest) {
            if (logger.isInfoEnabled()) {
                logger.info("Received a new SSE request.");
            }
        } else if (msg instanceof HttpContent) {
            HttpContent content = (HttpContent) msg;
            ByteBuf payload = content.content();
            payload.retain();
            out.add(payload);
        }
    }
}
