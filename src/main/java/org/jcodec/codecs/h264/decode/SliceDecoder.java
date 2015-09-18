package org.jcodec.codecs.h264.decode;

import static org.jcodec.codecs.h264.H264Const.PartPred.L0;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.debugPrint;
import static org.jcodec.codecs.h264.io.model.MBType.B_Direct_16x16;
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

        parser = new BitstreamParser(activePps, cabac, cavlc, mDecoder, in, di, mapper, sh, decoderState);

        decoderIntra16x16 = new MBlockDecoderIntra16x16(mapper, sh, di, frameOut.getPOC(), decoderState);
        decoderIntraNxN = new MBlockDecoderIntraNxN(mapper, sh, di, frameOut.getPOC(), decoderState);
        decoderInter = new MBlockDecoderInter(mapper, sh, di, frameOut.getPOC(), decoderState);
        decoderBDirect = new MBlockDecoderBDirect(mapper, sh, di, frameOut.getPOC(), decoderState);
        decoderInter8x8 = new MBlockDecoderInter8x8(mapper, decoderBDirect, sh, di, frameOut.getPOC(), decoderState);
        skipDecoder = new MBlockSkipDecoder(mapper, decoderBDirect, sh, di, frameOut.getPOC(), decoderState);
        decoderIPCM = new MBlockDecoderIPCM(mapper, decoderState);

        refListManager = new RefListManager(sh, sRefs, lRefs, frameOut);

    }

    private void decodeMacroblocks(SliceHeader sh, BitReader in, Frame[][] refList) {
        Picture8Bit mb = Picture8Bit.create(16, 16, sh.sps.chroma_format_idc);
        int mbWidth = sh.sps.pic_width_in_mbs_minus1 + 1;

        MBlock mBlock;
        while ((mBlock = parser.readMacroblock()) != null) {
            decode(mBlock, sh.slice_type, mb, refList);
            int mbAddr = mapper.getAddress(mBlock.mbIdx);
            int mbX = mbAddr % mbWidth;
            int mbY = mbAddr / mbWidth;
            putMacroblock(frameOut, mb, mbX, mbY);
            di.shs[mbAddr] = sh;
            di.refsUsed[mbAddr] = refList;
            mb.fill(0);
        }
    }

    public void decode(MBlock mBlock, SliceType sliceType, Picture8Bit mb, Frame[][] references) {
        if (mBlock.skipped) {
            skipDecoder.decodeSkip(mBlock, references, mb, sliceType);
        } else if (sliceType == SliceType.I) {
            decodeMBlockI(mBlock, mb);
        } else if (sliceType == SliceType.P) {
            decodeMBlockP(mBlock, mb, references);
        } else {
            decodeMBlockB(mBlock, mb, references);
        }
    }

    private void decodeMBlockI(MBlock mBlock, Picture8Bit mb) {
        decodeMBlockIInt(mBlock, mb);
    }

    private void decodeMBlockIInt(MBlock mBlock, Picture8Bit mb) {
        if (mBlock.curMbType == MBType.I_NxN) {
            decoderIntraNxN.decode(mBlock, mb);
        } else if (mBlock.curMbType == MBType.I_16x16) {
            decoderIntra16x16.decode(mBlock, mb);
        } else {
            Logger.warn("IPCM macroblock found. Not tested, may cause unpredictable behavior.");
            decoderIPCM.decode(mBlock, mb);
        }
    }

    private void decodeMBlockP(MBlock mBlock, Picture8Bit mb, Frame[][] references) {
        switch (mBlock.curMbType) {
        case P_16x16:
            decoderInter.decode16x16(mBlock, mb, references, L0);
            break;
        case P_16x8:
            decoderInter.decode16x8(mBlock, mb, references, L0, L0);
            break;
        case P_8x16:
            decoderInter.decode8x16(mBlock, mb, references, L0, L0);
            break;
        case P_8x8:
            decoderInter8x8.decode(mBlock, references, mb, P, false);
            break;
        case P_8x8ref0:
            decoderInter8x8.decode(mBlock, references, mb, P, true);
            break;
        default:
            decodeMBlockIInt(mBlock, mb);
            break;
        }
    }

    private void decodeMBlockB(MBlock mBlock, Picture8Bit mb, Frame[][] references) {
        if (mBlock.curMbType.isIntra()) {
            decodeMBlockIInt(mBlock, mb);
        } else {
            if (mBlock.curMbType == B_Direct_16x16) {
                decoderBDirect.decode(mBlock, mb, references);
            } else if (mBlock.mbType <= 3) {
                decoderInter.decode16x16(mBlock, mb, references, H264Const.bPredModes[mBlock.mbType][0]);
            } else if (mBlock.mbType == 22) {
                decoderInter8x8.decode(mBlock, references, mb, SliceType.B, false);
            } else if ((mBlock.mbType & 1) == 0) {
                decoderInter.decode16x8(mBlock, mb, references, H264Const.bPredModes[mBlock.mbType][0],
                        H264Const.bPredModes[mBlock.mbType][1]);
            } else {
                decoderInter.decode8x16(mBlock, mb, references, H264Const.bPredModes[mBlock.mbType][0],
                        H264Const.bPredModes[mBlock.mbType][1]);
            }
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