package org.jcodec.codecs.h264.io.read;

import java.io.IOException;

import org.jcodec.codecs.h264.io.model.ChromaFormat;
import org.jcodec.codecs.h264.io.model.CodedChroma;
import org.jcodec.codecs.h264.io.model.CoeffToken;
import org.jcodec.codecs.h264.io.model.MBlockNeighbourhood;
import org.jcodec.codecs.h264.io.model.ResidualBlock;
import org.jcodec.common.io.InBits;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Chroma reader of the codec macroblock of h264 bitstream
 * 
 * @author Jay Codec
 * 
 */
public class ChromaReader {
    private ChromaFormat chromaFormat;
    private boolean entropyCoding;

    private CoeffTokenReader coeffTokenReader;
    private ResidualCoeffsCABACReader cabacReader;
    private ResidualCoeffsCAVLCReader cavlcReader;

    static int[] mappingTop4x4 = { 20, 21, 0, 1, 22, 23, 4, 5, 2, 3, 8, 9, 6, 7, 12, 13 };
    static int[] mappingLeft4x4 = { 16, 0, 17, 2, 1, 4, 3, 6, 18, 8, 19, 10, 9, 12, 11, 14 };

    static int[] mappingTop4x2 = { 10, 11, 12, 13, 0, 1, 2, 3 };
    static int[] mappingLeft4x2 = { 8, 0, 1, 2, 9, 4, 5, 6 };

    static int[] mappingTop2x2 = { 6, 7, 0, 1 };
    static int[] mappingLeft2x2 = { 4, 0, 5, 2 };

    public ChromaReader(ChromaFormat chromaFormat, boolean entropyCoding) {
        this.chromaFormat = chromaFormat;
        this.entropyCoding = entropyCoding;

        coeffTokenReader = new CoeffTokenReader(chromaFormat);
        cavlcReader = new ResidualCoeffsCAVLCReader(chromaFormat);
        cabacReader = new ResidualCoeffsCABACReader();
    }

    public CodedChroma readChroma(InBits reader, int pattern, MBlockNeighbourhood neighbourhood) throws IOException {

        ResidualBlock cbDC = null;
        ResidualBlock crDC = null;

        if (chromaFormat != ChromaFormat.MONOCHROME) {
            if (!entropyCoding) {
                BlocksWithTokens cbAC = null;
                BlocksWithTokens crAC = null;
                if ((pattern & 3) > 0) {
                    cbDC = readChromaDC(reader);
                    crDC = readChromaDC(reader);
                }

                if ((pattern & 2) > 0) {
                    cbAC = readChromaAC(reader, neighbourhood.getCbLeft(), neighbourhood.getCbTop());
                    crAC = readChromaAC(reader, neighbourhood.getCrLeft(), neighbourhood.getCrTop());
                }

                if (cbAC == null)
                    cbAC = handleNullResidual();

                if (crAC == null)
                    crAC = handleNullResidual();

                return new CodedChroma(cbDC, cbAC.getBlock(), crDC, crAC.getBlock(), cbAC.getToken(), crAC.getToken());
            } else {
                ResidualBlock[] cbAC = null;
                ResidualBlock[] crAC = null;
                if ((pattern & 3) > 0) {
                    cbDC = readChromaDC(reader);
                    crDC = readChromaDC(reader);
                }

                if ((pattern & 2) > 0) {
                    cbAC = readChromaACCabac(reader);
                    crAC = readChromaACCabac(reader);
                }

                return new CodedChroma(cbDC, cbAC, crDC, crAC, null, null);
            }
        }

        return null;
    }

    private BlocksWithTokens handleNullResidual() {

        int nTokens = 16 / (chromaFormat.getSubWidth() * chromaFormat.getSubHeight());

        CoeffToken[] tokens = new CoeffToken[nTokens];
        ResidualBlock[] blocks = new ResidualBlock[nTokens];

        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = new CoeffToken(0, 0);
            blocks[i] = new ResidualBlock(new int[16]);
        }

