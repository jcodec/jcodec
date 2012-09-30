package org.jcodec.codecs.h264.io.read;

import java.io.IOException;

import org.jcodec.codecs.h264.io.model.ChromaFormat;
import org.jcodec.codecs.h264.io.model.MBPartPredMode;
import org.jcodec.codecs.h264.io.model.MBlockInter;
import org.jcodec.codecs.h264.io.model.MBlockIntra16x16;
import org.jcodec.codecs.h264.io.model.MBlockNeighbourhood;
import org.jcodec.codecs.h264.io.model.Macroblock;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.common.io.InBits;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A reader class for coded macroblocks
 * 
 * Dispatches calls to specific readers
 * 
 * @author Jay Codec
 * 
 */
public class MBlockReader {
    private IntraMBlockReader intraMBlockReader;
    private InterMBlockReader interMBlockReader;
    private IPCMMblockReader ipcmMblockReader;

    public MBlockReader(boolean transform8x8, ChromaFormat chromaFormat, boolean entropyCoding, int bitDepthLuma,
            int bitDepthChroma, int numRefIdxL0Active, int numRefIdxL1Active, boolean direct_8x8_inference_flag) {
        intraMBlockReader = new IntraMBlockReader(transform8x8, chromaFormat, entropyCoding);
        interMBlockReader = new InterMBlockReader(transform8x8, chromaFormat, entropyCoding, numRefIdxL0Active,
                numRefIdxL1Active, direct_8x8_inference_flag);
        ipcmMblockReader = new IPCMMblockReader(chromaFormat, bitDepthLuma, bitDepthChroma);
    }

    public Macroblock read(SliceType sliceType, int mbType, InBits reader, MBlockNeighbourhood neighbourhood,
            boolean mbFieldDecodingFlag) throws IOException {

        if (sliceType == SliceType.I) {
            return readMBlockI(mbType, reader, neighbourhood);
        } else if (sliceType == SliceType.P) {
            return readMBlockP(mbType, reader, neighbourhood, mbFieldDecodingFlag);

        } else if (sliceType == SliceType.B) {
            return readMBlockB(mbType, reader, neighbourhood, mbFieldDecodingFlag);
        }

        return null;
    }

    private Macroblock readMBlockI(int mbType, InBits reader, MBlockNeighbourhood neighbourhood) throws IOException {
        if (mbType == 0)
            return intraMBlockReader.readMBlockIntraNxN(reader, neighbourhood);
        else if (mbType >= 1 && mbType <= 24)
            return readMBlockIntra16x16(reader, mbType, neighbourhood);
        else if (mbType == 25)
            return ipcmMblockReader.readMBlockIPCM(reader);

        return null;
    }

    private Macroblock readMBlockP(int mbType, InBits reader, MBlockNeighbourhood neighbourhood,
            boolean mbFieldDecodingFlag) throws IOException {
        switch (mbType) {
        case 0:
            return interMBlockReader.readMBlockInter(reader, neighbourhood, 1,
                    new MBPartPredMode[] { MBPartPredMode.Pred_L0 }, mbFieldDecodingFlag, MBlockInter.Type.MB_16x16);
        case 1:
            return interMBlockReader.readMBlockInter(reader, neighbourhood, 2, new MBPartPredMode[] {
                    MBPartPredMode.Pred_L0, MBPartPredMode.Pred_L0 }, mbFieldDecodingFlag, MBlockInter.Type.MB_16x8);
        case 2:
            return interMBlockReader.readMBlockInter(reader, neighbourhood, 2, new MBPartPredMode[] {
                    MBPartPredMode.Pred_L0, MBPartPredMode.Pred_L0 }, mbFieldDecodingFlag, MBlockInter.Type.MB_8x16);
        case 3:
            return interMBlockReader
                    .readMBlockInter8x8(reader, neighbourhood, mbType, SliceType.P, mbFieldDecodingFlag);
        case 4:
            return interMBlockReader
                    .readMBlockInter8x8(reader, neighbourhood, mbType, SliceType.P, mbFieldDecodingFlag);
        default:
            return readMBlockI(mbType - 5, reader, neighbourhood);
        }
    }

