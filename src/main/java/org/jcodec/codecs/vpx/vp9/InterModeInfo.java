package org.jcodec.codecs.vpx.vp9;

import static org.jcodec.codecs.vpx.vp9.Consts.ALTREF_FRAME;
import static org.jcodec.codecs.vpx.vp9.Consts.BLOCK_4X4;
import static org.jcodec.codecs.vpx.vp9.Consts.BLOCK_4X8;
import static org.jcodec.codecs.vpx.vp9.Consts.BLOCK_8X8;
import static org.jcodec.codecs.vpx.vp9.Consts.BOTH_INTRA;
import static org.jcodec.codecs.vpx.vp9.Consts.BOTH_NEW;
import static org.jcodec.codecs.vpx.vp9.Consts.BOTH_PREDICTED;
import static org.jcodec.codecs.vpx.vp9.Consts.BOTH_ZERO;
import static org.jcodec.codecs.vpx.vp9.Consts.CLASS0_SIZE;
import static org.jcodec.codecs.vpx.vp9.Consts.COMPOUND_REF;
import static org.jcodec.codecs.vpx.vp9.Consts.GOLDEN_FRAME;
import static org.jcodec.codecs.vpx.vp9.Consts.INTRA_FRAME;
import static org.jcodec.codecs.vpx.vp9.Consts.INTRA_PLUS_NON_INTRA;
import static org.jcodec.codecs.vpx.vp9.Consts.LAST_FRAME;
import static org.jcodec.codecs.vpx.vp9.Consts.MV_CLASS_TREE;
import static org.jcodec.codecs.vpx.vp9.Consts.MV_FR_TREE;
import static org.jcodec.codecs.vpx.vp9.Consts.MV_JOINT_HNZVNZ;
import static org.jcodec.codecs.vpx.vp9.Consts.MV_JOINT_HNZVZ;
import static org.jcodec.codecs.vpx.vp9.Consts.MV_JOINT_HZVNZ;
import static org.jcodec.codecs.vpx.vp9.Consts.NEARESTMV;
import static org.jcodec.codecs.vpx.vp9.Consts.NEARMV;
import static org.jcodec.codecs.vpx.vp9.Consts.NEWMV;
import static org.jcodec.codecs.vpx.vp9.Consts.NEW_PLUS_NON_INTRA;
import static org.jcodec.codecs.vpx.vp9.Consts.REFERENCE_MODE_SELECT;
import static org.jcodec.codecs.vpx.vp9.Consts.SEG_LVL_REF_FRAME;
import static org.jcodec.codecs.vpx.vp9.Consts.SEG_LVL_SKIP;
import static org.jcodec.codecs.vpx.vp9.Consts.SWITCHABLE;
import static org.jcodec.codecs.vpx.vp9.Consts.TREE_INTERP_FILTER;
import static org.jcodec.codecs.vpx.vp9.Consts.TREE_INTER_MODE;
import static org.jcodec.codecs.vpx.vp9.Consts.TREE_INTRA_MODE;
import static org.jcodec.codecs.vpx.vp9.Consts.TREE_MV_JOINT;
import static org.jcodec.codecs.vpx.vp9.Consts.ZEROMV;
import static org.jcodec.codecs.vpx.vp9.Consts.ZERO_PLUS_PREDICTED;
import static org.jcodec.codecs.vpx.vp9.Consts.blH;
import static org.jcodec.codecs.vpx.vp9.Consts.blW;
import static org.jcodec.codecs.vpx.vp9.Consts.mv_ref_blocks;
import static org.jcodec.codecs.vpx.vp9.Consts.mv_ref_blocks_sm;
import static org.jcodec.codecs.vpx.vp9.Consts.size_group_lookup;

