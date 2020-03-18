package org.jcodec.codecs.h264.decode;

import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.common.model.ColorSpace;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Current state of the decoder, this data is accessed from many methods
 * 
 * @author The JCodec project
 * 
 */
public class DecoderState {
    public int[] chromaQpOffset;
    public int qp;
    public byte[][] leftRow;
    public byte[][] topLine;
    public byte[][] topLeft;

    ColorSpace chromaFormat;

    H264Utils.MvList mvTop;
    H264Utils.MvList mvLeft;
    H264Utils.MvList mvTopLeft;

    public DecoderState(SliceHeader sh) {
        int mbWidth = sh.sps.picWidthInMbsMinus1 + 1;
        chromaQpOffset = new int[] { sh.pps.chromaQpIndexOffset,
                sh.pps.extended != null ? sh.pps.extended.secondChromaQpIndexOffset : sh.pps.chromaQpIndexOffset };

        chromaFormat = sh.sps.chromaFormatIdc;

        mvTop = new H264Utils.MvList((mbWidth << 2) + 1);
        mvLeft = new H264Utils.MvList(4);
        mvTopLeft = new H264Utils.MvList(1);

        leftRow = new byte[3][16];
        topLeft = new byte[3][4];
        topLine = new byte[3][mbWidth << 4];

        qp = sh.pps.picInitQpMinus26 + 26 + sh.sliceQpDelta;
    }
}
