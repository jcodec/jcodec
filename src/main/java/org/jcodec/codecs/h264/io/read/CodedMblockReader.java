package org.jcodec.codecs.h264.io.read;

import static org.jcodec.codecs.h264.io.read.CAVLCReader.readSE;

import java.io.IOException;

import org.jcodec.codecs.h264.io.model.ChromaFormat;
import org.jcodec.codecs.h264.io.model.CodedChroma;
import org.jcodec.codecs.h264.io.model.CoeffToken;
import org.jcodec.codecs.h264.io.model.MBlockNeighbourhood;
import org.jcodec.codecs.h264.io.model.MBlockWithResidual;
import org.jcodec.codecs.h264.io.model.ResidualBlock;
import org.jcodec.common.io.InBits;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Base reader class for coded macroblocks
 * 
 * @author Jay Codec
 * 
 */
public abstract class CodedMblockReader {

    protected boolean entropyCoding;

    private ResidualCoeffsCAVLCReader cavlcReader;
    private CoeffTokenReader coeffTokenReader;
    private ResidualCoeffsCABACReader cabacReader;
    private ChromaReader chromaReader;

    static int[] mappingTop4x4 = { 20, 21, 0, 1, 22, 23, 4, 5, 2, 3, 8, 9, 6, 7, 12, 13 };
    static int[] mappingLeft4x4 = { 16, 0, 17, 2, 1, 4, 3, 6, 18, 8, 19, 10, 9, 12, 11, 14 };

    public CodedMblockReader(ChromaFormat chromaFormat, boolean entropyCoding) {
        this.entropyCoding = entropyCoding;

        coeffTokenReader = new CoeffTokenReader(chromaFormat);
        cavlcReader = new ResidualCoeffsCAVLCReader(chromaFormat);
        cabacReader = new ResidualCoeffsCABACReader();
        chromaReader = new ChromaReader(chromaFormat, entropyCoding);
    }

    public MBlockWithResidual readMBlockWithResidual(InBits in, MBlockNeighbourhood neighbourhood,
            int codedBlockPattern, boolean transform8x8Used) throws IOException {
        int codedBlockPatternLuma = codedBlockPattern % 16;
        int codedBlockPatternChroma = codedBlockPattern / 16;

        int mb_qp_delta = 0;
        if (codedBlockPatternLuma > 0 || codedBlockPatternChroma > 0) {
            mb_qp_delta = readSE(in, "mb_qp_delta");
        }

        BlocksWithTokens luma = readLumaNxN(in, neighbourhood, transform8x8Used, codedBlockPatternLuma);

        CodedChroma chroma = chromaReader.readChroma(in, codedBlockPatternChroma, neighbourhood);

        return new MBlockWithResidual(mb_qp_delta, chroma, luma.getToken(), luma.getBlock()) {
        };
    }

    private BlocksWithTokens readLumaNxN(InBits reader, MBlockNeighbourhood neighbourhood, boolean transform8x8Used,
            int codedBlockPatternLuma) throws IOException {

        if (entropyCoding) {
            ResidualBlock[] lumaBlock;
            if (transform8x8Used) {
                lumaBlock = readResidualLuma8x8Cabac(reader, codedBlockPatternLuma);
            } else {
                lumaBlock = readResidualLuma4x4Cabac(reader, codedBlockPatternLuma);
            }
            return new BlocksWithTokens(lumaBlock, null);

        } else {
            BlocksWithTokens luma;
            if (transform8x8Used) {
                luma = readResidualLuma8x8(reader, neighbourhood, codedBlockPatternLuma);
            } else {
                luma = readResidualLuma4x4(reader, neighbourhood, codedBlockPatternLuma);
            }
            return luma;
        }
    }

