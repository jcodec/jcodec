package org.jcodec.codecs.h264.decode;

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
    int[] chromaQpOffset;
    int qp;
    byte[][] leftRow;
    byte[][] topLine;
    byte[][] topLeft;

    ColorSpace chromaFormat;

    int[][][] mvTop;
    int[][][] mvLeft;
    int[][] mvTopLeft;

    public DecoderState(SliceHeader sh) {
        int mbWidth = sh.sps.pic_width_in_mbs_minus1 + 1;
        chromaQpOffset = new int[] { sh.pps.chroma_qp_index_offset,
                sh.pps.extended != null ? sh.pps.extended.secondChromaQpIndexOffset : sh.pps.chroma_qp_index_offset };

        chromaFormat = sh.sps.chroma_format_idc;

        mvTop = new int[2][(mbWidth << 2) + 1][3];
        mvLeft = new int[2][4][3];
        mvTopLeft = new int[2][3];

        leftRow = new byte[3][16];
        topLeft = new byte[3][4];
        topLine = new byte[3][mbWidth << 4];

        qp = sh.pps.pic_init_qp_minus26 + 26 + sh.slice_qp_delta;
    }
}
