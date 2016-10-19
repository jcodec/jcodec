package org.jcodec.scale;

import org.jcodec.common.model.Size;

/**
 * Resamples image interpolating points using bicubic filter.
 * 
 * The difference from Lanczoc resampler is that a separate set of taps are
 * generated for each destination point position.
 * 
 * @author Stanislav Vitvitskiy
 */
public class BicubicResampler extends BaseResampler {
    private short[][] horizontalTaps;
    private short[][] verticalTaps;
    private static double alpha = 0.6;

    public BicubicResampler(Size from, Size to) {
        super(from, to);
        horizontalTaps = buildFilterTaps(to.getWidth(), from.getWidth());
        verticalTaps = buildFilterTaps(to.getHeight(), from.getHeight());
    }

    private static short[][] buildFilterTaps(int to, int from) {
        double[] taps = new double[4];
        short[][] tapsOut = new short[to][4];
        double ratio = (double)from / to;
        double toByFrom = (double) to / from;
        double srcPos = 0;
        for (int i = 0; i < to; i++) {
            double fraction = srcPos - (int) srcPos;
            for (int t = -1; t < 3; t++) {
                double d = t - fraction;
                if (to < from) {
                    d *= toByFrom;
                }
                double x = Math.abs(d);
                double xx = x * x;
                double xxx = xx * x;
                if (d >= -1 && d <= 1) {
                    taps[t+1] = (2 - alpha) * xxx + (-3 + alpha) * xx + 1;
                } else if (d < -2 || d > 2) {
                    taps[t+1] = 0;
                } else {
                    taps[t+1] = -alpha * xxx + 5 * alpha * xx - 8 * alpha * x + 4 * alpha;
                }
            }
            normalizeAndGenerateFixedPrecision(taps, 7, tapsOut[i]);
            srcPos += ratio;
        }
        return tapsOut;
    }

    @Override
    protected short[] getTapsX(int dstX) {
        return horizontalTaps[dstX];
    }

    @Override
    protected short[] getTapsY(int dstY) {
        return verticalTaps[dstY];
    }

    @Override
    protected int nTaps() {
        return 4;
    }
}
