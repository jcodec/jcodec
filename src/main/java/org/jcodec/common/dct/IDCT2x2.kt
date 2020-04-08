package org.jcodec.common.dct

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
object IDCT2x2 {
    @JvmStatic
    fun idct(blk: IntArray, off: Int) {
        val x0 = blk[off]
        val x1 = blk[off + 1]
        val x2 = blk[off + 2]
        val x3 = blk[off + 3]
        val t0 = x0 + x2
        val t2 = x0 - x2
        val t1 = x1 + x3
        val t3 = x1 - x3
        blk[off] = t0 + t1 shr 3
        blk[off + 1] = t0 - t1 shr 3
        blk[off + 2] = t2 + t3 shr 3
        blk[off + 3] = t2 - t3 shr 3
    }
}