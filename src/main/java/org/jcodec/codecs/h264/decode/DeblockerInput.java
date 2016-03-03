package org.jcodec.codecs.h264.decode;

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
        picHeightInMbs = SeqParameterSet.getPicHeightInMbs(activeSps);

        int picHeight = picHeightInMbs << 2;
        int picWidth = picWidthInMbs << 2;
        int mbCount = picHeightInMbs * picWidthInMbs;

        nCoeff = new int[picHeight][picWidth];
        mvs = new int[2][picHeight][picWidth][3];
        mbTypes = new MBType[mbCount];
        tr8x8Used = new boolean[mbCount];
        mbQps = new int[3][mbCount];
        shs = new SliceHeader[mbCount];
        refsUsed = new Frame[mbCount][][];
    }
}
