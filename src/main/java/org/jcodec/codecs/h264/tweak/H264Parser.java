package org.jcodec.codecs.h264.tweak;

import static org.jcodec.codecs.h264.H264Const.bPartPredModes;
import static org.jcodec.codecs.h264.H264Const.bSubMbTypes;
import static org.jcodec.codecs.h264.H264Const.identityMapping16;
import static org.jcodec.codecs.h264.H264Const.last_sig_coeff_map_8x8;
import static org.jcodec.codecs.h264.H264Const.sig_coeff_map_8x8;
import static org.jcodec.codecs.h264.H264Const.PartPred.L0;
import static org.jcodec.codecs.h264.H264Utils.unescapeNAL;
import static org.jcodec.codecs.h264.decode.CAVLCReader.moreRBSPData;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readBool;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readNBit;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readSE;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readTE;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readUE;
import static org.jcodec.codecs.h264.decode.SliceDecoder.calcMVPrediction16x8Bottom;
import static org.jcodec.codecs.h264.decode.SliceDecoder.calcMVPrediction16x8Top;
import static org.jcodec.codecs.h264.decode.SliceDecoder.calcMVPrediction8x16Left;
import static org.jcodec.codecs.h264.decode.SliceDecoder.calcMVPrediction8x16Right;
import static org.jcodec.codecs.h264.decode.SliceDecoder.calcMVPredictionMedian;
import static org.jcodec.codecs.h264.decode.SliceDecoder.copyVect;
import static org.jcodec.codecs.h264.decode.SliceDecoder.saveVect;
import static org.jcodec.codecs.h264.io.model.MBType.B_8x8;
import static org.jcodec.codecs.h264.io.model.MBType.I_16x16;
import static org.jcodec.codecs.h264.io.model.MBType.P_8x8;
import static org.jcodec.codecs.h264.io.model.SliceType.P;
import static org.jcodec.common.model.ColorSpace.MONO;

import java.nio.ByteBuffer;
import java.util.List;

