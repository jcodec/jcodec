package org.jcodec.codecs.h264.decode.resilence;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.commons.io.IOUtils;
import org.jcodec.codecs.h264.decode.model.DecodedChroma;
import org.jcodec.codecs.h264.decode.model.DecodedMBlock;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Paints macroblock content using it's bourders as a basis. The macroblock is
 * painted the way the border distortion is kept to minimum.
 * 
 * @author Jay Codec
 * 
 */
public class Painter {

    public DecodedMBlock paintMBlock(DecodedMBlock left, DecodedMBlock top, DecodedMBlock right, DecodedMBlock bottom) {

        int[] leftPixY = null;
        int[] leftPixCb = null;
        int[] leftPixCr = null;
        if (left != null) {
            leftPixY = new int[16];
            leftPixCb = new int[8];
            leftPixCr = new int[8];
            for (int i = 0; i < 16; i++) {
                leftPixY[i] = left.getLuma()[(i << 4) + 15];
            }

            for (int i = 0; i < 8; i++) {
                leftPixCb[i] = left.getChroma().getCb()[(i << 3) + 7];
                leftPixCr[i] = left.getChroma().getCr()[(i << 3) + 7];
            }
        }

        int[] topPixY = null;
        int[] topPixCb = null;
        int[] topPixCr = null;
        if (top != null) {
            topPixY = new int[16];
            topPixCb = new int[8];
            topPixCr = new int[8];
            for (int i = 0; i < 16; i++) {
                topPixY[i] = top.getLuma()[240 + i];
            }

            for (int i = 0; i < 8; i++) {
                topPixCb[i] = top.getChroma().getCb()[56 + i];
                topPixCr[i] = top.getChroma().getCr()[56 + i];
            }
        }

        int[] rightPixY = null;
        int[] rightPixCb = null;
        int[] rightPixCr = null;
        if (right != null) {
            rightPixY = new int[16];
            rightPixCb = new int[8];
            rightPixCr = new int[8];
            for (int i = 0; i < 16; i++) {
                rightPixY[i] = right.getLuma()[(i << 4) + 15];
            }

            for (int i = 0; i < 8; i++) {
                rightPixCb[i] = right.getChroma().getCb()[(i << 3) + 7];
                rightPixCr[i] = right.getChroma().getCr()[(i << 3) + 7];
            }
        }

        int[] bottomPixY = null;
        int[] bottomPixCb = null;
        int[] bottomPixCr = null;
        if (bottom != null) {
            bottomPixY = new int[16];
            bottomPixCb = new int[8];
            bottomPixCr = new int[8];
            for (int i = 0; i < 16; i++) {
                bottomPixY[i] = bottom.getLuma()[240 + i];
            }

            for (int i = 0; i < 8; i++) {
                bottomPixCb[i] = bottom.getChroma().getCb()[56 + i];
                bottomPixCr[i] = bottom.getChroma().getCr()[56 + i];
            }
        }

        int[] cb = paintChroma(leftPixCb, topPixCb, rightPixCb, bottomPixCb);
        int[] cr = paintChroma(leftPixCr, topPixCr, rightPixCr, bottomPixCr);

        int[] luma;

        int cnt = (top != null ? 1 : 0) + (left != null ? 1 : 0) + (right != null ? 1 : 0) + (bottom != null ? 1 : 0);

        if (cnt == 4)
            luma = paintLuma4Sides(leftPixY, topPixY, rightPixY, bottomPixY);
        else if (cnt == 3) {
            if (top == null)
                luma = paintLuma3SidesLRB(leftPixY, rightPixY, bottomPixY);
            else if (bottom == null)
                luma = paintLuma3SidesLTR(leftPixY, topPixY, rightPixY);
            else if (left == null)
                luma = paintLuma3SidesTRB(topPixY, rightPixY, bottomPixY);
            else
                luma = paintLuma3SidesLTB(leftPixY, topPixY, bottomPixY);
        } else if (cnt == 2) {
            if (top != null && bottom != null)
                luma = paintLumaVert(topPixY, bottomPixY);
            else if (left != null && right != null)
                luma = paintLumaHor(leftPixY, rightPixY);
            else if (left != null) {
                if (top != null)
                    luma = paint2SideLuma(leftPixY, topPixY);
                else
                    luma = paint2SideLuma(leftPixY, bottomPixY);
            } else {
                if (top != null)
                    luma = paint2SideLuma(rightPixY, topPixY);
                else
                    luma = paint2SideLuma(rightPixY, bottomPixY);
            }
        } else {
            if (top != null)
                luma = paintLumaVer(topPixY);
            else if (left != null)
                luma = paintLumaHor(leftPixY);
            else if (right != null)
                luma = paintLumaHor(rightPixY);
            else
                luma = paintLumaVer(bottomPixY);
        }

        return new DecodedMBlock(luma, new DecodedChroma(cb, cr, 0, 0), 0, null, null, null);

    }

