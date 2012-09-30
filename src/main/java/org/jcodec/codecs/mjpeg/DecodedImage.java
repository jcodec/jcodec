package org.jcodec.codecs.mjpeg;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author Jay Codec
 *
 */
public class DecodedImage {
    private final int width;
    private final int height;
    private final int[] pixels;

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int[] getPixels() {
        return pixels;
    }

    DecodedImage(int width, int height, int[] pixels) {
        this.height = height;
        this.pixels = pixels;
        this.width = width;
    }

    public DecodedImage(int width, int height) {
        this(width, height, new int[width * height]);
    }

}
