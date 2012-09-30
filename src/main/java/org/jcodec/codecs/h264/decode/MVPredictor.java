package org.jcodec.codecs.h264.decode;

import static org.jcodec.codecs.h264.io.model.SubMBType.L0_4x4;
import static org.jcodec.codecs.h264.io.model.SubMBType.L0_4x8;
import static org.jcodec.codecs.h264.io.model.SubMBType.L0_8x4;

import org.jcodec.codecs.h264.decode.model.NearbyMotionVectors;
import org.jcodec.codecs.h264.io.model.Inter8x8Prediction;
import org.jcodec.codecs.h264.io.model.InterPrediction;
import org.jcodec.codecs.h264.io.model.SubMBType;
import org.jcodec.codecs.h264.io.model.Vector;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Contains the routines for motion vector prediction
 * 
 * @author Jay Codec
 * 
 */
public class MVPredictor {
    public Vector[] predictMotionVectors16x16(NearbyMotionVectors nearMV, InterPrediction pred) {
        Vector[] mvD = pred.getDecodedMVsL0();

        Vector left = nearMV.getA() != null ? nearMV.getA()[0] : null;
        Vector top = nearMV.getB() != null ? nearMV.getB()[0] : null;

        Vector mv = calcMVPredictionMedian(mvD[0], left, top, nearMV.getC(), nearMV.getD(), nearMV.hasA(),
                nearMV.hasB(), nearMV.hasC(), nearMV.hasD());

        return new Vector[] { mv };
    }

    public Vector[] predictMotionVectors8x16(NearbyMotionVectors nearMV, InterPrediction pred) {
        Vector[] mvD = pred.getDecodedMVsL0();

        Vector left0 = nearMV.getA() != null ? nearMV.getA()[0] : null;
        Vector top0 = null;
        Vector top1 = null;
        Vector top2 = null;
        if (nearMV.getB() != null) {
            top0 = nearMV.getB()[0];
            top1 = nearMV.getB()[1];
            top2 = nearMV.getB()[2];
        }

        Vector mvLeft = calcMVPrediction8x16(mvD[0], left0, top0, top2, nearMV.getD(), nearMV.hasA(), nearMV.hasB(),
                nearMV.hasB(), nearMV.hasD(), 0);

        Vector mvRight = calcMVPrediction8x16(mvD[1], mvLeft, top2, nearMV.getC(), top1, true, nearMV.hasB(),
                nearMV.hasC(), nearMV.hasB(), 1);

        return new Vector[] { mvLeft, mvRight };
    }

    public Vector[] predictMotionVectors16x8(NearbyMotionVectors nearMV, InterPrediction pred1) {
        Vector[] mvD = pred1.getDecodedMVsL0();

        Vector left0 = null, left1 = null, left2 = null;
        if (nearMV.getA() != null) {
            left0 = nearMV.getA()[0];
            left1 = nearMV.getA()[1];
            left2 = nearMV.getA()[2];
        }
        Vector top0 = nearMV.getB() != null ? nearMV.getB()[0] : null;

        Vector mvTop = calcMVPrediction16x8(mvD[0], left0, top0, nearMV.getC(), nearMV.getD(), nearMV.hasA(),
                nearMV.hasB(), nearMV.hasC(), nearMV.hasD(), 0);

        Vector mvBottom = calcMVPrediction16x8(mvD[1], left2, mvTop, null, left1, nearMV.hasA(), true, false,
                nearMV.hasA(), 1);

        return new Vector[] { mvTop, mvBottom };
    }

    public Vector[][] predictMotionVectors8x8(NearbyMotionVectors nearMV, Inter8x8Prediction prediction) {
        Vector[] top;
        if (nearMV.getB() != null) {
            top = new Vector[] { nearMV.getB()[0], nearMV.getB()[1], nearMV.getB()[2], nearMV.getB()[3], null, null,
                    null, null };
        } else {
            top = new Vector[] { null, null, null, null, null, null, null, null };
        }

        Vector[] left;
        if (nearMV.getA() != null) {
            left = new Vector[] { nearMV.getA()[0], nearMV.getA()[1], nearMV.getA()[2], nearMV.getA()[3], null, null,
                    null, null };
        } else {
            left = new Vector[] { null, null, null, null, null, null, null, null };
        }

        Vector[][] mvP = new Vector[4][];

        mvP[0] = getMVPrediction8x8ForBlock0(prediction.getSubMbTypes()[0], prediction.getDecodedMVsL0()[0], nearMV,
                top, left);

        mvP[1] = getMVPrediction8x8ForBlock1(prediction.getSubMbTypes()[1], prediction.getDecodedMVsL0()[1], nearMV,
                top, left);

        mvP[2] = getMVPrediction8x8ForBlock2(prediction.getSubMbTypes()[2], prediction.getDecodedMVsL0()[2], nearMV,
                top, left);

        mvP[3] = getMVPrediction8x8ForBlock3(prediction.getSubMbTypes()[3], prediction.getDecodedMVsL0()[3], nearMV,
                top, left);
        return mvP;
    }