    private int[] paintLumaHor(int[] leftPixY, int[] rightPixY) {
        int[] luma = new int[256];

        for (int j = 0; j < 16; j++) {
            int off = j << 4;
            luma[off + 8] = (leftPixY[j] + rightPixY[j] + 1) >> 1;
            luma[off + 12] = (luma[off + 8] + rightPixY[j] + 1) >> 1;
            luma[off + 4] = (luma[off + 8] + leftPixY[j] + 1) >> 1;

            luma[off + 1] = luma[off + 2] = (leftPixY[j] + luma[off + 4] + 1) >> 1;
            luma[off + 6] = (luma[off + 4] + luma[off + 8] + 1) >> 1;
            luma[off + 10] = (luma[off + 8] + luma[off + 12] + 1) >> 1;
            luma[off + 14] = (luma[off + 12] + rightPixY[j] + 1) >> 1;

            luma[off + 0] = (leftPixY[j] + luma[off + 1] + 1) >> 1;
            luma[off + 3] = (luma[off + 2] + luma[off + 4] + 1) >> 1;
            luma[off + 5] = (luma[off + 4] + luma[off + 6] + 1) >> 1;
            luma[off + 7] = (luma[off + 6] + luma[off + 8] + 1) >> 1;
            luma[off + 9] = (luma[off + 8] + luma[off + 10] + 1) >> 1;
            luma[off + 11] = (luma[off + 10] + luma[off + 12] + 1) >> 1;
            luma[off + 13] = (luma[off + 12] + luma[off + 14] + 1) >> 1;
            luma[off + 15] = (luma[off + 14] + rightPixY[j] + 1) >> 1;
        }
        return luma;
    }

    private int[] paintLumaVert(int[] topPixY, int[] bottomPixY) {
        int[] luma = new int[256];

        for (int i = 0; i < 16; i++) {
            luma[i + 128] = (topPixY[i] + bottomPixY[i] + 1) >> 1;
            luma[i + 192] = (luma[i + 128] + bottomPixY[i] + 1) >> 1;
            luma[i + 64] = (luma[i + 128] + topPixY[i] + 1) >> 1;

            luma[i + 16] = luma[i + 32] = (topPixY[i] + luma[i + 64] + 1) >> 1;
            luma[i + 96] = (luma[i + 64] + luma[i + 128] + 1) >> 1;
            luma[i + 160] = (luma[i + 128] + luma[i + 192] + 1) >> 1;
            luma[i + 224] = (luma[i + 192] + bottomPixY[i] + 1) >> 1;

            luma[i] = (topPixY[i] + luma[i + 16] + 1) >> 1;
            luma[i + 48] = (luma[i + 32] + luma[i + 64] + 1) >> 1;
            luma[i + 80] = (luma[i + 64] + luma[i + 96] + 1) >> 1;
            luma[i + 112] = (luma[i + 96] + luma[i + 128] + 1) >> 1;
            luma[i + 144] = (luma[i + 128] + luma[i + 160] + 1) >> 1;
            luma[i + 176] = (luma[i + 160] + luma[i + 192] + 1) >> 1;
            luma[i + 208] = (luma[i + 192] + luma[i + 224] + 1) >> 1;
            luma[i + 240] = (luma[i + 224] + bottomPixY[i] + 1) >> 1;
        }
        return luma;
    }

