package org.jcodec.codecs.mpa;

import static org.jcodec.codecs.mpa.MpaConst.dp;

import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 */
public class MpaPqmf {
    
    private static final double MY_PI   = 3.14159265358979323846;
    private static final float cos1_64  = (float) (1.0 / (2.0 * Math.cos(MY_PI / 64.0)));
    private static final float cos3_64  = (float) (1.0 / (2.0 * Math.cos(MY_PI * 3.0 / 64.0)));
    private static final float cos5_64  = (float) (1.0 / (2.0 * Math.cos(MY_PI * 5.0 / 64.0)));
    private static final float cos7_64  = (float) (1.0 / (2.0 * Math.cos(MY_PI * 7.0 / 64.0)));
    private static final float cos9_64  = (float) (1.0 / (2.0 * Math.cos(MY_PI * 9.0 / 64.0)));
    private static final float cos11_64 = (float) (1.0 / (2.0 * Math.cos(MY_PI * 11.0 / 64.0)));
    private static final float cos13_64 = (float) (1.0 / (2.0 * Math.cos(MY_PI * 13.0 / 64.0)));
    private static final float cos15_64 = (float) (1.0 / (2.0 * Math.cos(MY_PI * 15.0 / 64.0)));
    private static final float cos17_64 = (float) (1.0 / (2.0 * Math.cos(MY_PI * 17.0 / 64.0)));
    private static final float cos19_64 = (float) (1.0 / (2.0 * Math.cos(MY_PI * 19.0 / 64.0)));
    private static final float cos21_64 = (float) (1.0 / (2.0 * Math.cos(MY_PI * 21.0 / 64.0)));
    private static final float cos23_64 = (float) (1.0 / (2.0 * Math.cos(MY_PI * 23.0 / 64.0)));
    private static final float cos25_64 = (float) (1.0 / (2.0 * Math.cos(MY_PI * 25.0 / 64.0)));
    private static final float cos27_64 = (float) (1.0 / (2.0 * Math.cos(MY_PI * 27.0 / 64.0)));
    private static final float cos29_64 = (float) (1.0 / (2.0 * Math.cos(MY_PI * 29.0 / 64.0)));
    private static final float cos31_64 = (float) (1.0 / (2.0 * Math.cos(MY_PI * 31.0 / 64.0)));
    private static final float cos1_32  = (float) (1.0 / (2.0 * Math.cos(MY_PI / 32.0)));
    private static final float cos3_32  = (float) (1.0 / (2.0 * Math.cos(MY_PI * 3.0 / 32.0)));
    private static final float cos5_32  = (float) (1.0 / (2.0 * Math.cos(MY_PI * 5.0 / 32.0)));
    private static final float cos7_32  = (float) (1.0 / (2.0 * Math.cos(MY_PI * 7.0 / 32.0)));
    private static final float cos9_32  = (float) (1.0 / (2.0 * Math.cos(MY_PI * 9.0 / 32.0)));
    private static final float cos11_32 = (float) (1.0 / (2.0 * Math.cos(MY_PI * 11.0 / 32.0)));
    private static final float cos13_32 = (float) (1.0 / (2.0 * Math.cos(MY_PI * 13.0 / 32.0)));
    private static final float cos15_32 = (float) (1.0 / (2.0 * Math.cos(MY_PI * 15.0 / 32.0)));
    private static final float cos1_16  = (float) (1.0 / (2.0 * Math.cos(MY_PI / 16.0)));
    private static final float cos3_16  = (float) (1.0 / (2.0 * Math.cos(MY_PI * 3.0 / 16.0)));
    private static final float cos5_16  = (float) (1.0 / (2.0 * Math.cos(MY_PI * 5.0 / 16.0)));
    private static final float cos7_16  = (float) (1.0 / (2.0 * Math.cos(MY_PI * 7.0 / 16.0)));
    private static final float cos1_8   = (float) (1.0 / (2.0 * Math.cos(MY_PI / 8.0)));
    private static final float cos3_8   = (float) (1.0 / (2.0 * Math.cos(MY_PI * 3.0 / 8.0)));
    private static final float cos1_4   = (float) (1.0 / (2.0 * Math.cos(MY_PI / 4.0)));
    
