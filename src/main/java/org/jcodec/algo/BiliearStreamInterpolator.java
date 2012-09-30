package org.jcodec.algo;

import static java.lang.System.arraycopy;

import java.io.File;
import java.io.IOException;

import org.jcodec.codecs.wav.WavHeader;
import org.jcodec.codecs.wav.WavInput;
import org.jcodec.codecs.wav.WavOutput;
import org.jcodec.common.model.Rational;
import org.junit.Assert;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class BiliearStreamInterpolator extends StreamInterpolator {
    private int pos;
    private int step;

    private int[] lastSamples;
    private int nLastSamples;

    public static final int ROUND = 1 << 7;
    public static final int SHIFT = 8;
    public static final int MASK = 0xff;

    public BiliearStreamInterpolator(Rational r) {
        super(r);

        step = (r.getDen() << SHIFT) / r.getNum();
        lastSamples = new int[256];
    }

    public int[] interpolate(int[] in) {
        int[] result = new int[(int) ((((long) in.length + nLastSamples) * ratio.getNum()) / ratio.getDen())];

        int[] in1 = new int[nLastSamples + in.length];
        arraycopy(lastSamples, 0, in1, 0, nLastSamples);
        arraycopy(in, 0, in1, nLastSamples, in.length);

        for (int i = 0; i < result.length - 1; i++) {
            int ind = (int) (pos >> SHIFT);
            int s0 = in1[ind];
            int s1 = in1[ind + 1];
            result[i] = interpolateH(s0, s1, (int) (pos & MASK));

            pos += step;
        }
        nLastSamples = in1.length - (int) (pos >> SHIFT);
        arraycopy(in1, pos >> SHIFT, lastSamples, 0, nLastSamples);
        pos &= MASK;
        return result;
    }

    private int c = 0;

    private final int interpolateH(int s0, int s1, int shift) {
        Assert.assertTrue(s0 >= 0);
        Assert.assertTrue(s1 >= 0);
        Assert.assertTrue(shift < (MASK + 1));
        int s = ((s0 << SHIFT) + shift * (s1 - s0) + ROUND) >> SHIFT;
        Assert.assertTrue("" + s + ", " + s0 + ", " + s1 + ", " + (c++), s >= 0);
        return s;
    }

    public static void main(String[] args) throws IOException {
        WavInput inp = null;
        WavOutput out = null;
        try {
            inp = new WavInput(new File(args[0]));
            out = new WavOutput(new File(args[1]), new WavHeader(inp.getHeader(), 44100));
            
            BiliearStreamInterpolator in = new BiliearStreamInterpolator(new Rational(44100, 48000));
            
            int[] samples;
            while ((samples = inp.read(1024)) != null) {
                int[] outSamples = in.interpolate(samples);
                out.write(outSamples);
            }
            
        } finally {
            inp.close();
            out.close();
        }
    }
}
