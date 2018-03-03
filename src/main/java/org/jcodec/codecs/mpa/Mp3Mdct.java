package org.jcodec.codecs.mpa;

import java.util.Arrays;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 */
public class Mp3Mdct {

    private static final float factor36pt0 = 0.34729635533386f;
    private static final float factor36pt1 = 1.532088886238f;
    private static final float factor36pt2 = 1.8793852415718f;
    private static final float factor36pt3 = 1.732050808f;
    private static final float factor36pt4 = 1.9696155060244f;
    private static final float factor36pt5 = 1.2855752193731f;
    private static final float factor36pt6 = 0.68404028665134f;

    private static final float[] factor36 = { 0.501909918f, 0.517638090f, 0.551688959f, 0.610387294f, 0.871723397f,
            1.183100792f, 1.931851653f, 5.736856623f };

    private static final float cos075 = 0.991444861f;
    private static final float cos225 = 0.923879532f;
    private static final float cos300 = 0.866025403f;
    private static final float cos375 = 0.793353340f;
    private static final float cos450 = 0.707106781f;
    private static final float cos525 = 0.608761429f;
    private static final float cos600 = 0.500000000f;
    private static final float cos675 = 0.382683432f;
    private static final float cos825 = 0.130526192f;

    private static final float factor12pt0 = 1.931851653f;
    private static final float factor12pt1 = 0.517638090f;
    private static final float[] factor12 = { 0.504314480f, 0.541196100f, 0.630236207f, 0.821339815f, 1.306562965f,
            3.830648788f };

    private static float[] tmp = new float[16];

    //Wrong usage of Javascript keyword:in
    static void oneLong(float[] src, float[] dst) {
        for (int i = 17; i > 0; i--)
            src[i] += src[i - 1];

        for (int i = 17; i > 2; i -= 2)
            src[i] += src[i - 2];

        for (int i = 0, k = 0; i < 2; i++, k += 8) {
            float tmp0 = src[i] + src[i];
            float tmp1 = tmp0 + src[12 + i];
            float tmp2 = src[6 + i] * factor36pt3;

            tmp[k + 0] = tmp1 + src[4 + i] * factor36pt2 + src[8 + i] * factor36pt1 + src[16 + i] * factor36pt0;
            tmp[k + 1] = tmp0 + src[4 + i] - src[8 + i] - src[12 + i] - src[12 + i] - src[16 + i];
            tmp[k + 2] = tmp1 - src[4 + i] * factor36pt0 - src[8 + i] * factor36pt2 + src[16 + i] * factor36pt1;
            tmp[k + 3] = tmp1 - src[4 + i] * factor36pt1 + src[8 + i] * factor36pt0 - src[16 + i] * factor36pt2;

            tmp[k + 4] = src[2 + i] * factor36pt4 + tmp2 + src[10 + i] * factor36pt5 + src[14 + i] * factor36pt6;
            tmp[k + 5] = (src[2 + i] - src[10 + i] - src[14 + i]) * factor36pt3;
            tmp[k + 6] = src[2 + i] * factor36pt5 - tmp2 - src[10 + i] * factor36pt6 + src[14 + i] * factor36pt4;
            tmp[k + 7] = src[2 + i] * factor36pt6 - tmp2 + src[10 + i] * factor36pt4 - src[14 + i] * factor36pt5;
        }

        for (int i = 0, j = 4, k = 8, l = 12; i < 4; i++, j++, k++, l++) {
            float q1 = tmp[i];
            float q2 = tmp[k];
            tmp[i] += tmp[j];
            tmp[j] = q1 - tmp[j];
            tmp[k] = (tmp[k] + tmp[l]) * factor36[i];
            tmp[l] = (q2 - tmp[l]) * factor36[7 - i];
        }

        for (int i = 0; i < 4; i++) {
            dst[26 - i] = tmp[i] + tmp[8 + i];
            dst[8 - i] = tmp[8 + i] - tmp[i];
            dst[27 + i] = dst[26 - i];
            dst[9 + i] = -dst[8 - i];
        }

        for (int i = 0; i < 4; i++) {
            dst[21 - i] = tmp[7 - i] + tmp[15 - i];
            dst[3 - i] = tmp[15 - i] - tmp[7 - i];
            dst[32 + i] = dst[21 - i];
            dst[14 + i] = -dst[3 - i];
        }

        float tmp0 = src[0] - src[4] + src[8] - src[12] + src[16];
        float tmp1 = (src[1] - src[5] + src[9] - src[13] + src[17]) * cos450;
        dst[4] = tmp1 - tmp0;
        dst[13] = -dst[4];
        dst[31] = dst[22] = tmp0 + tmp1;
    }

    //Wrong usage of Javascript keyword:in
    static void threeShort(float[] src, float[] dst) {
        Arrays.fill(dst, 0.0f);

        for (int i = 0, outOff = 0; i < 3; i++, outOff += 6) {
            imdct12(src, dst, outOff, i);
        }
    }

    //Wrong usage of Javascript keyword:in
    private static void imdct12(float[] src, float[] dst, int outOff, int wndIdx) {

        for (int j = 15 + wndIdx, k = 12 + wndIdx; j >= 3 + wndIdx; j -= 3, k -= 3)
            src[j] += src[k];

        src[15 + wndIdx] += src[9 + wndIdx];
        src[9 + wndIdx] += src[3 + wndIdx];

        float pp2 = src[12 + wndIdx] * cos600;
        float pp1 = src[6 + wndIdx] * cos300;
        float sum = src[0 + wndIdx] + pp2;
        tmp[1] = src[wndIdx] - src[12 + wndIdx];
        tmp[0] = sum + pp1;
        tmp[2] = sum - pp1;

        pp2 = src[15 + wndIdx] * cos600;
        pp1 = src[9 + wndIdx] * cos300;
        sum = src[3 + wndIdx] + pp2;
        tmp[4] = src[3 + wndIdx] - src[15 + wndIdx];
        tmp[5] = sum + pp1;
        tmp[3] = sum - pp1;

        tmp[3] *= factor12pt0;
        tmp[4] *= cos450;
        tmp[5] *= factor12pt1;

        float t  = tmp[0];
        tmp[0]  += tmp[5];
        tmp[5]   = t - tmp[5];
        
        t        = tmp[1];
        tmp[1]  += tmp[4];
        tmp[4]   = t - tmp[4];
        
        t        = tmp[2];
        tmp[2]  += tmp[3];
        tmp[3]   = t - tmp[3];

        for (int j = 0; j < 6; j++)
            tmp[j] *= factor12[j];

        tmp[8]  = -tmp[0] * cos375;
        tmp[9]  = -tmp[0] * cos525;
        tmp[7]  = -tmp[1] * cos225;
        tmp[10] = -tmp[1] * cos675;
        tmp[6]  = -tmp[2] * cos075;
        tmp[11] = -tmp[2] * cos825;

        tmp[0]  = tmp[3];
        tmp[1]  = tmp[4]  * cos675;
        tmp[2]  = tmp[5]  * cos525;

        tmp[3]  = -tmp[5] * cos375;
        tmp[4]  = -tmp[4] * cos225;
        tmp[5]  = -tmp[0] * cos075;

        tmp[0] *= cos825;

        for (int i = 0, j = outOff + 6; i < 12; i++, j++) {
            dst[j] += tmp[i];
        }
    }
}
