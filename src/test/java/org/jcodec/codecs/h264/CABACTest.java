package org.jcodec.codecs.h264;
import static org.jcodec.codecs.h264.H264Const.PartPred.L0;

import org.jcodec.codecs.h264.io.CABAC;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.junit.Assert;
import org.junit.Test;

public class CABACTest {

    @Test
    public void testMBTypeI() {
        MockMDecoder m = new MockMDecoder(new int[] { 0 }, new int[] { 3 });
        Assert.assertEquals(0, new CABAC(1).readMBTypeI(m, null, null, false, false));

        MockMEncoder e = new MockMEncoder(new int[] { 0 }, new int[] { 3 });
        new CABAC(1).writeMBTypeI(e, null, null, false, false, 0);

        m = new MockMDecoder(new int[] { 1, 1 }, new int[] { 3, -2 });
        Assert.assertEquals(25, new CABAC(1).readMBTypeI(m, null, null, false, false));

        e = new MockMEncoder(new int[] { 1, 1 }, new int[] { 3, -2 });
        new CABAC(1).writeMBTypeI(e, null, null, false, false, 25);

        m = new MockMDecoder(new int[] { 0 }, new int[] { 4 });
        Assert.assertEquals(0, new CABAC(1).readMBTypeI(m, MBType.I_16x16, null, true, false));

        m = new MockMDecoder(new int[] { 0 }, new int[] { 4 });
        Assert.assertEquals(0, new CABAC(1).readMBTypeI(m, null, MBType.I_16x16, false, true));

        m = new MockMDecoder(new int[] { 0 }, new int[] { 5 });
        Assert.assertEquals(0, new CABAC(1).readMBTypeI(m, MBType.I_16x16, MBType.I_16x16, true, true));

        m = new MockMDecoder(new int[] { 1, 0, 0, 1, 1, 1, 0 }, new int[] { 3, -2, 6, 7, 8, 9, 10 });
        Assert.assertEquals(11, new CABAC(1).readMBTypeI(m, null, null, false, false));

        e = new MockMEncoder(new int[] { 1, 0, 0, 1, 1, 1, 0 }, new int[] { 3, -2, 6, 7, 8, 9, 10 });
        new CABAC(1).writeMBTypeI(e, null, null, false, false, 11);
    }

    @Test
    public void testMBTypeP() {
        CABAC cabac = new CABAC(2);
        MockMDecoder m = new MockMDecoder(new int[] { 0, 0, 1 }, new int[] { 14, 15, 16 });
        Assert.assertEquals(3, cabac.readMBTypeP(m));
    }

    @Test
    public void testReadIntraChromaPredMode() {
        MockMDecoder m = new MockMDecoder(new int[] { 0 }, new int[] { 64 });
        Assert.assertEquals(0, new CABAC(1).readIntraChromaPredMode(m, 0, null, null, false, false));

        MockMEncoder e = new MockMEncoder(new int[] { 0 }, new int[] { 64 });
        new CABAC(1).writeIntraChromaPredMode(e, 0, null, null, false, false, 0);

        m = new MockMDecoder(new int[] { 1, 1, 1 }, new int[] { 64, 67, 67 });
        Assert.assertEquals(3, new CABAC(1).readIntraChromaPredMode(m, 0, null, null, false, false));

        e = new MockMEncoder(new int[] { 1, 1, 1 }, new int[] { 64, 67, 67 });
        new CABAC(1).writeIntraChromaPredMode(e, 0, null, null, false, false, 3);
    }

    public void _testMBQpDelta() {
        MockMDecoder m = new MockMDecoder(new int[] { 0 }, new int[] { 60 });
        Assert.assertEquals(0, new CABAC(1).readMBQpDelta(m, null));

        m = new MockMDecoder(new int[] { 1, 1, 1, 1, 1, 1, 0 }, new int[] { 60, 62, 63, 63, 63, 63, 63 });
        Assert.assertEquals(6, new CABAC(1).readMBQpDelta(m, null));

        MockMEncoder e = new MockMEncoder(new int[] { 0 }, new int[] { 60 });
        new CABAC(1).writeMBQpDelta(e, null, 0);

        e = new MockMEncoder(new int[] { 1, 1, 1, 1, 1, 1, 0 }, new int[] { 60, 62, 63, 63, 63, 63, 63 });
        new CABAC(1).writeMBQpDelta(e, null, 6);
    }

    @Test
    public void testRefIdx() {
        MockMDecoder m = new MockMDecoder(new int[] { 0 }, new int[] { 54 });

        Assert.assertEquals(0, new CABAC(40).readRefIdx(m, true, false, null, null, L0, L0, L0, 17, 0, 0, 4, 4, 0));

        m = new MockMDecoder(new int[] { 1, 0 }, new int[] { 54, 58 });

        Assert.assertEquals(1,
                new CABAC(40).readRefIdx(m, true, true, MBType.P_16x16, MBType.P_16x16, L0, L0, L0, 23, 0, 0, 4, 2, 0));

        m = new MockMDecoder(new int[] { 1, 0, 1, 0 }, new int[] { 54, 58, 55, 58 });

        CABAC cabac = new CABAC(40);
        Assert.assertEquals(1, cabac.readRefIdx(m, true, false, MBType.P_16x16, null, L0, L0, L0, 21, 0, 0, 2, 4, 0));

        Assert.assertEquals(1, cabac.readRefIdx(m, true, false, MBType.P_8x16, null, L0, L0, L0, 21, 2, 0, 2, 4, 0));
    }

