package org.jcodec.codecs.vpx.vp9;

import static org.jcodec.codecs.vpx.vp9.Consts.LAST_FRAME;
import static org.jcodec.codecs.vpx.vp9.Consts.SINGLE_REF;
import static org.jcodec.codecs.vpx.vp9.Consts.SWITCHABLE;

import org.jcodec.codecs.vpx.VPXBooleanDecoder;
import org.jcodec.common.ArrayUtil;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Ignore;

import junit.framework.AssertionFailedError;

public class InterModeInfoTest {
    private static short[] Y_MODE_PROBS = { 35, 32, 18, 144, 218, 194, 41, 51, 98, 44, 68, 18, 165, 217, 196, 45, 40, 78,
            173, 80, 19, 176, 240, 193, 64, 35, 46, 221, 135, 38, 194, 248, 121, 96, 85, 29 };

    private static short[] INTER_MODE_PROBS = { 2, 173, 34, 7, 145, 85, 7, 166, 63, 7, 94, 66, 8, 64, 46, 17, 81, 31, 25,
            29, 30 };
    
    @Ignore("")
    @Test
    public void testReadInterModeInfo() {
        MockVPXBooleanDecoder decoder = new MockVPXBooleanDecoder(new int[] {}, new int[] {});
        DecodingContext c = new DecodingContext();
        int miCol = 1;
        int miRow = 5;
        int blSz = 3;
        c.miTileStartCol = 0;
        c.interpFilter = SWITCHABLE;
        c.refMode = SINGLE_REF;
        c.aboveCompound = new boolean[2];
        c.leftCompound = new boolean[6];
        c.aboveRefs = new int[2];
        c.leftRefs = new int[6];

        InterModeInfo modeInfo = new InterModeInfo() {
            @Override
            public int readInterMode(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder, DecodingContext c) {
                return 11;
            }

            @Override
            protected int readInterIntraMode(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder,
                    DecodingContext c) {
                throw new AssertionFailedError(String.format("readInterIntraMode [miCol=%d, miRow=%d, blSz=%d]", miCol, miRow, blSz));
            }

            @Override
            protected int readInterIntraModeSub(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder,
                    DecodingContext c) {
                throw new AssertionFailedError(String.format("readInterIntraModeSub [miCol=%d, miRow=%d, blSz=%d]", miCol, miRow, blSz));
            }

            @Override
            public int readKfUvMode(int yMode, VPXBooleanDecoder decoder, DecodingContext c) {
                throw new AssertionFailedError(String.format("readKfUvMode [yMode=%d]", yMode));
            }

            @Override
            public int readKfIntraMode(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder, DecodingContext c) {
                throw new AssertionFailedError(String.format("readKfIntraMode [miCol=%d, miRow=%d, blSz=%d]", miCol, miRow, blSz));
            }

            @Override
            public int readKfIntraModeSub(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder,
                    DecodingContext c) {
                throw new AssertionFailedError(String.format("readKfIntraModeSub [miCol=%d, miRow=%d, blSz=%d]", miCol, miRow, blSz));
            }

            @Override
            public int readTxSize(int miCol, int miRow, int blSz, boolean allowSelect, VPXBooleanDecoder decoder,
                    DecodingContext c) {
                System.out.println(String.format("readTxSize [miCol=%d, miRow=%d, blSz=%d, allowSelect=%d]", miCol,
                        miRow, blSz, allowSelect ? 1 : 0));
                return 1;
            }

            @Override
            public boolean readSkipFlag(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder, DecodingContext c) {
                System.out.println(String.format("readTxSize [miCol=%d, miRow=%d, blSz=%d]", miCol,
                        miRow, blSz));
                return false;
            }

            @Override
            public int readInterIntraUvMode(int yMode, VPXBooleanDecoder decoder, DecodingContext c) {
                throw new AssertionFailedError(String.format("readInterIntraUvMode [yMode=%d]", yMode));
            }

            @Override
            protected int readCompRef(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder, DecodingContext c) {
                throw new AssertionFailedError(String.format("readCompRef [miCol=%d, miRow=%d, blSz=%d]", miCol, miRow, blSz));
            }

            @Override
            protected int readSingleRef(int miCol, int miRow, VPXBooleanDecoder decoder, DecodingContext c) {
                System.out.println(String.format("readSingleRef [miCol=%d, miRow=%d]", miCol, miRow));
                return LAST_FRAME;
            }

            @Override
            protected boolean readRefMode(int miCol, int miRow, VPXBooleanDecoder decoder, DecodingContext c) {
                throw new AssertionFailedError(String.format("readRefMode [miCol=%d, miRow=%d]", miCol, miRow));
            }

            @Override
            protected int readInterpFilter(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder,
                    DecodingContext c) {
                System.out.println(String.format("readSingleRef [miCol=%d, miRow=%d, blSz=%d]", miCol, miRow, blSz));
                return 2;
            }

            @Override
            protected boolean readIsInter(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder,
                    DecodingContext c) {
                System.out.println(String.format("readSingleRef [miCol=%d, miRow=%d, blSz=%d]", miCol, miRow, blSz));
                return true;
            }

            @Override
            protected long readMV8x8AndAbove(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder,
                    DecodingContext c, int packedRefFrames, int lumaMode) {
                return 0;
            }

            @Override
            protected long[] readMvSub8x8(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder, DecodingContext c,
                    int packedRefFrames) {
                throw new AssertionFailedError(String.format("readMvSub8x8 [miCol=%d, miRow=%d, blSz=%d]", miCol, miRow, blSz));
            }

            @Override
            protected long[] readMV4x4(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder, DecodingContext c,
                    int packedRefFrames) {
                throw new AssertionFailedError(String.format("readMV4x4 [miCol=%d, miRow=%d, blSz=%d]", miCol, miRow, blSz));
            }
        }.read(miCol, miRow, blSz, decoder, c);

        Assert.assertEquals(true, modeInfo.isInter());
        Assert.assertEquals(MVList.create(MV.create(0, 0, 0), MV.create(0, 0, 0)), modeInfo.getMvl0());
        Assert.assertEquals(MVList.create(MV.create(0, 0, 0), MV.create(0, 0, 0)), modeInfo.getMvl1());
        Assert.assertEquals(MVList.create(MV.create(0, 0, 0), MV.create(0, 0, 0)), modeInfo.getMvl2());
        Assert.assertEquals(MVList.create(MV.create(0, 0, 0), MV.create(0, 0, 0)), modeInfo.getMvl3());

        Assert.assertEquals(0, modeInfo.getSegmentId());
        Assert.assertEquals(false, modeInfo.isSkip());
        Assert.assertEquals(0, modeInfo.getTxSize());
        Assert.assertEquals(0, modeInfo.getYMode());
        Assert.assertEquals(0, modeInfo.getSubModes());
        Assert.assertEquals(0, modeInfo.getUvMode());
    }

