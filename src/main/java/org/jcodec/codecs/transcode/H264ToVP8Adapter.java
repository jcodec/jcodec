package org.jcodec.codecs.transcode;

import static org.jcodec.codecs.h264.decode.SliceDecoder.calcQpChroma;
import static org.jcodec.codecs.transcode.H264ToVP8PixUtils.collectPred4x4;
import static org.jcodec.codecs.transcode.H264ToVP8PixUtils.map16x16;
import static org.jcodec.codecs.transcode.H264ToVP8PixUtils.map4x4;
import static org.jcodec.codecs.transcode.H264ToVP8PixUtils.map8x8;
import static org.jcodec.codecs.transcode.H264ToVP8PixUtils.mapSwapPred4x4;
import static org.jcodec.codecs.transcode.H264ToVP8PixUtils.renderChromaH264;
import static org.jcodec.codecs.transcode.H264ToVP8PixUtils.renderH264Luma;
import static org.jcodec.codecs.vpx.VPXConst.B_DC_PRED;
import static org.jcodec.codecs.vpx.VPXConst.B_HD_PRED;
import static org.jcodec.codecs.vpx.VPXConst.B_HE_PRED;
import static org.jcodec.codecs.vpx.VPXConst.B_HU_PRED;
import static org.jcodec.codecs.vpx.VPXConst.B_LD_PRED;
import static org.jcodec.codecs.vpx.VPXConst.B_RD_PRED;
import static org.jcodec.codecs.vpx.VPXConst.B_VE_PRED;
import static org.jcodec.codecs.vpx.VPXConst.B_VL_PRED;
import static org.jcodec.codecs.vpx.VPXConst.B_VR_PRED;
import static org.jcodec.codecs.vpx.VPXConst.DC_PRED;
import static org.jcodec.codecs.vpx.VPXConst.H_PRED;
import static org.jcodec.codecs.vpx.VPXConst.MV_NEAREST;
import static org.jcodec.codecs.vpx.VPXConst.MV_SPLIT;
import static org.jcodec.codecs.vpx.VPXConst.TM_PRED;
import static org.jcodec.codecs.vpx.VPXConst.V_PRED;
import static org.jcodec.codecs.vpx.tweak.VP8Serializer.mv;
import static org.jcodec.codecs.vpx.tweak.VP8Serializer.mvX;
import static org.jcodec.codecs.vpx.tweak.VP8Serializer.mvY;

import java.util.Arrays;

import org.jcodec.codecs.h264.H264Const;
import org.jcodec.codecs.h264.H264Const.PartPred;
import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.decode.BlockInterpolator;
import org.jcodec.codecs.h264.decode.ChromaPredictionBuilder;
import org.jcodec.codecs.h264.decode.CoeffTransformer;
import org.jcodec.codecs.h264.decode.Intra16x16PredictionBuilder;
import org.jcodec.codecs.h264.decode.Intra4x4PredictionBuilder;
import org.jcodec.codecs.h264.decode.aso.MapManager;
import org.jcodec.codecs.h264.decode.aso.Mapper;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.tweak.H264Handler;
import org.jcodec.codecs.vpx.VP8Encoder;
import org.jcodec.codecs.vpx.VP8InterPrediction;
import org.jcodec.codecs.vpx.VPXConst;
import org.jcodec.codecs.vpx.VPXDCT;
import org.jcodec.codecs.vpx.VPXPred;
import org.jcodec.codecs.vpx.VPXQuantizer;
import org.jcodec.codecs.vpx.tweak.VP8Handler;
import org.jcodec.codecs.vpx.tweak.VP8Serializer;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.tools.MathUtil;

/**
 * Converts H.264 symbols to VP8 symbols turning H.264 into VP8 on-the-fly
 * 
 * @author The JCodec Project
 * 
 */
public class H264ToVP8Adapter implements H264Handler {
    private VP8Handler vp8;
    private VPXQuantizer quantizer;
    private int[] chromaQpOffset;
    private int[][] leftRowVP8;
    private int[][] topLineVP8;
    private int mbWidth;
    private int[][] leftRowH264;
    private int[][] topLineH264;
    private int[][] topLeftVP8;
    private int[][] topLeftH264;
    private Picture[] refsH264;
    private Picture refVP8;
    private Picture pictureH264;
    private Picture pictureVP8;
    private int mbHeight;

    private int[] topMvMode;
    private int leftMvMode;
    private int topLeftMvMode;
    private int[] topMv;
    private int leftMv;
    private int topLeftMv;

    private boolean[] topMbIntra;
    private boolean leftMbIntra;
    
    private int[] outVP8 = new int[256];
    private int[] predH264 = new int[256];
    private int[] predVP8 = new int[289];
    private int[] copyDC = new int[16];
    private int[] copyAC = new int[16];
    private int[][] lumaACNew = new int[16][];
    private int[] lumaDCNew = new int[16];
    private int[] nCoeff = new int[16];
    private int[] h264PredChroma = new int[64];
    private int[] vp8PredChroma = new int[81];
    private int[] outVP8Chroma = new int[64];
    private int[] outH264Chroma = new int[64];
    private int[] mv = new int[4], cnt = new int[4];

    public static final int[] vp8LumaModeMap = { V_PRED, H_PRED, DC_PRED, TM_PRED };
    public static final int[] vp8ChromaModeMap = { DC_PRED, H_PRED, V_PRED, TM_PRED };
    public static final int[] vp8Intra4x4PredMap = { B_VE_PRED, B_HE_PRED, B_DC_PRED, B_LD_PRED, B_RD_PRED, B_VR_PRED,
            B_HD_PRED, B_VL_PRED, B_HU_PRED };
    private SliceAdapter sa;

    private int mbLast;
    private int[][] topMb264;
    private int[] leftMb264;
    private InplaceDeblocker deblocker;
    private int[] segmentQps;

    public H264ToVP8Adapter(VP8Handler vp8, int chr1QpOffset, int chr2QpOffset, int mbWidth, int mbHeight,
            Picture[] refsH264, Picture refVP8) {
        this.vp8 = vp8;
        quantizer = new VPXQuantizer();
        chromaQpOffset = new int[] { chr1QpOffset, chr2QpOffset };
        topLeftVP8 = new int[][] { new int[16], new int[8], new int[8] };
        leftRowVP8 = new int[][] { new int[16], new int[8], new int[8] };
        topLineVP8 = new int[][] { new int[mbWidth << 4], new int[mbWidth << 3], new int[mbWidth << 3] };
        resetPredictVP8(topLineVP8, 127);
        resetPredictVP8(leftRowVP8, 129);
        resetPredictVP8(topLeftVP8, 129);

        leftRowH264 = new int[][] { new int[16], new int[8], new int[8] };
        topLeftH264 = new int[][] { new int[16], new int[8], new int[8] };
        topLineH264 = new int[][] { new int[mbWidth << 4], new int[mbWidth << 3], new int[mbWidth << 3] };
        this.mbWidth = mbWidth;
        this.mbHeight = mbHeight;
        this.refsH264 = refsH264;
        this.refVP8 = refVP8;

        this.pictureH264 = Picture.create(mbWidth << 4, mbHeight << 4, ColorSpace.YUV420);
        this.pictureVP8 = Picture.create(mbWidth << 4, mbHeight << 4, ColorSpace.YUV420);

        topMvMode = new int[mbWidth];
        topLeftMvMode = leftMvMode = -1;
        Arrays.fill(topMvMode, -1);

        topMv = new int[mbWidth];

        mbLast = mbWidth * mbHeight - 1;
        topMb264 = new int[mbWidth][];
        deblocker = new InplaceDeblocker(mbWidth);

        topMbIntra = new boolean[mbWidth];
    }

    private void resetPredictVP8(int[][] predict, int val) {
        for (int c = 0; c < 3; c++)
            Arrays.fill(predict[c], val);
    }

    public Picture getRenderedH264() {
        return pictureH264;
    }

    public Picture getRenderedVP8() {
        return pictureVP8;
    }

    @Override
    public void slice(SliceHeader sh) {
        if (sa == null) {
            int h264Qp = sh.pps.pic_init_qp_minus26 + 26 + sh.slice_qp_delta;
            int s0qp = toVp8Qp(h264Qp);// H264ToVP8QpMap[h264Qp];
            segmentQps = new int[] { s0qp, MathUtil.clip1_127(s0qp - 5), MathUtil.clip1_127(s0qp + 5),
                    MathUtil.clip1_127(s0qp - 10) };

            vp8.beginFrame(segmentQps);
        }
        sa = new SliceAdapter(sh);
    }

