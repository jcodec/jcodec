package org.jcodec.common.dct;
import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.sqrt;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class DCTRef {

    static double[] coefficients = new double[64];

    static {
        for (int j = 0; j < 8; ++j) {
            coefficients[j] = sqrt(0.125);
            for (int i = 8; i < 64; i += 8) {
                coefficients[i + j] = 0.5 * cos(i * (j + 0.5) * PI / 64.0);
            }
        }
    }

    public static void fdct(int[] block, int off) {
        int i, j, k;
        double[] out = new double[8 * 8];

        for (i = 0; i < 64; i += 8) {
            for (j = 0; j < 8; ++j) {
                double tmp = 0;
                for (k = 0; k < 8; ++k) {
                    tmp += coefficients[i + k] * block[k * 8 + j + off];
                }
                out[i + j] = tmp * 4;
            }
        }

        for (j = 0; j < 8; ++j) {
            for (i = 0; i < 64; i += 8) {
                double tmp = 0;
                for (k = 0; k < 8; ++k) {
                    tmp += out[i + k] * coefficients[j * 8 + k];
                }
                block[i + j + off] = (int) (tmp + 0.499999999999);
            }
        }
    }

    public static void idct(int[] block, int off) {

        int i, j, k;
        double[] out = new double[8 * 8];

        /* out = block * coefficients */
        for (i = 0; i < 64; i += 8) {
            for (j = 0; j < 8; ++j) {
                double tmp = 0;
                for (k = 0; k < 8; ++k) {
                    tmp += block[i + k] * coefficients[k * 8 + j];
                }
                out[i + j] = tmp;
            }
        }

        /* block = (coefficients') * out */
        for (i = 0; i < 8; ++i) {
            for (j = 0; j < 8; ++j) {
                double tmp = 0;
                for (k = 0; k < 64; k += 8) {
                    tmp += coefficients[k + i] * out[k + j];
                }
                block[i * 8 + j] = (int) (tmp + 0.5);
            }
        }
    }
}