package org.jcodec.codecs.vpx.tweak;

import static org.jcodec.codecs.vpx.VP8Encoder.zigzag;
import static org.jcodec.codecs.vpx.VPXConst.B_DC_PRED;
import static org.jcodec.codecs.vpx.VPXConst.B_HD_PRED;
import static org.jcodec.codecs.vpx.VPXConst.B_HE_PRED;
import static org.jcodec.codecs.vpx.VPXConst.B_LD_PRED;
import static org.jcodec.codecs.vpx.VPXConst.B_PRED;
import static org.jcodec.codecs.vpx.VPXConst.B_RD_PRED;
import static org.jcodec.codecs.vpx.VPXConst.B_TM_PRED;
import static org.jcodec.codecs.vpx.VPXConst.B_VE_PRED;
import static org.jcodec.codecs.vpx.VPXConst.B_VL_PRED;
import static org.jcodec.codecs.vpx.VPXConst.B_VR_PRED;
import static org.jcodec.codecs.vpx.VPXConst.DC_PRED;
import static org.jcodec.codecs.vpx.VPXConst.H_PRED;
import static org.jcodec.codecs.vpx.VPXConst.MVSUB_LEFT;
import static org.jcodec.codecs.vpx.VPXConst.MVSUB_NEW;
import static org.jcodec.codecs.vpx.VPXConst.MVSUB_TOP;
import static org.jcodec.codecs.vpx.VPXConst.MVSUB_ZERO;
import static org.jcodec.codecs.vpx.VPXConst.MV_NEAR;
import static org.jcodec.codecs.vpx.VPXConst.MV_NEAREST;
import static org.jcodec.codecs.vpx.VPXConst.MV_NEW;
import static org.jcodec.codecs.vpx.VPXConst.MV_SPLIT;
import static org.jcodec.codecs.vpx.VPXConst.MV_SPLT_16;
import static org.jcodec.codecs.vpx.VPXConst.MV_SPLT_2_HOR;
import static org.jcodec.codecs.vpx.VPXConst.MV_SPLT_4;
import static org.jcodec.codecs.vpx.VPXConst.MV_ZERO;
import static org.jcodec.codecs.vpx.VPXConst.TM_PRED;
import static org.jcodec.codecs.vpx.VPXConst.V_PRED;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.jcodec.codecs.common.biari.VPxBooleanEncoder;
import org.jcodec.codecs.vpx.VP8Encoder;
import org.jcodec.codecs.vpx.VPXBitstream;
import org.jcodec.codecs.vpx.VPXConst;
import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Serializes VP8 symbols into VP8 stream
 * 
 * @author The JCodec project
 * 
 */
public class VP8Serializer implements VP8Handler {

    private VPXBitstream bitstream;
    private VPxBooleanEncoder resEnc;
    private VPxBooleanEncoder predEnc;
    private ByteBuffer out;
    private int mbWidth;
    private int[] tmp = new int[16];
    private ByteBuffer predBuf;
    private boolean keyFrame;
    private int[] bPredTop;
    private int[] bPredLeft;

    private int[] topMvMode;
    private int leftMvMode;
    private int topLeftMvMode;
    private int[] topMv;
    private int leftMv;
    private int topLeftMv;

    private int[] bMvTop;
    private int[] bMvLeft;
    private int mbHeight;

    public static final int PART_MV_16 = 0, PART_MV_QUART = 1, PART_MV_TOP_BOT = 2, PART_MV_LEFT_RIGHT = 3;

