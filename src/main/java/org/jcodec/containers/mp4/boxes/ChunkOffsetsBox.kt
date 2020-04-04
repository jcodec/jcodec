package org.jcodec.containers.mp4.boxes

import org.jcodec.platform.Platform
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 *
 * A box to hold chunk offsets
 *
 * @author The JCodec project
 */
class ChunkOffsetsBox(atom: Header) : FullBox(atom) {
    private var chunkOffsets: LongArray = LongArray(0)
    override fun parse(input: ByteBuffer) {
        super.parse(input)
        val length = input.int
        chunkOffsets = LongArray(length)
        for (i in 0 until length) {
            chunkOffsets[i] = Platform.unsignedInt(input.int)
        }
    }

    public override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        out.putInt(chunkOffsets.size)
        for (i in chunkOffsets.indices) {
            val offset = chunkOffsets[i]
            out.putInt(offset.toInt())
        }
    }

    override fun estimateSize(): Int {
        return 12 + 4 + chunkOffsets.size * 4
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
            return "stco"
        }

        @JvmStatic
        fun createChunkOffsetsBox(chunkOffsets: LongArray): ChunkOffsetsBox {
            val stco = ChunkOffsetsBox(Header(fourcc()))
            stco.chunkOffsets = chunkOffsets
            return stco
        }
    }
}