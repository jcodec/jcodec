package org.jcodec.codecs.mpa;

import static org.jcodec.codecs.mpa.MpaConst.win;

import org.jcodec.common.ArrayUtil;
import org.junit.Assert;
import org.junit.Test;

public class Mdct {

    private static final int D8 = 1023;
    private static final int D7 = 1015;
    private static final int D6 = 1000;
    private static final int D5 = 977;
    private static final int D4 = 946;
    private static final int D3 = 908;
    private static final int D2 = 864;
    private static final int D1 = 812;
    private static final int D0 = 755;
    private static final int W8 = 45;
    private static final int W7 = 134;
    private static final int W6 = 222;
    private static final int W5 = 308;
    private static final int W4 = 392;
    private static final int W3 = 473;
    private static final int W2 = 550;
    private static final int W0 = 692;
    private static final int W1 = 623;

    // Math.cos((n + 9.5d) * (Math.PI / 18) * (k + 0.5d))
    static final int[][] mdct18Lookup = {
            { W0, W1, W2, W3, W4, W5, W6, W7, W8, -W8, -W7, -W6, -W5, -W4, -W3, -W2, -W1, -W0, -D0, -D1, -D2, -D3, -D4,
                    -D5, -D6, -D7, -D8, -D8, -D7, -D6, -D5, -D4, -D3, -D2, -D1, -D0, },
            { -D1, -D4, -D7, -D7, -D4, -D1, -W1, -W4, -W7, W7, W4, W1, D1, D4, D7, D7, D4, D1, W1, W4, W7, -W7, -W4,
                    -W1, -D1, -D4, -D7, -D7, -D4, -D1, -W1, -W4, -W7, W7, W4, W1, },
            { -W2, -W7, W5, W0, D4, D8, D3, W1, W6, -W6, -W1, -D3, -D8, -D4, -W0, -W5, W7, W2, D2, D7, D5, D0, W4, -W8,
                    -W3, -D1, -D6, -D6, -D1, -W3, -W8, W4, D0, D5, D7, D2, },
            { D3, D7, D0, W6, -W4, -D2, -D8, -D1, -W5, W5, D1, D8, D2, W4, -W6, -D0, -D7, -D3, -W3, W7, W0, D6, D4, W2,
                    -W8, -W1, -D5, -D5, -W1, -W8, W2, D4, D6, W0, W7, -W3, },
//            { D3, +D7, +D0, +W6, -W4, -D2, -D8, -D1, -W5, +W5, +D1, +D8, +D2, +W4, -W6, -D0, -D7, -D3, -W3, +W7, +W0,
//                    +D6, +D4, +W2, -W8, -W1, -D5, -D5, -W1, -W8, +W2, +D4, +D6, +W0, +W7, -W3 },
            { W4, -W4, -D4, -D4, -W4, W4, D4, D4, W4, -W4, -D4, -D4, -W4, W4, D4, D4, W4, -W4, -D4, -D4, -W4, W4, D4,
                    D4, W4, -W4, -D4, -D4, -W4, W4, D4, D4, W4, -W4, -D4, -D4, },
            { -D5, -D1, W8, D2, D4, W6, -W0, -D7, -W3, W3, D7, W0, -W6, -D4, -D2, -W8, D1, D5, W5, -W1, -D8, -W2, W4,
                    D6, D0, -W7, -D3, -D3, -W7, D0, D6, W4, -W2, -D8, -W1, W5, },
//            { -D5, -D1, +W8, +D2, +D4, +W6, -W0, -D7, -W3, +W3, +D7, +W0, -W6, -D4, -D2, -W8, +D1, +D5, +W5, -W1, -D8,
//                    -W2, +W4, +D6, +D0, -W7, -D3, -D3, -W7, +D0, +D6, +W4, -W2, -D8, -W1, +W5 },                 
            { -W6, D1, D3, -W8, -D4, -D0, W5, D7, W2, -W2, -D7, -W5, D0, D4, W8, -D3, -D1, W6, D6, W1, -W3, -D8, -W4,
                    W0, D5, W7, -D2, -D2, W7, D5, W0, -W4, -D8, -W3, W1, D6, },
//            { -W6, +D1, +D3, -W8, -D4, -D0, +W5, +D7, +W2, -W2, -D7, -W5, +D0, +D4, +W8, -D3, -D1, +W6, +D6, +W1, -W3,
//                    -D8, -W4, +W0, +D5, +W7, -D2, -D2, +W7, +D5, +W0, -W4, -D8, -W3, +W1, +D6 }, 
            { D7, W4, -D1, -D1, W4, D7, W7, -D4, -W1, W1, D4, -W7, -D7, -W4, D1, D1, -W4, -D7, -W7, D4, W1, -W1, -D4,
                    W7, D7, W4, -D1, -D1, W4, D7, W7, -D4, -W1, W1, D4, -W7, },
            { W8, -D7, -W6, D5, W4, -D3, -W2, D1, W0, -W0, -D1, W2, D3, -W4, -D5, W6, D7, -W8, -D8, -W7, D6, W5, -D4,
                    -W3, D2, W1, -D0, -D0, W1, D2, -W3, -D4, W5, D6, -W7, -D8, },
//            { +W8, -D7, -W6, +D5, +W4, -D3, -W2, +D1, +W0, -W0, -D1, +W2, +D3, -W4, -D5, +W6, +D7, -W8, -D8, -W7, +D6,
//                    +W5, -D4, -W3, +D2, +W1, -D0, -D0, +W1, +D2, -W3, -D4, +W5, +D6, -W7, -D8 },
            { -D8, W7, D6, -W5, -D4, W3, D2, -W1, -D0, D0, W1, -D2, -W3, D4, W5, -D6, -W7, D8, -W8, -D7, W6, D5, -W4,
                    -D3, W2, D1, -W0, -W0, D1, W2, -D3, -W4, D5, W6, -D7, -W8, },
//            { -D8, +W7, +D6, -W5, -D4, +W3, +D2, -W1, -D0, +D0, +W1, -D2, -W3, +D4, +W5, -D6, -W7, +D8, -W8, -D7, +W6,
//                    +D5, -W4, -D3, +W2, +D1, -W0, -W0, +D1, +W2, -D3, -W4, +D5, +W6, -D7, -W8 },
            { W7, D4, -W1, -W1, D4, W7, -D7, W4, D1, -D1, -W4, D7, -W7, -D4, W1, W1, -D4, -W7, D7, -W4, -D1, D1, W4,
                    -D7, W7, D4, -W1, -W1, D4, W7, -D7, W4, D1, -D1, -W4, D7, },
            { D6, -W1, -W3, D8, -W4, -W0, D5, -W7, -D2, D2, W7, -D5, W0, W4, -D8, W3, W1, -D6, W6, D1, -D3, -W8, D4,
                    -D0, -W5, D7, -W2, -W2, D7, -W5, -D0, D4, -W8, -D3, D1, W6, },
//            { -D6, -W1, -W3, +D8, -W4, -W0, +D5, -W7, -D2, +D2, +W7, -D5, +W0, +W4, -D8, +W3, +W1, -D6, +W6, +D1, -D3,
//                    -W8, +D4, -D0, -W5, +D7, -W2, -W2, +D7, -W5, -D0, +D4, -W8, -D3, +D1, +W6 },
            { -W5, -W1, D8, -W2, -W4, D6, -D0, -W7, D3, -D3, W7, D0, -D6, W4, W2, -D8, W1, W5, -D5, D1, W8, -D2, D4,
                    -W6, -W0, D7, -W3, -W3, D7, -W0, -W6, D4, -D2, W8, D1, -D5, },
//            { -W5, -W1, +D8, -W2, -W4, +D6, -D0, -W7, +D3, -D3, +W7, +D0, -D6, +W4, +W2, -D8, +W1, +W5, -D5, +D1, +W8,
//                    -D2, +D4, -W6, -W0, +D7, -W3, -W3, +D7, -W0, -W6, +D4, -D2, +W8, +D1, -D5 },
            { -D4, D4, -W4, -W4, D4, -D4, W4, W4, -D4, D4, -W4, -W4, D4, -D4, W4, W4, -D4, D4, -W4, -W4, D4, -D4, W4,
                    W4, -D4, D4, -W4, -W4, D4, -D4, W4, W4, -D4, D4, -W4, -W4, },
            { W3, W7, -W0, D6, -D4, W2, W8, -W1, D5, -D5, W1, -W8, -W2, D4, -D6, W0, -W7, -W3, D3, -D7, D0, -W6, -W4,
                    D2, -D8, D1, -W5, -W5, D1, -D8, D2, -W4, -W6, D0, -D7, D3, },
//            { +W3, +W7, -W0, +D6, -D4, +W2, +W8, -W1, +D5, -D5, +W1, -W8, -W2, +D4, -D6, +W0, -W7, -W3, +D3, -D7, +D0,
//                    -W6, -W4, +D2, -D8, +D1, -W5, -W5, +D1, -D8, +D2, -W4, -W6, +D0, -D7, +D3 },
            { D2, -D7, D5, -D0, W4, W8, -W3, D1, -D6, D6, -D1, W3, -W8, -W4, D0, -D5, D7, -D2, W2, -W7, -W5, W0, -D4,
                    D8, -D3, W1, -W6, -W6, W1, -D3, D8, -D4, W0, -W5, -W7, W2, },
//            { +D2, -D7, +D5, -D0, +W4, +W8, -W3, +D1, -D6, +D6, -D1, +W3, -W8, -W4, +D0, -D5, +D7, -D2, +W2, -W7, -W5,
//                    +W0, -D4, +D8, -D3, +W1, -W6, -W6, +W1, -D3, +D8, -D4, +W0, -W5, -W7, +W2 },
            { -W1, W4, -W7, -W7, W4, -W1, D1, -D4, D7, -D7, D4, -D1, W1, -W4, W7, W7, -W4, W1, -D1, D4, -D7, D7, -D4,
                    D1, -W1, W4, -W7, -W7, W4, -W1, D1, -D4, D7, -D7, D4, -D1, },
            { -D0, D1, -D2, D3, -D4, D5, -D6, D7, -D8, D8, -D7, D6, -D5, D4, -D3, D2, -D1, D0, -W0, W1, -W2, W3, -W4,
                    W5, -W6, W7, -W8, -W8, W7, -W6, W5, -W4, W3, -W2, W1, -W0, },
//            { -D0, +D1, -D2, +D3, -D4, +D5, -D6, +D7, -D8, +D8, -D7, +D6, -D5, +D4, -D3, +D2, -D1, +D0, -W0, +W1, -W2,
//                    +W3, -W4, +W5, -W6, +W7, -W8, -W8, +W7, -W6, +W5, -W4, +W3, -W2, +W1, -W0 },
    
    };

