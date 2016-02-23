package org.jcodec.codecs.mpeg12;

import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * MPEG 1/2 decoder interframe motion compensation routines, octal interpolation
 * 
 * @author The JCodec project
 * 
 */
public class MPEGPredOct extends MPEGPred {
    // Max block size is 16x16
    private int[] tmp = new int[16 * 21];

    // TODO: this really implements interpolation for QUAD downsampling, add another linear step for OCT.
    // This is also to be complient with MPEG spec which uses linear interpolation.
    private static final int[][] COEFF = {

    { 0, 0, 128, 0, 0, 0 },

    { 0, -6, 123, 12, -1, 0 },

    { 2, -11, 108, 36, -8, 1 },

    { 0, -9, 93, 50, -6, 0 },

    { 3, -16, 77, 77, -16, 3 },

    { 0, -6, 50, 93, -9, 0 },

    { 1, -8, 36, 108, -11, 2 },

    { 0, -1, 12, 123, -6, 0 }

    };

    public MPEGPredOct(MPEGPred other) {
        super(other);
    }

    @Override
    public void predictPlane(byte[] ref, int refX, int refY, int refW, int refH, int refVertStep, int refVertOff,
            int[] tgt, int tgtY, int tgtW, int tgtH, int tgtVertStep) {
        int rx = refX >> 3, ry = refY >> 3;
        tgtW >>= 3;
        tgtH >>= 3;

        boolean safe = rx >= 2 && ry >= 2 && rx + tgtW + 3 < refW && ((ry + tgtH + 3) << refVertStep) < refH;
        if ((refX & 0x7) == 0) {
            if ((refY & 0x7) == 0) {
                if (safe)
                    predictFullXFullYSafe(ref, rx, ry, refW, refH, refVertStep, refVertOff, tgt, tgtY, tgtW, tgtH,
                            tgtVertStep);
                else
                    predictFullXFullYUnSafe(ref, rx, ry, refW, refH, refVertStep, refVertOff, tgt, tgtY, tgtW, tgtH,
                            tgtVertStep);
            } else {
                if (safe)
                    predictFullXSubYSafe(ref, rx, ry, refY & 0x7, refW, refH, refVertStep, refVertOff, tgt, tgtY, tgtW,
                            tgtH, tgtVertStep);
                else
                    predictFullXSubYUnSafe(ref, rx, ry, refY & 0x7, refW, refH, refVertStep, refVertOff, tgt, tgtY,
                            tgtW, tgtH, tgtVertStep);
            }
        } else if ((refY & 0x7) == 0) {
            if (safe)
                predictSubXFullYSafe(ref, rx, refX & 0x7, ry, refW, refH, refVertStep, refVertOff, tgt, tgtY, tgtW,
                        tgtH, tgtVertStep);
            else
                predictSubXFullYUnSafe(ref, rx, refX & 0x7, ry, refW, refH, refVertStep, refVertOff, tgt, tgtY, tgtW,
                        tgtH, tgtVertStep);
        } else {
            if (safe)
                predictSubXSubYSafe(ref, rx, refX & 0x7, ry, refY & 0x7, refW, refH, refVertStep, refVertOff, tgt,
                        tgtY, tgtW, tgtH, tgtVertStep);
            else
                predictSubXSubYUnSafe(ref, rx, refX & 0x7, ry, refY & 0x7, refW, refH, refVertStep, refVertOff, tgt,
                        tgtY, tgtW, tgtH, tgtVertStep);
        }
    }

    protected int getPix6(byte[] ref, int refW, int refH, int x, int y, int refVertStep, int refVertOff, int[] coeff) {
        int lastLine = refH - (1 << refVertStep) + refVertOff;
        int x0 = MathUtil.clip(x - 2, 0, refW - 1);
        int x1 = MathUtil.clip(x - 1, 0, refW - 1);
        int x2 = MathUtil.clip(x, 0, refW - 1);
        int x3 = MathUtil.clip(x + 1, 0, refW - 1);
        int x4 = MathUtil.clip(x + 2, 0, refW - 1);
        int x5 = MathUtil.clip(x + 3, 0, refW - 1);
        int off = MathUtil.clip(y, refVertOff, lastLine) * refW;

        return ref[off + x0] * coeff[0] + ref[off + x1] * coeff[1] + ref[off + x2] * coeff[2] + ref[off + x3]
                * coeff[3] + ref[off + x4] * coeff[4] + ref[off + x5] * coeff[5] + 16384;
    }

