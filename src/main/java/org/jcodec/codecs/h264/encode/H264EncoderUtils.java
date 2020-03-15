package org.jcodec.codecs.h264.encode;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.jcodec.codecs.h264.H264Utils.Mv.mvC;
import static org.jcodec.codecs.h264.H264Utils.Mv.mvRef;

/**
 * Contains utility functions commonly used in H264 encoder
 * 
 * @author Stanislav Vitvitskyy
 */
public class H264EncoderUtils {
    public static int median(int a, boolean ar, int b, boolean br, int c, boolean cr, int d, boolean dr, boolean aAvb,
            boolean bAvb, boolean cAvb, boolean dAvb) {
        ar &= aAvb;
        br &= bAvb;
        cr &= cAvb;

        if (!cAvb) {
            c = d;
            cr = dr;
            cAvb = dAvb;
        }

        if (aAvb && !bAvb && !cAvb) {
            b = c = a;
            bAvb = cAvb = aAvb;
        }

        a = aAvb ? a : 0;
        b = bAvb ? b : 0;
        c = cAvb ? c : 0;

        if (ar && !br && !cr)
            return a;
        else if (br && !ar && !cr)
            return b;
        else if (cr && !ar && !br)
            return c;

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
