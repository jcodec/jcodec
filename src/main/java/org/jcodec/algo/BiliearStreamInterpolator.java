package org.jcodec.algo;

import static java.lang.System.arraycopy;

import java.io.File;
import java.io.IOException;

import org.jcodec.codecs.wav.WavInput;
import org.jcodec.codecs.wav.WavOutput;
import org.jcodec.common.Assert;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.model.Rational;

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

    public int interpolate(int[] in, int[] result) {
        int sizeRequired = (int) ((((long) in.length + nLastSamples) * ratio.getNum()) / ratio.getDen());
        if (result.length < sizeRequired)
            throw new IllegalArgumentException(sizeRequired + " samples is required in the output buffer.");

        int[] in1 = new int[nLastSamples + in.length];
        arraycopy(lastSamples, 0, in1, 0, nLastSamples);
        arraycopy(in, 0, in1, nLastSamples, in.length);

        int i = 0;
        for (; i < result.length - 1; i++) {
            int ind = (int) (pos >> SHIFT);
            int s0 = in1[ind];
            int s1 = in1[ind + 1];
            result[i] = interpolateH(s0, s1, (int) (pos & MASK));

            pos += step;
        }
        nLastSamples = in1.length - (int) (pos >> SHIFT);
        arraycopy(in1, pos >> SHIFT, lastSamples, 0, nLastSamples);
        pos &= MASK;

        return i;
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
        WavInput.Adaptor inp = null;
        WavOutput.Adaptor out = null;
        try {
            inp = new WavInput.Adaptor(new File(args[0]));
            out = new WavOutput.Adaptor(new File(args[1]), new AudioFormat(inp.getFormat(), 44100));

            BiliearStreamInterpolator in = new BiliearStreamInterpolator(new Rational(44100, 48000));

            int[] samples = new int[1024];
            int[] outSamples = new int[2048];

            while (inp.read(samples, 1024) > 0) {
                int outSize = in.interpolate(samples, outSamples);
                out.write(outSamples, outSize);
            }

        } finally {
            inp.close();
            out.close();
        }
    }
}
