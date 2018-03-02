package org.jcodec.common.tools;
import static java.lang.Math.min;
import static js.lang.System.arraycopy;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.PictureHiBD;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rect;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class ImageOP {

    /**
     * 
     * @param src
     * @param area
     * @return
     */
    public static void subImageWithFillInt(int[] src, int width, int height, int[] dst, int dstW, int dstH, int offX,
            int offY) {
        int srcHeight = min(height - offY, dstH);
        int srcWidth = min(width - offX, dstW);
        int srcStride = width;

        int dstOff = 0, srcOff = offY * srcStride + offX;
        int i;
        for (i = 0; i < srcHeight; i++) {
            int j;
            for (j = 0; j < srcWidth; j++) {
                dst[dstOff + j] = src[srcOff + j];
            }
            int lastPix = dst[j - 1];
            for (; j < dstW; j++)
                dst[dstOff + j] = lastPix;
            srcOff += srcStride;
            dstOff += dstW;
        }
        int lastLine = dstOff - dstW;
        for (; i < dstH; i++) {
            arraycopy(dst, lastLine, dst, dstOff, dstW);
            dstOff += dstW;
        }
    }
    
    public static void subImageWithFill(byte[] src, int width, int height, byte[] dst, int dstW, int dstH, int offX,
            int offY) {
        int srcHeight = min(height - offY, dstH);
        int srcWidth = min(width - offX, dstW);
        int srcStride = width;

        int dstOff = 0, srcOff = offY * srcStride + offX;
        int i;
        for (i = 0; i < srcHeight; i++) {
            int j;
            for (j = 0; j < srcWidth; j++) {
                dst[dstOff + j] = src[srcOff + j];
            }
            byte lastPix = dst[j - 1];
            for (; j < dstW; j++)
                dst[dstOff + j] = lastPix;
            srcOff += srcStride;
            dstOff += dstW;
        }
        int lastLine = dstOff - dstW;
        for (; i < dstH; i++) {
            arraycopy(dst, lastLine, dst, dstOff, dstW);
            dstOff += dstW;
        }
    }
    
    public static void subImageWithFillPic8(Picture _in, Picture out, Rect rect) {
        int width = _in.getWidth();
        int height = _in.getHeight();
        ColorSpace color = _in.getColor();
        byte[][] data = _in.getData();

        for (int i = 0; i < data.length; i++) {
            subImageWithFill(data[i], width >> color.compWidth[i], height >> color.compHeight[i],
                    out.getPlaneData(i), rect.getWidth() >> color.compWidth[i],
                    rect.getHeight() >> color.compHeight[i], rect.getX() >> color.compWidth[i],
                    rect.getY() >> color.compHeight[i]);
        }
    }
}