    private int toVp8Qp(int h264Qp) {
//        return MathUtil.clip(h264Qp/* >> 1 */, 1, 127);
        return MathUtil.clip1_127(h264Qp);
    }

    @Override
    public void macroblockINxN(int mbIdx, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC,
            int[] lumaModes, int chromaMode) {
        sa.macroblockINxN(mbIdx, qp, lumaResidual, chromaDC, chromaAC, lumaModes, chromaMode);
    }

    @Override
    public void macroblockI16x16(int mbIdx, int qp, int[] lumaDC, int[][] lumaAC, int lumaModeH264, int chromaMode,
            int[][] chromaDC, int[][][] chromaAC) {
        sa.macroblockI16x16(mbIdx, qp, lumaDC, lumaAC, lumaModeH264, chromaMode, chromaDC, chromaAC);
    }

    @Override
    public void mblockPB16x16(int mbIdx, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC,
            int[][] mvs, PartPred pred) {
        sa.mblockPB16x16(mbIdx, qp, lumaResidual, chromaDC, chromaAC, mvs, pred);
    }

    @Override
    public void mblockPB16x8(int mbIdx, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC,
            int[][] mvs, PartPred p0, PartPred p1) {
        sa.mblockPB16x8(mbIdx, qp, lumaResidual, chromaDC, chromaAC, mvs, p0, p1);
    }

    @Override
    public void mblockPB8x16(int mbIdx, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC,
            int[][] mvs, PartPred p0, PartPred p1) {
        sa.mblockPB8x16(mbIdx, qp, lumaResidual, chromaDC, chromaAC, mvs, p0, p1);
    }

    @Override
    public void mblockPB8x8(int mbIdx, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC,
            int[][][] mvs, int[] subMbModes, PartPred l0, PartPred l02, PartPred l03, PartPred l04) {
        sa.mblockPB8x8(mbIdx, qp, lumaResidual, chromaDC, chromaAC, mvs, subMbModes, l0, l02, l03, l04);
    }

    @Override
    public void mblockBDirect(int mbIdx, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC) {
        sa.mblockBDirect(mbIdx, qp, lumaResidual, chromaDC, chromaAC);
    }

    @Override
    public void mblockPBSkip(int mbIdx, int mvX, int mvY) {
        sa.mblockPBSkip(mbIdx, mvX, mvY);
    }

    private void deblock264(SliceHeader sh, int mbAddr, int[] out264, int qp, int[] nCoeff, int[][] mvs, boolean intra) {
        int mbX = mbAddr % mbWidth;

        deblocker.deblock(sh, mbAddr, leftMb264, topMb264[mbX], out264, qp, nCoeff, mvs, refsH264, intra);

        if (topMb264[mbX] != null)
            map16x16(pictureH264.getPlaneData(0), mbWidth, mbAddr - mbWidth, topMb264[mbX]);

        topMb264[mbX] = leftMb264 = out264;
    }

    private void finish264() {
        for (int i = 0, base = (mbHeight - 1) * mbWidth; i < mbWidth; i++, base++)
            map16x16(pictureH264.getPlaneData(0), mbWidth, base, topMb264[i]);
    }

    private class SliceAdapter {

        private Mapper mapper;
        private SliceHeader sh;

        public SliceAdapter(SliceHeader sh) {
            mapper = new MapManager(sh.sps, sh.pps).getMapper(sh);
            this.sh = sh;
        }

        public void macroblockINxN(int mbIdx, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC,
                int[] lumaModes, int chromaMode) {
            int seg = chooseSeg(qp);

            int mbAddr = mapper.getAddress(mbIdx);
            int mbX = mbAddr % mbWidth, mbY = mbAddr / mbWidth;

            boolean topAvb = mapper.topAvailable(mbIdx) && (topMbIntra[mbX] || !sh.pps.constrained_intra_pred_flag);
            boolean leftAvb = mapper.leftAvailable(mbIdx) && (leftMbIntra || !sh.pps.constrained_intra_pred_flag);

            int[][] residualNew = new int[16][];
            int[] lumaModesNew = new int[16];
            int[] predH264 = new int[16], predVP8 = new int[16];

            if (mbX == 0) {
                resetPredictVP8(leftRowVP8, 129);
                resetPredictVP8(topLeftVP8, 129);
                topLeftVP8[0][0] = 127;

                leftMb264 = null;
            }
            int[] out264 = new int[256], outVP8 = new int[256];
            int t15 = topLineVP8[0][(mbX << 4) + 15];
            int[] nCoeff = new int[16];
            for (int i = 0; i < 16; i++) {
                int bi = H264Const.BLK_INV_MAP[i];
                int lumaMode = lumaModes[bi];
                int[] residual = lumaResidual[bi];

                int vp8LumaMode = vp8Intra4x4PredMap[lumaMode];
                lumaModesNew[i] = vp8LumaMode;

                int blkOffLeft = (i & 3) << 2, blkOffTop = i & ~3, blkAbsX = (mbX << 4) + blkOffLeft;

                if (residual != null) {
                    nCoeff[i] = nCoeff(residual);
                    CoeffTransformer.dequantizeAC(residual, qp);
                    CoeffTransformer.idct4x4(residual);
                } else {
                    nCoeff[i] = 0;
                    residual = new int[16];
                }
                boolean topRightAvailable = topAvb && mbX < mbWidth - 1;
                boolean trAvailable = ((bi == 0 || bi == 1 || bi == 4) && topAvb) || (bi == 5 && topRightAvailable)
                        || bi == 2 || bi == 6 || bi == 8 || bi == 9 || bi == 10 || bi == 12 || bi == 14;

                Intra4x4PredictionBuilder.predictWithMode(lumaMode, leftAvb || blkOffLeft != 0, topAvb
                        || blkOffTop != 0, trAvailable, leftRowH264[0], topLineH264[0], topLeftH264[0], blkAbsX,
                        blkOffTop, predH264);

                VPXPred.vp8BPred(vp8LumaMode, leftRowVP8[0], topLineVP8[0], t15, topLeftVP8[0], blkAbsX, blkOffTop,
                        predVP8);

                collectPred4x4(residual, predH264, leftRowH264[0], topLineH264[0], topLeftH264[0], blkAbsX, blkOffTop);
                
                mapSwapPred4x4(out264, residual, predH264, predVP8, blkOffLeft, blkOffTop);

                VPXDCT.fdct4x4(residual);
                quantizer.quantizeY(residual, segmentQps[seg]);

                int[] ac = Arrays.copyOf(residual, 16);
                quantizer.dequantizeY(ac, segmentQps[seg]);
                VPXDCT.idct4x4(ac);
                collectPred4x4(ac, predVP8, leftRowVP8[0], topLineVP8[0], topLeftVP8[0], blkAbsX, blkOffTop);
                map4x4(outVP8, predVP8, ac, blkOffLeft, blkOffTop);

                residualNew[i] = residual;
            }
            deblock264(sh, mbAddr, out264, qp, nCoeff, null, true);
            map16x16(pictureVP8.getPlaneData(0), mbWidth, mbAddr, outVP8);

            doChromaIntra(mbAddr, qp, chromaDC, chromaAC, leftAvb, topAvb, chromaMode, seg);

            topLeftMv = topMv[mbX];
            topLeftMvMode = topMvMode[mbX];
            topMvMode[mbX] = leftMvMode = -1;
            topMbIntra[mbX] = leftMbIntra = true;

            vp8.macroblockIntraBPred(mbAddr, seg, residualNew, chromaAC, lumaModesNew, vp8ChromaModeMap[chromaMode]);

            if (mbAddr == mbLast)
                finish264();
        }

        private int chooseSeg(int h264Qp) {
            int vp8Qp = toVp8Qp(h264Qp);
            return vp8Qp > segmentQps[0] + 2 ? 2 : (vp8Qp >= segmentQps[0] - 2 ? 0 : (vp8Qp >= segmentQps[1] - 2 ? 1
                    : 3));
        }

        private int nCoeff(int[] residual) {
            for (int i = 15; i >= 0; i--) {
                if (residual[i] != 0)
                    return i + 1;
            }
            return 0;
        }

