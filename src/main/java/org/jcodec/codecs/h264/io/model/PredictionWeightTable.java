package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class PredictionWeightTable {
    // luma_log2_weight_denom
    public int lumaLog2WeightDenom;
    // chroma_log2_weight_denom
    public int chromaLog2WeightDenom;

    // luma_weight
    public int[][] lumaWeight;
    // chroma_weight
    public int[][][] chromaWeight;

    // luma_offset
    public int[][] lumaOffset;
    // chroma_offset
    public int[][][] chromaOffset;

    public PredictionWeightTable() {
        this.lumaWeight = new int[2][];
        this.chromaWeight = new int[2][][];

        this.lumaOffset = new int[2][];
        this.chromaOffset = new int[2][][];
    }
}