        return new BlocksWithTokens(blocks, tokens);
    }

    private ResidualBlock readChromaDC(InBits reader) throws IOException {
        ResidualBlock blk;

        if (!entropyCoding) {
            CoeffToken coeffToken = coeffTokenReader.readForChromaDC(reader);

            blk = new ResidualBlock(cavlcReader.readCoeffs(reader, ResidualBlock.BlockType.BLOCK_CHROMA_DC, coeffToken));
        } else {
            blk = new ResidualBlock(cabacReader.readCoeffs(reader));
        }

        return blk;
    }

    private BlocksWithTokens readChromaAC(InBits reader, CoeffToken[] left, CoeffToken[] top) throws IOException {

        if (chromaFormat == ChromaFormat.YUV_444)
            return readChromaAC444(reader, left, top);

        else if (chromaFormat == ChromaFormat.YUV_422)
            return readChromaAC422(reader, left, top);

        else
            return readChromaAC420(reader, left, top);

    }

    private BlocksWithTokens readChromaAC444(InBits reader, CoeffToken[] left, CoeffToken[] top) throws IOException {
        CoeffToken[] pred = new CoeffToken[24];
        pred[16] = left[5];
        pred[17] = left[7];
        pred[18] = left[13];
        pred[19] = left[15];
        pred[20] = top[10];
        pred[21] = top[11];
        pred[22] = top[14];
        pred[23] = top[15];

        return readChromaACSub(reader, pred, 4, mappingLeft4x4, mappingTop4x4);

    }

    private BlocksWithTokens readChromaAC422(InBits reader, CoeffToken[] left, CoeffToken[] top) throws IOException {

        CoeffToken[] pred = new CoeffToken[14];
        pred[8] = left[5];
        pred[9] = left[7];
        pred[10] = left[2];
        pred[11] = left[3];
        pred[12] = left[6];
        pred[13] = left[7];

        return readChromaACSub(reader, pred, 2, mappingLeft4x2, mappingTop4x2);

    }

    private BlocksWithTokens readChromaAC420(InBits reader, CoeffToken[] left, CoeffToken[] top) throws IOException {

        CoeffToken[] pred = new CoeffToken[8];
        pred[4] = left != null ? left[1] : null;
        pred[5] = left != null ? left[3] : null;
        pred[6] = top != null ? top[2] : null;
        pred[7] = top != null ? top[3] : null;

        return readChromaACSub(reader, pred, 1, mappingLeft2x2, mappingTop2x2);
    }

    private BlocksWithTokens readChromaACSub(InBits reader, CoeffToken[] pred, int NumC8x8, int[] mapLeft, int[] mapTop)
            throws IOException {

        CoeffToken[] tokens = new CoeffToken[NumC8x8 * 4];
        ResidualBlock[] chromaACLevel = new ResidualBlock[NumC8x8 * 4];
        for (int i8x8 = 0; i8x8 < NumC8x8; i8x8++) {
            for (int i4x4 = 0; i4x4 < 4; i4x4++) {
                int blkAddr = i8x8 * 4 + i4x4;
                CoeffToken coeffToken = coeffTokenReader.read(reader, pred[mapLeft[blkAddr]], pred[mapTop[blkAddr]]);

                chromaACLevel[blkAddr] = new ResidualBlock(cavlcReader.readCoeffs(reader,
                        ResidualBlock.BlockType.BLOCK_CHROMA_AC, coeffToken));

                pred[blkAddr] = coeffToken;
                tokens[blkAddr] = coeffToken;
            }
        }

        return new BlocksWithTokens(chromaACLevel, tokens);
    }

    private ResidualBlock[] readChromaACCabac(InBits reader) throws IOException {

        int NumC8x8 = 4 / (chromaFormat.getSubWidth() * chromaFormat.getSubHeight());

        ResidualBlock[] chromaACLevel = new ResidualBlock[NumC8x8 * 4];
        for (int i8x8 = 0; i8x8 < NumC8x8; i8x8++) {
            for (int i4x4 = 0; i4x4 < 4; i4x4++) {
                chromaACLevel[i8x8 * 4 + i4x4] = new ResidualBlock(cabacReader.readCoeffs(reader));
            }
        }

        return chromaACLevel;
    }
}