import org.jcodec.codecs.common.biari.MDecoder;
import org.jcodec.codecs.h264.H264Const;
import org.jcodec.codecs.h264.H264Const.PartPred;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.decode.CoeffTransformer;
import org.jcodec.codecs.h264.decode.SliceHeaderReader;
import org.jcodec.codecs.h264.decode.aso.MapManager;
import org.jcodec.codecs.h264.decode.aso.Mapper;
import org.jcodec.codecs.h264.io.CABAC;
import org.jcodec.codecs.h264.io.CABAC.BlockType;
import org.jcodec.codecs.h264.io.CAVLC;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.common.IntObjectMap;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.model.ColorSpace;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class H264Parser {
    private IntObjectMap<SeqParameterSet> sps = new IntObjectMap<SeqParameterSet>();
    private IntObjectMap<PictureParameterSet> pps = new IntObjectMap<PictureParameterSet>();

    public H264Parser(SeqParameterSet[] spsList, PictureParameterSet[] ppsList) {
        for (SeqParameterSet _sps : spsList)
            sps.put(_sps.seq_parameter_set_id, _sps);

        for (PictureParameterSet _pps : ppsList)
            pps.put(_pps.pic_parameter_set_id, _pps);
    }

    public H264Parser() {
    }

    public void parse(List<ByteBuffer> nalUnits, H264Handler handler) {
        for (ByteBuffer nalUnit : nalUnits) {
            NALUnit marker = NALUnit.read(nalUnit);

            unescapeNAL(nalUnit);

            if (marker.type == NALUnitType.IDR_SLICE || marker.type == NALUnitType.NON_IDR_SLICE) {
                BitReader in = new BitReader(nalUnit.duplicate());
                SliceHeader sh = new SliceHeaderReader().readPart1(in);
                PictureParameterSet _pps = pps.get(sh.pic_parameter_set_id);
                SeqParameterSet _sps = sps.get(_pps.seq_parameter_set_id);
                if (_pps.entropy_coding_mode_flag)
                    new CABACSliceParser(_sps, _pps, nalUnit, marker, handler).parse();
                else
                    new CAVLCSliceParser(_sps, _pps, nalUnit, marker, handler).parse();
            } else if (marker.type == NALUnitType.SPS) {
                SeqParameterSet _sps = SeqParameterSet.read(nalUnit);
                sps.put(_sps.seq_parameter_set_id, _sps);
            } else if (marker.type == NALUnitType.PPS) {
                PictureParameterSet _pps = PictureParameterSet.read(nalUnit);
                pps.put(_pps.pic_parameter_set_id, _pps);
            }
        }
    }

    public void parse(ByteBuffer frame, H264Handler handler) {
        parse(H264Utils.splitFrame(frame), handler);
    }

    public static class CABACSliceParser extends BaseSliceParser {
        private CABAC cabac;
        private MDecoder mDecoder;

        public CABACSliceParser(SeqParameterSet _sps, PictureParameterSet _pps, ByteBuffer nalUnit, NALUnit marker,
                H264Handler handler) {
            super(_sps, _pps, marker, nalUnit, handler);
        }

        @Override
        protected void parseSliceData(SliceHeader sh, BitReader reader) {
            cabac = new CABAC(mbWidth);
            int qp = sh.pps.pic_init_qp_minus26 + 26 + sh.slice_qp_delta;
            int[][] cm = new int[2][1024];
            cabac.initModels(cm, sh.slice_type, sh.cabac_init_idc, qp);
            mDecoder = new MDecoder(reader.terminate(), cm);

            boolean mbaffFrameFlag = (sh.sps.mb_adaptive_frame_field_flag && !sh.field_pic_flag);
            boolean prevMbSkipped = false;
            MBType prevMBType = null;
            for (int i = 0;; i++) {

                int mbAddr = mapper.getAddress(i);
                int mbX = mbAddr % mbWidth;

                if (sh.slice_type.isIntra()
                        || !cabac.readMBSkipFlag(mDecoder, sh.slice_type, mapper.leftAvailable(i),
                                mapper.topAvailable(i), mbX)) {

                    boolean mb_field_decoding_flag = false;
                    if (mbaffFrameFlag && (i % 2 == 0 || (i % 2 == 1 && prevMbSkipped))) {
                        mb_field_decoding_flag = readMBFieldDecodingFlag();
                    }

                    prevMBType = decodeMacroblock(sh.slice_type, i, mb_field_decoding_flag, prevMBType);

                } else {
                    decodeSkip(i, sh.slice_type);
                    prevMBType = null;
                }

                if (mDecoder.decodeFinalBin() == 1)
                    break;
            }
        }

        protected boolean readMBFieldDecodingFlag() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected int readMBType(int mbIdx) {
            return cabac.readMBTypeI(mDecoder, leftMBType, topMBType[mapper.getMbX(mbIdx)],
                    mapper.leftAvailable(mbIdx), mapper.topAvailable(mbIdx));
        }

        @Override
        protected boolean readTransform8x8Flag(boolean leftAvailable, boolean topAvailable, MBType leftType,
                MBType topType, boolean is8x8Left, boolean is8x8Top) {
            return cabac.readTransform8x8Flag(mDecoder, leftAvailable, topAvailable, leftType, topType, is8x8Left,
                    is8x8Top);
        }

        @Override
        protected int readRem4x4PredMode() {
            return cabac.rem4x4PredMode(mDecoder);
        }

        @Override
        protected boolean readPrev4x4PredMode() {
            return cabac.prev4x4PredModeFlag(mDecoder);
        }

        @Override
        protected int readChromaPredMode(int mbX, boolean leftAvailable, boolean topAvailable) {
            return cabac
                    .readIntraChromaPredMode(mDecoder, mbX, leftMBType, topMBType[mbX], leftAvailable, topAvailable);
        }

        @Override
        protected int readCodedBlockPatternIntra(boolean leftAvailable, boolean topAvailable, int leftCBP, int topCBP,
                MBType leftMB, MBType topMB) {

            return cabac.codedBlockPatternIntra(mDecoder, leftAvailable, topAvailable, leftCBP, topCBP, leftMB, topMB);
        }

        @Override
        protected int readMBQpDelta(MBType prevMbType) {
            return cabac.readMBQpDelta(mDecoder, prevMbType);
        }

        @Override
        protected int[][] residualLuma8x8(boolean leftAvailable, boolean topAvailable, int mbX, int codedBlockPattern,
                MBType mbType, boolean tf8x8Left, boolean tf8x8Top) {
            int cbpLuma = codedBlockPattern & 0xf;

            int[][] result = new int[4][];
            for (int i = 0; i < 4; i++) {
                int blkOffLeft = (i & 1) << 1;
                int blkOffTop = i & 2;
                int blkX = (mbX << 2) + blkOffLeft;

                if ((cbpLuma & (1 << i)) == 0) {
                    continue;
                }
                int[] ac = new int[64];

                int nCoeff = cabac.readCoeffs(mDecoder, BlockType.LUMA_64, ac, 0, 64, CoeffTransformer.zigzag8x8,
                        sig_coeff_map_8x8, last_sig_coeff_map_8x8);
                cabac.setCodedBlock(blkX, blkOffTop);
                cabac.setCodedBlock(blkX + 1, blkOffTop);
                cabac.setCodedBlock(blkX, blkOffTop + 1);
                cabac.setCodedBlock(blkX + 1, blkOffTop + 1);
                result[i] = ac;
            }

            cabac.setPrevCBP(codedBlockPattern);

            return result;
        }

        @Override
        protected int[][] residualLuma(boolean leftAvailable, boolean topAvailable, int mbX, int codedBlockPattern,
                MBType curMbType) {
            int cbpLuma = codedBlockPattern & 0xf;

            int[][] result = new int[16][];
            for (int i = 0; i < 16; i++) {
                int blkOffLeft = H264Const.MB_BLK_OFF_LEFT[i];
                int blkOffTop = H264Const.MB_BLK_OFF_TOP[i];
                int blkX = (mbX << 2) + blkOffLeft;

                if ((cbpLuma & (1 << (i >> 2))) == 0) {

                    continue;
                }
                int[] ac = new int[16];

                if (cabac.readCodedBlockFlagLumaAC(mDecoder, BlockType.LUMA_16, blkX, blkOffTop, 0, leftMBType,
                        topMBType[mbX], leftAvailable, topAvailable, leftCBPLuma, topCBPLuma[mbX], cbpLuma, curMbType) == 1) {
                    cabac.readCoeffs(mDecoder, BlockType.LUMA_16, ac, 0, 16, CoeffTransformer.zigzag4x4,
                            identityMapping16, identityMapping16);
                    result[i] = ac;
                }
            }

            cabac.setPrevCBP(codedBlockPattern);

            return result;
        }

        @Override
        protected int[] chromaDC(int mbX, boolean leftAvailable, boolean topAvailable, int comp, MBType curMbType) {
            int[] dc = new int[(16 >> chromaFormat.compWidth[1]) >> chromaFormat.compHeight[1]];
            if (cabac.readCodedBlockFlagChromaDC(mDecoder, mbX, comp, leftMBType, topMBType[mbX], leftAvailable,
                    topAvailable, leftCBPChroma, topCBPChroma[mbX], curMbType) == 1) {
                cabac.readCoeffs(mDecoder, BlockType.CHROMA_DC, dc, 0, 4, identityMapping16, identityMapping16,
                        identityMapping16);
            }
            return dc;
        }

        @Override
        protected int[][] chromaAC(boolean leftAvailable, boolean topAvailable, int mbX, int comp, MBType curMbType,
                boolean codedAC) {
            int nBlocks = (16 >> chromaFormat.compWidth[1]) >> chromaFormat.compHeight[1];
            int[][] result = new int[nBlocks][];
            for (int i = 0; i < nBlocks; i++) {
                int[] ac = new int[16];
                int blkOffLeft = H264Const.MB_BLK_OFF_LEFT[i];
                int blkOffTop = H264Const.MB_BLK_OFF_TOP[i];

                int blkX = (mbX << 1) + blkOffLeft;

                if (codedAC) {

                    if (cabac.readCodedBlockFlagChromaAC(mDecoder, blkX, blkOffTop, comp, leftMBType, topMBType[mbX],
                            leftAvailable, topAvailable, leftCBPChroma, topCBPChroma[mbX], curMbType) == 1)
                        cabac.readCoeffs(mDecoder, BlockType.CHROMA_AC, ac, 1, 15, CoeffTransformer.zigzag4x4,
                                identityMapping16, identityMapping16);
                    result[i] = ac;
                }
            }

            return result;
        }

        @Override
        protected int[] residualLumaI16x16DC(boolean leftAvailable, boolean topAvailable, int mbX, int mbY, int cbpLuma) {
            int[] dc = new int[16];
            if (cabac.readCodedBlockFlagLumaDC(mDecoder, mbX, leftMBType, topMBType[mbX], leftAvailable, topAvailable,
                    MBType.I_16x16) == 1)
                cabac.readCoeffs(mDecoder, BlockType.LUMA_16_DC, dc, 0, 16, CoeffTransformer.zigzag4x4,
                        identityMapping16, identityMapping16);
            return dc;
        }

        @Override
        protected int[][] residualLumaI16x16AC(boolean leftAvailable, boolean topAvailable, int mbX, int mbY,
                int cbpLuma) {

            int[][] result = new int[16][];
            for (int i = 0; i < 16; i++) {
                int[] ac = new int[16];
                int blkOffLeft = H264Const.MB_BLK_OFF_LEFT[i];
                int blkOffTop = H264Const.MB_BLK_OFF_TOP[i];
                int blkX = (mbX << 2) + blkOffLeft;

                if ((cbpLuma & (1 << (i >> 2))) != 0) {
                    if (cabac.readCodedBlockFlagLumaAC(mDecoder, BlockType.LUMA_15_AC, blkX, blkOffTop, 0, leftMBType,
                            topMBType[mbX], leftAvailable, topAvailable, leftCBPLuma, topCBPLuma[mbX], cbpLuma,
                            MBType.I_16x16) == 1) {
                        cabac.readCoeffs(mDecoder, BlockType.LUMA_15_AC, ac, 1, 15, CoeffTransformer.zigzag4x4,
                                identityMapping16, identityMapping16);
                        result[i] = ac;
                    }
                }
            }
            return result;
        }

        @Override
        protected void updatePrevCBP(int mbX) {
        }

        @Override
        protected int readRefIdx(boolean leftAvailable, boolean topAvailable, MBType leftMBType, MBType topMBType,
                PartPred predModeLeft, PartPred predModeTop, PartPred pred, int mbX, int partX, int partY, int partW,
                int partH, int list) {
            return cabac.readRefIdx(mDecoder, leftAvailable, topAvailable, leftMBType, topMBType, predModeLeft,
                    predModeTop, pred, mbX, partX, partY, partW, partH, list);
        }

        @Override
        protected int readMVD(int comp, boolean leftAvailable, boolean topAvailable, MBType leftMBType,
                MBType topMBType, PartPred leftPred, PartPred topPred, PartPred pred, int mbX, int partX, int partY,
                int partW, int partH, int list) {
            return cabac.readMVD(mDecoder, comp, leftAvailable, topAvailable, leftMBType, topMBType, leftPred, topPred,
                    pred, mbX, partX, partY, partW, partH, list);
        }

        @Override
        protected int readCodedBlockPatternInter(boolean leftAvailable, boolean topAvailable, int leftCBP, int topCBP,
                MBType leftMB, MBType topMB) {
            return cabac.codedBlockPatternIntra(mDecoder, leftAvailable, topAvailable, leftCBP, topCBP, leftMB, topMB);
        }

        @Override
        protected int readSubMBTypeP() {
            return cabac.readSubMbTypeP(mDecoder);
        }

        @Override
        protected int readSubMBTypeB() {
            return cabac.readSubMbTypeB(mDecoder);
        }

        @Override
        protected int readMBTypeB(int mbIdx) {
            return cabac.readMBTypeB(mDecoder, leftMBType, topMBType[mapper.getMbX(mbIdx)],
                    mapper.leftAvailable(mbIdx), mapper.topAvailable(mbIdx));
        }

        @Override
        protected int readMBTypeP(int mbIdx) {
            return cabac.readMBTypeP(mDecoder);
        }
    }

    public static class CAVLCSliceParser extends BaseSliceParser {

        private BitReader reader;
        private CAVLC[] cavlc;

        public CAVLCSliceParser(SeqParameterSet _sps, PictureParameterSet _pps, ByteBuffer nalUnit, NALUnit marker,
                H264Handler handler) {
            super(_sps, _pps, marker, nalUnit, handler);
            cavlc = new CAVLC[] { new CAVLC(_sps, _pps, 2, 2), new CAVLC(_sps, _pps, 1, 1), new CAVLC(_sps, _pps, 1, 1) };
        }

        @Override
        protected void parseSliceData(SliceHeader sh, BitReader reader) {
            this.reader = reader;
            boolean mbaffFrameFlag = (sh.sps.mb_adaptive_frame_field_flag && !sh.field_pic_flag);
            boolean prevMbSkipped = false;
            int i;
            MBType prevMBType = null;
            for (i = 0;; i++) {
                if (sh.slice_type.isInter()) {
                    int mbSkipRun = readUE(reader, "mb_skip_run");
                    for (int j = 0; j < mbSkipRun; j++, i++) {
                        decodeSkip(i, sh.slice_type);
                    }

                    prevMbSkipped = mbSkipRun > 0;
                    prevMBType = null;

                    if (!moreRBSPData(reader) || i >= mbWidth * mbHeight)
                        break;
                }

                boolean mb_field_decoding_flag = false;
                if (mbaffFrameFlag && (i % 2 == 0 || (i % 2 == 1 && prevMbSkipped))) {
                    mb_field_decoding_flag = readMBFieldDecodingFlag();
                }

                prevMBType = decodeMacroblock(sh.slice_type, i, mb_field_decoding_flag, prevMBType);

                if (!moreRBSPData(reader) || i >= mbWidth * mbHeight - 1)
                    break;
            }
        }

        protected boolean readMBFieldDecodingFlag() {
            return readBool(reader, "mb_field_decoding_flag");
        }

        @Override
        protected int readMBType(int mbIdx) {
            return readUE(reader, "MB: mb_type");
        }

        @Override
        protected boolean readTransform8x8Flag(boolean leftAvailable, boolean topAvailable, MBType leftType,
                MBType topType, boolean is8x8Left, boolean is8x8Top) {
            return readBool(reader, "transform_size_8x8_flag");
        }

        @Override
        protected int readRem4x4PredMode() {
            return readNBit(reader, 3, "MB: rem_intra4x4_pred_mode");
        }

        @Override
        protected boolean readPrev4x4PredMode() {
            return readBool(reader, "MBP: prev_intra4x4_pred_mode_flag");
        }

        @Override
        protected int readChromaPredMode(int mbX, boolean leftAvailable, boolean topAvailable) {
            return readUE(reader, "MBP: intra_chroma_pred_mode");
        }

        @Override
        protected int readCodedBlockPatternIntra(boolean leftAvailable, boolean topAvailable, int leftCBP, int topCBP,
                MBType leftMB, MBType topMB) {
            return H264Const.CODED_BLOCK_PATTERN_INTRA_COLOR[readUE(reader, "coded_block_pattern")];
        }

        @Override
        protected int readMBQpDelta(MBType prevMbType) {
            return readSE(reader, "mb_qp_delta");
        }

        @Override
        protected int[][] residualLuma8x8(boolean leftAvailable, boolean topAvailable, int mbX, int codedBlockPattern,
                MBType curMbType, boolean tf8x8Left, boolean tf8x8Top) {
            int cbpLuma = codedBlockPattern & 0xf;

            int[][] result = new int[4][];
            for (int i = 0; i < 4; i++) {
                int blk8x8OffLeft = (i & 1) << 1;
                int blk8x8OffTop = i & 2;
                int blkX = (mbX << 2) + blk8x8OffLeft;

                if ((cbpLuma & (1 << i)) == 0) {
                    cavlc[0].setZeroCoeff(blkX, blk8x8OffTop);
                    cavlc[0].setZeroCoeff(blkX + 1, blk8x8OffTop);
                    cavlc[0].setZeroCoeff(blkX, blk8x8OffTop + 1);
                    cavlc[0].setZeroCoeff(blkX + 1, blk8x8OffTop + 1);
                    continue;
                }
                int[] ac64 = new int[64];
                int coeffs = 0;
                for (int j = 0; j < 4; j++) {
                    int[] ac16 = new int[16];
                    int blkOffLeft = blk8x8OffLeft + (j & 1);
                    int blkOffTop = blk8x8OffTop + (j >> 1);
                    coeffs += cavlc[0].readACBlock(reader, ac16, blkX + (j & 1), blkOffTop, blkOffLeft != 0
                            || leftAvailable, blkOffLeft == 0 ? leftMBType : curMbType, blkOffTop != 0 || topAvailable,
                            blkOffTop == 0 ? topMBType[mbX] : curMbType, 0, 16, H264Const.identityMapping16);
                    for (int k = 0; k < 16; k++)
                        ac64[CoeffTransformer.zigzag8x8[(k << 2) + j]] = ac16[k];
                }
                result[i] = ac64;
            }
            return result;
        }

        @Override
        protected int[][] residualLuma(boolean leftAvailable, boolean topAvailable, int mbX, int codedBlockPattern,
                MBType curMbType) {
            int cbpLuma = codedBlockPattern & 0xf;

            int[][] result = new int[16][];
            for (int i = 0; i < 16; i++) {
                int blkOffLeft = H264Const.MB_BLK_OFF_LEFT[i];
                int blkOffTop = H264Const.MB_BLK_OFF_TOP[i];
                int blkX = (mbX << 2) + blkOffLeft;

                if ((cbpLuma & (1 << (i >> 2))) == 0) {
                    cavlc[0].setZeroCoeff(blkX, blkOffTop);
                    continue;
                }
                int[] ac = new int[16];

                int nCoeff = cavlc[0].readACBlock(reader, ac, blkX, blkOffTop, blkOffLeft != 0 || leftAvailable,
                        blkOffLeft == 0 ? leftMBType : curMbType, blkOffTop != 0 || topAvailable,
                        blkOffTop == 0 ? topMBType[mbX] : curMbType, 0, 16, CoeffTransformer.zigzag4x4);
                result[i] = ac;
            }
            return result;
        }

        @Override
        protected int[] chromaDC(int mbX, boolean leftAvailable, boolean topAvailable, int comp, MBType curMbType) {
            int[] dc = new int[(16 >> chromaFormat.compWidth[1]) >> chromaFormat.compHeight[1]];
            cavlc[comp].readChromaDCBlock(reader, dc, leftAvailable, topAvailable);
            return dc;
        }

        @Override
        protected int[][] chromaAC(boolean leftAvailable, boolean topAvailable, int mbX, int comp, MBType curMbType,
                boolean codedAC) {
            int nBlocks = (16 >> chromaFormat.compWidth[1]) >> chromaFormat.compHeight[1];
            int[][] result = new int[nBlocks][];
            for (int i = 0; i < nBlocks; i++) {
                int[] ac = new int[16];
                int blkOffLeft = H264Const.MB_BLK_OFF_LEFT[i];
                int blkOffTop = H264Const.MB_BLK_OFF_TOP[i];

                int blkX = (mbX << 1) + blkOffLeft;

                if (codedAC) {

                    cavlc[comp].readACBlock(reader, ac, blkX, blkOffTop, blkOffLeft != 0 || leftAvailable,
                            blkOffLeft == 0 ? leftMBType : curMbType, blkOffTop != 0 || topAvailable,
                            blkOffTop == 0 ? topMBType[mbX] : curMbType, 1, 15, CoeffTransformer.zigzag4x4);
                    result[i] = ac;
                } else {
                    cavlc[comp].setZeroCoeff(blkX, blkOffTop);
                }
            }
            return result;
        }

        @Override
        protected int[] residualLumaI16x16DC(boolean leftAvailable, boolean topAvailable, int mbX, int mbY, int cbpLuma) {
            int[] dc = new int[16];
            cavlc[0].readLumaDCBlock(reader, dc, mbX, leftAvailable, leftMBType, topAvailable, topMBType[mbX],
                    CoeffTransformer.zigzag4x4);
            return dc;
        }

        @Override
        protected int[][] residualLumaI16x16AC(boolean leftAvailable, boolean topAvailable, int mbX, int mbY,
                int cbpLuma) {

            int[][] result = new int[16][];
            for (int i = 0; i < 16; i++) {
                int[] ac = new int[16];
                int blkOffLeft = H264Const.MB_BLK_OFF_LEFT[i];
                int blkOffTop = H264Const.MB_BLK_OFF_TOP[i];
                int blkX = (mbX << 2) + blkOffLeft;
                int blkY = (mbY << 2) + blkOffTop;

                if ((cbpLuma & (1 << (i >> 2))) != 0) {

                    int nCoeff = cavlc[0].readACBlock(reader, ac, blkX, blkOffTop, blkOffLeft != 0 || leftAvailable,
                            blkOffLeft == 0 ? leftMBType : I_16x16, blkOffTop != 0 || topAvailable,
                            blkOffTop == 0 ? topMBType[mbX] : I_16x16, 1, 15, CoeffTransformer.zigzag4x4);
                    result[i] = ac;
                } else {
                    cavlc[0].setZeroCoeff(blkX, blkOffTop);
                }
            }
            return result;
        }

        @Override
        protected void updatePrevCBP(int mbX) {
            cavlc[1].setZeroCoeff(mbX << 1, 0);
            cavlc[1].setZeroCoeff((mbX << 1) + 1, 1);
            cavlc[2].setZeroCoeff(mbX << 1, 0);
            cavlc[2].setZeroCoeff((mbX << 1) + 1, 1);
        }

        @Override
        protected int readRefIdx(boolean leftAvailable, boolean topAvailable, MBType leftMBType, MBType topMBType,
                PartPred predModeLeft, PartPred predModeTop, PartPred pred, int mbX, int partX, int partY, int partW,
                int partH, int list) {
            return readTE(reader, numRef[list] - 1);
        }

        @Override
        protected int readMVD(int comp, boolean leftAvailable, boolean topAvailable, MBType leftMBType,
                MBType topMBType, PartPred leftPred, PartPred topPred, PartPred pred, int mbX, int partX, int partY,
                int partW, int partH, int list) {
            return readSE(reader, "mvd_l0_x");
        }

        @Override
        protected int readCodedBlockPatternInter(boolean leftAvailable, boolean topAvailable, int i, int j,
                MBType leftMBType2, MBType mbType) {
            return H264Const.CODED_BLOCK_PATTERN_INTER_COLOR[readUE(reader, "coded_block_pattern")];
        }

        @Override
        protected int readSubMBTypeP() {
            return readUE(reader, "SUB: sub_mb_type");
        }

        @Override
        protected int readSubMBTypeB() {
            return readUE(reader, "SUB: sub_mb_type");
        }

        @Override
        protected int readMBTypeB(int mbIdx) {
            return readUE(reader, "MB: mb_type");
        }

        @Override
        protected int readMBTypeP(int mbIdx) {
            return readUE(reader, "MB: mb_type");
        }
    }

    public abstract static class BaseSliceParser {

        protected SeqParameterSet activeSPS;
        protected PictureParameterSet activePPS;
        protected NALUnit marker;
        protected H264Handler handler;
        protected Mapper mapper;
        protected MBType[] topMBType;
        protected MBType leftMBType;
        private ByteBuffer segment;
        private boolean transform8x8;
        private boolean[] tf8x8Top;
        private boolean tf8x8Left;
        private int[] i4x4PredLeft;
        private int[] i4x4PredTop;
        protected int[] topCBPLuma;
        protected int[] topCBPChroma;
        protected int leftCBPLuma;
        protected int leftCBPChroma;
        protected ColorSpace chromaFormat;
        protected int qp;
        protected int[] numRef;
        private PartPred[] predModeLeft;
        private PartPred[] predModeTop;
        private int[][][] mvTop;
        private int[][][] mvLeft;
        private int[][][] mvTopLeft;
        protected int mbWidth;
        protected int mbHeight;
        protected SliceHeader sh;

        public BaseSliceParser(SeqParameterSet _sps, PictureParameterSet _pps, NALUnit marker, ByteBuffer segment,
                H264Handler handler) {
            this.activeSPS = _sps;
            this.activePPS = _pps;
            this.marker = marker;
            this.handler = handler;
            this.segment = segment;
        }

        public void parse() {
            BitReader in = new BitReader(segment);
            SliceHeaderReader shr = new SliceHeaderReader();
            sh = shr.readPart1(in);
            sh.pps = activePPS;
            sh.sps = activeSPS;

            mbWidth = sh.sps.pic_width_in_mbs_minus1 + 1;
            mbHeight = H264Utils.getPicHeightInMbs(sh.sps);

            chromaFormat = sh.sps.chroma_format_idc;
            transform8x8 = sh.pps.extended == null ? false : sh.pps.extended.transform_8x8_mode_flag;

            i4x4PredLeft = new int[4];
            i4x4PredTop = new int[mbWidth << 2];
            topMBType = new MBType[mbWidth];

            topCBPLuma = new int[mbWidth];
            topCBPChroma = new int[mbWidth];

            if (sh.num_ref_idx_active_override_flag)
                numRef = new int[] { sh.num_ref_idx_active_minus1[0] + 1, sh.num_ref_idx_active_minus1[1] + 1 };
            else
                numRef = new int[] { activePPS.num_ref_idx_active_minus1[0] + 1,
                        activePPS.num_ref_idx_active_minus1[1] + 1 };

            mvTop = new int[2][(mbWidth << 2) + 1][3];
            mvLeft = new int[2][4][3];
            mvTopLeft = new int[2][4][3];

            predModeLeft = new PartPred[2];
            predModeTop = new PartPred[mbWidth << 1];

            tf8x8Top = new boolean[mbWidth];

            shr.readPart2(sh, marker, sh.sps, sh.pps, in);

            mapper = new MapManager(sh.sps, sh.pps).getMapper(sh);

            qp = activePPS.pic_init_qp_minus26 + 26 + sh.slice_qp_delta;

            handler.slice(sh);

            parseSliceData(sh, in);
        }

        protected MBType decodeMacroblock(SliceType sliceType, int mbIdx, boolean field, MBType prevMbType) {
            // //System.err.println("\n=============== " + mbIdx +
            // " ==================\n");
            if (sliceType == SliceType.I) {
                return decodeMBlockI(mbIdx, field, prevMbType);
            } else {
                return decodeMBlockP(sliceType, mbIdx, field, prevMbType);
            }
        }

        protected MBType decodeMBlockP(SliceType sliceType, int mbIdx, boolean field, MBType prevMbType) {

            boolean leftAvailable = mapper.leftAvailable(mbIdx);
            boolean topAvailable = mapper.topAvailable(mbIdx);
            boolean tlAvailable = mapper.topLeftAvailable(mbIdx);
            boolean trAvailable = mapper.topRightAvailable(mbIdx);

            MBType mbType;

            if (sliceType == P) {
                int mbt = readMBTypeP(mbIdx);
                switch (mbt) {
                case 0:
                    mbType = MBType.P_16x16;
                    mvs16x16(L0, mbIdx, leftAvailable, topAvailable, tlAvailable, trAvailable, prevMbType, mbType);
                    break;
                case 1:
                    mbType = MBType.P_16x8;
                    mvs16x8(L0, L0, mbIdx, leftAvailable, topAvailable, tlAvailable, trAvailable, prevMbType, mbType);
                    break;
                case 2:
                    mbType = MBType.P_8x16;
                    mvs8x16(L0, L0, mbIdx, leftAvailable, topAvailable, tlAvailable, trAvailable, prevMbType, mbType);
                    break;
                case 3:
                    mbType = MBType.P_8x8;
                    mvs8x8P(false, mbIdx, leftAvailable, topAvailable, tlAvailable, trAvailable, prevMbType, mbType);
                    break;
                case 4:
                    mbType = MBType.P_8x8ref0;
                    mvs8x8P(true, mbIdx, leftAvailable, topAvailable, tlAvailable, trAvailable, prevMbType, mbType);
                    break;
                default:
                    return decodeMBlockIInt(mbIdx, prevMbType, mbt - 5);
                }
            } else {
                int mbt = readMBTypeB(mbIdx);

                if (mbt >= 23) {
                    return decodeMBlockIInt(mbIdx, prevMbType, mbt - 23);
                } else {
                    mbType = H264Const.bMbTypes[mbt];

                    if (mbt == 0) {
                        mvsDirect(mbIdx, leftAvailable, topAvailable, prevMbType, mbType);
                    } else if (mbt <= 3)
                        mvs16x16(H264Const.bPredModes[mbt][0], mbIdx, leftAvailable, topAvailable, tlAvailable,
                                trAvailable, prevMbType, mbType);
                    else if (mbt == 22)
                        mvs8x8B(mbIdx, leftAvailable, topAvailable, tlAvailable, trAvailable, prevMbType, mbType);
                    else if ((mbt & 1) == 0)
                        mvs16x8(H264Const.bPredModes[mbt][0], H264Const.bPredModes[mbt][1], mbIdx, leftAvailable,
                                topAvailable, tlAvailable, trAvailable, prevMbType, mbType);
                    else
                        mvs8x16(H264Const.bPredModes[mbt][0], H264Const.bPredModes[mbt][1], mbIdx, leftAvailable,
                                topAvailable, tlAvailable, trAvailable, prevMbType, mbType);
                }
            }
            int mbX = mapper.getMbX(mbIdx);
            topMBType[mbX] = leftMBType = mbType;

            return mbType;
        }

        private int[][] readInterResidual(MBType prevMbType, int mbX, boolean leftAvailable, boolean topAvailable,
                MBType mbType, int[][] chromaDC, int[][][] chromaAC) {
            int codedBlockPattern = readCodedBlockPatternInter(leftAvailable, topAvailable, leftCBPLuma
                    | (leftCBPChroma << 4), topCBPLuma[mbX] | (topCBPChroma[mbX] << 4), leftMBType, topMBType[mbX]);
            int cbpLuma = codedBlockPattern & 0xf;
            int cbpChroma = codedBlockPattern >> 4;

            boolean transform8x8Used = false;
            if (cbpLuma != 0 && transform8x8) {
                transform8x8Used = readTransform8x8Flag(leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                        tf8x8Left, tf8x8Top[mbX]);
            }

            if (cbpLuma > 0 || cbpChroma > 0) {
                int mbQpDelta = readMBQpDelta(prevMbType);
                qp = (qp + mbQpDelta + 52) % 52;
            }

            int[][] lumaResidual;
            if (!transform8x8Used) {
                lumaResidual = residualLuma(leftAvailable, topAvailable, mbX, codedBlockPattern, mbType);
            } else
                lumaResidual = residualLuma8x8(leftAvailable, topAvailable, mbX, codedBlockPattern, mbType, tf8x8Left,
                        tf8x8Top[mbX]);

            decodeChroma(cbpChroma, mbX, leftAvailable, topAvailable, mbType, chromaDC, chromaAC);

            topCBPLuma[mbX] = leftCBPLuma = cbpLuma;
            topCBPChroma[mbX] = leftCBPChroma = cbpChroma;
            tf8x8Left = tf8x8Top[mbX] = transform8x8Used;
            return lumaResidual;
        }

        private void mvsDirect(int mbIdx, boolean mbA, boolean mbB, MBType prevMbType, MBType mbType) {
            int[][] chromaDC = new int[2][];
            int[][][] chromaAC = new int[2][][];
            int mbX = mapper.getMbX(mbIdx);

            int[][] lumaResidual = readInterResidual(prevMbType, mbX, mbA, mbB, mbType, chromaDC, chromaAC);

            handler.mblockBDirect(mbIdx, qp, lumaResidual, chromaDC, chromaAC);
        }

        private void mvs8x8P(boolean ref0, int mbIdx, boolean mbA, boolean mbB, boolean mbD, boolean mbC,
                MBType prevMbType, MBType mbType) {
            int mbX = mapper.getMbX(mbIdx);
            int[] subMbTypes = new int[4];
            for (int i = 0; i < 4; i++) {
                subMbTypes[i] = readSubMBTypeP();
            }
            int blk8x8X = mbX << 1;

            int[][][] mvs = new int[2][4][12];

            int[] refIdx = new int[4];
            if (numRef[0] > 1 && !ref0) {
                refIdx[0] = readRefIdx(mbA, mbB, leftMBType, topMBType[mbX], L0, L0, L0, mbX, 0, 0, 2, 2, 0);
                refIdx[1] = readRefIdx(true, mbB, P_8x8, topMBType[mbX], L0, L0, L0, mbX, 2, 0, 2, 2, 0);
                refIdx[2] = readRefIdx(mbA, true, leftMBType, P_8x8, L0, L0, L0, mbX, 0, 2, 2, 2, 0);
                refIdx[3] = readRefIdx(true, true, P_8x8, P_8x8, L0, L0, L0, mbX, 2, 2, 2, 2, 0);
            }

            decodeSubMb8x8(subMbTypes[0], mbD, mbB, mbC, mbA, refIdx[0], 0, 0, mbX, P_8x8, L0, L0, L0, 0, mvs[0][0]);

            decodeSubMb8x8(subMbTypes[1], mbD, mbB, mbC, mbA, refIdx[1], 2, 0, mbX, P_8x8, L0, L0, L0, 0, mvs[0][1]);

            decodeSubMb8x8(subMbTypes[2], mbD, mbB, mbC, mbA, refIdx[2], 0, 2, mbX, P_8x8, L0, L0, L0, 0, mvs[0][2]);

            decodeSubMb8x8(subMbTypes[3], mbD, mbB, mbC, mbA, refIdx[3], 2, 2, mbX, P_8x8, L0, L0, L0, 0, mvs[0][3]);

            predModeLeft[0] = predModeLeft[1] = predModeTop[blk8x8X] = predModeTop[blk8x8X + 1] = L0;

            int[][] chromaDC = new int[2][];
            int[][][] chromaAC = new int[2][][];

            int[][] lumaResidual = readInterResidual(prevMbType, mbX, mbA, mbB, mbType, chromaDC, chromaAC);

            handler.mblockPB8x8(mbIdx, qp, lumaResidual, chromaDC, chromaAC, mvs, subMbTypes, L0, L0, L0, L0);
        }

        private boolean tlAvb(int blkX, int blkY, boolean tlAvb, boolean lAvb, boolean tAvb) {
            return blkX == 0 && blkY == 0 ? tlAvb : (blkX == 0 ? lAvb : (blkY == 0 ? tAvb : true));
        }

        private boolean trAvb(int blkX, int blkY, int blkW, boolean trAvb, boolean tAvb) {
            int blkI = (blkY << 2) + blkX + blkW - 1;
            boolean[] arr = { tAvb, tAvb, tAvb, trAvb, true, false, true, false, true, true, true, false, true, false,
                    true, false };

            return arr[blkI];
        }

        private void decodeSubMb8x8(int subMbType, boolean mbD, boolean mbB, boolean mbC, boolean mbA, int refIdx,
                int blk8x8X, int blk8x8Y, int mbX, MBType curMBType, PartPred leftPred, PartPred topPred,
                PartPred partPred, int list, int[] out) {

            switch (subMbType) {
            case 3:
                decodeSub(mbD, mbB, mbC, mbA, refIdx, blk8x8X, blk8x8Y, 1, 1, mbX, curMBType, leftPred, topPred,
                        partPred, list, out, 0);
                decodeSub(mbD, mbB, mbC, mbA, refIdx, blk8x8X + 1, blk8x8Y, 1, 1, mbX, curMBType, leftPred, topPred,
                        partPred, list, out, 3);
                decodeSub(mbD, mbB, mbC, mbA, refIdx, blk8x8X, blk8x8Y + 1, 1, 1, mbX, curMBType, leftPred, topPred,
                        partPred, list, out, 6);
                decodeSub(mbD, mbB, mbC, mbA, refIdx, blk8x8X + 1, blk8x8Y + 1, 1, 1, mbX, curMBType, leftPred,
                        topPred, partPred, list, out, 9);
                break;
            case 2:
                decodeSub(mbD, mbB, mbC, mbA, refIdx, blk8x8X, blk8x8Y, 1, 2, mbX, curMBType, leftPred, topPred,
                        partPred, list, out, 0);
                decodeSub(mbD, mbB, mbC, mbA, refIdx, blk8x8X + 1, blk8x8Y, 1, 2, mbX, curMBType, leftPred, topPred,
                        partPred, list, out, 3);
                break;
            case 1:
                decodeSub(mbD, mbB, mbC, mbA, refIdx, blk8x8X, blk8x8Y, 2, 1, mbX, curMBType, leftPred, topPred,
                        partPred, list, out, 0);
                decodeSub(mbD, mbB, mbC, mbA, refIdx, blk8x8X, blk8x8Y + 1, 2, 1, mbX, curMBType, leftPred, topPred,
                        partPred, list, out, 3);
                break;
            case 0:
                decodeSub(mbD, mbB, mbC, mbA, refIdx, blk8x8X, blk8x8Y, 2, 2, mbX, curMBType, leftPred, topPred,
                        partPred, list, out, 0);
            }
        }

        private void decodeSub(boolean mbD, boolean mbB, boolean mbC, boolean mbA, int refIdx, int blkX, int blkY,
                int blkW, int blkH, int mbX, MBType curMBType, PartPred leftPred, PartPred topPred, PartPred partPred,
                int list, int[] out, int offOut) {

            boolean aAvb = blkX == 0 ? mbA : true;
            boolean bAvb = blkY == 0 ? mbB : true;
            boolean cAvb = trAvb(blkX, blkY, blkW, mbC, mbB);
            boolean dAvb = tlAvb(blkX, blkY, mbD, mbA, mbB);

            int mvdX = readMVD(0, aAvb, bAvb, blkX == 0 ? leftMBType : curMBType, blkY == 0 ? topMBType[mbX]
                    : curMBType, leftPred, topPred, partPred, mbX, blkX, blkY, 2, 2, list);
            int mvdY = readMVD(1, aAvb, bAvb, blkX == 0 ? leftMBType : curMBType, blkY == 0 ? topMBType[mbX]
                    : curMBType, leftPred, topPred, partPred, mbX, blkX, blkY, 2, 2, list);

            int blk8x8AbsX = (mbX << 2) + blkX;
            int[] l = mvLeft[list][blkY];
            int[] t = mvTop[list][blk8x8AbsX];
            int[] tr = cAvb ? mvTop[list][blk8x8AbsX + blkW] : null;
            int[] tl = dAvb ? mvTopLeft[list][blkY] : null;

            int mvX = out[offOut] = mvdX + calcMVPredictionMedian(l, t, tr, tl, aAvb, bAvb, cAvb, dAvb, refIdx, 0);
            int mvY = out[offOut + 1] = mvdY + calcMVPredictionMedian(l, t, tr, tl, aAvb, bAvb, cAvb, dAvb, refIdx, 1);
            out[offOut + 2] = refIdx;

            copyVect(mvTopLeft[list][blkY], mvTop[list][blk8x8AbsX + blkW - 1]);
            saveVect(mvTopLeft[list], blkY + 1, blkY + blkH, mvX, mvY, refIdx);
            saveVect(mvLeft[list], blkY, blkY + blkH, mvX, mvY, refIdx);
            saveVect(mvTop[list], blk8x8AbsX, blk8x8AbsX + blkW, mvX, mvY, refIdx);
        }

        private void mvs8x8B(int mbIdx, boolean mbA, boolean mbB, boolean mbD, boolean mbC, MBType prevMbType,
                MBType mbType) {
            int mbX = mapper.getMbX(mbIdx);

            int s0 = readSubMBTypeB(), s1 = readSubMBTypeB(), s2 = readSubMBTypeB(), s3 = readSubMBTypeB();
            PartPred p0 = bPartPredModes[s0], p1 = bPartPredModes[s1], p2 = bPartPredModes[s2], p3 = bPartPredModes[s3];
            int[][][] mvs = new int[2][4][];

            int[][] refIdx = new int[2][4];
            for (int list = 0; list < 2; list++) {
                if (numRef[list] <= 1)
                    continue;
                if (p0.usesList(list))
                    refIdx[list][0] = readRefIdx(mbA, mbB, leftMBType, topMBType[mbX], predModeLeft[0],
                            predModeTop[mbX << 1], p0, mbX, 0, 0, 2, 2, list);
                if (p1.usesList(list))
                    refIdx[list][1] = readRefIdx(true, mbB, B_8x8, topMBType[mbX], p0, predModeTop[(mbX << 1) + 1], p1,
                            mbX, 2, 0, 2, 2, list);
                if (p2.usesList(list))
                    refIdx[list][2] = readRefIdx(mbA, true, leftMBType, B_8x8, predModeLeft[1], p0, p2, mbX, 0, 2, 2,
                            2, list);
                if (p3.usesList(list))
                    refIdx[list][3] = readRefIdx(true, true, B_8x8, B_8x8, p2, p1, p3, mbX, 2, 2, 2, 2, list);
            }

            int blk8x8X = mbX << 1;
            for (int list = 0; list < 2; list++) {
                if (p0.usesList(list))
                    decodeSubMb8x8(bSubMbTypes[s0], mbD, mbB, mbC, mbA, refIdx[list][0], 0, 0, mbX, B_8x8,
                            predModeLeft[0], predModeTop[blk8x8X], p0, list, mvs[list][0]);

                if (p1.usesList(list))
                    decodeSubMb8x8(bSubMbTypes[s1], mbD, mbB, mbC, mbA, refIdx[list][1], 2, 0, mbX, B_8x8, p0,
                            predModeTop[blk8x8X + 1], p1, list, mvs[list][1]);

                if (p2.usesList(list))
                    decodeSubMb8x8(bSubMbTypes[s2], mbD, mbB, mbC, mbA, refIdx[list][2], 0, 2, mbX, B_8x8,
                            predModeLeft[1], p0, p2, list, mvs[list][2]);

                if (p3.usesList(list))
                    decodeSubMb8x8(bSubMbTypes[s3], mbD, mbB, mbC, mbA, refIdx[list][3], 2, 2, mbX, B_8x8, p2, p1, p3,
                            list, mvs[list][3]);
            }

            predModeLeft[0] = p1;
            predModeTop[blk8x8X] = p2;
            predModeLeft[1] = predModeTop[blk8x8X + 1] = p3;

            int[][] chromaDC = new int[2][];
            int[][][] chromaAC = new int[2][][];

            int[][] lumaResidual = readInterResidual(prevMbType, mbX, mbA, mbB, mbType, chromaDC, chromaAC);

            handler.mblockPB8x8(mbIdx, qp, lumaResidual, chromaDC, chromaAC, mvs, bSubMbTypes, p0, p1, p2, p3);
        }

        private void mvs8x16(PartPred p0, PartPred p1, int mbIdx, boolean leftAvailable, boolean topAvailable,
                boolean tlAvailable, boolean trAvailable, MBType prevMbType, MBType mbType) {
            int mbX = mapper.getMbX(mbIdx);
            int xx = mbX << 2;

            int blk8x8X = (mbX << 1);
            int[][] mvs = new int[2][6];

            for (int list = 0; list < 2; list++) {
                mvs[list][2] = p0.usesList(list) ? (numRef[list] > 1 ? readRefIdx(leftAvailable, topAvailable,
                        leftMBType, topMBType[mbX], predModeLeft[0], predModeTop[mbX << 1], p0, mbX, 0, 0, 2, 4, list)
                        : 0) : -1;
                mvs[list][5] = p1.usesList(list) ? (numRef[list] > 1 ? readRefIdx(true, topAvailable, mbType,
                        topMBType[mbX], p0, predModeTop[(mbX << 1) + 1], p1, mbX, 2, 0, 2, 4, list) : 0) : -1;
            }
            for (int list = 0; list < 2; list++) {

                if (p0.usesList(list)) {
                    int mvdX1 = readMVD(0, leftAvailable, topAvailable, leftMBType, topMBType[mbX], predModeLeft[0],
                            predModeTop[blk8x8X], p0, mbX, 0, 0, 2, 4, list);
                    int mvdY1 = readMVD(1, leftAvailable, topAvailable, leftMBType, topMBType[mbX], predModeLeft[0],
                            predModeTop[blk8x8X], p0, mbX, 0, 0, 2, 4, list);

                    int mvpX1 = calcMVPrediction8x16Left(mvLeft[list][0], mvTop[list][mbX << 2],
                            mvTop[list][(mbX << 2) + 2], mvTopLeft[list][0], leftAvailable, topAvailable, topAvailable,
                            tlAvailable, mvs[list][2], 0);
                    int mvpY1 = calcMVPrediction8x16Left(mvLeft[list][0], mvTop[list][mbX << 2],
                            mvTop[list][(mbX << 2) + 2], mvTopLeft[list][0], leftAvailable, topAvailable, topAvailable,
                            tlAvailable, mvs[list][2], 1);

                    mvs[list][0] = mvdX1 + mvpX1;
                    mvs[list][1] = mvdY1 + mvpY1;
                }
                saveVect(mvLeft[list], 0, 4, mvs[list][0], mvs[list][1], mvs[list][2]);

                if (p1.usesList(list)) {
                    int mvdX2 = readMVD(0, true, topAvailable, mbType, topMBType[mbX], p0, predModeTop[blk8x8X + 1],
                            p1, mbX, 2, 0, 2, 4, list);
                    int mvdY2 = readMVD(1, true, topAvailable, mbType, topMBType[mbX], p0, predModeTop[blk8x8X + 1],
                            p1, mbX, 2, 0, 2, 4, list);

                    int mvpX2 = calcMVPrediction8x16Right(mvLeft[list][0], mvTop[list][(mbX << 2) + 2],
                            mvTop[list][(mbX << 2) + 4], mvTop[list][(mbX << 2) + 1], true, topAvailable, trAvailable,
                            topAvailable, mvs[list][5], 0);
                    int mvpY2 = calcMVPrediction8x16Right(mvLeft[list][0], mvTop[list][(mbX << 2) + 2],
                            mvTop[list][(mbX << 2) + 4], mvTop[list][(mbX << 2) + 1], true, topAvailable, trAvailable,
                            topAvailable, mvs[list][5], 1);

                    mvs[list][3] = mvdX2 + mvpX2;
                    mvs[list][4] = mvdY2 + mvpY2;
                }

                copyVect(mvTopLeft[list][0], mvTop[list][xx + 3]);
                saveVect(mvTopLeft[list], 1, 4, mvs[list][3], mvs[list][4], mvs[list][5]);
                saveVect(mvTop[list], xx, xx + 2, mvs[list][0], mvs[list][1], mvs[list][2]);
                saveVect(mvTop[list], xx + 2, xx + 4, mvs[list][3], mvs[list][4], mvs[list][5]);
                saveVect(mvLeft[list], 0, 4, mvs[list][3], mvs[list][4], mvs[list][5]);
            }

            int[][] chromaDC = new int[2][];
            int[][][] chromaAC = new int[2][][];

            int[][] lumaResidual = readInterResidual(prevMbType, mbX, leftAvailable, topAvailable, mbType, chromaDC,
                    chromaAC);

            handler.mblockPB8x16(mbIdx, qp, lumaResidual, chromaDC, chromaAC, mvs, p0, p1);
        }

        private void mvs16x8(PartPred p0, PartPred p1, int mbIdx, boolean leftAvailable, boolean topAvailable,
                boolean tlAvailable, boolean trAvailable, MBType prevMbType, MBType mbType) {
            int mbX = mapper.getMbX(mbIdx);
            int blk8x8X = (mbX << 1);

            int[][] mvs = new int[2][6];
            for (int list = 0; list < 2; list++) {
                mvs[list][2] = p0.usesList(list) ? (numRef[list] > 1 ? readRefIdx(leftAvailable, topAvailable,
                        leftMBType, topMBType[mbX], predModeLeft[0], predModeTop[(mbX << 1)], p0, mbX, 0, 0, 4, 2, list)
                        : 0)
                        : -1;

                mvs[list][5] = p1.usesList(list) ? (numRef[list] > 1 ? readRefIdx(leftAvailable, true, leftMBType,
                        mbType, predModeLeft[1], p0, p1, mbX, 0, 2, 4, 2, list) : 0) : -1;
            }
            int xx = mbX << 2;

            for (int list = 0; list < 2; list++) {
                if (p0.usesList(list)) {

                    int mvdX1 = readMVD(0, leftAvailable, topAvailable, leftMBType, topMBType[mbX], predModeLeft[0],
                            predModeTop[blk8x8X], p0, mbX, 0, 0, 4, 2, list);
                    int mvdY1 = readMVD(1, leftAvailable, topAvailable, leftMBType, topMBType[mbX], predModeLeft[0],
                            predModeTop[blk8x8X], p0, mbX, 0, 0, 4, 2, list);

                    int mvpX1 = calcMVPrediction16x8Top(mvLeft[list][0], mvTop[list][xx], mvTop[list][(mbX << 2) + 4],
                            mvTopLeft[list][0], leftAvailable, topAvailable, trAvailable, tlAvailable, mvs[list][2], 0);
                    int mvpY1 = calcMVPrediction16x8Top(mvLeft[list][0], mvTop[list][xx], mvTop[list][(mbX << 2) + 4],
                            mvTopLeft[list][0], leftAvailable, topAvailable, trAvailable, tlAvailable, mvs[list][2], 1);

                    mvs[list][0] = mvdX1 + mvpX1;
                    mvs[list][1] = mvdY1 + mvpY1;
                }
                copyVect(mvTopLeft[list][0], mvTop[list][xx + 3]);
                saveVect(mvTopLeft[list], 1, 3, mvs[list][0], mvs[list][1], mvs[list][2]);
                saveVect(mvTop[list], xx, xx + 4, mvs[list][0], mvs[list][1], mvs[list][2]);
                saveVect(mvLeft[list], 0, 2, mvs[list][0], mvs[list][1], mvs[list][2]);

                if (p1.usesList(list)) {
                    int mvdX2 = readMVD(0, leftAvailable, true, leftMBType, MBType.P_16x8, predModeLeft[1], p0, p1,
                            mbX, 0, 2, 4, 2, list);
                    int mvdY2 = readMVD(1, leftAvailable, true, leftMBType, MBType.P_16x8, predModeLeft[1], p0, p1,
                            mbX, 0, 2, 4, 2, list);

                    int mvpX2 = calcMVPrediction16x8Bottom(mvLeft[list][2], mvTop[list][xx], null, mvTopLeft[list][2],
                            leftAvailable, true, false, leftAvailable, mvs[list][5], 0);
                    int mvpY2 = calcMVPrediction16x8Bottom(mvLeft[list][2], mvTop[list][xx], null, mvTopLeft[list][2],
                            leftAvailable, true, false, leftAvailable, mvs[list][5], 1);

                    mvs[list][3] = mvdX2 + mvpX2;
                    mvs[list][4] = mvdY2 + mvpY2;
                }
                saveVect(mvTopLeft[list], 3, 4, mvs[list][3], mvs[list][4], mvs[list][5]);
                saveVect(mvLeft[list], 2, 4, mvs[list][3], mvs[list][4], mvs[list][5]);
                saveVect(mvTop[list], xx, xx + 4, mvs[list][3], mvs[list][4], mvs[list][5]);
            }

            int[][] chromaDC = new int[2][];
            int[][][] chromaAC = new int[2][][];

            int[][] lumaResidual = readInterResidual(prevMbType, mbX, leftAvailable, topAvailable, mbType, chromaDC,
                    chromaAC);

            handler.mblockPB16x8(mbIdx, qp, lumaResidual, chromaDC, chromaAC, mvs, p0, p1);
        }

        private void mvs16x16(PartPred pred, int mbIdx, boolean leftAvailable, boolean topAvailable,
                boolean tlAvailable, boolean trAvailable, MBType prevMbType, MBType mbType) {

            int mbX = mapper.getMbX(mbIdx);
            int mbAddr = mapper.getAddress(mbIdx);
            int blk8x8X = (mbX << 1);
            int[][] mvs = new int[2][3];
            for (int list = 0; list < 2; list++) {
                mvs[list][2] = pred.usesList(list) ? (numRef[list] > 1 ? readRefIdx(leftAvailable, topAvailable,
                        leftMBType, topMBType[mbX], predModeLeft[0], predModeTop[(mbX << 1)], pred, mbX, 0, 0, 4, 4,
                        list) : 0) : -1;
            }

            for (int list = 0; list < 2; list++) {
                int mvX = 0, mvY = 0, r = mvs[list][2];
                if (pred.usesList(list)) {
                    int mvpX = calcMVPredictionMedian(mvLeft[list][0], mvTop[list][mbX << 2],
                            mvTop[list][(mbX << 2) + 4], mvTopLeft[list][0], leftAvailable, topAvailable, trAvailable,
                            tlAvailable, r, 0);
                    int mvpY = calcMVPredictionMedian(mvLeft[list][0], mvTop[list][mbX << 2],
                            mvTop[list][(mbX << 2) + 4], mvTopLeft[list][0], leftAvailable, topAvailable, trAvailable,
                            tlAvailable, r, 1);
                    int mvdX = readMVD(0, leftAvailable, topAvailable, leftMBType, topMBType[mbX], predModeLeft[0],
                            predModeTop[blk8x8X], pred, mbX, 0, 0, 4, 4, list);
                    int mvdY = readMVD(1, leftAvailable, topAvailable, leftMBType, topMBType[mbX], predModeLeft[0],
                            predModeTop[blk8x8X], pred, mbX, 0, 0, 4, 4, list);

                    mvX = mvdX + mvpX;
                    mvY = mvdY + mvpY;

                }
                int xx = mbX << 2;
                copyVect(mvTopLeft[list][0], mvTop[list][xx + 3]);
                saveVect(mvTopLeft[list], 1, 4, mvX, mvY, r);
                saveVect(mvTop[list], xx, xx + 4, mvX, mvY, r);
                saveVect(mvLeft[list], 0, 4, mvX, mvY, r);
                mvs[list][0] = mvX;
                mvs[list][1] = mvY;
            }

            int[][] chromaDC = new int[2][];
            int[][][] chromaAC = new int[2][][];

            int[][] lumaResidual = readInterResidual(prevMbType, mbX, leftAvailable, topAvailable, mbType, chromaDC,
                    chromaAC);

            handler.mblockPB16x16(mbIdx, qp, lumaResidual, chromaDC, chromaAC, mvs, pred);
        }

        protected MBType decodeMBlockI(int mbIdx, boolean field, MBType prevMbType) {

            int mbType = readMBType(mbIdx);

            return decodeMBlockIInt(mbIdx, prevMbType, mbType);
        }

        private MBType decodeMBlockIInt(int mbIdx, MBType prevMbType, int mbType) {
            MBType mbt;
            if (mbType == 0) {
                decodeMBlockIntraNxN(mbIdx, prevMbType);
                mbt = MBType.I_NxN;
            } else if (mbType >= 1 && mbType <= 24) {
                mbType--;
                decodeMBlockIntra16x16(mbType, mbIdx, prevMbType);
                mbt = MBType.I_16x16;
            } else {
                decodeMBlockIPCM(mbIdx);
                mbt = MBType.I_PCM;
            }

            int xx = mapper.getMbX(mbIdx) << 2;

            copyVect(mvTopLeft[0][0], mvTop[0][xx + 3]);
            copyVect(mvTopLeft[1][0], mvTop[1][xx + 3]);

            saveVect(mvTopLeft[0], 1, 4, 0, 0, -1);
            saveVect(mvTop[0], xx, xx + 4, 0, 0, -1);
            saveVect(mvLeft[0], 0, 4, 0, 0, -1);
            saveVect(mvTop[1], xx, xx + 4, 0, 0, -1);
            saveVect(mvLeft[1], 0, 4, 0, 0, -1);
            return mbt;
        }

        protected void decodeMBlockIntraNxN(int mbIndex, MBType prevMbType) {
            int mbX = mapper.getMbX(mbIndex);
            int mbY = mapper.getMbY(mbIndex);
            boolean leftAvailable = mapper.leftAvailable(mbIndex);
            boolean topAvailable = mapper.topAvailable(mbIndex);
            
            boolean transform8x8Used = false;
            if (transform8x8) {
                transform8x8Used = readTransform8x8Flag(leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                        tf8x8Left, tf8x8Top[mbX]);
            }

            int[] lumaModes;
            if (!transform8x8Used) {
                lumaModes = new int[16];
                for (int i = 0; i < 16; i++) {
                    int blkX = H264Const.MB_BLK_OFF_LEFT[i];
                    int blkY = H264Const.MB_BLK_OFF_TOP[i];
                    lumaModes[i] = readPredictionI4x4Block(leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                            blkX, blkY, mbX);
                }
            } else {
                lumaModes = new int[4];
                for (int i = 0; i < 4; i++) {
                    int blkX = (i & 1) << 1;
                    int blkY = i & 2;
                    lumaModes[i] = readPredictionI4x4Block(leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                            blkX, blkY, mbX);
                    i4x4PredLeft[blkY + 1] = i4x4PredLeft[blkY];
                    i4x4PredTop[(mbX << 2) + blkX + 1] = i4x4PredTop[(mbX << 2) + blkX];
                }
            }
            int chromaMode = readChromaPredMode(mbX, leftAvailable, topAvailable);

            int codedBlockPattern = readCodedBlockPatternIntra(leftAvailable, topAvailable, leftCBPLuma
                    | (leftCBPChroma << 4), topCBPLuma[mbX] | (topCBPChroma[mbX] << 4), leftMBType, topMBType[mbX]);

            int cbpLuma = codedBlockPattern & 0xf;
            int cbpChroma = codedBlockPattern >> 4;

            if (cbpLuma > 0 || cbpChroma > 0) {
                qp = (qp + readMBQpDelta(prevMbType) + 52) % 52;
            }

            int[][] lumaResidual;
            if (!transform8x8Used) {
                lumaResidual = residualLuma(leftAvailable, topAvailable, mbX, codedBlockPattern, MBType.I_NxN);
            } else
                lumaResidual = residualLuma8x8(leftAvailable, topAvailable, mbX, codedBlockPattern, MBType.I_NxN,
                        tf8x8Left, tf8x8Top[mbX]);

            int[][] chromaDC = new int[2][];
            int[][][] chromaAC = new int[2][][];
            decodeChroma(cbpChroma, mbX, leftAvailable, topAvailable, MBType.I_NxN, chromaDC, chromaAC);

            handler.macroblockINxN(mbIndex, qp, lumaResidual, chromaDC, chromaAC, lumaModes, chromaMode);

            // System.err.println("idx: " + mbIndex + ", addr: " + address);
            topCBPLuma[mbX] = leftCBPLuma = cbpLuma;
            topCBPChroma[mbX] = leftCBPChroma = cbpChroma;
            tf8x8Left = tf8x8Top[mbX] = transform8x8Used;
            topMBType[mbX] = leftMBType = MBType.I_NxN;

        }

        public void decodeChroma(int pattern, int mbX, boolean leftAvailable, boolean topAvailable, MBType curMbType,
                int[][] chromaDC, int[][][] chromaAC) {

            if (chromaFormat == MONO)
                return;

            if (pattern != 0) {
                if ((pattern & 3) > 0) {
                    chromaDC[0] = chromaDC(mbX, leftAvailable, topAvailable, 1, curMbType);
                    chromaDC[1] = chromaDC(mbX, leftAvailable, topAvailable, 2, curMbType);
                }
                chromaAC[0] = chromaAC(leftAvailable, topAvailable, mbX, 1, curMbType, (pattern & 2) > 0);
                chromaAC[1] = chromaAC(leftAvailable, topAvailable, mbX, 2, curMbType, (pattern & 2) > 0);
            } else {
                updatePrevCBP(mbX);
            }
        }

        private int readPredictionI4x4Block(boolean leftAvailable, boolean topAvailable, MBType leftMBType,
                MBType topMBType, int blkX, int blkY, int mbX) {
            
            leftAvailable = leftAvailable && (leftMBType != null && leftMBType.intra || !activePPS.constrained_intra_pred_flag);
            topAvailable = topAvailable && (topMBType != null && topMBType.intra || !activePPS.constrained_intra_pred_flag);

            int mode = 2;
            if ((leftAvailable || blkX > 0) && (topAvailable || blkY > 0)) {
                int predModeB = topMBType == MBType.I_NxN || blkY > 0 ? i4x4PredTop[(mbX << 2) + blkX] : 2;
                int predModeA = leftMBType == MBType.I_NxN || blkX > 0 ? i4x4PredLeft[blkY] : 2;
                mode = Math.min(predModeB, predModeA);
            }
            if (!readPrev4x4PredMode()) {
                int rem_intra4x4_pred_mode = readRem4x4PredMode();
                mode = rem_intra4x4_pred_mode + (rem_intra4x4_pred_mode < mode ? 0 : 1);
            }
            i4x4PredTop[(mbX << 2) + blkX] = i4x4PredLeft[blkY] = mode;
            return mode;
        }

        protected void decodeMBlockIntra16x16(int mbType, int mbIndex, MBType prevMbType) {

            int mbX = mapper.getMbX(mbIndex);
            int mbY = mapper.getMbY(mbIndex);

            int cbpChroma = (mbType / 4) % 3;
            int cbpLuma = (mbType / 12) * 15;

            boolean leftAvailable = mapper.leftAvailable(mbIndex);
            boolean topAvailable = mapper.topAvailable(mbIndex);

            int chromaPredictionMode = readChromaPredMode(mbX, leftAvailable, topAvailable);
            int mbQPDelta = readMBQpDelta(prevMbType);
            qp = (qp + mbQPDelta + 52) % 52;

            int[] lumaDC = residualLumaI16x16DC(leftAvailable, topAvailable, mbX, mbY, cbpLuma);
            int[][] lumaAC = residualLumaI16x16AC(leftAvailable, topAvailable, mbX, mbY, cbpLuma);

            int[][] chromaDC = new int[2][];
            int[][][] chromaAC = new int[2][][];
            decodeChroma(cbpChroma, mbX, leftAvailable, topAvailable, MBType.I_16x16, chromaDC, chromaAC);

            handler.macroblockI16x16(mbIndex, qp, lumaDC, lumaAC, mbType % 4, chromaPredictionMode, chromaDC, chromaAC);

            topMBType[mbX] = leftMBType = MBType.I_16x16;
            topCBPLuma[mbX] = leftCBPLuma = cbpLuma;
            topCBPChroma[mbX] = leftCBPChroma = cbpChroma;
            tf8x8Left = tf8x8Top[mbX] = false;
        }

        protected void decodeSkip(int mbIdx, SliceType sliceType) {
            int mbX = mapper.getMbX(mbIdx);
            int mbY = mapper.getMbY(mbIdx);

            if (sliceType == P) {
                predictPSkip(mbIdx);
            } else {
                predictBDirect(mbX, mbY, mapper.leftAvailable(mbIdx), mapper.topAvailable(mbIdx),
                        mapper.topLeftAvailable(mbIdx), mapper.topRightAvailable(mbIdx));
            }

            topMBType[mbX] = leftMBType = null;
        }

        private void predictBDirect(int mbX, int mbY, boolean leftAvailable, boolean topAvailable,
                boolean topLeftAvailable, boolean topRightAvailable) {
            throw new RuntimeException("AAAAAAAAAAAAAAAAAAAAAAAAAAAA!!!!!!!!!!!!!!!!!!!!");
        }

        public void predictPSkip(int mbIdx) {
            int mbX = mapper.getMbX(mbIdx);
            boolean lAvb = mapper.leftAvailable(mbIdx);
            boolean tAvb = mapper.topAvailable(mbIdx);
            boolean tlAvb = mapper.topLeftAvailable(mbIdx);
            boolean trAvb = mapper.topRightAvailable(mbIdx);

            int mvX = 0, mvY = 0;
            if (lAvb && tAvb) {
                int[] b = mvTop[0][mbX << 2];
                int[] a = mvLeft[0][0];

                if ((a[0] != 0 || a[1] != 0 || a[2] != 0) && (b[0] != 0 || b[1] != 0 || b[2] != 0)) {
                    mvX = calcMVPredictionMedian(a, b, mvTop[0][(mbX << 2) + 4], mvTopLeft[0][0], lAvb, tAvb, trAvb,
                            tlAvb, 0, 0);
                    mvY = calcMVPredictionMedian(a, b, mvTop[0][(mbX << 2) + 4], mvTopLeft[0][0], lAvb, tAvb, trAvb,
                            tlAvb, 0, 1);
                }
            }
            int blk8x8X = mbX << 1;
            predModeLeft[0] = predModeLeft[1] = predModeTop[blk8x8X] = predModeTop[blk8x8X + 1] = L0;

            int xx = mbX << 2;
            copyVect(mvTopLeft[0][0], mvTop[0][xx + 3]);
            saveVect(mvTopLeft[0], 1, 4, mvX, mvY, 0);
            saveVect(mvTop[0], xx, xx + 4, mvX, mvY, 0);
            saveVect(mvLeft[0], 0, 4, mvX, mvY, 0);

            handler.mblockPBSkip(mbIdx, mvX, mvY);
        }

        protected void decodeMBlockIPCM(int mbIdx) {
            throw new RuntimeException("IPCM");
        }

        protected abstract int[] residualLumaI16x16DC(boolean leftAvailable, boolean topAvailable, int mbX, int mbY,
                int cbpLuma);

        protected abstract int[][] residualLumaI16x16AC(boolean leftAvailable, boolean topAvailable, int mbX, int mbY,
                int cbpLuma);

        protected abstract int[][] chromaAC(boolean leftAvailable, boolean topAvailable, int mbX, int comp,
                MBType curMbType, boolean codedAC);

        protected abstract int[] chromaDC(int mbX, boolean leftAvailable, boolean topAvailable, int comp,
                MBType curMbType);

        protected abstract int readMBQpDelta(MBType prevMbType);

        protected abstract int readMBType(int mbIdx);

        protected abstract boolean readMBFieldDecodingFlag();

        protected abstract void parseSliceData(SliceHeader sh, BitReader in);

        protected abstract boolean readTransform8x8Flag(boolean leftAvailable, boolean topAvailable, MBType leftMBType,
                MBType topMBType, boolean tf8x8Left, boolean tf8x8Top);

        protected abstract int readRem4x4PredMode();

        protected abstract boolean readPrev4x4PredMode();

        protected abstract int readChromaPredMode(int mbX, boolean leftAvailable, boolean topAvailable);

        protected abstract int readCodedBlockPatternIntra(boolean leftAvailable, boolean topAvailable, int leftCBP,
                int topCBP, MBType leftMB, MBType topMB);

        protected abstract int[][] residualLuma8x8(boolean leftAvailable, boolean topAvailable, int mbX,
                int codedBlockPattern, MBType mbType, boolean tf8x8Left, boolean tf8x8Top);

        protected abstract int[][] residualLuma(boolean leftAvailable, boolean topAvailable, int mbX,
                int codedBlockPattern, MBType mbType);

        protected abstract int readRefIdx(boolean leftAvailable, boolean topAvailable, MBType leftMBType,
                MBType topMBType, PartPred predModeLeft, PartPred predModeTop, PartPred pred, int mbX, int partX,
                int partY, int partW, int partH, int list);

        protected abstract int readMVD(int comp, boolean leftAvailable, boolean topAvailable, MBType leftMBType,
                MBType topMBType, PartPred leftPred, PartPred topPred, PartPred pred, int mbX, int partX, int partY,
                int partW, int partH, int list);

        protected abstract void updatePrevCBP(int mbX);

        protected abstract int readCodedBlockPatternInter(boolean leftAvailable, boolean topAvailable, int leftCBP,
                int topCBP, MBType leftMB, MBType topMB);

        protected abstract int readSubMBTypeP();

        protected abstract int readSubMBTypeB();

        protected abstract int readMBTypeB(int mbIdx);

        protected abstract int readMBTypeP(int mbIdx);
    }
}