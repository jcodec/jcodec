package org.jcodec.containers.mxf.model

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class MXFPartition(val pack: MXFPartitionPack, val essenceFilePos: Long, val isClosed: Boolean, val isComplete: Boolean, val essenceLength: Long) {

    companion object {
        @JvmStatic
        fun read(ul: UL, bb: ByteBuffer?, packSize: Long, nextPartition: Long): MXFPartition {
            val closed = ul[14] and 1 == 0
            val complete = ul[14] > 2
            val pp = MXFPartitionPack(ul)
            pp.readBuf(bb!!)
            val essenceFilePos = (roundToKag(pp.thisPartition + packSize, pp.kagSize)
                    + roundToKag(pp.headerByteCount, pp.kagSize)
                    + roundToKag(pp.indexByteCount, pp.kagSize))
            return MXFPartition(pp, essenceFilePos, closed, complete, nextPartition - essenceFilePos)
        }

        fun roundToKag(position: Long, kag_size: Int): Long {
            val ret = position / kag_size * kag_size
            return if (ret == position) ret else ret + kag_size
        }
    }

}