    public VP8Serializer(ByteBuffer _buf, int mbWidth, int mbHeight, boolean keyFrame, int version) {
        // System.err.println("\n\n");

        out = _buf.duplicate();
        out.order(ByteOrder.LITTLE_ENDIAN);

        this.mbWidth = mbWidth;
        this.mbHeight = mbHeight;
        bitstream = new VPXBitstream(VPXConst.tokenDefaultBinProbs, mbWidth);

        int predLen = 512 + (mbWidth * mbHeight << 3);
        predBuf = out.duplicate();
        predBuf.limit(predLen);
        predBuf.order(ByteOrder.LITTLE_ENDIAN);
        out.position(predLen);

        VP8Encoder.writeHeader(predBuf, predLen - (keyFrame ? 10 : 3), keyFrame, version);
        if (keyFrame)
            VP8Encoder.writeHeaderWH(predBuf, mbWidth << 4, mbHeight << 4);

        this.keyFrame = keyFrame;

        resEnc = new VPxBooleanEncoder(out);
        predEnc = new VPxBooleanEncoder(predBuf);
        bPredTop = new int[mbWidth << 2];
        bPredLeft = new int[4];

        bMvTop = new int[mbWidth << 2];
        bMvLeft = new int[4];

        topMvMode = new int[mbWidth];
        topLeftMvMode = leftMvMode = -1;
        Arrays.fill(topMvMode, -1);

        topMv = new int[mbWidth];
    }

    @Override
    public void beginFrame(int[] qps) {
        segmentProbs = new int[] { 128, 128, 128 };
        writeHeader(predEnc, keyFrame, qps, segmentProbs);
    }

    public ByteBuffer getResult() {
        predEnc.stop();
        resEnc.stop();
        out.flip();

        return out;
    }

    public static void getMVModeProbs(int[] probs, int[] cnt) {
        probs[0] = VPXConst.mv_mode_contexts[cnt[0]][0];
        probs[1] = VPXConst.mv_mode_contexts[cnt[1]][1];
        probs[2] = VPXConst.mv_mode_contexts[cnt[2]][2];
        probs[3] = VPXConst.mv_mode_contexts[cnt[3]][3];
    }

    public static void writeMvMode(int mode, int[] probs, VPxBooleanEncoder boolEnc) {
        if (mode == MV_ZERO)
            boolEnc.writeBit(probs[0], 0);
        else {
            boolEnc.writeBit(probs[0], 1);
            if (mode == MV_NEAREST)
                boolEnc.writeBit(probs[1], 0);
            else {
                boolEnc.writeBit(probs[1], 1);
                if (mode == MV_NEAR)
                    boolEnc.writeBit(probs[2], 0);
                else {
                    boolEnc.writeBit(probs[2], 1);
                    boolEnc.writeBit(probs[3], mode - MV_NEW);
                }
            }
        }
    }

    private void writeMvComponent(int val, int comp, VPxBooleanEncoder boolEnc) {
        int[] probs = VPXConst.mv_default_probs[comp];
        int sign = MathUtil.sign(val), abs = MathUtil.abs(val);
        if (abs <= 7) {
            boolEnc.writeBit(probs[0], 0);
            writeMvTree(abs, probs, boolEnc);
        } else {
            boolEnc.writeBit(probs[0], 1);
            for (int i = 0; i < 3; i++)
                boolEnc.writeBit(probs[9 + i], (abs >> i) & 1);
            for (int i = 9; i > 3; i--)
                boolEnc.writeBit(probs[9 + i], (abs >> i) & 1);
            if (abs > 15) {
                boolEnc.writeBit(probs[12], (abs >> 3) & 1);
            }
        }
        if (abs != 0)
            boolEnc.writeBit(probs[1], sign);
    }

    private void writeMvTree(int val, int[] probs, VPxBooleanEncoder boolEnc) {
        int probi = 0;
        int i1 = (val >> 2) & 1;
        boolEnc.writeBit(probs[probi + 2], i1);
        probi += i1 * 3 + 1;

        int i2 = (val >> 1) & 1;
        boolEnc.writeBit(probs[probi + 2], i2);
        probi += i2 + 1;

        int i3 = val & 1;
        boolEnc.writeBit(probs[probi + 2], i3);
    }

    public static void writeMvPartition(int part, VPxBooleanEncoder boolEnc) {
        if (part == PART_MV_16)
            boolEnc.writeBit(110, 0);
        else {
            boolEnc.writeBit(110, 1);
            if (part == PART_MV_QUART)
                boolEnc.writeBit(111, 0);
            else {
                boolEnc.writeBit(111, 1);
                boolEnc.writeBit(150, part - PART_MV_TOP_BOT);
            }
        }
    }

