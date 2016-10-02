package org.jcodec.scale;

import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Size;
import org.jcodec.common.tools.MathUtil;

/**
 * Resamples image interpolating points using Lanczos sinc over sine windowed
 * filter.
 * 
 * @author Stanislav Vitvitskiy
 */
public class LanczosResampler {
    private ThreadLocal<int[]> tempBuffers = new ThreadLocal<int[]>();
    private int nTaps = 6;
    private int precision = 256;
    private byte[][] tapsXs;
    private byte[][] tapsYs;
    private Size toSize;
    private Size fromSize;
    private double scaleFactorX;
    private double scaleFactorY;

    public LanczosResampler(Size from, Size to) {
        this.toSize = to;
        this.fromSize = from;
        scaleFactorX = (double) to.getWidth() / from.getWidth();
        scaleFactorY = (double) to.getHeight() / from.getHeight();
        tapsXs = new byte[precision][nTaps];
        tapsYs = new byte[precision][nTaps];
        buildTaps(nTaps, precision, scaleFactorX, tapsXs);
        buildTaps(nTaps, precision, scaleFactorY, tapsYs);
    }

    private static double sinc(double x) {
        return x == 0 ? 1f : (Math.sin(x) / x);
    }

    private static void buildTaps(int nTaps, int precision, double scaleFactor, byte[][] tapsOut) {
        double[] taps = new double[nTaps];
        for (int i = 0; i < precision; i++) {
            double o = (double) (i) / precision;
            float sum = 0;
            for (int j = -nTaps / 2 + 1, t = 0; j < nTaps / 2 + 1; j++, t++) {
                double x = -o + j;
                double sinc_val = scaleFactor * sinc(scaleFactor * x * Math.PI);
                double wnd_val = Math.sin((x * Math.PI) / (nTaps - 1) + Math.PI / 2);
                double val = sinc_val * wnd_val;
                taps[t] = val;
                sum += val;
            }
            // Normalize the taps
            for (int j = 0; j < nTaps; j++) {
                tapsOut[i][j] = (byte) ((taps[j] * 128) / sum);
            }
        }
    }

    byte getPel(Picture8Bit pic, int plane, int x, int y) {
        if (x < 0)
            x = 0;
        if (y < 0)
            y = 0;
        int w = pic.getPlaneWidth(plane);
        if (x > w - 1)
            x = w - 1;
        int h = pic.getPlaneHeight(plane);
        if (y > h - 1)
            y = h - 1;

        return pic.getData()[plane][x + y * w];
    }

    /**
     * Interpolates points using a 2d convolution
     */
    public void resample(Picture8Bit in, Picture8Bit out) {
        int[] temp = tempBuffers.get();
        if (temp == null) {
            temp = new int[toSize.getWidth() * (fromSize.getHeight() + nTaps)];
            tempBuffers.set(temp);
        }
        for (int p = 0; p < in.getColor().nComp; p++) {
            // Horizontal pass
            for (int y = 0; y < in.getPlaneHeight(p) + nTaps; y++) {
                for (int x = 0; x < out.getPlaneWidth(p); x++) {
                    int oi = (int) ((float) (x * precision) / scaleFactorX);
                    int full_pel = oi / precision;
                    int sub_pel = oi % precision;

                    int sum = 0;
                    for (int i = 0; i < nTaps; i++) {
                        sum += (getPel(in, p, full_pel + i - nTaps / 2 + 1, y - nTaps / 2 + 1) + 128) * tapsXs[sub_pel][i];
                    }
                    temp[y * toSize.getWidth() + x] = sum;
                }
            }

            // Vertical pass
            for (int y = 0; y < out.getPlaneHeight(p); y++) {
                for (int x = 0; x < out.getPlaneWidth(p); x++) {
                    int oy = (int) ((float) (y * precision) / scaleFactorY);
                    int full_pel = oy / precision;
                    int sub_pel = oy % precision;

                    int sum = 0;
                    for (int i = 0; i < nTaps; i++) {
                        sum += temp[x + (full_pel + i) * toSize.getWidth()] * tapsYs[sub_pel][i];
                    }
                    out.getPlaneData(p)[y * out.getPlaneWidth(p) + x] = (byte) (MathUtil.clip((sum + 8192) >> 14, 0, 255) - 128);
                }
            }
        }
    }
}