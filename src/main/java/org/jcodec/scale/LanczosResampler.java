package org.jcodec.scale;

import org.jcodec.common.model.Size;

/**
 * Resamples image interpolating points using Lanczos sinc over sine windowed
 * filter.
 * 
 * @author Stanislav Vitvitskiy
 */
public class LanczosResampler extends BaseResampler {
    //The type (or one of its parents) contains already a method called [nTaps]
    private static final int _nTaps = 6;
    private int precision = 256;
    private short[][] tapsXs;
    private short[][] tapsYs;
    private double _scaleFactorX;
    private double _scaleFactorY;

    public LanczosResampler(Size from, Size to) {
        super(from, to);
        _scaleFactorX = (double) to.getWidth() / from.getWidth();
        _scaleFactorY = (double) to.getHeight() / from.getHeight();
        tapsXs = new short[precision][_nTaps];
        tapsYs = new short[precision][_nTaps];
        buildTaps(_nTaps, precision, _scaleFactorX, tapsXs);
        buildTaps(_nTaps, precision, _scaleFactorY, tapsYs);
    }

    private static double sinc(double x) {
        return x == 0 ? 1f : (Math.sin(x) / x);
    }

    private static void buildTaps(int nTaps, int precision, double scaleFactor, short[][] tapsOut) {
        double[] taps = new double[nTaps];
        for (int i = 0; i < precision; i++) {
            double o = (double) (i) / precision;
            for (int j = -nTaps / 2 + 1, t = 0; j < nTaps / 2 + 1; j++, t++) {
                double x = -o + j;
                double sinc_val = scaleFactor * sinc(scaleFactor * x * Math.PI);
                double wnd_val = Math.sin((x * Math.PI) / (nTaps - 1) + Math.PI / 2);
                taps[t] = sinc_val * wnd_val;
            }
            normalizeAndGenerateFixedPrecision(taps, 7, tapsOut[i]);
        }
    }

    @Override
    protected short[] getTapsX(int dstX) {
        int oi = (int) ((float) (dstX * precision) / _scaleFactorX);
        int sub_pel = oi % precision;
        return tapsXs[sub_pel];
    }

    @Override
    protected short[] getTapsY(int dstY) {
        int oy = (int) ((float) (dstY * precision) / _scaleFactorY);
        int sub_pel = oy % precision;

        return tapsYs[sub_pel];
    }

    @Override
    protected int nTaps() {
        return _nTaps;
    }
}