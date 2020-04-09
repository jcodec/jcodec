package org.jcodec.codecs.h264.encode;

import org.jcodec.Utils;
import org.jcodec.codecs.h264.decode.BlockInterpolator;
import org.jcodec.common.model.Picture;
import org.junit.Assert;
import org.junit.Test;

public class MotionEstimatorTest {
    @Test
    public void testMotionEstimator() {
        Picture pic = Utils.buildSmoothRandomPic(22 + 32, 22 + 32, 50, 0.2);
        Picture out = Picture.create(16, 16, pic.getColor());
        BlockInterpolator intr = new BlockInterpolator();
        for (int v = -2; v <= 2; v += 1) {
            for (int h = -2; h <= 2; h += 1) {
                for (int i = 0; i < 9; i++) {
                    int mbX = i % 3;
                    int mbY = i / 3;
                    intr.getBlockLuma(pic, out, 0, (mbX << 6) + 12 + h, (mbY << 6) + 12 + v, 16, 16);
                    int[] qPix = MotionEstimator.estimateQPix(pic, out.getPlaneData(0), new int[] { 12, 12 }, mbX, mbY);
                    Assert.assertEquals(12 + h, qPix[0]);
                    Assert.assertEquals(12 + v, qPix[1]);
                }
            }
        }
    }
}
