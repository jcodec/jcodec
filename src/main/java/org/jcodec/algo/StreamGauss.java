package org.jcodec.algo;

import org.junit.Assert;

/**
 * A filter which applies Gaussian blur to an image. This is a subclass of
 * ConvolveFilter which simply creates a kernel with a Gaussian distribution for
 * blurring.
 * 
 * @author The JCodec project
 * @author Jerry Huxtable
 */
public class StreamGauss {

    private int[] vect;

    public StreamGauss(float radius) {
        makeKernel(radius);
    }

    void gauss(int[] samples, int[] out) {
        for (int i = 0; i < samples.length - vect.length; i++) {
            int val = 0;
            for (int j = 0; j < vect.length; j++) {
                val += samples[i + j] * vect[j];
            }
            int v = val >> 10;
            Assert.assertTrue(v > 0);
            out[i] = v;
        }
    }

    /**
     * Make a Gaussian blur kernel. fixed point, 10 bit precision
     */
    public void makeKernel(float radius) {
        int r = (int) Math.ceil(radius);
        int rows = r * 2 + 1;
        vect = new int[rows];

        float sigma = radius / 3;
        float sigma22 = 2 * sigma * sigma;
        float sigmaPi2 = 2 * (float) Math.PI * sigma;
        float sqrtSigmaPi2 = (float) Math.sqrt(sigmaPi2);
        float radius2 = radius * radius;
        int total = 0;
        int index = 0;
        for (int row = -r; row <= r; row++) {
            float distance = row * row;
            if (distance > radius2)
                vect[index] = 0;
            else
                vect[index] = (int) ((Math.exp(-(distance) / sigma22) / sqrtSigmaPi2) * 1024);

            total += vect[index];
            index++;
        }
        for (int i = 0; i < rows; i++)
            vect[i] = (vect[i] << 10) / total;
    }
}