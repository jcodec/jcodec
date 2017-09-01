package org.jcodec.codecs.vpx.vp9;

import org.jcodec.codecs.vpx.VPXBooleanDecoder;
import org.junit.Assert;

public class MockVPXBooleanDecoder extends VPXBooleanDecoder {
    private int[] probs;
    private int[] bits;
    private int pos;

    public MockVPXBooleanDecoder(int[] probs, int[] bits) {
        Assert.assertEquals(probs.length, bits.length);
        this.probs = probs;
        this.bits = bits;
    }

    @Override
    public int readBit(int prob) {
        Assert.assertTrue(pos < probs.length);
        Assert.assertEquals(probs[pos], prob);
        return bits[pos++];
    }

    public boolean isFullyRead() {
        return pos == bits.length;
    }
}