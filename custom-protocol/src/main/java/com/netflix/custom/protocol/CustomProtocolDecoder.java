package com.netflix.custom.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A protocol similar to SSE but is over TCP instead of HTTP. The following is the data sent/recieved over the wire:
 *
 <PRE>
 [attribute_name]:[attribute_value][line feed or carriage return]
 </PRE>
 *
 * @author Nitesh Kant
 */
public class CustomProtocolDecoder extends ReplayingDecoder<CustomProtocolDecoder.State>{

    private static final Logger logger = LoggerFactory.getLogger(CustomProtocolDecoder.class);

    private String attributeName; // transient data; current attribute name
    private CompositeByteBuf partialValueCompositeBuf;

    public CustomProtocolDecoder() {
        super(State.SKIP_CONTROL_CHARS);
    }

    @SuppressWarnings("fallthrough")
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state()) {
            case SKIP_CONTROL_CHARS:
                skipControlCharacters(in);
                checkpoint(State.READ_ATTRIBUTE_NAME);
                break;
            case READ_ATTRIBUTE_NAME:
                StringBuilder attributeNameBuilder = readTillAColonOrEndOfLine(in);
                if (null != attributeNameBuilder) {
                    attributeName = attributeNameBuilder.toString();
                }
                break;
            case READ_ATTRIBUTE_VALUE:
            case READ_PARTIAL_ATTRIBUTE_VALUE:
                readAttributeValue(in, out, ctx);
                break;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Decode loop over. attribute name {} and out list size {}", attributeName, out.size());
        }
    }

    @Nullable
    private StringBuilder readTillAColonOrEndOfLine(ByteBuf in) {
        StringBuilder attributeNameGatherer = new StringBuilder();

        for (; ; ) {
            char nextChar = (char) in.readByte();
            switch (nextChar) {
                case CustomProtocolConstants.CR:
                    // End of line before finding a colon means invalid line.
                    logger.warn("Invalid line found; encountered end of line before finding a colon. Data found: {}",
                                attributeNameGatherer.toString());
                    checkpoint(State.SKIP_CONTROL_CHARS); // again repeat the same method of finding an attribute name.
                    break;
                case CustomProtocolConstants.LF:
                    // End of line before finding a colon means invalid line.
                    logger.warn("Invalid line found; encountered end of line before finding a colon. Data found: {}",
                                attributeNameGatherer.toString());
                    checkpoint(State.SKIP_CONTROL_CHARS); // again repeat the same method of finding an attribute name.
                    break;
                case CustomProtocolConstants.COLON:
                    checkpoint(State.READ_ATTRIBUTE_VALUE);
                    break;
            }

            switch (state()) {
                case SKIP_CONTROL_CHARS:
                    // This means error encountered so ignore the buffer & return.
                    return null;
                case READ_ATTRIBUTE_VALUE:
                    return attributeNameGatherer;
                default:
                    attributeNameGatherer.append(nextChar);
                    break;
            }
        }
    }

    private void readAttributeValue(ByteBuf in, List<Object> out, ChannelHandlerContext ctx) {
        in.markReaderIndex();
        ContentEndSearcher contentEndSearcher = new ContentEndSearcher();
        int readerIndexAfterSearch = in.forEachByte(contentEndSearcher);
        if (readerIndexAfterSearch < 0) { // reached end of content before finding an end of line
            ByteBuf availableContent = in.resetReaderIndex().readBytes(actualReadableBytes());// read everything in a buffer
            if (contentEndSearcher.endOfValueFound) {
                // Buffer exactly contains the value; means only one attribute was sent.
                onAttributeReadComplete(out, availableContent);
            } else {
                if (state() == State.READ_ATTRIBUTE_VALUE) {
                    partialValueCompositeBuf = ctx.channel().alloc().compositeBuffer();
                }
                partialValueCompositeBuf.addComponent(availableContent);
                checkpoint(State.READ_PARTIAL_ATTRIBUTE_VALUE); // from now on we read partial content.
            }
        } else {
            ByteBuf attributeValue = in.resetReaderIndex().readBytes(readerIndexAfterSearch - in.readerIndex());// read till the end of value.
            onAttributeReadComplete(out, attributeValue);
        }
    }

    private void onAttributeReadComplete(List<Object> out, ByteBuf availableContent) {
        out.add(new Attribute(attributeName, availableContent));
        checkpoint(State.SKIP_CONTROL_CHARS);
    }

    /**
     * Ignores all control characters and whitespaces & leaves the reader index of the buffer at the first legible
     * character.
     *
     * @param buffer Buffer to read.
     */
    private static void skipControlCharacters(ByteBuf buffer) {
        for (;;) {
            char c = (char) buffer.readUnsignedByte();
            if (!Character.isISOControl(c) &&
                !Character.isWhitespace(c)) {
                buffer.readerIndex(buffer.readerIndex() - 1);
                break;
            }
        }
    }

    enum State {
        SKIP_CONTROL_CHARS,
        READ_ATTRIBUTE_NAME,
        READ_ATTRIBUTE_VALUE,
        READ_PARTIAL_ATTRIBUTE_VALUE,
    }

    private static class ContentEndSearcher implements ByteBufProcessor {

        private boolean endOfValueFound;

        @Override
        public boolean process(byte value) throws Exception {
            switch (value) {
                case CustomProtocolConstants.CR:
                case CustomProtocolConstants.LF:
                    endOfValueFound = true;
                    return false;
            }
            return true;
        }
    }
}