    public static void writeInt(VPxBooleanEncoder boolEnc, int data, int bits) {
        int bit;

        for (bit = bits - 1; bit >= 0; bit--)
            boolEnc.writeBit(128, (1 & (data >> bit)));
    }

    private void writeHeader(VPxBooleanEncoder predBe, boolean keyFrame, int[] segmentQps, int[] probs) {
        if (keyFrame) {
            predBe.writeBit(128, 0); // clr_type
            predBe.writeBit(128, 0); // clamp_type
        }
        writeSegmentInfo(predBe, segmentQps, probs);
        predBe.writeBit(128, 0); // filter type
        writeInt(predBe, 1, 6); // filter level
        writeInt(predBe, 0, 3); // sharpness level
        predBe.writeBit(128, 0); // deltas enabled
        writeInt(predBe, 0, 2); // partition type
        writeInt(predBe, segmentQps[0], 7);
        predBe.writeBit(128, 0); // y1dc_delta_q
        predBe.writeBit(128, 0); // y2dc_delta_q
        predBe.writeBit(128, 0); // y2ac_delta_q
        predBe.writeBit(128, 0); // uvdc_delta_q
        predBe.writeBit(128, 0); // uvac_delta_q
        if (keyFrame)
            predBe.writeBit(128, 0); // refresh entropy probs
        else {
            predBe.writeBit(128, 0); // refresh golden frame
            predBe.writeBit(128, 0); // refresh alt-ref frame
            writeInt(predBe, 0, 2); // copy buffer to golden
            writeInt(predBe, 0, 2); // copy buffer to altref
            predBe.writeBit(128, 0); // sign bias golden
            predBe.writeBit(128, 0); // sign bias altref
            predBe.writeBit(128, 0); // refresh entropy probs
            predBe.writeBit(128, 1); // refresh last
        }

        int[][][][] probFlags = VPXConst.tokenProbUpdateFlagProbs;
        for (int i = 0; i < probFlags.length; i++) {
            for (int j = 0; j < probFlags[i].length; j++) {
                for (int k = 0; k < probFlags[i][j].length; k++) {
                    for (int l = 0; l < probFlags[i][j][k].length; l++)
                        predBe.writeBit(probFlags[i][j][k][l], 0);
                }
            }
        }

        predBe.writeBit(128, 0); // mb_no_coeff_skip

        if (!keyFrame) {
            writeInt(predBe, 128, 8); // prob intra
            writeInt(predBe, 128, 8); // prob last
            writeInt(predBe, 128, 8); // prob gf
            predBe.writeBit(128, 0); // intra_16x16_prob_update_flag
            predBe.writeBit(128, 0); // intra_chroma_prob_update_flag
            for (int i = 0; i < 2; i++)
                for (int j = 0; j < 19; j++)
                    predBe.writeBit(VPXConst.mv_update_probs[i][j], 0); // mv_prob_update_flag
        }
    }

    private void writeSegmentInfo(VPxBooleanEncoder predBe, int[] segmentQps, int[] probs) {
        predBe.writeBit(128, 1); // segmentation enabled
        predBe.writeBit(128, 1); // update_mb_segmentation_map
        predBe.writeBit(128, 1); // update_segment_feature_data

        predBe.writeBit(128, 1); // segment_feature_mode - absolute

        for (int i = 0; i < segmentQps.length; i++) {
            predBe.writeBit(128, 1); // quantizer_update
            writeInt(predBe, segmentQps[i], 7); // quantizer_update_value
            predBe.writeBit(128, 0);
        }
        for (int i = segmentQps.length; i < 4; i++)
            predBe.writeBit(128, 0); // quantizer_update

        predBe.writeBit(128, 0); // loop_filter_update
        predBe.writeBit(128, 0); // loop_filter_update
        predBe.writeBit(128, 0); // loop_filter_update
        predBe.writeBit(128, 0); // loop_filter_update

        for (int i = 0; i < 3; i++) {
            predBe.writeBit(128, 1); // segment_prob_update
            writeInt(predBe, probs[i], 8);
        }
    }

