package org.jcodec.codecs.h264.encode;

import static org.jcodec.codecs.h264.H264Const.BLK_DISP_MAP;

import org.jcodec.codecs.h264.decode.Intra4x4PredictionBuilder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.junit.Assert;
import org.junit.Test;

public class IntraPredEstimatorTest {

    @Test
    public void testLumaPred4x4() {

        int[] pred = new int[] { 0, 7, 8, 6, 6, 6, 7, 2, 2, 2, 4, 5, 3, 8, 3, 7 };
        for (int i = 0; i < 10000; i++) {
            for (int mbY = 0; mbY < 2; mbY++) {
                for (int mbX = 0; mbX < 2; mbX++) {
                    for (int bInd = 0; bInd < 16; bInd++) {
                        int dInd = BLK_DISP_MAP[bInd];
                        boolean hasLeft = (dInd & 0x3) != 0 || mbX != 0;
                        boolean hasTop = dInd >= 4 || mbY != 0;
                        do {
                            pred[bInd] = Math.min(8, (int) (Math.random() * 9));
                        } while (!Intra4x4PredictionBuilder.available(pred[bInd], hasLeft, hasTop));
//                        System.out.print(String.format("%d ", pred[bInd]));
                    }
//                    System.out.println(String.format(": %d,%d", mbX, mbY));
                    oneTest(pred, mbX, mbY);
                }
            }
        }
    }

    private void oneTest(int[] pred, int mbX, int mbY) {
        Picture pic0 = Picture.create(16, 16, ColorSpace.YUV420), pic1 = Picture.create(16, 16, ColorSpace.YUV420);
        Picture big = Picture.create(32, 32, ColorSpace.YUV420);

        byte[] topLine0 = new byte[32];
        byte[] topLine1 = new byte[32];
        byte[] topLine2 = new byte[32];
        byte[] leftRow0 = new byte[16];
        byte[] leftRow1 = new byte[16];
        byte[] leftRow2 = new byte[16];
        byte[] tl0 = new byte[4];
        byte[] tl1 = new byte[4];
        byte[] tl2 = new byte[4];
        EncodingContext ctx = new EncodingContext(2, 2);
        int mbOff = mbX << 4;
        for (int i = 0; i < 16; i++) {
            ctx.topLine[0][mbOff + i] = topLine0[mbOff + i] = topLine1[mbOff + i] = topLine2[mbOff + i] = (byte) (16 * i + 75);
            ctx.leftRow[0][i] = leftRow0[i] = leftRow1[i] = leftRow2[i] = (byte) (15 * i + 33);
        }
        ctx.topLeft[0] = tl0[0] = tl1[0] = tl2[0] = 127;
        ctx.topLeft[1] = tl0[1] = tl1[1] = tl2[1] = ctx.leftRow[0][3];
        ctx.topLeft[2] = tl0[2] = tl1[2] = tl2[2] = ctx.leftRow[0][7];
        ctx.topLeft[3] = tl0[3] = tl1[3] = tl2[3] = ctx.leftRow[0][11];
        int[] residual = new int[16];

        for (int i8x8 = 0; i8x8 < 4; i8x8++) {
            for (int i4x4 = 0; i4x4 < 4; i4x4++) {
                int bInd = (i8x8 << 2) + i4x4;
                int dInd = BLK_DISP_MAP[bInd];
                boolean trAvailable = true;
                if ((dInd <= 3 && mbY == 0) || (dInd == 3 && mbX == 1) || dInd == 7 || dInd == 11 || dInd == 15 || dInd == 5 || dInd == 13)
                    trAvailable = false;
                Intra4x4PredictionBuilder.predictWithMode(pred[bInd], residual, (dInd & 0x3) != 0 || mbX != 0, dInd >= 4 || mbY != 0, trAvailable, leftRow0,
                        topLine0, tl0, mbOff, (dInd & 0x3) << 2, (dInd >> 2) << 2, pic0.getPlaneData(0));
            }
        }

        MBEncoderHelper.putBlkPic(big, pic0, mbX << 4, mbY << 4);
        int[] out = IntraPredEstimator.getLumaPred4x4(big, ctx, mbX, mbY, 24);

        for (int i8x8 = 0; i8x8 < 4; i8x8++) {
            for (int i4x4 = 0; i4x4 < 4; i4x4++) {
                int bInd = (i8x8 << 2) + i4x4;
                int dInd = BLK_DISP_MAP[bInd];
                boolean trAvailable = true;
                if ((dInd <= 3 && mbY == 0) || (dInd == 3 && mbX == 1) || dInd == 7 || dInd == 11 || dInd == 15 || dInd == 5 || dInd == 13)
                        trAvailable = false;
                Intra4x4PredictionBuilder.predictWithMode(out[bInd], residual, (dInd & 0x3) != 0 || mbX != 0, dInd >= 4 || mbY != 0, trAvailable, leftRow1,
                        topLine1, tl1, mbOff, (dInd & 0x3) << 2, (dInd >> 2) << 2, pic0.getPlaneData(0));
                Intra4x4PredictionBuilder.predictWithMode(pred[bInd], residual, (dInd & 0x3) != 0 || mbX != 0, dInd >= 4 || mbY != 0, trAvailable, leftRow2,
                        topLine2, tl2, mbOff, (dInd & 0x3) << 2, (dInd >> 2) << 2, pic1.getPlaneData(0));
            }
        }
        Assert.assertArrayEquals(pic0.getPlaneData(0), pic1.getPlaneData(0));
    }

    public void testLumaMode() {

    }
}
