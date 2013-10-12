package com.netflix.custom.protocol;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;

/**
 * @author Nitesh Kant
 */
public class Attribute {

    private String name;
    private ByteBuf value;

    public Attribute(String name, ByteBuf value) {
        Preconditions.checkNotNull(name, "attribute name can not be null");
        Preconditions.checkNotNull(value, "attribute value can not be null");
        this.name = name;
        this.value = value;
    }

    public String name() {
        return name;
    }

    public ByteBuf value() {
        return value;
    }

    public String valueAsString(Charset charset) {
        byte[] content = new byte[value.readableBytes()];
        value.readBytes(content);
        return new String(content, charset);
    }
}
