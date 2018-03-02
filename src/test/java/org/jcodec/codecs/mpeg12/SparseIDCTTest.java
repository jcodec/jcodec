package org.jcodec.codecs.mpeg12;
import org.jcodec.common.dct.DCTRef;
import org.jcodec.common.dct.SimpleIDCT10Bit;
import org.jcodec.common.dct.SparseIDCT;
import org.jcodec.platform.Platform;
import org.junit.Assert;
import org.junit.Test;

import java.lang.System;

public class SparseIDCTTest {

    @Test
    public void testSparseIDCT() {
        long count = 0, n = 1000000;
        for (int i = 0; i < 10000; i++)
            count += oneTest();

        System.out.println((100 * count) / (64 * n));
    }

    private int oneTest() {
        int[] pixels = new int[64];
        for (int i = 0; i < 64; i++) {
            pixels[i] = (int) (Math.random() * 256);
        }

        int[] dct = Platform.copyOfInt(pixels, 64);

        DCTRef.fdct(dct, 0);

        int[] refidct = Platform.copyOfInt(dct, 64);
        SimpleIDCT10Bit.idct10(refidct, 0);

        int[] sparse = new int[64];
        SparseIDCT.start(sparse, dct[0]);
        for (int i = 1; i < 64; i++)
            SparseIDCT.coeff(sparse, i, dct[i]);
        SparseIDCT.finish(sparse);

        int max = 0, count = 0;
        for (int i = 0; i < 64; i++) {
            int diff = Math.abs(sparse[i] - refidct[i]);
            if (diff > max)
                max = diff;
            if (diff != 0)
                count++;
        }
        Assert.assertTrue(max < 2);
        return count;
    }
}
