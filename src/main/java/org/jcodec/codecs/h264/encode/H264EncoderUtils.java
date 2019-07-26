package org.jcodec.codecs.h264.encode;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Contains utility functions commonly used in H264 encoder
 * @author Stanislav Vitvitskyy
 */
public class H264EncoderUtils {
    public static int median(int a, int b, int c, int d, boolean aAvb, boolean bAvb, boolean cAvb, boolean dAvb) {
        if (!cAvb) {
            c = d;
            cAvb = dAvb;
        }

        if (aAvb && !bAvb && !cAvb) {
            b = c = a;
            bAvb = cAvb = aAvb;
        }

        a = aAvb ? a : 0;
        b = bAvb ? b : 0;
        c = cAvb ? c : 0;

        return a + b + c - min(min(a, b), c) - max(max(a, b), c);
    }

    public static int mse(int[] orig, int[] enc, int w, int h) {
        int sum = 0;
        for (int i = 0, off = 0; i < h; i++) {
            for (int j = 0; j < w; j++, off++) {
                int diff = orig[off] - enc[off];
                sum += diff * diff;
            }
        }
        return sum / (w * h);
    }
}
