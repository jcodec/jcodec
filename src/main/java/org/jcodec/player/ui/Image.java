package org.jcodec.player.ui;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Image {
    private byte[] buffer;
    private int stride;
    private int width;
    private int height;

    public Image(byte[] buffer, int stride, int width, int height) {
        this.buffer = buffer;
        this.stride = stride;
        this.width = width;
        this.height = height;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public int getStride() {
        return stride;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    void draw(Image src, int x, int y) {
        int srcOff = 0;
        for (int j = 0; j < src.height; j++) {
            int off = (y + j) * stride;
            System.arraycopy(src.buffer, srcOff, buffer, off + (x << 1), src.stride);
            srcOff += src.stride;
        }
    }
}
