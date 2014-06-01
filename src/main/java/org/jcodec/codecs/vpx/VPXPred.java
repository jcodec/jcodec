package org.jcodec.codecs.vpx;

import java.util.Arrays;

import org.jcodec.common.ArrayUtil;
import org.jcodec.common.tools.MathUtil;

public class VPXPred {

    public static void predTM(int x, int yOff, int[] leftRow, int[] topLine, int[] topLeft, int[] pred, int size) {
        for (int i = 0, off = 0; i < size; i++) {
            for (int j = 0; j < size; j++, off++) {
                pred[off] = MathUtil.clip(leftRow[i + yOff] + topLine[j + x] - topLeft[yOff], 0, 255);
            }
        }
    }

    private static void predVer(int x, int y, int[] leftRow, int[] topLine, int[] pred, int size) {
        for (int i = 0, off = 0; i < size; i++) {
            for (int j = 0; j < size; j++, off++) {
                pred[off] = topLine[j + x];
            }
        }
    }

    private static void predHor(int x, int y, int[] leftRow, int[] topLine, int[] pred, int size) {
        for (int i = 0, off = 0; i < size; i++) {
            for (int j = 0; j < size; j++, off++) {
                pred[off] = leftRow[i];
            }
        }
    }

    private static void lumaDCPred(int x, int y, int[] leftRow, int[] topLine, int[] pred, int log2Size) {
        int val;
        if (x == 0 && y == 0)
            val = 128;
        else if (y == 0) {
            int round = 1 << (log2Size - 1);
            val = (ArrayUtil.sum(leftRow) + round) >> log2Size;
        } else if (x == 0) {
            int round = 1 << (log2Size - 1), count = 1 << log2Size;
            val = (ArrayUtil.sum(topLine, x, count) + round) >> log2Size;
        } else {
            int round = 1 << log2Size;
            val = (ArrayUtil.sum(leftRow) + ArrayUtil.sum(topLine, x, round) + round) >> (log2Size + 1);
        }
        Arrays.fill(pred, val);
    }

    public static void pred816(int chromaMode, int x, int y, int[] leftRow, int[] topLine, int[] topLeft, int[] pred,
            int log2Size) {
        if (chromaMode == VPXConst.DC_PRED)
            lumaDCPred(x, y, leftRow, topLine, pred, log2Size);
        else if (chromaMode == VPXConst.H_PRED)
            predHor(x, y, leftRow, topLine, pred, 1 << log2Size);
        else if (chromaMode == VPXConst.V_PRED)
            predVer(x, y, leftRow, topLine, pred, 1 << log2Size);
        else {
            int size = 1 << log2Size;
            predTM(x, y & (size - 1), leftRow, topLine, topLeft, pred, size);
        }
    }

    public static void vp8BPred(int mode, int[] leftRow, int[] topLine, int t15, int[] topLeft, int blkAbsX,
            int blkOffY, int[] out) {
        int l3 = leftRow[blkOffY + 3];
        int l2 = leftRow[blkOffY + 2];
        int l1 = leftRow[blkOffY + 1];
        int l0 = leftRow[blkOffY];
        int tl = topLeft[blkOffY];

        switch (mode) {
        case VPXConst.B_HE_PRED:
            predictHE(out, l3, l2, l1, l0, tl);
            break;
        case VPXConst.B_HU_PRED:
            predictHU(out, l3, l2, l1, l0);
            break;
        default:
            int t0 = topLine[blkAbsX];
            int t1 = topLine[blkAbsX + 1];
            int t2 = topLine[blkAbsX + 2];
            int t3 = topLine[blkAbsX + 3];

            switch (mode) {

            case VPXConst.B_DC_PRED:
                predictDC(blkAbsX, out, l3, l2, l1, l0, t0, t1, t2, t3);
                break;
            case VPXConst.B_TM_PRED:
                predTM(blkAbsX, blkOffY, leftRow, topLine, topLeft, out, 4);
                break;
            case VPXConst.B_RD_PRED:
                predictRD(out, l3, l2, l1, l0, tl, t0, t1, t2, t3);
                break;
            case VPXConst.B_VR_PRED:
                predictVR(out, l3, l2, l1, l0, tl, t0, t1, t2, t3);
                break;

            case VPXConst.B_HD_PRED:
                predictHD(out, l3, l2, l1, l0, tl, t0, t1, t2, t3);
                break;
            default:
                int t4 = t15,
                t5 = t15,
                t6 = t15,
                t7 = t15;
                if (blkAbsX < topLine.length - 4) {
                    t4 = topLine[blkAbsX + 4];
                    t5 = topLine[blkAbsX + 5];
                    t6 = topLine[blkAbsX + 6];
                    t7 = topLine[blkAbsX + 7];
                }
                switch (mode) {
                case VPXConst.B_VE_PRED:
                    predictVE(out, tl, t0, t1, t2, t3, t4);
                    break;
                case VPXConst.B_LD_PRED:
                    predictLD(out, t0, t1, t2, t3, t4, t5, t6, t7);
                    break;
                case VPXConst.B_VL_PRED:
                    predictVL(out, t0, t1, t2, t3, t4, t5, t6, t7);
                    break;
                }
            }
        }
    }

