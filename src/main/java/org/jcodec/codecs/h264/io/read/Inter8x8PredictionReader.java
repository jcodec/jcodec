package org.jcodec.codecs.h264.io.read;

import static org.jcodec.codecs.h264.io.model.SubMBType.Bi_4x4;
import static org.jcodec.codecs.h264.io.model.SubMBType.Bi_4x8;
import static org.jcodec.codecs.h264.io.model.SubMBType.Bi_8x4;
import static org.jcodec.codecs.h264.io.model.SubMBType.Bi_8x8;
import static org.jcodec.codecs.h264.io.model.SubMBType.Direct_8x8;
import static org.jcodec.codecs.h264.io.model.SubMBType.L0_4x4;
import static org.jcodec.codecs.h264.io.model.SubMBType.L0_4x8;
import static org.jcodec.codecs.h264.io.model.SubMBType.L0_8x4;
import static org.jcodec.codecs.h264.io.model.SubMBType.L0_8x8;
import static org.jcodec.codecs.h264.io.model.SubMBType.L1_4x4;
import static org.jcodec.codecs.h264.io.model.SubMBType.L1_4x8;
import static org.jcodec.codecs.h264.io.model.SubMBType.L1_8x4;
import static org.jcodec.codecs.h264.io.model.SubMBType.L1_8x8;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readSE;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readTE;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readUE;

import java.io.IOException;

import org.jcodec.codecs.h264.io.model.Inter8x8Prediction;
import org.jcodec.codecs.h264.io.model.MBPartPredMode;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.codecs.h264.io.model.SubMBType;
import org.jcodec.codecs.h264.io.model.Vector;
import org.jcodec.common.io.InBits;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Reads prediction for Inter 8x8 macroblock.
 * 
 * Unlike other types of inter macroblock it's parts are further subdivided. So
 * prediction is readed for each individual part
 * 
 * 
 * @author Jay Codec
 * 
 */
public class Inter8x8PredictionReader {
    private int numRefIdxL0Active;
    private int numRefIdxL1Active;

    public Inter8x8PredictionReader(int numRefIdxL0Active, int numRefIdxL1Active) {
        this.numRefIdxL0Active = numRefIdxL0Active;
        this.numRefIdxL1Active = numRefIdxL1Active;
    }

    SubMBType[] bMapping = { Direct_8x8, L0_8x8, L1_8x8, Bi_8x8, L0_8x4, L0_4x8, L1_8x4, L1_4x8, Bi_8x4, Bi_4x8,
            L0_4x4, L1_4x4, Bi_4x4 };
    SubMBType[] pMapping = { L0_8x8, L0_8x4, L0_4x8, L0_4x4 };

    public Inter8x8Prediction read(InBits reader, int mb_type, SliceType sliceType, boolean mb_field_decoding_flag)
            throws IOException {

        SubMBType[] subMbTypes = new SubMBType[4];
        for (int mbPartIdx = 0; mbPartIdx < 4; mbPartIdx++) {
            int subMbTypeCode = readUE(reader, "SUB: sub_mb_type");
            if (sliceType == SliceType.B)
                subMbTypes[mbPartIdx] = bMapping[subMbTypeCode];
            else if (sliceType == SliceType.P)
                subMbTypes[mbPartIdx] = pMapping[subMbTypeCode];
            else
                throw new IOException("Invalid bitstream: slicetype should be B or P for inter prediction");
        }

        int[] refIdxL0 = new int[4];
        for (int mbPartIdx = 0; mbPartIdx < 4; mbPartIdx++) {
            if ((numRefIdxL0Active > 1 || mb_field_decoding_flag)
                    && !(mb_type == 4 && sliceType == SliceType.P) // P_8x8ref0
                    && subMbTypes[mbPartIdx] != Direct_8x8
                    && subMbTypes[mbPartIdx].getPredMode() != MBPartPredMode.Pred_L1) {
                refIdxL0[mbPartIdx] = readTE(reader, numRefIdxL0Active - 1);
            }
        }
        int[] refIdxL1 = new int[4];
        for (int mbPartIdx = 0; mbPartIdx < 4; mbPartIdx++) {
            if ((numRefIdxL1Active > 1 || mb_field_decoding_flag) && subMbTypes[mbPartIdx] != Direct_8x8
                    && subMbTypes[mbPartIdx].getPredMode() != MBPartPredMode.Pred_L0) {
                refIdxL1[mbPartIdx] = readTE(reader, numRefIdxL1Active - 1);
            }
        }

        Vector[][] mvdL0 = new Vector[4][];
        for (int mbPartIdx = 0; mbPartIdx < 4; mbPartIdx++) {
            if (subMbTypes[mbPartIdx] != Direct_8x8 && subMbTypes[mbPartIdx].getPredMode() != MBPartPredMode.Pred_L1) {

                mvdL0[mbPartIdx] = new Vector[subMbTypes[mbPartIdx].getNumParts()];

                for (int subMbPartIdx = 0; subMbPartIdx < subMbTypes[mbPartIdx].getNumParts(); subMbPartIdx++) {

                    int x = readSE(reader, "PRED: mvd_l0[" + mbPartIdx + "][" + subMbPartIdx + "]");
                    int y = readSE(reader, "PRED: mvd_l0[" + mbPartIdx + "][" + subMbPartIdx + "]");
                    mvdL0[mbPartIdx][subMbPartIdx] = new Vector(x, y, refIdxL0[mbPartIdx]);
                }
            }
        }

        Vector[][] mvdL1 = new Vector[4][];
        for (int mbPartIdx = 0; mbPartIdx < 4; mbPartIdx++) {
            if (subMbTypes[mbPartIdx] != Direct_8x8 && subMbTypes[mbPartIdx].getPredMode() != MBPartPredMode.Pred_L0) {

                mvdL1[mbPartIdx] = new Vector[subMbTypes[mbPartIdx].getNumParts()];

                for (int subMbPartIdx = 0; subMbPartIdx < subMbTypes[mbPartIdx].getNumParts(); subMbPartIdx++) {
                    int x = readSE(reader, "mvd_l1[" + mbPartIdx + "][" + subMbPartIdx + "]");
                    int y = readSE(reader, "mvd_l1[" + mbPartIdx + "][" + subMbPartIdx + "]");
                    mvdL1[mbPartIdx][subMbPartIdx] = new Vector(x, y, refIdxL1[mbPartIdx]);
                }
            }
        }

        return new Inter8x8Prediction(subMbTypes, refIdxL0, refIdxL1, mvdL0, mvdL1);
    }
}