    public Vector[] predictMotionVectorsSkip(NearbyMotionVectors nearMV) {
        Vector[] ZERO = new Vector[] { new Vector(0, 0, 0) };
        if (nearMV.hasA() && nearMV.hasB()) {
            Vector v1 = nearMV.getA() != null ? nearMV.getA()[0] : null;
            Vector v2 = nearMV.getB() != null ? nearMV.getB()[0] : null;

            if ((v1 != null && v1.getX() == 0 && v1.getY() == 0 && v1.getRefId() == 0)
                    || (v2 != null && v2.getX() == 0 && v2.getY() == 0 && v2.getRefId() == 0)) {
                return ZERO;
            }

            return new Vector[] { calcMVPredictionMedian(ZERO[0], v1, v2, nearMV.getC(), nearMV.getD(), nearMV.hasA(),
                    nearMV.hasB(), nearMV.hasC(), nearMV.hasD()) };
        } else {
            return ZERO;
        }
    }

    private Vector[] getMVPrediction8x8ForBlock3(SubMBType subMbType, Vector[] mvD, NearbyMotionVectors nearMV,
            Vector[] top, Vector[] left) {

        if (subMbType == L0_4x4) {
            Vector mv1 = calcMVPredictionMedian(mvD[0], left[6], top[6], top[7], top[5], true, true, true, true);
            Vector mv2 = calcMVPredictionMedian(mvD[1], mv1, top[7], null, top[6], true, true, false, true);
            Vector mv3 = calcMVPredictionMedian(mvD[2], left[7], mv1, mv2, left[6], true, true, true, true);
            Vector mv4 = calcMVPredictionMedian(mvD[3], mv3, mv2, null, mv1, true, true, false, true);
            return new Vector[] { mv1, mv2, mv3, mv4 };
        } else if (subMbType == L0_4x8) {
            Vector mv1 = calcMVPredictionMedian(mvD[0], left[6], top[6], top[7], top[5], true, true, true, true);
            Vector mv2 = calcMVPredictionMedian(mvD[1], mv1, top[7], null, top[6], true, true, false, true);
            return new Vector[] { mv1, mv2 };
        } else if (subMbType == L0_8x4) {
            Vector mv1 = calcMVPredictionMedian(mvD[0], left[6], top[6], null, left[5], true, true, false, true);
            Vector mv2 = calcMVPredictionMedian(mvD[1], left[7], mv1, null, left[6], true, true, false, true);
            top[6] = top[7] = mv2;
            return new Vector[] { mv1, mv2 };
        } else { // L1_8x8
            Vector mv = calcMVPredictionMedian(mvD[0], left[6], top[6], null, left[5], true, true, false, true);
            return new Vector[] { mv };
        }
    }

    private Vector[] getMVPrediction8x8ForBlock2(SubMBType subMbType, Vector[] mvD, NearbyMotionVectors nearMV,
            Vector[] top, Vector[] left) {

        if (subMbType == L0_4x4) {
            Vector mv1 = calcMVPredictionMedian(mvD[0], left[2], top[4], top[5], left[1], nearMV.hasA(), true, true,
                    nearMV.hasA());
            Vector mv2 = calcMVPredictionMedian(mvD[1], mv1, top[5], top[6], top[4], true, true, true, true);
            Vector mv3 = calcMVPredictionMedian(mvD[2], left[3], mv1, mv2, left[2], nearMV.hasA(), true, true,
                    nearMV.hasA());
            Vector mv4 = calcMVPredictionMedian(mvD[3], mv3, mv2, null, mv1, true, true, false, true);
            left[6] = mv2;
            left[7] = mv4;

            return new Vector[] { mv1, mv2, mv3, mv4 };
        } else if (subMbType == L0_4x8) {
            Vector mv1 = calcMVPredictionMedian(mvD[0], left[2], top[4], top[5], left[1], nearMV.hasA(), true, true,
                    nearMV.hasA());
            Vector mv2 = calcMVPredictionMedian(mvD[1], mv1, top[5], top[6], top[4], true, true, true, true);

            left[6] = left[7] = mv2;

            return new Vector[] { mv1, mv2 };
        } else if (subMbType == L0_8x4) {
            Vector mv1 = calcMVPredictionMedian(mvD[0], left[2], top[4], top[6], left[1], nearMV.hasA(), true, true,
                    nearMV.hasA());
            Vector mv2 = calcMVPredictionMedian(mvD[1], left[3], mv1, null, left[2], nearMV.hasA(), true, false, true);

            left[6] = mv1;
            left[7] = mv2;

            return new Vector[] { mv1, mv2 };
        } else { // L1_8x8
            Vector mv = calcMVPredictionMedian(mvD[0], left[2], top[4], top[6], left[1], nearMV.hasA(), true, true,
                    nearMV.hasA());
            left[6] = left[7] = mv;
            return new Vector[] { mv };
        }
    }