    private void writeSegmetId(VPxBooleanEncoder boolEnc, int id, int[] probs) {
        int bit1 = (id >> 1) & 1;
        boolEnc.writeBit(probs[0], bit1);
        boolEnc.writeBit(probs[1 + bit1], id & 1);
    }

    public void writeYModeKey(int mode, VPxBooleanEncoder enc) {
        if (mode != B_PRED) {
            enc.writeBit(145, 1);
            if (mode == DC_PRED || mode == V_PRED) {
                enc.writeBit(156, 0);
                enc.writeBit(163, mode - DC_PRED);
            } else {
                enc.writeBit(156, 1);
                enc.writeBit(128, mode - H_PRED);
            }
        } else
            enc.writeBit(145, 0);
    }

    public void writeYMode(int mode, VPxBooleanEncoder enc) {
        if (mode != DC_PRED) {
            enc.writeBit(112, 1);
            if (mode == V_PRED || mode == H_PRED) {
                enc.writeBit(86, 0);
                enc.writeBit(140, mode - V_PRED);
            } else {
                enc.writeBit(86, 1);
                enc.writeBit(37, mode == TM_PRED ? 0 : 1);
            }
        } else
            enc.writeBit(112, 0);
    }

    public void writeBModeKey(int mode, int blkAbsX, int blkOffY, VPxBooleanEncoder enc) {
        int[] p = VPXConst.kf_bmode_prob[bPredTop[blkAbsX]][bPredLeft[blkOffY]];
        writeBModeInt(mode, enc, p);
    }

    public void writeBMode(int mode, VPxBooleanEncoder enc) {
        writeBModeInt(mode, enc, VPXConst.bmode_prob);
    }

    private void writeBModeInt(int mode, VPxBooleanEncoder enc, int[] p) {
        // System.err.println();
        if (mode != B_DC_PRED) {
            enc.writeBit(p[0], 1);
            if (mode != B_TM_PRED) {
                enc.writeBit(p[1], 1);
                if (mode != B_VE_PRED) {
                    enc.writeBit(p[2], 1);
                    if (mode == B_HE_PRED || mode == B_RD_PRED || mode == B_VR_PRED) {
                        enc.writeBit(p[3], 0);
                        if (mode == B_HE_PRED)
                            enc.writeBit(p[4], 0);
                        else {
                            enc.writeBit(p[4], 1);
                            enc.writeBit(p[5], mode == B_RD_PRED ? 0 : 1);
                        }
                    } else {
                        enc.writeBit(p[3], 1);
                        if (mode == B_LD_PRED)
                            enc.writeBit(p[6], 0);
                        else {
                            enc.writeBit(p[6], 1);
                            if (mode == B_VL_PRED)
                                enc.writeBit(p[7], 0);
                            else {
                                enc.writeBit(p[7], 1);
                                enc.writeBit(p[8], mode == B_HD_PRED ? 0 : 1);
                            }
                        }
                    }
                } else
                    enc.writeBit(p[2], 0);
            } else
                enc.writeBit(p[1], 0);
        } else
            enc.writeBit(p[0], 0);
    }

    public void writeUvModeKey(int mode, VPxBooleanEncoder enc) {
        if (mode != DC_PRED) {
            enc.writeBit(142, 1);
            if (mode != V_PRED) {
                enc.writeBit(114, 1);
                enc.writeBit(183, mode - H_PRED);
            } else
                enc.writeBit(114, 0);
        } else
            enc.writeBit(142, 0);
    }

    public void writeUvMode(int mode, VPxBooleanEncoder enc) {
        if (mode != DC_PRED) {
            enc.writeBit(162, 1);
            if (mode != V_PRED) {
                enc.writeBit(101, 1);
                enc.writeBit(204, mode - H_PRED);
            } else
                enc.writeBit(101, 0);
        } else
            enc.writeBit(162, 0);
    }

