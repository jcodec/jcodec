package org.jcodec.codecs.mpeg12;

import static org.jcodec.codecs.mpeg12.MPEGConst.vlcMotionCode;
import static org.jcodec.codecs.mpeg12.bitstream.SequenceExtension.Chroma420;
import static org.jcodec.codecs.mpeg12.bitstream.SequenceExtension.Chroma444;

import java.io.IOException;

import org.jcodec.common.io.InBits;
import org.jcodec.common.model.Picture;
import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MPEGPred {
    private int[][][] mvPred = new int[2][2][2];
    private int chromaFormat;
    private int[][] fCode;

    public MPEGPred(int[][] fCode, int chromaFormat) {
        this.fCode = fCode;
        this.chromaFormat = chromaFormat;
    }

    public final void predictEvenEvenSafe(int[] ref, int refX, int refY, int refW, int refH, int refVertStep,
            int[] tgt, int tgtY, int tgtW, int tgtH, int tgtVertStep) {
        int offRef = refY * refW + refX, offTgt = tgtW * tgtY, lfRef = (refW << refVertStep) - tgtW, lfTgt = tgtVertStep
                * tgtW;

        for (int i = 0; i < tgtH; i++) {
            for (int j = 0; j < tgtW; j++)
                tgt[offTgt++] = ref[offRef++];
            offRef += lfRef;
            offTgt += lfTgt;
        }
    }

    public final void predictEvenOddSafe(int[] ref, int refX, int refY, int refW, int refH, int refVertStep, int[] tgt,
            int tgtY, int tgtW, int tgtH, int tgtVertStep) {
        int offRef = refY * refW + refX, offTgt = tgtW * tgtY, lfRef = (refW << refVertStep) - tgtW, lfTgt = tgtVertStep
                * tgtW;

        for (int i = 0; i < tgtH; i++) {
            for (int j = 0; j < tgtW; j++)
                tgt[offTgt++] = (ref[offRef++] + ref[offRef]) >> 1;
            offRef += lfRef;
            offTgt += lfTgt;
        }
    }

    public final void predictOddEvenSafe(int[] ref, int refX, int refY, int refW, int refH, int refVertStep, int[] tgt,
            int tgtY, int tgtW, int tgtH, int tgtVertStep) {
        int offRef = refY * refW + refX, offTgt = tgtW * tgtY, lfRef = (refW << refVertStep) - tgtW, lfTgt = tgtVertStep
                * tgtW;

        for (int i = 0; i < tgtH; i++) {
            for (int j = 0; j < tgtW; j++) {
                tgt[offTgt++] = (ref[offRef] + ref[offRef + refW]) >> 1;
                ++offRef;
            }
            offRef += lfRef;
            offTgt += lfTgt;
        }
    }

    public final void predictOddOddSafe(int[] ref, int refX, int refY, int refW, int refH, int refVertStep, int[] tgt,
            int tgtY, int tgtW, int tgtH, int tgtVertStep) {
        int offRef = refY * refW + refX, offTgt = tgtW * tgtY, lfRef = (refW << refVertStep) - tgtW, lfTgt = tgtVertStep
                * tgtW;

        for (int i = 0; i < tgtH; i++) {
            for (int j = 0; j < tgtW; j++)
                tgt[offTgt++] = (ref[offRef++] + ref[offRef + refW]) >> 1;
            offRef += lfRef;
            offTgt += lfTgt;
        }
    }

    private final int getPix(int[] ref, int refW, int refH, int x1, int y1, int x2, int y2) {
        x1 = MathUtil.clip(x1, 0, refW - 1);
        y1 = MathUtil.clip(y1, 0, refH - 1);
        x2 = MathUtil.clip(x2, 0, refW - 1);
        y2 = MathUtil.clip(y2, 0, refH - 1);

        return (ref[y1 * refW + x1] + ref[y2 * refW + x2]) >> 1;
    }

    public final void predictEvenEvenUnSafe(int[] ref, int refX, int refY, int refW, int refH, int refVertStep,
            int[] tgt, int tgtY, int tgtW, int tgtH, int tgtVertStep) {
        int tgtOff = tgtW * tgtY, jump = tgtVertStep * tgtW;
        for (int j = 0; j < tgtH; j += refVertStep + 1) {
            for (int i = 0; i < tgtW; i++) {
                tgt[tgtOff++] = getPix(ref, refW, refH, i + refX, j + refY, i + refX, j + refY);
            }
            tgtOff += jump;
        }
    }

    public final void predictEvenOddUnSafe(int[] ref, int refX, int refY, int refW, int refH, int refVertStep,
            int[] tgt, int tgtY, int tgtW, int tgtH, int tgtVertStep) {
        int tgtOff = tgtW * tgtY, jump = tgtVertStep * tgtW;
        for (int j = 0; j < tgtH; j += refVertStep + 1) {
            for (int i = 0; i < tgtW; i++) {
                tgt[tgtOff++] = getPix(ref, refW, refH, i + refX, j + refY, i + refX + 1, j + refY);
            }
            tgtOff += jump;
        }
    }

    public final void predictOddEvenUnSafe(int[] ref, int refX, int refY, int refW, int refH, int refVertStep,
            int[] tgt, int tgtY, int tgtW, int tgtH, int tgtVertStep) {
        int tgtOff = tgtW * tgtY, jump = tgtVertStep * tgtW;
        for (int j = 0; j < tgtH; j += refVertStep + 1) {
            for (int i = 0; i < tgtW; i++) {
                tgt[tgtOff++] = getPix(ref, refW, refH, i + refX, j + refY, i + refX, j + refY + refVertStep + 1);
            }
            tgtOff += jump;
        }
    }

    public final void predictOddOddUnSafe(int[] ref, int refX, int refY, int refW, int refH, int refVertStep,
            int[] tgt, int tgtY, int tgtW, int tgtH, int tgtVertStep) {
        int tgtOff = tgtW * tgtY, jump = tgtVertStep * tgtW;
        for (int j = 0; j < tgtH; j += refVertStep + 1) {
            for (int i = 0; i < tgtW; i++) {
                tgt[tgtOff++] = getPix(ref, refW, refH, i + refX, j + refY, i + refX + 1, j + refY + refVertStep + 1);
            }
            tgtOff += jump;
        }
    }

    public final void predictPlane(int[] ref, int refX, int refY, int refW, int refH, int refVertStep, int[] tgt,
            int tgtY, int tgtW, int tgtH, int tgtVertStep) {
        int rx = refX >> 1, ry = refY >> 1;

        boolean safe = rx >= 0 && ry >= 0 && rx + tgtW < refW && ry + tgtH < refH;
        if ((refX & 0x1) == 0) {
            if ((refY & 0x1) == 0) {
                if (safe)
                    predictEvenEvenSafe(ref, rx, ry, refW, refH, refVertStep, tgt, tgtY, tgtW, tgtH, tgtVertStep);
                else
                    predictEvenEvenUnSafe(ref, rx, ry, refW, refH, refVertStep, tgt, tgtY, tgtW, tgtH, tgtVertStep);
            } else {
                if (safe)
                    predictOddEvenSafe(ref, rx, ry, refW, refH, refVertStep, tgt, tgtY, tgtW, tgtH, tgtVertStep);
                else
                    predictOddEvenUnSafe(ref, rx, ry, refW, refH, refVertStep, tgt, tgtY, tgtW, tgtH, tgtVertStep);
            }
        } else if ((refY & 0x1) == 0) {
            if (safe)
                predictEvenOddSafe(ref, rx, ry, refW, refH, refVertStep, tgt, tgtY, tgtW, tgtH, tgtVertStep);
            else
                predictEvenOddUnSafe(ref, rx, ry, refW, refH, refVertStep, tgt, tgtY, tgtW, tgtH, tgtVertStep);
        } else {
            if (safe)
                predictOddOddSafe(ref, rx, ry, refW, refH, refVertStep, tgt, tgtY, tgtW, tgtH, tgtVertStep);
            else
                predictOddOddUnSafe(ref, rx, ry, refW, refH, refVertStep, tgt, tgtY, tgtW, tgtH, tgtVertStep);
        }
    }

    public void predictInField(Picture[] reference, int x, int y, int[][] mbPix, InBits in, int motionType,
            int backward, int topField) throws IOException {
        switch (motionType) {
        case 1:
            predict16x16Field(reference, x, y, in, backward, mbPix);
            break;
        case 2:
            predict16x8MC(reference, x, y, in, backward, mbPix, 0, 0);
            predict16x8MC(reference, x, y, in, backward, mbPix, 8, 1);
            break;
        case 3:
            predict16x16DualPrimeField(reference, x, y, in, mbPix, topField);
        }
    }

    public void predictInFrame(Picture reference, int x, int y, int[][] mbPix, InBits in, int motionType, int backward,
            int spatial_temporal_weight_code) throws IOException {
        Picture[] refs = new Picture[] { reference, reference };
        switch (motionType) {
        case 1:
            predictFieldInFrame(reference, x, y, mbPix, in, backward, spatial_temporal_weight_code);
            break;
        case 2:
            predict16x16Frame(reference, x, y, in, backward, mbPix);
            break;
        case 3:
            predict16x16DualPrimeFrame(refs, x, y, in, backward, mbPix);
            break;
        }
    }

    private void predict16x16DualPrimeField(Picture[] reference, int x, int y, InBits in, int[][] mbPix, int topField)
            throws IOException {
        int codeX = vlcMotionCode.readVLC(in) - 16, residialX = 0;
        if (fCode[0][0] != 1 && codeX != 0)
            residialX = in.readNBit(fCode[0][0] - 1);
        int dmX = MPEGConst.vlcDualPrime.readVLC(in);

        int codeY = vlcMotionCode.readVLC(in) - 16, residialY = 0;
        if (fCode[0][1] != 1 && codeY != 0)
            residialY = in.readNBit(fCode[0][1] - 1);
        int dmY = MPEGConst.vlcDualPrime.readVLC(in);

        int vect1X = mvPred[0][0][0] + (1 << fCode[0][0]) * codeX + residialX;
        int vect1Y = mvPred[0][0][1] + (1 << fCode[0][1]) * codeY + residialY;

        int vect2X = dpXField(vect1X, dmX, topField);
        int vect2Y = dpYField(vect1Y, dmY, topField);

        int cw = chromaFormat == Chroma420 ? 1 : 0;
        int ch = chromaFormat == Chroma444 ? 0 : 1;

        int[][] mbPix1 = new int[3][256], mbPix2 = new int[3][256];

        predictPlane(reference[topField].getPlaneData(0), x + vect1X, ((y + vect1Y) << 1) + (1 - topField),
                reference[topField].getPlaneWidth(0), reference[topField].getPlaneHeight(0), 1, mbPix1[0], 0, 16, 16, 0);
        predictPlane(reference[topField].getPlaneData(1), (x + vect1X) >> cw, (((y + vect1Y) >> ch) << 1)
                + (1 - topField), reference[topField].getPlaneWidth(1), reference[topField].getPlaneHeight(1), 1,
                mbPix1[1], 0, 16 >> cw, 16 >> ch, 0);
        predictPlane(reference[topField].getPlaneData(2), (x + vect1X) >> cw, (((y + vect1Y) >> ch) << 1)
                + (1 - topField), reference[topField].getPlaneWidth(2), reference[topField].getPlaneHeight(2), 1,
                mbPix1[2], 0, 16 >> cw, 16 >> ch, 0);

        predictPlane(reference[1 - topField].getPlaneData(0), x + vect2X, ((y + vect2Y) << 1) + topField,
                reference[1 - topField].getPlaneWidth(0), reference[1 - topField].getPlaneHeight(0), 1, mbPix2[0], 0,
                16, 16, 0);
        predictPlane(reference[1 - topField].getPlaneData(1), (x + vect2X) >> cw, (((y + vect2Y) >> ch) << 1)
                + topField, reference[1 - topField].getPlaneWidth(1), reference[1 - topField].getPlaneHeight(1), 1,
                mbPix2[1], 0, 16 >> cw, 16 >> ch, 0);
        predictPlane(reference[1 - topField].getPlaneData(2), (x + vect2X) >> cw, (((y + vect2Y) >> ch) << 1)
                + topField, reference[1 - topField].getPlaneWidth(2), reference[1 - topField].getPlaneHeight(2), 1,
                mbPix2[2], 0, 16 >> cw, 16 >> ch, 0);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 256; j++)
                mbPix[i][j] = (mbPix1[i][j] + mbPix2[i][j]) >> 1;
        }

        mvPred[1][0][0] = mvPred[0][0][0] = vect1X;
        mvPred[1][0][1] = mvPred[0][0][1] = vect1Y;
    }

    private final int dpYField(int vect1y, int dmY, int topField) {
        return ((vect1y + 1) >> 1) + ((topField << 1) - 1) + dmY;
    }

    private final int dpXField(int vect1x, int dmX, int topField) {
        return ((vect1x + 1) >> 1) + dmX;
    }

    private final int dpYFrame(int vect1y, int dmY, int topField, int topFieldFirst) {
        int dist = 3 - (Math.abs(topFieldFirst - topField) << 1);
        return ((vect1y * dist + 1) >> 1) + ((topField << 1) - 1) + dmY;
    }

    private final int dpXFrame(int vect1x, int dmX, int topField, int topFieldFirst) {
        int dist = 3 - (Math.abs(topFieldFirst - topField) << 1);
        return ((vect1x * dist + 1) >> 1) + dmX;
    }

    private void predict16x8MC(Picture[] reference, int x, int y, InBits in, int backward, int[][] mbPix, int vertPos,
            int vectIdx) throws IOException {
        int field = in.read1Bit();

        predictGeneric(reference[field], x, ((y + vertPos) << 1) + field, in, backward, mbPix, 0, 16, 8, 1, 0, vectIdx);
    }

    private void predict16x16Field(Picture[] reference, int x, int y, InBits in, int backward, int[][] mbPix)
            throws IOException {
        int field = in.read1Bit();

        predictGeneric(reference[field], x, (y << 1) + field, in, backward, mbPix, 0, 16, 16, 1, 0, 0);

        mvPred[1][backward][0] = mvPred[0][backward][0];
        mvPred[1][backward][1] = mvPred[0][backward][1];
    }

    private void predict16x16DualPrimeFrame(Picture[] refs, int x, int y, InBits in, int backward, int[][] mbPix) {
        throw new RuntimeException();
    }

    private void predict16x16Frame(Picture reference, int x, int y, InBits in, int backward, int[][] mbPix)
            throws IOException {
        predictGeneric(reference, x, y, in, backward, mbPix, 0, 16, 16, 0, 0, 0);

        mvPred[1][backward][0] = mvPred[0][backward][0];
        mvPred[1][backward][1] = mvPred[0][backward][1];
    }

    private final int mvectDecode(InBits in, int fcode, int pred) throws IOException {
        int code = vlcMotionCode.readVLC(in);
        if (code == 0) {
            return pred;
        }
        if (code < 0) {
            return 0xffff;
        }

        int sign, val, shift;
        sign = in.read1Bit();
        shift = fcode - 1;
        val = code;
        if (shift > 0) {
            val = (val - 1) << shift;
            val |= in.readNBit(shift);
            val++;
        }
        if (sign != 0)
            val = -val;
        val += pred;

        return sign_extend(val, 5 + shift);
    }

    private final int sign_extend(int val, int bits) {
        int shift = 32 - bits;
        return (val << shift) >> shift;
    }

    private void predictGeneric(Picture reference, int x, int y, InBits in, int backward, int[][] mbPix, int tgtY,
            int blkW, int blkH, int isSrcField, int isDstField, int vectIdx) throws IOException {
        int vectX = mvectDecode(in, fCode[backward][0], mvPred[0][backward][0]);
        int vectY = mvectDecode(in, fCode[backward][1], mvPred[0][backward][1]);

//        System.out.println("MV: (" + vectX + "," + vectY + ")");

        predictMB(reference, (x << 1) + vectX, (y << 1) + vectY, blkW, blkH, isSrcField, mbPix, tgtY, isDstField);

        mvPred[vectIdx][backward][0] = vectX;
        mvPred[vectIdx][backward][1] = vectY;
    }

    private void predictFieldInFrame(Picture reference, int x, int y, int[][] mbPix, InBits in, int backward,
            int spatial_temporal_weight_code) throws IOException {
        int field = in.read1Bit();
        predictGeneric(reference, x, (y << 1) + field, in, backward, mbPix, 0, 16, 8, 1, 1, 0);
        if (spatial_temporal_weight_code == 0 || spatial_temporal_weight_code == 1) {
            field = in.read1Bit();
            predictGeneric(reference, x, (y << 1) + field, in, backward, mbPix, 1, 16, 8, 1, 1, 1);
        } else {
            mvPred[1][backward][0] = mvPred[0][backward][0];
            mvPred[1][backward][1] = mvPred[0][backward][1];
            predictMB(reference, mvPred[1][backward][0], (mvPred[1][backward][1] << 1) + (1 - field), 16, 8, 1, mbPix,
                    1, 1);
        }
    }

    public void predictMB(Picture ref, int refX, int refY, int blkW, int blkH, int refVertStep, int[][] tgt, int tgtY,
            int tgtVertStep) {
        int cw = chromaFormat == Chroma420 ? 1 : 0;
        int ch = chromaFormat == Chroma444 ? 0 : 1;

        predictPlane(ref.getPlaneData(0), refX, refY, ref.getPlaneWidth(0), ref.getPlaneHeight(0), refVertStep, tgt[0],
                tgtY, blkW, blkH, tgtVertStep);
        predictPlane(ref.getPlaneData(1), refX >> cw, refY >> ch, ref.getPlaneWidth(1), ref.getPlaneHeight(1),
                refVertStep, tgt[1], tgtY, blkW >> cw, blkH >> ch, tgtVertStep);
        predictPlane(ref.getPlaneData(2), refX >> cw, refY >> ch, ref.getPlaneWidth(2), ref.getPlaneHeight(2),
                refVertStep, tgt[2], tgtY, blkW >> cw, blkH >> ch, tgtVertStep);
    }

    public void predict16x16NoMV(Picture picture, int x, int y, int pictureStructure, int backward, int[][] mbPix) {
        int predX = (x << 1) + mvPred[0][backward][0];
        int predY = (y << 1) + mvPred[0][backward][1];
        if (pictureStructure == 3) {
            predictMB(picture, predX, predY, 16, 16, 0, mbPix, 0, 0);
        } else
            predictMB(picture, predX, (predY << 1) + pictureStructure - 1, 16, 16, 1, mbPix, 0, 0);
    }

    public void reset() {
        mvPred[0][0][0] = mvPred[0][0][1] = mvPred[0][1][0] = mvPred[0][1][1] = mvPred[1][0][0] = mvPred[1][0][1] = mvPred[1][1][0] = mvPred[1][1][1] = 0;
    }
}