    private Vector[] getMVPrediction8x8ForBlock1(SubMBType subMbType, Vector[] mvD, NearbyMotionVectors nearMV,
            Vector[] top, Vector[] left) {

        if (subMbType == L0_4x4) {
            Vector mv1 = calcMVPredictionMedian(mvD[0], left[4], top[2], top[3], top[1], true, nearMV.hasB(),
                    nearMV.hasB(), nearMV.hasB());
            Vector mv2 = calcMVPredictionMedian(mvD[1], mv1, top[3], nearMV.getC(), top[2], true, nearMV.hasB(),
                    nearMV.hasC(), nearMV.hasB());
            Vector mv3 = calcMVPredictionMedian(mvD[2], left[5], mv1, mv2, left[4], true, true, true, true);
            Vector mv4 = calcMVPredictionMedian(mvD[3], mv3, mv2, null, mv1, true, true, false, true);

            top[6] = mv3;
            top[7] = mv4;

            return new Vector[] { mv1, mv2, mv3, mv4 };
        } else if (subMbType == L0_4x8) {
            Vector mv1 = calcMVPredictionMedian(mvD[0], left[4], top[2], top[3], top[1], true, nearMV.hasB(),
                    nearMV.hasB(), nearMV.hasB());

            Vector mv2 = calcMVPredictionMedian(mvD[1], mv1, top[3], nearMV.getC(), top[2], true, nearMV.hasB(),
                    nearMV.hasC(), nearMV.hasB());

            top[6] = mv1;
            top[7] = mv2;

            return new Vector[] { mv1, mv2 };
        } else if (subMbType == L0_8x4) {

            Vector mv1 = calcMVPredictionMedian(mvD[0], left[4], top[2], nearMV.getC(), top[1], true, nearMV.hasB(),
                    nearMV.hasC(), nearMV.hasB());
            Vector mv2 = calcMVPredictionMedian(mvD[1], left[5], mv1, null, left[4], true, true, false, true);

            top[6] = top[7] = mv2;

            return new Vector[] { mv1, mv2 };
        } else { // L1_8x8\

            Vector mv = calcMVPredictionMedian(mvD[0], left[4], top[2], nearMV.getC(), top[1], true, nearMV.hasB(),
                    nearMV.hasC(), nearMV.hasB());

            top[6] = top[7] = mv;
            return new Vector[] { mv };
        }
    }

    private Vector[] getMVPrediction8x8ForBlock0(SubMBType subMbType, Vector[] mvD, NearbyMotionVectors nearMV,
            Vector[] top, Vector[] left) {

        if (subMbType == L0_4x4) {

            Vector mv1 = calcMVPredictionMedian(mvD[0], left[0], top[0], top[1], nearMV.getD(), nearMV.hasA(),
                    nearMV.hasB(), nearMV.hasB(), nearMV.hasD());
            Vector mv2 = calcMVPredictionMedian(mvD[1], mv1, top[1], top[2], top[0], true, nearMV.hasB(),
                    nearMV.hasB(), nearMV.hasB());
            Vector mv3 = calcMVPredictionMedian(mvD[2], left[1], mv1, mv2, left[0], nearMV.hasA(), true, true,
                    nearMV.hasA());
            Vector mv4 = calcMVPredictionMedian(mvD[3], mv3, mv2, null, mv1, true, true, false, true);
            left[4] = mv2;
            left[5] = mv4;
            top[4] = mv3;
            top[5] = mv4;

            return new Vector[] { mv1, mv2, mv3, mv4 };
        } else if (subMbType == L0_4x8) {
            Vector mv1 = calcMVPredictionMedian(mvD[0], left[0], top[0], top[1], nearMV.getD(), nearMV.hasA(),
                    nearMV.hasB(), nearMV.hasB(), nearMV.hasD());
            Vector mv2 = calcMVPredictionMedian(mvD[1], mv1, top[1], top[2], top[0], true, nearMV.hasB(),
                    nearMV.hasB(), nearMV.hasB());

            left[4] = left[5] = mv2;
            top[4] = mv1;
            top[5] = mv2;

            return new Vector[] { mv1, mv2 };
        } else if (subMbType == L0_8x4) {

            Vector mv1 = calcMVPredictionMedian(mvD[0], left[0], top[0], top[2], nearMV.getD(), nearMV.hasA(),
                    nearMV.hasB(), nearMV.hasB(), nearMV.hasD());
            Vector mv2 = calcMVPredictionMedian(mvD[1], left[1], mv1, null, left[0], nearMV.hasA(), true, false,
                    nearMV.hasA());

            left[4] = mv1;
            left[5] = mv2;
            top[4] = top[5] = mv2;

            return new Vector[] { mv1, mv2 };
        } else { // L1_8x8

            Vector mv = calcMVPredictionMedian(mvD[0], left[0], top[0], top[2], nearMV.getD(), nearMV.hasA(),
                    nearMV.hasB(), nearMV.hasB(), nearMV.hasD());

            left[4] = left[5] = mv;
            top[4] = top[5] = mv;
            return new Vector[] { mv };
        }
    }

