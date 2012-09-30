package org.jcodec.codecs.h264.io.read;

import static org.jcodec.codecs.h264.io.read.CAVLCReader.readBool;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readUE;

import java.io.IOException;

import org.jcodec.codecs.h264.io.model.ChromaFormat;
import org.jcodec.codecs.h264.io.model.Inter8x8Prediction;
import org.jcodec.codecs.h264.io.model.InterPrediction;
import org.jcodec.codecs.h264.io.model.MBPartPredMode;
import org.jcodec.codecs.h264.io.model.MBlockBDirect16x16;
import org.jcodec.codecs.h264.io.model.MBlockInter;
import org.jcodec.codecs.h264.io.model.MBlockInter.Type;
import org.jcodec.codecs.h264.io.model.MBlockInter8x8;
import org.jcodec.codecs.h264.io.model.MBlockNeighbourhood;
import org.jcodec.codecs.h264.io.model.MBlockWithResidual;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.codecs.h264.io.model.SubMBType;
import org.jcodec.common.io.InBits;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Inter coded macroblocks reader
 * 
 * @author Jay Codec
 * 
 */
public class InterMBlockReader extends CodedMblockReader {

    private boolean transform8x8;
    private ChromaFormat chromaFormat;
    private boolean direct_8x8_inference_flag;

    private InterPredictionReader interPredictionReader;
    private Inter8x8PredictionReader inter8x8PredictionReader;

    private static int[] coded_block_pattern_inter_color = new int[] { 0, 16, 1, 2, 4, 8, 32, 3, 5, 10, 12, 15, 47, 7,
            11, 13, 14, 6, 9, 31, 35, 37, 42, 44, 33, 34, 36, 40, 39, 43, 45, 46, 17, 18, 20, 24, 19, 21, 26, 28, 23,
            27, 29, 30, 22, 25, 38, 41 };

    private static int[] coded_block_pattern_inter_monochrome = new int[] { 0, 1, 2, 4, 8, 3, 5, 10, 12, 15, 7, 11, 13,
            14, 6, 9 };

    public InterMBlockReader(boolean transform8x8, ChromaFormat chromaFormat, boolean entropyCoding,
            int numRefIdxL0Active, int numRefIdxL1Active, boolean direct_8x8_inference_flag) {

        super(chromaFormat, entropyCoding);

        this.transform8x8 = transform8x8;
        this.chromaFormat = chromaFormat;
        this.entropyCoding = entropyCoding;
        this.direct_8x8_inference_flag = direct_8x8_inference_flag;

        interPredictionReader = new InterPredictionReader(numRefIdxL0Active, numRefIdxL1Active);
        inter8x8PredictionReader = new Inter8x8PredictionReader(numRefIdxL0Active, numRefIdxL1Active);
    }

    public MBlockInter8x8 readMBlockInter8x8(InBits reader, MBlockNeighbourhood neighbourhood, int mb_type,
            SliceType sliceType, boolean mb_field_decoding_flag) throws IOException {

        Inter8x8Prediction prediction = inter8x8PredictionReader.read(reader, mb_type, sliceType,
                mb_field_decoding_flag);

        boolean noSubMbPartSizeLessThan8x8Flag = true;
        for (int mbPartIdx = 0; mbPartIdx < 4; mbPartIdx++) {
            if (prediction.getSubMbTypes()[mbPartIdx] != SubMBType.Direct_8x8) {
                if (prediction.getSubMbTypes()[mbPartIdx].getNumParts() > 1) {
                    noSubMbPartSizeLessThan8x8Flag = false;
                }
            } else if (!direct_8x8_inference_flag) {
                noSubMbPartSizeLessThan8x8Flag = false;
            }
        }

        int codedBlockPattern = readCodedBlockPattern(reader);
        int codedBlockPatternLuma = codedBlockPattern % 16;

        boolean transform8x8Used = false;
        if (codedBlockPatternLuma > 0 && transform8x8 && noSubMbPartSizeLessThan8x8Flag) {
            transform8x8Used = readBool(reader, "MB: transform_size_8x8_flag");
        }

        return new MBlockInter8x8(readMBlockWithResidual(reader, neighbourhood, codedBlockPattern, transform8x8Used),
                prediction);
    }

    public MBlockBDirect16x16 readMBlockBDirect(InBits reader, MBlockNeighbourhood neighbourhood) throws IOException {
        int codedBlockPattern = readCodedBlockPattern(reader);
        int codedBlockPatternLuma = codedBlockPattern % 16;

        boolean transform8x8Used = false;
        if (codedBlockPatternLuma > 0 && transform8x8 && direct_8x8_inference_flag) {
            transform8x8Used = readBool(reader, "MB: transform_size_8x8_flag");
        }

        MBlockWithResidual mb = readMBlockWithResidual(reader, neighbourhood, codedBlockPattern, transform8x8Used);

        return new MBlockBDirect16x16(mb);
    }

    public MBlockInter readMBlockInter(InBits reader, MBlockNeighbourhood neighbourhood, int numPartitions,
            MBPartPredMode[] predMode, boolean mb_field_decoding_flag, Type type) throws IOException {
        InterPrediction interPrediction = interPredictionReader.read(reader, numPartitions, mb_field_decoding_flag,
                predMode);

        int coded_block_pattern = readCodedBlockPattern(reader);
        int codedBlockPatternLuma = coded_block_pattern % 16;

        boolean transform8x8Used = false;
        if (codedBlockPatternLuma > 0 && transform8x8) {
            transform8x8Used = readBool(reader, "MB: transform_size_8x8_flag");
        }

        MBlockWithResidual mb = readMBlockWithResidual(reader, neighbourhood, coded_block_pattern, transform8x8Used);

        return new MBlockInter(mb, interPrediction, type);

    }

    protected int readCodedBlockPattern(InBits reader) throws IOException {
        int val = readUE(reader, "coded_block_pattern");
        return getCodedBlockPatternMapping()[val];
    }

    protected int[] getCodedBlockPatternMapping() {
        if (chromaFormat == ChromaFormat.MONOCHROME) {
            return coded_block_pattern_inter_monochrome;
        } else {
            return coded_block_pattern_inter_color;
        }
    }

}