        public void macroblockI16x16(int mbIdx, int qp, int[] lumaDC, int[][] lumaAC, int lumaModeH264, int chromaMode,
                int[][] chromaDC, int[][][] chromaAC) {
            int seg = chooseSeg(qp);
            int mbAddr = mapper.getAddress(mbIdx);
            int mbX = mbAddr % mbWidth, mbY = mbAddr / mbWidth;

            boolean topAvb = mapper.topAvailable(mbIdx) && (topMbIntra[mbX] || !sh.pps.constrained_intra_pred_flag);
            boolean leftAvb = mapper.leftAvailable(mbIdx) && (leftMbIntra || !sh.pps.constrained_intra_pred_flag);

            int lumaModeVP8 = vp8LumaModeMap[lumaModeH264];

            if (mbX == 0) {
                resetPredictVP8(leftRowVP8, 129);
                resetPredictVP8(topLeftVP8, 129);
                topLeftVP8[0][0] = 127;
                leftMb264 = null;
            }

            int[] predH264 = new int[256], predVP8 = new int[256];
            VPXPred.pred816(lumaModeVP8, mbX << 4, mbY << 4, leftRowVP8[0], topLineVP8[0], topLeftVP8[0], predVP8, 4);
            Intra16x16PredictionBuilder.predictWithMode(lumaModeH264, predH264, leftAvb, topAvb, leftRowH264[0],
                    topLineH264[0], topLeftH264[0], mbX << 4);

            CoeffTransformer.invDC4x4(lumaDC);
            CoeffTransformer.dequantizeDC4x4(lumaDC, qp);

            int[] outVP8 = new int[256], outH264 = new int[256];

            int[][] lumaACNew = new int[16][];
            int[] nCoeff = new int[16];
            for (int i = 0; i < 16; i++) {
                int invIdx = H264Const.BLK_INV_MAP[i];
                int blkOffLeft = invIdx & 3;
                int blkOffTop = invIdx >> 2;

                if (lumaAC[i] == null) {
                    nCoeff[invIdx] = 0;
                    lumaAC[i] = new int[16];
                } else {
                    nCoeff[invIdx] = nCoeff(lumaAC[i]);
                    CoeffTransformer.dequantizeAC(lumaAC[i], qp);
                }

                lumaAC[i][0] = lumaDC[invIdx];
                CoeffTransformer.idct4x4(lumaAC[i]);
                renderH264Luma(outH264, lumaAC[i], predH264, predVP8, blkOffLeft, blkOffTop, 16);
                VPXDCT.fdct4x4(lumaAC[i]);
                lumaDC[invIdx] = lumaAC[i][0];
                quantizer.quantizeY(lumaAC[i], segmentQps[seg]);
                lumaAC[i][0] = 0;
                lumaACNew[invIdx] = lumaAC[i];
            }

            topLeftH264[0][0] = topLineH264[0][(mbX << 4) + 15];
            H264Encoder.copyCol(outH264, 15, 16, topLeftH264[0], 1);
            System.arraycopy(outH264, 240, topLineH264[0], mbX << 4, 16);
            H264Encoder.copyCol(outH264, 15, 16, leftRowH264[0], 0);

            VPXDCT.walsh4x4(lumaDC);
            quantizer.quantizeY2(lumaDC, segmentQps[seg]);

            int[] copyDC = Arrays.copyOf(lumaDC, 16);
            quantizer.dequantizeY2(copyDC, segmentQps[seg]);
            VPXDCT.iwalsh4x4(copyDC);

            for (int i = 0; i < 16; i++) {
                int[] copyAC = Arrays.copyOf(lumaACNew[i], 16);
                renderVP8Luma(outVP8, copyDC[i], copyAC, predVP8, i & 3, i >> 2, 16, seg);
            }

            topLeftVP8[0][0] = topLineVP8[0][(mbX << 4) + 15];
            VP8Encoder.copyCol(outVP8, 15, 16, topLeftVP8[0], 1);
            System.arraycopy(outVP8, 240, topLineVP8[0], mbX << 4, 16);
            VP8Encoder.copyCol(outVP8, 15, 16, leftRowVP8[0], 0);

            deblock264(sh, mbAddr, outH264, qp, nCoeff, null, true);
            map16x16(pictureVP8.getPlaneData(0), mbWidth, mbAddr, outVP8);

            doChromaIntra(mbAddr, qp, chromaDC, chromaAC, leftAvb, topAvb, chromaMode, seg);

            topLeftMv = topMv[mbX];
            topLeftMvMode = topMvMode[mbX];
            topMvMode[mbX] = leftMvMode = -1;
            topMbIntra[mbX] = leftMbIntra = true;

            vp8.macroblockIntra(mbAddr, seg, lumaDC, lumaACNew, chromaAC, lumaModeVP8, vp8ChromaModeMap[chromaMode]);

            if (mbAddr == mbLast)
                finish264();
        }

        int[] ZERO = new int[4];

        private void doChromaIntra(int mbAddr, int qp, int[][] chromaDC, int[][][] chromaAC, boolean leftAvb,
                boolean topAvb, int chromaModeH264, int seg) {
            int mbX = mbAddr % mbWidth, mbY = mbAddr / mbWidth;
            int chromaModeVP8 = vp8ChromaModeMap[chromaModeH264];

            for (int c = 0; c < 2; c++) {

                int[] h264Pred = new int[64], vp8Pred = new int[64];
                VPXPred.pred816(chromaModeVP8, mbX << 3, mbY << 3, leftRowVP8[c + 1], topLineVP8[c + 1],
                        topLeftVP8[c + 1], vp8Pred, 3);
                ChromaPredictionBuilder.predictWithMode(h264Pred, chromaModeH264, mbX, leftAvb, topAvb,
                        leftRowH264[c + 1], topLineH264[c + 1], topLeftH264[c + 1]);

                doChromaResidual(qp, chromaDC, chromaAC, mbX, mbY, c, h264Pred, vp8Pred, 8, seg);
            }
        }

        private void renderChromaVP8(int[] coeffs, int vp8Qp, int[] out, int blkX, int blkY, int[] pred, int predStride) {
            quantizer.dequantizeUV(coeffs, vp8Qp);
            VPXDCT.idct4x4(coeffs);
            VP8Encoder.putBlk(out, pred, predStride, coeffs, 3, blkX << 2, blkY << 2);
        }

        private void renderVP8Luma(int[] out, int dc, int[] coeffs, int[] predVP8, int blkIndX, int blkIndY,
                int predStride, int seg) {
            quantizer.dequantizeY(coeffs, segmentQps[seg]);
            coeffs[0] = dc;
            VPXDCT.idct4x4(coeffs);
            VP8Encoder.putBlk(out, predVP8, predStride, coeffs, 4, blkIndX << 2, blkIndY << 2);
        }

        private void renderVP8LumaJust(int[] out, int[] coeffs, int[] predVP8, int blkIndX, int blkIndY,
                int predStride, int seg) {
            quantizer.dequantizeY(coeffs, segmentQps[seg]);
            VPXDCT.idct4x4(coeffs);
            VP8Encoder.putBlk(out, predVP8, predStride, coeffs, 4, blkIndX << 2, blkIndY << 2);
        }

