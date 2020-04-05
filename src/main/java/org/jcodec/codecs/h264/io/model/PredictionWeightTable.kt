package org.jcodec.codecs.h264.io.model

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class PredictionWeightTable {
    // luma_log2_weight_denom
    @JvmField
    var lumaLog2WeightDenom = 0

    // chroma_log2_weight_denom
    @JvmField
    var chromaLog2WeightDenom = 0

    // luma_weight
    @JvmField
    var lumaWeight: Array<IntArray?>

    // chroma_weight
    @JvmField
    var chromaWeight: Array<Array<IntArray>?>

    // luma_offset
    @JvmField
    var lumaOffset: Array<IntArray?>

    // chroma_offset
    @JvmField
    var chromaOffset: Array<Array<IntArray>?>

    init {
        lumaWeight = arrayOfNulls(2)
        chromaWeight = arrayOfNulls(2)
        lumaOffset = arrayOfNulls(2)
        chromaOffset = arrayOfNulls(2)
    }
}