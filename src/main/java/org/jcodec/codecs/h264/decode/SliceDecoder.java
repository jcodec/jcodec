package org.jcodec.codecs.h264.decode;

import static org.jcodec.codecs.h264.H264Const.PartPred.L0;
import static org.jcodec.codecs.h264.decode.DecoderUtils.putMacroblock;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.debugPrint;
import static org.jcodec.codecs.h264.io.model.MBType.B_Direct_16x16;
import static org.jcodec.codecs.h264.io.model.SliceType.P;

import org.jcodec.codecs.h264.DeblockingFilter;
import org.jcodec.codecs.h264.DecodedMBlock;
import org.jcodec.codecs.h264.EncodedMBlock;
import org.jcodec.codecs.h264.H264Const;
import org.jcodec.codecs.h264.H264Decoder.DeblockingFilterContext;
import org.jcodec.codecs.h264.decode.aso.MapManager;
import org.jcodec.codecs.h264.decode.aso.Mapper;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.common.ArrayUtil;
import org.jcodec.common.IntObjectMap;
import org.jcodec.common.logging.Logger;

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

    private Mapper mapper;

    private MBlockDecoderIntra16x16 decoderIntra16x16;
    private MBlockDecoderIntraNxN decoderIntraNxN;
    private MBlockDecoderInter decoderInter;
    private MBlockDecoderInter8x8 decoderInter8x8;
    private MBlockSkipDecoder skipDecoder;
    private MBlockDecoderBDirect decoderBDirect;
    private RefListManager refListManager;
    private MBlockDecoderIPCM decoderIPCM;
    private SliceReader parser;
    private SeqParameterSet activeSps;
    private Frame frameOut;
    private DecoderState decoderState;
    private IntObjectMap<Frame> lRefs;
    private Frame[] sRefs;
    private DeblockingFilterContext deblockingContext;
    private DeblockingFilter deblockingFilter;

    public SliceDecoder(SeqParameterSet activeSps, Frame[] sRefs, IntObjectMap<Frame> lRefs,
            DeblockingFilterContext deblockingContext, Frame result) {
        this.activeSps = activeSps;
        this.frameOut = result;
        this.sRefs = sRefs;
        this.lRefs = lRefs;
        this.deblockingContext = deblockingContext;
        deblockingFilter = new DeblockingFilter();
    }

    public void decode(SliceReader sliceReader) {

        parser = sliceReader;

        initContext();

        debugPrint("============%d============= ", frameOut.getPOC());

        Frame[][] refList = refListManager.getRefList();

        decodeMacroblocks(refList);
    }

    private void initContext() {

        SliceHeader sh = parser.getSliceHeader();

        decoderState = new DecoderState(sh);
        mapper = new MapManager(sh.sps, sh.pps).getMapper(sh);

        decoderIntra16x16 = new MBlockDecoderIntra16x16(mapper, sh, frameOut.getPOC(), decoderState);
        decoderIntraNxN = new MBlockDecoderIntraNxN(mapper, sh, frameOut.getPOC(), decoderState);
        decoderInter = new MBlockDecoderInter(mapper, sh, frameOut.getPOC(), decoderState);
        decoderBDirect = new MBlockDecoderBDirect(mapper, sh, frameOut.getPOC(), decoderState);
        decoderInter8x8 = new MBlockDecoderInter8x8(mapper, decoderBDirect, sh, frameOut.getPOC(), decoderState);
        skipDecoder = new MBlockSkipDecoder(mapper, decoderBDirect, sh, frameOut.getPOC(), decoderState);
        decoderIPCM = new MBlockDecoderIPCM(mapper, decoderState);

        refListManager = new RefListManager(sh, sRefs, lRefs, frameOut);
    }

    private void decodeMacroblocks(Frame[][] refList) {
        DecodedMBlock mb = new DecodedMBlock();
        int mbWidth = activeSps.pic_width_in_mbs_minus1 + 1;

        EncodedMBlock mBlock = new EncodedMBlock(activeSps.chroma_format_idc);
        while (parser.readMacroblock(mBlock)) {
            int mbAddr = mapper.getAddress(mBlock.mbIdx);

            SliceHeader sh = parser.getSliceHeader();
            mb.setAlphaC0Offset(sh.slice_alpha_c0_offset_div2 << 1);
            mb.setBetaOffset(sh.slice_beta_offset_div2 << 1);
            mb.setDeblockTop(mbAddr >= mbWidth && sh.disable_deblocking_filter_idc == 1
                    || (sh.disable_deblocking_filter_idc == 2 && mapper.topAvailable(mBlock.mbIdx)));
            mb.setDeblockLeft(mbAddr > 0 && sh.disable_deblocking_filter_idc == 1
                    || (sh.disable_deblocking_filter_idc == 2 && mapper.leftAvailable(mBlock.mbIdx)));

            decode(mBlock, parser.getSliceHeader().slice_type, mb, refList);
            int mbX = mbAddr % mbWidth;
            int mbY = mbAddr / mbWidth;
            if (mbY > 0)
                putMacroblock(frameOut, deblockingContext.getTop(mbX).getPixels(), mbX, mbY - 1);
            deblockingContext.save(mb, mbX);
            saveMvs(mb, mbX, mbY);

            mb.clear();
            mBlock.clear();
        }
    }

    private void saveMvs(DecodedMBlock mb, int mbX, int mbY) {
        frameOut.getMvs().saveMBVectors(mbX, mbY, mb.getMxL0(), mb.getMyL0(), mb.getRefIdxL0(), mb.getRefPOCL0(), mb.getRefShortTermL0(),
                mb.getMxL1(), mb.getMyL1(), mb.getRefIdxL1(), mb.getRefPOCL1(), mb.getRefShortTermL1());
    }

    public void decode(EncodedMBlock mBlock, SliceType sliceType, DecodedMBlock mb, Frame[][] references) {
        int mbX = mapper.getMbX(mBlock.mbIdx);
        int mbY = mapper.getMbY(mBlock.mbIdx);

        ArrayUtil.copyPermute(mBlock.nCoeff, mb.getNc(), H264Const.BLK_INV_MAP);
        if (mBlock.skipped) {
            skipDecoder.decodeSkip(mBlock, mb, references, sliceType);
            deblockingFilter.deblockMbP(mb, mbX > 0 ? deblockingContext.getLeft(mbX) : null,
                    mbY > 0 ? deblockingContext.getTop(mbX) : null);
        } else if (sliceType == SliceType.I) {
            decodeMBlockI(mBlock, mb);
            deblockingFilter.deblockMbP(mb, mbX > 0 ? deblockingContext.getLeft(mbX) : null,
                    mbY > 0 ? deblockingContext.getTop(mbX) : null);
        } else if (sliceType == SliceType.P) {
            decodeMBlockP(mBlock, mb, references);
            deblockingFilter.deblockMbP(mb, mbX > 0 ? deblockingContext.getLeft(mbX) : null,
                    mbY > 0 ? deblockingContext.getTop(mbX) : null);
        } else {
            decodeMBlockB(mBlock, mb, references);
            deblockingFilter.deblockMbP(mb, mbX > 0 ? deblockingContext.getLeft(mbX) : null,
                    mbY > 0 ? deblockingContext.getTop(mbX) : null);
        }
    }

    private void decodeMBlockI(EncodedMBlock mBlock, DecodedMBlock mb) {
        decodeMBlockIInt(mBlock, mb);
    }

    private void decodeMBlockIInt(EncodedMBlock mBlock, DecodedMBlock mb) {
        if (mBlock.curMbType == MBType.I_NxN) {
            decoderIntraNxN.decode(mBlock, mb);
        } else if (mBlock.curMbType == MBType.I_16x16) {
            decoderIntra16x16.decode(mBlock, mb);
        } else {
            Logger.warn("IPCM macroblock found. Not tested, may cause unpredictable behavior.");
            decoderIPCM.decode(mBlock, mb);
        }
    }

    private void decodeMBlockP(EncodedMBlock mBlock, DecodedMBlock mb, Frame[][] references) {
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
            decoderInter8x8.decode(mBlock, mb, references, P, false);
            break;
        case P_8x8ref0:
            decoderInter8x8.decode(mBlock, mb, references, P, true);
            break;
        default:
            decodeMBlockIInt(mBlock, mb);
            break;
        }
    }

    private void decodeMBlockB(EncodedMBlock mBlock, DecodedMBlock mb, Frame[][] references) {
        if (mBlock.curMbType.isIntra()) {
            decodeMBlockIInt(mBlock, mb);
        } else {
            if (mBlock.curMbType == B_Direct_16x16) {
                decoderBDirect.decode(mBlock, mb, references);
            } else if (mBlock.mbType <= 3) {
                decoderInter.decode16x16(mBlock, mb, references, H264Const.bPredModes[mBlock.mbType][0]);
            } else if (mBlock.mbType == 22) {
                decoderInter8x8.decode(mBlock, mb, references, SliceType.B, false);
            } else if ((mBlock.mbType & 1) == 0) {
                decoderInter.decode16x8(mBlock, mb, references, H264Const.bPredModes[mBlock.mbType][0],
                        H264Const.bPredModes[mBlock.mbType][1]);
            } else {
                decoderInter.decode8x16(mBlock, mb, references, H264Const.bPredModes[mBlock.mbType][0],
                        H264Const.bPredModes[mBlock.mbType][1]);
            }
        }
    }
}