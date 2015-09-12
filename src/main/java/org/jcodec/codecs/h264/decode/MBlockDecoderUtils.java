package org.jcodec.codecs.h264.decode;

import static org.jcodec.common.tools.MathUtil.clip;

import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.Picture8Bit;

public class MBlockDecoderUtils {
    private static boolean debug;
    public static final int[] NULL_VECTOR = new int[] { 0, 0, -1 };

    public static void debugPrint(String str) {
        if (debug)
            Logger.debug(str);
    }

    public static void setDebug(boolean newValue) {
        debug = newValue;
    }

    static void collectPredictors(DecoderState sharedState, Picture8Bit outMB, int mbX) {
        sharedState.topLeft[0][0] = sharedState.topLine[0][(mbX << 4) + 15];
        sharedState.topLeft[0][1] = outMB.getPlaneData(0)[63];
        sharedState.topLeft[0][2] = outMB.getPlaneData(0)[127];
        sharedState.topLeft[0][3] = outMB.getPlaneData(0)[191];
        System.arraycopy(outMB.getPlaneData(0), 240, sharedState.topLine[0], mbX << 4, 16);
        copyCol(outMB.getPlaneData(0), 16, 15, 16, sharedState.leftRow[0]);

        collectChromaPredictors(sharedState, outMB, mbX);
    }

    static void collectChromaPredictors(DecoderState sharedState, Picture8Bit outMB, int mbX) {
        sharedState.topLeft[1][0] = sharedState.topLine[1][(mbX << 3) + 7];
        sharedState.topLeft[2][0] = sharedState.topLine[2][(mbX << 3) + 7];

        System.arraycopy(outMB.getPlaneData(1), 56, sharedState.topLine[1], mbX << 3, 8);
        System.arraycopy(outMB.getPlaneData(2), 56, sharedState.topLine[2], mbX << 3, 8);

        copyCol(outMB.getPlaneData(1), 8, 7, 8, sharedState.leftRow[1]);
        copyCol(outMB.getPlaneData(2), 8, 7, 8, sharedState.leftRow[2]);
    }

    private static void copyCol(byte[] planeData, int n, int off, int stride, byte[] out) {
        for (int i = 0; i < n; i++, off += stride) {
            out[i] = planeData[off];
        }
    }

    static void saveMvsIntra(DeblockerInput di, int mbX, int mbY) {
        for (int j = 0, blkOffY = mbY << 2, blkInd = 0; j < 4; j++, blkOffY++) {
            for (int i = 0, blkOffX = mbX << 2; i < 4; i++, blkOffX++, blkInd++) {
                di.mvs[0][blkOffY][blkOffX] = NULL_VECTOR;
                di.mvs[1][blkOffY][blkOffX] = NULL_VECTOR;
            }
        }
    }

    static void mergeResidual(Picture8Bit mb, int[][][] residual, int[][] blockLUT, int[][] posLUT) {
        for (int comp = 0; comp < 3; comp++) {
            byte[] to = mb.getPlaneData(comp);
            for (int i = 0; i < to.length; i++) {
                to[i] = (byte) clip(to[i] + residual[comp][blockLUT[comp][i]][posLUT[comp][i]], -128, 127);
            }
        }
    }

    static void saveVect(int[][] mv, int from, int to, int x, int y, int r) {
        for (int i = from; i < to; i++) {
            mv[i][0] = x;
            mv[i][1] = y;
            mv[i][2] = r;
        }
    }

    public static int calcMVPredictionMedian(int[] a, int[] b, int[] c, int[] d, boolean aAvb, boolean bAvb,
            boolean cAvb, boolean dAvb, int ref, int comp) {

        if (!cAvb) {
            c = d;
            cAvb = dAvb;
        }

        if (aAvb && !bAvb && !cAvb) {
            b = c = a;
            bAvb = cAvb = aAvb;
        }

        a = aAvb ? a : NULL_VECTOR;
        b = bAvb ? b : NULL_VECTOR;
        c = cAvb ? c : NULL_VECTOR;

        if (a[2] == ref && b[2] != ref && c[2] != ref)
            return a[comp];
        else if (b[2] == ref && a[2] != ref && c[2] != ref)
            return b[comp];
        else if (c[2] == ref && a[2] != ref && b[2] != ref)
            return c[comp];

        return a[comp] + b[comp] + c[comp] - min(a[comp], b[comp], c[comp]) - max(a[comp], b[comp], c[comp]);
    }

    public static int max(int x, int x2, int x3) {
        return x > x2 ? (x > x3 ? x : x3) : (x2 > x3 ? x2 : x3);
    }

    public static int min(int x, int x2, int x3) {
        return x < x2 ? (x < x3 ? x : x3) : (x2 < x3 ? x2 : x3);
    }

    static void copyVect(int[] to, int[] from) {
        to[0] = from[0];
        to[1] = from[1];
        to[2] = from[2];
    }

    static void saveMvs(DeblockerInput di, int[][][] x, int mbX, int mbY) {
        for (int j = 0, blkOffY = mbY << 2, blkInd = 0; j < 4; j++, blkOffY++) {
            for (int i = 0, blkOffX = mbX << 2; i < 4; i++, blkOffX++, blkInd++) {
                di.mvs[0][blkOffY][blkOffX] = x[0][blkInd];
                di.mvs[1][blkOffY][blkOffX] = x[1][blkInd];
            }
        }
    }

    static void savePrediction8x8(DecoderState sharedState, int mbX, int[][] x, int list) {
        copyVect(sharedState.mvTopLeft[list], sharedState.mvTop[list][(mbX << 2) + 3]);
        copyVect(sharedState.mvLeft[list][0], x[3]);
        copyVect(sharedState.mvLeft[list][1], x[7]);
        copyVect(sharedState.mvLeft[list][2], x[11]);
        copyVect(sharedState.mvLeft[list][3], x[15]);
        copyVect(sharedState.mvTop[list][mbX << 2], x[12]);
        copyVect(sharedState.mvTop[list][(mbX << 2) + 1], x[13]);
        copyVect(sharedState.mvTop[list][(mbX << 2) + 2], x[14]);
        copyVect(sharedState.mvTop[list][(mbX << 2) + 3], x[15]);
    }

    public static void saveVectIntra(DecoderState sharedState, int mbX) {
        int xx = mbX << 2;

        copyVect(sharedState.mvTopLeft[0], sharedState.mvTop[0][xx + 3]);
        copyVect(sharedState.mvTopLeft[1], sharedState.mvTop[1][xx + 3]);

        saveVect(sharedState.mvTop[0], xx, xx + 4, 0, 0, -1);
        saveVect(sharedState.mvLeft[0], 0, 4, 0, 0, -1);
        saveVect(sharedState.mvTop[1], xx, xx + 4, 0, 0, -1);
        saveVect(sharedState.mvLeft[1], 0, 4, 0, 0, -1);
    }

}
