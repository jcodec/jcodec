package org.jcodec.codecs.mjpeg;

import org.jcodec.api.UnhandledStateException;
import org.jcodec.common.dct.DCT;
import org.jcodec.common.dct.IntDCT;
import org.jcodec.common.dct.SlowDCT;
import org.jcodec.common.tools.Debug;
import org.jcodec.platform.Platform;

public class VerifyDCT extends DCT {

    private static SlowDCT slow = SlowDCT.INSTANCE;
    private static IntDCT fast = IntDCT.INSTANCE;

    static int diffcnt = 0;

    public int[] decode(int[] orig) {
        int[] expected = slow.decode(orig);
        int[] actual = fast.decode(orig);
        if (!Platform.arrayEqualsInt(expected, actual)) {
            System.out.println("\nwhile decoding: ");
            Debug.print8x8i(orig);
            System.out.println("expected: ");
            Debug.print8x8i(expected);
            System.out.println("actual: ");
            Debug.print8x8i(actual);
            System.out.println("diff: ");
            for (int i = 0; i < expected.length; i++) {
                if (i % 8 == 0) {
                    System.out.println();
                }
                System.out.printf("%3d, ", (expected[i] - actual[i]));
            }
            diffcnt++;

            if (diffcnt == 10) {
                throw new UnhandledStateException();
            }
        }
        return expected;
    }

}