        public void mblockPB16x16(int mbIdx, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC,
                int[][] mvs, PartPred pred) {
            int[] outH264 = new int[256];
            
            int seg = chooseSeg(qp);
            int mbAddr = mapper.getAddress(mbIdx);

            int mbX = mbAddr % mbWidth, mbY = mbAddr / mbWidth;
            int minX = -(mbX << 6) - 64, maxX = ((mbWidth - mbX) << 6) + 64, minY = -(mbY << 6) - 64, maxY = ((mbHeight - mbY) << 6) + 64;

            if (mbX == 0) {
                resetPredictVP8(leftRowVP8, 129);
                resetPredictVP8(topLeftVP8, 129);
                topLeftVP8[0][0] = 127;
                leftMb264 = null;
            }
            
            BlockInterpolator.getBlockLuma(refsH264[mvs[0][2]], predH264, 16, 0, (mbX << 6) + mvs[0][0], (mbY << 6)
                    + mvs[0][1], 16, 16);

            VP8Serializer.predictMv(topMvMode[mbX], leftMvMode, topLeftMvMode, topMv[mbX], leftMv, topLeftMv, mv, cnt,
                    minX, maxX, minY, maxY);
            int mvVp8x = MathUtil
                    .clip(mvs[0][0], Math.max(minX, -1023 + mvX(mv[0])), Math.min(maxX, 1023 + mvX(mv[0])));
            int mvVp8y = MathUtil
                    .clip(mvs[0][1], Math.max(minY, -1023 + mvY(mv[0])), Math.min(maxY, 1023 + mvY(mv[0])));

            VP8InterPrediction.getBlock(refVP8.getPlaneData(0), refVP8.getPlaneWidth(0), refVP8.getPlaneHeight(0),
                    predVP8, 0, 17, ((mbX << 6) + mvVp8x) << 1, ((mbY << 6) + mvVp8y) << 1, 16, 16);

            for (int i = 0; i < 16; i++) {
                int invIdx = H264Const.BLK_INV_MAP[i];
                int blkOffLeft = invIdx & 3;
                int blkOffTop = invIdx >> 2;

                if (lumaResidual[i] == null) {
                    nCoeff[invIdx] = 0;
                    lumaResidual[i] = new int[16];
                } else {
                    nCoeff[invIdx] = nCoeff(lumaResidual[i]);
                    CoeffTransformer.dequantizeAC(lumaResidual[i], qp);
                    CoeffTransformer.idct4x4(lumaResidual[i]);
                }

                renderH264Luma(outH264, lumaResidual[i], predH264, predVP8, blkOffLeft, blkOffTop, 17);
                VPXDCT.fdct4x4(lumaResidual[i]);
                lumaDCNew[invIdx] = lumaResidual[i][0];
                quantizer.quantizeY(lumaResidual[i], segmentQps[seg]);
                lumaResidual[i][0] = 0;
                lumaACNew[invIdx] = lumaResidual[i];
            }

            topLeftH264[0][0] = topLineH264[0][(mbX << 4) + 15];
            H264Encoder.copyCol(outH264, 15, 16, topLeftH264[0], 1);
            System.arraycopy(outH264, 240, topLineH264[0], mbX << 4, 16);
            H264Encoder.copyCol(outH264, 15, 16, leftRowH264[0], 0);

            VPXDCT.walsh4x4(lumaDCNew);
            quantizer.quantizeY2(lumaDCNew, segmentQps[seg]);

            System.arraycopy(lumaDCNew, 0, copyDC, 0, 16);
            quantizer.dequantizeY2(copyDC, segmentQps[seg]);
            VPXDCT.iwalsh4x4(copyDC);

            for (int i = 0; i < 16; i++) {
                System.arraycopy(lumaACNew[i], 1, copyAC, 1, 15);
                renderVP8Luma(outVP8, copyDC[i], copyAC, predVP8, i & 3, i >> 2, 17, seg);
            }

            topLeftVP8[0][0] = topLineVP8[0][(mbX << 4) + 15];
            VP8Encoder.copyCol(outVP8, 15, 16, topLeftVP8[0], 1);
            System.arraycopy(outVP8, 240, topLineVP8[0], mbX << 4, 16);
            VP8Encoder.copyCol(outVP8, 15, 16, leftRowVP8[0], 0);

            int[][] mvsDeblock = new int[16][];
            Arrays.fill(mvsDeblock, mvs[0]);

            deblock264(sh, mbAddr, outH264, qp, nCoeff, mvsDeblock, false);
            map16x16(pictureVP8.getPlaneData(0), mbWidth, mbAddr, outVP8);

            int vp8Mv = VP8Serializer.mv(mvVp8x, mvVp8y);

            doChromaInter16x16(mbAddr, qp, chromaDC, chromaAC, mvs[0], vp8Mv, seg);

            topLeftMv = topMv[mbX];
            topMv[mbX] = leftMv = vp8Mv;
            topLeftMvMode = topMvMode[mbX];
            topMvMode[mbX] = leftMvMode = MV_NEAREST;
            topMbIntra[mbX] = leftMbIntra = false;

            vp8.macroblockInter(mbAddr, seg, lumaDCNew, lumaACNew, chromaAC, vp8Mv);

            if (mbAddr == mbLast)
                finish264();
        }

        private void doChromaInter16x16(int mbAddr, int qp, int[][] chromaDC, int[][][] chromaAC, int[] mvsH264,
                int mvVp8, int seg) {
            int mbX = mbAddr % mbWidth, mbY = mbAddr / mbWidth;
            for (int c = 0; c < 2; c++) {
                Picture refH264 = refsH264[mvsH264[2]];
                BlockInterpolator.getBlockChroma(refH264.getPlaneData(c + 1), refH264.getPlaneWidth(c + 1),
                        refH264.getPlaneHeight(c + 1), h264PredChroma, 0, 8, (mbX << 6) + mvsH264[0],
                        (mbY << 6) + mvsH264[1], 8, 8);

                VP8InterPrediction.getBlock(refVP8.getPlaneData(c + 1), refVP8.getPlaneWidth(c + 1),
                        refVP8.getPlaneHeight(c + 1), vp8PredChroma, 0, 9, (mbX << 6) + mvX(mvVp8), (mbY << 6) + mvY(mvVp8),
                        8, 8);

                doChromaResidual(qp, chromaDC, chromaAC, mbX, mbY, c, h264PredChroma, vp8PredChroma, 9, seg);
            }
        }

        private void doChromaInter16x8(int mbAddr, int qp, int[][] chromaDC, int[][][] chromaAC, int[] mvsH264,
                int mvVp81, int mvVp82, int seg) {
            int mbX = mbAddr % mbWidth, mbY = mbAddr / mbWidth;

            for (int c = 0; c < 2; c++) {
                Picture refH264 = refsH264[mvsH264[2]];

                BlockInterpolator.getBlockChroma(refH264.getPlaneData(c + 1), refH264.getPlaneWidth(c + 1),
                        refH264.getPlaneHeight(c + 1), h264PredChroma, 0, 8, (mbX << 6) + mvsH264[0],
                        (mbY << 6) + mvsH264[1], 8, 4);

                VP8InterPrediction.getBlock(refVP8.getPlaneData(c + 1), refVP8.getPlaneWidth(c + 1),
                        refVP8.getPlaneHeight(c + 1), vp8PredChroma, 0, 9, (mbX << 6) + mvX(mvVp81),
                        (mbY << 6) + mvY(mvVp81), 8, 4);

                refH264 = refsH264[mvsH264[5]];
                BlockInterpolator.getBlockChroma(refH264.getPlaneData(c + 1), refH264.getPlaneWidth(c + 1),
                        refH264.getPlaneHeight(c + 1), h264PredChroma, 32, 8, (mbX << 6) + mvsH264[3], (mbY << 6) + 32
                                + mvsH264[4], 8, 4);

                VP8InterPrediction.getBlock(refVP8.getPlaneData(c + 1), refVP8.getPlaneWidth(c + 1),
                        refVP8.getPlaneHeight(c + 1), vp8PredChroma, 9 * 4, 9, (mbX << 6) + mvX(mvVp82), (mbY << 6) + 32
                                + mvY(mvVp82), 8, 4);

                doChromaResidual(qp, chromaDC, chromaAC, mbX, mbY, c, h264PredChroma, vp8PredChroma, 9, seg);
            }
        }

        private void doChromaInter8x16(int mbAddr, int qp, int[][] chromaDC, int[][][] chromaAC, int[] mvsH264,
                int mvVp81, int mvVp82, int seg) {
            int mbX = mbAddr % mbWidth, mbY = mbAddr / mbWidth;

            for (int c = 0; c < 2; c++) {
                Picture refH264 = refsH264[mvsH264[2]];

                BlockInterpolator.getBlockChroma(refH264.getPlaneData(c + 1), refH264.getPlaneWidth(c + 1),
                        refH264.getPlaneHeight(c + 1), h264PredChroma, 0, 8, (mbX << 6) + mvsH264[0],
                        (mbY << 6) + mvsH264[1], 4, 8);

                VP8InterPrediction.getBlock(refVP8.getPlaneData(c + 1), refVP8.getPlaneWidth(c + 1),
                        refVP8.getPlaneHeight(c + 1), vp8PredChroma, 0, 9, (mbX << 6) + mvX(mvVp81),
                        (mbY << 6) + mvY(mvVp81), 4, 8);

                refH264 = refsH264[mvsH264[5]];
                BlockInterpolator.getBlockChroma(refH264.getPlaneData(c + 1), refH264.getPlaneWidth(c + 1),
                        refH264.getPlaneHeight(c + 1), h264PredChroma, 4, 8, (mbX << 6) + 32 + mvsH264[3], (mbY << 6)
                                + mvsH264[4], 4, 8);

                VP8InterPrediction.getBlock(refVP8.getPlaneData(c + 1), refVP8.getPlaneWidth(c + 1),
                        refVP8.getPlaneHeight(c + 1), vp8PredChroma, 4, 9, (mbX << 6) + 32 + mvX(mvVp82), (mbY << 6)
                                + mvY(mvVp82), 4, 8);

                doChromaResidual(qp, chromaDC, chromaAC, mbX, mbY, c, h264PredChroma, vp8PredChroma, 9, seg);
            }
        }

