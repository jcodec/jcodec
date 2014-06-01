package org.jcodec.codecs.h264.tweak;

import java.util.Arrays;

import org.jcodec.codecs.h264.H264Const.PartPred;
import org.jcodec.codecs.h264.io.model.SliceHeader;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * h264 handler that just logs incoming symbols to an output
 * 
 * @author The JCodec project
 * 
 */
public class LogH264Handler implements H264Handler {

    @Override
    public void macroblockINxN(int mbAddr, int qpDelta, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC,
            int[] lumaModes, int chromaMode) {
        System.out.println("MACROBLOCK[" + mbAddr + "]: INxN");

        System.out.println("PREDICTION NxN " + Arrays.toString(lumaModes) + ", " + chromaMode);

        printMBResidual(lumaResidual, chromaDC, chromaAC);
    }

    private void printMBResidual(int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC) {
        for (int i = 0; i < 16; i++) {
            if (lumaResidual[i] == null)
                continue;

            System.out.println("LUMA BLOCK " + i);
            for (int j = 0; j < lumaResidual[i].length; j++)
                System.out.print(lumaResidual[i][j] + ",");
            System.out.println();
        }

        printChroma(chromaDC, chromaAC);
    }

    private void printChroma(int[][] chromaDC, int[][][] chromaAC) {
        for (int i = 0; i < 2; i++) {
            if (chromaDC[i] != null) {
                System.out.println("CHROMA DC BLOCK [" + i + "]");
                for (int j = 0; j < chromaDC[i].length; j++)
                    System.out.print(chromaDC[i][j] + ",");
                System.out.println();
            }
        }
        for (int i = 0; i < 2; i++) {
            if (chromaAC[i] != null) {
                for (int k = 0; k < 4; k++) {
                    System.out.println("CHROMA [" + i + "] AC BLOCK " + k);
                    if (chromaAC[i][k] == null)
                        continue;
                    for (int j = 1; j < chromaAC[i][k].length; j++)
                        System.out.print(chromaAC[i][k][j] + ",");
                    System.out.println();
                }
            }
        }
    }

    @Override
    public void macroblockI16x16(int mbAddr, int qpDelta, int[] lumaDC, int[][] lumaAC, int lumaMode, int chromaMode,
            int[][] chromaDC, int[][][] chromaAC) {
        System.out.println("MACROBLOCK[" + mbAddr + "]: I16x16");
        System.out.println("PREDICTION 16x16: " + lumaMode + ", " + chromaMode);

        System.out.println("LUMA DC BLOCK");
        for (int i = 0; i < lumaDC.length; i++)
            System.out.print(lumaDC[i] + ",");
        System.out.println();

        for (int i = 0; i < 16; i++) {
            if (lumaAC[i] == null)
                continue;
            System.out.println("LUMA AC BLOCK " + i);
            for (int j = 1; j < lumaAC[i].length; j++)
                System.out.print(lumaAC[i][j] + ",");
            System.out.println();
        }

        printChroma(chromaDC, chromaAC);
    }

    @Override
    public void mblockPB16x16(int mbAddr, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC,
            int[][] mvs, PartPred pred) {
        System.out.println("MACROBLOCK[" + mbAddr + "]: PB16x16");
        for (int list = 0; list < 2; list++) {
            if (pred.usesList(list))
                System.out
                        .println("MV[" + list + "]: (" + mvs[list][0] + "," + mvs[list][1] + "," + mvs[list][2] + ")");
        }

        printMBResidual(lumaResidual, chromaDC, chromaAC);
    }

    @Override
    public void mblockPB16x8(int mbAddr, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC,
            int[][] mvs, PartPred p0, PartPred p1) {
        System.out.println("MACROBLOCK[" + mbAddr + "]: PB16x8");
        for (int list = 0; list < 2; list++) {
            if (p0.usesList(list))
                System.out
                        .println("MV[" + list + "]: (" + mvs[list][0] + "," + mvs[list][1] + "," + mvs[list][2] + ")");
            if (p1.usesList(list))
                System.out
                        .println("MV[" + list + "]: (" + mvs[list][3] + "," + mvs[list][4] + "," + mvs[list][5] + ")");
        }
        printMBResidual(lumaResidual, chromaDC, chromaAC);
    }

    @Override
    public void mblockPB8x16(int mbAddr, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC,
            int[][] mvs, PartPred p0, PartPred p1) {
        System.out.println("MACROBLOCK[" + mbAddr + "]: PB8x16");
        for (int list = 0; list < 2; list++) {
            if (p0.usesList(list))
                System.out
                        .println("MV[" + list + "]: (" + mvs[list][0] + "," + mvs[list][1] + "," + mvs[list][2] + ")");
            if (p1.usesList(list))
                System.out
                        .println("MV[" + list + "]: (" + mvs[list][3] + "," + mvs[list][4] + "," + mvs[list][5] + ")");
        }
        printMBResidual(lumaResidual, chromaDC, chromaAC);
    }

    @Override
    public void mblockPB8x8(int mbAddr, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC,
            int[][][] mvs, int[] subMbModes, PartPred l0, PartPred l02, PartPred l03, PartPred l04) {
        System.out.println("MACROBLOCK[" + mbAddr + "]: PB8x8");
        String[] modeNames = new String[] { "8x8", "8x4", "4x8", "4x4" };
        for (int list = 0; list < 2; list++) {
            for (int i = 0; i < 4; i++) {
                System.out.println("BLK: " + modeNames[subMbModes[i]]);
                int mvCount = subMbModes[i] == 0 ? 1 : (subMbModes[i] == 3 ? 4 : 2);
                for (int j = 0; j < mvCount; j++) {
                    System.out.println("MV[" + list + "]: (" + mvs[list][i][j * 3] + "," + mvs[list][i][j * 3 + 1]
                            + "," + mvs[list][i][j * 3 + 2] + ")");
                }
            }
        }
        printMBResidual(lumaResidual, chromaDC, chromaAC);
    }

    @Override
    public void mblockBDirect(int mbAddr, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC) {
        System.out.println("MACROBLOCK[" + mbAddr + "]: DIRECT");

        printMBResidual(lumaResidual, chromaDC, chromaAC);
    }

    @Override
    public void mblockPBSkip(int mbIdx, int mvX, int mvY) {
    }

    @Override
    public void slice(SliceHeader sh) {
        System.out.println("SLICE");
    }
}
