package org.jcodec.codecs.h264.decode;

import org.jcodec.codecs.h264.H264Const.PartPred;
import org.jcodec.codecs.h264.io.model.MBType;
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

    MBType[] topMBType;
    MBType leftMBType;
    ColorSpace chromaFormat;
    boolean transform8x8;

    int[][][] mvTop;
    int[][][] mvLeft;
    int[][] mvTopLeft;

    int leftCBPLuma;
    int[] topCBPLuma;

    int leftCBPChroma;
    int[] topCBPChroma;
    int[] numRef;

    boolean tf8x8Left;
    boolean[] tf8x8Top;

    int[] i4x4PredTop;
    int[] i4x4PredLeft;

    PartPred[] predModeLeft;
    PartPred[] predModeTop;
    
    public DecoderState(SliceHeader sh) {
        int mbWidth = sh.sps.pic_width_in_mbs_minus1 + 1;
        chromaQpOffset = new int[] { sh.pps.chroma_qp_index_offset,
                sh.pps.extended != null ? sh.pps.extended.second_chroma_qp_index_offset : sh.pps.chroma_qp_index_offset };

        chromaFormat = sh.sps.chroma_format_idc;
        transform8x8 = sh.pps.extended == null ? false : sh.pps.extended.transform_8x8_mode_flag;

        topMBType = new MBType[mbWidth];

        topCBPLuma = new int[mbWidth];
        topCBPChroma = new int[mbWidth];

        mvTop = new int[2][(mbWidth << 2) + 1][3];
        mvLeft = new int[2][4][3];
        mvTopLeft = new int[2][3];

        leftRow = new byte[3][16];
        topLeft = new byte[3][4];
        topLine = new byte[3][mbWidth << 4];

        predModeLeft = new PartPred[2];
        predModeTop = new PartPred[mbWidth << 1];

        tf8x8Top = new boolean[mbWidth];

        i4x4PredLeft = new int[4];
        i4x4PredTop = new int[mbWidth << 2];
        qp = sh.pps.pic_init_qp_minus26 + 26 + sh.slice_qp_delta;
        
        if (sh.num_ref_idx_active_override_flag)
            numRef = new int[] { sh.num_ref_idx_active_minus1[0] + 1, sh.num_ref_idx_active_minus1[1] + 1 };
        else
            numRef = new int[] { sh.pps.num_ref_idx_active_minus1[0] + 1, sh.pps.num_ref_idx_active_minus1[1] + 1 };
    }
}
