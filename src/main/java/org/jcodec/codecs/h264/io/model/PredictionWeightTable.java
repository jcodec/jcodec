package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class PredictionWeightTable {
    public int luma_log2_weight_denom;
    public int chroma_log2_weight_denom;
    public OffsetWeight[] luma_offset_weight_l0;
    public OffsetWeight[][] chroma_offset_weight_l0;
    public OffsetWeight[] luma_offset_weight_l1;
    public OffsetWeight[][] chroma_offset_weight_l1;

    public static class OffsetWeight {
        public int offset;
        public int weight;
    }

}