    static final int[] sineWnd = { 45, 134, 222, 308, 392, 473, 550, W1, 692, 755, 812, 864, 908, 946, 977, 1000, 1015,
            1023, 1023, 1015, 1000, 977, 946, 908, 864, 812, 755, 692, W1, 550, 473, 392, 308, 222, 134, 45 };

    public void mdct18(int[] x, int[] out) {
        for (int k = 0; k < 18; k++) {
            int sum = 0;
            for (int n = 0; n < 36; n++) {
                sum += x[n] * mdct18Lookup[k][n];
            }
            out[k] = ((((sum + 512) >> 10) * 114) + 512) >> 10;
        }
    }
    
    public void mdct18Fast(int[] x, int[] out) {
        int x0 = x[0], x1 = x[1], x2 = x[2], x3 = x[3], x4 = x[4], x5 = x[5], x6 = x[6], x7 = x[7], x8 = x[8];
        int y0 = x[9], y1 = x[10], y2 = x[11], y3 = x[12], y4 = x[13], y5 = x[14], y6 = x[15], y7 = x[16], y8 = x[17];
        int t0 = x[18], t1 = x[19], t2 = x[20], t3 = x[21], t4 = x[22], t5 = x[23], t6 = x[24], t7 = x[25], t8 = x[26];
        int u0 = x[27], u1 = x[28], u2 = x[29], u3 = x[30], u4 = x[31], u5 = x[32], u6 = x[33], u7 = x[34], u8 = x[35];
        
        int s0 = x0 - y8;
        int s1 = x1 - y7;
        int s2 = x2 - y6;
        int s3 = x3 - y5;
        int s4 = x4 - y4;
        int s5 = x5 - y3;
        int s6 = x6 - y2;
        int s7 = x7 - y1;
        int s8 = x8 - y0;
        
        int s9  = t0 + u8;
        int s10 = t1 + u7;
        int s11 = t2 + u6;
        int s12 = t3 + u5;
        int s13 = t4 + u4;
        int s14 = t5 + u3;
        int s15 = t6 + u2;
        int s16 = t7 + u1;
        int s17 = t8 + u0;
        int r17 = y5 + y6;
        int r13 = x2 + x3;

        int l0 = s0 + s5;
        int l1 = s1 + s4;
        int e0 = l0 + s15;
        int e1 = l1 + s16;

        int e5 = s11 - s12 - s8;
        int e2 = r17 - s17 - r13;
        int e3 = s6  - s9  + s14;
        int e4 = s10  -s7  - s13 ;

        int f0 = x0 + x5 - x1 - x4 + x8 - y8 - y3 + y7 + y4 - y0 - u1 - u6 + u2 + u5      - t7 - t2 + t3 + t6;
        int f1 = x6 + x7 - x2 - x3      - y1 - y2 + y5 + y6      - u7 - u8 + u3 + u4 - u0 - t8 - t0 - t1 + t4 + t5;
        
        out[4]  = W4 * f0 + D4 * f1;
        out[13] = W4 * f1 - D4 * f0;
        
        out[1]  = D7 * e2 + W4 * e4 - D1 * e0 - W1 * e3 - D4 * e1 + W7 * e5;
        out[7]  = D7 * e0 + W4 * e1 + D1 * e2 + W7 * e3 + D4 * e4 + W1 * e5;
        out[10] = D1 * -e5 + D4 * e1 - D7 * e3 + W1 * e2 - W4 * e4 + W7 * e0;
        out[16] = D1 * e3 + D4 * e4 + D7 * -e5 - W1 * e0 + W4 * e1 + W7 * e2;
        
        out[0]  = W0 * s0 + W1 * s1 + W2 * s2 + W3 * s3 + W4 * s4 + W5 * s5 + W6 * s6 + W7 * s7 + W8 * s8 - D0 * s9
                - D1 * s10 - D2 * s11 - D3 * s12 - D4 * s13 - D5 * s14 - D6 * s15 - D7 * s16 - D8 * s17;
        out[2]  = -W2 * s0 - W7 * s1 + W5 * s2 + W0 * s3 + D4 * s4 + D8 * s5 + D3 * s6 + W1 * s7 + W6 * s8 + D2 * s9
                + D7 * s10 + D5 * s11 + D0 * s12 + W4 * s13 - W8 * s14 - W3 * s15 - D1 * s16 - D6 * s17;
        out[3]  = D3 * s0 + D7 * s1 + D0 * s2 + W6 * s3 - W4 * s4 - D2 * s5 - D8 * s6 - D1 * s7 - W5 * s8 - W3 * s9
                + W7 * s10 + W0 * s11 + D6 * s12 + D4 * s13 + W2 * s14 - W8 * s15 - W1 * s16 - D5 * s17;
        out[5]  = -D5 * s0 - D1 * s1 + W8 * s2 + D2 * s3 + D4 * s4 + W6 * s5 - W0 * s6 - D7 * s7 - W3 * s8 + W5 * s9
                - W1 * s10 - D8 * s11 - W2 * s12 + W4 * s13 + D6 * s14 + D0 * s15 - W7 * s16 - D3 * s17;
        out[6]  = -W6 * s0 + D1 * s1 + D3 * s2 - W8 * s3 - D4 * s4 - D0 * s5 + W5 * s6 + D7 * s7 + W2 * s8 + D6 * s9
                + W1 * s10 - W3 * s11 - D8 * s12 - W4 * s13 + W0 * s14 + D5 * s15 + W7 * s16 - D2 * s17;
        out[8]  = -D0 * s17 + D1 * s7 + D2 * s15 - D3 * s5 - D4 * s13 + D5 * s3 + D6 * s11 - D7 * s1 - D8 * s9 + W0 * s8
                + W1 * s16 - W2 * s6 - W3 * s14 + W4 * s4 + W5 * s12 - W6 * s2 - W7 * s10 + W8 * s0;
        out[9]  = -D0 * s8 + D1 * s16 + D2 * s6 - D3 * s14 - D4 * s4 + D5 * s12 + D6 * s2 - D7 * s10 - D8 * s0 - W0 * s17
                - W1 * s7 + W2 * s15 + W3 * s5 - W4 * s13 - W5 * s3 + W6 * s11 + W7 * s1 - W8 * s9;
        out[11] = -D0 * s14 + D1 * s10 - D2 * s8 - D3 * s11 + D4 * s13 + D5 * s6 + D6 * s0 + D7 * s16 + D8 * s3
                - W0 * s5 - W1 * s1 - W2 * s17 - W3 * s2 - W4 * s4 - W5 * s15 + W6 * s9 - W7 * s7 - W8 * s12;
        out[12] = -D0 * s6 + D1 * s10 - D2 * s12 + D3 * s8 + D4 * s13 - D5 * s9 + D6 * s5 + D7 * s16 + D8 * s2
                - W0 * s15 - W1 * s1 - W2 * s3 - W3 * s17 - W4 * s4 - W5 * s0 - W6 * s14 - W7 * s7 + W8 * s11;
        out[14] = D0 * s11 + D1 * s16 + D2 * s14 + D3 * s9 - D4 * s4 + D5 * s8 + D6 * s3 - D7 * s10 - D8 * s15 - W0 * s2
                - W1 * s7 + W2 * s5 + W3 * s0 - W4 * s13 - W5 * s17 - W6 * s12 + W7 * s1 + W8 * s6;
        out[15] = -D0 * s3 + D1 * s7 + D2 * s0 - D3 * s15 - D4 * s13 + D5 * s2 - D6 * s8 - D7 * s1 + D8 * s14 + W0 * s12
                + W1 * s16 + W2 * s9 - W3 * s6 + W4 * s4 - W5 * s11 - W6 * s17 - W7 * s10 + W8 * s5;
        out[17] = -D0 * s0 + D1 * s1 - D2 * s2 + D3 * s3 - D4 * s4 + D5 * s5 - D6 * s6 + D7 * s7 - D8 * s8 - W0 * s9
                + W1 * s10 - W2 * s11 + W3 * s12 - W4 * s13 + W5 * s14 - W6 * s15 + W7 * s16 - W8 * s17;
         
         for (int i = 0; i < 18; i++)
             out[i] = ((((out[i] + 512) >> 10) * 114) + 512) >> 10;
    }
    