    @Test
    public void testMVD() {

        MockMDecoder m = new MockMDecoder(new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 0 }, new int[] { 40,
                43, 44, 45, 46, 46, 46, 46, 46, -1, -1, -1, -1, -1, -1, -1 });
        CABAC cabac = new CABAC(2);
        Assert.assertEquals(28, cabac.readMVD(m, 0, false, false, null, null, L0, L0, L0, 0, 0, 0, 2, 1, 0));

        m = new MockMDecoder(new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 0 }, new int[] { 47, 50,
                51, 52, 53, 53, 53, 53, 53, -1, -1, -1, -1, -1, -1, -1, -1, -1 });
        Assert.assertEquals(64, cabac.readMVD(m, 1, false, false, null, null, L0, L0, L0, 0, 0, 0, 2, 1, 0));

        m = new MockMDecoder(new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 0 }, new int[] { 41, 43,
                44, 45, 46, 46, 46, 46, 46, -1, -1, -1, -1, -1, -1, -1, -1, -1 });
        Assert.assertEquals(40, cabac.readMVD(m, 0, false, true, null, MBType.P_8x8, L0, L0, L0, 0, 0, 1, 2, 1, 0));

        m = new MockMDecoder(new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 0, 0, 0, 1 }, new int[] { 49,
                50, 51, 52, 53, 53, 53, 53, 53, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 });
        Assert.assertEquals(-89, cabac.readMVD(m, 1, false, true, null, MBType.P_8x8, L0, L0, L0, 0, 0, 1, 2, 1, 0));

        m = new MockMDecoder(new int[] { 1, 1, 1, 1, 0, 0 }, new int[] { 41, 43, 44, 45, 46, -1 });
        Assert.assertEquals(4, cabac.readMVD(m, 0, true, false, MBType.P_8x8, null, L0, L0, L0, 0, 2, 0, 1, 2, 0));

        m = new MockMDecoder(new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 1 }, new int[] { 49, 50,
                51, 52, 53, 53, 53, 53, 53, -1, -1, -1, -1, -1, -1, -1, -1, -1 });
        Assert.assertEquals(-40, cabac.readMVD(m, 1, true, false, MBType.P_8x8, null, L0, L0, L0, 0, 2, 0, 1, 2, 0));

        m = new MockMDecoder(new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0 }, new int[] { 41, 43, 44, 45, 46,
                46, 46, 46, 46, -1, -1, -1, -1, -1 });
        Assert.assertEquals(16, cabac.readMVD(m, 0, true, false, MBType.P_8x8, null, L0, L0, L0, 0, 3, 0, 1, 2, 0));

        m = new MockMDecoder(new int[] { 0 }, new int[] { 49 });
        Assert.assertEquals(0, cabac.readMVD(m, 1, true, false, MBType.P_8x8, null, L0, L0, L0, 0, 3, 0, 1, 2, 0));

        m = new MockMDecoder(new int[] { 0 }, new int[] { 42 });
        Assert.assertEquals(0, cabac.readMVD(m, 0, false, true, null, MBType.P_8x8, L0, L0, L0, 0, 0, 2, 2, 2, 0));

        m = new MockMDecoder(new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 0 }, new int[] { 49, 50,
                51, 52, 53, 53, 53, 53, 53, -1, -1, -1, -1, -1, -1, -1, -1, -1 });
        Assert.assertEquals(64, cabac.readMVD(m, 1, false, true, null, MBType.P_8x8, L0, L0, L0, 0, 0, 2, 2, 2, 0));

        m = new MockMDecoder(new int[] { 0 }, new int[] { 41 });
        Assert.assertEquals(0,
                cabac.readMVD(m, 0, true, true, MBType.P_8x8, MBType.P_8x8, L0, L0, L0, 0, 2, 2, 2, 2, 0));

        m = new MockMDecoder(new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 0 }, new int[] { 49, 50,
                51, 52, 53, 53, 53, 53, 53, -1, -1, -1, -1, -1, -1, -1, -1, -1 });
        Assert.assertEquals(40,
                cabac.readMVD(m, 1, true, true, MBType.P_8x8, MBType.P_8x8, L0, L0, L0, 0, 2, 2, 2, 2, 0));
    }

    @Test
    public void testMBSkipFlag() {
        CABAC cabac = new CABAC(2);
        MockMDecoder m = new MockMDecoder(new int[] { 0 }, new int[] { 11 });
        Assert.assertEquals(false, cabac.readMBSkipFlag(m, SliceType.P, false, false, 0));

        m = new MockMDecoder(new int[] { 0 }, new int[] { 12 });
        Assert.assertEquals(false, cabac.readMBSkipFlag(m, SliceType.P, true, false, 1));

        m = new MockMDecoder(new int[] { 0 }, new int[] { 12 });
        Assert.assertEquals(false, cabac.readMBSkipFlag(m, SliceType.P, false, true, 0));

        m = new MockMDecoder(new int[] { 0 }, new int[] { 13 });
        Assert.assertEquals(false, cabac.readMBSkipFlag(m, SliceType.P, true, true, 1));
    }

    @Test
    public void testSubMbType() {

        MockMDecoder m = new MockMDecoder(new int[] { 0, 0 }, new int[] { 21, 22 });
        Assert.assertEquals(1, new CABAC(1).readSubMbTypeP(m));

        m = new MockMDecoder(new int[] { 0, 1, 1 }, new int[] { 21, 22, 23 });
        Assert.assertEquals(2, new CABAC(1).readSubMbTypeP(m));

        m = new MockMDecoder(new int[] { 1 }, new int[] { 21 });
        Assert.assertEquals(0, new CABAC(1).readSubMbTypeP(m));
    }
}