    private static final float bf32[] = { cos1_64, cos3_64, cos5_64, cos7_64, cos9_64, cos11_64, cos13_64, cos15_64,
            cos17_64, cos19_64, cos21_64, cos23_64, cos25_64, cos27_64, cos29_64, cos31_64 };
    
    private static final float bf16[] = { cos1_32, cos3_32, cos5_32, cos7_32, cos9_32, cos11_32, cos13_32, cos15_32 };
    
    private static final float bf8[] = { cos1_16, cos3_16, cos5_16, cos7_16 };
    
    public static void computeFilter(int sampleOff, float[] samples, short[] out, int outOff, float scalefactor) {
        int dvp = 0;
        for (int i = 0; i < 32; i++) {
            float pcm_sample;
            int b = i << 4;
            pcm_sample = (float) ((
                    samples[((16 + sampleOff) & 0xf) + dvp] * dp[b + 0]  + 
                    samples[((15 + sampleOff) & 0xf) + dvp] * dp[b + 1]  +
                    samples[((14 + sampleOff) & 0xf) + dvp] * dp[b + 2]  + 
                    samples[((13 + sampleOff) & 0xf) + dvp] * dp[b + 3]  + 
                    samples[((12 + sampleOff) & 0xf) + dvp] * dp[b + 4]  + 
                    samples[((11 + sampleOff) & 0xf) + dvp] * dp[b + 5]  + 
                    samples[((10 + sampleOff) & 0xf) + dvp] * dp[b + 6]  +
                    samples[(( 9 + sampleOff) & 0xf) + dvp] * dp[b + 7]  + 
                    samples[(( 8 + sampleOff) & 0xf) + dvp] * dp[b + 8]  + 
                    samples[(( 7 + sampleOff) & 0xf) + dvp] * dp[b + 9]  + 
                    samples[(( 6 + sampleOff) & 0xf) + dvp] * dp[b + 10] + 
                    samples[(( 5 + sampleOff) & 0xf) + dvp] * dp[b + 11] + 
                    samples[(( 4 + sampleOff) & 0xf) + dvp] * dp[b + 12] + 
                    samples[(( 3 + sampleOff) & 0xf) + dvp] * dp[b + 13] + 
                    samples[(( 2 + sampleOff) & 0xf) + dvp] * dp[b + 14] + 
                    samples[(( 1 + sampleOff) & 0xf) + dvp] * dp[b + 15]
                    ) * scalefactor);

            out[outOff + i] = (short) MathUtil.clip((int) pcm_sample, -0x8000, 0x7fff);
            dvp += 16;
        }
    }
    
    static void computeButterfly(int pos, float[] s) {
        butterfly32(s);
        
        butterfly16L(s);
        butterfly16H(s);

        butterfly8L(s, 0);
        butterfly8H(s, 0);
        butterfly8L(s, 16);
        butterfly8H(s, 16);
        
        for (int i = 0; i < 32; i += 8) {
            butterfly4L(s, i);
            butterfly4H(s, i);
        }
        
        for (int i = 0; i < 32; i += 4) {
            butterfly2L(s, i);
            butterfly2H(s, i);
        }
        
        float k0 = -s[14] - s[15] - s[10] - s[11];
        float k1 = s[29] + s[31] + s[25];
        float k2 = k1 + s[17];
        float k3 = k1 + s[21] + s[23];
        float k4 = s[15] + s[11];
        float k5 = s[15] + s[13] + s[9];
        float k6 = s[7] + s[5];
        float k7 = s[31] + s[23];
        float k8 = k7 + s[27];
        float k9 = s[31] + s[27] + s[19];
        float k10 = -s[26] - s[27] - s[30] - s[31];
        float k11 = -s[24] - s[28] - s[30] - s[31];
        float k12 = s[20] + s[22] + s[23];
        float k13 = s[21] + s[29];
        
        float s0 = s[0];
        float s1 = s[1];
        float s2 = s[2];
        float s3 = s[3];
        float s4 = s[4];
        float s6 = s[6];
        float s7 = s[7];
        float s8 = s[8];
        float s12 = s[12];
        float s13 = s[13];
        float s14 = s[14];
        float s15 = s[15];
        float s16 = s[16];
        float s18 = s[18];
        float s19 = s[19];
        float s21 = s[21];
        float s22 = s[22];
        float s23 = s[23];
        float s28 = s[28];
        float s29 = s[29];
        float s30 = s[30];
        float s31 = s[31];
        
        s[0] = s1;
        s[1] = k2;
        s[2] = k5;
        s[3] = k3;
        s[4] = k6;
        s[5] = k8 + k13;
        s[6] = k4 + s13;
        s[7] = k9 + s29;
        s[8] = s3;
        s[9] = k9;
        s[10] = k4;
        s[11] = k8;
        s[12] = s7;
        s[13] = k7;
        s[14] = s15;
        s[15] = s31;
        s[16] = -k2 - s30;
        s[17] = -k5 - s14;
        s[18] = -k3 - s22 - s30;
        s[19] = -k6 - s6;
        s[20] = k10 - s29 - s21 - s22 - s23;
        s[21] = k0 - s13;
        s[22] = k10 - s29 - s18 - s19;
        s[23] = -s3 - s2;
        s[24] = k10 - s28 - s18 - s19;
        s[25] = k0 - s12;
        s[26] = k10 - s28 - k12;
        s[27] = -s6 - s7 - s4;
        s[28] = k11 - k12;
        s[29] = -s14 - s15 - s12 - s8;
        s[30] = k11 - s16;
        s[31] = -s0;
    }