    @Override
    public void macroblockInter(int mbAddr, int segId, int[] lumaDC, int[][] lumaAC, int[][][] chroma, int thisMv) {
        // System.err.println("\n========================= MB INTER BEGIN ========================\n");
        int mbX = mbAddr % mbWidth, mbY = mbAddr / mbWidth;
        if (mbX == 0) {
            leftMv = 0;
            topLeftMvMode = leftMvMode = -1;
            bitstream.resetWHTPred();
        }
        writeSegmetId(predEnc, segId, segmentProbs);
        predEnc.writeBit(128, 1); // prob intra

        predEnc.writeBit(128, 0); // last frame to use - prob golden

        // System.err.println("MV16x16: " + mvX(thisMv) + "," + mvY(thisMv));

        int mvX = mvX(thisMv), mvY = mvY(thisMv);
        int[] mv = new int[4], cnt = new int[4], probs = new int[4];

        int fromX = -(mbX << 6) - 64, fromY = -(mbY << 6) - 64, toX = ((mbWidth - mbX) << 6) + 64, toY = ((mbHeight - mbY) << 6) + 64;
        predictMv(topMvMode[mbX], leftMvMode, topLeftMvMode, topMv[mbX], leftMv, topLeftMv, mv, cnt, fromX, toX, fromY,
                toY);
        getMVModeProbs(probs, cnt);

        int mvMode = thisMv == 0 ? MV_ZERO : (thisMv == mv[1] ? MV_NEAREST : (thisMv == mv[2] ? MV_NEAR : MV_NEW));
        writeMvMode(mvMode, probs, predEnc);

        if (mvMode == MV_NEW) {
            writeMvComponent(mvY - mvY(mv[0]), 0, predEnc);
            writeMvComponent(mvX - mvX(mv[0]), 1, predEnc);
        }

        // System.err.println("\n========================= MB INTER RESIDUAL ========================\n");

        lumaResidualWHT(lumaDC, lumaAC, mbX);
        chromaResidual(chroma, mbX);

        topLeftMv = topMv[mbX];
        topMv[mbX] = leftMv = thisMv;
        topLeftMvMode = topMvMode[mbX];
        topMvMode[mbX] = leftMvMode = mvMode;
        Arrays.fill(bMvLeft, thisMv);
        Arrays.fill(bMvTop, mbX << 2, (mbX << 2) + 4, thisMv);
        // System.err.println("\n========================= MB INTER END ========================\n");
    }

    public static int mvY(int mv) {
        return (short) (mv >>> 16);
    }

    public static int mvX(int mv) {
        return (short) (mv & 0xffff);
    }

    public static void predictMv(int tMode, int lMode, int tlMode, int tMv, int lMv, int tlMv, int[] mv, int[] cnt,
            int fX, int tX, int fY, int tY) {
        int CNT_ZERO = 0, CNT_NEAREST = 1, CNT_NEAR = 2, CNT_SPLITMV = 3;

        mv[0] = mv[1] = mv[2] = 0;
        cnt[0] = cnt[1] = cnt[2] = cnt[3] = 0;
        int idx = 0;

        if (tMode != -1) {
            if (tMv != 0)
                mv[++idx] = tMv;
            cnt[idx] += 2;
        }
        if (lMode != -1) {
            if (lMv != 0) {
                if (lMv != mv[idx])
                    mv[++idx] = lMv;
                cnt[idx] += 2;
            } else
                cnt[CNT_ZERO] += 2;
        }
        if (tlMode != -1) {
            if (tlMv != 0) {
                if (tlMv != mv[idx])
                    mv[++idx] = tlMv;
                cnt[idx] += 1;
            } else
                cnt[CNT_ZERO] += 1;
        }
        /* If we have three distinct MVs ... */
        if (cnt[CNT_SPLITMV] != 0) {
            /* See if above-left MV can be merged with NEAREST */
            if (mv[idx] == mv[CNT_NEAREST])
                cnt[CNT_NEAREST] += 1;
        }
        cnt[CNT_SPLITMV] = ((tMode == MV_SPLIT ? 1 : 0) + (lMode == MV_SPLIT ? 1 : 0)) * 2
                + (tlMode == MV_SPLIT ? 1 : 0);
        if (cnt[CNT_NEAR] > cnt[CNT_NEAREST]) {
            swap(cnt, CNT_NEAR, CNT_NEAREST);
            swap(mv, CNT_NEAR, CNT_NEAREST);
        }
        if (cnt[CNT_NEAREST] >= cnt[CNT_ZERO])
            mv[CNT_ZERO] = mv[CNT_NEAREST];

        mv[0] = clampMv(mv[0], fX, tX, fY, tY);
        mv[1] = clampMv(mv[1], fX, tX, fY, tY);
        mv[2] = clampMv(mv[2], fX, tX, fY, tY);
    }

