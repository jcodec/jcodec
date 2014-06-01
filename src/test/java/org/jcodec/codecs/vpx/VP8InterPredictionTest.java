package org.jcodec.codecs.vpx;

import org.junit.Assert;
import org.junit.Test;

public class VP8InterPredictionTest {
    int[] block = new int[] {
         // @formatter:off
         0, 8, 16, 24, 32, 40, 48, 56,
         8, 16, 24, 32, 40, 48, 56, 64,
         16, 24, 32, 40, 48, 56, 64, 72,
         24, 32, 40, 48, 56, 64, 72, 80,
         32, 40, 48, 56, 64, 72, 80, 88,
         40, 48, 56, 64, 72, 80, 88, 96,
         48, 56, 64, 72, 80, 88, 96, 104,
         56, 64, 72, 80, 88, 96, 104, 112
         // @formatter:on
    };

    @Test
    public void testInterp() {

        int[] dst = new int[25];
        int[] cmp = new int[] {
// @formatter:off
                34, 42, 50, 58, 56,
                42, 50, 58, 66, 64,
                50, 58, 66, 74, 72,
                58, 66, 74, 82, 80,
                59, 67, 75, 83, 88
// @formatter:on
        };
        VP8InterPrediction.getBlock(block, 8, 8, dst, 0, 5, 11, 23, 4, 4);
        Assert.assertArrayEquals(cmp, dst);
    }

    @Test
    public void testInterpUnsafe1() {

        int[] dst = new int[25];
        int[] cmp = new int[] {
// @formatter:off
                 0,  0,  5, 13, 16,
                 0,  0,  5, 13, 16,
                 3,  3,  8, 16, 16,
                11, 11, 16, 24, 24,
                16, 16, 21, 29, 32
// @formatter:on
        };
        VP8InterPrediction.getBlock(block, 8, 8, dst, 0, 5, -11, -13, 4, 4);
        Assert.assertArrayEquals(cmp, dst);
    }

}
