package org.jcodec.codecs.vpx;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class VPXDCT {

    public static void fdct4x4(int[] coef) {
        for (int i = 0; i < 16; i += 4) {
            int a1 = ((coef[i] + coef[i + 3]) << 3);
            int b1 = ((coef[i + 1] + coef[i + 2]) << 3);
            int c1 = ((coef[i + 1] - coef[i + 2]) << 3);
            int d1 = ((coef[i] - coef[i + 3]) << 3);

            coef[i] = a1 + b1;
            coef[i + 2] = a1 - b1;

            coef[i + 1] = (c1 * 2217 + d1 * 5352 + 14500) >> 12;
            coef[i + 3] = (d1 * 2217 - c1 * 5352 + 7500) >> 12;
        }

        for (int i = 0; i < 4; i++) {
            int a1 = coef[i] + coef[i + 12];
            int b1 = coef[i + 4] + coef[i + 8];
            int c1 = coef[i + 4] - coef[i + 8];
            int d1 = coef[i] - coef[i + 12];

            coef[i] = (a1 + b1 + 7) >> 4;
            coef[i + 8] = (a1 - b1 + 7) >> 4;

            coef[i + 4] = ((c1 * 2217 + d1 * 5352 + 12000) >> 16) + (d1 != 0 ? 1 : 0);
            coef[i + 12] = (d1 * 2217 - c1 * 5352 + 51000) >> 16;
        }
    }

    public static void walsh4x4(int[] coef) {
        for (int i = 0; i < 16; i += 4) {
            int a1 = ((coef[i] + coef[i + 2]) << 2);
            int d1 = ((coef[i + 1] + coef[i + 3]) << 2);
            int c1 = ((coef[i + 1] - coef[i + 3]) << 2);
            int b1 = ((coef[i] - coef[i + 2]) << 2);

            coef[i] = a1 + d1 + (a1 != 0 ? 1 : 0);
            coef[i + 1] = b1 + c1;
            coef[i + 2] = b1 - c1;
            coef[i + 3] = a1 - d1;
        }

        for (int i = 0; i < 4; i++) {
            int a1 = coef[i] + coef[i + 8];
            int d1 = coef[i + 4] + coef[i + 12];
            int c1 = coef[i + 4] - coef[i + 12];
            int b1 = coef[i] - coef[i + 8];

            int a2 = a1 + d1;
            int b2 = b1 + c1;
            int c2 = b1 - c1;
            int d2 = a1 - d1;

            a2 += a2 < 0 ? 1 : 0;
            b2 += b2 < 0 ? 1 : 0;
            c2 += c2 < 0 ? 1 : 0;
            d2 += d2 < 0 ? 1 : 0;

            coef[i] = (a2 + 3) >> 3;
            coef[i + 4] = (b2 + 3) >> 3;
            coef[i + 8] = (c2 + 3) >> 3;
            coef[i + 12] = (d2 + 3) >> 3;
        }
    }

    public static int cospi8sqrt2minus1 = 20091;
    public static int sinpi8sqrt2 = 35468;

    public static void idct4x4(int[] coef) {

        for (int i = 0; i < 4; i++) {
            int a1 = coef[i] + coef[i + 8];
            int b1 = coef[i] - coef[i + 8];

            int temp1 = (coef[i + 4] * sinpi8sqrt2) >> 16;
            int temp2 = coef[i + 12] + ((coef[i + 12] * cospi8sqrt2minus1) >> 16);
            int c1 = temp1 - temp2;

            temp1 = coef[i + 4] + ((coef[i + 4] * cospi8sqrt2minus1) >> 16);
            temp2 = (coef[i + 12] * sinpi8sqrt2) >> 16;
            int d1 = temp1 + temp2;

            coef[i] = a1 + d1;
            coef[i + 12] = a1 - d1;

            coef[i + 4] = b1 + c1;
            coef[i + 8] = b1 - c1;
        }

        for (int i = 0; i < 16; i += 4) {
            int a1 = coef[i] + coef[i + 2];
            int b1 = coef[i] - coef[i + 2];

            int temp1 = (coef[i + 1] * sinpi8sqrt2) >> 16;
            int temp2 = coef[i + 3] + ((coef[i + 3] * cospi8sqrt2minus1) >> 16);
            int c1 = temp1 - temp2;

            temp1 = coef[i + 1] + ((coef[i + 1] * cospi8sqrt2minus1) >> 16);
            temp2 = (coef[i + 3] * sinpi8sqrt2) >> 16;
            int d1 = temp1 + temp2;

            coef[i] = (a1 + d1 + 4) >> 3;
            coef[i + 3] = (a1 - d1 + 4) >> 3;

            coef[i + 1] = (b1 + c1 + 4) >> 3;
            coef[i + 2] = (b1 - c1 + 4) >> 3;

        }
    }

    public static void iwalsh4x4(int[] coef) {

        for (int i = 0; i < 4; i++) {
            int a1 = coef[i] + coef[i + 12];
            int b1 = coef[i + 4] + coef[i + 8];
            int c1 = coef[i + 4] - coef[i + 8];
            int d1 = coef[i] - coef[i + 12];

            coef[i] = a1 + b1;
            coef[i + 4] = c1 + d1;
            coef[i + 8] = a1 - b1;
            coef[i + 12] = d1 - c1;
        }

        for (int i = 0; i < 16; i += 4) {
            int a1 = coef[i] + coef[i + 3];
            int b1 = coef[i + 1] + coef[i + 2];
            int c1 = coef[i + 1] - coef[i + 2];
            int d1 = coef[i] - coef[i + 3];

            int a2 = a1 + b1;
            int b2 = c1 + d1;
            int c2 = a1 - b1;
            int d2 = d1 - c1;

            coef[i] = (a2 + 3) >> 3;
            coef[i + 1] = (b2 + 3) >> 3;
            coef[i + 2] = (c2 + 3) >> 3;
            coef[i + 3] = (d2 + 3) >> 3;

        }
    }
}