    private static void swap(int[] arr, int i, int j) {
        int tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }

    private static int clampMv(int mv, int fromX, int toX, int fromY, int toY) {
        return mv(MathUtil.clip(mvX(mv), fromX, toX), MathUtil.clip(mvY(mv), fromY, toY));
    }

    public static int mv(int mvX, int mvY) {
        return (mvY << 16) | (mvX & 0xffff);
    }

    @Override
    public void macroblockInterSplit(int mbAddr, int segId, int[][] luma, int[][][] chroma, int splitMode, int[] mvs) {
        // System.err.println("\n========================= MB SPLIT BEGIN ========================\n");
        int mbX = mbAddr % mbWidth, mbY = mbAddr / mbWidth;
        if (mbX == 0) {
            leftMv = 0;
            topLeftMvMode = leftMvMode = -1;
            bitstream.resetWHTPred();
            Arrays.fill(bMvLeft, 0);
        }
        writeSegmetId(predEnc, segId, segmentProbs);
        predEnc.writeBit(128, 1); // prob intra

        predEnc.writeBit(128, 0); // last frame to use - prob golden
        int[] mv = new int[4], cnt = new int[4], probs = new int[4];

        int fromX = -(mbX << 6) - 64, fromY = -(mbY << 6) - 64, toX = ((mbWidth - mbX) << 6) + 64, toY = ((mbHeight - mbY) << 6) + 64;
        predictMv(topMvMode[mbX], leftMvMode, topLeftMvMode, topMv[mbX], leftMv, topLeftMv, mv, cnt, (short) fromX,
                (short) toX, (short) fromY, (short) toY);
        getMVModeProbs(probs, cnt);

        writeMvMode(MV_SPLIT, probs, predEnc);
        writeMvSplitMode(splitMode, predEnc);
        int[] mvs16 = new int[16];
        int blk4x4BaseX = mbX << 2;
        for (int blk = 0; blk < mvs.length; blk++) {
            int[] set = VPXConst.subblock_sets[splitMode][blk];
            int blk4x4X = set[0] & 3, blk4x4Y = set[0] >> 2, blk4x4AbsX = blk4x4BaseX + blk4x4X;

            int lmv = (blk4x4X == 0 ? bMvLeft[blk4x4Y] : mvs16[set[0] - 1]), tmv = (blk4x4Y == 0 ? bMvTop[blk4x4AbsX]
                    : mvs16[set[0] - 4]);

            int subMvMode = mvs[blk] == 0 ? MVSUB_ZERO : (mvs[blk] == lmv ? MVSUB_LEFT : (mvs[blk] == tmv ? MVSUB_TOP
                    : MVSUB_NEW));
//            System.err.println("SUBMV: " + mvX(mvs[blk]) + ", " + mvY(mvs[blk]) + "\n");
            writeSubMode(lmv, tmv, subMvMode, predEnc);
            if (subMvMode == MVSUB_NEW) {
                writeMvComponent(mvY(mvs[blk]) - mvY(mv[0]), 0, predEnc);
                writeMvComponent(mvX(mvs[blk]) - mvX(mv[0]), 1, predEnc);
            }

            for (int i = 0; i < set.length; i++)
                mvs16[set[i]] = mvs[blk];

            fromX += VPXConst.subblock_width[splitMode] << 2;
            toX -= VPXConst.subblock_width[splitMode] << 2;
            fromY += VPXConst.subblock_height[splitMode] << 2;
            toY -= VPXConst.subblock_height[splitMode] << 2;
        }

        bMvLeft[0] = mvs16[3];
        bMvLeft[1] = mvs16[7];
        bMvLeft[2] = mvs16[11];
        bMvLeft[3] = mvs16[15];
        bMvTop[blk4x4BaseX] = mvs16[12];
        bMvTop[blk4x4BaseX + 1] = mvs16[13];
        bMvTop[blk4x4BaseX + 2] = mvs16[14];
        bMvTop[blk4x4BaseX + 3] = mvs16[15];

        // System.err.println("\n========================= MB RESIDUAL BEGIN ========================\n");
        lumaResidual(luma, mbX);
        chromaResidual(chroma, mbX);

        topLeftMvMode = topMvMode[mbX];
        topMvMode[mbX] = leftMvMode = MV_SPLIT;
        topLeftMv = topMv[mbX];
        leftMv = topMv[mbX] = mvs[mvs.length - 1];
        // System.err.println("\n========================= MB INTER END ========================\n");
    }

