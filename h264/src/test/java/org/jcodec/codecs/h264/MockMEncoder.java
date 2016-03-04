package org.jcodec.codecs.h264;
import org.jcodec.codecs.common.biari.MEncoder;
import org.junit.Assert;

public class MockMEncoder extends MEncoder {

    private int[] bits;
    private int[] m;
    private int pos;

    public MockMEncoder(int[] bits, int[] m) {
        super(null, null);
        this.bits = bits;
        this.m = m;
    }

    @Override
    public void encodeBin(int model, int bin) {
        Assert.assertEquals(m[pos], model);
        Assert.assertEquals(bits[pos++], bin);
    }

    @Override
    public void encodeBinBypass(int bin) {
        Assert.assertEquals(bits[pos++], bin);
    }

    @Override
    public void encodeBinFinal(int bin) {
        Assert.assertEquals(m[pos], -2);
        Assert.assertEquals(bits[pos++], bin);
    }

    @Override
    public void finishEncoding() {
    }

}
