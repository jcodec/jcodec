package org.jcodec.codecs.h264.io.read;

import static org.jcodec.codecs.h264.io.read.CAVLCReader.readBool;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readSE;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readUE;

import java.io.IOException;

import org.jcodec.codecs.h264.io.model.ChromaFormat;
import org.jcodec.codecs.h264.io.model.CodedChroma;
import org.jcodec.codecs.h264.io.model.CoeffToken;
import org.jcodec.codecs.h264.io.model.IntraNxNPrediction;
import org.jcodec.codecs.h264.io.model.MBlockIntra16x16;
import org.jcodec.codecs.h264.io.model.MBlockIntraNxN;
import org.jcodec.codecs.h264.io.model.MBlockNeighbourhood;
import org.jcodec.codecs.h264.io.model.MBlockWithResidual;
import org.jcodec.codecs.h264.io.model.ResidualBlock;
import org.jcodec.common.io.InBits;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Reader for coded macroblocks with intra prediction
 * 
 * @author Jay Codec
 * 
 */
public class IntraMBlockReader extends CodedMblockReader {
    private boolean transform8x8;
    private ChromaFormat chromaFormat;

    private ResidualCoeffsCAVLCReader cavlcReader;
    private CoeffTokenReader coeffTokenReader;
    private ChromaReader chromaReader;

    private static int[] coded_block_pattern_intra_color = new int[] { 47, 31, 15, 0, 23, 27, 29, 30, 7, 11, 13, 14,
            39, 43, 45, 46, 16, 3, 5, 10, 12, 19, 21, 26, 28, 35, 37, 42, 44, 1, 2, 4, 8, 17, 18, 20, 24, 6, 9, 22, 25,
            32, 33, 34, 36, 40, 38, 41 };

    private static int[] coded_block_pattern_intra_monochrome = new int[] { 15, 0, 7, 11, 13, 14, 3, 5, 10, 12, 1, 2,
            4, 8, 6, 9 };

    public IntraMBlockReader(boolean transform8x8, ChromaFormat chromaFormat, boolean entropyCoding) {
        super(chromaFormat, entropyCoding);

        this.transform8x8 = transform8x8;
        this.chromaFormat = chromaFormat;

        coeffTokenReader = new CoeffTokenReader(chromaFormat);
        cavlcReader = new ResidualCoeffsCAVLCReader(chromaFormat);
        chromaReader = new ChromaReader(chromaFormat, entropyCoding);
    }

    public MBlockIntraNxN readMBlockIntraNxN(InBits reader, MBlockNeighbourhood neighbourhood) throws IOException {
        boolean transform8x8Used = false;
        if (transform8x8) {
            transform8x8Used = readBool(reader, "transform_size_8x8_flag");
        }

        IntraNxNPrediction prediction;

        if (!transform8x8Used) {
            prediction = IntraPredictionReader.readPrediction4x4(reader, neighbourhood.getPredLeft(),
                    neighbourhood.getPredTop(), neighbourhood.isLeftAvailable(), neighbourhood.isTopAvailable());
        } else {
            prediction = IntraPredictionReader.readPrediction8x8(reader);
        }

        int codedBlockPattern = readCodedBlockPattern(reader);

        MBlockWithResidual mb = readMBlockWithResidual(reader, neighbourhood, codedBlockPattern, transform8x8Used);
        return new MBlockIntraNxN(mb, prediction);
    }

    public MBlockIntra16x16 readMBlockIntra16x16(InBits reader, MBlockNeighbourhood neighbourhood,
            int lumaPredictionMode, int codedBlockPatternChroma, int codedBlockPatternLuma) throws IOException {

        CoeffToken[] pred = new CoeffToken[24];
        CoeffToken[] lumaLeft = neighbourhood.getLumaLeft();
        CoeffToken[] lumaTop = neighbourhood.getLumaTop();

        pred[16] = lumaLeft != null ? lumaLeft[5] : null;
        pred[17] = lumaLeft != null ? lumaLeft[7] : null;
        pred[18] = lumaLeft != null ? lumaLeft[13] : null;
        pred[19] = lumaLeft != null ? lumaLeft[15] : null;
        pred[20] = lumaTop != null ? lumaTop[10] : null;
        pred[21] = lumaTop != null ? lumaTop[11] : null;
        pred[22] = lumaTop != null ? lumaTop[14] : null;
        pred[23] = lumaTop != null ? lumaTop[15] : null;

        CoeffToken[] tokens = new CoeffToken[16];

        int chromaPredictionMode = readUE(reader, "MBP: intra_chroma_pred_mode");
        int mbQPDelta = 0;

        mbQPDelta = readSE(reader, "mb_qp_delta");

        ResidualBlock lumaDC;
        {
            CoeffToken coeffToken = coeffTokenReader.read(reader, pred[mappingLeft4x4[0]], pred[mappingTop4x4[0]]);

            lumaDC = new ResidualBlock(cavlcReader.readCoeffs(reader, ResidualBlock.BlockType.BLOCK_LUMA_16x16_DC,
                    coeffToken));
        }

        ResidualBlock[] lumaAC = new ResidualBlock[16];
        for (int i8x8 = 0; i8x8 < 4; i8x8++) {
            for (int i4x4 = 0; i4x4 < 4; i4x4++) {
                int blkAddr = i8x8 * 4 + i4x4;
                if ((codedBlockPatternLuma & (1 << i8x8)) > 0) {
                    CoeffToken coeffToken = coeffTokenReader.read(reader, pred[mappingLeft4x4[blkAddr]],
                            pred[mappingTop4x4[blkAddr]]);

                    lumaAC[blkAddr] = new ResidualBlock(cavlcReader.readCoeffs(reader,
                            ResidualBlock.BlockType.BLOCK_LUMA_16x16_AC, coeffToken));
                    pred[blkAddr] = coeffToken;
                    tokens[blkAddr] = coeffToken;
                } else {
                    pred[blkAddr] = new CoeffToken(0, 0);
                    tokens[blkAddr] = pred[blkAddr];
                }
            }
        }

        CodedChroma chroma = chromaReader.readChroma(reader, codedBlockPatternChroma, neighbourhood);

        return new MBlockIntra16x16(chroma, mbQPDelta, lumaDC, lumaAC, tokens, lumaPredictionMode, chromaPredictionMode);
    }

    protected int readCodedBlockPattern(InBits reader) throws IOException {
        int val = readUE(reader, "coded_block_pattern");
        return getCodedBlockPatternMapping()[val];
    }

    protected int[] getCodedBlockPatternMapping() {
        if (chromaFormat == ChromaFormat.MONOCHROME) {
            return coded_block_pattern_intra_monochrome;
        } else {
            return coded_block_pattern_intra_color;
        }
    }

}
