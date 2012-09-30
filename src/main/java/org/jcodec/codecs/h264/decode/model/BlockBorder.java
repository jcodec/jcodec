package org.jcodec.codecs.h264.decode.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A block border
 * 
 * Containes left-most pixels from left neighbor block (A), bottom-most pixels
 * from upper block (B), bottom-most pixels from upper-right block (D),
 * bottom-right pixel from upper-left block (C)
 * 
 * All values can be set to null, which means they are not available
 * 
 * @author Jay Codec
 * 
 */
public class BlockBorder {
    int[] left;
    int[] top;
    int[] topRight;
    Integer topLeft;

    public BlockBorder(int[] left, int[] top, Integer topLeft) {
        this.left = left;
        this.top = top;
        this.topLeft = topLeft;
    }

    public int[] getLeft() {
        return left;
    }

    public int[] getTop() {
        return top;
    }

    public Integer getTopLeft() {
        return topLeft;
    }
}