        private void doChromaInter8x8(int mbAddr, int qp, int[][] chromaDC, int[][][] chromaAC, int[][] mvsH264,
                int[][] mvsVp8, int[] subMbModes, int seg) {
            int mbX = mbAddr % mbWidth, mbY = mbAddr / mbWidth;

            for (int c = 0; c < 2; c++) {

                for (int blkY = 0, i = 0; blkY < 2; blkY++) {
                    for (int blkX = 0; blkX < 2; blkX++, i++) {
                        Picture refH264 = refsH264[mvsH264[i][2]];
                        int ox = (mbX << 6) + (blkX << 5), oy = (mbY << 6) + (blkY << 5);
                        int blkPixOff = (blkX << 2) + (blkY << 5);

                        int mvVp8x, mvVp8y;
                        if (subMbModes[i] == 0) {
                            BlockInterpolator.getBlockChroma(refH264.getPlaneData(c + 1), refH264.getPlaneWidth(c + 1),
                                    refH264.getPlaneHeight(c + 1), h264PredChroma, blkPixOff, 8, ox + mvsH264[i][0], oy
                                            + mvsH264[i][1], 4, 4);
                            mvVp8x = mvsVp8[i][0];
                            mvVp8y = mvsVp8[i][1];
                        } else if (subMbModes[i] == 1) {
                            BlockInterpolator.getBlockChroma(refH264.getPlaneData(c + 1), refH264.getPlaneWidth(c + 1),
                                    refH264.getPlaneHeight(c + 1), h264PredChroma, blkPixOff, 8, ox + mvsH264[i][0], oy
                                            + mvsH264[i][1], 4, 2);

                            BlockInterpolator.getBlockChroma(refH264.getPlaneData(c + 1), refH264.getPlaneWidth(c + 1),
                                    refH264.getPlaneHeight(c + 1), h264PredChroma, blkPixOff + 16, 8, ox + mvsH264[i][3], oy
                                            + 16 + mvsH264[i][4], 4, 2);
                            mvVp8x = (mvsVp8[i][0] + mvsVp8[i][3] + 1) >> 1;
                            mvVp8y = (mvsVp8[i][1] + mvsVp8[i][4] + 1) >> 1;
                        } else if (subMbModes[i] == 2) {
                            BlockInterpolator.getBlockChroma(refH264.getPlaneData(c + 1), refH264.getPlaneWidth(c + 1),
                                    refH264.getPlaneHeight(c + 1), h264PredChroma, blkPixOff, 8, ox + mvsH264[i][0], oy
                                            + mvsH264[i][1], 2, 4);

                            BlockInterpolator.getBlockChroma(refH264.getPlaneData(c + 1), refH264.getPlaneWidth(c + 1),
                                    refH264.getPlaneHeight(c + 1), h264PredChroma, blkPixOff + 2, 8, ox + 16 + mvsH264[i][3],
                                    oy + mvsH264[i][4], 2, 4);
                            mvVp8x = (mvsVp8[i][0] + mvsVp8[i][3] + 1) >> 1;
                            mvVp8y = (mvsVp8[i][1] + mvsVp8[i][4] + 1) >> 1;
                        } else {
                            BlockInterpolator.getBlockChroma(refH264.getPlaneData(c + 1), refH264.getPlaneWidth(c + 1),
                                    refH264.getPlaneHeight(c + 1), h264PredChroma, blkPixOff, 8, ox + mvsH264[i][0], oy
                                            + mvsH264[i][1], 2, 2);

                            BlockInterpolator.getBlockChroma(refH264.getPlaneData(c + 1), refH264.getPlaneWidth(c + 1),
                                    refH264.getPlaneHeight(c + 1), h264PredChroma, blkPixOff + 2, 8, ox + 16 + mvsH264[i][3],
                                    oy + mvsH264[i][4], 2, 2);

                            BlockInterpolator.getBlockChroma(refH264.getPlaneData(c + 1), refH264.getPlaneWidth(c + 1),
                                    refH264.getPlaneHeight(c + 1), h264PredChroma, blkPixOff + 16, 8, ox + mvsH264[i][6], oy
                                            + 16 + mvsH264[i][7], 2, 2);

                            BlockInterpolator.getBlockChroma(refH264.getPlaneData(c + 1), refH264.getPlaneWidth(c + 1),
                                    refH264.getPlaneHeight(c + 1), h264PredChroma, blkPixOff + 18, 8,
                                    ox + 16 + mvsH264[i][9], oy + 16 + mvsH264[i][10], 2, 2);
                            mvVp8x = (mvsVp8[i][0] + mvsVp8[i][3] + mvsVp8[i][6] + mvsVp8[i][9] + 2) >> 2;
                            mvVp8y = (mvsVp8[i][1] + mvsVp8[i][4] + mvsVp8[i][7] + mvsVp8[i][10] + 2) >> 2;
                        }

                        VP8InterPrediction.getBlock(refVP8.getPlaneData(c + 1), refVP8.getPlaneWidth(c + 1),
                                refVP8.getPlaneHeight(c + 1), vp8PredChroma, (blkX << 2) + blkY * 36, 9, ox + mvVp8x, oy
                                        + mvVp8y, 4, 4);
                    }
                }

                doChromaResidual(qp, chromaDC, chromaAC, mbX, mbY, c, h264PredChroma, vp8PredChroma, 9, seg);
            }
        }

        private void doChromaResidual(int qp, int[][] chromaDC, int[][][] chromaAC, int mbX, int mbY, int c,
                int[] h264Pred, int[] vp8Pred, int predStride, int seg) {
            
            int crQp = calcQpChroma(qp, chromaQpOffset[c]);
            if (chromaDC[c] != null) {
                CoeffTransformer.invDC2x2(chromaDC[c]);
                CoeffTransformer.dequantizeDC2x2(chromaDC[c], crQp);
            } else
                chromaDC[c] = ZERO;

            for (int i = 0; i < chromaDC[c].length; i++) {
                if (chromaAC[c] == null)
                    chromaAC[c] = new int[4][16];
                if (chromaAC[c][i] == null)
                    chromaAC[c][i] = new int[16];
                else
                    CoeffTransformer.dequantizeAC(chromaAC[c][i], crQp);
                chromaAC[c][i][0] = chromaDC[c][i];
                CoeffTransformer.idct4x4(chromaAC[c][i]);
                renderChromaH264(outH264Chroma, chromaAC[c][i], h264Pred, i & 1, i >> 1, vp8Pred, predStride);
                VPXDCT.fdct4x4(chromaAC[c][i]);
                quantizer.quantizeUV(chromaAC[c][i], segmentQps[seg]);
                renderChromaVP8(Arrays.copyOf(chromaAC[c][i], 16), segmentQps[seg], outVP8Chroma, i & 1, i >> 1, vp8Pred,
                        predStride);
            }
            topLeftH264[c + 1][0] = topLineH264[c + 1][(mbX << 3) + 7];
            H264Encoder.copyCol(outH264Chroma, 7, 8, topLeftH264[c + 1], 1);

            topLeftVP8[c + 1][0] = topLineVP8[c + 1][(mbX << 3) + 7];
            VP8Encoder.copyCol(outVP8Chroma, 7, 8, topLeftVP8[c + 1], 1);

            System.arraycopy(outVP8Chroma, 56, topLineVP8[c + 1], mbX << 3, 8);
            VP8Encoder.copyCol(outVP8Chroma, 7, 8, leftRowVP8[c + 1], 0);

            System.arraycopy(outH264Chroma, 56, topLineH264[c + 1], mbX << 3, 8);
            H264Encoder.copyCol(outH264Chroma, 7, 8, leftRowH264[c + 1], 0);

            map8x8(pictureH264.getPlaneData(c + 1), mbWidth << 3, mbX, mbY, outH264Chroma);
            map8x8(pictureVP8.getPlaneData(c + 1), mbWidth << 3, mbX, mbY, outVP8Chroma);
        }