    protected BlocksWithTokens readResidualLuma8x8(InBits reader, MBlockNeighbourhood neighbourhood, int pattern)
            throws IOException {

        BlocksWithTokens luma4x4 = readResidualLuma4x4(reader, neighbourhood, pattern);

        ResidualBlock[] lumaLevel8x8 = new ResidualBlock[4];
        ResidualBlock[] lumaLevel = luma4x4.getBlock();

        for (int i8x8 = 0; i8x8 < 4; i8x8++) {
            int[] lumaCoeffs8x8 = new int[64];

            for (int i4x4 = 0; i4x4 < 4; i4x4++) {
                int blkAddr = i8x8 * 4 + i4x4;

                for (int i = 0; i < 16; i++) {
                    lumaCoeffs8x8[4 * i + i4x4] = lumaLevel[blkAddr].getCoeffs()[i];
                }
            }
            lumaLevel8x8[i8x8] = new ResidualBlock(lumaCoeffs8x8);
        }

        luma4x4.setBlock(lumaLevel8x8);

        return luma4x4;
    }

    protected BlocksWithTokens readResidualLuma4x4(InBits reader, MBlockNeighbourhood neighbourhood, int pattern)
            throws IOException {
        ResidualBlock[] lumaLevel = new ResidualBlock[16];

        CoeffToken[] lumaLeft = neighbourhood.getLumaLeft();
        CoeffToken[] lumaTop = neighbourhood.getLumaTop();

        CoeffToken[] pred = new CoeffToken[24];
        pred[16] = lumaLeft != null ? lumaLeft[5] : null;
        pred[17] = lumaLeft != null ? lumaLeft[7] : null;
        pred[18] = lumaLeft != null ? lumaLeft[13] : null;
        pred[19] = lumaLeft != null ? lumaLeft[15] : null;
        pred[20] = lumaTop != null ? lumaTop[10] : null;
        pred[21] = lumaTop != null ? lumaTop[11] : null;
        pred[22] = lumaTop != null ? lumaTop[14] : null;
        pred[23] = lumaTop != null ? lumaTop[15] : null;

        CoeffToken[] tokens = new CoeffToken[16];

        for (int i8x8 = 0; i8x8 < 4; i8x8++) {
            for (int i4x4 = 0; i4x4 < 4; i4x4++) {
                int blkAddr = i8x8 * 4 + i4x4;
                if ((pattern & (1 << i8x8)) > 0) {
                    CoeffToken coeffToken = coeffTokenReader.read(reader, pred[mappingLeft4x4[blkAddr]],
                            pred[mappingTop4x4[blkAddr]]);

                    lumaLevel[blkAddr] = new ResidualBlock(cavlcReader.readCoeffs(reader,
                            ResidualBlock.BlockType.BLOCK_LUMA_4x4, coeffToken));

                    pred[blkAddr] = coeffToken;
                    tokens[blkAddr] = coeffToken;
                } else {
                    pred[blkAddr] = new CoeffToken(0, 0);
                    tokens[blkAddr] = pred[blkAddr];
                }
            }
        }

        return new BlocksWithTokens(lumaLevel, tokens);
    }

    protected ResidualBlock[] readResidualLuma4x4Cabac(InBits reader, int pattern) throws IOException {

        ResidualBlock[] lumaLevel = new ResidualBlock[16];

        for (int i8x8 = 0; i8x8 < 4; i8x8++) {
            for (int i4x4 = 0; i4x4 < 4; i4x4++) {
                if ((pattern & (1 << i8x8)) > 0) {
                    lumaLevel[i8x8 * 4 + i4x4] = new ResidualBlock(cabacReader.readCoeffs(reader));
                }
            }
        }

        return lumaLevel;
    }

    protected ResidualBlock[] readResidualLuma8x8Cabac(InBits reader, int pattern) throws IOException {

        ResidualBlock[] lumaLevel8x8 = new ResidualBlock[4];

        for (int i8x8 = 0; i8x8 < 4; i8x8++) {
            if ((pattern & (1 << i8x8)) > 0) {
                lumaLevel8x8[i8x8] = new ResidualBlock(cabacReader.readCoeffs(reader));
            }
        }

        return lumaLevel8x8;
    }
}
