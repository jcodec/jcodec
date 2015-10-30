package org.jcodec.codecs.h264.io.model;

import static org.jcodec.codecs.h264.decode.CAVLCReader.readSE;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeSE;

import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.BitWriter;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Scaling list entity
 * 
 * capable to serialize / deserialize with CAVLC bitstream
 * 
 * 
 * @author The JCodec project
 * 
 */
public class ScalingList {

    public int[] scalingList;
    public boolean useDefaultScalingMatrixFlag;

    public void write(BitWriter out)  {
        if (useDefaultScalingMatrixFlag) {
            writeSE(out, 0, "SPS: ");
            return;
        }

        int lastScale = 8;
        int nextScale = 8;
        for (int j = 0; j < scalingList.length; j++) {
            if (nextScale != 0) {
                int deltaScale = scalingList[j] - lastScale - 256;
                writeSE(out, deltaScale, "SPS: ");
            }
            lastScale = scalingList[j];
        }
    }

    public static ScalingList read(BitReader in, int sizeOfScalingList)  {

        ScalingList sl = new ScalingList();
        sl.scalingList = new int[sizeOfScalingList];
        int lastScale = 8;
        int nextScale = 8;
        for (int j = 0; j < sizeOfScalingList; j++) {
            if (nextScale != 0) {
                int deltaScale = readSE(in, "deltaScale");
                nextScale = (lastScale + deltaScale + 256) % 256;
                sl.useDefaultScalingMatrixFlag = (j == 0 && nextScale == 0);
            }
            sl.scalingList[j] = nextScale == 0 ? lastScale : nextScale;
            lastScale = sl.scalingList[j];
        }
        return sl;
    }
}