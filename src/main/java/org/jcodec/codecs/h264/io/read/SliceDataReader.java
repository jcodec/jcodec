package org.jcodec.codecs.h264.io.read;

import static org.jcodec.codecs.h264.io.read.CAVLCReader.moreRBSPData;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readAE;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readBool;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readNBit;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readUE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.codecs.h264.decode.aso.MBlockMapper;
import org.jcodec.codecs.h264.io.model.ChromaFormat;
import org.jcodec.codecs.h264.io.model.CodedMacroblock;
import org.jcodec.codecs.h264.io.model.CoeffToken;
import org.jcodec.codecs.h264.io.model.IntraNxNPrediction;
import org.jcodec.codecs.h264.io.model.MBlockIPCM;
import org.jcodec.codecs.h264.io.model.MBlockIntra16x16;
import org.jcodec.codecs.h264.io.model.MBlockIntraNxN;
import org.jcodec.codecs.h264.io.model.MBlockNeighbourhood;
import org.jcodec.codecs.h264.io.model.Macroblock;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.common.io.InBits;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Reads a coded slice from the bitstream
 * 
 * @author Jay Codec
 * 
 */
public class SliceDataReader {

    private boolean entropyCoding;
    private boolean mbaff;
    private boolean frameMbsOnly;

    private boolean transform8x8;
    private ChromaFormat chromaFormat;
    private int bitDepthLuma;
    private int bitDepthChroma;
    private int numRefIdxL0Active;
    private int numRefIdxL1Active;
    private boolean constrainedIntraPred;

    public SliceDataReader(boolean transform8x8, ChromaFormat chromaFormat, boolean entropyCoding, boolean mbaff,
            boolean frameMbsOnly, int numSliceGroups, int bitDepthLuma, int bitDepthChroma, int numRefIdxL0Active,
            int numRefIdxL1Active, boolean constrainedIntraPred) {
        this.entropyCoding = entropyCoding;
        this.mbaff = mbaff;
        this.frameMbsOnly = frameMbsOnly;
        this.transform8x8 = transform8x8;
        this.chromaFormat = chromaFormat;
        this.bitDepthLuma = bitDepthLuma;
        this.bitDepthChroma = bitDepthChroma;
        this.numRefIdxL0Active = numRefIdxL0Active;
        this.numRefIdxL1Active = numRefIdxL1Active;
        this.constrainedIntraPred = constrainedIntraPred;
    }

    public Macroblock[] read(InBits in, SliceHeader sh, MBlockMapper mBlockMap) throws IOException {

        int numRefIdxL0ActiveForSlice = numRefIdxL0Active;
        if (sh.num_ref_idx_active_override_flag) {
            numRefIdxL0ActiveForSlice = sh.num_ref_idx_l0_active_minus1 + 1;
        }

        MBlockReader mBlockReader = new MBlockReader(transform8x8, chromaFormat, entropyCoding, bitDepthLuma,
                bitDepthChroma, numRefIdxL0ActiveForSlice, numRefIdxL1Active, frameMbsOnly);

        if (entropyCoding)
            return readCABAC(in, sh, mBlockReader, mBlockMap);
        else
            return readCAVLC(in, sh, mBlockReader, mBlockMap);
    }

    public Macroblock[] readCABAC(InBits in, SliceHeader sh, MBlockReader mBlockReader, MBlockMapper mBlockMap)
            throws IOException {

        List<Macroblock> mblocks = new ArrayList<Macroblock>();

        while (in.curBit() != 0) {
            readNBit(in, 1, "DAT: cabac_alignment_one_bit");
        }

        boolean mbaffFrameFlag = (mbaff && !sh.field_pic_flag);

        boolean moreData = true;
        boolean prevMbSkipped = false;
        for (int i = 0; moreData; i++) {
            boolean mb_skip_flag = false;

            if (sh.slice_type.isInter()) {
                mb_skip_flag = readAE(in);
                moreData = !mb_skip_flag;
            }

            boolean mb_field_decoding_flag = false;
            if (mbaffFrameFlag && (i % 2 == 0 || (i % 2 == 1 && prevMbSkipped))) {
                mb_field_decoding_flag = readBool(in, "mb_field_decoding_flag");
            }

            MBlockNeighbourhood neighbourhood = calcNeighbourhood(i, mblocks, mBlockMap);

            // println("*********** POC: X (I/P) MB: " + currMbAddr
            // + " Slice: X Type X **********");
            //
            int mbType = readUE(in, "MB: mb_type");

            Macroblock mblock = mBlockReader.read(sh.slice_type, mbType, in, neighbourhood, mb_field_decoding_flag);

            mblocks.set(i, mblock);
            if (sh.slice_type.isInter())
                prevMbSkipped = mb_skip_flag;
            if (mbaffFrameFlag && i % 2 == 0) {
                moreData = true;
            } else {
                boolean end_of_slice_flag = readAE(in);
                moreData = !end_of_slice_flag;
            }
        }

        return mblocks.toArray(new Macroblock[] {});
    }

