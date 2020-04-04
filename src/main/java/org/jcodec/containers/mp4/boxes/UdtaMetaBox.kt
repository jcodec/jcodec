package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * The meata box inside 'udta'
 *
 * @author The JCodec project
 */
class UdtaMetaBox(atom: Header) : MetaBox(atom) {
    override fun parse(input: ByteBuffer) {
        input.int
        super.parse(input)
    }

    override fun doWrite(out: ByteBuffer) {
        out.putInt(0)
        super.doWrite(out)
    }

    companion object {
        @JvmStatic
        fun createUdtaMetaBox(): UdtaMetaBox {
            return UdtaMetaBox(Header.createHeader(fourcc(), 0))
        }

        fun fourcc(): String {
            return "meta"
        }
    }
}