    protected int getPix6Vert(byte[] ref, int refW, int refH, int x, int y, int refVertStep, int refVertOff, int[] coeff) {
        int lastLine = refH - (1 << refVertStep) + refVertOff;
        int y0 = MathUtil.clip(y - (2 << refVertStep), refVertOff, lastLine);
        int y1 = MathUtil.clip(y - (1 << refVertStep), refVertOff, lastLine);
        int y2 = MathUtil.clip(y, 0, lastLine);
        int y3 = MathUtil.clip(y + (1 << refVertStep), refVertOff, lastLine);
        int y4 = MathUtil.clip(y + (2 << refVertStep), refVertOff, lastLine);
        int y5 = MathUtil.clip(y + (3 << refVertStep), refVertOff, lastLine);
        x = MathUtil.clip(x, 0, refW - 1);

        return ref[y0 * refW + x] * coeff[0] + ref[y1 * refW + x] * coeff[1] + ref[y2 * refW + x] * coeff[2]
                + ref[y3 * refW + x] * coeff[3] + ref[y4 * refW + x] * coeff[4] + ref[y5 * refW + x] * coeff[5] + 16384;
    }

    private void predictSubXSubYUnSafe(byte[] ref, int rx, int ix, int ry, int iy, int refW, int refH, int refVertStep,
            int refVertOff, int[] tgt, int tgtY, int tgtW, int tgtH, int tgtVertStep) {

        int offTgt = tgtW * tgtY;
        int dblTgtW = tgtW << 1;
        int tripleTgtW = dblTgtW + tgtW;
        int lfTgt = tgtVertStep * tgtW;

        int[] coeff = COEFF[ix];
        for (int i = -2, offTmp = 0; i < tgtH + 3; i++) {
            int y = ((i + ry) << refVertStep) + refVertOff;
            for (int j = 0; j < tgtW; j++, ++offTmp) {
                tmp[offTmp] = getPix6(ref, refW, refH, j + rx, y, refVertStep, refVertOff, coeff);
            }
        }

        coeff = COEFF[iy];
        for (int i = 0, offTmp = dblTgtW; i < tgtH; i++) {
            for (int j = 0; j < tgtW; j++, ++offTmp, ++offTgt) {
                tgt[offTgt] = (tmp[offTmp - dblTgtW] * coeff[0] + tmp[offTmp - tgtW] * coeff[1] + tmp[offTmp]
                        * coeff[2] + tmp[offTmp + tgtW] * coeff[3] + tmp[offTmp + dblTgtW] * coeff[4]
                        + tmp[offTmp + tripleTgtW] * coeff[5] + 8192) >> 14;
            }
            offTgt += lfTgt;
        }
    }

    private void predictSubXSubYSafe(byte[] ref, int rx, int ix, int ry, int iy, int refW, int refH, int refVertStep,
            int refVertOff, int[] tgt, int tgtY, int tgtW, int tgtH, int tgtVertStep) {

        int[] coeff = COEFF[ix];

        int offRef = (((ry - 2) << refVertStep) + refVertOff) * refW + rx;
        int offTgt = tgtW * tgtY;
        int lfRef = (refW << refVertStep) - tgtW;
        int lfTgt = tgtVertStep * tgtW;
        int dblTgtW = tgtW << 1;
        int tripleTgtW = dblTgtW + tgtW;

        for (int i = 0, offTmp = 0; i < tgtH + 5; i++) {
            for (int j = 0; j < tgtW; j++, ++offTmp, ++offRef) {
                tmp[offTmp] = ref[offRef - 2] * coeff[0] + ref[offRef - 1] * coeff[1] + ref[offRef] * coeff[2]
                        + ref[offRef + 1] * coeff[3] + ref[offRef + 2] * coeff[4] + ref[offRef + 3] * coeff[5];
            }
            offRef += lfRef;
        }

        coeff = COEFF[iy];
        for (int i = 0, offTmp = dblTgtW; i < tgtH; i++) {
            for (int j = 0; j < tgtW; j++, ++offTmp, ++offTgt) {
                tgt[offTgt] = ((tmp[offTmp - dblTgtW] * coeff[0] + tmp[offTmp - tgtW] * coeff[1] + tmp[offTmp]
                        * coeff[2] + tmp[offTmp + tgtW] * coeff[3] + tmp[offTmp + dblTgtW] * coeff[4]
                        + tmp[offTmp + tripleTgtW] * coeff[5] + 8192) >> 14) + 128;
            }
            offTgt += lfTgt;
        }
    }

