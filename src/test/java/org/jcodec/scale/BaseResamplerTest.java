package org.jcodec.scale;

import org.junit.Assert;
import org.junit.Test;

public class BaseResamplerTest {
    @Test
    public void testNormalize() {
        double[][] taps = new double[][] { { 0.35, 0.51, 0.74, -0.23 }, { -0.11, 0.53, 0.73, -0.15 },
                { -0.15, 0.13, 0.99, 0.15 } };
        double[][] normalized = new double[][] { 
                { 32.700729927, 47.6496350365, 69.1386861314, -21.4890510949 },
                { -14.08, 67.84, 93.44, -19.2 },
                { -17.1428571429, 14.8571428571, 113.1428571429, 17.1428571429 } };
        short[][] fixed128 = new short[][] { { 33, 48, 69, -22 }, { -14, 68, 93, -19 }, { -17, 15, 113, 17 } };

        short[] out = new short[4];
        for (int i = 0; i < taps.length; i++) {
            BaseResampler.normalizeAndGenerateFixedPrecision(taps[i], 7, out);
            Assert.assertArrayEquals(fixed128[i], out);
            Assert.assertArrayEquals(normalized[i], taps[i], 0.0001);
        }
    }

}