import org.jcodec.codecs.common.biari.Packed4BitList;
import org.jcodec.codecs.vpx.VPXBooleanDecoder;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class InterModeInfo extends ModeInfo {

    private long mvl0;
    private long mvl1;
    private long mvl2;
    private long mvl3;

    public InterModeInfo(int segmentId, boolean skip, int txSize, int yMode, int subModes, int uvMode, long mvl0,
            long mvl1, long mvl2, long mvl3) {
        super(segmentId, skip, txSize, yMode, subModes, uvMode);
        this.mvl0 = mvl0;
        this.mvl1 = mvl1;
        this.mvl2 = mvl2;
        this.mvl3 = mvl3;
    }

    @Override
    public boolean isInter() {
        return true;
    }

    public long getMvl0() {
        return mvl0;
    }

    public long getMvl1() {
        return mvl1;
    }

    public long getMvl2() {
        return mvl2;
    }

    public long getMvl3() {
        return mvl3;
    }

    public static InterModeInfo readInter(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder, Probabilities probs, DecodingContext c) {
        int segmentId = 0;
        if (c.isSegmentationEnabled()) {
            segmentId = predicSegmentId(miCol, miRow, blSz, c);
            if (c.isUpdateSegmentMap()) {
                if (!c.isSegmentMapConditionalUpdate() || !readSegIdPredicted(miCol, miRow, blSz, decoder, probs, c)) {
                    segmentId = readSegmentId(decoder, probs);
                }
            }
        }
        boolean skip = true;
        if (!c.isSegmentFeatureActive(segmentId, SEG_LVL_SKIP))
            skip = readSkipFlag(miCol, miRow, blSz, decoder, probs, c);

        boolean isInter = c.getSegmentFeature(segmentId, SEG_LVL_REF_FRAME) != INTRA_FRAME;
        if (!c.isSegmentFeatureActive(segmentId, SEG_LVL_REF_FRAME))
            isInter = readIsInter(miCol, miRow, blSz, decoder, probs, c);

        int txSize = readTxSize(miCol, miRow, blSz, !skip || !isInter, decoder, probs, c);

        if (!isInter)
            return readIntraSpecificMode(miCol, miRow, blSz, decoder, probs, c, segmentId, skip, txSize);
        else
            return readInterSpecificMode(miCol, miRow, blSz, decoder, probs, c, segmentId, skip, txSize);
    }

    private static InterModeInfo readInterSpecificMode(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder,
            Probabilities probStore, DecodingContext c, int segmentId, boolean skip, int txSize) {

        int packedRefFrames = readRefFrames(miCol, miRow, blSz, segmentId, decoder, probStore, c);

        int lumaMode = ZEROMV;
        if (!c.isSegmentFeatureActive(segmentId, SEG_LVL_SKIP)) {
            if (blSz >= BLOCK_8X8) {
                lumaMode = readLumaMode(miCol, miRow, blSz, decoder, probStore, c);
            }
        }
        int interpFilter = c.getInterpFilter();
        if (interpFilter == SWITCHABLE) {
            interpFilter = readInterpFilter(miCol, miRow, blSz, decoder, probStore, c);
        }

        if (blSz < BLOCK_8X8) {
            if (blSz == BLOCK_4X4) {
                return readMV4x4(miCol, miRow, blSz, decoder, probStore, c, segmentId, skip, txSize, packedRefFrames);
            } else {
                return readMvSub8x8(miCol, miRow, blSz, decoder, probStore, c, segmentId, skip, txSize,
                        packedRefFrames);
            }
        } else {
            return readMV8x8(miCol, miRow, blSz, decoder, probStore, c, segmentId, skip, txSize, packedRefFrames,
                    lumaMode);
        }
    }

    private static InterModeInfo readMV8x8(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder,
            Probabilities probStore, DecodingContext c, int segmentId, boolean skip, int txSize, int packedRefFrames,
            int lumaMode) {

        long mvl = readSub0(miCol, miRow, blSz, decoder, probStore, c, lumaMode, packedRefFrames);

        updateMVLineBuffers(miCol, miRow, blSz, c, mvl);
        updateMVLineBuffers4x4(miCol, miRow, blSz, c, mvl, mvl);
        return new InterModeInfo(segmentId, skip, txSize, lumaMode, 0, lumaMode, mvl, 0, 0, 0);
    }

    private static InterModeInfo readMvSub8x8(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder,
            Probabilities probStore, DecodingContext c, int segmentId, boolean skip, int txSize, int packedRefFrames) {

        int subMode0 = readLumaMode(miCol, miRow, blSz, decoder, probStore, c);
        long mvl0 = readSub0(miCol, miRow, blSz, decoder, probStore, c, subMode0, packedRefFrames);

        int subMode1 = readLumaMode(miCol, miRow, blSz, decoder, probStore, c);
        int blk = blSz == BLOCK_4X8 ? 1 : 2;
        long mvl1 = readSub12(miCol, miRow, blSz, decoder, probStore, c, mvl0, subMode1, blk, packedRefFrames);

        if (blSz == BLOCK_4X8) {
            updateMVLineBuffers4x4(miCol, miRow, blSz, c, mvl1, mvl0);
        } else {
            updateMVLineBuffers4x4(miCol, miRow, blSz, c, mvl0, mvl1);
        }
        updateMVLineBuffers(miCol, miRow, blSz, c, mvl1);
        int subModes = (subMode0 << 8) | subMode1;
        return new InterModeInfo(segmentId, skip, txSize, 0, subModes, 0, mvl0, mvl1, 0, 0);
    }

    private static InterModeInfo readMV4x4(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder,
            Probabilities probStore, DecodingContext c, int segmentId, boolean skip, int txSize, int packedRefFrames) {

        int subMode0 = readLumaMode(miCol, miRow, blSz, decoder, probStore, c);
        long mvl0 = readSub0(miCol, miRow, blSz, decoder, probStore, c, subMode0, packedRefFrames);

        int subMode1 = readLumaMode(miCol, miRow, blSz, decoder, probStore, c);
        long mvl1 = readSub12(miCol, miRow, blSz, decoder, probStore, c, mvl0, subMode1, 1, packedRefFrames);

        int subMode2 = readLumaMode(miCol, miRow, blSz, decoder, probStore, c);
        long mvl2 = readSub12(miCol, miRow, blSz, decoder, probStore, c, mvl0, subMode2, 2, packedRefFrames);

        int subMode3 = readLumaMode(miCol, miRow, blSz, decoder, probStore, c);
        long mvl3 = readMvSub3(miCol, miRow, blSz, decoder, probStore, c, mvl0, mvl1, mvl2, subMode3, packedRefFrames);

        updateMVLineBuffers(miCol, miRow, blSz, c, mvl3);
        updateMVLineBuffers4x4(miCol, miRow, blSz, c, mvl1, mvl2);

        int subModes = (subMode0 << 24) | (subMode1 << 16) | (subMode2 << 8) | subMode3;
        return new InterModeInfo(segmentId, skip, txSize, -1, subModes, -1, mvl0, mvl1, mvl2, mvl3);
    }

    private static long readSub0(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder, Probabilities probStore,
            DecodingContext c, int lumaMode, int packedRefFrames) {
        int ref0 = Packed4BitList.get(packedRefFrames, 0);
        int ref1 = Packed4BitList.get(packedRefFrames, 1);
        boolean compoundPred = Packed4BitList.get(packedRefFrames, 2) == 1;

        long nearestNearMv00 = findBestMv(miCol, miRow, blSz, ref0, 0, c, true);
        long nearestNearMv01 = 0;
        if (compoundPred)
            nearestNearMv01 = findBestMv(miCol, miRow, blSz, ref1, 0, c, true);
        int mv0 = 0, mv1 = 0;
        if (lumaMode == NEWMV) {
            mv0 = readDiffMv(decoder, probStore, c, nearestNearMv00);
            if (compoundPred)
                mv1 = readDiffMv(decoder, probStore, c, nearestNearMv01);
        } else if (lumaMode != ZEROMV) {
            mv0 = MVList.get(nearestNearMv00, lumaMode - NEARESTMV);
            mv1 = MVList.get(nearestNearMv01, lumaMode - NEARESTMV);
        }

        return MVList.create(mv0, mv1);
    }

    private static long readSub12(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder, Probabilities probStore,
            DecodingContext c, long mvl0, int subMode1, int blk, int packedRefFrames) {
        int ref0 = Packed4BitList.get(packedRefFrames, 0);
        int ref1 = Packed4BitList.get(packedRefFrames, 1);
        boolean compoundPred = Packed4BitList.get(packedRefFrames, 2) == 1;
        int mv10 = 0, mv11 = 0;
        long nearestNearMv00 = findBestMv(miCol, miRow, blSz, ref0, 0, c, true);
        long nearestNearMv01 = 0;
        if (compoundPred)
            nearestNearMv01 = findBestMv(miCol, miRow, blSz, ref1, 0, c, true);
        if (subMode1 == NEWMV) {
            mv10 = readDiffMv(decoder, probStore, c, nearestNearMv00);
            if (compoundPred)
                mv11 = readDiffMv(decoder, probStore, c, nearestNearMv01);
        } else if (subMode1 != ZEROMV) {
            long nearestNearMv10 = prepandSubMvBlk12(findBestMv(miCol, miRow, blSz, ref0, blk, c, false),
                    MVList.get(mvl0, 0));
            long nearestNearMv11 = 0;
            if (compoundPred)
                nearestNearMv11 = prepandSubMvBlk12(findBestMv(miCol, miRow, blSz, ref1, blk, c, false),
                        MVList.get(mvl0, 1));
            mv10 = MVList.get(nearestNearMv10, subMode1 - NEARESTMV);
            mv11 = MVList.get(nearestNearMv11, subMode1 - NEARESTMV);
        }

        return MVList.create(mv10, mv11);
    }

    private static long readMvSub3(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder, Probabilities probStore,
            DecodingContext c, long mvl0, long mvl1, long mvl2, int subMode3, int packedRefFrames) {
        int ref0 = Packed4BitList.get(packedRefFrames, 0);
        int ref1 = Packed4BitList.get(packedRefFrames, 1);
        boolean compoundPred = Packed4BitList.get(packedRefFrames, 2) == 1;

        long nearestNearMv00 = findBestMv(miCol, miRow, blSz, ref0, 0, c, true);
        long nearestNearMv01 = 0;
        if (compoundPred)
            nearestNearMv01 = findBestMv(miCol, miRow, blSz, ref1, 0, c, true);
        int mv30 = 0, mv31 = 0;
        if (subMode3 == NEWMV) {
            mv30 = readDiffMv(decoder, probStore, c, nearestNearMv00);
            if (compoundPred)
                mv31 = readDiffMv(decoder, probStore, c, nearestNearMv01);
        } else if (subMode3 != ZEROMV) {
            long nearestNearMv30 = prepandSubMvBlk3(findBestMv(miCol, miRow, blSz, ref0, 3, c, false),
                    MVList.get(mvl0, 0), MVList.get(mvl1, 0), MVList.get(mvl2, 0));
            long nearestNearMv31 = 0;
            if (compoundPred)
                nearestNearMv31 = prepandSubMvBlk3(findBestMv(miCol, miRow, blSz, ref1, 3, c, false),
                        MVList.get(mvl0, 1), MVList.get(mvl1, 1), MVList.get(mvl2, 1));
            mv30 = MVList.get(nearestNearMv30, subMode3 - NEARESTMV);
            mv31 = MVList.get(nearestNearMv31, subMode3 - NEARESTMV);
        }

        return MVList.create(mv30, mv31);
    }

    private static int readRefFrames(int miCol, int miRow, int blSz, int segmentId, VPXBooleanDecoder decoder,
            Probabilities probStore, DecodingContext c) {
        int ref0 = c.getSegmentFeature(segmentId, SEG_LVL_REF_FRAME), ref1 = INTRA_FRAME;
        boolean compoundPred = false;
        if (!c.isSegmentFeatureActive(segmentId, SEG_LVL_REF_FRAME)) {
            int refMode = c.getRefMode();
            compoundPred = refMode == COMPOUND_REF;
            if (refMode == REFERENCE_MODE_SELECT)
                compoundPred = readCompMode(miCol, miRow, decoder, probStore, c);
            if (compoundPred) {
                int compRef = readCompRef(miCol, miRow, blSz, decoder, probStore, c);
                int fixedRef = c.getCompFixedRef();
                if (c.refFrameSignBias(fixedRef) == 0) {
                    ref0 = fixedRef;
                    ref1 = compRef;
                } else {
                    ref0 = compRef;
                    ref1 = fixedRef;
                }
            } else {
                ref0 = readSingleRef(miCol, miRow, decoder, probStore, c);
            }
        }
        updateRefFrameLineBuffers(miCol, miRow, blSz, c, ref0, ref1, compoundPred);

        return Packed4BitList._3(ref0, ref1, compoundPred ? 1 : 0);
    }

    private static void updateMVLineBuffers(int miCol, int miRow, int blSz, DecodingContext c, long mv) {
        long[][] leftMVs = c.getLeftMVs();
        long[][] aboveMVs = c.getAboveMVs();
        long[][] aboveLeftMVs = c.getAboveLeftMVs();

        for (int i = 0; i < Math.max(3, blW[blSz]); i++) {
            aboveLeftMVs[2][i] = aboveLeftMVs[1][i];
            aboveLeftMVs[1][i] = aboveLeftMVs[0][i];
            aboveLeftMVs[0][i] = aboveMVs[i][miCol + i];
        }

        for (int i = 0; i < Math.max(3, blH[blSz]); i++) {
            int offTop = (miRow + i) % 8;
            aboveLeftMVs[i][2] = aboveLeftMVs[i][1];
            aboveLeftMVs[i][1] = aboveLeftMVs[i][0];
            aboveLeftMVs[i][0] = leftMVs[i][offTop];
        }

        for (int j = 0; j < Math.max(3, blH[blSz]); j++) {
            for (int i = 0; i < blW[blSz]; i++) {
                int offLeft = miCol + i;
                aboveMVs[2][offLeft] = aboveMVs[1][offLeft];
                aboveMVs[1][offLeft] = aboveMVs[0][offLeft];
                aboveMVs[0][offLeft] = mv;
            }
        }

        for (int j = 0; j < Math.max(3, blW[blSz]); j++) {
            for (int i = 0; i < blH[blSz]; i++) {
                int offTop = (miRow + i) % 8;
                leftMVs[2][offTop] = leftMVs[1][offTop];
                leftMVs[1][offTop] = leftMVs[0][offTop];
                leftMVs[0][offTop] = mv;
            }
        }
    }

    private static void updateMVLineBuffers4x4(int miCol, int miRow, int blSz, DecodingContext c, long mvLeft,
            long mvAbove) {
        long[] leftMVs = c.getLeft4x4MVs();
        long[] aboveMVs = c.getAbove4x4MVs();

        aboveMVs[miCol] = mvAbove;
        leftMVs[miRow % 8] = mvLeft;
    }

    private static void updateRefFrameLineBuffers(int miCol, int miRow, int blSz, DecodingContext c, int ref0, int ref1,
            boolean compoundPred) {
        boolean[] aboveCompound = c.getAboveCompound();
        boolean[] leftCompound = c.getLeftCompound();
        int[][][] refs = c.getRefs();
        for (int i = 0; i < blW[blSz]; i++)
            aboveCompound[i + miCol] = compoundPred;
        for (int i = 0; i < blH[blSz]; i++)
            leftCompound[(miRow + i) % 8] = compoundPred;

        for (int i = 0; i < blH[blSz]; i++)
            for (int j = 0; j < blW[blSz]; j++) {
                refs[j][i][0] = ref0;
                refs[j][i][1] = ref1;
            }
    }

    private static int readDiffMv(VPXBooleanDecoder decoder, Probabilities probStore, DecodingContext c,
            long nearNearest) {
        int bestMv = MVList.get(nearNearest, 0);
        boolean useHp = c.isAllowHpMv() && !largeMv(bestMv);
        int joint = decoder.readTree(TREE_MV_JOINT, probStore.getMvJointProbs());

        int diffMv0 = 0, diffMv1 = 0;
        if (joint == MV_JOINT_HZVNZ || joint == MV_JOINT_HNZVNZ)
            diffMv0 = readMvComponent(decoder, probStore, 0, useHp);
        if (joint == MV_JOINT_HNZVZ || joint == MV_JOINT_HNZVNZ)
            diffMv1 = readMvComponent(decoder, probStore, 1, useHp);

        return MV.create(MV.x(bestMv) + diffMv0, MV.y(bestMv) + diffMv1, MV.ref(bestMv));
    }

    private static int readMvComponent(VPXBooleanDecoder decoder, Probabilities probStore, int comp, boolean useHp) {
        boolean sign = decoder.readBitEq() == 1;
        int mvClass = decoder.readTree(MV_CLASS_TREE, probStore.getMvClassProbs()[comp]);
        int mag;
        if (mvClass == 0) {
            int mvClass0Bit = decoder.readBit(probStore.getMvClass0bitProbs()[comp]);
            int mvClass0Fr = decoder.readTree(MV_FR_TREE, probStore.getMvClassFrProbs()[comp][mvClass0Bit]);
            int mvClass0Hp = useHp ? decoder.readBit(probStore.getMvClass0HpProbs()[comp]) : 1;
            mag = ((mvClass0Bit << 3) | (mvClass0Fr << 1) | mvClass0Hp) + 1;
        } else {
            int d = 0;
            for (int i = 0; i < mvClass; i++) {
                int mvBit = decoder.readBit(probStore.getMvBitsProb()[comp][i]);
                d |= mvBit << i;
            }
            mag = CLASS0_SIZE << (mvClass + 2);
            int mvFr = decoder.readTree(MV_FR_TREE, probStore.getMvFrProbs()[comp]);
            int mvHp = useHp ? decoder.readBit(probStore.getMvHpProbs()[comp]) : 1;
            mag += ((d << 3) | (mvFr << 1) | mvHp) + 1;
        }
        return sign ? -mag : mag;
    }

    /**
     * This creates unnecessary nasty complication, unfortunately
     */
    private static boolean largeMv(int mv) {
        return (MV.x(mv) >= 64 || MV.x(mv) <= -64) && (MV.y(mv) >= 64 || MV.y(mv) <= -64);
    }

    /**
     * Finds near and nearest MVs and returns an MVList
     */
    public static long findBestMv(int miCol, int miRow, int blSz, int ref, int blk, DecodingContext c,
            boolean clearHp) {
        long[][] leftMVs = c.getLeftMVs();
        long[][] aboveMVs = c.getAboveMVs();
        long[][] aboveLeftMVs = c.getAboveLeftMVs();
        long[] left4x4MVs = c.getLeft4x4MVs();
        long[] above4x4MVs = c.getAbove4x4MVs();
        long list = 0;
        boolean checkDifferentRef = false;

        // STEP 1. The first two position will be tried separately as for them
        // we might want to take override motion vectors for some of the sub-8x8
        // blocks.
        int pt0 = mv_ref_blocks[blSz][0];
        int pt1 = mv_ref_blocks[blSz][1];
        long mvp0 = getMV(leftMVs, aboveMVs, aboveLeftMVs, pt0, miRow, miCol, c);
        long mvp1 = getMV(leftMVs, aboveMVs, aboveLeftMVs, pt1, miRow, miCol, c);

        // Specifically for block 1 we prefer left predictor on the same row and
        // for block 2 we prefer above predictor on the same column.
        if (blk == 1) {
            mvp0 = mvp0 == -1 ? -1 : left4x4MVs[miRow % 8];
        } else if (blk == 2) {
            mvp1 = mvp1 == -1 ? -1 : above4x4MVs[miCol];
        }

        checkDifferentRef = mvp0 != 0 | mvp1 != 0;

        list = processCandidate(ref, list, mvp0);
        list = processCandidate(ref, list, mvp1);

        // STEP 2: Iterate through the rest of the positions taking only motion
        // vectors for the same reference.
        for (int i = 2; i < mv_ref_blocks[blSz].length && MVList.size(list) < 2; i++) {
            long mvi = getMV(leftMVs, aboveMVs, aboveLeftMVs, mv_ref_blocks[blSz][i], miRow, miCol, c);
            checkDifferentRef |= mvi != 0;
            list = processCandidate(ref, list, mvi);
        }

        // STEP 3: If that was not enough pick up the vectors from the previous
        // frame.
        if (MVList.size(list) < 2 && c.isUsePrevFrameMvs()) {
            long[][] prevFrameMv = c.getPrevFrameMv();
            long prevMv = prevFrameMv[miCol][miRow];
            list = processCandidate(ref, list, prevMv);
        }

        // STEP 4: If that was not enough pick up the vectors for different
        // references.
        if (MVList.size(list) < 2 && checkDifferentRef) {
            for (int i = 0; i < mv_ref_blocks[blSz].length && MVList.size(list) < 2; i++) {
                long mvp = getMV(leftMVs, aboveMVs, aboveLeftMVs, mv_ref_blocks[blSz][i], miRow, miCol, c);
                list = processNECandidate(ref, c, list, mvp);
            }
        }

        // STEP 5: And if that was not enough pick up motion vector(s) from the
        // previous frame for a different reference.
        if (MVList.size(list) < 2 && c.isUsePrevFrameMvs()) {
            long[][] prevFrameMv = c.getPrevFrameMv();
            long prevMv = prevFrameMv[miCol][miRow];
            list = processNECandidate(ref, c, list, prevMv);
        }

        // Just need to calmp MVs and clear the HP in some cases.
        list = clampMvs(miCol, miRow, blSz, c, list);
        if (clearHp) {
            list = clearHp(c, list);
        }

        return list;
    }

    /**
     * Clears the last precision bit (HP) making the MV effectively QPel in case
     * the MV is too large (it's magnitude is greater than 8).
     */
    private static long clearHp(DecodingContext c, long list) {
        int mv0 = MVList.get(list, 0);
        if (!c.isAllowHpMv() || largeMv(mv0)) {
            mv0 = MV.create(MV.x(mv0) & ~1, MV.y(mv0) & ~1, MV.ref(mv0));
        }
        int mv1 = MVList.get(list, 1);
        if (!c.isAllowHpMv() || largeMv(mv1)) {
            mv1 = MV.create(MV.x(mv1) & ~1, MV.y(mv1) & ~1, MV.ref(mv1));
        }

        list = MVList.create(mv0, mv1);
        return list;
    }

    private static long clampMvs(int miCol, int miRow, int blSz, DecodingContext c, long list) {
        int mv0 = MVList.get(list, 0);
        int mv1 = MVList.get(list, 1);

        int mv0xCl = clampMvCol(miCol, blSz, c, MV.x(mv0));
        int mv0yCl = clampMvRow(miRow, blSz, c, MV.y(mv0));
        int mv1xCl = clampMvCol(miCol, blSz, c, MV.x(mv1));
        int mv1yCl = clampMvRow(miRow, blSz, c, MV.y(mv1));
        return MVList.create(MV.create(mv0xCl, mv0yCl, MV.ref(mv0)), MV.create(mv1xCl, mv1yCl, MV.ref(mv1)));
    }

    private static long processNECandidate(int ref, DecodingContext c, long list, long mvp) {
        int mv0 = MVList.get(mvp, 0);
        int mv1 = MVList.get(mvp, 1);
        boolean matchMv = MV.x(mv0) == MV.x(mv1) && MV.y(mv0) == MV.y(mv1);
        list = processNEComponent(ref, c, list, mv0);
        if (!matchMv)
            list = processNEComponent(ref, c, list, mv1);
        return list;
    }

    private static long processNEComponent(int ref, DecodingContext c, long list, int mv0) {
        int ref0 = MV.ref(mv0);
        if (ref0 != INTRA_FRAME && ref0 != ref) {
            // Invert sign in case MV is on the different side
            int q = c.refFrameSignBias(ref0) * c.refFrameSignBias(ref);
            int mv = MV.create(MV.x(mv0) * q, MV.y(mv0), ref);
            list = MVList.addUniq(list, mv);
        }
        return list;
    }

    private static long processCandidate(int ref, long list, long mvp0) {
        int mv00 = MVList.get(mvp0, 0);
        int mv01 = MVList.get(mvp0, 1);
        if (MV.ref(mv00) == ref) {
            list = MVList.addUniq(list, mv00);
        } else if (MV.ref(mv01) == ref) {
            list = MVList.addUniq(list, mv01);
        }
        return list;
    }

    private static long prepandSubMvBlk12(long list, int blkMv) {
        long nlist = 0;

        nlist = MVList.add(nlist, blkMv);
        nlist = MVList.addUniq(nlist, MVList.get(list, 0));
        nlist = MVList.addUniq(nlist, MVList.get(list, 0));
        return nlist;
    }

    private static long prepandSubMvBlk3(long list, int blk0Mv, int blk1Mv, int blk2Mv) {
        long nlist = 0;

        nlist = MVList.add(nlist, blk2Mv);
        nlist = MVList.addUniq(nlist, blk1Mv);
        nlist = MVList.addUniq(nlist, blk0Mv);
        nlist = MVList.addUniq(nlist, MVList.get(list, 0));
        nlist = MVList.addUniq(nlist, MVList.get(list, 0));

        return nlist;
    }

    private static int clampMvRow(int miRow, int blSz, DecodingContext c, int mv) {
        int mbToTopEdge = -(miRow << 6);
        int mbToBottomEdge = (c.getMiFrameHeight() - blH[blSz] - miRow) << 6;
        return clip3(mbToTopEdge - 128, mbToBottomEdge + 128, mv);
    }

    private static int clip3(int from, int to, int v) {
        return v < from ? from : (v > to ? to : v);
    }

    private static int clampMvCol(int miCol, int blSz, DecodingContext c, int mv) {
        int mbToLeftEdge = -(miCol << 6);
        int mbToRightEdge = (c.getMiFrameWidth() - blW[blSz] - miCol) << 6;
        return clip3(mbToLeftEdge - 128, mbToRightEdge + 128, mv);
    }

    private static int readInterpFilter(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder,
            Probabilities probStore, DecodingContext c) {
        boolean availAbove = miRow > 0; // Frame based
        boolean availLeft = miCol > c.getTileStart(); // Tile based
        int[][][] refs = c.getRefs();
        int aboveRefFrame0 = refs[miRow - 1][miCol][0];
        int leftRefFrame0 = refs[miRow][miCol - 1][0];
        int[] leftInterpFilters = c.getLeftInterpFilters();
        int[] aboveInterpFilters = c.getAboveInterpFilters();
        int ctx;
        int leftInterp = (availLeft && leftRefFrame0 > INTRA_FRAME) ? leftInterpFilters[miRow % 8] : 3;
        int aboveInterp = (availAbove && aboveRefFrame0 > INTRA_FRAME) ? aboveInterpFilters[miCol] : 3;
        if (leftInterp == aboveInterp)
            ctx = leftInterp;
        else if (leftInterp == 3 && aboveInterp != 3)
            ctx = aboveInterp;
        else if (leftInterp != 3 && aboveInterp == 3)
            ctx = leftInterp;
        else
            ctx = 3;

        int[][] probs = probStore.getInterpFilterProbs();

        int ret = decoder.readTree(TREE_INTERP_FILTER, probs[ctx]);

        for (int i = 0; i < blW[blSz]; i++)
            aboveInterpFilters[miCol + i] = ret;
        for (int i = 0; i < blH[blSz]; i++)
            leftInterpFilters[(miRow + i) % 8] = ret;

        return ret;
    }

    private static int readLumaMode(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder, Probabilities probStore,
            DecodingContext c) {
        int ind0 = mv_ref_blocks_sm[blSz][0];
        int ind1 = mv_ref_blocks_sm[blSz][1];
        int[] leftModes = c.getLeftLumaModes();
        int[] aboveModes = c.getAboveLumaModes();

        int mode0 = getMode(leftModes, aboveModes, ind0, miRow, miCol, c);
        int mode1 = getMode(leftModes, aboveModes, ind1, miRow, miCol, c);
        int ctx;
        if ((mode0 == NEARMV || mode0 == NEARESTMV)) {
            if ((mode1 == NEARMV || mode1 == NEARESTMV)) {
                ctx = BOTH_PREDICTED;
            } else if (mode1 == NEWMV) {
                ctx = NEW_PLUS_NON_INTRA;
            } else if (mode1 == ZEROMV) {
                ctx = ZERO_PLUS_PREDICTED;
            } else {
                ctx = INTRA_PLUS_NON_INTRA;
            }
        } else if (mode0 == ZEROMV) {
            if ((mode1 == NEARMV || mode1 == NEARESTMV)) {
                ctx = ZERO_PLUS_PREDICTED;
            } else if (mode1 == NEWMV) {
                ctx = NEW_PLUS_NON_INTRA;
            } else if (mode1 == ZEROMV) {
                ctx = BOTH_ZERO;
            } else {
                ctx = INTRA_PLUS_NON_INTRA;
            }
        } else if (mode0 == NEWMV) {
            if ((mode1 == NEARMV || mode1 == NEARESTMV)) {
                ctx = NEW_PLUS_NON_INTRA;
            } else if (mode1 == NEWMV) {
                ctx = BOTH_NEW;
            } else if (mode1 == ZEROMV) {
                ctx = NEW_PLUS_NON_INTRA;
            } else {
                ctx = NEW_PLUS_NON_INTRA;
            }
        } else {
            ctx = mode1 >= NEARESTMV ? INTRA_PLUS_NON_INTRA : BOTH_INTRA;
        }
        int[][] probs = probStore.getInterModeProbs();

        int ret = NEARESTMV + decoder.readTree(TREE_INTER_MODE, probs[ctx]);

        for (int i = 0; i < blW[blSz]; i++)
            aboveModes[miCol + i] = ret;
        for (int i = 0; i < blH[blSz]; i++)
            leftModes[(miRow + i) % 8] = ret;

        return ret;
    }

    private static int getMode(int[] leftModes, int[] aboveModes, int ind0, int miRow, int miCol, DecodingContext c) {
        switch (ind0) {
        // (-1,0)
        case 0:
            return miCol >= c.getTileStart() ? leftModes[miRow % 8] : -1;
        // (0,-1)
        case 1:
            return miRow > 0 ? aboveModes[miCol] : -1;
        // (-1,1)
        case 2:
            return miCol >= c.getTileStart() && miRow < c.getTileHeight() - 1 ? leftModes[(miRow % 8) + 1] : -1;
        // (1,-1)
        case 3:
            return miRow > 0 && miCol < c.getTileWidth() - 1 ? aboveModes[miCol + 1] : -1;
        // (-1, 3)
        case 4:
            return miCol >= c.getTileStart() && miRow < c.getTileHeight() - 3 ? leftModes[(miRow % 8) + 3] : -1;
        // (3, -1)
        case 5:
            return miRow > 0 && miCol < c.getTileWidth() - 3 ? aboveModes[miCol + 3] : -1;
        }
        return -1;
    }

    private static long getMV(long[][] leftMV, long[][] aboveMV, long[][] aboveLeftMV, int ind0, int miRow, int miCol,
            DecodingContext c) {
        int ts = c.getTileStart();
        int th = c.getTileHeight();
        int tw = c.getTileWidth();
        switch (ind0) {
        // (-1,0)
        case 0:
            return miCol >= ts ? leftMV[0][miRow % 8] : 0;
        // (0,-1)
        case 1:
            return miRow > 0 ? aboveMV[0][miCol] : 0;
        // (-1,1)
        case 2:
            return miCol >= ts && miRow < th - 1 ? leftMV[0][(miRow % 8) + 1] : 0;
        // (1,-1)
        case 3:
            return miRow > 0 && miCol < tw - 1 ? aboveMV[0][miCol + 1] : 0;
        // (-1, 3)
        case 4:
            return miCol >= ts && miRow < th - 3 ? leftMV[0][(miRow % 8) + 3] : 0;
        // (3, -1)
        case 5:
            return miRow > 0 && miCol < tw - 3 ? aboveMV[0][miCol + 3] : 0;
        // 6 -> (-1, 2)
        case 6:
            return miCol >= ts && miRow < th - 2 ? leftMV[0][(miRow % 8) + 2] : 0;
        // 7 -> (2, -1)
        case 7:
            return miRow > 0 && miCol < tw - 2 ? aboveMV[0][miCol + 2] : 0;
        // 8 -> (-1, 4)
        case 8:
            return miCol >= ts && miRow < th - 4 ? leftMV[0][(miRow % 8) + 4] : 0;
        // 9 -> (4, -1)
        case 9:
            return miRow > 0 && miCol < tw - 4 ? aboveMV[0][miCol + 4] : 0;
        // 10 -> (-1, 6)
        case 10:
            return miCol >= ts && miRow < th - 6 ? leftMV[0][(miRow % 8) + 6] : 0;
        // 11 -> (-1, -1)
        case 11:
            return miCol >= ts && miRow > 0 ? aboveLeftMV[0][0] : 0;
        // 12 -> (-2, 0)
        case 12:
            return miCol >= ts + 1 ? leftMV[1][miRow % 8] : 0;
        // 13 -> (0, -2)
        case 13:
            return miRow > 1 ? aboveMV[1][miCol] : 0;
        // 14 -> (-3, 0)
        case 14:
            return miCol >= ts + 2 ? leftMV[2][miRow % 8] : 0;
        // 15 -> (0, -3)
        case 15:
            return miRow > 2 ? aboveMV[2][miCol] : 0;
        // 16 -> (-2, -1)
        case 16:
            return miCol >= ts + 1 && miRow > 0 ? aboveLeftMV[0][1] : 0;
        // 17 -> (-1, -2)
        case 17:
            return miCol >= ts && miRow > 1 ? aboveLeftMV[1][0] : 0;
        // 18 -> (-2, -2)
        case 18:
            return miCol >= ts + 1 && miRow > 1 ? aboveLeftMV[1][1] : 0;
        // 19 -> (-3, -3)
        case 19:
            return miCol >= ts + 2 && miRow > 2 ? aboveLeftMV[2][2] : 0;
        }
        return 0;
    }

    private static int readCompRef(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder, Probabilities probStore,
            DecodingContext c) {
        int compFixedRef = c.getCompFixedRef();
        int fixRefIdx = c.refFrameSignBias(compFixedRef);
        boolean availAbove = miRow > 0; // Frame based
        boolean availLeft = miCol > c.getTileStart(); // Tile based
        boolean[] aboveCompound = c.getAboveCompound();
        boolean[] leftCompound = c.getLeftCompound();
        int[][][] refs = c.getRefs();
        int aboveRefFrame0 = refs[miRow - 1][miCol][0];
        int leftRefFrame0 = refs[miRow][miCol - 1][0];
        int aboveRefFrame1 = refs[miRow - 1][miCol][1];
        int leftRefFrame1 = refs[miRow][miCol - 1][1];
        boolean aboveIntra = aboveRefFrame0 <= INTRA_FRAME;
        boolean leftIntra = leftRefFrame0 <= INTRA_FRAME;
        boolean aboveSingle = !aboveCompound[miCol];
        boolean leftSingle = !leftCompound[miRow % 8];
        int aboveVarRefFrame, leftVarRefFrame;
        if (fixRefIdx == 0) {
            aboveVarRefFrame = aboveRefFrame1;
            leftVarRefFrame = leftRefFrame1;
        } else {
            aboveVarRefFrame = aboveRefFrame0;
            leftVarRefFrame = leftRefFrame0;
        }

        int compVarRef0 = c.getCompVarRef(0);
        int compVarRef1 = c.getCompVarRef(1);

        int ctx;
        if (availAbove && availLeft) {
            if (aboveIntra && leftIntra) {
                ctx = 2;
            } else if (leftIntra) {
                if (aboveSingle)
                    ctx = 1 + 2 * (aboveRefFrame0 != compVarRef1 ? 1 : 0);
                else
                    ctx = 1 + 2 * (aboveVarRefFrame != compVarRef1 ? 1 : 0);
            } else if (aboveIntra) {
                if (leftSingle)
                    ctx = 1 + 2 * (leftRefFrame0 != compVarRef1 ? 1 : 0);
                else
                    ctx = 1 + 2 * (leftVarRefFrame != compVarRef1 ? 1 : 0);
            } else {
                int vrfa = aboveSingle ? aboveRefFrame0 : aboveVarRefFrame;
                int vrfl = leftSingle ? leftRefFrame0 : leftVarRefFrame;
                if (vrfa == vrfl && compVarRef1 == vrfa) {
                    ctx = 0;
                } else if (leftSingle && aboveSingle) {
                    if ((vrfa == compFixedRef && vrfl == compVarRef0) || (vrfl == compFixedRef && vrfa == compVarRef0))
                        ctx = 4;
                    else if (vrfa == vrfl)
                        ctx = 3;
                    else
                        ctx = 1;
                } else if (leftSingle || aboveSingle) {
                    int vrfc = leftSingle ? vrfa : vrfl;
                    int rfs = aboveSingle ? vrfa : vrfl;
                    if (vrfc == compVarRef1 && rfs != compVarRef1)
                        ctx = 1;
                    else if (rfs == compVarRef1 && vrfc != compVarRef1)
                        ctx = 2;
                    else
                        ctx = 4;
                } else if (vrfa == vrfl) {
                    ctx = 4;
                } else {
                    ctx = 2;
                }
            }
        } else if (availAbove) {
            if (aboveIntra) {
                ctx = 2;
            } else {
                if (aboveSingle)
                    ctx = 3 * (aboveRefFrame0 != compVarRef1 ? 1 : 0);
                else
                    ctx = 4 * (aboveVarRefFrame != compVarRef1 ? 1 : 0);
            }
        } else if (availLeft) {
            if (leftIntra) {
                ctx = 2;
            } else {
                if (leftSingle)
                    ctx = 3 * (leftRefFrame0 != compVarRef1 ? 1 : 0);
                else
                    ctx = 4 * (leftVarRefFrame != compVarRef1 ? 1 : 0);
            }
        } else {
            ctx = 2;
        }

        int[] probs = probStore.getCompRefProbs();

        return decoder.readBit(probs[ctx]);
    }

    private static int readSingleRef(int miCol, int miRow, VPXBooleanDecoder decoder, Probabilities probStore,
            DecodingContext c) {
        boolean singleRefP1 = readSingRef(0, miCol, miRow, decoder, probStore, c);
        if (singleRefP1) {
            boolean singleRefP2 = readSingRef(0, miCol, miRow, decoder, probStore, c);
            return singleRefP2 ? ALTREF_FRAME : GOLDEN_FRAME;
        } else {
            return LAST_FRAME;
        }
    }

    private static boolean readSingRef(int bin, int miCol, int miRow, VPXBooleanDecoder decoder,
            Probabilities probStore, DecodingContext c) {
        boolean availAbove = miRow > 0; // Frame based
        boolean availLeft = miCol > c.getTileStart(); // Tile based
        boolean[] aboveCompound = c.getAboveCompound();
        boolean[] leftCompound = c.getLeftCompound();
        int[][][] refs = c.getRefs();
        int aboveRefFrame0 = refs[miRow - 1][miCol][0];
        int leftRefFrame0 = refs[miRow][miCol - 1][0];
        int aboveRefFrame1 = refs[miRow - 1][miCol][1];
        int leftRefFrame1 = refs[miRow][miCol - 1][1];
        boolean aboveIntra = aboveRefFrame0 <= INTRA_FRAME;
        boolean leftIntra = leftRefFrame0 <= INTRA_FRAME;
        boolean aboveSingle = !aboveCompound[miCol];
        boolean leftSingle = !leftCompound[miRow % 8];

        int ctx;
        if (availAbove && availLeft) {
            if (aboveIntra && leftIntra) {
                ctx = 2;
            } else if (leftIntra) {
                if (aboveSingle) {
                    if (bin == 0)
                        ctx = 4 * (aboveRefFrame0 == LAST_FRAME ? 1 : 0);
                    else {
                        if (aboveRefFrame0 == LAST_FRAME)
                            ctx = 3;
                        else
                            ctx = 4 * (aboveRefFrame0 == GOLDEN_FRAME ? 1 : 0);
                    }
                } else {
                    if (bin == 0)
                        ctx = 1 + (aboveRefFrame0 == LAST_FRAME || aboveRefFrame1 == LAST_FRAME ? 1 : 0);
                    else
                        ctx = 1 + 2 * (aboveRefFrame0 == GOLDEN_FRAME || aboveRefFrame1 == GOLDEN_FRAME ? 1 : 0);
                }
            } else if (aboveIntra) {
                if (leftSingle) {
                    if (bin == 0)
                        ctx = 4 * (leftRefFrame0 == LAST_FRAME ? 1 : 0);
                    else {
                        if (leftRefFrame0 == LAST_FRAME)
                            ctx = 3;
                        else
                            ctx = 4 * (leftRefFrame0 == GOLDEN_FRAME ? 1 : 0);
                    }
                } else {
                    if (bin == 0)
                        ctx = 1 + (leftRefFrame0 == LAST_FRAME || leftRefFrame1 == LAST_FRAME ? 1 : 0);
                    else
                        ctx = 1 + 2 * (leftRefFrame0 == GOLDEN_FRAME || leftRefFrame1 == GOLDEN_FRAME ? 1 : 0);
                }
            } else {
                if (aboveSingle && leftSingle) {
                    if (bin == 0) {
                        ctx = 2 * (aboveRefFrame0 == LAST_FRAME ? 1 : 0) + 2 * (leftRefFrame0 == LAST_FRAME ? 1 : 0);
                    } else {
                        if (aboveRefFrame0 == LAST_FRAME && leftRefFrame0 == LAST_FRAME) {
                            ctx = 3;
                        } else if (aboveRefFrame0 == LAST_FRAME) {
                            ctx = 4 * (leftRefFrame0 == GOLDEN_FRAME ? 1 : 0);
                        } else if (leftRefFrame0 == LAST_FRAME) {
                            ctx = 4 * (aboveRefFrame0 == GOLDEN_FRAME ? 1 : 0);
                        } else {
                            ctx = 2 * (aboveRefFrame0 == GOLDEN_FRAME ? 1 : 0)
                                    + 2 * (leftRefFrame0 == GOLDEN_FRAME ? 1 : 0);
                        }
                    }
                } else if (!aboveSingle && !leftSingle) {
                    if (bin == 0) {
                        ctx = 1 + (aboveRefFrame0 == LAST_FRAME || aboveRefFrame1 == LAST_FRAME
                                || leftRefFrame0 == LAST_FRAME || leftRefFrame1 == LAST_FRAME ? 1 : 0);
                    } else {
                        if (aboveRefFrame0 == leftRefFrame0 && aboveRefFrame1 == leftRefFrame1)
                            ctx = 3 * (aboveRefFrame0 == GOLDEN_FRAME || aboveRefFrame1 == GOLDEN_FRAME ? 1 : 0);
                        else
                            ctx = 2;
                    }
                } else {
                    int rfs = aboveSingle ? aboveRefFrame0 : leftRefFrame0;
                    int crf1 = aboveSingle ? leftRefFrame0 : aboveRefFrame0;
                    int crf2 = aboveSingle ? leftRefFrame1 : aboveRefFrame1;
                    if (bin == 0) {
                        if (rfs == LAST_FRAME)
                            ctx = 3 + (crf1 == LAST_FRAME || crf2 == LAST_FRAME ? 1 : 0);
                        else
                            ctx = crf1 == LAST_FRAME || crf2 == LAST_FRAME ? 1 : 0;
                    } else {
                        if (rfs == GOLDEN_FRAME)
                            ctx = 3 + (crf1 == GOLDEN_FRAME || crf2 == GOLDEN_FRAME ? 1 : 0);
                        else if (rfs == ALTREF_FRAME)
                            ctx = crf1 == GOLDEN_FRAME || crf2 == GOLDEN_FRAME ? 1 : 0;
                        else
                            ctx = 1 + 2 * (crf1 == GOLDEN_FRAME || crf2 == GOLDEN_FRAME ? 1 : 0);
                    }
                }
            }
        } else if (availAbove) {
            if (aboveIntra || (bin == 1 && aboveRefFrame0 == LAST_FRAME && aboveSingle)) {
                ctx = 2;
            } else { // inter

                if (aboveSingle) {
                    if (bin == 0)
                        ctx = 4 * (aboveRefFrame0 == LAST_FRAME ? 1 : 0);
                    else
                        ctx = 4 * (aboveRefFrame0 == GOLDEN_FRAME ? 1 : 0);
                } else {
                    if (bin == 0)
                        ctx = 1 + (aboveRefFrame0 == LAST_FRAME || aboveRefFrame1 == LAST_FRAME ? 1 : 0);
                    else
                        ctx = 3 * (aboveRefFrame0 == GOLDEN_FRAME || aboveRefFrame1 == GOLDEN_FRAME ? 1 : 0);
                }
            }
        } else if (availLeft) {

            if (leftIntra || (bin == 1 && leftRefFrame0 == LAST_FRAME && leftSingle)) {
                ctx = 2;
            } else {
                if (leftSingle) {
                    if (bin == 0)
                        ctx = 4 * (leftRefFrame0 == LAST_FRAME ? 1 : 0);
                    else
                        ctx = 4 * (leftRefFrame0 == GOLDEN_FRAME ? 1 : 0);
                } else {
                    if (bin == 0)
                        ctx = 1 + (leftRefFrame0 == LAST_FRAME || leftRefFrame1 == LAST_FRAME ? 1 : 0);
                    else
                        ctx = 3 * (leftRefFrame0 == GOLDEN_FRAME || leftRefFrame1 == GOLDEN_FRAME ? 1 : 0);
                }
            }
        } else {
            ctx = 2;
        }
        int[][] probs = probStore.getSingleRefProbs();

        return decoder.readBit(probs[ctx][bin]) == 1;
    }

    private static boolean readCompMode(int miCol, int miRow, VPXBooleanDecoder decoder, Probabilities probStore,
            DecodingContext c) {
        boolean availAbove = miRow > 0; // Frame based
        boolean availLeft = miCol > c.getTileStart(); // Tile based
        boolean[] aboveCompound = c.getAboveCompound();
        boolean[] leftCompound = c.getLeftCompound();
        int[][][] refs = c.getRefs();
        int aboveRefFrame0 = refs[miRow - 1][miCol][0];
        int leftRefFrame0 = refs[miRow][miCol - 1][0];
        int compFixedRef = c.getCompFixedRef();
        boolean aboveSingle = !aboveCompound[miCol];
        boolean leftSingle = !leftCompound[miRow % 8];
        boolean aboveIntra = aboveRefFrame0 <= INTRA_FRAME;
        boolean leftIntra = leftRefFrame0 <= INTRA_FRAME;

        int ctx;
        if (availAbove && availLeft) {
            if (aboveSingle && leftSingle)
                ctx = (aboveRefFrame0 == compFixedRef) ^ (leftRefFrame0 == compFixedRef) ? 1 : 0;
            else if (aboveSingle) {
                ctx = 2 + (aboveRefFrame0 == compFixedRef || aboveIntra ? 1 : 0);
            } else if (leftSingle)
                ctx = 2 + (leftRefFrame0 == compFixedRef || leftIntra ? 1 : 0);
            else
                ctx = 4;
        } else if (availAbove) {
            if (aboveSingle)
                ctx = aboveRefFrame0 == compFixedRef ? 1 : 0;
            else
                ctx = 3;
        } else if (availLeft) {
            if (leftSingle)
                ctx = leftRefFrame0 == compFixedRef ? 1 : 0;
            else
                ctx = 3;
        } else {
            ctx = 1;
        }
        int[] probs = probStore.getCompModeProbs();

        return decoder.readBit(probs[ctx]) == 1;
    }

    private static InterModeInfo readIntraSpecificMode(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder,
            Probabilities probs, DecodingContext c, int segmentId, boolean skip, int txSize) {
        int yMode;
        int subModes = 0;
        if (blSz >= BLOCK_8X8) {
            yMode = readIntraMode(miCol, miRow, blSz, decoder, probs, c);
        } else {
            subModes = readSubIntraMode(miCol, miRow, blSz, decoder, probs, c);
            // last submode is always the lowest byte
            yMode = subModes & 0xff;
        }
        int uvMode = readUVMode(yMode, decoder, probs, c);

        return new InterModeInfo(segmentId, skip, txSize, yMode, subModes, uvMode,0,0,0,0);
    }

    private static int readIntraMode(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder, Probabilities probStore,
            DecodingContext c) {
        int[][] probs = probStore.getYModeProbs();
        return decoder.readTree(TREE_INTRA_MODE, probs[size_group_lookup[blSz]]);
    }

    private static int readSubIntraMode(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder,
            Probabilities probStore, DecodingContext c) {
        int[][] probs = probStore.getYModeProbs();
        return decoder.readTree(TREE_INTRA_MODE, probs[0]);
    }

    private static int readUVMode(int yMode, VPXBooleanDecoder decoder, Probabilities probStore, DecodingContext c) {
        int[][] probs = probStore.getUVModeProbs();
        return decoder.readTree(TREE_INTRA_MODE, probs[yMode]);
    }

    private static boolean readIsInter(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder,
            Probabilities probStore, DecodingContext c) {
        boolean availAbove = miRow > 0; // Frame based
        boolean availLeft = miCol > c.getTileStart(); // Tile based
        int[][][] refs = c.getRefs();

        boolean leftIntra = availLeft ? refs[miRow][miCol - 1][0] <= INTRA_FRAME : true;
        boolean aboveIntra = availAbove ? refs[miRow - 1][miCol][0] <= INTRA_FRAME : true;

        int ctx = 0;
        if (availAbove && availLeft)
            ctx = (leftIntra && aboveIntra) ? 3 : (leftIntra || aboveIntra ? 1 : 0);
        else if (availAbove || availLeft)
            ctx = 2 * (availAbove ? (aboveIntra ? 1 : 0) : (leftIntra ? 1 : 0));

        int[] probs = probStore.getIsInterProbs();
        return decoder.readBit(probs[ctx]) == 1;
    }

    private static boolean readSegIdPredicted(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder,
            Probabilities probs, DecodingContext c) {
        boolean[] aboveSegIdPredicted = c.getAboveSegIdPredicted();
        boolean[] leftSegIdPredicted = c.getLeftSegIdPredicted();

        int ctx = (aboveSegIdPredicted[miRow] ? 1 : 0) + (leftSegIdPredicted[miCol] ? 1 : 0);
        int[] prob = probs.getSegmentationPredProb();

        boolean ret = decoder.readBit(prob[ctx]) == 1;

        for (int i = 0; i < blH[blSz]; i++)
            aboveSegIdPredicted[miCol + i] = ret;
        for (int i = 0; i < blW[blSz]; i++)
            leftSegIdPredicted[miRow + i] = ret;

        return false;
    }

    private static int predicSegmentId(int miCol, int miRow, int blSz, DecodingContext c) {
        int blWcl = Math.min(c.getTileWidth() - miCol, blW[blSz]);
        int blHcl = Math.min(c.getTileHeight() - miRow, blH[blSz]);
        int[][] prevSegmentIds = c.getPrevSegmentIds();
        int seg = 7;
        for (int y = 0; y < blHcl; y++)
            for (int x = 0; x < blWcl; x++)
                seg = Math.min(seg, prevSegmentIds[miRow + y][miCol + x]);
        return seg;
    }

}
