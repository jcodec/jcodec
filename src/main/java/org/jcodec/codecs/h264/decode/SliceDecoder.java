package org.jcodec.codecs.h264.decode;

import static org.jcodec.codecs.h264.H264Const.PartPred.L0;
import static org.jcodec.codecs.h264.decode.CAVLCReader.moreRBSPData;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readBool;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readUE;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.debugPrint;
import static org.jcodec.codecs.h264.io.model.MBType.P_16x16;
import static org.jcodec.codecs.h264.io.model.MBType.P_16x8;
import static org.jcodec.codecs.h264.io.model.MBType.P_8x16;
import static org.jcodec.codecs.h264.io.model.SliceType.P;

import java.nio.ByteBuffer;

import org.jcodec.codecs.common.biari.MDecoder;
import org.jcodec.codecs.h264.H264Const;
import org.jcodec.codecs.h264.decode.aso.MapManager;
import org.jcodec.codecs.h264.decode.aso.Mapper;
import org.jcodec.codecs.h264.io.CABAC;
import org.jcodec.codecs.h264.io.CAVLC;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.common.IntObjectMap;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.Picture8Bit;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A decoder for an individual slice
 * 
 * @author The JCodec project
 * 
 */
public class SliceDecoder {

    private CAVLC[] cavlc;
    private CABAC cabac;
    private Mapper mapper;

    private MBlockDecoderIntra16x16 decoderIntra16x16;
    private MBlockDecoderIntraNxN decoderIntraNxN;
    private MBlockDecoderInter decoderInter;
    private MBlockDecoderInter8x8 decoderInter8x8;
    private MBlockSkipDecoder skipDecoder;
    private MBlockDecoderBDirect decoderBDirect;
    private RefListManager refListManager;
    private MBlockDecoderIPCM decoderIPCM;
    private BitstreamParser parser;
    private SeqParameterSet activeSps;
    private PictureParameterSet activePps;
    private Frame frameOut;
    private DecoderState decoderState;
    private DeblockerInput di;
    private IntObjectMap<Frame> lRefs;
    private Frame[] sRefs;

    public SliceDecoder(SeqParameterSet activeSps, PictureParameterSet activePps, Frame[] sRefs,
            IntObjectMap<Frame> lRefs, DeblockerInput di, Frame result) {
        this.di = di;
        this.activeSps = activeSps;
        this.activePps = activePps;
        this.frameOut = result;
        this.sRefs = sRefs;
        this.lRefs = lRefs;
    }

    public void decode(ByteBuffer segment, NALUnit nalUnit) {
        BitReader in = new BitReader(segment);
        SliceHeaderReader shr = new SliceHeaderReader();
        SliceHeader sh = shr.readPart1(in);
        sh.sps = activeSps;
        sh.pps = activePps;
        shr.readPart2(sh, nalUnit, sh.sps, sh.pps, in);

        initContext(sh, segment, in);

        debugPrint("============" + frameOut.getPOC() + "============= " + sh.slice_type.name());

        Frame[][] refList = refListManager.getRefList();

        decodeMacroblocks(sh, in, refList);
    }

    private void initContext(SliceHeader sh, ByteBuffer segment, BitReader in) {
        cavlc = new CAVLC[] { new CAVLC(sh.sps, sh.pps, 2, 2), new CAVLC(sh.sps, sh.pps, 1, 1),
                new CAVLC(sh.sps, sh.pps, 1, 1) };

        int mbWidth = sh.sps.pic_width_in_mbs_minus1 + 1;
        cabac = new CABAC(mbWidth);

        decoderState = new DecoderState(sh);

        MDecoder mDecoder = null;
        if (activePps.entropy_coding_mode_flag) {
            in.terminate();
            int[][] cm = new int[2][1024];
            cabac.initModels(cm, sh.slice_type, sh.cabac_init_idc, decoderState.qp);
            mDecoder = new MDecoder(segment, cm);
        }

        mapper = new MapManager(sh.sps, sh.pps).getMapper(sh);

        parser = new BitstreamParser(activePps, cabac, cavlc, mDecoder, in, di, decoderState);

        decoderIntra16x16 = new MBlockDecoderIntra16x16(mapper, parser, sh, di, frameOut.getPOC(), decoderState);
        decoderIntraNxN = new MBlockDecoderIntraNxN(mapper, parser, sh, di, frameOut.getPOC(), decoderState);
        decoderInter = new MBlockDecoderInter(mapper, parser, sh, di, frameOut.getPOC(), decoderState);
        decoderBDirect = new MBlockDecoderBDirect(mapper, parser, sh, di, frameOut.getPOC(), decoderState);
        decoderInter8x8 = new MBlockDecoderInter8x8(mapper, parser, decoderBDirect, sh, di, frameOut.getPOC(),
                decoderState);
        skipDecoder = new MBlockSkipDecoder(mapper, parser, decoderBDirect, sh, di, frameOut.getPOC(), decoderState);
        decoderIPCM = new MBlockDecoderIPCM(mapper, in, decoderState);

        refListManager = new RefListManager(sh, sRefs, lRefs, frameOut);

    }

