package org.jcodec.codecs.h264.decode.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class PixelBuffer {
    private int[] pred;
    private int start;
    private int logStride;

    public static PixelBuffer wrap(int[] pred, int start, int logStride) {
        return new PixelBuffer(pred, start, logStride);
    }

    public PixelBuffer(int[] pred, int start, int logStride) {
        this.pred = pred;
        this.start = start;
        this.logStride = logStride;
    }

    public void put(int x, int y, int val) {
        pred[start + x + (y << logStride)] = val;
    }

    public void put(PixelBuffer other, int width, int height) {
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                pred[start + i + (j << logStride)] = other.pred[other.start + i + (j << other.logStride)];
            }
        }
    }

    public int[] getPred() {
        return pred;
    }

    public int getStart() {
        return start;
    }

    public int getLogStride() {
        return logStride;
    }
}