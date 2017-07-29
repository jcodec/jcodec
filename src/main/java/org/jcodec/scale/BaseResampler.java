package org.jcodec.scale;

import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;
import org.jcodec.common.tools.MathUtil;

/**
 * Resamples image interpolating points using Lanczos sinc over sine windowed
 * filter.
 * 
 * @author Stanislav Vitvitskiy
 */
public abstract class BaseResampler {
    private ThreadLocal<int[]> tempBuffers = new ThreadLocal<int[]>();
    private Size toSize;
    private Size fromSize;
    private double scaleFactorX;
    private double scaleFactorY;

    public BaseResampler(Size from, Size to) {
        this.toSize = to;
        this.fromSize = from;
        scaleFactorX = (double) from.getWidth() / to.getWidth();
        scaleFactorY = (double) from.getHeight() / to.getHeight();
    }

    byte getPel(Picture pic, int plane, int x, int y) {
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

    protected abstract short[] getTapsX(int dstX);

    protected abstract short[] getTapsY(int dstY);

    protected abstract int nTaps();

    /**
     * Converts floating point taps to fixed precision taps.
     * 
     * @param taps
     *            The 64 bit double representation
     * @param precBits
     *            Precision bits
     * @param out
     *            Taps converted to fixed precision
     */
    public static void normalizeAndGenerateFixedPrecision(double[] taps, int precBits, short[] out) {
        double sum = 0;
        for (int i = 0; i < taps.length; i++) {
            sum += taps[i];
        }
        int sumFix = 0;
        int precNum = 1 << precBits;
        for (int i = 0; i < taps.length; i++) {
            double d = (taps[i] * precNum) / sum + precNum;
            int s = (int) d;
            taps[i] = d - s;
            out[i] = (short) (s - precNum);
            sumFix += out[i];
        }
        long tapsTaken = 0;
        while (sumFix < precNum) {
            int maxI = -1;
            for (int i = 0; i < taps.length; i++) {
                if ((tapsTaken & (1 << i)) == 0 && (maxI == -1 || taps[i] > taps[maxI]))
                    maxI = i;
            }
            out[maxI]++;
            sumFix++;
            tapsTaken |= (1 << maxI);
        }

        for (int i = 0; i < taps.length; i++) {
            taps[i] += out[i];
            if ((tapsTaken & (1 << i)) != 0)
                taps[i] -= 1;
        }
    }

    /**
     * Interpolates points using a 2d convolution
     */
    public void resample(Picture in, Picture out) {
        int[] temp = tempBuffers.get();
        if (temp == null) {
            temp = new int[toSize.getWidth() * (fromSize.getHeight() + nTaps())];
            tempBuffers.set(temp);
        }
        for (int p = 0; p < in.getColor().nComp; p++) {
            // Horizontal pass
            for (int y = 0; y < in.getPlaneHeight(p) + nTaps(); y++) {
                for (int x = 0; x < out.getPlaneWidth(p); x++) {
                    short[] tapsXs = getTapsX(x);
                    int srcX = (int) (scaleFactorX * x) - nTaps() / 2 + 1;

                    int sum = 0;
                    for (int i = 0; i < nTaps(); i++) {
                        sum += (getPel(in, p, srcX + i, y - nTaps() / 2 + 1) + 128) * tapsXs[i];
                    }
                    temp[y * toSize.getWidth() + x] = sum;
                }
            }

            // Vertical pass
            for (int y = 0; y < out.getPlaneHeight(p); y++) {
                for (int x = 0; x < out.getPlaneWidth(p); x++) {
                    short[] tapsYs = getTapsY(y);
                    int srcY = (int) (scaleFactorY * y);

                    int sum = 0;
                    for (int i = 0; i < nTaps(); i++) {
                        sum += temp[x + (srcY + i) * toSize.getWidth()] * tapsYs[i];
                    }
                    out.getPlaneData(p)[y * out.getPlaneWidth(p) + x] = (byte) (MathUtil.clip((sum + 8192) >> 14, 0,
                            255) - 128);
                }
            }
        }
    }
}