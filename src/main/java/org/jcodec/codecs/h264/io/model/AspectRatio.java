package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Aspect ratio
 * 
 * dynamic enum
 * 
 * @author Jay Codec
 * 
 */
public class AspectRatio {

    public static final AspectRatio Extended_SAR = new AspectRatio(255);

    private int value;

    private AspectRatio(int value) {
        this.value = value;
    }

    public static AspectRatio fromValue(int value) {
        if (value == Extended_SAR.value) {
            return Extended_SAR;
        }
        return new AspectRatio(value);
    }

    public int getValue() {
        return value;
    }
}
