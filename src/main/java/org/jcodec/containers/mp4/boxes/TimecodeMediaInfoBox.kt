package org.jcodec.containers.mp4.boxes

import org.jcodec.common.io.NIOUtils
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class TimecodeMediaInfoBox(atom: Header) : FullBox(atom) {
    private var font: Short = 0
    private var face: Short = 0
    private var size: Short = 0
    private var color: ShortArray
    private var bgcolor: ShortArray
    private var name: String? = null
    override fun parse(input: ByteBuffer) {
        super.parse(input)
        font = input.short
        face = input.short
        size = input.short
        input.short
        color[0] = input.short
        color[1] = input.short
        color[2] = input.short
        bgcolor[0] = input.short
        bgcolor[1] = input.short
        bgcolor[2] = input.short
        name = NIOUtils.readPascalString(input)
    }

    override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        out.putShort(font)
        out.putShort(face)
        out.putShort(size)
        out.putShort(0.toShort())
        out.putShort(color[0])
        out.putShort(color[1])
        out.putShort(color[2])
        out.putShort(bgcolor[0])
        out.putShort(bgcolor[1])
        out.putShort(bgcolor[2])
        NIOUtils.writePascalString(out, name)
    }

    override fun estimateSize(): Int {
        return 32 + 1 + NIOUtils.asciiString(name).size
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "tcmi"
        }

        @JvmStatic
        fun createTimecodeMediaInfoBox(font: Short, face: Short, size: Short, color: ShortArray,
                                       bgcolor: ShortArray, name: String?): TimecodeMediaInfoBox {
            val box = TimecodeMediaInfoBox(Header(fourcc()))
            box.font = font
            box.face = face
            box.size = size
            box.color = color
            box.bgcolor = bgcolor
            box.name = name
            return box
        }
    }

    init {
        color = ShortArray(3)
        bgcolor = ShortArray(3)
    }
}