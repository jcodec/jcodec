package org.jcodec.algo;

import java.util.Arrays;

/**
 * 
 * Base for all filters that use convolution with kernel
 * 
 * @author Jay Codec
 * 
 */
public abstract class ConvolutionFilter implements AudioFilter {

    private double[] kernel;
    private float[] prev;

    protected abstract double[] buildKernel();

    @Override
    public int filter(float[] in, int max, float[] out) {
        if (kernel == null) {
            kernel = buildKernel();
        }

        int sample = 0;
        if (prev != null) {
            for (int i = 0; i < kernel.length / 2; i++) {
                double result = 0;
                for (int j = 0; j < kernel.length; j++) {
                    result += kernel[j] * safeGetP(in, prev.length - kernel.length + i + j);
                }
                out[sample++] = (float) result;
            }
        }
        if (prev == null)
            prev = new float[kernel.length / 2];

        for (int i = 0; i < max - kernel.length / 2 && sample < out.length; i++) {
            double result = 0;
            for (int j = 0; j < kernel.length; j++) {
                result += kernel[j] * safeGet(in, i + j - kernel.length / 2);
            }
            out[sample++] = (float) result;
        }

        prev = Arrays.copyOf(in, max);

        return sample;
    }

    private double safeGetP(float[] samples, int pos) {
        return pos < prev.length ? prev[pos] : samples[pos - prev.length];
    }

    private double safeGet(float[] samples, int pos) {
        return pos < 0 ? prev[prev.length + pos] : samples[pos];
    }
}