    @Test
    public void testReadIntraMode() {
        MockVPXBooleanDecoder decoder = new MockVPXBooleanDecoder(new int[] { 44, 68, 18, 165, 217 },
                new int[] { 1, 1, 1, 0, 0 });
        DecodingContext c = new DecodingContext();
        int miCol = 9;
        int miRow = 1;
        int blSz = 3;
        ArrayUtil.fill2D(c.yModeProbs, Y_MODE_PROBS, 0);

        Assert.assertEquals(2, new InterModeInfo().readInterIntraMode(miCol, miRow, blSz, decoder, c));
    }
    
    @Ignore
    @Test
    public void testUVMode() {
        MockVPXBooleanDecoder decoder = new MockVPXBooleanDecoder(
                new int[] { 101, 21, 107, 181, 192, 103 },
                new int[] { 1, 1, 1, 0, 1, 1 });
        DecodingContext c = new DecodingContext();

        int intraMode = 9;
        Assert.assertEquals(5,
                new InterModeInfo().readKfUvMode(intraMode, decoder, c));
    }

    @Test
    public void testReadIntraModeSub() {
        MockVPXBooleanDecoder decoder = new MockVPXBooleanDecoder(
                new int[] { 35, 32, 18, 144, 41, 51, 98, 35, 32, 35, 32, 18, 144, 218, 35, 32, 101, 21, 107, 181, 192 },
                new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 0, 1, 0, 1, 1, 1, 0, 0 });
        DecodingContext c = new DecodingContext();
        int miCol = 8;
        int miRow = 1;
        int blSz = 0;
        ArrayUtil.fill2D(c.yModeProbs, Y_MODE_PROBS, 0);

        Assert.assertEquals(ModeInfo.vect4(7, 9, 2, 9),
                new InterModeInfo().readInterIntraModeSub(miCol, miRow, blSz, decoder, c));
    }

    @Test
    public void testReadInterModeSub() {
        MockVPXBooleanDecoder decoder = new MockVPXBooleanDecoder(new int[] { 7, 94, 7, 94, 66, 7, 94, 66, 7, 94 },
                new int[] { 1, 0, 1, 1, 0, 1, 1, 0, 1, 0 });
        DecodingContext c = new DecodingContext();
        int miCol = 3;
        int miRow = 1;
        int blSz = 0;
        c.miTileStartCol = 0;
        ArrayUtil.fill2D(c.interModeProbs, INTER_MODE_PROBS, 0);

        c.aboveModes = new int[] { 0, 0, 10, 13, 0, 2, 2 };
        c.leftModes = new int[] { 10, 11, 9, 2, 2 };
        c.tileHeight = 36;
        c.tileWidth = 64;

        Assert.assertEquals(10, new InterModeInfo().readInterMode(miCol, miRow, blSz, decoder, c));
    }

    @Test
    public void testReadInterMode() {
        MockVPXBooleanDecoder decoder = new MockVPXBooleanDecoder(new int[] { 7, 166, 63 }, new int[] { 1, 1, 1 });
        DecodingContext c = new DecodingContext();
        int miCol = 0;
        int miRow = 0;
        int blSz = 0;
        c.miTileStartCol = 0;
        ArrayUtil.fill2D(c.interModeProbs, INTER_MODE_PROBS, 0);

        c.aboveModes = new int[8];
        c.leftModes = new int[8];
        c.tileHeight = 36;
        c.tileWidth = 64;

        Assert.assertEquals(13, new InterModeInfo().readInterMode(miCol, miRow, blSz, decoder, c));
    }
    
    @Ignore
    @Test
    public void readInterpFilter() {
        int miCol = 1;
        int miRow = 5;
        int blSz = 3;
        MockVPXBooleanDecoder decoder = new MockVPXBooleanDecoder(new int[] { 149, 144 }, new int[] { 1, 1 });
        DecodingContext c = new DecodingContext();
        c.miTileStartCol = 0;
        c.aboveRefs = new int[] {};
        c.leftRefs = new int[] {};
        c.aboveInterpFilters = new int[] {0, 1};
        c.leftInterpFilters = new int[] {0, 0, 0, 0, 0, 0};
        c.aboveRefs = new int[] {LAST_FRAME, LAST_FRAME };
        c.leftRefs = new int[] {LAST_FRAME, LAST_FRAME, LAST_FRAME, LAST_FRAME, LAST_FRAME, LAST_FRAME};
        
        Assert.assertEquals(2, new InterModeInfo().readInterpFilter(miCol, miRow, blSz, decoder, c));
    }
}