    private void writeSubMode(int lmv, int tmv, int subMvMode, VPxBooleanEncoder boolEnc) {
        int ctx = lmv == 0 && tmv == 0 ? 4 : (lmv == tmv ? 3 : (tmv == 0 ? 2 : (lmv == 0 ? 1 : 0)));
        int[] probs = VPXConst.mv_submode_prob[ctx];
        if (subMvMode == MVSUB_LEFT)
            boolEnc.writeBit(probs[0], 0);
        else {
            boolEnc.writeBit(probs[0], 1);
            if (subMvMode == MVSUB_TOP)
                boolEnc.writeBit(probs[1], 0);
            else {
                boolEnc.writeBit(probs[1], 1);
                boolEnc.writeBit(probs[2], subMvMode - MVSUB_ZERO);
            }
        }
    }

    private void writeMvSplitMode(int splitMode, VPxBooleanEncoder boolEnc) {
        if (splitMode == MV_SPLT_16)
            boolEnc.writeBit(110, 0);
        else {
            boolEnc.writeBit(110, 1);
            if (splitMode == MV_SPLT_4)
                boolEnc.writeBit(111, 0);
            else {
                boolEnc.writeBit(111, 1);
                boolEnc.writeBit(150, splitMode - MV_SPLT_2_HOR);
            }
        }
    }

    private int[] B_MODE_MAP = new int[] { B_DC_PRED, B_VE_PRED, B_HE_PRED, B_TM_PRED };
    private int[] segmentProbs;

    @Override
    public void macroblockIntra(int mbAddr, int segId, int[] lumaDC, int[][] lumaAC, int[][][] chroma, int lumaMode,
            int chromaMode) {

        int mbX = mbAddr % mbWidth, mbY = mbAddr / mbWidth;
        if (mbX == 0) {
            bitstream.resetWHTPred();
        }
        // //System.err.println("========================================(" +
        // mbX
        // + "," + mbY
        // + ")========================================");

        writeSegmetId(predEnc, segId, segmentProbs);

        if (!keyFrame) {
            predEnc.writeBit(128, 0); // prob intra
            writeYMode(lumaMode, predEnc);
            writeUvMode(chromaMode, predEnc);
        } else {
            writeYModeKey(lumaMode, predEnc);
            writeUvModeKey(chromaMode, predEnc);
        }

        int blkOffX = mbX << 2;
        bPredLeft[0] = bPredLeft[1] = bPredLeft[2] = bPredLeft[3] = bPredTop[blkOffX] = bPredTop[blkOffX + 1] = bPredTop[blkOffX + 2] = bPredTop[blkOffX + 3] = B_MODE_MAP[lumaMode];

        lumaResidualWHT(lumaDC, lumaAC, mbX);
        chromaResidual(chroma, mbX);

        topLeftMv = topMv[mbX];
        topLeftMvMode = topMvMode[mbX];
        topMvMode[mbX] = leftMvMode = -1;
        Arrays.fill(bMvLeft, 0);
        Arrays.fill(bMvTop, mbX << 2, (mbX << 2) + 4, 0);
    }

