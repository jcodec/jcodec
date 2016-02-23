package org.jcodec.codecs.mjpeg;

import static junit.framework.Assert.assertTrue;

import org.jcodec.codecs.mjpeg.tools.Asserts;
import org.jcodec.common.dct.DCT;
import org.jcodec.common.dct.IntDCT;
import org.jcodec.common.dct.SlowDCT;
import org.jcodec.common.tools.Debug;
import org.jcodec.platform.Platform;
import org.junit.Test;

public class DctTest {
    static int[] input = new int[] { -416, -33, -60, 32, 48, -40, 0, 0, 0, -24,
            -56, 19, 26, 0, 0, 0, -42, 13, 80, -24, -40, 0, 0, 0, -56, 17, 44,
            -29, 0, 0, 0, 0, 18, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    static int[] expectedOutput = new int[] { 60, 63, 55, 58, 70, 61, 58, 80,
            58, 56, 56, 83, 108, 88, 63, 71, 60, 52, 62, 113, 150, 116, 70, 67,
            66, 56, 68, 122, 156, 116, 69, 72, 69, 62, 65, 100, 120, 86, 59,
            76, 68, 68, 61, 68, 78, 60, 53, 78, 74, 82, 67, 54, 63, 64, 65, 83,
            83, 96, 77, 56, 70, 83, 83, 89 };

    static int[] input2 = new int[] { -964, -18, 3, 0, 0, 0, 0, 0, -36, -9, 6,
            -4, 0, 0, 0, 0, 6, 21, 6, 0, 0, 0, 0, 0, -20, 8, -16, 0, 0, 0, 0,
            0, 15, 5, -12, 16, 0, 0, 0, 0, -7, -6, 16, -10, 0, 0, 0, 0, 8, 0,
            0, 0, 0, 0, 0, 0, 0, -8, 0, 0, 0, 0, 0, 0, };
    static int[] expectedOutput2 = new int[] { 2, 2, 1, 1, 1, 1, 1, 0, 0, 1, 1,
            0, 0, 1, 3, 5, 0, 1, 1, 0, 0, 6, 18, 27, 3, 2, 1, 3, 7, 11, 15, 18,
            0, 0, 3, 12, 20, 20, 11, 2, 1, 5, 9, 11, 9, 7, 6, 6, 7, 9, 9, 5, 1,
            3, 11, 19, 29, 23, 18, 19, 23, 24, 18, 12, };

    static int[] input3 = new int[] { -556, -51, 6, -8, 5, 0, 0, 0, -6, 3, 3,
            -8, -5, 0, 0, 0, -33, 3, 36, -8, 0, 16, -10, 0, -8, -24, 4, 0, 8,
            0, 0, 0, 0, 0, -12, 8, 0, -14, 0, 0, 0, -6, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, };
    static int[] expectedOutput3 = new int[] { 43, 44, 41, 45, 44, 48, 67, 75,
            54, 52, 55, 54, 44, 50, 67, 69, 55, 59, 65, 63, 59, 63, 65, 65, 46,
            58, 64, 65, 80, 77, 59, 66, 44, 52, 57, 64, 81, 77, 60, 71, 48, 43,
            51, 60, 63, 66, 70, 78, 53, 45, 48, 55, 51, 56, 72, 75, 58, 55, 49,
            52, 51, 52, 63, 62, };

    @Test
    public void testIntDct() throws Exception {
        DCT dct = IntDCT.INSTANCE;
        doTestRelaxed(dct, input, expectedOutput);
        doTestRelaxed(dct, input2, expectedOutput2);
        doTestRelaxed(dct, input3, expectedOutput3);
    }

    @Test
    public void testSlowDct() throws Exception {
        DCT dct = SlowDCT.INSTANCE;
        doTestStrict(dct, input, expectedOutput);
        doTestStrict(dct, input2, expectedOutput2);
        doTestStrict(dct, input3, expectedOutput3);
    }

    private void doTestRelaxed(DCT dct, int[] input, int[] expected) {
        int[] output = dct.decode(input);
        Asserts.assertEpsilonEquals(expected, output, 1);
    }

    private void doTestStrict(DCT dct, int[] input, int[] expected) {
        int[] output = dct.decode(input);
        boolean equals = Platform.arrayEquals(expected, output);
        if (!equals) {
            Debug.print8x8(expected);
            System.out.println();
            Debug.print8x8(output);
        }
        assertTrue(equals);
    }

   
}
