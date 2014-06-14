package org.jcodec.algo;

import java.util.Arrays;

/**
 * Lanczos resampler
 * 
 * @author Jay Codec
 * 
 */
public class LanczosInterpolator implements AudioFilter {

    public static double lanczos(double x, int a) {
        return x < -a ? 0 : x > a ? 0 : (a * Math.sin(Math.PI * x) * Math.sin(Math.PI * x / a))
                / (Math.PI * Math.PI * x * x);
    }

    private float[] p;
    private int stoppedAt;
    private double phase;

    private double rateStep;

    public LanczosInterpolator(int fromRate, int toRate) {
        rateStep = (double) fromRate / toRate;
    }

    public float safeGet(float[] vals, int idx) {
        return idx < 0 ? p[p.length + idx] : vals[idx];
    }

    public float safeGetP(float[] vals, int idx) {
        return idx > p.length - 1 ? vals[idx - p.length] : p[idx];
    }

    public int filter(float[] values, int max, float[] out) {

        int sample = 0;

        if (p != null) {
            for (int i = stoppedAt;; i++) {
                double time = i * rateStep + phase;

                int p0i = (int) Math.floor(time);
                int q0i = (int) Math.ceil(time);

                double p0d = p0i - time;

                if (p0i > max - 1) {
                    phase = time - max;
                    break;
                }

                if (p0d < -.001) {
                    double q0d = q0i - time;

                    double p0c = lanczos(p0d, 3);
                    double q0c = lanczos(q0d, 3);

                    double p1c = lanczos(p0d - 1, 3);
                    double q1c = lanczos(q0d + 1, 3);

                    double p2c = lanczos(p0d - 2, 3);
                    double q2c = lanczos(q0d + 2, 3);

                    double factor = 1d / (p0c + p1c + p2c + q0c + q1c + q2c);

                    out[sample++] = (float) ((safeGetP(values, q0i) * q0c + safeGetP(values, q0i + 1) * q1c
                            + safeGetP(values, q0i + 2) * q2c + safeGetP(values, p0i) * p0c + safeGetP(values, p0i - 1)
                            * p1c + safeGetP(values, p0i - 2) * p2c) * factor);
                } else {
                    out[sample++] = safeGetP(values, q0i);
                }
            }
        } else
            p = new float[3];

        int i = 0;
        for (; sample < out.length; i++) {
            double time = i * rateStep + phase;

            int p0i = (int) Math.floor(time);
            int q0i = (int) Math.ceil(time);

            if (q0i > max - 3) {
                break;
            }

            double p0d = p0i - time;
            if (p0d < -.001) {
                double q0d = q0i - time;

                double p0c = lanczos(p0d, 3);
                double q0c = lanczos(q0d, 3);

                double p1c = lanczos(p0d - 1, 3);
                double q1c = lanczos(q0d + 1, 3);

                double p2c = lanczos(p0d - 2, 3);
                double q2c = lanczos(q0d + 2, 3);

                double factor = 1d / (p0c + p1c + p2c + q0c + q1c + q2c);

                out[sample++] = (float) ((safeGet(values, q0i) * q0c + safeGet(values, q0i + 1) * q1c
                        + safeGet(values, q0i + 2) * q2c + safeGet(values, p0i) * p0c + safeGet(values, p0i - 1) * p1c + safeGet(
                        values, p0i - 2) * p2c) * factor);
            } else {
                out[sample++] = values[p0i];
            }
        }
        stoppedAt = i;

        p = Arrays.copyOf(values, max);

        return sample;
    }
}
