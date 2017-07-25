package org.jcodec.codecs.h264.decode;
import static org.jcodec.codecs.h264.H264Const.PartPred.L0;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.debugPrint;
import static org.jcodec.codecs.h264.io.model.MBType.B_Direct_16x16;
import static org.jcodec.codecs.h264.io.model.MBType.P_16x16;
import static org.jcodec.codecs.h264.io.model.MBType.P_16x8;
import static org.jcodec.codecs.h264.io.model.MBType.P_8x16;
import static org.jcodec.codecs.h264.io.model.MBType.P_8x8;
import static org.jcodec.codecs.h264.io.model.MBType.P_8x8ref0;
import static org.jcodec.codecs.h264.io.model.SliceType.P;

import org.jcodec.codecs.h264.H264Const;
import org.jcodec.codecs.h264.decode.aso.MapManager;
import org.jcodec.codecs.h264.decode.aso.Mapper;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.model.SliceType;
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
    private DecodedFrame frameOut;
    private DecoderState decoderState;
    private IntObjectMap<Frame> lRefs;
    private Frame[] sRefs;

    public SliceDecoder(SeqParameterSet activeSps, Frame[] sRefs,
            IntObjectMap<Frame> lRefs, DecodedFrame result) {
        this.activeSps = activeSps;
        this.frameOut = result;
        this.sRefs = sRefs;
        this.lRefs = lRefs;
    }

    public void decodeFromReader(SliceReader sliceReader, int poc) {

        parser = sliceReader;
        
        initContext(poc);

        debugPrint("============%d============= ", poc);

        Frame[][] refList = refListManager.getRefList();

        decodeMacroblocks(refList);
    }

    private void initContext(int poc) {
        
        SliceHeader sh = parser.getSliceHeader();
        
        decoderState = new DecoderState(sh);
        mapper = new MapManager(sh.sps, sh.pps).getMapper(sh);

        decoderIntra16x16 = new MBlockDecoderIntra16x16(mapper, sh, poc, decoderState);
        decoderIntraNxN = new MBlockDecoderIntraNxN(mapper, sh, poc, decoderState);
        decoderInter = new MBlockDecoderInter(mapper, sh, poc, decoderState);
        decoderBDirect = new MBlockDecoderBDirect(mapper, sh, poc, decoderState);
        decoderInter8x8 = new MBlockDecoderInter8x8(mapper, decoderBDirect, sh, poc, decoderState);
        skipDecoder = new MBlockSkipDecoder(mapper, decoderBDirect, sh, poc, decoderState);
        decoderIPCM = new MBlockDecoderIPCM(mapper, decoderState);

        refListManager = new RefListManager(sh, sRefs, lRefs, poc);
    }

    private void decodeMacroblocks(Frame[][] refList) {
        int mbWidth = activeSps.picWidthInMbsMinus1 + 1;

        CodedMBlock mBlock = new CodedMBlock(activeSps.chromaFormatIdc);
        while (parser.readMacroblock(mBlock)) {
            int mbAddr = mapper.getAddress(mBlock.mbIdx);
            int mbX = mbAddr % mbWidth;
            int mbY = mbAddr / mbWidth;
            DecodedMBlock mb = frameOut.getMb(mbX, mbY);
            decode(mBlock, parser.getSliceHeader().sliceType, mb, refList);
            mb.shs = parser.getSliceHeader();
            mb.refsUsed = refList;
            fillCoeff(mBlock, mb);
            mBlock.clear();
        }
    }

    private void fillCoeff(CodedMBlock mBlock, DecodedMBlock mb) {
        for (int i = 0; i < 16; i++) {
            int blkOffLeft = H264Const.MB_BLK_OFF_LEFT[i];
            int blkOffTop = H264Const.MB_BLK_OFF_TOP[i];

            mb.nCoeff[blkOffTop][blkOffLeft] = mBlock.nCoeff[i];
        }
    }

    public void decode(CodedMBlock mBlock, SliceType sliceType, DecodedMBlock mb, Frame[][] references) {
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

    private void decodeMBlockI(CodedMBlock mBlock, DecodedMBlock mb) {
        decodeMBlockIInt(mBlock, mb);
    }

    private void decodeMBlockIInt(CodedMBlock mBlock, DecodedMBlock mb) {
        if (mBlock.curMbType == MBType.I_NxN) {
            decoderIntraNxN.decode(mBlock, mb);
        } else if (mBlock.curMbType == MBType.I_16x16) {
            decoderIntra16x16.decode(mBlock, mb);
        } else {
            Logger.warn("IPCM macroblock found. Not tested, may cause unpredictable behavior.");
            decoderIPCM.decode(mBlock, mb);
        }
    }

    private void decodeMBlockP(CodedMBlock mBlock, DecodedMBlock mb, Frame[][] references) {
        if (P_16x16 == mBlock.curMbType) {
            decoderInter.decode16x16(mBlock, mb, references, L0);
        } else if (P_16x8 == mBlock.curMbType) {
            decoderInter.decode16x8(mBlock, mb, references, L0, L0);
        } else if (P_8x16 == mBlock.curMbType) {
            decoderInter.decode8x16(mBlock, mb, references, L0, L0);
        } else if (P_8x8 == mBlock.curMbType) {
            decoderInter8x8.decode(mBlock, references, mb, P, false);
        } else if (P_8x8ref0 == mBlock.curMbType) {
            decoderInter8x8.decode(mBlock, references, mb, P, true);
        } else {
            decodeMBlockIInt(mBlock, mb);
        }
    }

    private void decodeMBlockB(CodedMBlock mBlock, DecodedMBlock mb, Frame[][] references) {
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
}