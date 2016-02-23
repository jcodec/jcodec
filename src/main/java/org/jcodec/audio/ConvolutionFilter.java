package org.jcodec.audio;

import java.nio.FloatBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Base for all filters that use convolution with kernel
 * 
 * @author The JCodec project
 * 
 */
public abstract class ConvolutionFilter implements AudioFilter {

    private double[] kernel;

    protected abstract double[] buildKernel();

    @Override
    public void filter(FloatBuffer[] _in, long[] pos, FloatBuffer[] out) {
        if (_in.length != 1)
            throw new IllegalArgumentException(this.getClass().getName()
                    + " filter is designed to work only on one input");
        if (out.length != 1)
            throw new IllegalArgumentException(this.getClass().getName()
                    + " filter is designed to work only on one output");

        FloatBuffer in0 = _in[0];
        FloatBuffer out0 = out[0];

        if (kernel == null) {
            kernel = buildKernel();
        }

        if (out0.remaining() < in0.remaining() - kernel.length)
            throw new IllegalArgumentException("Output buffer is too small");
        if (in0.remaining() <= kernel.length)
            throw new IllegalArgumentException("Input buffer should contain > kernel lenght (" + kernel.length
                    + ") samples.");

        int halfKernel = kernel.length / 2;

        int i;
        for (i = in0.position() + halfKernel; i < in0.limit() - halfKernel; i++) {
            double result = 0;
            for (int j = 0; j < kernel.length; j++) {
                result += kernel[j] * in0.get(i + j - halfKernel);
            }
            out0.put((float) result);
        }
        in0.position(i - halfKernel);
    }

    @Override
    public int getDelay() {
        if (kernel == null) {
            kernel = buildKernel();
        }
        return kernel.length / 2;
    }

    @Override
    public int getNInputs() {
        return 1;
    }

    @Override
    public int getNOutputs() {
        return 1;
    }
}