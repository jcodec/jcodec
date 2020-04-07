package org.jcodec.codecs.h264.io;

import org.jcodec.codecs.h264.H264Const;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.BitWriter;
import org.jcodec.common.io.VLC;
import org.jcodec.common.tools.MathUtil;

import static org.jcodec.codecs.h264.decode.CAVLCReader.readU;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readZeroBitCount;

public class CAVLCUtil {

    static void writeLevels(BitWriter out, int[] levels, int totalCoeff, int trailingOnes) {

        int suffixLen = totalCoeff > 10 && trailingOnes < 3 ? 1 : 0;
        for (int i = totalCoeff - trailingOnes - 1; i >= 0; i--) {
            int absLev = CAVLC.Companion.unsigned(levels[i]);
            if (i == totalCoeff - trailingOnes - 1 && trailingOnes < 3)
                absLev -= 2;

            int prefix = absLev >> suffixLen;
            if (suffixLen == 0 && prefix < 14 || suffixLen > 0 && prefix < 15) {
                out.writeNBit(1, prefix + 1);
                out.writeNBit(absLev, suffixLen);
            } else if (suffixLen == 0 && absLev < 30) {
                out.writeNBit(1, 15);
                out.writeNBit(absLev - 14, 4);
            } else {
                if (suffixLen == 0)
                    absLev -= 15;
                int len, code;
                for (len = 12; (code = absLev - (len + 3 << suffixLen) - (1 << len) + 4096) >= (1 << len); len++)
                    ;
                out.writeNBit(1, len + 4);
                out.writeNBit(code, len);
            }
            if (suffixLen == 0)
                suffixLen = 1;
            if (MathUtil.abs(levels[i]) > (3 << (suffixLen - 1)) && suffixLen < 6)
                suffixLen++;
        }
    }


    public static int readCoeffs(BitReader _in, VLC coeffTokenTab, VLC[] totalZerosTab, int[] coeffLevel, int firstCoeff,
                                 int nCoeff, int[] zigzag) {
        int coeffToken = coeffTokenTab.readVLC(_in);
        int totalCoeff = CAVLC.Companion.totalCoeff(coeffToken);
        int trailingOnes = CAVLC.Companion.trailingOnes(coeffToken);

        if (totalCoeff > 0) {
            int suffixLength = totalCoeff > 10 && trailingOnes < 3 ? 1 : 0;

            int[] level = new int[totalCoeff];
            int i;
            for (i = 0; i < trailingOnes; i++)
                level[i] = 1 - 2 * _in.read1Bit();

            for (; i < totalCoeff; i++) {
                int level_prefix = readZeroBitCount(_in, "");
                int levelSuffixSize = suffixLength;
                if (level_prefix == 14 && suffixLength == 0)
                    levelSuffixSize = 4;
                if (level_prefix >= 15)
                    levelSuffixSize = level_prefix - 3;

                int levelCode = (CAVLC.Companion.Min(15, level_prefix) << suffixLength);
                if (levelSuffixSize > 0) {
                    int level_suffix = readU(_in, levelSuffixSize, "RB: level_suffix");
                    levelCode += level_suffix;
                }
                if (level_prefix >= 15 && suffixLength == 0)
                    levelCode += 15;
                if (level_prefix >= 16)
                    levelCode += (1 << (level_prefix - 3)) - 4096;
                if (i == trailingOnes && trailingOnes < 3)
                    levelCode += 2;

                if (levelCode % 2 == 0)
                    level[i] = (levelCode + 2) >> 1;
                else
                    level[i] = (-levelCode - 1) >> 1;

                if (suffixLength == 0)
                    suffixLength = 1;
                if (CAVLC.Companion.Abs(level[i]) > (3 << (suffixLength - 1)) && suffixLength < 6)
                    suffixLength++;
            }

            int zerosLeft;
            if (totalCoeff < nCoeff) {
                if (coeffLevel.length == 4) {
                    zerosLeft = H264Const.totalZeros4[totalCoeff - 1].readVLC(_in);
                } else if (coeffLevel.length == 8) {
                    zerosLeft = H264Const.totalZeros8[totalCoeff - 1].readVLC(_in);
                } else {
                    zerosLeft = H264Const.totalZeros16[totalCoeff - 1].readVLC(_in);
                }
            } else
                zerosLeft = 0;

            int[] runs = new int[totalCoeff];
            int r;
            for (r = 0; r < totalCoeff - 1 && zerosLeft > 0; r++) {
                int run = H264Const.run[Math.min(6, zerosLeft - 1)].readVLC(_in);
                zerosLeft -= run;
                runs[r] = run;
            }
            runs[r] = zerosLeft;

            for (int j = totalCoeff - 1, cn = 0; j >= 0 && cn < nCoeff; j--, cn++) {
                cn += runs[j];
                coeffLevel[zigzag[cn + firstCoeff]] = level[j];
            }
        }

        // System.out.print("[");
        // for (int i = 0; i < nCoeff; i++)
        // System.out.print(coeffLevel[i + firstCoeff] + ", ");
        // System.out.println("]");

        return coeffToken;
    }


    static int writeBlockGen(BitWriter out, int[] coeff, VLC[] totalZerosTab, int firstCoeff, int maxCoeff,
                             int[] scan, VLC coeffTokenTab) {
        int trailingOnes = 0, totalCoeff = 0, totalZeros = 0;
        int[] runBefore = new int[maxCoeff];
        int[] levels = new int[maxCoeff];
        for (int i = 0; i < maxCoeff; i++) {
            int c = coeff[scan[i + firstCoeff]];
            if (c == 0) {
                runBefore[totalCoeff]++;
                totalZeros++;
            } else {
                levels[totalCoeff++] = c;
            }
        }
        if (totalCoeff < maxCoeff)
            totalZeros -= runBefore[totalCoeff];

        for (trailingOnes = 0; trailingOnes < totalCoeff && trailingOnes < 3
                && Math.abs(levels[totalCoeff - trailingOnes - 1]) == 1; trailingOnes++)
            ;

        int coeffToken = H264Const.coeffToken(totalCoeff, trailingOnes);

        coeffTokenTab.writeVLC(out, coeffToken);

        if (totalCoeff > 0) {
            CAVLC.Companion.writeTrailingOnes(out, levels, totalCoeff, trailingOnes);
            writeLevels(out, levels, totalCoeff, trailingOnes);

            if (totalCoeff < maxCoeff) {
                totalZerosTab[totalCoeff - 1].writeVLC(out, totalZeros);
                CAVLC.Companion.writeRuns(out, runBefore, totalCoeff, totalZeros);
            }
        }
        return coeffToken;
    }


}
