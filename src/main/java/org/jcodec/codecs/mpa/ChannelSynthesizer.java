package org.jcodec.codecs.mpa;

import static org.jcodec.codecs.mpa.MpaPqmf.computeButterfly;
import static org.jcodec.codecs.mpa.MpaPqmf.computeFilter;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 */
class ChannelSynthesizer {
    private float[][] v = new float[2][512];
    private int pos;
    private float scalefactor;
    private int current;

    public ChannelSynthesizer(int channelnumber, float factor) {
        scalefactor = factor;
        pos = 15;
    }
    
    private void distributeSamples(int pos, float[] dest, float[] next, float[] s) {
        for (int i = 0; i < 16; i++)
            dest[(i << 4) + pos] = s[i];
        
        for (int i = 1; i < 17; i++)
            next[(i << 4) + pos] = s[15 + i];

        dest[256 + pos] = 0.0f;
        next[0 + pos] = -s[0];

        for (int i = 0; i < 15; i++)
            dest[272 + (i << 4) + pos] = -s[15 - i];

        for (int i = 0; i < 15; i++)
            next[272 + (i << 4) + pos] = s[30 - i];

    }

    public void synthesize(float[] coeffs, short[] out, int off) {
        computeButterfly(pos, coeffs);
        int next = ~current & 1;
        distributeSamples(pos, v[current], v[next], coeffs);
        computeFilter(pos, v[current], out, off, scalefactor);

        pos = (pos + 1) & 0xf;
        current = next;
    }
}
