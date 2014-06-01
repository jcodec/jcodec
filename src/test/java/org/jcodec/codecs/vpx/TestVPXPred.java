package org.jcodec.codecs.vpx;

import org.jcodec.codecs.vp8.VP8Util;
import org.junit.Assert;
import org.junit.Test;

public class TestVPXPred {

    @Test
    public void testPredHU() {

        int[] left = new int[] { 110, 120, 130, 140 };
        int[] groundTruth = VP8Util.predictHU(left);
        int[] result = new int[16];

        VPXPred.predictHU(result, left[3], left[2], left[1], left[0]);

        Assert.assertArrayEquals(groundTruth, result);

    }

    @Test
    public void testPredHD() {

        int[] left = new int[] { 110, 120, 130, 140 };
        int[] tl = new int[] {15};
        int[] top = new int[] { 220, 230, 240, 250 };
        int[] groundTruth = VP8Util.predictHD(top, left, tl[0]);
        int[] result = new int[16];

        VPXPred.vp8BPred(VPXConst.B_HD_PRED, left, top, 250, tl, 0, 0, result);

        Assert.assertArrayEquals(groundTruth, result);

    }

    @Test
    public void testPredVL() {

        int[] left = new int[] { 110, 120, 130, 140 };
        int[] tl = new int[] {15};
        int[] top = new int[] { 110, 120, 130, 140, 150, 160, 170, 180 };
        int[] tr = new int[] { 150, 160, 170, 180 };
        int[] groundTruth = VP8Util.predictVL(top, tr);
        int[] result = new int[16];

        VPXPred.vp8BPred(VPXConst.B_VL_PRED, left, top, 140, tl, 0, 0, result);

        Assert.assertArrayEquals(groundTruth, result);

    }

    @Test
    public void testPredVR() {

        int[] left = new int[] { 110, 120, 130, 140 };
        int[] tl = new int[] {15};
        int[] top = new int[] { 220, 230, 240, 250 };

        int[] groundTruth = VP8Util.predictVR(top, left, tl[0]);
        int[] result = new int[16];

        VPXPred.vp8BPred(VPXConst.B_VR_PRED, left, top, 250, tl, 0, 0, result);

        Assert.assertArrayEquals(groundTruth, result);

    }

    @Test
    public void testPredRD() {

        int[] left = new int[] { 110, 120, 130, 140 };
        int[] tl = new int[] {15};
        int[] top = new int[] { 220, 230, 240, 250 };
        int[] groundTruth = VP8Util.predictRD(top, left, tl[0]);
        int[] result = new int[16];

        VPXPred.vp8BPred(VPXConst.B_RD_PRED, left, top, 250, tl, 0, 0, result);

        Assert.assertArrayEquals(groundTruth, result);

    }

    @Test
    public void testPredLD() {

        int[] left = new int[] { 110, 120, 130, 140 };
        int[] tl = new int[] {15};
        int[] top = new int[] { 110, 120, 130, 140, 150, 160, 170, 180 };
        int[] tr = new int[] { 150, 160, 170, 180 };
        int[] groundTruth = VP8Util.predictLD(top, tr);
        int[] result = new int[16];

        VPXPred.vp8BPred(VPXConst.B_LD_PRED, left, top, 140, tl, 0, 0, result);

        Assert.assertArrayEquals(groundTruth, result);

    }

    @Test
    public void testPredHE() {

        int[] left = new int[] { 110, 120, 130, 140 };
        int[] tl = new int[] {15};
        int[] top = new int[] { 110, 120, 130, 140 };
        int[] groundTruth = VP8Util.predictHE(left, tl[0]);
        int[] result = new int[16];

        VPXPred.vp8BPred(VPXConst.B_HE_PRED, left, top, 140, tl, 0, 0, result);

        Assert.assertArrayEquals(groundTruth, result);

    }

    @Test
    public void testPredVE() {

        int[] tl = new int[] {15};
        int[] left = new int[] { 110, 120, 130, 140 };
        int[] top = new int[] { 110, 120, 130, 140, 150, 160, 170, 180 };
        int[] tr = new int[] { 150, 160, 170, 180 };
        int[] groundTruth = VP8Util.predictVE(top, tl[0], tr);
        int[] result = new int[16];

        VPXPred.vp8BPred(VPXConst.B_VE_PRED, left, top, 140, tl, 0, 0, result);

        Assert.assertArrayEquals(groundTruth, result);

    }

    @Test
    public void testPredTM() {

        int[] left = new int[] { 0, 0, 0, 0, 110, 120, 130, 140 };
        int[] l1 = new int[] { 110, 120, 130, 140 };
        int[] tl = new int[] { 0, 0, 0, 0, 15, 110, 120, 130 };
        int[] t1 = new int[] { 220, 230, 240, 250 };
        int[] top = new int[] { 0, 0, 0, 0, 220, 230, 240, 250 };
        int[] groundTruth = VP8Util.predictTM(t1, l1, tl[4]);
        int[] result = new int[16];

        VPXPred.vp8BPred(VPXConst.B_TM_PRED, left, top, 250, tl, 4, 4, result);

        Assert.assertArrayEquals(groundTruth, result);

    }

    @Test
    public void testPredDC() {

        int[] left = new int[] { 110, 120, 130, 140 };
        int[] tl = new int[] {15};
        int[] top = new int[] { 220, 230, 240, 250 };
        int[] groundTruth = VP8Util.predictDC(top, left);
        int[] result = new int[16];

        VPXPred.vp8BPred(VPXConst.B_DC_PRED, left, top, 250, tl, 0, 0, result);

        Assert.assertArrayEquals(groundTruth, result);

    }

}
