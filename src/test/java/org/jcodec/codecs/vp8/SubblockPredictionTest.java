package org.jcodec.codecs.vp8;

import org.jcodec.codecs.vpx.VP8Util;
import org.jcodec.codecs.vpx.vp8.enums.BPredictionMode;
import org.jcodec.codecs.vpx.vp8.intrapred.AllIntraPred;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;
import org.jcodec.codecs.vpx.vp8.pointerhelper.ReadOnlyIntArrPointer;
import org.junit.Assert;
import org.junit.Test;

public class SubblockPredictionTest {

    @Test
    public void testVR() {
        short above[] = { 46, 50, 55, 60 };
        short left[] = { 40, 37, 35, 35 };
        short aboveLeft = 42;
        short aboveRight[] = { 68, 68, 68, 68 };
        short[] p = { 44, 48, 53, 58, 43, 46, 50, 55, 40, 44, 48, 53, 37, 43, 46, 50 };
        /*-----------------------------------------------------*/
        testBPred(BPredictionMode.B_VR_PRED,above,aboveLeft,aboveRight,left,p);
    }

    @Test
    public void testHD() {
        short above[] = { 40, 40, 41, 42 };
        short left[] = { 35, 35, 35, 35 };
        short aboveLeft = 36;
        short aboveRight[] = { 46, 50, 55, 60 };
        short[] p = { 36, 37, 39, 40, 35, 35, 36, 37, 35, 35, 35, 35, 35, 35, 35, 35 };
        /*-----------------------------------------------------*/
        testBPred(BPredictionMode.B_HD_PRED,above,aboveLeft,aboveRight,left,p);
    }

    @Test
    public void testHU() {
        short above[] = { 48, 48, 47, 43 };
        short left[] = { 17, 17, 17, 17 };
        short aboveLeft = 48;
        short aboveRight[] = { 38, 38, 38, 38 };
        short[] p = { 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17 };
        /*-----------------------------------------------------*/
        testBPred(BPredictionMode.B_HU_PRED,above,aboveLeft,aboveRight,left,p);
    }

    @Test
    public void testRD() {
        short above[] = { 39, 37, 37, 37 };
        short left[] = { 48, 48, 48, 48 };
        short aboveLeft = 43;
        short aboveRight[] = { 37, 37, 37, 37 };
        short[] p = { 43, 40, 38, 37, 47, 43, 40, 38, 48, 47, 43, 40, 48, 48, 47, 43 };
        /*-----------------------------------------------------*/
        testBPred(BPredictionMode.B_RD_PRED,above,aboveLeft,aboveRight,left,p);
    }

    @Test
    public void testVE() {
        short above[] = { 38, 37, 37, 37 };
        short aboveLeft = 41;
        short aboveRight[] = { 37, 37, 37, 37 };
        short[] p = { 39, 37, 37, 37, 39, 37, 37, 37, 39, 37, 37, 37, 39, 37, 37, 37 };
        /*-----------------------------------------------------*/
        testBPred(BPredictionMode.B_VE_PRED,above,aboveLeft,aboveRight,new short[1],p);
    }

    @Test
    public void testHE() {
        short above[] = { 48, 48, 48, 48 };
        short left[] = { 40, 40, 40, 39 };
        short aboveLeft = 44;
        short aboveRight[] = { 45, 45, 45, 45 };
        short[] p = { 41, 41, 41, 41, 40, 40, 40, 40, 40, 40, 40, 40, 39, 39, 39, 39 };
        /*-----------------------------------------------------*/
        testBPred(BPredictionMode.B_HE_PRED,above,aboveLeft,aboveRight,left,p);
    }

    @Test
    public void testDC() {
        short above[] = { 39, 39, 39, 39 };
        short left[] = { 38, 40, 59, 126 };
        short[] p = { 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52 };
        /*-----------------------------------------------------*/
        testBPred(BPredictionMode.B_DC_PRED,above,(short)-1,new short[0],left,p);
    }

    @Test
    public void testTM() {
        short above[] = { 41, 41, 41, 41 };
        short left[] = { 37, 48, 104, 180 };
        short aboveLeft=41;
        short[] p = { 37, 37, 37, 37, 48, 48, 48, 48, 104, 104, 104, 104, 180, 180, 180, 180 };
        /*-----------------------------------------------------*/
        testBPred(BPredictionMode.B_TM_PRED,above,aboveLeft,new short[0],left,p);
    }

    @Test
    public void testLD() {
        short above[] = { 92, 92, 92, 92  };
        short left[] = { 88, 87, 83, 79 };
        short aboveLeft=81;
        short aboveRight[]={85, 85, 85, 85};
        short[] p = { 92, 92, 90, 87, 92, 90, 87, 85, 90, 87, 85, 85, 87, 85, 85, 85 };
        /*-----------------------------------------------------*/
        testBPred(BPredictionMode.B_LD_PRED,above,aboveLeft,aboveRight,left,p);
    }

    @Test
    public void testVL() {
        short above[] = { 87, 85, 83, 73 };
        short left[] = { 94, 83, 81, 87 };
        short aboveLeft=104;
        short aboveRight[]={61, 61, 61, 61};
        short[] p = { 86, 84, 78, 67, 85, 81, 73, 64, 84, 78, 67, 61, 81, 73, 64, 61 };
        /*-----------------------------------------------------*/
        testBPred(BPredictionMode.B_VL_PRED,above,aboveLeft,aboveRight,left,p);
    }

    private void testBPred(BPredictionMode mode, short[] above, short aboveLeft, short[] aboveRight, short[] left, short[] expectedOut) {
        // Transform inputs to new format
        short[] aboveFull=new short[1+above.length+aboveRight.length];
        aboveFull[0]=aboveLeft;
        System.arraycopy(above,0,aboveFull,1,above.length);
        System.arraycopy(aboveRight,0,aboveFull,1+above.length,aboveRight.length);
        short[] result=new short[expectedOut.length];
        // Execute prediction in new way
        AllIntraPred.bpred[mode.ordinal()].call(FullAccessIntArrPointer.toPointer(result),4,new ReadOnlyIntArrPointer(aboveFull,1), new ReadOnlyIntArrPointer(left,0));
        // We can make the assertion now
        Assert.assertArrayEquals(expectedOut, result);
    }
}
