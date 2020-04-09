package org.jcodec.codecs.prores

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Codebook for ProRes codes
 *
 * @author The JCodec project
 */
class Codebook(@JvmField var riceOrder: Int, @JvmField var expOrder: Int, @JvmField var switchBits: Int) {
    @JvmField
    var golombOffset: Int
    @JvmField
    var golombBits: Int
    var riceMask: Int

    init {
        golombOffset = (1 shl expOrder) - (switchBits + 1 shl riceOrder)
        golombBits = expOrder - switchBits - 1
        riceMask = (1 shl riceOrder) - 1
    }
}