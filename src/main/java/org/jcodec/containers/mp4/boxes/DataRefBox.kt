package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 *
 * @author The JCodec project
 */
class DataRefBox(atom: Header) : NodeBox(atom) {
    override fun parse(input: ByteBuffer) {
        input.int
        input.int
        super.parse(input)
    }

    public override fun doWrite(out: ByteBuffer) {
        out.putInt(0)
        out.putInt(_boxes.size)
        super.doWrite(out)
    }

    override fun estimateSize(): Int {
        return 8 + super.estimateSize()
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "dref"
        }

        @JvmStatic
        fun createDataRefBox(): DataRefBox {
            return DataRefBox(Header(fourcc()))
        }
    }
}