    public static void predictHU(int[] out, int l3, int l2, int l1, int l0) {
        out[0] = avg2(l0, l1);
        out[1] = avg3(l0, l1, l2);
        out[2] = out[4] = avg2(l1, l2);
        out[3] = out[5] = avg3(l1, l2, l3);
        out[6] = out[8] = avg2(l2, l3);
        out[7] = out[9] = avg3(l2, l3, l3);
        out[10] = out[11] = out[12] = out[13] = out[14] = out[15] = l3;
    }

    public static void predictHD(int[] out, int l3, int l2, int l1, int l0, int tl, int t0, int t1, int t2, int t3) {

        out[12] = avg2(l3, l2);
        out[13] = avg3(l3, l2, l1);
        out[8] = out[14] = avg2(l2, l1);
        out[9] = out[15] = avg3(l2, l1, l0);
        out[10] = out[4] = avg2(l1, l0);
        out[11] = out[5] = avg3(l1, l0, tl);
        out[6] = out[0] = avg2(l0, tl);
        out[7] = out[1] = avg3(l0, tl, t0);
        out[2] = avg3(tl, t0, t1);
        out[3] = avg3(t0, t1, t2);
    }

    public static void predictVL(int[] out, int t0, int t1, int t2, int t3, int t4, int t5, int t6, int t7) {
        out[0] = avg2(t0, t1);
        out[4] = avg3(t0, t1, t2);
        out[8] = out[1] = avg2(t1, t2);
        out[5] = out[12] = avg3(t1, t2, t3);
        out[9] = out[2] = avg2(t2, t3);
        out[13] = out[6] = avg3(t2, t3, t4);
        out[10] = out[3] = avg2(t3, t4);
        out[14] = out[7] = avg3(t3, t4, t5);
        out[11] = avg3(t4, t5, t6);
        out[15] = avg3(t5, t6, t7);
    }

    public static void predictVR(int[] out, int l3, int l2, int l1, int l0, int tl, int t0, int t1, int t2, int t3) {
        out[12] = avg3(l2, l1, l0);
        out[8] = avg3(l1, l0, tl);
        out[13] = out[4] = avg3(l0, tl, t0);
        out[9] = out[0] = avg2(tl, t0);
        out[14] = out[5] = avg3(tl, t0, t1);
        out[10] = out[1] = avg2(t0, t1);
        out[15] = out[6] = avg3(t0, t1, t2);
        out[11] = out[2] = avg2(t1, t2);
        out[7] = avg3(t1, t2, t3);
        out[3] = avg2(t2, t3);
    }

    public static void predictRD(int[] out, int l3, int l2, int l1, int l0, int tl, int t0, int t1, int t2, int t3) {
        out[12] = avg3(l3, l2, l1);
        out[13] = out[8] = avg3(l2, l1, l0);
        out[14] = out[9] = out[4] = avg3(l1, l0, tl);
        out[15] = out[10] = out[5] = out[0] = avg3(l0, tl, t0);
        out[11] = out[6] = out[1] = avg3(tl, t0, t1);
        out[7] = out[2] = avg3(t0, t1, t2);
        out[3] = avg3(t1, t2, t3);
    }

    public static void predictLD(int[] out, int t0, int t1, int t2, int t3, int t4, int t5, int t6, int t7) {
        out[0] = avg3(t0, t1, t2);
        out[1] = out[4] = avg3(t1, t2, t3);
        out[2] = out[5] = out[8] = avg3(t2, t3, t4);
        out[3] = out[6] = out[9] = out[12] = avg3(t3, t4, t5);
        out[7] = out[10] = out[13] = avg3(t4, t5, t6);
        out[11] = out[14] = avg3(t5, t6, t7);
        out[15] = avg3(t6, t7, t7);
    }

    public static void predictHE(int[] out, int l3, int l2, int l1, int l0, int tl) {
        out[12] = out[13] = out[14] = out[15] = avg3(l2, l3, l3);
        out[8] = out[9] = out[10] = out[11] = avg3(l1, l2, l3);
        out[4] = out[5] = out[6] = out[7] = avg3(l0, l1, l2);
        out[0] = out[1] = out[2] = out[3] = avg3(tl, l0, l1);
    }

    public static void predictVE(int[] out, int tl, int t0, int t1, int t2, int t3, int t4) {
        out[0] = out[4] = out[8] = out[12] = avg3(tl, t0, t1);
        out[1] = out[5] = out[9] = out[13] = avg3(t0, t1, t2);
        out[2] = out[6] = out[10] = out[14] = avg3(t1, t2, t3);
        out[3] = out[7] = out[11] = out[15] = avg3(t2, t3, t4);
    }

    public static int avg2(int x, int y) {
        return (x + y + 1) >> 1;
    }

    public static int avg3(int x, int y, int z) {
        return (x + y + y + z + 2) >> 2;
    }

    public static void predictDC(int blkAbsX, int[] out, int l3, int l2, int l1, int l0, int t0, int t1, int t2, int t3) {
        int val = (l0 + l1 + l2 + l3 + t0 + t1 + t2 + t3 + 4) >> 3;

        Arrays.fill(out, val);
    }
}