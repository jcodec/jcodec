package org.jcodec.audio;
import js.lang.IllegalArgumentException;
import js.nio.FloatBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Lanczos resampler
 * 
 * @author The JCodec project
 * 
 */
public class LanczosInterpolator implements AudioFilter {

    public static double lanczos(double x, int a) {
        return x < -a ? 0 : x > a ? 0 : (a * Math.sin(Math.PI * x) * Math.sin(Math.PI * x / a))
                / (Math.PI * Math.PI * x * x);
    }

    private double rateStep;

    public LanczosInterpolator(int fromRate, int toRate) {
        rateStep = (double) fromRate / toRate;
    }

    public void filter(FloatBuffer[] _in, long[] pos, FloatBuffer[] out) {
        if (_in.length != 1)
            throw new IllegalArgumentException(this.getClass().getName()
                    + " filter is designed to work only on one input");
        if (out.length != 1)
            throw new IllegalArgumentException(this.getClass().getName()
                    + " filter is designed to work only on one output");

        FloatBuffer in0 = _in[0];
        FloatBuffer out0 = out[0];

        if (out0.remaining() < (in0.remaining() - 6) / rateStep)
            throw new IllegalArgumentException("Output buffer is too small");
        if (in0.remaining() <= 6)
            throw new IllegalArgumentException("Input buffer should contain > 6 samples.");

        for (int outSample = 0;; outSample++) {
            double inSample = 3 + outSample * rateStep + Math.ceil(pos[0] / rateStep) * rateStep - pos[0];

            int p0i = (int) Math.floor(inSample);
            int q0i = (int) Math.ceil(inSample);
            if (p0i >= in0.limit() - 3) {
                in0.position(p0i - 3);
                break;
            }

            double p0d = p0i - inSample;
            if (p0d < -.001) {
                double q0d = q0i - inSample;

                double p0c = lanczos(p0d, 3);
                double q0c = lanczos(q0d, 3);

                double p1c = lanczos(p0d - 1, 3);
                double q1c = lanczos(q0d + 1, 3);

                double p2c = lanczos(p0d - 2, 3);
                double q2c = lanczos(q0d + 2, 3);

                double factor = 1d / (p0c + p1c + p2c + q0c + q1c + q2c);

                out0.put((float) ((in0.get(q0i) * q0c + in0.get(q0i + 1) * q1c + in0.get(q0i + 2) * q2c + in0.get(p0i)
                        * p0c + in0.get(p0i - 1) * p1c + in0.get(p0i - 2) * p2c) * factor));
            } else {
                out0.put(in0.get(p0i));
            }
        }
    }

    @Override
    public int getDelay() {
        return 3;
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