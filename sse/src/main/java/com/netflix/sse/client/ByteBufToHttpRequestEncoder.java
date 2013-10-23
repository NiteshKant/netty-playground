package com.netflix.sse.client;

import com.netflix.sse.protocol.SSEProtocolConstants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.Attribute;

import java.util.List;

/**
 * @author Nitesh Kant
 */
public class ByteBufToHttpRequestEncoder extends MessageToMessageEncoder<ByteBuf> {

    private final String host;
    private final String serverUri;

    public ByteBufToHttpRequestEncoder(String host, String serverUri) {
        this.host = host;
        this.serverUri = serverUri;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        Attribute<Boolean> respInitiated = ctx.channel().attr(SSEProtocolConstants.RESPONSE_INITIATED_KEY);
        respInitiated.setIfAbsent(false);

        msg.retain();
        if (!respInitiated.get()) {
            DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST,
                                                                        serverUri,
                                                                        msg);
            request.headers().set(HttpHeaders.Names.HOST, host);
            request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            request.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain");
            request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, msg.readableBytes());
            out.add(request);
        } else {
            SSEClient.removeHttpSpecificHandlers(ctx.pipeline());
            out.add(msg);
        }
    }
}
