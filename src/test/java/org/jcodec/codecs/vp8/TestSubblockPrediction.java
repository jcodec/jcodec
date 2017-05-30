package org.jcodec.codecs.vp8;

import org.jcodec.codecs.vpx.VP8Util;
import org.junit.Assert;
import org.junit.Test;

public class TestSubblockPrediction {

    @Test
    public void testVR() {
        int above[] = { 46, 50, 55, 60 };
        int left[] = { 40, 37, 35, 35 };
        int aboveLeft = 42;
        int aboveRight[] = { 68, 68, 68, 68 };
        int[] p = { 44, 48, 53, 58, 43, 46, 50, 55, 40, 44, 48, 53, 37, 43, 46, 50 };
        /*-----------------------------------------------------*/
        Assert.assertArrayEquals(p, VP8Util.predictVR(above, left, aboveLeft));
    }

    @Test
    public void testHD() {
        int above[] = { 40, 40, 41, 42 };
        int left[] = { 35, 35, 35, 35 };
        int aboveLeft = 36;
        int aboveRight[] = { 46, 50, 55, 60 };
        int[] p = { 36, 37, 39, 40, 35, 35, 36, 37, 35, 35, 35, 35, 35, 35, 35, 35 };
        /*-----------------------------------------------------*/
        Assert.assertArrayEquals(p, VP8Util.predictHD(above, left, aboveLeft));
    }

    @Test
    public void testHU() {
        int above[] = { 48, 48, 47, 43 };
        int left[] = { 17, 17, 17, 17 };
        int aboveLeft = 48;
        int aboveRight[] = { 38, 38, 38, 38 };
        int[] p = { 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17 };
        /*-----------------------------------------------------*/
        Assert.assertArrayEquals(p, VP8Util.predictHU(left));
    }

    @Test
    public void testRD() {
        int above[] = { 39, 37, 37, 37 };
        int left[] = { 48, 48, 48, 48 };
        int aboveLeft = 43;
        int aboveRight[] = { 37, 37, 37, 37 };
        int[] p = { 43, 40, 38, 37, 47, 43, 40, 38, 48, 47, 43, 40, 48, 48, 47, 43 };
        /*-----------------------------------------------------*/
        Assert.assertArrayEquals(p, VP8Util.predictRD(above, left, aboveLeft));
    }

    @Test
    public void testVE() {
        int above[] = { 38, 37, 37, 37 };
        int aboveLeft = 41;
        int aboveRight[] = { 37, 37, 37, 37 };
        int[] p = { 39, 37, 37, 37, 39, 37, 37, 37, 39, 37, 37, 37, 39, 37, 37, 37 };
        Assert.assertArrayEquals(p, VP8Util.predictVE(above, aboveLeft, aboveRight));

        /*-----------------------------------------------------*/

    }

    @Test
    public void testHE() {
        int above[] = { 48, 48, 48, 48 };
        int left[] = { 40, 40, 40, 39 };
        int aboveLeft = 44;
        int aboveRight[] = { 45, 45, 45, 45 };
        int[] p = { 41, 41, 41, 41, 40, 40, 40, 40, 40, 40, 40, 40, 39, 39, 39, 39 };
        /*-----------------------------------------------------*/
        Assert.assertArrayEquals(p, VP8Util.predictHE(left, aboveLeft));
    }

    @Test
    public void testDC() {
        int above[] = { 39, 39, 39, 39 };
        int left[] = { 38, 40, 59, 126 };
        int[] p = { 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52 };
        /*-----------------------------------------------------*/
        Assert.assertArrayEquals(p, VP8Util.predictDC(above, left));
    }

    @Test
    public void testTM() {
        int above[] = { 41, 41, 41, 41 };
        int left[] = { 37, 48, 104, 180 };
        int aboveLeft = 41;
        int[] p = { 37, 37, 37, 37, 48, 48, 48, 48, 104, 104, 104, 104, 180, 180, 180, 180 };
        /*-----------------------------------------------------*/
        Assert.assertArrayEquals(p, VP8Util.predictTM(above, left, aboveLeft));
    }

    @Test
    public void testLD() {
        int above[] = { 92, 92, 92, 92 };
        int left[] = { 88, 87, 83, 79 };
        int aboveLeft = 81;
        int aboveRight[] = { 85, 85, 85, 85 };
        int[] p = { 92, 92, 90, 87, 92, 90, 87, 85, 90, 87, 85, 85, 87, 85, 85, 85 };
        /*-----------------------------------------------------*/
        Assert.assertArrayEquals(p, VP8Util.predictLD(above, aboveRight));
    }

    @Test
    public void testVL() {
        int above[] = { 87, 85, 83, 73 };
        int left[] = { 94, 83, 81, 87 };
        int aboveLeft = 104;
        int aboveRight[] = { 61, 61, 61, 61 };
        int[] p = { 86, 84, 78, 67, 85, 81, 73, 64, 84, 78, 67, 61, 81, 73, 64, 61 };
        /*-----------------------------------------------------*/
        Assert.assertArrayEquals(p, VP8Util.predictVL(above, aboveRight));

    }
}
