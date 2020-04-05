package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 *
 * @author The JCodec project
 */
class SampleDescriptionBox(header: Header) : NodeBox(header) {
    override fun parse(input: ByteBuffer) {
        input.int
        input.int
        super.parse(input)
    }

    public override fun doWrite(out: ByteBuffer) {
        out.putInt(0)
        //even if there is no sample descriptors entry count can not be less than 1
        out.putInt(Math.max(1, _boxes.size))
        super.doWrite(out)
    }

    override fun estimateSize(): Int {
        return 8 + super.estimateSize()
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "stsd"
        }

        @JvmStatic
        fun createSampleDescriptionBox(entries: Array<SampleEntry>): SampleDescriptionBox {
            val box = SampleDescriptionBox(Header(fourcc()))
            for (i in entries.indices) {
                val e = entries[i]
                box._boxes.add(e!!)
            }
            return box
        }
    }
}