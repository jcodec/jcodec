package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class PredictionWeightTable {
    public int luma_log2_weight_denom;
    public int chroma_log2_weight_denom;

    public int[][] luma_weight;
    public int[][][] chroma_weight;

    public int[][] luma_offset;
    public int[][][] chroma_offset;

    public PredictionWeightTable() {
        this.luma_weight = new int[2][];
        this.chroma_weight = new int[2][][];

        this.luma_offset = new int[2][];
        this.chroma_offset = new int[2][][];
    }
}
