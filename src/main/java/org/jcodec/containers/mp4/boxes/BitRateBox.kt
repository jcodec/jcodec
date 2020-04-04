package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class BitRateBox(atom: Header) : FullBox(atom) {
    var bufferSizeDB = 0
        private set
    var maxBitrate = 0
        private set
    var avgBitrate = 0
        private set

    override fun estimateSize(): Int {
        return 24
    }

    override fun parse(input: ByteBuffer) {
        super.parse(input)
        bufferSizeDB = input.int
        maxBitrate = input.int
        avgBitrate = input.int
    }

    override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        out.putInt(bufferSizeDB)
        out.putInt(maxBitrate)
        out.putInt(avgBitrate)
    }

    companion object {
        fun createUriBox(bufferSizeDB: Int, maxBitrate: Int, avgBitrate: Int): BitRateBox {
            val box = BitRateBox(Header(fourcc()))
            box.bufferSizeDB = bufferSizeDB
            box.maxBitrate = maxBitrate
            box.avgBitrate = avgBitrate
            return box
        }

        @JvmStatic
        fun fourcc(): String {
            return "btrt"
        }
    }
}