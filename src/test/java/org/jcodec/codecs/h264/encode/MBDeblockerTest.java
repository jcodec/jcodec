package org.jcodec.codecs.h264.encode;
import org.jcodec.Utils;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.common.ArrayUtil;
import org.jcodec.common.model.Picture;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class MBDeblockerTest {

    @Ignore @Test
    public void testCalcStrength() {
        EncodedMB left = new EncodedMB();
        EncodedMB top = new EncodedMB();
        EncodedMB cur = new EncodedMB();

        int[][] v = new int[4][4];
        int[][] h = new int[4][4];

        // h[0][0] -> 1
        top.mx[12] = 4;

        // h[2][0] -> 1
        cur.mx[2] = -2;
        top.mx[14] = 2;

        // h[1][0] -> 0
        cur.my[1] = -1;
        top.my[13] = 2;

        // h[3][0] -> 2
        top.nc[15] = 1;

        // h[1][1] -> 2, v[1][0] -> 2, v[2][0] -> 2
        cur.nc[4] = 1;

        // h[2]
        cur.mx[6] = 2;
        cur.mx[10] = -2;
        cur.mx[11] = 2;
        cur.mx[14] = 2;

        left.type = MBType.I_16x16;
        MBDeblocker.calcStrengthForBlocks(cur, left, v, MBDeblocker.LOOKUP_IDX_P_V, MBDeblocker.LOOKUP_IDX_Q_V);
        top.type = MBType.P_16x16;
        MBDeblocker.calcStrengthForBlocks(cur, top, h, MBDeblocker.LOOKUP_IDX_P_H, MBDeblocker.LOOKUP_IDX_Q_H);

        Utils.assertArrayEquals(new int[][] { { 4, 0, 0, 0 }, { 4, 2, 0, 0 }, { 4, 0, 0, 1 }, { 4, 0, 0, 0 } },
                ArrayUtil.rotate(v));

        Utils.assertArrayEquals(new int[][] { { 1, 0, 1, 2 }, { 2, 0, 1, 0 }, { 2, 0, 1, 0 }, { 0, 0, 1, 0 } }, h);
    }

    @Ignore @Test
    public void testMBlockGeneric() {
        final EncodedMB cur = new EncodedMB(), left = new EncodedMB(), top = new EncodedMB();

        final List<String> actions = new ArrayList<String>();
        MBDeblocker deblocker = new MBDeblocker() {
            @Override
            protected void filterBs4(int indexAlpha, int indexBeta, byte[] pelsP, byte[] pelsQ, int p3Idx, int p2Idx,
                    int p1Idx, int p0Idx, int q0Idx, int q1Idx, int q2Idx, int q3Idx) {
                actions.add("{" + "bs:" + 4 + "," + "indexAlpha:" + indexAlpha + "," + "indexBeta:" + indexBeta + ","
                        + "pelsP:" + label(pelsP, cur, left, top) + "," + "pelsQ:" + label(pelsQ, cur, left, top)
                        + "," + "p3Idx:" + p3Idx + "," + "p2Idx:" + p2Idx + "," + "p1Idx:" + p1Idx + "," + "p0Idx:"
                        + p0Idx + "," + "q0Idx:" + q0Idx + "," + "q1Idx:" + q1Idx + "," + "q2Idx:" + q2Idx + ","
                        + "q3Idx:" + q3Idx + "," + "isChroma:" + "false" + "}");
            }

            @Override
            protected void filterBs(int bs, int indexAlpha, int indexBeta, byte[] pelsP, byte[] pelsQ, int p2Idx,
                    int p1Idx, int p0Idx, int q0Idx, int q1Idx, int q2Idx) {
                actions.add("{" + "bs:" + bs + "," + "indexAlpha:" + indexAlpha + "," + "indexBeta:" + indexBeta + ","
                        + "pelsP:" + label(pelsP, cur, left, top) + "," + "pelsQ:" + label(pelsQ, cur, left, top)
                        + "," + "p2Idx:" + p2Idx + "," + "p1Idx:" + p1Idx + "," + "p0Idx:" + p0Idx + "," + "q0Idx:"
                        + q0Idx + "," + "q1Idx:" + q1Idx + "," + "q2Idx:" + q2Idx + "," + "isChroma:" + "false" + "}");
            }

            @Override
            protected void filterBs4Chr(int indexAlpha, int indexBeta, byte[] pelsP, byte[] pelsQ, int p1Idx, int p0Idx,
                    int q0Idx, int q1Idx) {
                actions.add("{" + "bs:" + 4 + "," + "indexAlpha:" + indexAlpha + "," + "indexBeta:" + indexBeta + ","
                        + "pelsP:" + label(pelsP, cur, left, top) + "," + "pelsQ:" + label(pelsQ, cur, left, top)
                        + "," + "p1Idx:" + p1Idx + "," + "p0Idx:" + p0Idx + "," + "q0Idx:" + q0Idx + "," + "q1Idx:"
                        + q1Idx + "," + "isChroma:" + "true" + "}");
            }

            @Override
            protected void filterBsChr(int bs, int indexAlpha, int indexBeta, byte[] pelsP, byte[] pelsQ, int p1Idx,
                    int p0Idx, int q0Idx, int q1Idx) {
                actions.add("{" + "bs:" + bs + "," + "indexAlpha:" + indexAlpha + "," + "indexBeta:" + indexBeta + ","
                        + "pelsP:" + label(pelsP, cur, left, top) + "," + "pelsQ:" + label(pelsQ, cur, left, top)
                        + "," + "p1Idx:" + p1Idx + "," + "p0Idx:" + p0Idx + "," + "q0Idx:" + q0Idx + "," + "q1Idx:"
                        + q1Idx + "," + "isChroma:" + "true" + "}");
            }

            private String label(byte[] pels, EncodedMB curMB, EncodedMB leftMB, EncodedMB topMB) {
                Picture cur = curMB.pixels, top = topMB.pixels, left = leftMB.pixels;
                if (cur.getPlaneData(0) == pels || cur.getPlaneData(1) == pels || cur.getPlaneData(2) == pels)
                    return "cur";
                else if (left.getPlaneData(0) == pels || left.getPlaneData(1) == pels || left.getPlaneData(2) == pels)
                    return "left";
                else if (top.getPlaneData(0) == pels || top.getPlaneData(1) == pels || top.getPlaneData(2) == pels)
                    return "top";
                else
                    return "N/A";
            }
        };

        int[][] vertStrength = { { 4, 4, 4, 4 }, { 1, 1, 1, 1 }, { 1, 1, 1, 1 }, { 1, 1, 1, 1 } };
        int[][] horizStrength = { { 4, 4, 4, 4 }, { 1, 1, 1, 1 }, { 1, 1, 1, 1 }, { 1, 1, 1, 1 } };
        deblocker.deblockMBGeneric(cur, left, top, vertStrength, horizStrength);

        String[] expected = {
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p3Idx:12,p2Idx:13,p1Idx:14,p0Idx:15,q0Idx:0,q1Idx:1,q2Idx:2,q3Idx:3,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p3Idx:28,p2Idx:29,p1Idx:30,p0Idx:31,q0Idx:16,q1Idx:17,q2Idx:18,q3Idx:19,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p3Idx:44,p2Idx:45,p1Idx:46,p0Idx:47,q0Idx:32,q1Idx:33,q2Idx:34,q3Idx:35,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p3Idx:60,p2Idx:61,p1Idx:62,p0Idx:63,q0Idx:48,q1Idx:49,q2Idx:50,q3Idx:51,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p3Idx:76,p2Idx:77,p1Idx:78,p0Idx:79,q0Idx:64,q1Idx:65,q2Idx:66,q3Idx:67,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p3Idx:92,p2Idx:93,p1Idx:94,p0Idx:95,q0Idx:80,q1Idx:81,q2Idx:82,q3Idx:83,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p3Idx:108,p2Idx:109,p1Idx:110,p0Idx:111,q0Idx:96,q1Idx:97,q2Idx:98,q3Idx:99,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p3Idx:124,p2Idx:125,p1Idx:126,p0Idx:127,q0Idx:112,q1Idx:113,q2Idx:114,q3Idx:115,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p3Idx:140,p2Idx:141,p1Idx:142,p0Idx:143,q0Idx:128,q1Idx:129,q2Idx:130,q3Idx:131,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p3Idx:156,p2Idx:157,p1Idx:158,p0Idx:159,q0Idx:144,q1Idx:145,q2Idx:146,q3Idx:147,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p3Idx:172,p2Idx:173,p1Idx:174,p0Idx:175,q0Idx:160,q1Idx:161,q2Idx:162,q3Idx:163,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p3Idx:188,p2Idx:189,p1Idx:190,p0Idx:191,q0Idx:176,q1Idx:177,q2Idx:178,q3Idx:179,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p3Idx:204,p2Idx:205,p1Idx:206,p0Idx:207,q0Idx:192,q1Idx:193,q2Idx:194,q3Idx:195,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p3Idx:220,p2Idx:221,p1Idx:222,p0Idx:223,q0Idx:208,q1Idx:209,q2Idx:210,q3Idx:211,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p3Idx:236,p2Idx:237,p1Idx:238,p0Idx:239,q0Idx:224,q1Idx:225,q2Idx:226,q3Idx:227,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p3Idx:252,p2Idx:253,p1Idx:254,p0Idx:255,q0Idx:240,q1Idx:241,q2Idx:242,q3Idx:243,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p1Idx:6,p0Idx:7,q0Idx:0,q1Idx:1,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p1Idx:14,p0Idx:15,q0Idx:8,q1Idx:9,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p1Idx:22,p0Idx:23,q0Idx:16,q1Idx:17,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p1Idx:30,p0Idx:31,q0Idx:24,q1Idx:25,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p1Idx:38,p0Idx:39,q0Idx:32,q1Idx:33,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p1Idx:46,p0Idx:47,q0Idx:40,q1Idx:41,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p1Idx:54,p0Idx:55,q0Idx:48,q1Idx:49,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p1Idx:62,p0Idx:63,q0Idx:56,q1Idx:57,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p1Idx:6,p0Idx:7,q0Idx:0,q1Idx:1,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p1Idx:14,p0Idx:15,q0Idx:8,q1Idx:9,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p1Idx:22,p0Idx:23,q0Idx:16,q1Idx:17,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p1Idx:30,p0Idx:31,q0Idx:24,q1Idx:25,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p1Idx:38,p0Idx:39,q0Idx:32,q1Idx:33,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p1Idx:46,p0Idx:47,q0Idx:40,q1Idx:41,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p1Idx:54,p0Idx:55,q0Idx:48,q1Idx:49,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:left,pelsQ:cur,p1Idx:62,p0Idx:63,q0Idx:56,q1Idx:57,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:1,p1Idx:2,p0Idx:3,q0Idx:4,q1Idx:5,q2Idx:6,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:17,p1Idx:18,p0Idx:19,q0Idx:20,q1Idx:21,q2Idx:22,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:33,p1Idx:34,p0Idx:35,q0Idx:36,q1Idx:37,q2Idx:38,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:49,p1Idx:50,p0Idx:51,q0Idx:52,q1Idx:53,q2Idx:54,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:65,p1Idx:66,p0Idx:67,q0Idx:68,q1Idx:69,q2Idx:70,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:81,p1Idx:82,p0Idx:83,q0Idx:84,q1Idx:85,q2Idx:86,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:97,p1Idx:98,p0Idx:99,q0Idx:100,q1Idx:101,q2Idx:102,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:113,p1Idx:114,p0Idx:115,q0Idx:116,q1Idx:117,q2Idx:118,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:129,p1Idx:130,p0Idx:131,q0Idx:132,q1Idx:133,q2Idx:134,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:145,p1Idx:146,p0Idx:147,q0Idx:148,q1Idx:149,q2Idx:150,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:161,p1Idx:162,p0Idx:163,q0Idx:164,q1Idx:165,q2Idx:166,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:177,p1Idx:178,p0Idx:179,q0Idx:180,q1Idx:181,q2Idx:182,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:193,p1Idx:194,p0Idx:195,q0Idx:196,q1Idx:197,q2Idx:198,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:209,p1Idx:210,p0Idx:211,q0Idx:212,q1Idx:213,q2Idx:214,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:225,p1Idx:226,p0Idx:227,q0Idx:228,q1Idx:229,q2Idx:230,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:241,p1Idx:242,p0Idx:243,q0Idx:244,q1Idx:245,q2Idx:246,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:0,p0Idx:1,q0Idx:2,q1Idx:3,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:8,p0Idx:9,q0Idx:10,q1Idx:11,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:16,p0Idx:17,q0Idx:18,q1Idx:19,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:24,p0Idx:25,q0Idx:26,q1Idx:27,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:32,p0Idx:33,q0Idx:34,q1Idx:35,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:40,p0Idx:41,q0Idx:42,q1Idx:43,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:48,p0Idx:49,q0Idx:50,q1Idx:51,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:56,p0Idx:57,q0Idx:58,q1Idx:59,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:0,p0Idx:1,q0Idx:2,q1Idx:3,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:8,p0Idx:9,q0Idx:10,q1Idx:11,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:16,p0Idx:17,q0Idx:18,q1Idx:19,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:24,p0Idx:25,q0Idx:26,q1Idx:27,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:32,p0Idx:33,q0Idx:34,q1Idx:35,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:40,p0Idx:41,q0Idx:42,q1Idx:43,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:48,p0Idx:49,q0Idx:50,q1Idx:51,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:56,p0Idx:57,q0Idx:58,q1Idx:59,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:5,p1Idx:6,p0Idx:7,q0Idx:8,q1Idx:9,q2Idx:10,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:21,p1Idx:22,p0Idx:23,q0Idx:24,q1Idx:25,q2Idx:26,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:37,p1Idx:38,p0Idx:39,q0Idx:40,q1Idx:41,q2Idx:42,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:53,p1Idx:54,p0Idx:55,q0Idx:56,q1Idx:57,q2Idx:58,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:69,p1Idx:70,p0Idx:71,q0Idx:72,q1Idx:73,q2Idx:74,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:85,p1Idx:86,p0Idx:87,q0Idx:88,q1Idx:89,q2Idx:90,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:101,p1Idx:102,p0Idx:103,q0Idx:104,q1Idx:105,q2Idx:106,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:117,p1Idx:118,p0Idx:119,q0Idx:120,q1Idx:121,q2Idx:122,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:133,p1Idx:134,p0Idx:135,q0Idx:136,q1Idx:137,q2Idx:138,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:149,p1Idx:150,p0Idx:151,q0Idx:152,q1Idx:153,q2Idx:154,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:165,p1Idx:166,p0Idx:167,q0Idx:168,q1Idx:169,q2Idx:170,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:181,p1Idx:182,p0Idx:183,q0Idx:184,q1Idx:185,q2Idx:186,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:197,p1Idx:198,p0Idx:199,q0Idx:200,q1Idx:201,q2Idx:202,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:213,p1Idx:214,p0Idx:215,q0Idx:216,q1Idx:217,q2Idx:218,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:229,p1Idx:230,p0Idx:231,q0Idx:232,q1Idx:233,q2Idx:234,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:245,p1Idx:246,p0Idx:247,q0Idx:248,q1Idx:249,q2Idx:250,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:2,p0Idx:3,q0Idx:4,q1Idx:5,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:10,p0Idx:11,q0Idx:12,q1Idx:13,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:18,p0Idx:19,q0Idx:20,q1Idx:21,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:26,p0Idx:27,q0Idx:28,q1Idx:29,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:34,p0Idx:35,q0Idx:36,q1Idx:37,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:42,p0Idx:43,q0Idx:44,q1Idx:45,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:50,p0Idx:51,q0Idx:52,q1Idx:53,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:58,p0Idx:59,q0Idx:60,q1Idx:61,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:2,p0Idx:3,q0Idx:4,q1Idx:5,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:10,p0Idx:11,q0Idx:12,q1Idx:13,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:18,p0Idx:19,q0Idx:20,q1Idx:21,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:26,p0Idx:27,q0Idx:28,q1Idx:29,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:34,p0Idx:35,q0Idx:36,q1Idx:37,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:42,p0Idx:43,q0Idx:44,q1Idx:45,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:50,p0Idx:51,q0Idx:52,q1Idx:53,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:58,p0Idx:59,q0Idx:60,q1Idx:61,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:9,p1Idx:10,p0Idx:11,q0Idx:12,q1Idx:13,q2Idx:14,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:25,p1Idx:26,p0Idx:27,q0Idx:28,q1Idx:29,q2Idx:30,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:41,p1Idx:42,p0Idx:43,q0Idx:44,q1Idx:45,q2Idx:46,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:57,p1Idx:58,p0Idx:59,q0Idx:60,q1Idx:61,q2Idx:62,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:73,p1Idx:74,p0Idx:75,q0Idx:76,q1Idx:77,q2Idx:78,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:89,p1Idx:90,p0Idx:91,q0Idx:92,q1Idx:93,q2Idx:94,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:105,p1Idx:106,p0Idx:107,q0Idx:108,q1Idx:109,q2Idx:110,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:121,p1Idx:122,p0Idx:123,q0Idx:124,q1Idx:125,q2Idx:126,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:137,p1Idx:138,p0Idx:139,q0Idx:140,q1Idx:141,q2Idx:142,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:153,p1Idx:154,p0Idx:155,q0Idx:156,q1Idx:157,q2Idx:158,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:169,p1Idx:170,p0Idx:171,q0Idx:172,q1Idx:173,q2Idx:174,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:185,p1Idx:186,p0Idx:187,q0Idx:188,q1Idx:189,q2Idx:190,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:201,p1Idx:202,p0Idx:203,q0Idx:204,q1Idx:205,q2Idx:206,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:217,p1Idx:218,p0Idx:219,q0Idx:220,q1Idx:221,q2Idx:222,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:233,p1Idx:234,p0Idx:235,q0Idx:236,q1Idx:237,q2Idx:238,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:249,p1Idx:250,p0Idx:251,q0Idx:252,q1Idx:253,q2Idx:254,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:4,p0Idx:5,q0Idx:6,q1Idx:7,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:12,p0Idx:13,q0Idx:14,q1Idx:15,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:20,p0Idx:21,q0Idx:22,q1Idx:23,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:28,p0Idx:29,q0Idx:30,q1Idx:31,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:36,p0Idx:37,q0Idx:38,q1Idx:39,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:44,p0Idx:45,q0Idx:46,q1Idx:47,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:52,p0Idx:53,q0Idx:54,q1Idx:55,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:60,p0Idx:61,q0Idx:62,q1Idx:63,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:4,p0Idx:5,q0Idx:6,q1Idx:7,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:12,p0Idx:13,q0Idx:14,q1Idx:15,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:20,p0Idx:21,q0Idx:22,q1Idx:23,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:28,p0Idx:29,q0Idx:30,q1Idx:31,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:36,p0Idx:37,q0Idx:38,q1Idx:39,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:44,p0Idx:45,q0Idx:46,q1Idx:47,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:52,p0Idx:53,q0Idx:54,q1Idx:55,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:60,p0Idx:61,q0Idx:62,q1Idx:63,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p3Idx:192,p2Idx:208,p1Idx:224,p0Idx:240,q0Idx:0,q1Idx:16,q2Idx:32,q3Idx:48,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p3Idx:193,p2Idx:209,p1Idx:225,p0Idx:241,q0Idx:1,q1Idx:17,q2Idx:33,q3Idx:49,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p3Idx:194,p2Idx:210,p1Idx:226,p0Idx:242,q0Idx:2,q1Idx:18,q2Idx:34,q3Idx:50,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p3Idx:195,p2Idx:211,p1Idx:227,p0Idx:243,q0Idx:3,q1Idx:19,q2Idx:35,q3Idx:51,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p3Idx:196,p2Idx:212,p1Idx:228,p0Idx:244,q0Idx:4,q1Idx:20,q2Idx:36,q3Idx:52,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p3Idx:197,p2Idx:213,p1Idx:229,p0Idx:245,q0Idx:5,q1Idx:21,q2Idx:37,q3Idx:53,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p3Idx:198,p2Idx:214,p1Idx:230,p0Idx:246,q0Idx:6,q1Idx:22,q2Idx:38,q3Idx:54,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p3Idx:199,p2Idx:215,p1Idx:231,p0Idx:247,q0Idx:7,q1Idx:23,q2Idx:39,q3Idx:55,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p3Idx:200,p2Idx:216,p1Idx:232,p0Idx:248,q0Idx:8,q1Idx:24,q2Idx:40,q3Idx:56,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p3Idx:201,p2Idx:217,p1Idx:233,p0Idx:249,q0Idx:9,q1Idx:25,q2Idx:41,q3Idx:57,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p3Idx:202,p2Idx:218,p1Idx:234,p0Idx:250,q0Idx:10,q1Idx:26,q2Idx:42,q3Idx:58,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p3Idx:203,p2Idx:219,p1Idx:235,p0Idx:251,q0Idx:11,q1Idx:27,q2Idx:43,q3Idx:59,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p3Idx:204,p2Idx:220,p1Idx:236,p0Idx:252,q0Idx:12,q1Idx:28,q2Idx:44,q3Idx:60,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p3Idx:205,p2Idx:221,p1Idx:237,p0Idx:253,q0Idx:13,q1Idx:29,q2Idx:45,q3Idx:61,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p3Idx:206,p2Idx:222,p1Idx:238,p0Idx:254,q0Idx:14,q1Idx:30,q2Idx:46,q3Idx:62,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p3Idx:207,p2Idx:223,p1Idx:239,p0Idx:255,q0Idx:15,q1Idx:31,q2Idx:47,q3Idx:63,isChroma:false}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p1Idx:48,p0Idx:56,q0Idx:0,q1Idx:8,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p1Idx:49,p0Idx:57,q0Idx:1,q1Idx:9,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p1Idx:50,p0Idx:58,q0Idx:2,q1Idx:10,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p1Idx:51,p0Idx:59,q0Idx:3,q1Idx:11,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p1Idx:52,p0Idx:60,q0Idx:4,q1Idx:12,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p1Idx:53,p0Idx:61,q0Idx:5,q1Idx:13,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p1Idx:54,p0Idx:62,q0Idx:6,q1Idx:14,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p1Idx:55,p0Idx:63,q0Idx:7,q1Idx:15,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p1Idx:48,p0Idx:56,q0Idx:0,q1Idx:8,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p1Idx:49,p0Idx:57,q0Idx:1,q1Idx:9,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p1Idx:50,p0Idx:58,q0Idx:2,q1Idx:10,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p1Idx:51,p0Idx:59,q0Idx:3,q1Idx:11,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p1Idx:52,p0Idx:60,q0Idx:4,q1Idx:12,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p1Idx:53,p0Idx:61,q0Idx:5,q1Idx:13,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p1Idx:54,p0Idx:62,q0Idx:6,q1Idx:14,isChroma:true}",
                "{bs:4,indexAlpha:0,indexBeta:0,pelsP:top,pelsQ:cur,p1Idx:55,p0Idx:63,q0Idx:7,q1Idx:15,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:16,p1Idx:32,p0Idx:48,q0Idx:64,q1Idx:80,q2Idx:96,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:17,p1Idx:33,p0Idx:49,q0Idx:65,q1Idx:81,q2Idx:97,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:18,p1Idx:34,p0Idx:50,q0Idx:66,q1Idx:82,q2Idx:98,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:19,p1Idx:35,p0Idx:51,q0Idx:67,q1Idx:83,q2Idx:99,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:20,p1Idx:36,p0Idx:52,q0Idx:68,q1Idx:84,q2Idx:100,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:21,p1Idx:37,p0Idx:53,q0Idx:69,q1Idx:85,q2Idx:101,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:22,p1Idx:38,p0Idx:54,q0Idx:70,q1Idx:86,q2Idx:102,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:23,p1Idx:39,p0Idx:55,q0Idx:71,q1Idx:87,q2Idx:103,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:24,p1Idx:40,p0Idx:56,q0Idx:72,q1Idx:88,q2Idx:104,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:25,p1Idx:41,p0Idx:57,q0Idx:73,q1Idx:89,q2Idx:105,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:26,p1Idx:42,p0Idx:58,q0Idx:74,q1Idx:90,q2Idx:106,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:27,p1Idx:43,p0Idx:59,q0Idx:75,q1Idx:91,q2Idx:107,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:28,p1Idx:44,p0Idx:60,q0Idx:76,q1Idx:92,q2Idx:108,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:29,p1Idx:45,p0Idx:61,q0Idx:77,q1Idx:93,q2Idx:109,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:30,p1Idx:46,p0Idx:62,q0Idx:78,q1Idx:94,q2Idx:110,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:31,p1Idx:47,p0Idx:63,q0Idx:79,q1Idx:95,q2Idx:111,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:0,p0Idx:8,q0Idx:16,q1Idx:24,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:1,p0Idx:9,q0Idx:17,q1Idx:25,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:2,p0Idx:10,q0Idx:18,q1Idx:26,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:3,p0Idx:11,q0Idx:19,q1Idx:27,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:4,p0Idx:12,q0Idx:20,q1Idx:28,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:5,p0Idx:13,q0Idx:21,q1Idx:29,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:6,p0Idx:14,q0Idx:22,q1Idx:30,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:7,p0Idx:15,q0Idx:23,q1Idx:31,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:0,p0Idx:8,q0Idx:16,q1Idx:24,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:1,p0Idx:9,q0Idx:17,q1Idx:25,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:2,p0Idx:10,q0Idx:18,q1Idx:26,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:3,p0Idx:11,q0Idx:19,q1Idx:27,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:4,p0Idx:12,q0Idx:20,q1Idx:28,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:5,p0Idx:13,q0Idx:21,q1Idx:29,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:6,p0Idx:14,q0Idx:22,q1Idx:30,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:7,p0Idx:15,q0Idx:23,q1Idx:31,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:80,p1Idx:96,p0Idx:112,q0Idx:128,q1Idx:144,q2Idx:160,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:81,p1Idx:97,p0Idx:113,q0Idx:129,q1Idx:145,q2Idx:161,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:82,p1Idx:98,p0Idx:114,q0Idx:130,q1Idx:146,q2Idx:162,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:83,p1Idx:99,p0Idx:115,q0Idx:131,q1Idx:147,q2Idx:163,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:84,p1Idx:100,p0Idx:116,q0Idx:132,q1Idx:148,q2Idx:164,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:85,p1Idx:101,p0Idx:117,q0Idx:133,q1Idx:149,q2Idx:165,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:86,p1Idx:102,p0Idx:118,q0Idx:134,q1Idx:150,q2Idx:166,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:87,p1Idx:103,p0Idx:119,q0Idx:135,q1Idx:151,q2Idx:167,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:88,p1Idx:104,p0Idx:120,q0Idx:136,q1Idx:152,q2Idx:168,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:89,p1Idx:105,p0Idx:121,q0Idx:137,q1Idx:153,q2Idx:169,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:90,p1Idx:106,p0Idx:122,q0Idx:138,q1Idx:154,q2Idx:170,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:91,p1Idx:107,p0Idx:123,q0Idx:139,q1Idx:155,q2Idx:171,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:92,p1Idx:108,p0Idx:124,q0Idx:140,q1Idx:156,q2Idx:172,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:93,p1Idx:109,p0Idx:125,q0Idx:141,q1Idx:157,q2Idx:173,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:94,p1Idx:110,p0Idx:126,q0Idx:142,q1Idx:158,q2Idx:174,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:95,p1Idx:111,p0Idx:127,q0Idx:143,q1Idx:159,q2Idx:175,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:16,p0Idx:24,q0Idx:32,q1Idx:40,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:17,p0Idx:25,q0Idx:33,q1Idx:41,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:18,p0Idx:26,q0Idx:34,q1Idx:42,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:19,p0Idx:27,q0Idx:35,q1Idx:43,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:20,p0Idx:28,q0Idx:36,q1Idx:44,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:21,p0Idx:29,q0Idx:37,q1Idx:45,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:22,p0Idx:30,q0Idx:38,q1Idx:46,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:23,p0Idx:31,q0Idx:39,q1Idx:47,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:16,p0Idx:24,q0Idx:32,q1Idx:40,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:17,p0Idx:25,q0Idx:33,q1Idx:41,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:18,p0Idx:26,q0Idx:34,q1Idx:42,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:19,p0Idx:27,q0Idx:35,q1Idx:43,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:20,p0Idx:28,q0Idx:36,q1Idx:44,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:21,p0Idx:29,q0Idx:37,q1Idx:45,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:22,p0Idx:30,q0Idx:38,q1Idx:46,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:23,p0Idx:31,q0Idx:39,q1Idx:47,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:144,p1Idx:160,p0Idx:176,q0Idx:192,q1Idx:208,q2Idx:224,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:145,p1Idx:161,p0Idx:177,q0Idx:193,q1Idx:209,q2Idx:225,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:146,p1Idx:162,p0Idx:178,q0Idx:194,q1Idx:210,q2Idx:226,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:147,p1Idx:163,p0Idx:179,q0Idx:195,q1Idx:211,q2Idx:227,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:148,p1Idx:164,p0Idx:180,q0Idx:196,q1Idx:212,q2Idx:228,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:149,p1Idx:165,p0Idx:181,q0Idx:197,q1Idx:213,q2Idx:229,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:150,p1Idx:166,p0Idx:182,q0Idx:198,q1Idx:214,q2Idx:230,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:151,p1Idx:167,p0Idx:183,q0Idx:199,q1Idx:215,q2Idx:231,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:152,p1Idx:168,p0Idx:184,q0Idx:200,q1Idx:216,q2Idx:232,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:153,p1Idx:169,p0Idx:185,q0Idx:201,q1Idx:217,q2Idx:233,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:154,p1Idx:170,p0Idx:186,q0Idx:202,q1Idx:218,q2Idx:234,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:155,p1Idx:171,p0Idx:187,q0Idx:203,q1Idx:219,q2Idx:235,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:156,p1Idx:172,p0Idx:188,q0Idx:204,q1Idx:220,q2Idx:236,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:157,p1Idx:173,p0Idx:189,q0Idx:205,q1Idx:221,q2Idx:237,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:158,p1Idx:174,p0Idx:190,q0Idx:206,q1Idx:222,q2Idx:238,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p2Idx:159,p1Idx:175,p0Idx:191,q0Idx:207,q1Idx:223,q2Idx:239,isChroma:false}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:32,p0Idx:40,q0Idx:48,q1Idx:56,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:33,p0Idx:41,q0Idx:49,q1Idx:57,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:34,p0Idx:42,q0Idx:50,q1Idx:58,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:35,p0Idx:43,q0Idx:51,q1Idx:59,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:36,p0Idx:44,q0Idx:52,q1Idx:60,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:37,p0Idx:45,q0Idx:53,q1Idx:61,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:38,p0Idx:46,q0Idx:54,q1Idx:62,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:39,p0Idx:47,q0Idx:55,q1Idx:63,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:32,p0Idx:40,q0Idx:48,q1Idx:56,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:33,p0Idx:41,q0Idx:49,q1Idx:57,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:34,p0Idx:42,q0Idx:50,q1Idx:58,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:35,p0Idx:43,q0Idx:51,q1Idx:59,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:36,p0Idx:44,q0Idx:52,q1Idx:60,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:37,p0Idx:45,q0Idx:53,q1Idx:61,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:38,p0Idx:46,q0Idx:54,q1Idx:62,isChroma:true}",
                "{bs:1,indexAlpha:0,indexBeta:0,pelsP:cur,pelsQ:cur,p1Idx:39,p0Idx:47,q0Idx:55,q1Idx:63,isChroma:true}" };

        Assert.assertArrayEquals(expected, actions.toArray(new String[0]));
    }
}
