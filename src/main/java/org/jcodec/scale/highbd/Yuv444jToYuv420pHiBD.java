package org.jcodec.scale.highbd;

import org.jcodec.common.model.PictureHiBD;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Yuv444jToYuv420pHiBD implements TransformHiBD {
    public static int Y_COEFF = 7168;

    public Yuv444jToYuv420pHiBD() {
    }

    public void transform(PictureHiBD src, PictureHiBD dst) {
        int[] sy = src.getPlaneData(0);
        int[] dy = dst.getPlaneData(0);
        for (int i = 0; i < src.getPlaneWidth(0) * src.getPlaneHeight(0); i++)
            dy[i] = (sy[i] * Y_COEFF >> 13) + 16;

        copyAvg(src.getPlaneData(1), dst.getPlaneData(1), src.getPlaneWidth(1), src.getPlaneHeight(1));
        copyAvg(src.getPlaneData(2), dst.getPlaneData(2), src.getPlaneWidth(2), src.getPlaneHeight(2));
    }

    private void copyAvg(int[] src, int[] dst, int width, int height) {
        int offSrc = 0, offDst = 0;
        for (int y = 0; y < (height >> 1); y++) {
            for (int x = 0; x < width; x += 2, offDst++, offSrc += 2) {

                int a = ((src[offSrc] - 128) * Y_COEFF >> 13) + 128;
                int b = ((src[offSrc + 1] - 128) * Y_COEFF >> 13) + 128;
                int c = ((src[offSrc + width] - 128) * Y_COEFF >> 13) + 128;
                int d = ((src[offSrc + width + 1] - 128) * Y_COEFF >> 13) + 128;

                dst[offDst] = (a + b + c + d + 2) >> 2;
            }
            offSrc += width;
        }
    }
}