    private Vector vectorAdd(Vector mvP, Vector mvD) {
        return new Vector(mvP.getX() + mvD.getX(), mvP.getY() + mvD.getY(), mvD.getRefId());
    }

    public Vector calcMVPredictionMedian(Vector mvD, Vector a, Vector b, Vector c, Vector d, boolean hasA,
            boolean hasB, boolean hasC, boolean hasD) {

        if (!hasC && hasD) {
            hasC = hasD;
            c = d;
        }

        if (a == null && b == null && c == null)
            return mvD;

        if (hasA && !hasB && !hasC)
            b = c = a;

        int refA = a != null ? a.getRefId() : -1;
        int refB = b != null ? b.getRefId() : -1;
        int refC = c != null ? c.getRefId() : -1;

        if (refA == mvD.getRefId() && refB != mvD.getRefId() && refC != mvD.getRefId())
            return vectorAdd(a, mvD);
        else if (refB == mvD.getRefId() && refA != mvD.getRefId() && refC != mvD.getRefId())
            return vectorAdd(b, mvD);
        else if (refC == mvD.getRefId() && refA != mvD.getRefId() && refB != mvD.getRefId())
            return vectorAdd(c, mvD);

        if (c == null)
            c = new Vector(0, 0, 0);
        if (a == null)
            a = new Vector(0, 0, 0);
        if (b == null)
            b = new Vector(0, 0, 0);

        int x = a.getX() + b.getX() + c.getX() - min(a.getX(), b.getX(), c.getX()) - max(a.getX(), b.getX(), c.getX())
                + mvD.getX();
        int y = a.getY() + b.getY() + c.getY() - min(a.getY(), b.getY(), c.getY()) - max(a.getY(), b.getY(), c.getY())
                + mvD.getY();

        return new Vector(x, y, mvD.getRefId());
    }

    private int max(int x, int x2, int x3) {
        return x > x2 ? (x > x3 ? x : x3) : (x2 > x3 ? x2 : x3);
    }

    private int min(int x, int x2, int x3) {
        return x < x2 ? (x < x3 ? x : x3) : (x2 < x3 ? x2 : x3);
    }

    public Vector calcMVPrediction16x8(Vector mvD, Vector a, Vector b, Vector c, Vector d, boolean hasA, boolean hasB,
            boolean hasC, boolean hasD, int idx) {

        if (idx == 0 && b != null && b.getRefId() == mvD.getRefId())
            return vectorAdd(b, mvD);
        else if (idx == 1 && a != null && a.getRefId() == mvD.getRefId())
            return vectorAdd(a, mvD);
        else
            return calcMVPredictionMedian(mvD, a, b, c, d, hasA, hasB, hasC, hasD);
    }

    public Vector calcMVPrediction8x16(Vector mvD, Vector a, Vector b, Vector c, Vector d, boolean hasA, boolean hasB,
            boolean hasC, boolean hasD, int idx) {
        Vector localC = hasC ? c : d;

        if (idx == 0 && a != null && a.getRefId() == mvD.getRefId())
            return vectorAdd(a, mvD);
        else if (idx == 1 && localC != null && localC.getRefId() == mvD.getRefId())
            return vectorAdd(localC, mvD);
        else
            return calcMVPredictionMedian(mvD, a, b, c, d, hasA, hasB, hasC, hasD);
    }
}
