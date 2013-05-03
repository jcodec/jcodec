package org.jcodec.codecs.h264;

import junit.framework.Assert;

import org.jcodec.codecs.common.biari.MDecoder;

public class MockMDecoder extends MDecoder {

    private int[] out;
    private int[] m;
    private int pos;

    public MockMDecoder(int[] out, int[] m) {
        super(null, null);
        this.out = out;
        this.m = m;
    }

    protected void readOneByte()  {
    }

    protected void initCodeRegister() {
    }

    @Override
    public int decodeBin(int _m){
        Assert.assertEquals(m[pos], _m);

        return out[pos++];
    }

    @Override
    public int decodeFinalBin() {
        Assert.assertEquals(m[pos], -2);
        return out[pos++];
    }

    @Override
    public int decodeBinBypass() {
        Assert.assertEquals(m[pos], -1);
        return out[pos++];
    }
}