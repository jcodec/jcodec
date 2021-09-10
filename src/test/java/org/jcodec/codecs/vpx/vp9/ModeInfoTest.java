package org.jcodec.codecs.vpx.vp9;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class ModeInfoTest {

    @Ignore
    @Test
    public void testReadSkipFlag() {
        MockVPXBooleanDecoder decoder = new MockVPXBooleanDecoder(new int[] { 64 }, new int[] { 0 });
        DecodingContext c = new DecodingContext();
        c.aboveSkipped = new boolean[24];
        c.leftSkipped = new boolean[8];
        int miCol = 20;
        int miRow = 12;
        int blSz = 8;
        c.miTileStartCol = 0;
        c.aboveSkipped[20] = true;
        c.leftSkipped[12 & 0x7] = true;

        Assert.assertEquals(false, new ModeInfo().readSkipFlag(miCol, miRow, blSz, decoder, c));
    }

    @Test
    public void testReadSubIntraMode() {
        MockVPXBooleanDecoder decoder = new MockVPXBooleanDecoder(
                new int[] { 137, 137, 30, 42, 148, 151, 137, 82, 26, 26, 171, 44, 32, 105 },
                new int[] { 0, 1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0 });
        DecodingContext c = new DecodingContext();
        int miCol = 0;
        int miRow = 0;
        int blSz = 0;
        c.miTileStartCol = 0;
        c.aboveModes = new int[2];
        c.leftModes = new int[16];

        Assert.assertEquals(ModeInfo.vect4(0, 2, 0, 6),
                new ModeInfo().readKfIntraModeSub(miCol, miRow, blSz, decoder, c));
    }
    
    @Test
    public void testReadIntraMode() {
        MockVPXBooleanDecoder decoder = new MockVPXBooleanDecoder(
                new int[] { 91, 30, 32, 116, 93},
                new int[] { 1, 1, 1, 1, 0 });
        DecodingContext c = new DecodingContext();
        int miCol = 2;
        int miRow = 0;
        int blSz = 3;
        c.miTileStartCol = 0;
        c.aboveModes = new int[16];
        c.leftModes = new int[16];
        c.leftModes[0] = 3;
        
        Assert.assertEquals(3,
                new ModeInfo().readKfIntraMode(miCol, miRow, blSz, decoder, c));
    }
    
    @Test
    public void testUVMode() {
        MockVPXBooleanDecoder decoder = new MockVPXBooleanDecoder(new int[] { 144, 11, 54, 157, 195, 130 },
                new int[] { 1, 1, 1, 0, 1, 1 });
        DecodingContext c = new DecodingContext();

        int intraMode = 0;
        Assert.assertEquals(5, new ModeInfo().readInterIntraUvMode(intraMode, decoder, c));
    }
    
    @Test
    public void testTxSize() {
        MockVPXBooleanDecoder decoder = new MockVPXBooleanDecoder(new int[] { 44 }, new int[] { 1 });
        DecodingContext c = new DecodingContext();
        int miCol = 2;
        int miRow = 0;
        int blSz = 3;
        c.miTileStartCol = 0;
        c.txMode = Consts.TX_MODE_SELECT;
        c.aboveSkipped = new boolean[8];
        c.leftSkipped = new boolean[8];
        c.aboveTxSizes = new int[8];
        c.leftTxSizes = new int[8];
        c.tx8x8Probs = new short[][] { { 44 }, { 66 } };
        c.tx16x16Probs = new short[][] { { 20, 152 }, { 15, 101 } };
        c.tx16x16Probs = new short[][] { { 3, 136, 37 }, { 5, 52, 13 } };
        
        Assert.assertEquals(1,
                new ModeInfo().readTxSize(miCol, miRow, blSz, true, decoder, c));
    }
    
    @Ignore
    @Test
    public void testTxSizeInter() {
        MockVPXBooleanDecoder decoder = new MockVPXBooleanDecoder(new int[] { 3, 136, 37 }, new int[] { 1, 1, 1 });
        DecodingContext c = new DecodingContext();
        int miCol = 16;
        int miRow = 4;
        int blSz = 9;
        c.miTileStartCol = 0;
        c.txMode = Consts.TX_MODE_SELECT;
        c.aboveSkipped = new boolean[17];
        c.leftSkipped = new boolean[8];
        c.aboveTxSizes = new int[17];
        c.leftTxSizes = new int[8];
        c.tx8x8Probs = new short[][] { { 44 }, { 66 } };
        c.tx16x16Probs = new short[][] { { 20, 152 }, { 15, 101 } };
        c.tx16x16Probs = new short[][] { { 3, 136, 37 }, { 5, 52, 13 } };
        c.aboveTxSizes[miCol] = 1;
        c.leftTxSizes[miRow & 0x7] = 2;
        
        Assert.assertEquals(3,
                new ModeInfo().readTxSize(miCol, miRow, blSz, true, decoder, c));
    }
}
