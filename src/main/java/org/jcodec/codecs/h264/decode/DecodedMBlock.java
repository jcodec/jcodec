package org.jcodec.codecs.h264.decode;

import org.jcodec.codecs.h264.H264Utils.MvList2D;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;

public class DecodedMBlock {
    public Picture8Bit mb;
    public MvList2D mvs;
    public MBType mbTypes;
    public int[] mbQps = new int[3];
    public boolean tr8x8Used;
    public SliceHeader shs;
    public Frame[][] refsUsed;
    public int[][] nCoeff = new int[4][4];
    
    public DecodedMBlock() {
        mb = Picture8Bit.create(16, 16, ColorSpace.YUV420);
        mvs = new MvList2D(4, 4);
    }
}
