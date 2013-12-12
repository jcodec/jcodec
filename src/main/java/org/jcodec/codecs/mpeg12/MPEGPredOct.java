package org.jcodec.codecs.mpeg12;

import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * MPEG 1/2 decoder interframe motion compensation routines, octal interpolation
 * 
 * TODO: implement 6-tap interpolation for half-pixel positions
 * 
 * @author The JCodec project
 * 
 */
public class MPEGPredOct extends MPEGPred {
    public MPEGPredOct(MPEGPred other) {
        super(other);
    }

    @Override
    public void predictPlane(int[] ref, int refX, int refY, int refW, int refH, int refVertStep, int refVertOff,
            int[] tgt, int tgtY, int tgtW, int tgtH, int tgtVertStep) {
        int rx = refX >> 3, ry = refY >> 3;

        boolean safe = rx >= 0 && ry >= 0 && rx + tgtW < refW && (ry + tgtH << refVertStep) < refH;
        if ((refX & 0x7) == 0) {
            if ((refY & 0x7) == 0) {
                if (safe)
                    predictEvenEvenSafe(ref, rx, ry, refW, refH, refVertStep, refVertOff, tgt, tgtY, tgtW >> 2,
                            tgtH >> 2, tgtVertStep);
                else
                    predictEvenEvenUnSafe(ref, rx, ry, refW, refH, refVertStep, refVertOff, tgt, tgtY, tgtW >> 2,
                            tgtH >> 2, tgtVertStep);
            } else {
                if (safe)
                    predictOddEvenSafe(ref, rx, ry, refY - (ry << 3), refW, refH, refVertStep, refVertOff, tgt,
                            tgtY, tgtW >> 2, tgtH >> 2, tgtVertStep);
                else
                    predictOddEvenUnSafe(ref, rx, ry, refY - (ry << 3), refW, refH, refVertStep, refVertOff, tgt,
                            tgtY, tgtW >> 2, tgtH >> 2, tgtVertStep);
            }
        } else if ((refY & 0x7) == 0) {
            if (safe)
                predictEvenOddSafe(ref, rx, refX - (rx << 3), ry, refW, refH, refVertStep, refVertOff, tgt, tgtY,
                        tgtW >> 2, tgtH >> 2, tgtVertStep);
            else
                predictEvenOddUnSafe(ref, rx, refX - (rx << 3), ry, refW, refH, refVertStep, refVertOff, tgt, tgtY,
                        tgtW >> 2, tgtH >> 2, tgtVertStep);
        } else {
            if (safe)
                predictOddOddSafe(ref, rx, refX - (rx << 3), ry, refY - (ry << 3), refW, refH, refVertStep,
                        refVertOff, tgt, tgtY, tgtW >> 2, tgtH >> 2, tgtVertStep);
            else
                predictOddOddUnSafe(ref, rx, refX - (rx << 3), ry, refY - (ry << 3), refW, refH, refVertStep,
                        refVertOff, tgt, tgtY, tgtW >> 2, tgtH >> 2, tgtVertStep);
        }
    }

    private void predictOddOddUnSafe(int[] ref, int rx, int ix, int ry, int iy, int refW, int refH,
            int refVertStep, int refVertOff, int[] tgt, int tgtY, int tgtW, int tgtH, int tgtVertStep) {
        int tgtOff = tgtW * tgtY, jump = tgtVertStep * tgtW;
        for (int j = 0; j < tgtH; j++) {
            int y1 = ((j + ry) << refVertStep) + refVertOff;
            int y2 = ((j + ry + 1) << refVertStep) + refVertOff;
            for (int i = 0; i < tgtW; i++) {
                int ptX = i + rx;
                tgt[tgtOff++] = getPix4(ref, refW, refH, ptX, y1, ptX + 1, y1, ptX, y2, ptX + 1, y2, refVertStep,
                        refVertOff, ix, iy);
            }
            tgtOff += jump;
        }
    }

    protected int getPix4(int[] ref, int refW, int refH, int x1, int y1, int x2, int y2, int x3, int y3, int x4,
            int y4, int refVertStep, int refVertOff, int ix, int iy) {
        int lastLine = refH - (1 << refVertStep) + refVertOff;
        x1 = MathUtil.clip(x1, 0, refW - 1);
        y1 = MathUtil.clip(y1, 0, lastLine);
        x2 = MathUtil.clip(x2, 0, refW - 1);
        y2 = MathUtil.clip(y2, 0, lastLine);
        x3 = MathUtil.clip(x3, 0, refW - 1);
        y3 = MathUtil.clip(y3, 0, lastLine);
        x4 = MathUtil.clip(x4, 0, refW - 1);
        y4 = MathUtil.clip(y4, 0, lastLine);

        int nix = 8 - ix, niy = 8 - iy;

        return (ref[y1 * refW + x1] * nix * niy + ref[y2 * refW + x2] * ix * niy + ref[y3 * refW + x3] * nix * iy
                + ref[y4 * refW + x4] * ix * iy + 32) >> 6;
    }