        public void mblockPB16x8(int mbIdx, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC,
                int[][] mvs, PartPred p0, PartPred p1) {
            int seg = chooseSeg(qp);
            int mbAddr = mapper.getAddress(mbIdx);
            int mbX = mbAddr % mbWidth, mbY = mbAddr / mbWidth;

            int minX = -(mbX << 6) - 64, maxX = ((mbWidth - mbX) << 6) + 64;
            int minY = -(mbY << 6) - 64, maxY = ((mbHeight - mbY) << 6) + 64;

            if (mbX == 0) {
                resetPredictVP8(leftRowVP8, 129);
                resetPredictVP8(topLeftVP8, 129);
                topLeftVP8[0][0] = 127;
                leftMb264 = null;
            }
            int[] outH264 = new int[256];

            BlockInterpolator.getBlockLuma(refsH264[mvs[0][2]], predH264, 16, 0, (mbX << 6) + mvs[0][0], (mbY << 6)
                    + mvs[0][1], 16, 8);
            BlockInterpolator.getBlockLuma(refsH264[mvs[0][5]], predH264, 16, 128, (mbX << 6) + mvs[0][3], (mbY << 6)
                    + 32 + mvs[0][4], 16, 8);

            VP8Serializer.predictMv(topMvMode[mbX], leftMvMode, topLeftMvMode, topMv[mbX], leftMv, topLeftMv, mv, cnt,
                    minX, maxX, minY, maxY);
            int mvVp81X = MathUtil.clip(mvs[0][0], Math.max(minX, -1023 + mvX(mv[0])),
                    Math.min(maxX, 1023 + mvX(mv[0])));
            int mvVp82X = MathUtil.clip(mvs[0][3], Math.max(minX, -1023 + mvX(mv[0])),
                    Math.min(maxX, 1023 + mvX(mv[0])));
            int mvVp81Y = MathUtil.clip(mvs[0][1], Math.max(minY, -1023 + mvY(mv[0])),
                    Math.min(maxY, 1023 + mvY(mv[0])));
            int mvVp82Y = MathUtil.clip(mvs[0][4], Math.max(minY, -1023 + mvY(mv[0])),
                    Math.min(maxY, 1023 + mvY(mv[0])));

            VP8InterPrediction.getBlock(refVP8.getPlaneData(0), refVP8.getPlaneWidth(0), refVP8.getPlaneHeight(0),
                    predVP8, 0, 17, ((mbX << 6) + mvVp81X) << 1, ((mbY << 6) + mvVp81Y) << 1, 16, 8);
            VP8InterPrediction.getBlock(refVP8.getPlaneData(0), refVP8.getPlaneWidth(0), refVP8.getPlaneHeight(0),
                    predVP8, 17 * 8, 17, ((mbX << 6) + mvVp82X) << 1, ((mbY << 6) + 32 + mvVp82Y) << 1, 16, 8);

            int[] mv1 = { mvs[0][0], mvs[0][1], mvs[0][2] }, mv2 = { mvs[0][3], mvs[0][4], mvs[0][5] };
            int[][] mvsDeblock = new int[][] { mv1, mv1, mv1, mv1, mv1, mv1, mv1, mv1, mv2, mv2, mv2, mv2, mv2, mv2,
                    mv2, mv2, };
            lumaResidualInter(mbAddr, qp, lumaResidual, outH264, outVP8, predH264, predVP8, lumaACNew, mvsDeblock, seg);

            int mvVp81 = mv(mvVp81X, mvVp81Y);
            int mvVp82 = mv(mvVp82X, mvVp82Y);

            doChromaInter16x8(mbAddr, qp, chromaDC, chromaAC, mvs[0], mvVp81, mvVp82, seg);

            topLeftMvMode = topMvMode[mbX];
            topMvMode[mbX] = leftMvMode = MV_SPLIT;
            topLeftMv = topMv[mbX];
            leftMv = topMv[mbX] = mvVp82;
            topMbIntra[mbX] = leftMbIntra = false;

            vp8.macroblockInterSplit(mbAddr, seg, lumaACNew, chromaAC, VPXConst.MV_SPLT_2_HOR, new int[] { mvVp81,
                    mvVp82 });

            if (mbAddr == mbLast)
                finish264();
        }

        private void lumaResidualInter(int mbAddr, int qp, int[][] lumaAC, int[] outH264, int[] outVP8, int[] predH264,
                int[] predVP8, int[][] lumaACNew, int[][] mvsDeblock, int seg) {
            int mbX = mbAddr % mbWidth, mbY = mbAddr / mbWidth;

            for (int i = 0; i < 16; i++) {
                int invIdx = H264Const.BLK_INV_MAP[i];
                int blkOffLeft = invIdx & 3;
                int blkOffTop = invIdx >> 2;

                if (lumaAC[i] == null) {
                    nCoeff[invIdx] = 0;
                    lumaAC[i] = new int[16];
                } else {
                    nCoeff[invIdx] = nCoeff(lumaAC[i]);
                    CoeffTransformer.dequantizeAC(lumaAC[i], qp);
                    CoeffTransformer.idct4x4(lumaAC[i]);
                    // testQpOptions(lumaAC[i], qp);
                }

                renderH264Luma(outH264, lumaAC[i], predH264, predVP8, blkOffLeft, blkOffTop, 17);
                VPXDCT.fdct4x4(lumaAC[i]);
                quantizer.quantizeY(lumaAC[i], segmentQps[seg]);
                lumaACNew[invIdx] = lumaAC[i];
            }

            topLeftH264[0][0] = topLineH264[0][(mbX << 4) + 15];
            H264Encoder.copyCol(outH264, 15, 16, topLeftH264[0], 1);
            System.arraycopy(outH264, 240, topLineH264[0], mbX << 4, 16);
            H264Encoder.copyCol(outH264, 15, 16, leftRowH264[0], 0);

            for (int i = 0; i < 16; i++) {
                System.arraycopy(lumaACNew[i], 0, copyAC, 0, 16);
                renderVP8LumaJust(outVP8, copyAC, predVP8, i & 3, i >> 2, 17, seg);
            }

            topLeftVP8[0][0] = topLineVP8[0][(mbX << 4) + 15];
            VP8Encoder.copyCol(outVP8, 15, 16, topLeftVP8[0], 1);
            System.arraycopy(outVP8, 240, topLineVP8[0], mbX << 4, 16);
            VP8Encoder.copyCol(outVP8, 15, 16, leftRowVP8[0], 0);

            deblock264(sh, mbAddr, outH264, qp, nCoeff, mvsDeblock, false);
            map16x16(pictureVP8.getPlaneData(0), mbWidth, mbAddr, outVP8);
        }

        public void mblockPB8x16(int mbIdx, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC,
                int[][] mvs, PartPred p0, PartPred p1) {
            int seg = chooseSeg(qp);
            int mbAddr = mapper.getAddress(mbIdx);
            int mbX = mbAddr % mbWidth, mbY = mbAddr / mbWidth;

            int minX = -(mbX << 6) - 64, maxX = ((mbWidth - mbX) << 6) + 64;
            int minY = -(mbY << 6) - 64, maxY = ((mbHeight - mbY) << 6) + 64;

            if (mbX == 0) {
                resetPredictVP8(leftRowVP8, 129);
                resetPredictVP8(topLeftVP8, 129);
                topLeftVP8[0][0] = 127;
                leftMb264 = null;
            }
            int[] outH264 = new int[256];

            BlockInterpolator.getBlockLuma(refsH264[mvs[0][2]], predH264, 16, 0, (mbX << 6) + mvs[0][0], (mbY << 6)
                    + mvs[0][1], 8, 16);
            BlockInterpolator.getBlockLuma(refsH264[mvs[0][5]], predH264, 16, 8, (mbX << 6) + 32 + mvs[0][3],
                    (mbY << 6) + mvs[0][4], 8, 16);

            VP8Serializer.predictMv(topMvMode[mbX], leftMvMode, topLeftMvMode, topMv[mbX], leftMv, topLeftMv, mv, cnt,
                    minX, maxX, minY, maxY);
            int mvVp81X = MathUtil.clip(mvs[0][0], Math.max(minX, -1023 + mvX(mv[0])),
                    Math.min(maxX, 1023 + mvX(mv[0])));
            int mvVp82X = MathUtil.clip(mvs[0][3], Math.max(minX, -1023 + mvX(mv[0])),
                    Math.min(maxX, 1023 + mvX(mv[0])));
            int mvVp81Y = MathUtil.clip(mvs[0][1], Math.max(minY, -1023 + mvY(mv[0])),
                    Math.min(maxY, 1023 + mvY(mv[0])));
            int mvVp82Y = MathUtil.clip(mvs[0][4], Math.max(minY, -1023 + mvY(mv[0])),
                    Math.min(maxY, 1023 + mvY(mv[0])));

            VP8InterPrediction.getBlock(refVP8.getPlaneData(0), refVP8.getPlaneWidth(0), refVP8.getPlaneHeight(0),
                    predVP8, 0, 17, ((mbX << 6) + mvVp81X) << 1, ((mbY << 6) + mvVp81Y) << 1, 8, 16);
            VP8InterPrediction.getBlock(refVP8.getPlaneData(0), refVP8.getPlaneWidth(0), refVP8.getPlaneHeight(0),
                    predVP8, 8, 17, ((mbX << 6) + 32 + mvVp82X) << 1, ((mbY << 6) + mvVp82Y) << 1, 8, 16);

            int[] mv1 = { mvs[0][0], mvs[0][1], mvs[0][2] }, mv2 = { mvs[0][3], mvs[0][4], mvs[0][5] };
            int[][] mvsDeblock = new int[][] { mv1, mv1, mv2, mv2, mv1, mv1, mv2, mv2, mv1, mv1, mv2, mv2, mv1, mv1,
                    mv2, mv2 };
            lumaResidualInter(mbAddr, qp, lumaResidual, outH264, outVP8, predH264, predVP8, lumaACNew, mvsDeblock, seg);

            int mvVp82 = mv(mvVp82X, mvVp82Y);
            int mvVp81 = mv(mvVp81X, mvVp81Y);

            doChromaInter8x16(mbAddr, qp, chromaDC, chromaAC, mvs[0], mvVp81, mvVp82, seg);

            topLeftMvMode = topMvMode[mbX];
            topMvMode[mbX] = leftMvMode = MV_SPLIT;
            topLeftMv = topMv[mbX];
            leftMv = topMv[mbX] = mvVp82;
            topMbIntra[mbX] = leftMbIntra = false;

            vp8.macroblockInterSplit(mbAddr, 0, lumaACNew, chromaAC, VPXConst.MV_SPLT_2_VER,
                    new int[] { mvVp81, mvVp82 });

            if (mbAddr == mbLast)
                finish264();
        }