    private Macroblock readMBlockB(int mbType, InBits reader, MBlockNeighbourhood neighbourhood,
            boolean mbFieldDecodingFlag) throws IOException {

        switch (mbType) {
        case 0:
            return interMBlockReader.readMBlockBDirect(reader, neighbourhood);
        case 1:
            return interMBlockReader.readMBlockInter(reader, neighbourhood, 1,
                    new MBPartPredMode[] { MBPartPredMode.Pred_L0 }, mbFieldDecodingFlag, MBlockInter.Type.MB_16x16);
        case 2:
            return interMBlockReader.readMBlockInter(reader, neighbourhood, 1,
                    new MBPartPredMode[] { MBPartPredMode.Pred_L1 }, mbFieldDecodingFlag, MBlockInter.Type.MB_16x16);
        case 3:
            return interMBlockReader.readMBlockInter(reader, neighbourhood, 1,
                    new MBPartPredMode[] { MBPartPredMode.BiPred }, mbFieldDecodingFlag, MBlockInter.Type.MB_16x16);
        case 4:
            return interMBlockReader.readMBlockInter(reader, neighbourhood, 2, new MBPartPredMode[] {
                    MBPartPredMode.Pred_L0, MBPartPredMode.Pred_L0 }, mbFieldDecodingFlag, MBlockInter.Type.MB_16x8);
        case 5:
            return interMBlockReader.readMBlockInter(reader, neighbourhood, 2, new MBPartPredMode[] {
                    MBPartPredMode.Pred_L0, MBPartPredMode.Pred_L0 }, mbFieldDecodingFlag, MBlockInter.Type.MB_8x16);
        case 6:
            return interMBlockReader.readMBlockInter(reader, neighbourhood, 2, new MBPartPredMode[] {
                    MBPartPredMode.Pred_L1, MBPartPredMode.Pred_L1 }, mbFieldDecodingFlag, MBlockInter.Type.MB_16x8);
        case 7:
            return interMBlockReader.readMBlockInter(reader, neighbourhood, 2, new MBPartPredMode[] {
                    MBPartPredMode.Pred_L1, MBPartPredMode.Pred_L1 }, mbFieldDecodingFlag, MBlockInter.Type.MB_8x16);
        case 8:
            return interMBlockReader.readMBlockInter(reader, neighbourhood, 2, new MBPartPredMode[] {
                    MBPartPredMode.Pred_L0, MBPartPredMode.Pred_L1 }, mbFieldDecodingFlag, MBlockInter.Type.MB_16x8);
        case 9:
            return interMBlockReader.readMBlockInter(reader, neighbourhood, 2, new MBPartPredMode[] {
                    MBPartPredMode.Pred_L0, MBPartPredMode.Pred_L1 }, mbFieldDecodingFlag, MBlockInter.Type.MB_8x16);
        case 10:
            return interMBlockReader.readMBlockInter(reader, neighbourhood, 2, new MBPartPredMode[] {
                    MBPartPredMode.Pred_L1, MBPartPredMode.Pred_L0 }, mbFieldDecodingFlag, MBlockInter.Type.MB_16x8);
        case 11:
            return interMBlockReader.readMBlockInter(reader, neighbourhood, 2, new MBPartPredMode[] {
                    MBPartPredMode.Pred_L1, MBPartPredMode.Pred_L0 }, mbFieldDecodingFlag, MBlockInter.Type.MB_8x16);
        case 12:
            return interMBlockReader.readMBlockInter(reader, neighbourhood, 2, new MBPartPredMode[] {
                    MBPartPredMode.Pred_L0, MBPartPredMode.BiPred }, mbFieldDecodingFlag, MBlockInter.Type.MB_16x8);
        case 13:
            return interMBlockReader.readMBlockInter(reader, neighbourhood, 2, new MBPartPredMode[] {
                    MBPartPredMode.Pred_L0, MBPartPredMode.BiPred }, mbFieldDecodingFlag, MBlockInter.Type.MB_8x16);
        case 14:
            return interMBlockReader.readMBlockInter(reader, neighbourhood, 2, new MBPartPredMode[] {
                    MBPartPredMode.Pred_L1, MBPartPredMode.BiPred }, mbFieldDecodingFlag, MBlockInter.Type.MB_16x8);
        case 15:
            return interMBlockReader.readMBlockInter(reader, neighbourhood, 2, new MBPartPredMode[] {
                    MBPartPredMode.Pred_L1, MBPartPredMode.BiPred }, mbFieldDecodingFlag, MBlockInter.Type.MB_8x16);
        case 16:
            return interMBlockReader.readMBlockInter(reader, neighbourhood, 2, new MBPartPredMode[] {
                    MBPartPredMode.BiPred, MBPartPredMode.Pred_L0 }, mbFieldDecodingFlag, MBlockInter.Type.MB_16x8);
        case 17:
            return interMBlockReader.readMBlockInter(reader, neighbourhood, 2, new MBPartPredMode[] {
                    MBPartPredMode.BiPred, MBPartPredMode.Pred_L0 }, mbFieldDecodingFlag, MBlockInter.Type.MB_8x16);
        case 18:
            return interMBlockReader.readMBlockInter(reader, neighbourhood, 1, new MBPartPredMode[] {
                    MBPartPredMode.BiPred, MBPartPredMode.Pred_L1 }, mbFieldDecodingFlag, MBlockInter.Type.MB_16x8);
        case 19:
            return interMBlockReader.readMBlockInter(reader, neighbourhood, 1, new MBPartPredMode[] {
                    MBPartPredMode.BiPred, MBPartPredMode.Pred_L1 }, mbFieldDecodingFlag, MBlockInter.Type.MB_8x16);
        case 20:
            return interMBlockReader.readMBlockInter(reader, neighbourhood, 1, new MBPartPredMode[] {
                    MBPartPredMode.BiPred, MBPartPredMode.BiPred }, mbFieldDecodingFlag, MBlockInter.Type.MB_16x8);
        case 21:
            return interMBlockReader.readMBlockInter(reader, neighbourhood, 1, new MBPartPredMode[] {
                    MBPartPredMode.BiPred, MBPartPredMode.BiPred }, mbFieldDecodingFlag, MBlockInter.Type.MB_8x16);
        case 22:
            return interMBlockReader
                    .readMBlockInter8x8(reader, neighbourhood, mbType, SliceType.B, mbFieldDecodingFlag);
        default:
            return readMBlockI(mbType - 23, reader, neighbourhood);
        }
    }