    private void predictSubXFullYUnSafe(byte[] ref, int rx, int ix, int ry, int refW, int refH, int refVertStep,
            int refVertOff, int[] tgt, int tgtY, int tgtW, int tgtH, int tgtVertStep) {
        int[] coeff = COEFF[ix];

        int tgtOff = tgtW * tgtY;
        int lfTgt = tgtVertStep * tgtW;

        for (int i = 0; i < tgtH; i++) {
            int y = ((i + ry) << refVertStep) + refVertOff;
            for (int j = 0; j < tgtW; j++) {
                tgt[tgtOff++] = (getPix6(ref, refW, refH, j + rx, y, refVertStep, refVertOff, coeff) + 64) >> 7;
            }
            tgtOff += lfTgt;
        }
    }

    private void predictSubXFullYSafe(byte[] ref, int rx, int ix, int ry, int refW, int refH, int refVertStep,
            int refVertOff, int[] tgt, int tgtY, int tgtW, int tgtH, int tgtVertStep) {
        int[] coeff = COEFF[ix];

        int offRef = ((ry << refVertStep) + refVertOff) * refW + rx;
        int offTgt = tgtW * tgtY;
        int lfRef = (refW << refVertStep) - tgtW;
        int lfTgt = tgtVertStep * tgtW;

        for (int i = 0; i < tgtH; i++) {
            for (int j = 0; j < tgtW; j++, ++offRef) {
                tgt[offTgt++] = ((ref[offRef - 2] * coeff[0] + ref[offRef - 1] * coeff[1] + ref[offRef] * coeff[2]
                        + ref[offRef + 1] * coeff[3] + ref[offRef + 2] * coeff[4] + ref[offRef + 3] * coeff[5] + 64) >> 7) + 128;
            }
            offRef += lfRef;
            offTgt += lfTgt;
        }
    }

    private void predictFullXSubYUnSafe(byte[] ref, int rx, int ry, int iy, int refW, int refH, int refVertStep,
            int refVertOff, int[] tgt, int tgtY, int tgtW, int tgtH, int tgtVertStep) {
        int[] coeff = COEFF[iy];

        int tgtOff = tgtW * tgtY;
        int lfTgt = tgtVertStep * tgtW;

        for (int i = 0; i < tgtH; i++) {
            int y = ((i + ry) << refVertStep) + refVertOff;
            for (int j = 0; j < tgtW; j++) {
                tgt[tgtOff++] = (getPix6Vert(ref, refW, refH, j + rx, y, refVertStep, refVertOff, coeff) + 64) >> 7;
            }
            tgtOff += lfTgt;
        }
    }

    private void predictFullXSubYSafe(byte[] ref, int rx, int ry, int iy, int refW, int refH, int refVertStep,
            int refVertOff, int[] tgt, int tgtY, int tgtW, int tgtH, int tgtVertStep) {
        int[] coeff = COEFF[iy];

        int offTgt = tgtW * tgtY;
        int offRef = ((ry << refVertStep) + refVertOff) * refW + rx;

        int singleRefW = refW << refVertStep;
        int dblRefW = refW << (1 + refVertStep);
        int tripleRefW = dblRefW + singleRefW;

        int lfTgt = tgtVertStep * tgtW;
        int lfRef = (refW << refVertStep) - tgtW;

        for (int i = 0; i < tgtH; i++) {
            for (int j = 0; j < tgtW; ++j, ++offTgt, ++offRef) {
                tgt[offTgt] = ((ref[offRef - dblRefW] * coeff[0] + ref[offRef - singleRefW] * coeff[1] + ref[offRef]
                        * coeff[2] + ref[offRef + singleRefW] * coeff[3] + ref[offRef + dblRefW] * coeff[4]
                        + ref[offRef + tripleRefW] * coeff[5] + 64) >> 7) + 128;
            }
            offRef += lfRef;
            offTgt += lfTgt;
        }
    }
}