    private void predictOddOddSafe(int[] ref, int rx, int ix, int ry, int iy, int refW, int refH, int refVertStep,
            int refVertOff, int[] tgt, int tgtY, int tgtW, int tgtH, int tgtVertStep) {
        int offRef = ((ry << refVertStep) + refVertOff) * refW + rx, offTgt = tgtW * tgtY, lfRef = (refW << refVertStep)
                - tgtW, lfTgt = tgtVertStep * tgtW, stride = refW << refVertStep;

        int nix = 8 - ix, niy = 8 - iy;
        for (int i = 0; i < tgtH; i++) {
            for (int j = 0; j < tgtW; j++) {
                tgt[offTgt++] = (ref[offRef] * nix * niy + ref[offRef + 1] * ix * niy + ref[offRef + stride] * nix
                        * iy + ref[offRef + stride + 1] * ix * iy + 32) >> 6;
                ++offRef;
            }
            offRef += lfRef;
            offTgt += lfTgt;
        }
    }

    protected int getPix2(int[] ref, int refW, int refH, int x1, int y1, int x2, int y2, int refVertStep,
            int refVertOff, int i) {
        int ni = 8 - i;

        x1 = MathUtil.clip(x1, 0, refW - 1);
        int lastLine = refH - (1 << refVertStep) + refVertOff;
        y1 = MathUtil.clip(y1, 0, lastLine);
        x2 = MathUtil.clip(x2, 0, refW - 1);
        y2 = MathUtil.clip(y2, 0, lastLine);

        return (ref[y1 * refW + x1] * ni + ref[y2 * refW + x2] * i + 4) >> 3;
    }

    private void predictEvenOddUnSafe(int[] ref, int rx, int ix, int ry, int refW, int refH, int refVertStep,
            int refVertOff, int[] tgt, int tgtY, int tgtW, int tgtH, int tgtVertStep) {
        int tgtOff = tgtW * tgtY, jump = tgtVertStep * tgtW;
        for (int j = 0; j < tgtH; j++) {
            int y = ((j + ry) << refVertStep) + refVertOff;
            for (int i = 0; i < tgtW; i++) {
                tgt[tgtOff++] = getPix2(ref, refW, refH, i + rx, y, i + rx + 1, y, refVertStep, refVertOff, ix);
            }
            tgtOff += jump;
        }
    }

    private void predictEvenOddSafe(int[] ref, int rx, int ix, int ry, int refW, int refH, int refVertStep,
            int refVertOff, int[] tgt, int tgtY, int tgtW, int tgtH, int tgtVertStep) {
        int offRef = ((ry << refVertStep) + refVertOff) * refW + rx, offTgt = tgtW * tgtY, lfRef = (refW << refVertStep)
                - tgtW, lfTgt = tgtVertStep * tgtW;

        int nix = 8 - ix;
        for (int i = 0; i < tgtH; i++) {
            for (int j = 0; j < tgtW; j++) {
                tgt[offTgt++] = (ref[offRef] * nix + ref[offRef + 1] * ix + 4) >> 3;
                ++offRef;
            }
            offRef += lfRef;
            offTgt += lfTgt;
        }
    }

    private void predictOddEvenUnSafe(int[] ref, int rx, int ry, int iy, int refW, int refH, int refVertStep,
            int refVertOff, int[] tgt, int tgtY, int tgtW, int tgtH, int tgtVertStep) {
        int tgtOff = tgtW * tgtY, jump = tgtVertStep * tgtW;
        for (int j = 0; j < tgtH; j++) {
            int y1 = ((j + ry) << refVertStep) + refVertOff;
            int y2 = ((j + ry + 1) << refVertStep) + refVertOff;
            for (int i = 0; i < tgtW; i++) {
                tgt[tgtOff++] = getPix2(ref, refW, refH, i + rx, y1, i + rx, y2, refVertStep, refVertOff, iy);
            }
            tgtOff += jump;
        }
    }

    private void predictOddEvenSafe(int[] ref, int rx, int ry, int iy, int refW, int refH, int refVertStep,
            int refVertOff, int[] tgt, int tgtY, int tgtW, int tgtH, int tgtVertStep) {
        int offRef = ((ry << refVertStep) + refVertOff) * refW + rx, offTgt = tgtW * tgtY, lfRef = (refW << refVertStep)
                - tgtW, lfTgt = tgtVertStep * tgtW, stride = refW << refVertStep;

        int niy = 8 - iy;
        for (int i = 0; i < tgtH; i++) {
            for (int j = 0; j < tgtW; j++) {
                tgt[offTgt++] = (ref[offRef] * niy + ref[offRef + stride] * iy + 4) >> 3;
                ++offRef;
            }
            offRef += lfRef;
            offTgt += lfTgt;
        }
    }
}