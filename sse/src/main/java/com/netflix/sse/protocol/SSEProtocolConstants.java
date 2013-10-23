package com.netflix.sse.protocol;

import io.netty.util.AttributeKey;

/**
 * @author Nitesh Kant
 */
public class SSEProtocolConstants {

    public static final AttributeKey<Boolean> RESPONSE_INITIATED_KEY = new AttributeKey<Boolean>("stream_response_initiated");
    public static final AttributeKey<Boolean> REQUEST_INITIATED_KEY = new AttributeKey<Boolean>("stream_request_initiated");
}