    public void mdct18Fast1(int[] x, int[] out) {
        int x0 = x[0], x1 = x[1], x2 = x[2], x3 = x[3], x4 = x[4], x5 = x[5], x6 = x[6], x7 = x[7], x8 = x[8];
        int y0 = x[9], y1 = x[10], y2 = x[11], y3 = x[12], y4 = x[13], y5 = x[14], y6 = x[15], y7 = x[16], y8 = x[17];
        int t0 = x[18], t1 = x[19], t2 = x[20], t3 = x[21], t4 = x[22], t5 = x[23], t6 = x[24], t7 = x[25], t8 = x[26];
        int u0 = x[27], u1 = x[28], u2 = x[29], u3 = x[30], u4 = x[31], u5 = x[32], u6 = x[33], u7 = x[34], u8 = x[35];
        
        out[4]  = W4 * (x0 + x5 - x1 - x4 + x8 - y8 - y3 + y7 + y4 - y0 - u1 - u6 + u2 + u5      - t7 - t2 + t3 + t6) + D4 * (x6 + x7 - x2 - x3      - y1 - y2 + y5 + y6      - u7 - u8 + u3 + u4 - u0 - t8 - t0 - t1 + t4 + t5);
        out[13] = W4 * (x6 + x7 - x2 - x3      - y1 - y2 + y5 + y6      - u7 - u8 + u3 + u4 - u0 - t8 - t0 - t1 + t4 + t5) - D4 * (x0 + x5 - x1 - x4 + x8 - y8 - y3 + y7 + y4 - y0 - u1 - u6 + u2 + u5      - t7 - t2 + t3 + t6);
        
        out[1]  = D7 * (y5 + y6 - (t8 + u0) - (x2 + x3)) + W4 * (t1 + u7  -(x7 - y1)  - (t4 + u4)) - D1 * (x0 - y8 + x5 - y3 + t6 + u2) - W1 * (x6 - y2  - (t0 + u8)  + t5 + u3) - D4 * (x1 - y7 + x4 - y4 + t7 + u1) + W7 * (t2 + u6 - (t3 + u5) - (x8 - y0));
        out[7]  = D7 * (x0 - y8 + x5 - y3 + t6 + u2) + W4 * (x1 - y7 + x4 - y4 + t7 + u1) + D1 * (y5 + y6 - (t8 + u0) - (x2 + x3)) + W7 * (x6 - y2  - (t0 + u8)  + t5 + u3) + D4 * (t1 + u7  -(x7 - y1)  - (t4 + u4)) + W1 * (t2 + u6 - (t3 + u5) - (x8 - y0));
        out[10] = D1 * -(t2 + u6 - (t3 + u5) - (x8 - y0)) + D4 * (x1 - y7 + x4 - y4 + t7 + u1) - D7 * (x6 - y2  - (t0 + u8)  + t5 + u3) + W1 * (y5 + y6 - (t8 + u0) - (x2 + x3)) - W4 * (t1 + u7  -(x7 - y1)  - (t4 + u4)) + W7 * (x0 - y8 + x5 - y3 + t6 + u2);
        out[16] = D1 * (x6 - y2  - (t0 + u8)  + t5 + u3) + D4 * (t1 + u7  -(x7 - y1)  - (t4 + u4)) + D7 * -(t2 + u6 - (t3 + u5) - (x8 - y0)) - W1 * (x0 - y8 + x5 - y3 + t6 + u2) + W4 * (x1 - y7 + x4 - y4 + t7 + u1) + W7 * (y5 + y6 - (t8 + u0) - (x2 + x3));
        
        out[0]  = W0 * (x0 - y8) + W1 * (x1 - y7) + W2 * (x2 - y6) + W3 * (x3 - y5) + W4 * (x4 - y4) + W5 * (x5 - y3) + W6 * (x6 - y2) + W7 * (x7 - y1) + W8 * (x8 - y0) - D0 * (t0 + u8)
                - D1 * (t1 + u7) - D2 * (t2 + u6) - D3 * (t3 + u5) - D4 * (t4 + u4) - D5 * (t5 + u3) - D6 * (t6 + u2) - D7 * (t7 + u1) - D8 * (t8 + u0);
        out[2]  = -W2 * (x0 - y8) - W7 * (x1 - y7) + W5 * (x2 - y6) + W0 * (x3 - y5) + D4 * (x4 - y4) + D8 * (x5 - y3) + D3 * (x6 - y2) + W1 * (x7 - y1) + W6 * (x8 - y0) + D2 * (t0 + u8)
                + D7 * (t1 + u7) + D5 * (t2 + u6) + D0 * (t3 + u5) + W4 * (t4 + u4) - W8 * (t5 + u3) - W3 * (t6 + u2) - D1 * (t7 + u1) - D6 * (t8 + u0);
        out[3]  = D3 * (x0 - y8) + D7 * (x1 - y7) + D0 * (x2 - y6) + W6 * (x3 - y5) - W4 * (x4 - y4) - D2 * (x5 - y3) - D8 * (x6 - y2) - D1 * (x7 - y1) - W5 * (x8 - y0) - W3 * (t0 + u8)
                + W7 * (t1 + u7) + W0 * (t2 + u6) + D6 * (t3 + u5) + D4 * (t4 + u4) + W2 * (t5 + u3) - W8 * (t6 + u2) - W1 * (t7 + u1) - D5 * (t8 + u0);
        out[5]  = -D5 * (x0 - y8) - D1 * (x1 - y7) + W8 * (x2 - y6) + D2 * (x3 - y5) + D4 * (x4 - y4) + W6 * (x5 - y3) - W0 * (x6 - y2) - D7 * (x7 - y1) - W3 * (x8 - y0) + W5 * (t0 + u8)
                - W1 * (t1 + u7) - D8 * (t2 + u6) - W2 * (t3 + u5) + W4 * (t4 + u4) + D6 * (t5 + u3) + D0 * (t6 + u2) - W7 * (t7 + u1) - D3 * (t8 + u0);
        out[6]  = -W6 * (x0 - y8) + D1 * (x1 - y7) + D3 * (x2 - y6) - W8 * (x3 - y5) - D4 * (x4 - y4) - D0 * (x5 - y3) + W5 * (x6 - y2) + D7 * (x7 - y1) + W2 * (x8 - y0) + D6 * (t0 + u8)
                + W1 * (t1 + u7) - W3 * (t2 + u6) - D8 * (t3 + u5) - W4 * (t4 + u4) + W0 * (t5 + u3) + D5 * (t6 + u2) + W7 * (t7 + u1) - D2 * (t8 + u0);
        out[8]  = -D0 * (t8 + u0) + D1 * (x7 - y1) + D2 * (t6 + u2) - D3 * (x5 - y3) - D4 * (t4 + u4) + D5 * (x3 - y5) + D6 * (t2 + u6) - D7 * (x1 - y7) - D8 * (t0 + u8) + W0 * (x8 - y0)
                + W1 * (t7 + u1) - W2 * (x6 - y2) - W3 * (t5 + u3) + W4 * (x4 - y4) + W5 * (t3 + u5) - W6 * (x2 - y6) - W7 * (t1 + u7) + W8 * (x0 - y8);
        out[9]  = -D0 * (x8 - y0) + D1 * (t7 + u1) + D2 * (x6 - y2) - D3 * (t5 + u3) - D4 * (x4 - y4) + D5 * (t3 + u5) + D6 * (x2 - y6) - D7 * (t1 + u7) - D8 * (x0 - y8) - W0 * (t8 + u0)
                - W1 * (x7 - y1) + W2 * (t6 + u2) + W3 * (x5 - y3) - W4 * (t4 + u4) - W5 * (x3 - y5) + W6 * (t2 + u6) + W7 * (x1 - y7) - W8 * (t0 + u8);
        out[11] = -D0 * (t5 + u3) + D1 * (t1 + u7) - D2 * (x8 - y0) - D3 * (t2 + u6) + D4 * (t4 + u4) + D5 * (x6 - y2) - D6 * (x0 + y8) + D7 * (t7 + u1) + D8 * (x3 - y5)
                - W0 * (x5 - y3) - W1 * (x1 - y7) - W2 * (t8 + u0) - W3 * (x2 - y6) - W4 * (x4 - y4) - W5 * (t6 + u2) + W6 * (t0 + u8) - W7 * (x7 - y1) - W8 * (t3 + u5);
        out[12] = -D0 * (x6 - y2) + D1 * (t1 + u7) - D2 * (t3 + u5) + D3 * (x8 - y0) + D4 * (t4 + u4) - D5 * (t0 + u8) + D6 * (x5 - y3) + D7 * (t7 + u1) + D8 * (x2 - y6)
                - W0 * (t6 + u2) - W1 * (x1 - y7) - W2 * (x3 - y5) - W3 * (t8 + u0) - W4 * (x4 - y4) - W5 * (x0 - y8) - W6 * (t5 + u3) - W7 * (x7 - y1) + W8 * (t2 + u6);
        out[14] = D0 * (t2 + u6) + D1 * (t7 + u1) + D2 * (t5 + u3) + D3 * (t0 + u8) - D4 * (x4 - y4) + D5 * (x8 - y0) + D6 * (x3 - y5) - D7 * (t1 + u7) - D8 * (t6 + u2) - W0 * (x2 - y6)
                - W1 * (x7 - y1) + W2 * (x5 - y3) + W3 * (x0 - y8) - W4 * (t4 + u4) - W5 * (t8 + u0) - W6 * (t3 + u5) + W7 * (x1 - y7) + W8 * (x6 - y2);
        out[15] = -D0 * (x3 - y5) + D1 * (x7 - y1) + D2 * (x0 - y8) - D3 * (t6 + u2) - D4 * (t4 + u4) + D5 * (x2 - y6) - D6 * (x8 - y0) - D7 * (x1 - y7) + D8 * (t5 + u3) + W0 * (t3 + u5)
                + W1 * (t7 + u1) + W2 * (t0 + u8) - W3 * (x6 - y2) + W4 * (x4 - y4) - W5 * (t2 + u6) - W6 * (t8 + u0) - W7 * (t1 + u7) + W8 * (x5 - y3);
        out[17] = -D0 * (x0 - y8) + D1 * (x1 - y7) - D2 * (x2 - y6) + D3 * (x3 - y5) - D4 * (x4 - y4) + D5 * (x5 - y3) - D6 * (x6 - y2) + D7 * (x7 - y1) - D8 * (x8 - y0) - W0 * (t0 + u8)
                + W1 * (t1 + u7) - W2 * (t2 + u6) + W3 * (t3 + u5) - W4 * (t4 + u4) + W5 * (t5 + u3) - W6 * (t6 + u2) + W7 * (t7 + u1) - W8 * (t8 + u0);
         
         for (int i = 0; i < 18; i++)
             out[i] = ((((out[i] + 512) >> 10) * 114) + 512) >> 10;
    }
    
