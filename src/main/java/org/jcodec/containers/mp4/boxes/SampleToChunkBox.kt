package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Sample to chunk mapping box
 *
 * @author The JCodec project
 */
class SampleToChunkBox(atom: Header) : FullBox(atom) {
    class SampleToChunkEntry(var first: Long, var count: Int, var entry: Int)

    private var sampleToChunk: Array<SampleToChunkEntry?> = emptyArray()
    override fun parse(input: ByteBuffer) {
        super.parse(input)
        val size = input.int
        sampleToChunk = arrayOfNulls(size)
        for (i in 0 until size) {
            sampleToChunk[i] = SampleToChunkEntry(input.int.toLong(), input.int,
                    input.int)
        }
    }

    fun getSampleToChunk(): Array<SampleToChunkEntry?> {
        return sampleToChunk
    }

    public override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        out.putInt(sampleToChunk.size)
        for (i in sampleToChunk.indices) {
            val stc = sampleToChunk[i]
            out.putInt(stc!!.first.toInt())
            out.putInt(stc.count)
            out.putInt(stc.entry)
        }
    }

    override fun estimateSize(): Int {
        return 16 + sampleToChunk.size * 12
    }

    fun setSampleToChunk(sampleToChunk: Array<SampleToChunkEntry?>) {
        this.sampleToChunk = sampleToChunk
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "stsc"
        }

        @JvmStatic
        fun createSampleToChunkBox(sampleToChunk: Array<SampleToChunkEntry?>): SampleToChunkBox {
            val box = SampleToChunkBox(Header(fourcc()))
            box.sampleToChunk = sampleToChunk
            return box
        }
    }
}