    private void decodeMacroblocks(SliceHeader sh, BitReader in, Frame[][] refList) {
        Picture8Bit mb = Picture8Bit.create(16, 16, sh.sps.chroma_format_idc);
        int mbWidth1 = sh.sps.pic_width_in_mbs_minus1 + 1;
        boolean mbaffFrameFlag = (sh.sps.mb_adaptive_frame_field_flag && !sh.field_pic_flag);

        boolean prevMbSkipped = false;
        int i;
        MBType prevMBType = null;
        for (i = 0;; i++) {
            if (sh.slice_type.isInter() && !activePps.entropy_coding_mode_flag) {
                int mbSkipRun = readUE(in, "mb_skip_run");
                for (int j = 0; j < mbSkipRun; j++, i++) {
                    int mbAddr = mapper.getAddress(i);
                    debugPrint("---------------------- MB (" + (mbAddr % mbWidth1) + "," + (mbAddr / mbWidth1)
                            + ") ---------------------");
                    skipDecoder.decodeSkip(refList, i, mb, sh.slice_type);
                    di.shs[mbAddr] = sh;
                    di.refsUsed[mbAddr] = refList;
                    putMacroblock(frameOut, mb, mapper.getMbX(i), mapper.getMbY(i));
                    mb.fill(0);
                }

                prevMbSkipped = mbSkipRun > 0;
                prevMBType = null;

                if (!moreRBSPData(in))
                    break;
            }

            int mbAddr = mapper.getAddress(i);
            di.shs[mbAddr] = sh;
            di.refsUsed[mbAddr] = refList;
            int mbX = mbAddr % mbWidth1;
            int mbY = mbAddr / mbWidth1;
            debugPrint("---------------------- MB (" + mbX + "," + mbY + ") ---------------------");

            if (sh.slice_type.isIntra()
                    || (!activePps.entropy_coding_mode_flag || !parser.readMBSkipFlag(sh.slice_type,
                            mapper.leftAvailable(i), mapper.topAvailable(i), mbX))) {

                boolean mb_field_decoding_flag = false;
                if (mbaffFrameFlag && (i % 2 == 0 || (i % 2 == 1 && prevMbSkipped))) {
                    mb_field_decoding_flag = readBool(in, "mb_field_decoding_flag");
                }

                prevMBType = decode(sh.slice_type, i, mb_field_decoding_flag, prevMBType, mb, refList);

            } else {
                skipDecoder.decodeSkip(refList, i, mb, sh.slice_type);
                prevMBType = null;
            }

            putMacroblock(frameOut, mb, mbX, mbY);

            if (activePps.entropy_coding_mode_flag && parser.decodeFinalBin() == 1)
                break;
            else if (!activePps.entropy_coding_mode_flag && !moreRBSPData(in))
                break;

            mb.fill(0);
        }
    }

    public MBType decode(SliceType sliceType, int mbAddr, boolean field, MBType prevMbType, Picture8Bit mb,
            Frame[][] references) {
        if (sliceType == SliceType.I) {
            return decodeMBlockI(mbAddr, field, prevMbType, mb);
        } else if (sliceType == SliceType.P) {
            return decodeMBlockP(mbAddr, field, prevMbType, mb, references);
        } else {
            return decodeMBlockB(mbAddr, field, prevMbType, mb, references);
        }
    }

    private MBType decodeMBlockI(int mbIdx, boolean field, MBType prevMbType, Picture8Bit mb) {

        int mbType = parser.decodeMBTypeI(mbIdx, mapper.leftAvailable(mbIdx), mapper.topAvailable(mbIdx),
                decoderState.leftMBType, decoderState.topMBType[mapper.getMbX(mbIdx)]);
        return decodeMBlockIInt(mbType, mbIdx, field, prevMbType, mb);
    }