    public void imdct18(int[] x, int[] out) {
        for (int n = 0; n < 36; n++) {
            int sum = 0;
            for (int k = 0; k < 18; k++) {
                sum += x[k] * mdct18Lookup[k][n];
            }
            out[n] = (sum + 512) >> 10;
        }
    }
    
    @Test
    public void testMp3Mdct() {
        System.out.println("======== IMP RESP ========");
        
        for (int i = 0; i < 18; i++) {
            float[] o = new float[18];
            float[] out = new float[36];
            o[i] = 1;
            int[] oi = new int[18];
            oi[i] = 65536;
            int[] oo = new int[36];
            int[] ooo = new int[36];
            
            Mp3Mdct.oneLong(o, out);
            imdct18(oi, oo);
            sineWindow(oo, ooo, 0, 0);
            
            for (int z = 0; z < 36; z++) {
                int a = (int)(65536 * out[z] * win[0][z]);
                int b = (int)(ooo[z]); // * ratio[z]
                System.out.println(String.format("%10d : %10d -> %f", a, b, (float)b / a));
            }
        }
        
        System.out.println("======== IMP RESP ========");
    }

    @Test
    public void testMdct() {
        int[] x = ArrayUtil.randomIntArray(18 * 3, 0, 65535);
        int[] xo = new int[18 * 3];
        int[] xoo = new int[18 * 3];

        // two windows
        for (int i = 0, off = 0; i < 2; i++, off += 18) {
            int[] w = new int[36];
            int[] o = new int[18];
            sineWindow(x, w, off, 0);
//            mdct18(w, o);
            mdct18Fast(w, o);
            imdct18(o, w);
            
            float[] wf = new float[36];
            float[] of = new float[18];
            for (int z = 0; z < 18; z++)
                of[z] = ((float)o[z]) / 65536f;
            Mp3Mdct.oneLong(of, wf);

            sineWindow(w, xo, 0, off);
            
            for(int z = 0; z < 36; z++) {
                xoo[z + off] += wf[z] * win[0][z] * 65536;
                System.out.println(String.format("%06d: %06d %4.3f", xoo[z + off], xo[off + z], (float)xoo[z + off] / xo[off + z]));
            }
        }
        System.out.println();

        assertApproximatelyEquals(x, xo, .05, 18, 18);
        assertApproximatelyEquals(xoo, xo, .15, 0, 18*3);
    }

    private void sineWindow(int[] in, int[] out, int offI, int offO) {
        for (int j = 0; j < 36; j++) {
            out[j + offO] += (in[j + offI] * sineWnd[j] + 512) >> 10;
        }

        System.out.println();
    }

    private void assertApproximatelyEquals(int[] rand, int[] newRand, double threash, int from, int cnt) {
        double maxr = 0;
        for (int i = 0; i < Math.min(rand.length, cnt); i++) {
            double r = 1 - (newRand[i + from] != 0 ? (double) rand[i + from] / newRand[i + from] : 0);
            System.out.println(newRand[i + from] + " - " + rand[i + from]);
            if (r > maxr)
                maxr = r;
        }
        Assert.assertTrue("Maxr: " + maxr, maxr < threash);
    }
}