    public MBlockIntra16x16 readMBlockIntra16x16(InBits reader, int mbType, MBlockNeighbourhood neighbourhood)
            throws IOException {
        switch (mbType) {
        case 1:
            return intraMBlockReader.readMBlockIntra16x16(reader, neighbourhood, 0, 0, 0);
        case 2:
            return intraMBlockReader.readMBlockIntra16x16(reader, neighbourhood, 1, 0, 0);
        case 3:
            return intraMBlockReader.readMBlockIntra16x16(reader, neighbourhood, 2, 0, 0);
        case 4:
            return intraMBlockReader.readMBlockIntra16x16(reader, neighbourhood, 3, 0, 0);
        case 5:
            return intraMBlockReader.readMBlockIntra16x16(reader, neighbourhood, 0, 1, 0);
        case 6:
            return intraMBlockReader.readMBlockIntra16x16(reader, neighbourhood, 1, 1, 0);
        case 7:
            return intraMBlockReader.readMBlockIntra16x16(reader, neighbourhood, 2, 1, 0);
        case 8:
            return intraMBlockReader.readMBlockIntra16x16(reader, neighbourhood, 3, 1, 0);
        case 9:
            return intraMBlockReader.readMBlockIntra16x16(reader, neighbourhood, 0, 2, 0);
        case 10:
            return intraMBlockReader.readMBlockIntra16x16(reader, neighbourhood, 1, 2, 0);
        case 11:
            return intraMBlockReader.readMBlockIntra16x16(reader, neighbourhood, 2, 2, 0);
        case 12:
            return intraMBlockReader.readMBlockIntra16x16(reader, neighbourhood, 3, 2, 0);
        case 13:
            return intraMBlockReader.readMBlockIntra16x16(reader, neighbourhood, 0, 0, 15);
        case 14:
            return intraMBlockReader.readMBlockIntra16x16(reader, neighbourhood, 1, 0, 15);
        case 15:
            return intraMBlockReader.readMBlockIntra16x16(reader, neighbourhood, 2, 0, 15);
        case 16:
            return intraMBlockReader.readMBlockIntra16x16(reader, neighbourhood, 3, 0, 15);
        case 17:
            return intraMBlockReader.readMBlockIntra16x16(reader, neighbourhood, 0, 1, 15);
        case 18:
            return intraMBlockReader.readMBlockIntra16x16(reader, neighbourhood, 1, 1, 15);
        case 19:
            return intraMBlockReader.readMBlockIntra16x16(reader, neighbourhood, 2, 1, 15);
        case 20:
            return intraMBlockReader.readMBlockIntra16x16(reader, neighbourhood, 3, 1, 15);
        case 21:
            return intraMBlockReader.readMBlockIntra16x16(reader, neighbourhood, 0, 2, 15);
        case 22:
            return intraMBlockReader.readMBlockIntra16x16(reader, neighbourhood, 1, 2, 15);
        case 23:
            return intraMBlockReader.readMBlockIntra16x16(reader, neighbourhood, 2, 2, 15);
        case 24:
            return intraMBlockReader.readMBlockIntra16x16(reader, neighbourhood, 3, 2, 15);
        }

        return null;
    }
}
