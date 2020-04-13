package org.jcodec.containers.mxf.model

import org.jcodec.common.io.NIOUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class DeltaEntries(private val posTabIdx: ByteArray, private val slice: ByteArray, private val elementData: IntArray) {

    companion object {
        @JvmStatic
        fun read(bb: ByteBuffer): DeltaEntries {
            bb.order(ByteOrder.BIG_ENDIAN)
            val n = bb.int
            val len = bb.int
            val posTabIdx = ByteArray(n)
            val slice = ByteArray(n)
            val elementDelta = IntArray(n)
            for (i in 0 until n) {
                posTabIdx[i] = bb.get()
                slice[i] = bb.get()
                elementDelta[i] = bb.int
                NIOUtils.skip(bb, len - 6)
            }
            return DeltaEntries(posTabIdx, slice, elementDelta)
        }
    }

}