    public Macroblock[] readCAVLC(InBits in, SliceHeader sh, MBlockReader mBlockReader, MBlockMapper mBlockMap)
            throws IOException {

        List<Macroblock> mblocks = new ArrayList<Macroblock>();

        boolean mbaffFrameFlag = (mbaff && !sh.field_pic_flag);

        boolean prevMbSkipped = false;
        for (int i = 0;; i++) {
            if (sh.slice_type.isInter()) {
                int mbSkipRun = readUE(in, "mb_skip_run");
                for (int j = 0; j < mbSkipRun; j++, i++)
                    mblocks.add(null);

                prevMbSkipped = mbSkipRun > 0;

                if (!moreRBSPData(in))
                    break;
            }

            boolean mb_field_decoding_flag = false;
            if (mbaffFrameFlag && (i % 2 == 0 || (i % 2 == 1 && prevMbSkipped))) {
                mb_field_decoding_flag = readBool(in, "mb_field_decoding_flag");
            }

            MBlockNeighbourhood neighbourhood = calcNeighbourhood(i, mblocks, mBlockMap);

            // println("*********** POC: X (I/P) MB: " + currMbAddr
            // + " Slice: X Type X **********");

            int mbType = readUE(in, "MB: mb_type");

            Macroblock mblock = mBlockReader.read(sh.slice_type, mbType, in, neighbourhood, mb_field_decoding_flag);

            mblocks.add(mblock);

            if (!moreRBSPData(in))
                break;
        }

        return mblocks.toArray(new Macroblock[] {});
    }

    private static CoeffToken[] EMPTY_LUMA = new CoeffToken[] { new CoeffToken(0, 0), new CoeffToken(0, 0),
            new CoeffToken(0, 0), new CoeffToken(0, 0),

            new CoeffToken(0, 0), new CoeffToken(0, 0), new CoeffToken(0, 0), new CoeffToken(0, 0),

            new CoeffToken(0, 0), new CoeffToken(0, 0), new CoeffToken(0, 0), new CoeffToken(0, 0),

            new CoeffToken(0, 0), new CoeffToken(0, 0), new CoeffToken(0, 0), new CoeffToken(0, 0) };

    private static CoeffToken[] EMPTY_CHROMA = new CoeffToken[] { new CoeffToken(0, 0), new CoeffToken(0, 0),
            new CoeffToken(0, 0), new CoeffToken(0, 0) };

    private MBlockNeighbourhood calcNeighbourhood(int mbIndex, List<Macroblock> mblocks, MBlockMapper mBlockMap) {

        boolean leftAvailable = false, topAvailable = false;
        IntraNxNPrediction predLeft = null;
        IntraNxNPrediction predTop = null;

        CoeffToken[] lumaTokensLeft = null;
        CoeffToken[] cbTokensLeft = null;
        CoeffToken[] crTokensLeft = null;
        int leftIndex = mBlockMap.getLeftMBIdx(mbIndex);
        if (leftIndex != -1) {
            Macroblock leftMb = mblocks.get(leftIndex);
            if (leftMb instanceof CodedMacroblock) {
                CodedMacroblock mbLeft = (CodedMacroblock) leftMb;
                lumaTokensLeft = mbLeft.getLumaTokens();
                cbTokensLeft = mbLeft.getChroma().getCoeffTokenCb();
                crTokensLeft = mbLeft.getChroma().getCoeffTokenCr();
                if (mbLeft instanceof MBlockIntraNxN)
                    predLeft = ((MBlockIntraNxN) mbLeft).getPrediction();
            } else {
                lumaTokensLeft = EMPTY_LUMA;
                cbTokensLeft = EMPTY_CHROMA;
                crTokensLeft = EMPTY_CHROMA;
            }
            boolean leftIntra = (leftMb instanceof MBlockIntraNxN) || (leftMb instanceof MBlockIntra16x16)
                    || (leftMb instanceof MBlockIPCM);

            leftAvailable = !constrainedIntraPred || leftIntra;
        }

        CoeffToken[] lumaTokensTop = null;
        CoeffToken[] cbTokensTop = null;
        CoeffToken[] crTokensTop = null;
        int topIndex = mBlockMap.getTopMBIdx(mbIndex);
        if (topIndex != -1) {
            Macroblock topMb = mblocks.get(topIndex);
            if (topMb instanceof CodedMacroblock) {
                CodedMacroblock mbTop = (CodedMacroblock) topMb;
                lumaTokensTop = mbTop.getLumaTokens();
                cbTokensTop = mbTop.getChroma().getCoeffTokenCb();
                crTokensTop = mbTop.getChroma().getCoeffTokenCr();
                if (mbTop instanceof MBlockIntraNxN)
                    predTop = ((MBlockIntraNxN) mbTop).getPrediction();
            } else {
                lumaTokensTop = EMPTY_LUMA;
                cbTokensTop = EMPTY_CHROMA;
                crTokensTop = EMPTY_CHROMA;
            }

            boolean topIntra = (topMb instanceof MBlockIntraNxN) || (topMb instanceof MBlockIntra16x16)
                    || (topMb instanceof MBlockIPCM);

            topAvailable = !constrainedIntraPred || topIntra;
        }

        return new MBlockNeighbourhood(lumaTokensLeft, lumaTokensTop, cbTokensLeft, cbTokensTop, crTokensLeft,
                crTokensTop, predLeft, predTop, leftAvailable, topAvailable);
    }

    // private int getPicHeightInMbs(SliceHeader sh) {
    // int frameHeightInMbs = (2 - (frameMbsOnly ? 1 : 0))
    // * picHeightInMapUnits;
    // return frameHeightInMbs / (1 + (sh.field_pic_flag ? 1 : 0));
    // }
}
