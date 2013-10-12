package com.netflix.custom.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

/**
 * @author Nitesh Kant
 */
public final class CustomProtocolConstants {

    private static final Logger logger = LoggerFactory.getLogger(CustomProtocolEncoder.class);

    public static Charset charsetToUse;

    static {
        try {
            charsetToUse = Charset.forName("UTF-8");
        } catch (Exception e) {
            logger.error("Error getting utf-8 charset. Using default charset now.", e);
            charsetToUse = Charset.defaultCharset();
        }
    }

    /**
     * Carriage return
     */
    public static final byte CR = 13;

    /**
     * Line feed character
     */
    public static final byte LF = 10;

    /**
     * Colon ':'
     */
    public static final byte COLON = 58;
}
