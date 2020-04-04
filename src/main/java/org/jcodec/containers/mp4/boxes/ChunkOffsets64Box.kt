package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 *
 * Box type
 *
 * @author The JCodec project
 */
class ChunkOffsets64Box(atom: Header) : FullBox(atom) {
    private var chunkOffsets: LongArray = LongArray(0)
    override fun parse(input: ByteBuffer) {
        super.parse(input)
        val length = input.int
        chunkOffsets = LongArray(length)
        for (i in 0 until length) {
            chunkOffsets[i] = input.long
        }
    }

    override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        out.putInt(chunkOffsets.size)
        for (i in chunkOffsets.indices) {
            val offset = chunkOffsets[i]
            out.putLong(offset)
        }
    }

    override fun estimateSize(): Int {
        return 12 + 4 + chunkOffsets.size * 8
    }

    fun getChunkOffsets(): LongArray {
        return chunkOffsets
    }

    fun setChunkOffsets(chunkOffsets: LongArray) {
        this.chunkOffsets = chunkOffsets
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "co64"
        }

        @JvmStatic
        fun createChunkOffsets64Box(offsets: LongArray): ChunkOffsets64Box {
            val co64 = ChunkOffsets64Box(Header.createHeader(fourcc(), 0))
            co64.chunkOffsets = offsets
            return co64
        }
    }
}