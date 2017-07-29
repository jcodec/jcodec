package org.jcodec.scale.highbd;
import org.jcodec.common.model.PictureHiBD;

import java.lang.IllegalArgumentException;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Yuv422pToYuv420jHiBD implements TransformHiBD {
    public static int COEFF = 9362;
    private int shift;
    private int halfSrc;
    private int halfDst;

    public Yuv422pToYuv420jHiBD(int upshift, int downshift) {
        this.shift = downshift + 13 - upshift;
        if(shift < 0) {
            throw new IllegalArgumentException("Maximum upshift allowed: " + (downshift + 13));
        }
        halfSrc = 128 << Math.max(downshift - upshift, 0);
        halfDst = 128 << Math.max(upshift - downshift, 0);
    }

    public void transform(PictureHiBD src, PictureHiBD dst) {
        int[] sy = src.getPlaneData(0);
        int[] dy = dst.getPlaneData(0);
        for (int i = 0; i < src.getPlaneWidth(0) * src.getPlaneHeight(0); i++)
            dy[i] = (sy[i] - 16) * COEFF >> shift;

        copyAvg(src.getPlaneData(1), dst.getPlaneData(1), src.getPlaneWidth(1), src.getPlaneHeight(1));
        copyAvg(src.getPlaneData(2), dst.getPlaneData(2), src.getPlaneWidth(2), src.getPlaneHeight(2));
    }

    private void copyAvg(int[] src, int[] dst, int width, int height) {
        int offSrc = 0, offDst = 0;
        for (int y = 0; y < height / 2; y++) {
            for (int x = 0; x < width; x++, offDst++, offSrc++) {
                int a = ((src[offSrc] - halfSrc) * COEFF >> shift) + halfDst;
                int b = ((src[offSrc + width] - halfSrc) * COEFF >> shift) + halfDst;

                dst[offDst] = (a + b + 1) >> 1;
            }
            offSrc += width;
        }
    }
}