    private void lumaResidualWHT(int[] lumaDC, int[][] lumaAC, int mbX) {
        bitstream.encodeCoeffsWHT(resEnc, zigzag(lumaDC, tmp), mbX);
        for (int i = 0; i < 16; i++) {
            if (lumaAC[i] == null)
                bitstream.encodeCoeffsEmpty(resEnc, mbX, i & 3, i >> 2, VPXConst.BLK_TYPE_DCT15, 1, 0);
            else
                bitstream.encodeCoeffsDCT15(resEnc, zigzag(lumaAC[i], tmp), mbX, i & 3, i >> 2);
            // System.err.println();
        }
    }

    private void chromaResidual(int[][][] chroma, int mbX) {
        for (int c = 0; c < 2; c++) {
            for (int i = 0; i < 4; i++) {
                if (chroma[c][i] == null)
                    bitstream.encodeCoeffsEmpty(resEnc, mbX, i & 1, i >> 1, VPXConst.BLK_TYPE_UV, 0, c + 1);
                else
                    bitstream.encodeCoeffsDCTUV(resEnc, zigzag(chroma[c][i], tmp), c + 1, mbX, i & 1, i >> 1);
                // System.err.println("\n");
            }
        }
    }

    @Override
    public void macroblockIntraBPred(int mbAddr, int segId, int[][] luma, int[][][] chroma, int[] lumaModes,
            int chromaMode) {

        int mbX = mbAddr % mbWidth, mbY = mbAddr / mbWidth;
        // //System.err.println("========================================(" +
        // mbX
        // + "," + mbY + ")========================================");

        if (mbX == 0) {
            Arrays.fill(bPredLeft, 0);
            bitstream.resetWHTPred();
        }

        writeSegmetId(predEnc, segId, segmentProbs);

        if (!keyFrame) {
            predEnc.writeBit(128, 0); // prob intra
            writeYMode(B_PRED, predEnc);
            for (int i = 0; i < 16; i++) {
                int blkOffY = i >> 2;
                int blkAbsX = (mbX << 2) + (i & 3);
                writeBMode(lumaModes[i], predEnc);
                bPredLeft[blkOffY] = bPredTop[blkAbsX] = lumaModes[i];
            }

            // System.err.println();
            writeUvMode(chromaMode, predEnc);
            // System.err.println();
        } else {
            writeYModeKey(B_PRED, predEnc);
            for (int i = 0, sh = 0; i < 16; i++, sh += 4) {
                int blkOffY = i >> 2;
                int blkAbsX = (mbX << 2) + (i & 3);
                writeBModeKey(lumaModes[i], blkAbsX, blkOffY, predEnc);
                bPredLeft[blkOffY] = bPredTop[blkAbsX] = lumaModes[i];
            }

            // System.err.println();
            writeUvModeKey(chromaMode, predEnc);
            // System.err.println();
        }

        lumaResidual(luma, mbX);
        chromaResidual(chroma, mbX);

        topLeftMv = topMv[mbX];
        topLeftMvMode = topMvMode[mbX];
        topMvMode[mbX] = leftMvMode = -1;
        Arrays.fill(bMvLeft, 0);
        Arrays.fill(bMvTop, mbX << 2, (mbX << 2) + 4, 0);
    }

    private void lumaResidual(int[][] luma, int mbX) {
        long subMbMode = 0;
        for (int i = 0, sh = 0; i < 16; i++, sh += 4) {
            if (luma[i] == null)
                bitstream.encodeCoeffsEmpty(resEnc, mbX, i & 3, i >> 2, VPXConst.BLK_TYPE_DCT16, 0, 0);
            else
                bitstream.encodeCoeffsDCT16(resEnc, zigzag(luma[i], tmp), mbX, i & 3, i >> 2);
            // System.err.println("\n");
        }
    }
}