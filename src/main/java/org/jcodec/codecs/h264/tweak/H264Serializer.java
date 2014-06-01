package org.jcodec.codecs.h264.tweak;

import static org.jcodec.codecs.h264.io.model.MBType.I_16x16;

import java.nio.ByteBuffer;

import org.jcodec.codecs.h264.H264Const;
import org.jcodec.codecs.h264.H264Const.PartPred;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.decode.CoeffTransformer;
import org.jcodec.codecs.h264.decode.aso.MapManager;
import org.jcodec.codecs.h264.decode.aso.Mapper;
import org.jcodec.codecs.h264.io.CAVLC;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.RefPicMarkingIDR;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.codecs.h264.io.write.CAVLCWriter;
import org.jcodec.codecs.h264.io.write.SliceHeaderWriter;
import org.jcodec.common.io.BitWriter;
import org.jcodec.common.model.ColorSpace;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class H264Serializer implements H264Handler {

    private ByteBuffer buf;
    private int mbWidth;
    private int mbHeight;
    private SliceSerializer ss;
    private SeqParameterSet sps;
    private PictureParameterSet pps;
    private ByteBuffer sliceStart;
    private ByteBuffer result;
    private int initQp;
    private boolean idr;
    private boolean transform8x8Allowed;

    public H264Serializer(ByteBuffer buf, int mbWidth, int mbHeight, boolean idr) {
        this.buf = buf;
        this.mbWidth = mbWidth;
        this.mbHeight = mbHeight;

        buf.putInt(0x1);
        new NALUnit(NALUnitType.SPS, 3).write(buf);
        sps = initSPS(mbWidth, mbHeight);
        H264Utils.writeSPS(buf, sps);

        buf.putInt(0x1);
        new NALUnit(NALUnitType.PPS, 3).write(buf);
        pps = initPPS(initQp);
        H264Utils.writePPS(buf, pps);
        this.idr = idr;
        sliceStart = buf.duplicate();
    }

    @Override
    public void slice(SliceHeader sh) {
        if (ss != null)
            ss.finish();

        buf.putInt(0x1);
        new NALUnit(idr ? NALUnitType.IDR_SLICE : NALUnitType.NON_IDR_SLICE, 2).write(buf);

        BitWriter bw = new BitWriter(buf);

        new SliceHeaderWriter().write(sh, idr, 2, bw);
        ss = new SliceSerializer(sh, bw);
    }

    public PictureParameterSet initPPS(int initQp) {
        PictureParameterSet pps = new PictureParameterSet();
        pps.pic_init_qp_minus26 = initQp - 26;
        return pps;
    }

    public SeqParameterSet initSPS(int mbW, int mbH) {
        SeqParameterSet sps = new SeqParameterSet();
        sps.pic_width_in_mbs_minus1 = mbW - 1;
        sps.pic_height_in_map_units_minus1 = mbH - 1;
        sps.chroma_format_idc = ColorSpace.YUV420;
        sps.profile_idc = 66;
        sps.level_idc = 40;
        sps.frame_mbs_only_flag = true;

        return sps;
    }

    @Override
    public void macroblockINxN(int mbAddr, int qpDelta, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC,
            int[] lumaModes, int chromaMode) {
        if (result != null)
            throw new IllegalStateException("Frame is complete");
        ss.macroblockINxN(mbAddr, qpDelta, lumaResidual, chromaDC, chromaAC, lumaModes, chromaMode);
    }

    @Override
    public void macroblockI16x16(int mbAddr, int qpDelta, int[] lumaDC, int[][] lumaAC, int lumaMode, int chromaMode,
            int[][] chromaDC, int[][][] chromaAC) {
        if (result != null)
            throw new IllegalStateException("Frame is complete");
        ss.macroblockI16x16(mbAddr, qpDelta, lumaDC, lumaAC, lumaMode, chromaMode, chromaDC, chromaAC);
    }

    public ByteBuffer getResult() {
        if (result == null) {
            ss.finish();
            result = sliceStart;
            result.limit(buf.position());
            H264Utils.escapeNAL(result);
            result.position(0);
        }
        return result;
    }

    public class SliceSerializer {
        private CAVLC[] cavlc;
        private BitWriter out;
        private MBType prevMbType;
        private MBType[] topMBType;
        private MBType leftMBType;
        private boolean tf8x8Left;
        private boolean[] tf8x8Top;
        private int[] i4x4PredLeft;
        private int[] i4x4PredTop;
        private int leftCBPChroma;
        private int[] topCBPLuma;
        private int leftCBPLuma;
        private int[] topCBPChroma;
        private int curQp;
        private Mapper mapper;

        public SliceSerializer(SliceHeader sh, BitWriter out) {
            this.cavlc = new CAVLC[] { new CAVLC(sps, pps, 2, 2), new CAVLC(sps, pps, 1, 1), new CAVLC(sps, pps, 1, 1) };
            this.out = out;
            this.topMBType = new MBType[mbWidth];
            this.tf8x8Top = new boolean[mbWidth];
            this.i4x4PredLeft = new int[4];
            this.i4x4PredTop = new int[mbWidth << 2];
            this.topCBPLuma = new int[mbWidth];
            this.topCBPChroma = new int[mbWidth];
            this.curQp = initQp;
            mapper = new MapManager(sh.sps, sh.pps).getMapper(sh);
        }

        public void finish() {
            out.write1Bit(1);
            out.flush();
        }

        public void macroblockINxN(int mbIdx, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC,
                int[] lumaModes, int chromaMode) {
            int mbAddr = mapper.getAddress(mbIdx);
            
            writeMBType(0, mbAddr);

            int mbX = mbAddr % mbWidth, mbY = mbAddr / mbWidth;
            boolean tf8x8Used = lumaModes.length == 4;
            if (tf8x8Used && !transform8x8Allowed)
                throw new RuntimeException("Transform 8x8 not allowed for this stream");
            boolean leftAvailable = mbX > 0, topAvailable = mbY > 0;
            if (transform8x8Allowed) {
                writeTransform8x8Flag(tf8x8Used, leftAvailable, topAvailable, leftMBType, topMBType[mbX], tf8x8Left,
                        tf8x8Top[mbX]);
            }

            if (!tf8x8Used) {
                for (int i = 0; i < 16; i++) {
                    writePredictionI4x4Block(lumaModes[i], leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                            H264Const.MB_BLK_OFF_LEFT[i], H264Const.MB_BLK_OFF_TOP[i], mbX);
                }
            } else {
                for (int i = 0; i < 4; i++) {
                    int blkX = (i & 1) << 1;
                    int blkY = i & 2;
                    writePredictionI4x4Block(lumaModes[i], leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                            blkX, blkY, mbX);
                    i4x4PredLeft[blkY + 1] = i4x4PredLeft[blkY];
                    i4x4PredTop[(mbX << 2) + blkX + 1] = i4x4PredTop[(mbX << 2) + blkX];
                }
            }
            writeChromaPredMode(chromaMode, mbX, leftAvailable, topAvailable);
            int cbp = calcCBP(lumaResidual, chromaDC, chromaAC), cbpChroma = cbp >> 4, cbpLuma = cbp & 0xf;
            writeCodedBlockPatternIntra(cbp, leftAvailable, topAvailable, leftCBPLuma | (leftCBPChroma << 4),
                    topCBPLuma[mbX] | (topCBPChroma[mbX] << 4), leftMBType, topMBType[mbX]);

            if (cbpLuma > 0 || cbpChroma > 0) {
                writeMBQpDelta(qp - curQp, prevMbType);
            }
            curQp = qp;

            if (!tf8x8Used) {
                writeResidualLuma(lumaResidual, leftAvailable, topAvailable, mbX, mbY, MBType.I_NxN, cbpLuma);
            } else
                writeResidualLuma8x8(lumaResidual, leftAvailable, topAvailable, mbX, mbY, MBType.I_NxN, tf8x8Left,
                        tf8x8Top[mbX], cbpLuma);

            writeChroma(chromaDC, chromaAC, cbpChroma, mbX, mbY, MBType.I_NxN);

            topCBPLuma[mbX] = leftCBPLuma = cbpLuma;
            topCBPChroma[mbX] = leftCBPChroma = cbpChroma;
            leftMBType = topMBType[mbX] = prevMbType = MBType.I_NxN;
            tf8x8Left = tf8x8Top[mbX] = tf8x8Used;
        }

        private void writeCodedBlockPatternIntra(int cbp, boolean leftAvailable, boolean topAvailable, int i, int j,
                MBType leftMBType2, MBType mbType) {
            CAVLCWriter.writeUE(out, H264Const.CODED_BLOCK_PATTERN_INTRA_COLOR_INV[cbp]);
        }

        public void macroblockI16x16(int mbIdx, int qp, int[] lumaDC, int[][] lumaAC, int lumaMode, int chromaMode,
                int[][] chromaDC, int[][][] chromaAC) {
            int mbAddr = mapper.getAddress(mbIdx);

            int mbX = mbAddr % mbWidth, mbY = mbAddr / mbWidth;

            int cbp = calcCBP(lumaAC, chromaDC, chromaAC), cbpChroma = cbp >> 4, cbpLuma = cbp & 0xf;

            int mbType = (cbpLuma > 0 ? 12 : 0) + cbpChroma * 4 + lumaMode + 1;
            cbpLuma = cbpLuma == 0 ? 0 : 15;
            writeMBType(mbType, mbAddr);
            writeChromaPredMode(chromaMode, mbX, mbX > 0, mbY > 0);
            writeMBQpDelta(qp - curQp, prevMbType);
            curQp = qp;

            cavlc[0].writeLumaDCBlock(out, mbX << 2, mbY << 2, I_16x16, I_16x16, lumaDC, H264Const.totalZeros16, 0, 16,
                    CoeffTransformer.zigzag4x4);
            int mbLeftBlk = mbX << 2, mbTopBlk = mbY << 2;
            for (int i = 0; i < 16; i++) {
                int blkOffLeft = H264Const.MB_BLK_OFF_LEFT[i];
                int blkOffTop = H264Const.MB_BLK_OFF_TOP[i];
                int blkX = mbLeftBlk + blkOffLeft;
                int blkY = mbTopBlk + blkOffTop;
                MBType leftMBType2 = blkOffLeft == 0 ? leftMBType : I_16x16;
                MBType topMBType2 = blkOffTop == 0 ? topMBType[mbX] : I_16x16;

                if (lumaAC[i] == null) {
                    if ((cbpLuma & (1 << (i >> 2))) != 0)
                        cavlc[0].writeZeroCoeffToken(out, blkX, blkY, leftMBType2, topMBType2);
                    else
                        cavlc[0].setZeroCoeff(blkX, blkOffTop);
                    continue;
                }

                cavlc[0].writeACBlock(out, blkX, blkY, leftMBType2, topMBType2, lumaAC[i], H264Const.totalZeros16, 1,
                        15, CoeffTransformer.zigzag4x4);
            }

            writeChroma(chromaDC, chromaAC, cbpChroma, mbX, mbY, MBType.I_16x16);

            topCBPLuma[mbX] = leftCBPLuma = cbpLuma;
            topCBPChroma[mbX] = leftCBPChroma = cbpChroma;
            leftMBType = topMBType[mbX] = prevMbType = MBType.I_16x16;
            tf8x8Left = tf8x8Top[mbX] = false;
        }

        private void writeChroma(int[][] chromaDC, int[][][] chromaAC, int pattern, int mbX, int mbY, MBType curMbType) {
            if ((pattern & 3) != 0) {
                writeChromaDC(chromaDC[0], mbX, mbX > 0, mbY > 0, 1, curMbType);
                writeChromaDC(chromaDC[1], mbX, mbX > 0, mbY > 0, 2, curMbType);
            }

            if (pattern == 2) {
                writeChromaAC(chromaAC[0], mbX > 0, mbY > 0, mbX, mbY, 1, curMbType);
                writeChromaAC(chromaAC[1], mbX > 0, mbY > 0, mbX, mbY, 2, curMbType);
            } else {
                updatePrevCBP(mbX);
            }
        }

        private int calcCBP(int[][] lumaAC, int[][] chromaDC, int[][][] chromaAC) {
            int cbpLuma = 0;
            for (int i = 0; i < 16; i++) {
                if (lumaAC[i] != null && allZero(lumaAC[i]))
                    lumaAC[i] = null;
                if (lumaAC[i] != null)
                    cbpLuma |= (1 << (i >> 2));
            }
            boolean nonZeroAC = false;
            for (int i = 0; i < 2; i++) {
                if (chromaAC[i] == null)
                    continue;
                for (int j = 0; j < chromaAC[i].length; j++) {
                    if (chromaAC[i][j] != null && allZero(chromaAC[i][j]))
                        chromaAC[i][j] = null;
                    if (chromaAC[i][j] != null) {
                        nonZeroAC = true;
                    }
                }
            }
            int cbpChroma;
            if (!nonZeroAC) {
                boolean nonZeroDC = chromaDC[0] != null && !allZero(chromaDC[0]) || chromaDC[1] != null
                        && !allZero(chromaDC[1]);
                cbpChroma = nonZeroDC ? 1 : 0;
            } else
                cbpChroma = 2;
            int cbp = (cbpChroma << 4) | cbpLuma;
            return cbp;
        }

        private boolean allZero(int[] is) {
            for (int i = 0; i < is.length; i++)
                if (is[i] != 0)
                    return false;
            return true;
        }

        protected void writeMBType(int mbType, int mbAddr) {
            CAVLCWriter.writeUE(out, mbType);
        }

        protected void writeChromaPredMode(int chromaPredMode, int mbX, boolean leftAvailable, boolean topAvailable) {
            CAVLCWriter.writeUE(out, chromaPredMode);
        }

        protected void writeMBQpDelta(int qpDelta, MBType prevMbType) {
            CAVLCWriter.writeSE(out, qpDelta);
        }

        protected void updatePrevCBP(int mbX) {
            cavlc[1].setZeroCoeff(mbX << 1, 0);
            cavlc[1].setZeroCoeff((mbX << 1) + 1, 1);
            cavlc[2].setZeroCoeff(mbX << 1, 0);
            cavlc[2].setZeroCoeff((mbX << 1) + 1, 1);
        }

        private void writeChromaAC(int[][] ac, boolean leftAvailable, boolean topAvaialbe, int mbX, int mbY, int comp,
                MBType curMbType) {
            for (int i = 0; i < ac.length; i++) {
                int blkOffLeft = H264Const.MB_BLK_OFF_LEFT[i];
                int blkOffTop = H264Const.MB_BLK_OFF_TOP[i];

                int blkX = (mbX << 1) + blkOffLeft;
                int blkY = (mbY << 1) + blkOffTop;

                MBType leftMBType2 = blkOffLeft == 0 ? leftMBType : curMbType;
                MBType topMBType2 = blkOffTop == 0 ? topMBType[mbX] : curMbType;

                if (ac[i] != null) {
                    cavlc[comp].writeACBlock(out, blkX, blkY, leftMBType2, topMBType2, ac[i], H264Const.totalZeros16,
                            1, 15, CoeffTransformer.zigzag4x4);
                } else {
                    cavlc[comp].writeZeroCoeffToken(out, blkX, blkY, leftMBType2, topMBType2);
                }
            }
        }

        protected void writeChromaDC(int[] dc, int mbX, boolean leftAvailable, boolean topAvailable, int comp,
                MBType curMbType) {
            cavlc[comp].writeChrDCBlock(out, dc, H264Const.totalZeros4, 0, dc.length, new int[] { 0, 1, 2, 3 });
        }

        protected void writeTransform8x8Flag(boolean tf8x8Used, boolean leftAvailable, boolean topAvailable,
                MBType leftMBType, MBType topMBType, boolean tf8x8UsedLeft, boolean tf8x8UsedTop) {
            CAVLCWriter.writeBool(out, tf8x8Used);
        }

        protected void writePredictionI4x4Block(int mode, boolean leftAvailable, boolean topAvailable,
                MBType leftMBType, MBType topMBType, int blkOffLeft, int blkOffTop, int mbX) {

            int predMode = 2;
            if ((leftAvailable || blkOffLeft > 0) && (topAvailable || blkOffTop > 0)) {
                int predModeB = topMBType == MBType.I_NxN || blkOffTop > 0 ? i4x4PredTop[(mbX << 2) + blkOffLeft] : 2;
                int predModeA = leftMBType == MBType.I_NxN || blkOffLeft > 0 ? i4x4PredLeft[blkOffTop] : 2;
                predMode = Math.min(predModeB, predModeA);
            }

            i4x4PredTop[(mbX << 2) + blkOffLeft] = i4x4PredLeft[blkOffTop] = mode;

            if (mode != predMode) {
                writeUse4x4PredMode(false);
                if (mode > predMode)
                    mode -= 1;
                writeRem4x4PredMode(mode);
            } else {
                writeUse4x4PredMode(true);
            }
        }

        private void writeRem4x4PredMode(int mode) {
            CAVLCWriter.writeU(out, mode, 3);

        }

        private void writeUse4x4PredMode(boolean b) {
            CAVLCWriter.writeBool(out, b);
        }

        private void writeResidualLuma8x8(int[][] lumaResidual, boolean leftAvailable, boolean topAvailable, int mbX,
                int mbY, MBType curMbType, boolean tf8x8Left, boolean tf8x8Top, int cbpLuma) {

            for (int i = 0; i < 4; i++) {
                int blk8x8OffLeft = (i & 1) << 1;
                int blk8x8OffTop = i & 2;
                int blkX = (mbX << 2) + blk8x8OffLeft;
                int blkY = (mbY << 2) + blk8x8OffTop;

                if ((cbpLuma & (1 << i)) == 0) {
                    cavlc[0].setZeroCoeff(blkX, blk8x8OffTop);
                    cavlc[0].setZeroCoeff(blkX + 1, blk8x8OffTop);
                    cavlc[0].setZeroCoeff(blkX, blk8x8OffTop + 1);
                    cavlc[0].setZeroCoeff(blkX + 1, blk8x8OffTop + 1);
                    continue;
                }
                for (int j = 0; j < 4; j++) {
                    int[] ac16 = new int[16];
                    for (int k = 0; k < 16; k++)
                        ac16[k] = lumaResidual[i][CoeffTransformer.zigzag8x8[(k << 2) + j]];
                    int blkOffLeft = blk8x8OffLeft + (j & 1);
                    int blkOffTop = blk8x8OffTop + (j >> 1);
                    cavlc[0].writeACBlock(out, blkX + (j & 1), blkOffTop, blkOffLeft == 0 ? leftMBType : curMbType,
                            blkOffTop == 0 ? topMBType[mbX] : curMbType, ac16, H264Const.totalZeros16, 0, 16,
                            H264Const.identityMapping16);

                }
            }
        }

        private void writeResidualLuma(int[][] lumaResidual, boolean leftAvailable, boolean topAvailable, int mbX,
                int mbY, MBType curMbType, int cbpLuma) {

            for (int i = 0; i < 16; i++) {
                int blkOffLeft = H264Const.MB_BLK_OFF_LEFT[i];
                int blkOffTop = H264Const.MB_BLK_OFF_TOP[i];
                int blkX = (mbX << 2) + blkOffLeft;
                int blkY = (mbY << 2) + blkOffTop;
                MBType leftMBType2 = blkOffLeft == 0 ? leftMBType : curMbType;
                MBType topMBType2 = blkOffTop == 0 ? topMBType[mbX] : curMbType;

                if (lumaResidual[i] == null) {
                    if ((cbpLuma & (1 << (i >> 2))) != 0)
                        cavlc[0].writeZeroCoeffToken(out, blkX, blkY, leftMBType2, topMBType2);
                    else
                        cavlc[0].setZeroCoeff(blkX, blkOffTop);
                    continue;
                }

                cavlc[0].writeACBlock(out, blkX, blkY, leftMBType2, topMBType2, lumaResidual[i],
                        H264Const.totalZeros16, 0, 16, CoeffTransformer.zigzag4x4);
            }
        }
    }

    @Override
    public void mblockPB16x16(int mbIdx, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC,
            int[][] mvs, PartPred pred) {
        throw new RuntimeException("PB16x16");
    }

    @Override
    public void mblockPB16x8(int mbIdx, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC,
            int[][] mvs, PartPred p0, PartPred p1) {
        throw new RuntimeException("PB16x8");
    }

    @Override
    public void mblockPB8x16(int mbIdx, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC,
            int[][] mvs, PartPred p0, PartPred p1) {
        throw new RuntimeException("PB8x16");
    }

    @Override
    public void mblockPB8x8(int mbIdx, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC,
            int[][][] mvs, int[] subMbTypes, PartPred l0, PartPred l02, PartPred l03, PartPred l04) {
        throw new RuntimeException("PB8x8");
    }

    @Override
    public void mblockBDirect(int mbIdx, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC) {
        throw new RuntimeException("DIRECT");
    }

    @Override
    public void mblockPBSkip(int mbIdx, int mvX, int mvY) {
    }
}