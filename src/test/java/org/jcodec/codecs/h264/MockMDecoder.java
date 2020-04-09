package org.jcodec.codecs.h264;
import org.jcodec.codecs.common.biari.MDecoder;
import org.junit.Assert;

import java.nio.ByteBuffer;

public class MockMDecoder extends MDecoder {

    private int[] out;
    private int[] m;
    private int pos;

    public MockMDecoder(int[] out, int[] m) {
        super(ByteBuffer.allocate(0), new int[0][0]);
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