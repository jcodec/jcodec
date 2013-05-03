package org.jcodec.codecs.h264.decode;

import static org.jcodec.common.tools.MathUtil.clip;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Prediction builder for chroma samples
 * 
 * @author Jay Codec
 * 
 */
public class ChromaPredictionBuilder {

    public static void predictWithMode(int[] planeData, int chromaMode, int mbX, boolean leftAvailable,
            boolean topAvailable, int[] leftRow, int[] topLine, int[] topLeft) {

        switch (chromaMode) {
        case 0:
            predictDC(planeData, mbX, leftAvailable, topAvailable, leftRow, topLine);
            break;
        case 1:
            predictHorizontal(planeData, mbX, leftAvailable, leftRow);
            break;
        case 2:
            predictVertical(planeData, mbX, topAvailable, topLine);
            break;
        case 3:
            predictPlane(planeData, mbX, leftAvailable, topAvailable, leftRow, topLine, topLeft);
            break;
        }

    }

    public static void predictDC(int[] planeData, int mbX, boolean leftAvailable, boolean topAvailable, int[] leftRow,
            int[] topLine) {
        predictDCInside(planeData, 0, 0, mbX, leftAvailable, topAvailable, leftRow, topLine);
        predictDCTopBorder(planeData, 1, 0, mbX, leftAvailable, topAvailable, leftRow, topLine);
        predictDCLeftBorder(planeData, 0, 1, mbX, leftAvailable, topAvailable, leftRow, topLine);
        predictDCInside(planeData, 1, 1, mbX, leftAvailable, topAvailable, leftRow, topLine);
    }

    public static void predictVertical(int[] planeData, int mbX, boolean topAvailable, int[] topLine) {
        for (int off = 0, j = 0; j < 8; j++) {
            for (int i = 0; i < 8; i++, off++)
                planeData[off] = clip(planeData[off] + topLine[(mbX << 3) + i], 0, 255);
        }
    }

    public static void predictHorizontal(int[] planeData, int mbX, boolean leftAvailable, int[] leftRow) {
        for (int off = 0, j = 0; j < 8; j++) {
            for (int i = 0; i < 8; i++, off++)
                planeData[off] = clip(planeData[off] + leftRow[j], 0, 255);
        }
    }

    public static void predictDCInside(int[] planeData, int blkX, int blkY, int mbX, boolean leftAvailable,
            boolean topAvailable, int[] leftRow, int[] topLine) {

        int s0, blkOffX = (blkX << 2) + (mbX << 3), blkOffY = blkY << 2;

        if (leftAvailable && topAvailable) {
            s0 = 0;
            for (int i = 0; i < 4; i++)
                s0 += leftRow[i + blkOffY];
            for (int i = 0; i < 4; i++)
                s0 += topLine[blkOffX + i];

            s0 = (s0 + 4) >> 3;
        } else if (leftAvailable) {
            s0 = 0;
            for (int i = 0; i < 4; i++)
                s0 += leftRow[blkOffY + i];
            s0 = (s0 + 2) >> 2;
        } else if (topAvailable) {
            s0 = 0;
            for (int i = 0; i < 4; i++)
                s0 += topLine[blkOffX + i];
            s0 = (s0 + 2) >> 2;
        } else {
            s0 = 128;
        }

        for (int off = (blkY << 5) + (blkX << 2), j = 0; j < 4; j++, off += 8) {
            planeData[off] = clip(planeData[off] + s0, 0, 255);
            planeData[off + 1] = clip(planeData[off + 1] + s0, 0, 255);
            planeData[off + 2] = clip(planeData[off + 2] + s0, 0, 255);
            planeData[off + 3] = clip(planeData[off + 3] + s0, 0, 255);
        }
    }

    public static void predictDCTopBorder(int[] planeData, int blkX, int blkY, int mbX, boolean leftAvailable,
            boolean topAvailable, int[] leftRow, int[] topLine) {

        int s1, blkOffX = (blkX << 2) + (mbX << 3), blkOffY = blkY << 2;
        if (topAvailable) {
            s1 = 0;
            for (int i = 0; i < 4; i++)
                s1 += topLine[blkOffX + i];

            s1 = (s1 + 2) >> 2;
        } else if (leftAvailable) {
            s1 = 0;
            for (int i = 0; i < 4; i++)
                s1 += leftRow[blkOffY + i];
            s1 = (s1 + 2) >> 2;
        } else {
            s1 = 128;
        }

        for (int off = (blkY << 5) + (blkX << 2), j = 0; j < 4; j++, off += 8) {
            planeData[off] = clip(planeData[off] + s1, 0, 255);
            planeData[off + 1] = clip(planeData[off + 1] + s1, 0, 255);
            planeData[off + 2] = clip(planeData[off + 2] + s1, 0, 255);
            planeData[off + 3] = clip(planeData[off + 3] + s1, 0, 255);
        }
    }

    public static void predictDCLeftBorder(int[] planeData, int blkX, int blkY, int mbX, boolean leftAvailable,
            boolean topAvailable, int[] leftRow, int[] topLine) {

        int s2, blkOffX = (blkX << 2) + (mbX << 3), blkOffY = blkY << 2;
        if (leftAvailable) {
            s2 = 0;
            for (int i = 0; i < 4; i++)
                s2 += leftRow[blkOffY + i];
            s2 = (s2 + 2) >> 2;
        } else if (topAvailable) {
            s2 = 0;
            for (int i = 0; i < 4; i++)
                s2 += topLine[blkOffX + i];
            s2 = (s2 + 2) >> 2;
        } else {
            s2 = 128;
        }

        for (int off = (blkY << 5) + (blkX << 2), j = 0; j < 4; j++, off += 8) {
            planeData[off] = clip(planeData[off] + s2, 0, 255);
            planeData[off + 1] = clip(planeData[off + 1] + s2, 0, 255);
            planeData[off + 2] = clip(planeData[off + 2] + s2, 0, 255);
            planeData[off + 3] = clip(planeData[off + 3] + s2, 0, 255);
        }
    }

    public static void predictPlane(int[] planeData, int mbX, boolean leftAvailable, boolean topAvailable,
            int[] leftRow, int[] topLine, int[] topLeft) {
        int H = 0, blkOffX = (mbX << 3);

        for (int i = 0; i < 3; i++) {
            H += (i + 1) * (topLine[blkOffX + 4 + i] - topLine[blkOffX + 2 - i]);
        }
        H += 4 * (topLine[blkOffX + 7] - topLeft[0]);

        int V = 0;
        for (int j = 0; j < 3; j++) {
            V += (j + 1) * (leftRow[4 + j] - leftRow[2 - j]);
        }
        V += 4 * (leftRow[7] - topLeft[0]);

        int c = (34 * V + 32) >> 6;
        int b = (34 * H + 32) >> 6;
        int a = 16 * (leftRow[7] + topLine[blkOffX + 7]);

        for (int off = 0, j = 0; j < 8; j++) {
            for (int i = 0; i < 8; i++, off++) {
                int val = (a + b * (i - 3) + c * (j - 3) + 16) >> 5;
                planeData[off] = clip(planeData[off] + clip(val, 0, 255), 0, 255);
            }
        }
    }
}
