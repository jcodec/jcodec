package org.jcodec.codecs.h264.decode;

import static org.jcodec.codecs.h264.io.model.SeqParameterSet.getPicHeightInMbs;

import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Contains an input for deblocking filter
 * 
 * @author The JCodec project
 * 
 */
public class DeblockerInput {
    public int[][] nCoeff;
    public H264Utils.MvList2D mvs;
    public MBType[] mbTypes;
    public int[][] mbQps;
    public boolean[] tr8x8Used;
    public Frame[][][] refsUsed;
    public SliceHeader[] shs;

    public DeblockerInput(SeqParameterSet activeSps) {
        int picWidthInMbs = activeSps.picWidthInMbsMinus1 + 1;
        int picHeightInMbs = getPicHeightInMbs(activeSps);

        nCoeff = new int[picHeightInMbs << 2][picWidthInMbs << 2];
        mvs = new H264Utils.MvList2D(picWidthInMbs << 2, picHeightInMbs << 2);
        mbTypes = new MBType[picHeightInMbs * picWidthInMbs];
        tr8x8Used = new boolean[picHeightInMbs * picWidthInMbs];
        mbQps = new int[3][picHeightInMbs * picWidthInMbs];
        shs = new SliceHeader[picHeightInMbs * picWidthInMbs];
        refsUsed = new Frame[picHeightInMbs * picWidthInMbs][][];
    }
}
