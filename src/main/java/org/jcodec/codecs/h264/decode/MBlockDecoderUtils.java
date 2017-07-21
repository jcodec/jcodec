package org.jcodec.codecs.h264.decode;
import static java.lang.System.arraycopy;
import static org.jcodec.codecs.h264.H264Utils.Mv.mvC;
import static org.jcodec.codecs.h264.H264Utils.Mv.mvRef;
import static org.jcodec.codecs.h264.H264Utils.Mv.packMv;
import static org.jcodec.common.ArrayUtil.shiftLeft1;
import static org.jcodec.common.tools.MathUtil.clip;

import org.jcodec.codecs.h264.H264Utils.MvList;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.Picture8Bit;

public class MBlockDecoderUtils {
    private static boolean debug;
    public static final int NULL_VECTOR = packMv(0, 0, -1);

    public static void debugPrint(Object... arguments) {
        if (debug && arguments.length > 0) {
            if (arguments.length == 1) {
                Logger.debug(""+arguments[0]);
            } else {
                String fmt = (String)arguments[0];
                shiftLeft1(arguments);
                Logger.debug(String.format(fmt, arguments));
            }
        }
    }

    static void collectPredictors(DecoderState sharedState, Picture8Bit outMB, int mbX) {
        sharedState.topLeft[0][0] = sharedState.topLine[0][(mbX << 4) + 15];
        sharedState.topLeft[0][1] = outMB.getPlaneData(0)[63];
        sharedState.topLeft[0][2] = outMB.getPlaneData(0)[127];
        sharedState.topLeft[0][3] = outMB.getPlaneData(0)[191];
        arraycopy(outMB.getPlaneData(0), 240, sharedState.topLine[0], mbX << 4, 16);
        copyCol(outMB.getPlaneData(0), 16, 15, 16, sharedState.leftRow[0]);

        collectChromaPredictors(sharedState, outMB, mbX);
    }

    static void collectChromaPredictors(DecoderState sharedState, Picture8Bit outMB, int mbX) {
        sharedState.topLeft[1][0] = sharedState.topLine[1][(mbX << 3) + 7];
        sharedState.topLeft[2][0] = sharedState.topLine[2][(mbX << 3) + 7];

        arraycopy(outMB.getPlaneData(1), 56, sharedState.topLine[1], mbX << 3, 8);
        arraycopy(outMB.getPlaneData(2), 56, sharedState.topLine[2], mbX << 3, 8);

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
                di.mvs.setMv(blkOffX, blkOffY, 0, NULL_VECTOR);
                di.mvs.setMv(blkOffX, blkOffY, 1, NULL_VECTOR);
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

    static void saveVect(MvList mv, int list, int from, int to, int vect) {
        for (int i = from; i < to; i++) {
            mv.setMv(i, list, vect);
        }
    }

    /**
     * Calculates median prediction 
     * @param a, b, c and d are packed motion vectors
     */
    public static int calcMVPredictionMedian(int a, int b, int c, int d, boolean aAvb, boolean bAvb, boolean cAvb,
            boolean dAvb, int ref, int comp) {

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

        if (mvRef(a) == ref && mvRef(b) != ref && mvRef(c) != ref)
            return mvC(a, comp);
        else if (mvRef(b) == ref && mvRef(a) != ref && mvRef(c) != ref)
            return mvC(b, comp);
        else if (mvRef(c) == ref && mvRef(a) != ref && mvRef(b) != ref)
            return mvC(c, comp);

        return mvC(a, comp) + mvC(b, comp) + mvC(c, comp) - min(mvC(a, comp), mvC(b, comp), mvC(c, comp))
                - max(mvC(a, comp), mvC(b, comp), mvC(c, comp));
    }

    public static int max(int x, int x2, int x3) {
        return x > x2 ? (x > x3 ? x : x3) : (x2 > x3 ? x2 : x3);
    }

    public static int min(int x, int x2, int x3) {
        return x < x2 ? (x < x3 ? x : x3) : (x2 < x3 ? x2 : x3);
    }

    static void saveMvs(DeblockerInput di, MvList x, int mbX, int mbY) {
        for (int j = 0, blkOffY = mbY << 2, blkInd = 0; j < 4; j++, blkOffY++) {
            for (int i = 0, blkOffX = mbX << 2; i < 4; i++, blkOffX++, blkInd++) {
                di.mvs.setMv(blkOffX, blkOffY, 0, x.getMv(blkInd, 0));
                di.mvs.setMv(blkOffX, blkOffY, 1, x.getMv(blkInd, 1));
            }
        }
    }

    static void savePrediction8x8(DecoderState sharedState, int mbX, MvList x) {
        sharedState.mvTopLeft.copyPair(0, sharedState.mvTop, (mbX << 2) + 3);
        sharedState.mvLeft.copyPair(0, x, 3);
        sharedState.mvLeft.copyPair(1, x, 7);
        sharedState.mvLeft.copyPair(2, x, 11);
        sharedState.mvLeft.copyPair(3, x, 15);
        sharedState.mvTop.copyPair(mbX << 2, x, 12);
        sharedState.mvTop.copyPair((mbX << 2) + 1, x, 13);
        sharedState.mvTop.copyPair((mbX << 2) + 2, x, 14);
        sharedState.mvTop.copyPair((mbX << 2) + 3, x, 15);
    }

    public static void saveVectIntra(DecoderState sharedState, int mbX) {
        int xx = mbX << 2;

        sharedState.mvTopLeft.copyPair(0, sharedState.mvTop, xx + 3);

        saveVect(sharedState.mvTop, 0, xx, xx + 4, NULL_VECTOR);
        saveVect(sharedState.mvLeft, 0, 0, 4, NULL_VECTOR);
        saveVect(sharedState.mvTop, 1, xx, xx + 4, NULL_VECTOR);
        saveVect(sharedState.mvLeft, 1, 0, 4, NULL_VECTOR);
    }
}
