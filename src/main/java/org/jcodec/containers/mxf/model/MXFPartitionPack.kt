package org.jcodec.containers.mxf.model

import org.jcodec.common.io.NIOUtils
import org.jcodec.containers.mxf.model.UL.Companion.read
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class MXFPartitionPack(ul: UL) : MXFMetadata(ul) {
    var kagSize = 0
        private set
    var thisPartition: Long = 0
        private set
    var prevPartition: Long = 0
        private set
    var footerPartition: Long = 0
        private set
    var headerByteCount: Long = 0
        private set
    var indexByteCount: Long = 0
        private set
    var indexSid = 0
        private set
    var bodySid = 0
        private set
    var op: UL? = null
        private set
    var nbEssenceContainers = 0
        private set

    override fun readBuf(bb: ByteBuffer) {
        bb.order(ByteOrder.BIG_ENDIAN)
        NIOUtils.skip(bb, 4)
        kagSize = bb.int
        thisPartition = bb.long
        prevPartition = bb.long
        footerPartition = bb.long
        headerByteCount = bb.long
        indexByteCount = bb.long
        indexSid = bb.int
        NIOUtils.skip(bb, 8)
        bodySid = bb.int
        op = read(bb)
        nbEssenceContainers = bb.int
    }

}