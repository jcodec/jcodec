package org.jcodec.codecs.h264.io.read;

import static org.jcodec.codecs.h264.io.read.CAVLCReader.readBool;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readU;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readZeroBitCount;

import java.io.IOException;

import org.jcodec.codecs.h264.io.model.ChromaFormat;
import org.jcodec.codecs.h264.io.model.CoeffToken;
import org.jcodec.codecs.h264.io.model.ResidualBlock.BlockType;
import org.jcodec.codecs.h264.io.model.RunBeforeToken;
import org.jcodec.codecs.h264.io.model.TotalZerosToken;
import org.jcodec.common.io.InBits;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class ResidualCoeffsCAVLCReader {

    private ChromaFormat chromaFormat;

    public ResidualCoeffsCAVLCReader(ChromaFormat chromaFormat) {
        this.chromaFormat = chromaFormat;
    }

    public int[] readCoeffs(InBits in, BlockType blockType, CoeffToken coeffToken) throws IOException {

        int maxCoeff = blockType.getMaxCoeffs();
        if (blockType == blockType.BLOCK_CHROMA_DC)
            maxCoeff = 16 / (chromaFormat.getSubHeight() * chromaFormat.getSubHeight());

        int[] coeffLevel = new int[maxCoeff];

        if (coeffToken.totalCoeff > 0) {
            int suffixLength;
            if (coeffToken.totalCoeff > 10 && coeffToken.trailingOnes < 3) {
                suffixLength = 1;
            } else {
                suffixLength = 0;
            }

            int[] level = new int[coeffToken.totalCoeff];
            for (int i = 0; i < coeffToken.totalCoeff; i++) {
                if (i < coeffToken.trailingOnes) {
                    boolean trailing_ones_sign_flag = readBool(in, "RB: trailing_ones_sign_flag");
                    level[i] = 1 - 2 * (trailing_ones_sign_flag ? 1 : 0);
                } else {
                    int level_prefix = readZeroBitCount(in, blockType.getLabel() + " level prefix");
                    int levelSuffixSize = suffixLength;
                    if (level_prefix == 14 && suffixLength == 0)
                        levelSuffixSize = 4;
                    if (level_prefix >= 15)
                        levelSuffixSize = level_prefix - 3;

                    int levelCode = (Min(15, level_prefix) << suffixLength);
                    if (levelSuffixSize > 0) {
                        int level_suffix = readU(in, levelSuffixSize, "RB: level_suffix");
                        levelCode += level_suffix;
                    }
                    if (level_prefix >= 15 && suffixLength == 0)
                        levelCode += 15;
                    if (level_prefix >= 16)
                        levelCode += (1 << (level_prefix - 3)) - 4096;
                    if (i == coeffToken.trailingOnes && coeffToken.trailingOnes < 3)
                        levelCode += 2;
                    if (levelCode % 2 == 0)
                        level[i] = (levelCode + 2) >> 1;
                    else
                        level[i] = (-levelCode - 1) >> 1;
                    if (suffixLength == 0)
                        suffixLength = 1;
                    if (Abs(level[i]) > (3 << (suffixLength - 1)) && suffixLength < 6)
                        suffixLength++;
                }
            }
            int zerosLeft;
            if (coeffToken.totalCoeff < maxCoeff) {
                int total_zeros;
                if (blockType == BlockType.BLOCK_CHROMA_DC && chromaFormat == ChromaFormat.YUV_420) {
                    total_zeros = TotalZerosToken.readCr2x2(in, coeffToken.totalCoeff);
                } else if (blockType == BlockType.BLOCK_CHROMA_DC && chromaFormat == ChromaFormat.YUV_422) {
                    total_zeros = TotalZerosToken.readCr2x2(in, coeffToken.totalCoeff);
                } else {
                    total_zeros = TotalZerosToken.read4x4(in, coeffToken.totalCoeff);
                }

                zerosLeft = total_zeros;
            } else
                zerosLeft = 0;
            int[] run = new int[coeffToken.totalCoeff];
            for (int i = 0; i < coeffToken.totalCoeff - 1; i++) {
                if (zerosLeft > 0) {
                    int run_before = RunBeforeToken.read(in, zerosLeft);
                    run[i] = run_before;
                } else
                    run[i] = 0;
                zerosLeft = zerosLeft - run[i];
            }
            run[coeffToken.totalCoeff - 1] = zerosLeft;
            int coeffNum = -1;
            for (int i = coeffToken.totalCoeff - 1; i >= 0; i--) {
                coeffNum += run[i] + 1;
                coeffLevel[coeffNum] = level[i];
            }
        }

        return coeffLevel;
    }

    private static int Min(int i, int level_prefix) {
        return i < level_prefix ? i : level_prefix;
    }

    private static int Abs(int i) {
        return i < 0 ? -i : i;
    }

}
