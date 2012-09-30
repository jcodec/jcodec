package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public enum SliceType {
    P, B, I, SP, SI;

    public boolean isIntra() {
        return this == I || this == SI;
    }

    public boolean isInter() {
        return this != I && this != SI;
    }

    public static SliceType fromValue(int j) {
        for (SliceType sliceType : values()) {
            if (sliceType.ordinal() == j)
                return sliceType;
        }
        return null;
    }
}
