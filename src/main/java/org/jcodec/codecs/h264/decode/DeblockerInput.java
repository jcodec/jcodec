package org.jcodec.codecs.h264.decode;

import static org.jcodec.codecs.h264.H264Utils.getPicHeightInMbs;

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
    public int[][][][] mvs;
    public MBType[] mbTypes;
    public int[][] mbQps;
    public boolean[] tr8x8Used;
    public Frame[][][] refsUsed;
    public SliceHeader[] shs;
    public int picWidthInMbs;
    public int picHeightInMbs;

    public DeblockerInput(SeqParameterSet activeSps) {
        picWidthInMbs = activeSps.pic_width_in_mbs_minus1 + 1;
        picHeightInMbs = getPicHeightInMbs(activeSps);

        nCoeff = new int[picHeightInMbs << 2][picWidthInMbs << 2];
        mvs = new int[2][picHeightInMbs << 2][picWidthInMbs << 2][3];
        mbTypes = new MBType[picHeightInMbs * picWidthInMbs];
        tr8x8Used = new boolean[picHeightInMbs * picWidthInMbs];
        mbQps = new int[3][picHeightInMbs * picWidthInMbs];
        shs = new SliceHeader[picHeightInMbs * picWidthInMbs];
        refsUsed = new Frame[picHeightInMbs * picWidthInMbs][][];
    }
}
