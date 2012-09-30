package org.jcodec.codecs.h264.io.read;

import static org.jcodec.codecs.h264.io.read.CAVLCReader.readBool;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readNBit;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readUE;

import java.io.IOException;

import org.jcodec.codecs.h264.io.model.IntraNxNPrediction;
import org.jcodec.common.io.InBits;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Reads intra predictions parts of macroblocks
 * 
 * @author Jay Codec
 * 
 */
public class IntraPredictionReader {
    static int[] mappingTop = { 16, 17, 0, 1, 18, 19, 4, 5, 2, 3, 8, 9, 6, 7, 12, 13 };
    static int[] mappingLeft = { 20, 0, 21, 2, 1, 4, 3, 6, 22, 8, 23, 10, 9, 12, 11, 14 };

    /**
     * Reads intra 4x4 prediction
     * 
     * Requires predictions of top and left macroblocks
     * 
     * @param reader
     * @param predLeft
     * @param predTop
     * @return
     * @throws IOException
     */
    public static IntraNxNPrediction readPrediction4x4(InBits reader, IntraNxNPrediction predLeft,
            IntraNxNPrediction predTop, boolean leftAvailable, boolean topAvailable) throws IOException {

        int[] predModes = new int[24];
        if (predTop == null) {
            if (!topAvailable) {
                predModes[16] = predModes[17] = predModes[18] = predModes[19] = -1;
            } else {
                predModes[16] = predModes[17] = predModes[18] = predModes[19] = -2;
            }
        } else {
            predModes[16] = predTop.getLumaModes()[10];
            predModes[17] = predTop.getLumaModes()[11];
            predModes[18] = predTop.getLumaModes()[14];
            predModes[19] = predTop.getLumaModes()[15];
        }

        if (predLeft == null) {
            if (!leftAvailable) {
                predModes[20] = predModes[21] = predModes[22] = predModes[23] = -1;
            } else {
                predModes[20] = predModes[21] = predModes[22] = predModes[23] = -2;
            }
        } else {
            predModes[20] = predLeft.getLumaModes()[5];
            predModes[21] = predLeft.getLumaModes()[7];
            predModes[22] = predLeft.getLumaModes()[13];
            predModes[23] = predLeft.getLumaModes()[15];
        }

        int[] lumaModes = new int[16];
        for (int iBlk = 0; iBlk < 16; iBlk++) {

            int predIntra4x4PredMode = predictMode(predModes, iBlk);

            boolean prev_intra4x4_pred_mode_flag = readBool(reader, "MBP: prev_intra4x4_pred_mode_flag");

            if (!prev_intra4x4_pred_mode_flag) {
                int rem_intra4x4_pred_mode = readNBit(reader, 3, "MB: rem_intra4x4_pred_mode");
                if (rem_intra4x4_pred_mode < predIntra4x4PredMode)
                    lumaModes[iBlk] = rem_intra4x4_pred_mode;
                else
                    lumaModes[iBlk] = rem_intra4x4_pred_mode + 1;

            } else {
                lumaModes[iBlk] = predIntra4x4PredMode;
            }
            predModes[iBlk] = lumaModes[iBlk];
        }

        int chromaMode = readUE(reader, "intra_chroma_pred_mode");

        return new IntraNxNPrediction(lumaModes, chromaMode);
    }

    private static int predictMode(int[] predModes, int iBlk) {
        int predTop = predModes[mappingTop[iBlk]];
        int predLeft = predModes[mappingLeft[iBlk]];

        boolean dcMode = predTop == -1 || predLeft == -1;
        int top = !dcMode && predTop >= 0 ? predTop : 2;
        int left = !dcMode && predLeft >= 0 ? predLeft : 2;

        return top < left ? top : left;
    }

    public static IntraNxNPrediction readPrediction8x8(InBits reader) throws IOException {
        throw new UnsupportedOperationException("8x8 not supported");
        // boolean[] prev_intra8x8_pred_mode_flag = new boolean[4];
        // int[] rem_intra8x8_pred_mode = new int[4];
        // for (int luma8x8BlkIdx = 0; luma8x8BlkIdx < 4; luma8x8BlkIdx++) { //
        // 2
        // prev_intra8x8_pred_mode_flag[luma8x8BlkIdx] = reader
        // .readBool("MBP: prev_intra8x8_pred_mode_flag");
        // if (!prev_intra8x8_pred_mode_flag[luma8x8BlkIdx]) { // 2
        // rem_intra8x8_pred_mode[luma8x8BlkIdx] = (int) reader.readNBit(
        // 3, "MB: rem_intra8x8_pred_mode");
        // }
        // }
        // int chromaMode = reader.readUE("intra_chroma_pred_mode");
        //
        // return new IntraNxNPrediction(intra4x4PredModes, chromaMode);
    }
}