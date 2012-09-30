package org.jcodec.common.model;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public class Picture8Bit {
    private int width;
    private int height;
    private byte[] y;
    private byte[] cb;
    private byte[] cr;

    public Picture8Bit(int width, int height, byte[] y, byte[] cb, byte[] cr) {
        this.width = width;
        this.height = height;
        this.y = y;
        this.cb = cb;
        this.cr = cr;
    }

    public static Picture8Bit create422(int width, int height) {
        return new Picture8Bit(width, height, new byte[width * height], new byte[(width * height) >> 1],
                new byte[(width * height) >> 1]);
    }

    public static Picture8Bit create420(int width, int height) {
        return new Picture8Bit(width, height, new byte[width * height], new byte[(width * height) >> 2],
                new byte[(width * height) >> 2]);
    }

    public Picture8Bit(Picture8Bit other) {
        this(other.width, other.height, other.y, other.cb, other.cr);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public byte[] getY() {
        return y;
    }

    public byte[] getCb() {
        return cb;
    }

    public byte[] getCr() {
        return cr;
    }
}