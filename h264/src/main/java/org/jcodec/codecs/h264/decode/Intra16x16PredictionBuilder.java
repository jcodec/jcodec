package org.jcodec.codecs.h264.decode;

import static org.jcodec.codecs.h264.H264Const.LUMA_4x4_BLOCK_LUT;
import static org.jcodec.codecs.h264.H264Const.LUMA_4x4_POS_LUT;
import static org.jcodec.common.tools.MathUtil.clip;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Prediction builder class for intra 16x16 coded macroblocks
 * 
 * 
 * @author The JCodec project
 * 
 */
public class Intra16x16PredictionBuilder {
    public static void predictWithMode(int predMode, int[][] residual, boolean leftAvailable, boolean topAvailable,
            byte[] leftRow, byte[] topLine, byte[] topLeft, int x, byte[] pixOut) {
        switch (predMode) {
        case 0:
            predictVertical(residual, topAvailable, topLine, x, pixOut);
            break;
        case 1:
            predictHorizontal(residual, leftAvailable, leftRow, x, pixOut);
            break;
        case 2:
            predictDC(residual, leftAvailable, topAvailable, leftRow, topLine, x, pixOut);
            break;
        case 3:
            predictPlane(residual, leftAvailable, topAvailable, leftRow, topLine, topLeft, x, pixOut);
            break;
        }

    }

    public static void predictVertical(int[][] residual, boolean topAvailable, byte[] topLine, int x, byte[] pixOut) {
        int off = 0;
        for (int j = 0; j < 16; j++) {
            for (int i = 0; i < 16; i++, off++)
                pixOut[off] = (byte) clip(residual[LUMA_4x4_BLOCK_LUT[off]][LUMA_4x4_POS_LUT[off]] + topLine[x + i], -128, 127);
        }
    }

    public static void predictHorizontal(int[][] residual, boolean leftAvailable, byte[] leftRow, int x, byte[] pixOut) {
        int off = 0;
        for (int j = 0; j < 16; j++) {
            for (int i = 0; i < 16; i++, off++)
                pixOut[off] = (byte) clip(residual[LUMA_4x4_BLOCK_LUT[off]][LUMA_4x4_POS_LUT[off]] + leftRow[j], -128, 127);
        }
    }

    public static void predictDC(int[][] residual, boolean leftAvailable, boolean topAvailable, byte[] leftRow,
            byte[] topLine, int x, byte[] pixOut) {
        int s0;
        if (leftAvailable && topAvailable) {
            s0 = 0;
            for (int i = 0; i < 16; i++)
                s0 += leftRow[i];
            for (int i = 0; i < 16; i++)
                s0 += topLine[x + i];

            s0 = (s0 + 16) >> 5;
        } else if (leftAvailable) {
            s0 = 0;
            for (int i = 0; i < 16; i++)
                s0 += leftRow[i];
            s0 = (s0 + 8) >> 4;
        } else if (topAvailable) {
            s0 = 0;
            for (int i = 0; i < 16; i++)
                s0 += topLine[x + i];
            s0 = (s0 + 8) >> 4;
        } else {
            s0 = 0;
        }

        for (int i = 0; i < 256; i++)
            pixOut[i] = (byte) clip(residual[LUMA_4x4_BLOCK_LUT[i]][LUMA_4x4_POS_LUT[i]] + s0, -128, 127);
    }

    public static void predictPlane(int[][] residual, boolean leftAvailable, boolean topAvailable, byte[] leftRow,
            byte[] topLine, byte[] topLeft, int x, byte[] pixOut) {
        int H = 0;

        for (int i = 0; i < 7; i++) {
            H += (i + 1) * (topLine[x + 8 + i] - topLine[x + 6 - i]);
        }
        H += 8 * (topLine[x + 15] - topLeft[0]);

        int V = 0;
        for (int j = 0; j < 7; j++) {
            V += (j + 1) * (leftRow[8 + j] - leftRow[6 - j]);
        }
        V += 8 * (leftRow[15] - topLeft[0]);

        int c = (5 * V + 32) >> 6;
        int b = (5 * H + 32) >> 6;
        int a = 16 * (leftRow[15] + topLine[x + 15]);

        int off = 0;
        for (int j = 0; j < 16; j++) {
            for (int i = 0; i < 16; i++, off++) {
                int val = clip((a + b * (i - 7) + c * (j - 7) + 16) >> 5, -128, 127);
                pixOut[off] = (byte) clip(residual[LUMA_4x4_BLOCK_LUT[off]][LUMA_4x4_POS_LUT[off]] + val, -128, 127);
            }
        }
    }
}