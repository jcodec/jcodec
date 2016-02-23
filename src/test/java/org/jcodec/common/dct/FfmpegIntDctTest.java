package org.jcodec.common.dct;

import org.jcodec.common.tools.Debug;
import org.junit.Ignore;
import org.junit.Test;

import static java.lang.System.arraycopy;
import static java.lang.System.currentTimeMillis;

public class FfmpegIntDctTest {
    
    @Test
    @Ignore
    public void testPerformance() throws Exception {
        short[] input = new short[] { -416, -33, -60, 32, 48, -40, 0, 0, 0,
                -24, -56, 19, 26, 0, 0, 0, -42, 13, 80, -24, -40, 0, 0, 0, -56,
                17, 44, -29, 0, 0, 0, 0, 18, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        FfmpegIntDct dct = new FfmpegIntDct();
        int count = 40000000;
        long start = currentTimeMillis();
        short copy[] = new short[64];
        for (int i = 0; i < count; i++) {
            arraycopy(input, 0, copy, 0, 64);
            dct.decode(copy);
        }
        long time = currentTimeMillis() - start;
        long kdctPerSec = count / time;
        System.out.println(kdctPerSec + "kdct/sec");
    }

    @Test
    public void testIdct() throws Exception {
        short[] input = new short[] { -416, -33, -60, 32, 48, -40, 0, 0, 0,
                -24, -56, 19, 26, 0, 0, 0, -42, 13, 80, -24, -40, 0, 0, 0, -56,
                17, 44, -29, 0, 0, 0, 0, 18, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        int[] expectedOutput = new int[] { 60, 63, 55, 58, 70, 61, 58, 80, 58,
                56, 56, 83, 108, 88, 63, 71, 60, 52, 62, 113, 150, 116, 70, 67,
                66, 56, 68, 122, 156, 116, 69, 72, 69, 62, 65, 100, 120, 86,
                59, 76, 68, 68, 61, 68, 78, 60, 53, 78, 74, 82, 67, 54, 63, 64,
                65, 83, 83, 96, 77, 56, 70, 83, 83, 89 };

        short[] output = new FfmpegIntDct().decode(input);
//        for (int i = 0; i < output.length; i++) {
//            output[i] = (short) IntDCT.range_limit(output[i]);
//        }
        Debug.print8x8(expectedOutput);
        Debug.print8x8(output);
    }

}
