package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Sound media header
 *
 * @author The JCodec project
 */
class SoundMediaHeaderBox(atom: Header) : FullBox(atom) {
    var balance: Short = 0
        private set

    override fun parse(input: ByteBuffer) {
        super.parse(input)
        balance = input.short
        input.short
    }

    override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        out.putShort(balance)
        out.putShort(0.toShort())
    }

    override fun estimateSize(): Int {
        return 16
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "smhd"
        }

        @JvmStatic
        fun createSoundMediaHeaderBox(): SoundMediaHeaderBox {
            return SoundMediaHeaderBox(Header(fourcc()))
        }
    }
}