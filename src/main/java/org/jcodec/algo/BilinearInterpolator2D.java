package org.jcodec.algo;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Plane;
import org.jcodec.common.model.Size;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class BilinearInterpolator2D implements Interpolator2D {

    private static int ROUND = 1 << 15;

    /**
     * Interpolate input plane to fill the output plane
     * 
     * @param in
     * @param out
     */
    public void interpolate(Plane in, Plane out) {
        int stepX = (in.getSize().getWidth() << 8) / out.getSize().getWidth();
        int stepY = (in.getSize().getHeight() << 8) / out.getSize().getHeight();
        int[] data = in.getData();
        int stride = in.getSize().getWidth();
        int[] outData = out.getData();

        int posY = 0, line = 0, outOff = 0;
        for (int y = 0; y < out.getSize().getHeight() - 1; y++) {
            interpolateLine(outData, outOff, out.getSize().getWidth(), data, line, line + stride, posY & 0xff, stepX);
            outOff += out.getSize().getWidth();
            posY += stepY;
            line = (posY >> 8) * stride;
        }
        interpolateLine(outData, outOff, out.getSize().getWidth(), data, line, line, posY & 0xff, stepX);
    }

    private final void interpolateLine(int[] dst, int dstOff, int dstWidth, int[] src, int line, int nextLine,
            int shiftY, int stepX) {
        int posX = 0;

        for (int x = 0; x < dstWidth - 1; x++) {
            int ind = posX >> 8;
            dst[dstOff++] = interpolateHV(shiftY, posX & 0xff, src[line + ind], src[line + ind + 1],
                    src[nextLine + ind], src[nextLine + ind + 1]);

            posX += stepX;
        }
        int ind = posX >> 8;
        dst[dstOff++] = interpolateHV(shiftY, posX & 0xff, src[line + ind], src[line + ind], src[nextLine + ind],
                src[nextLine + ind]);
    }

    private final int interpolateHV(int shiftY, int shiftX, int s00, int s01, int s10, int s11) {
        int s0 = (s00 << 8) + (s01 - s00) * shiftX;
        int s1 = (s10 << 8) + (s11 - s10) * shiftX;
        return ((s0 << 8) + (s1 - s0) * shiftY + ROUND) >> 16;
    }

    /**
     * Interpolate an input picture to fill the output picture
     * 
     * @param in
     * @param out
     */
    public void interpolate(Picture in, Picture out) {
        int[][] data = in.getData();

        ColorSpace inClr = in.getColor();
        ColorSpace outClr = out.getColor();
        for (int i = 0; i < data.length; i++) {
            interpolate(new Plane(data[i], new Size(in.getWidth() * inClr.compWidth[i], in.getHeight()
                    * inClr.compHeight[i])),
                    new Plane(data[i], new Size(in.getWidth() * outClr.compWidth[i], in.getHeight()
                            * outClr.compHeight[i])));
        }
    }
}
