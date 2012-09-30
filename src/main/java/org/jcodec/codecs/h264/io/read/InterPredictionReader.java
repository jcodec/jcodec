package org.jcodec.codecs.h264.io.read;

import static org.jcodec.codecs.h264.io.read.CAVLCReader.readSE;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readTE;

import java.io.IOException;

import org.jcodec.codecs.h264.io.model.InterPrediction;
import org.jcodec.codecs.h264.io.model.MBPartPredMode;
import org.jcodec.codecs.h264.io.model.Vector;
import org.jcodec.common.io.InBits;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A reader for intrer prediction
 * 
 * 
 * @author Jay Codec
 * 
 */
public class InterPredictionReader {

    private int numRefIdxL0Active;
    private int numRefIdxL1Active;

    public InterPredictionReader(int numRefIdxL0Active, int numRefIdxL1Active) {
        this.numRefIdxL0Active = numRefIdxL0Active;
        this.numRefIdxL1Active = numRefIdxL1Active;
    }

    public InterPrediction read(InBits reader, int numPartitions, boolean mb_field_decoding_flag,
            MBPartPredMode[] predMode) throws IOException {
        int[] refIdxL0 = new int[numPartitions];
        for (int mbPartIdx = 0; mbPartIdx < numPartitions; mbPartIdx++) {
            if ((numRefIdxL0Active > 1 || mb_field_decoding_flag) && predMode[mbPartIdx] != MBPartPredMode.Pred_L1) {

                refIdxL0[mbPartIdx] = readTE(reader, numRefIdxL0Active - 1);
            }
        }
        int[] refIdxL1 = new int[numPartitions];
        for (int mbPartIdx = 0; mbPartIdx < numPartitions; mbPartIdx++) {
            if ((numRefIdxL1Active > 1 || mb_field_decoding_flag) && predMode[mbPartIdx] != MBPartPredMode.Pred_L0) {
                refIdxL1[mbPartIdx] = readTE(reader, numRefIdxL1Active - 1);
            }
        }

        Vector[] mvdL0 = new Vector[numPartitions];

        for (int mbPartIdx = 0; mbPartIdx < numPartitions; mbPartIdx++) {
            if (predMode[mbPartIdx] != MBPartPredMode.Pred_L1) {
                int x = readSE(reader, "mvd_l0");
                int y = readSE(reader, "mvd_l0");
                mvdL0[mbPartIdx] = new Vector(x, y, refIdxL0[mbPartIdx]);
            }
        }

        Vector[] mvdL1 = new Vector[numPartitions];

        for (int mbPartIdx = 0; mbPartIdx < numPartitions; mbPartIdx++) {
            if (predMode[mbPartIdx] != MBPartPredMode.Pred_L0) {
                int x = readSE(reader, "mvd_l1");
                int y = readSE(reader, "mvd_l1");
                mvdL1[mbPartIdx] = new Vector(x, y, refIdxL1[mbPartIdx]);
            }
        }

        return new InterPrediction(refIdxL0, refIdxL1, mvdL0, mvdL1);
    }

}
