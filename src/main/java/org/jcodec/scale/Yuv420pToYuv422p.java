package org.jcodec.scale;

import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Yuv420pToYuv422p implements Transform {

    private int shiftUp;
    private int shiftDown;

    public Yuv420pToYuv422p(int shiftUp, int shiftDown) {
        this.shiftUp = shiftUp;
        this.shiftDown = shiftDown;
    }

    public void transform(Picture src, Picture dst) {
        copy(src.getPlaneData(0), dst.getPlaneData(0), src.getWidth(), dst.getWidth(), dst.getHeight(), shiftUp,
                shiftDown);

        copy(src.getPlaneData(1), dst.getPlaneData(1), 0, 0, 1, 2, src.getWidth() >> 1, dst.getWidth() >> 1,
                src.getHeight() >> 1, dst.getHeight(), shiftUp, shiftDown);
        copy(src.getPlaneData(1), dst.getPlaneData(1), 0, 1, 1, 2, src.getWidth() >> 1, dst.getWidth() >> 1,
                src.getHeight() >> 1, dst.getHeight(), shiftUp, shiftDown);
        copy(src.getPlaneData(2), dst.getPlaneData(2), 0, 0, 1, 2, src.getWidth() >> 1, dst.getWidth() >> 1,
                src.getHeight() >> 1, dst.getHeight(), shiftUp, shiftDown);
        copy(src.getPlaneData(2), dst.getPlaneData(2), 0, 1, 1, 2, src.getWidth() >> 1, dst.getWidth() >> 1,
                src.getHeight() >> 1, dst.getHeight(), shiftUp, shiftDown);
    }

    private static void copy(int[] src, int[] dest, int offX, int offY, int stepX, int stepY, int strideSrc,
            int strideDest, int heightSrc, int heightDst, int upShift, int downShift) {
        int offD = offX + offY * strideDest, srcOff = 0;
        for (int i = 0; i < heightSrc; i++) {
            for (int j = 0; j < strideSrc; j++) {
                dest[offD] = (src[srcOff++] & 0xff) << 2;
                offD += stepX;
            }
            int lastOff = offD - stepX;
            for (int j = strideSrc * stepX; j < strideDest; j += stepX) {
                dest[offD] = dest[lastOff];
                offD += stepX;
            }
            offD += (stepY - 1) * strideDest;
        }
        int lastLine = offD - stepY * strideDest;
        for (int i = heightSrc * stepY; i < heightDst; i += stepY) {
            for (int j = 0; j < strideDest; j += stepX) {
                dest[offD] = dest[lastLine + j];
                offD += stepX;
            }
            offD += (stepY - 1) * strideDest;
        }
    }

    private static void copy(int[] src, int[] dest, int srcWidth, int dstWidth, int dstHeight, int shiftUp,
            int shiftDown) {
        int height = src.length / srcWidth;
        int dstOff = 0, srcOff = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < srcWidth; j++) {
                dest[dstOff++] = (src[srcOff++] & 0xff) << 2;
            }
            for (int j = srcWidth; j < dstWidth; j++)
                dest[dstOff++] = dest[srcWidth - 1];
        }
        int lastLine = (height - 1) * dstWidth;
        for (int i = height; i < dstHeight; i++) {
            for (int j = 0; j < dstWidth; j++) {
                dest[dstOff++] = dest[lastLine + j];
            }
        }
    }
}