package org.jcodec.common.dct;
import static java.lang.System.currentTimeMillis;
import static org.jcodec.common.dct.IntDCT.DESCALE;
import static org.jcodec.common.dct.IntDCT.range_limit;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.System;
import java.nio.IntBuffer;

public class IntDCTTest {
    
    @Test
    @Ignore
    public void _testPerformance() throws Exception {
        int[] input = new int[] { -416, -33, -60, 32, 48, -40, 0, 0, 0, -24,
                -56, 19, 26, 0, 0, 0, -42, 13, 80, -24, -40, 0, 0, 0, -56, 17,
                44, -29, 0, 0, 0, 0, 18, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        IntDCT dct = new IntDCT();
        int count = 40000000;
        IntBuffer ws = IntBuffer.allocate(64);
        IntBuffer out = IntBuffer.allocate(64);
        IntBuffer inptr = IntBuffer.wrap(input);
        long start = currentTimeMillis();
        for (int i = 0; i < count; i++) {
            ws.clear();
            out.clear();
            inptr.clear();
            dct.doDecode(inptr, ws, out);
        }
        long time = currentTimeMillis() - start;
        long kdctPerSec = count / time;
        System.out.println(kdctPerSec + "kdct/sec");
    }
    
    @Test
    public void testRangeLimit() {
        Assert.assertEquals(-128 & 0xff, range_limit(0));
        Assert.assertEquals(-127 & 0xff, range_limit(1));
        Assert.assertEquals(-1 & 0xff, range_limit(133));
        Assert.assertEquals(126, range_limit(1022));

        for (int i = -256; i < 1024; i++) {
            System.out.printf("range_limit(%4d) == %4d\n", i, range_limit(i));
        }
    }
    
    @Test
    public void testDescale() {
        Assert.assertEquals(-4063, DESCALE(-8321313, 11));
        Assert.assertEquals(18, DESCALE(36362, 11));
    }

}
