package com.netflix.custom.server;

import com.netflix.custom.protocol.Attribute;
import com.netflix.custom.protocol.CustomProtocolConstants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
* @author Nitesh Kant
*/
public class StreamAttributeHandler implements AttributeHandler {

    public static final String ATTRIBUTE_NAME = "stream";

    private static final Logger logger = LoggerFactory.getLogger(StreamAttributeHandler.class);

    private DefaultChannelPromise currentStreamInProgressPromise;

    private final AtomicBoolean streamInProgress = new AtomicBoolean();
    public static final String STREAM_IN_PROGRESS_ERR_MSG =
            "A stream is already in progress, you can not have two stream at a time for the same client.";

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private int dataCounter;
    private ScheduledFuture<?> streamTaskFuture;

    @Override
    public ChannelFuture handle(Attribute attribute, final ChannelHandlerContext ctx) {
        DefaultChannelPromise promise = new DefaultChannelPromise(ctx.channel());
        if (!streamInProgress.compareAndSet(false, true)) {
            ByteBuf errMsg = ctx.alloc().buffer(STREAM_IN_PROGRESS_ERR_MSG.length())
                                .writeBytes(STREAM_IN_PROGRESS_ERR_MSG.getBytes(
                                        CustomProtocolConstants.charsetToUse));
            ctx.writeAndFlush(new Attribute("error", errMsg), promise);
        } else {
            currentStreamInProgressPromise = promise;
            String frequencyInSecondsStr = attribute.valueAsString(Charset.defaultCharset());
            int frequencyInSeconds;
            try {
                frequencyInSeconds = Integer.parseInt(frequencyInSecondsStr);
            } catch (NumberFormatException e) {
                logger.error("The value of a stream attribute should be an integer. Taking frequency as 1 second", e);
                frequencyInSeconds = 1;
            }

            streamTaskFuture = scheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    if (!streamInProgress.get()) {
                        return;
                    }
                    String content = "Stream data" + ++dataCounter;
                    logger.info("Sending another stream data, with content {}", content);
                    ctx.writeAndFlush(new Attribute("data", ctx.alloc().buffer(content.length())
                                                               .writeBytes(content.getBytes())));
                }
            }, 0, frequencyInSeconds, TimeUnit.SECONDS);
        }
        return promise;
    }

    public void stopStream() {
        if (streamInProgress.compareAndSet(true, false)) {
            if (null != streamTaskFuture) {
                streamTaskFuture.cancel(false);
                streamTaskFuture = null;
                logger.info("Stopped the stream.");
            }
            if (null != currentStreamInProgressPromise) {
                currentStreamInProgressPromise.cancel(false);
                currentStreamInProgressPromise = null;
            }
        }
    }
}