    private static void butterfly16H(float[] s) {
        for (int i = 0; i < 8; i++) {
            float tmp0 = s[16 + i];
            float tmp1 = s[31 - i];
            s[16 + i] = tmp0 + tmp1;
            s[31 - i] = -(tmp0 - tmp1) * bf16[i];
        }
    }

    private static void butterfly16L(float[] s) {
        for (int i = 0; i < 8; i++) {
            float tmp0 = s[i];
            float tmp1 = s[15 - i];
            s[i] = tmp0 + tmp1;
            s[15 - i] = (tmp0 - tmp1) * bf16[i];
        }
    }

    private static void butterfly8H(float[] s, int o) {
        for (int i = 0; i < 4; i++) {
            float tmp0 = s[o + 8 + i];
            float tmp1 = s[o + 15 - i];
            s[o + 8 + i] = tmp0 + tmp1;
            s[o + 15 - i] = -(tmp0 - tmp1) * bf8[i];
        }
    }

    private static void butterfly8L(float[] s, int o) {
        for (int i = 0; i < 4; i++) {
            float tmp0 = s[o + i];
            float tmp1 = s[o + 7 - i];
            s[o + i] = tmp0 + tmp1;
            s[o + 7 - i] = (tmp0 - tmp1) * bf8[i];
        }
    }
    
    private static void butterfly4H(float[] s, int o) {
        float tmp0 = s[o + 4];
        float tmp1 = s[o + 7];
        s[o + 4] = tmp0 + tmp1;
        s[o + 7] = -(tmp0 - tmp1) * cos1_8;
        
        float tmp2 = s[o + 5];
        float tmp3 = s[o + 6];
        s[o + 5] = tmp2 + tmp3;
        s[o + 6] = -(tmp2 - tmp3) * cos3_8;
    }

    private static void butterfly4L(float[] s, int o) {
        float tmp0 = s[o];
        float tmp1 = s[o + 3];
        s[o + 0] = tmp0 + tmp1;
        s[o + 3] = (tmp0 - tmp1) * cos1_8;
        
        float tmp2 = s[o + 1];
        float tmp3 = s[o + 2];
        s[o + 1] = tmp2 + tmp3;
        s[o + 2] = (tmp2 - tmp3) * cos3_8;
    }
    
    private static void butterfly2H(float[] s, int o) {
        float tmp0 = s[o + 2];
        float tmp1 = s[o + 3];
        s[o + 2] = tmp0 + tmp1;
        s[o + 3] = -(tmp0 - tmp1) * cos1_4;
    }

    private static void butterfly2L(float[] s, int o) {
        float tmp0 = s[o];
        float tmp1 = s[o + 1];
        s[o + 0] = tmp0 + tmp1;
        s[o + 1] = (tmp0 - tmp1) * cos1_4;
    }
    
    
    private static void butterfly32(float[] s) {

        for (int i = 0; i < 16; i++) {
            float tmp0 = s[i];
            float tmp1 = s[31 - i];
            s[i] = tmp0 + tmp1;
            s[31 - i] = (tmp0 - tmp1) * bf32[i];
        }
    }
}
