package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * A box storing a list of synch samples
 *
 * @author The JCodec project
 */
open class SyncSamplesBox(header: Header) : FullBox(header) {
    var syncSamples: IntArray = IntArray(0)
        protected set

    override fun parse(input: ByteBuffer) {
        super.parse(input)
        val len = input.int
        syncSamples = IntArray(len)
        for (i in 0 until len) {
            syncSamples[i] = input.int
        }
    }

    override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        out.putInt(syncSamples.size)
        for (i in syncSamples.indices) out.putInt(syncSamples[i])
    }

    override fun estimateSize(): Int {
        return 16 + syncSamples.size * 4
    }

    companion object {
        const val STSS = "stss"
        @JvmStatic
        fun createSyncSamplesBox(array: IntArray): SyncSamplesBox {
            val stss = SyncSamplesBox(Header(STSS))
            stss.syncSamples = array
            return stss
        }
    }
}