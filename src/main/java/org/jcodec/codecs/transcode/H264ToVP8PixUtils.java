package org.jcodec.codecs.transcode;

import static org.jcodec.common.tools.MathUtil.clamp;

public class H264ToVP8PixUtils {
    public static void renderChromaH264(int[] out, int[] coeffs, int[] pred, int blkIndX, int blkIndY, int[] vp8Pred,
            int predStride) {
        int blkX = blkIndX << 2, blkY = blkIndY << 2;
        for (int line = 0, srcOff = 0, dstOff = (blkY << 3) + blkX, offVP8 = blkY * predStride + blkX; line < 4; line++, offVP8 += predStride) {
            int a = clamp(coeffs[srcOff] + pred[dstOff]);
            int b = clamp(coeffs[srcOff + 1] + pred[dstOff + 1]);
            int c = clamp(coeffs[srcOff + 2] + pred[dstOff + 2]);
            int d = clamp(coeffs[srcOff + 3] + pred[dstOff + 3]);

            coeffs[srcOff] = a - vp8Pred[offVP8];
            coeffs[srcOff + 1] = b - vp8Pred[offVP8 + 1];
            coeffs[srcOff + 2] = c - vp8Pred[offVP8 + 2];
            coeffs[srcOff + 3] = d - vp8Pred[offVP8 + 3];

            out[dstOff] = a;
            out[dstOff + 1] = b;
            out[dstOff + 2] = c;
            out[dstOff + 3] = d;

            srcOff += 4;
            dstOff += 8;
        }
    }

    public static void renderH264Luma(int[] out, int[] coeffs, int[] predH264, int[] predVP8, int blkIndX, int blkIndY,
            int vp8Stride) {
        int blkX = blkIndX << 2, blkY = blkIndY << 2;

        for (int line = 0, srcOff = 0, dstOff = (blkY << 4) + blkX, offVP8 = blkY * vp8Stride + blkX; line < 4; line++, offVP8 += vp8Stride) {
            int a = clamp(coeffs[srcOff] + predH264[dstOff]);
            int b = clamp(coeffs[srcOff + 1] + predH264[dstOff + 1]);
            int c = clamp(coeffs[srcOff + 2] + predH264[dstOff + 2]);
            int d = clamp(coeffs[srcOff + 3] + predH264[dstOff + 3]);

            coeffs[srcOff] = a - predVP8[offVP8];
            coeffs[srcOff + 1] = b - predVP8[offVP8 + 1];
            coeffs[srcOff + 2] = c - predVP8[offVP8 + 2];
            coeffs[srcOff + 3] = d - predVP8[offVP8 + 3];

            out[dstOff] = a;
            out[dstOff + 1] = b;
            out[dstOff + 2] = c;
            out[dstOff + 3] = d;

            srcOff += 4;
            dstOff += 16;
        }
    }

    public static void map4x4(int[] out264, int[] predH264, int[] ac, int blkOffLeft, int blkOffTop) {
        for (int i = 0, off = (blkOffTop << 4) + blkOffLeft; i < 16; i += 4, off += 16) {
            out264[off] = clamp(ac[i] + predH264[i]);
            out264[off + 1] = clamp(ac[i + 1] + predH264[i + 1]);
            out264[off + 2] = clamp(ac[i + 2] + predH264[i + 2]);
            out264[off + 3] = clamp(ac[i + 3] + predH264[i + 3]);
        }
    }

    public static void mapSwapPred4x4(int[] out264, int[] ac, int[] predH264, int[] predVP8, int blkOffLeft,
            int blkOffTop) {
        for (int i = 0, off = (blkOffTop << 4) + blkOffLeft; i < 16; i += 4, off += 16) {
            int a = clamp(ac[i] + predH264[i]);
            int b = clamp(ac[i + 1] + predH264[i + 1]);
            int c = clamp(ac[i + 2] + predH264[i + 2]);
            int d = clamp(ac[i + 3] + predH264[i + 3]);

            ac[i] = a - predVP8[i];
            ac[i + 1] = b - predVP8[i + 1];
            ac[i + 2] = c - predVP8[i + 2];
            ac[i + 3] = d - predVP8[i + 3];

            out264[off] = a;
            out264[off + 1] = b;
            out264[off + 2] = c;
            out264[off + 3] = d;
        }
    }

    public static void map8x8(int[] pic, int stride, int mbX, int mbY, int[] out264) {
        for (int i = 0, off = (mbY << 3) * stride + (mbX << 3), mbo = 0; i < 8; i++, off += stride, mbo += 8) {
            pic[off] = out264[mbo];
            pic[off + 1] = out264[mbo + 1];
            pic[off + 2] = out264[mbo + 2];
            pic[off + 3] = out264[mbo + 3];

            pic[off + 4] = out264[mbo + 4];
            pic[off + 5] = out264[mbo + 5];
            pic[off + 6] = out264[mbo + 6];
            pic[off + 7] = out264[mbo + 7];
        }
    }

    public static void collectPred4x4(int[] ac, int[] pred, int[] leftRow, int[] topLine, int[] topLeft, int x, int y) {
        topLeft[y] = topLine[x + 3];

        topLeft[y + 1] = leftRow[y] = clamp(ac[3] + pred[3]);
        topLeft[y + 2] = leftRow[y + 1] = clamp(ac[7] + pred[7]);
        topLeft[y + 3] = leftRow[y + 2] = clamp(ac[11] + pred[11]);
        leftRow[y + 3] = clamp(ac[15] + pred[15]);

        topLine[x] = clamp(ac[12] + pred[12]);
        topLine[x + 1] = clamp(ac[13] + pred[13]);
        topLine[x + 2] = clamp(ac[14] + pred[14]);
        topLine[x + 3] = clamp(ac[15] + pred[15]);
    }

    public static void map16x16(int[] pic, int mbWidth, int mbAddr, int[] out264) {
        int mbX = mbAddr % mbWidth, mbY = mbAddr / mbWidth, stride = mbWidth << 4;
        for (int i = 0, off = (mbY << 4) * stride + (mbX << 4), mbo = 0; i < 16; i++, off += stride, mbo += 16) {
                pic[off] = out264[mbo];
                pic[off + 1] = out264[mbo + 1];
                pic[off + 2] = out264[mbo + 2];
                pic[off + 3] = out264[mbo + 3];
                
                pic[off + 4] = out264[mbo + 4];
                pic[off + 5] = out264[mbo + 5];
                pic[off + 6] = out264[mbo + 6];
                pic[off + 7] = out264[mbo + 7];
                
                pic[off + 8] = out264[mbo + 8];
                pic[off + 9] = out264[mbo + 9];
                pic[off + 10] = out264[mbo + 10];
                pic[off + 11] = out264[mbo + 11];
                
                pic[off + 12] = out264[mbo + 12];
                pic[off + 13] = out264[mbo + 13];
                pic[off + 14] = out264[mbo + 14];
                pic[off + 15] = out264[mbo + 15];
        }
    }
}