    private int[] paintLumaHor(int[] leftPixY) {
        int[] luma = new int[256];

        for (int j = 0; j < 16; j++) {
            for (int i = 0; i < 16; i++) {
                luma[(j << 4) + i] = leftPixY[j];
            }
        }
        return luma;
    }

    private int[] paintLumaVer(int[] topPixY) {
        int[] luma = new int[256];

        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                luma[(j << 4) + i] = topPixY[i];
            }
        }
        return luma;
    }

    private int[] paintLuma3SidesLTB(int[] leftPixY, int[] topPixY, int[] bottomPixY) {

        int[] one = paintLumaVert(topPixY, bottomPixY);

        int[] right = new int[16];
        for (int j = 0; j < 16; j++)
            right[j] = one[(j << 4) + 15];

        int[] two = paintLumaHor(leftPixY, right);

        for (int i = 0; i < 256; i++) {
            one[i] = (one[i] + two[i] + 1) >> 1;
        }

        return one;

    }

    private int[] paintLuma3SidesTRB(int[] topPixY, int[] rightPixY, int[] bottomPixY) {
        int[] one = paintLumaVert(topPixY, bottomPixY);

        int[] left = new int[16];
        for (int j = 0; j < 16; j++)
            left[j] = one[j << 4];

        int[] two = paintLumaHor(left, rightPixY);

        for (int i = 0; i < 256; i++) {
            one[i] = (one[i] + two[i] + 1) >> 1;
        }

        return one;

    }

    private int[] paintLuma3SidesLTR(int[] leftPixY, int[] topPixY, int[] rightPixY) {

        int[] one = paintLumaHor(leftPixY, rightPixY);

        int[] bottom = new int[16];
        for (int i = 0; i < 16; i++)
            bottom[i] = one[240 + i];

        int[] two = paintLumaVert(topPixY, bottom);

        for (int i = 0; i < 256; i++) {
            one[i] = (one[i] + two[i] + 1) >> 1;
        }

        return one;
    }

    private int[] paintLuma3SidesLRB(int[] leftPixY, int[] rightPixY, int[] bottomPixY) {
        int[] one = paintLumaHor(leftPixY, rightPixY);

        int[] two = paintLumaVert(one, bottomPixY);

        for (int i = 0; i < 256; i++) {
            one[i] = (one[i] + two[i] + 1) >> 1;
        }

        return one;
    }

    private int[] paintLuma4Sides(int[] leftPixY, int[] topPixY, int[] rightPixY, int[] bottomPixY) {
        int[] one = paintLumaHor(leftPixY, rightPixY);
        int[] two = paintLumaVert(topPixY, bottomPixY);

        for (int i = 0; i < 256; i++) {
            one[i] = (one[i] + two[i] + 1) >> 1;
        }

        return one;
    }

    private int[] paintChroma(int[] left, int[] top, int[] right, int[] bottom) {
        int[] result = new int[64];

        int sum = 0;
        int count = 0;

        if (left != null) {
            for (int i = 0; i < 8; i++)
                sum += left[i];
            count += 8;
        }

        if (top != null) {
            for (int i = 0; i < 8; i++)
                sum += top[i];
            count += 8;
        }

        if (right != null) {
            for (int i = 0; i < 8; i++)
                sum += right[i];
            count += 8;
        }

        if (bottom != null) {
            for (int i = 0; i < 8; i++)
                sum += bottom[i];
            count += 8;
        }

        int val = sum / count;

        for (int i = 0; i < 64; i++)
            result[i] = val;

        return result;
    }

    private int[] paint2SideLuma(int[] sideA, int[] sideB) {
        int[] one = paintLumaHor(sideA);
        int[] two = paintLumaVer(sideB);

        for (int i = 0; i < 256; i++) {
            one[i] = (one[i] + two[i] + 1) >> 1;
        }

        return one;
    }

    public static void savePGM(String string, int[] y, int width, int height) {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(string));

            PrintStream ps = new PrintStream(out);
            ps.println("P5");
            ps.println(width + " " + height);
            ps.println("255");
            ps.flush();

            for (int i = 0; i < y.length; i++)
                out.write(y[i]);

            out.flush();

        } catch (IOException e) {
            IOUtils.closeQuietly(out);
        }
    }
}