    private MBType decodeMBlockIInt(int mbType, int mbIdx, boolean field, MBType prevMbType, Picture8Bit mb) {
        MBType mbt;
        if (mbType == 0) {
            decoderIntraNxN.decode(mbIdx, prevMbType, mb);
            mbt = MBType.I_NxN;
        } else if (mbType >= 1 && mbType <= 24) {
            mbType--;
            decoderIntra16x16.decode(mbType, mbIdx, prevMbType, mb);
            mbt = MBType.I_16x16;
        } else {
            Logger.warn("IPCM macroblock found. Not tested, may cause unpredictable behavior.");
            decoderIPCM.decode(mbIdx, mb);
            mbt = MBType.I_PCM;
        }
        return mbt;
    }

    private MBType decodeMBlockP(int mbIdx, boolean field, MBType prevMbType, Picture8Bit mb, Frame[][] references) {
        int mbType = parser.readMBTypeP();

        switch (mbType) {
        case 0:
            decoderInter.decode16x16(mb, references, mbIdx, prevMbType, L0, P_16x16);
            return MBType.P_16x16;
        case 1:
            decoderInter.decode16x8(mb, references, mbIdx, prevMbType, L0, L0, P_16x8);
            return MBType.P_16x8;
        case 2:
            decoderInter.decode8x16(mb, references, mbIdx, prevMbType, L0, L0, P_8x16);
            return MBType.P_8x16;
        case 3:
            decoderInter8x8.decode(mbType, references, mb, P, mbIdx, field, prevMbType, false);
            return MBType.P_8x8;
        case 4:
            decoderInter8x8.decode(mbType, references, mb, P, mbIdx, field, prevMbType, true);
            return MBType.P_8x8ref0;
        default:
            return decodeMBlockIInt(mbType - 5, mbIdx, field, prevMbType, mb);
        }
    }

    private MBType decodeMBlockB(int mbIdx, boolean field, MBType prevMbType, Picture8Bit mb, Frame[][] references) {
        int mbType = parser.readMBTypeB(mbIdx, mapper.leftAvailable(mbIdx), mapper.topAvailable(mbIdx),
                decoderState.leftMBType, decoderState.topMBType[mapper.getMbX(mbIdx)]);
        if (mbType >= 23) {
            return decodeMBlockIInt(mbType - 23, mbIdx, field, prevMbType, mb);
        } else {
            MBType curMBType = H264Const.bMbTypes[mbType];

            if (mbType == 0)
                decoderBDirect.decode(mbIdx, field, prevMbType, mb, references);
            else if (mbType <= 3)
                decoderInter.decode16x16(mb, references, mbIdx, prevMbType, H264Const.bPredModes[mbType][0], curMBType);
            else if (mbType == 22)
                decoderInter8x8.decode(mbType, references, mb, SliceType.B, mbIdx, field, prevMbType, false);
            else if ((mbType & 1) == 0)
                decoderInter.decode16x8(mb, references, mbIdx, prevMbType, H264Const.bPredModes[mbType][0],
                        H264Const.bPredModes[mbType][1], curMBType);
            else
                decoderInter.decode8x16(mb, references, mbIdx, prevMbType, H264Const.bPredModes[mbType][0],
                        H264Const.bPredModes[mbType][1], curMBType);

            return curMBType;
        }
    }

    public void putMacroblock(Picture8Bit tgt, Picture8Bit decoded, int mbX, int mbY) {

        byte[] luma = tgt.getPlaneData(0);
        int stride = tgt.getPlaneWidth(0);

        byte[] cb = tgt.getPlaneData(1);
        byte[] cr = tgt.getPlaneData(2);
        int strideChroma = tgt.getPlaneWidth(1);

        int dOff = 0;
        for (int i = 0; i < 16; i++) {
            System.arraycopy(decoded.getPlaneData(0), dOff, luma, (mbY * 16 + i) * stride + mbX * 16, 16);
            dOff += 16;
        }
        for (int i = 0; i < 8; i++) {
            System.arraycopy(decoded.getPlaneData(1), i * 8, cb, (mbY * 8 + i) * strideChroma + mbX * 8, 8);
        }
        for (int i = 0; i < 8; i++) {
            System.arraycopy(decoded.getPlaneData(2), i * 8, cr, (mbY * 8 + i) * strideChroma + mbX * 8, 8);
        }
    }
}