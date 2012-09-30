package org.jcodec.common.dct;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;

import org.jcodec.codecs.mjpeg.ImageConvert;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author Jay Codec
 *
 */
public class SlowDCT extends DCT {
    public static final SlowDCT INSTANCE = new SlowDCT();
    /** r - Reciprocal */
    private static final double rSqrt2 = 1 / sqrt(2);

    public short[] encode(byte[] orig) {
        short[] result = new short[64];
        for (int u = 0; u < 8; u++) {
            for (int v = 0; v < 8; v++) {
                float sum = 0;
                for (int i = 0; i < 8; i++) {
                    for (int j = 0; j < 8; j++) {
                        sum += (float) orig[i * 8 + j]
                                * Math.cos((Math.PI / 8) * (i + 0.5) * u)
                                * Math.cos((Math.PI / 8) * (j + 0.5) * v);
                    }
                }
                result[u * 8 + v] = (byte) sum;
            }
        }

        result[0] = (byte) ((float) result[0] / 8);
        double sqrt2 = Math.sqrt(2);
        for (int i = 1; i < 8; i++) {
            result[i] = (byte) ((float) result[0] * sqrt2 / 8);
            result[i * 8] = (byte) ((float) result[0] * sqrt2 / 8);

            for (int j = 1; j < 8; j++) {
                result[i * 8 + j] = (byte) ((float) result[0] / 4);
            }
        }

        return result;
    }

    public int[] decode(int[] orig) {
        int res[] = new int[64];
        int i = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                double sum = 0;
                int pixOffset = 0;
                for (int u = 0; u < 8; u++) {
                    double cu = (u == 0) ? rSqrt2 : 1;
                    for (int v = 0; v < 8; v++) {
                        double cv = (v == 0) ? rSqrt2 : 1;
                        double svu = orig[pixOffset];
                        double c1 = ((2 * x + 1) * v * PI) / 16.;
                        double c2 = ((2 * y + 1) * u * PI) / 16.;
                        sum += cu * cv * svu * cos(c1) * cos(c2);
                        pixOffset++;
                    }
                }
                sum *= 0.25;
                sum = round(sum + 128);
                int isum = ((int) sum);
                res[i++] = ImageConvert.icrop(isum);
            }
        }
        return res;
    }

}