        public void mblockPB8x8(int mbIdx, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC,
                int[][][] mvs, int[] subMbModes, PartPred l0, PartPred l02, PartPred l03, PartPred l04) {

            int seg = chooseSeg(qp);

            int mbAddr = mapper.getAddress(mbIdx);
            int mbX = mbAddr % mbWidth, mbY = mbAddr / mbWidth;

            int[][] mvsVp8 = new int[4][12];
            int[][] mvsDeblock = new int[16][];

            if (mbX == 0) {
                resetPredictVP8(leftRowVP8, 129);
                resetPredictVP8(topLeftVP8, 129);
                topLeftVP8[0][0] = 127;
                leftMb264 = null;
            }
            int[] outH264 = new int[256];

            int minX = -(mbX << 6) - 64, maxX = ((mbWidth - mbX) << 6) + 64;
            int minY = -(mbY << 6) - 64, maxY = ((mbHeight - mbY) << 6) + 64;

            VP8Serializer.predictMv(topMvMode[mbX], leftMvMode, topLeftMvMode, topMv[mbX], leftMv, topLeftMv, mv, cnt,
                    minX, maxX, minY, maxY);

            int[] mvs16 = new int[16];
            for (int blkY = 0, i = 0; blkY < 2; blkY++) {
                for (int blkX = 0; blkX < 2; blkX++, i++) {

                    int h264BlkPix = (blkY << 7) + (blkX << 3), vp8BlkPix = (blkY << 7) + (blkY << 3) + (blkX << 3), blkOff = (blkY << 3)
                            + (blkX << 1), blkBaseX = (mbX << 6) + (blkX << 5), blkBaseY = (mbY << 6) + (blkY << 5);
                    if (subMbModes[i] == 0) {

                        BlockInterpolator.getBlockLuma(refsH264[mvs[0][i][2]], predH264, 16, h264BlkPix, blkBaseX
                                + mvs[0][i][0], blkBaseY + mvs[0][i][1], 8, 8);

                        mvsVp8[i][0] = MathUtil.clip(mvs[0][i][0], Math.max(minX, -1023 + mvX(mv[0])),
                                Math.min(maxX, 1023 + mvX(mv[0])));
                        mvsVp8[i][1] = MathUtil.clip(mvs[0][i][1], Math.max(minY, -1023 + mvY(mv[0])),
                                Math.min(maxY, 1023 + mvY(mv[0])));

                        VP8InterPrediction.getBlock(refVP8.getPlaneData(0), refVP8.getPlaneWidth(0),
                                refVP8.getPlaneHeight(0), predVP8, vp8BlkPix, 17, blkBaseX + mvsVp8[i][0] << 1,
                                blkBaseY + mvsVp8[i][1] << 1, 8, 8);

                        mvs16[blkOff] = mvs16[blkOff + 1] = mvs16[blkOff + 4] = mvs16[blkOff + 5] = mv(mvsVp8[i][0],
                                mvsVp8[i][1]);
                        mvsDeblock[blkOff] = mvsDeblock[blkOff + 1] = mvsDeblock[blkOff + 4] = mvsDeblock[blkOff + 5] = new int[] {
                                mvs[0][i][0], mvs[0][i][1], mvs[0][i][2] };

                    } else if (subMbModes[i] == 1) {

                        BlockInterpolator.getBlockLuma(refsH264[mvs[0][i][2]], predH264, 16, h264BlkPix, blkBaseX
                                + mvs[0][i][0], blkBaseY + mvs[0][i][1], 8, 4);
                        BlockInterpolator.getBlockLuma(refsH264[mvs[0][i][2]], predH264, 16, h264BlkPix + 64, blkBaseX
                                + mvs[0][i][3], blkBaseY + 16 + mvs[0][i][4], 8, 4);

                        mvsVp8[i][0] = MathUtil.clip(mvs[0][i][0], Math.max(minX, -1023 + mvX(mv[0])),
                                Math.min(maxX, 1023 + mvX(mv[0])));
                        mvsVp8[i][3] = MathUtil.clip(mvs[0][i][3], Math.max(minX, -1023 + mvX(mv[0])),
                                Math.min(maxX, 1023 + mvX(mv[0])));
                        mvsVp8[i][1] = MathUtil.clip(mvs[0][i][1], Math.max(minY, -1023 + mvY(mv[0])),
                                Math.min(maxY, 1023 + mvY(mv[0])));
                        mvsVp8[i][4] = MathUtil.clip(mvs[0][i][4], Math.max(minY, -1023 + mvY(mv[0])),
                                Math.min(maxY, 1023 + mvY(mv[0])));

                        VP8InterPrediction.getBlock(refVP8.getPlaneData(0), refVP8.getPlaneWidth(0),
                                refVP8.getPlaneHeight(0), predVP8, vp8BlkPix, 17, blkBaseX + mvsVp8[i][0] << 1,
                                blkBaseY + mvsVp8[i][1] << 1, 8, 4);
                        VP8InterPrediction.getBlock(refVP8.getPlaneData(0), refVP8.getPlaneWidth(0),
                                refVP8.getPlaneHeight(0), predVP8, vp8BlkPix + 17 * 4, 17,
                                blkBaseX + mvsVp8[i][3] << 1, blkBaseY + 16 + mvsVp8[i][4] << 1, 8, 4);

                        mvs16[blkOff] = mvs16[blkOff + 1] = mv(mvsVp8[i][0], mvsVp8[i][1]);
                        mvs16[blkOff + 4] = mvs16[blkOff + 5] = mv(mvsVp8[i][3], mvsVp8[i][4]);

                        mvsDeblock[blkOff] = mvsDeblock[blkOff + 1] = new int[] { mvs[0][i][0], mvs[0][i][1],
                                mvs[0][i][2] };
                        mvsDeblock[blkOff + 4] = mvsDeblock[blkOff + 5] = new int[] { mvs[0][i][3], mvs[0][i][4],
                                mvs[0][i][5] };

                    } else if (subMbModes[i] == 2) {

                        BlockInterpolator.getBlockLuma(refsH264[mvs[0][i][2]], predH264, 16, h264BlkPix, blkBaseX
                                + mvs[0][i][0], blkBaseY + mvs[0][i][1], 4, 8);
                        BlockInterpolator.getBlockLuma(refsH264[mvs[0][i][2]], predH264, 16, h264BlkPix + 4, blkBaseX
                                + 16 + mvs[0][i][3], blkBaseY + mvs[0][i][4], 4, 8);

                        mvsVp8[i][0] = MathUtil.clip(mvs[0][i][0], Math.max(minX, -1023 + mvX(mv[0])),
                                Math.min(maxX, 1023 + mvX(mv[0])));
                        mvsVp8[i][3] = MathUtil.clip(mvs[0][i][3], Math.max(minX, -1023 + mvX(mv[0])),
                                Math.min(maxX, 1023 + mvX(mv[0])));
                        mvsVp8[i][1] = MathUtil.clip(mvs[0][i][1], Math.max(minY, -1023 + mvY(mv[0])),
                                Math.min(maxY, 1023 + mvY(mv[0])));
                        mvsVp8[i][4] = MathUtil.clip(mvs[0][i][4], Math.max(minY, -1023 + mvY(mv[0])),
                                Math.min(maxY, 1023 + mvY(mv[0])));

                        VP8InterPrediction.getBlock(refVP8.getPlaneData(0), refVP8.getPlaneWidth(0),
                                refVP8.getPlaneHeight(0), predVP8, vp8BlkPix, 17, blkBaseX + mvsVp8[i][0] << 1,
                                blkBaseY + mvsVp8[i][1] << 1, 4, 8);
                        VP8InterPrediction.getBlock(refVP8.getPlaneData(0), refVP8.getPlaneWidth(0),
                                refVP8.getPlaneHeight(0), predVP8, vp8BlkPix + 4, 17,
                                blkBaseX + 16 + mvsVp8[i][3] << 1, blkBaseY + mvsVp8[i][4] << 1, 4, 8);

                        mvs16[blkOff] = mvs16[blkOff + 4] = mv(mvsVp8[i][0], mvsVp8[i][1]);
                        mvs16[blkOff + 1] = mvs16[blkOff + 5] = mv(mvsVp8[i][3], mvsVp8[i][4]);

                        mvsDeblock[blkOff] = mvsDeblock[blkOff + 4] = new int[] { mvs[0][i][0], mvs[0][i][1],
                                mvs[0][i][2] };
                        mvsDeblock[blkOff + 1] = mvsDeblock[blkOff + 5] = new int[] { mvs[0][i][3], mvs[0][i][4],
                                mvs[0][i][5] };

                    } else {

                        BlockInterpolator.getBlockLuma(refsH264[mvs[0][i][2]], predH264, 16, h264BlkPix, blkBaseX
                                + mvs[0][i][0], blkBaseY + mvs[0][i][1], 4, 4);
                        BlockInterpolator.getBlockLuma(refsH264[mvs[0][i][2]], predH264, 16, h264BlkPix + 4, blkBaseX
                                + 16 + mvs[0][i][3], blkBaseY + mvs[0][i][4], 4, 4);
                        BlockInterpolator.getBlockLuma(refsH264[mvs[0][i][2]], predH264, 16, h264BlkPix + 64, blkBaseX
                                + mvs[0][i][6], blkBaseY + 16 + mvs[0][i][7], 4, 4);
                        BlockInterpolator.getBlockLuma(refsH264[mvs[0][i][2]], predH264, 16, h264BlkPix + 68, blkBaseX
                                + 16 + mvs[0][i][9], blkBaseY + 16 + mvs[0][i][10], 4, 4);

                        mvsVp8[i][0] = MathUtil.clip(mvs[0][i][0], Math.max(minX, -1023 + mvX(mv[0])),
                                Math.min(maxX, 1023 + mvX(mv[0])));
                        mvsVp8[i][3] = MathUtil.clip(mvs[0][i][3], Math.max(minX, -1023 + mvX(mv[0])),
                                Math.min(maxX, 1023 + mvX(mv[0])));
                        mvsVp8[i][6] = MathUtil.clip(mvs[0][i][6], Math.max(minX, -1023 + mvX(mv[0])),
                                Math.min(maxX, 1023 + mvX(mv[0])));
                        mvsVp8[i][9] = MathUtil.clip(mvs[0][i][9], Math.max(minX, -1023 + mvX(mv[0])),
                                Math.min(maxX, 1023 + mvX(mv[0])));

                        mvsVp8[i][1] = MathUtil.clip(mvs[0][i][1], Math.max(minY, -1023 + mvY(mv[0])),
                                Math.min(maxY, 1023 + mvY(mv[0])));
                        mvsVp8[i][4] = MathUtil.clip(mvs[0][i][4], Math.max(minY, -1023 + mvY(mv[0])),
                                Math.min(maxY, 1023 + mvY(mv[0])));
                        mvsVp8[i][7] = MathUtil.clip(mvs[0][i][7], Math.max(minY, -1023 + mvY(mv[0])),
                                Math.min(maxY, 1023 + mvY(mv[0])));
                        mvsVp8[i][10] = MathUtil.clip(mvs[0][i][10], Math.max(minY, -1023 + mvY(mv[0])),
                                Math.min(maxY, 1023 + mvY(mv[0])));

                        VP8InterPrediction.getBlock(refVP8.getPlaneData(0), refVP8.getPlaneWidth(0),
                                refVP8.getPlaneHeight(0), predVP8, vp8BlkPix, 17, blkBaseX + mvsVp8[i][0] << 1,
                                blkBaseY + mvsVp8[i][1] << 1, 4, 4);
                        VP8InterPrediction.getBlock(refVP8.getPlaneData(0), refVP8.getPlaneWidth(0),
                                refVP8.getPlaneHeight(0), predVP8, vp8BlkPix + 4, 17,
                                blkBaseX + 16 + mvsVp8[i][3] << 1, blkBaseY + mvsVp8[i][4] << 1, 4, 4);
                        VP8InterPrediction.getBlock(refVP8.getPlaneData(0), refVP8.getPlaneWidth(0),
                                refVP8.getPlaneHeight(0), predVP8, vp8BlkPix + 4 * 17, 17,
                                blkBaseX + mvsVp8[i][6] << 1, blkBaseY + 16 + mvsVp8[i][7] << 1, 4, 4);
                        VP8InterPrediction.getBlock(refVP8.getPlaneData(0), refVP8.getPlaneWidth(0),
                                refVP8.getPlaneHeight(0), predVP8, vp8BlkPix + 4 + 4 * 17, 17, blkBaseX + 16
                                        + mvsVp8[i][9] << 1, blkBaseY + 16 + mvsVp8[i][10] << 1, 4, 4);

                        mvs16[blkOff] = mv(mvsVp8[i][0], mvsVp8[i][1]);
                        mvs16[blkOff + 1] = mv(mvsVp8[i][3], mvsVp8[i][4]);
                        mvs16[blkOff + 4] = mv(mvsVp8[i][6], mvsVp8[i][7]);
                        mvs16[blkOff + 5] = mv(mvsVp8[i][9], mvsVp8[i][10]);

                        mvsDeblock[blkOff] = new int[] { mvs[0][i][0], mvs[0][i][1], mvs[0][i][2] };
                        mvsDeblock[blkOff + 1] = new int[] { mvs[0][i][3], mvs[0][i][4], mvs[0][i][5] };
                        mvsDeblock[blkOff + 4] = new int[] { mvs[0][i][6], mvs[0][i][7], mvs[0][i][8] };
                        mvsDeblock[blkOff + 5] = new int[] { mvs[0][i][9], mvs[0][i][10], mvs[0][i][11] };

                    }
                }
            }

            lumaResidualInter(mbAddr, qp, lumaResidual, outH264, outVP8, predH264, predVP8, lumaACNew, mvsDeblock, seg);

            doChromaInter8x8(mbAddr, qp, chromaDC, chromaAC, mvs[0], mvsVp8, subMbModes, seg);

            topLeftMvMode = topMvMode[mbX];
            topMvMode[mbX] = leftMvMode = MV_SPLIT;
            topLeftMv = topMv[mbX];
            leftMv = topMv[mbX] = mvs16[15];
            topMbIntra[mbX] = leftMbIntra = false;

            if ((subMbModes[0] == 0) && (subMbModes[1] == 0) && (subMbModes[2] == 0) && (subMbModes[3] == 0)) {
                int[] mvs4 = new int[] { mv(mvsVp8[0][0], mvsVp8[0][1]), mv(mvsVp8[1][0], mvsVp8[1][1]),
                        mv(mvsVp8[2][0], mvsVp8[2][1]), mv(mvsVp8[3][0], mvsVp8[3][1]) };
                vp8.macroblockInterSplit(mbAddr, 0, lumaACNew, chromaAC, VPXConst.MV_SPLT_4, mvs4);
            } else {
                vp8.macroblockInterSplit(mbAddr, 0, lumaACNew, chromaAC, VPXConst.MV_SPLT_16, mvs16);
            }

            if (mbAddr == mbLast)
                finish264();
        }

        public void mblockBDirect(int mbIdx, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC) {
            throw new RuntimeException("BDirect");
        }

        public void mblockPBSkip(int mbIdx, int mvX, int mvY) {
            mblockPB16x16(mbIdx, 10, new int[16][], new int[2][4], new int[2][4][], new int[][] { { mvX, mvY, 0 } },
                    PartPred.L0);
        }
    }
}