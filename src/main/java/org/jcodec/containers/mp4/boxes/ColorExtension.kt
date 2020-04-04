package org.jcodec.containers.mp4.boxes

import org.jcodec.common.JCodecUtil2
import org.jcodec.platform.Platform
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Default box factory
 *
 * @author The JCodec project
 */
class ColorExtension(header: Header) : Box(header) {
    var primariesIndex: Short = 0
        private set
    var transferFunctionIndex: Short = 0
        private set
    var matrixIndex: Short = 0
        private set
    private var type = "nclc"
    private var colorRange: Byte? = null
    fun setColorRange(colorRange: Byte?) {
        this.colorRange = colorRange
    }

    override fun parse(input: ByteBuffer) {
        val dst = ByteArray(4)
        input[dst]
        type = Platform.stringFromBytes(dst)
        primariesIndex = input.short
        transferFunctionIndex = input.short
        matrixIndex = input.short
        if (input.hasRemaining()) {
            colorRange = input.get()
        }
    }

    public override fun doWrite(out: ByteBuffer) {
        out.put(JCodecUtil2.asciiString(type))
        out.putShort(primariesIndex)
        out.putShort(transferFunctionIndex)
        out.putShort(matrixIndex)
        if (colorRange != null) {
            out.put(colorRange!!)
        }
    }

    override fun estimateSize(): Int {
        return 8 + 8
    }

    companion object {
        const val RANGE_UNSPECIFIED: Byte = 0
        const val AVCOL_RANGE_MPEG: Byte = 1 ///< the normal 219*2^(n-8) "MPEG" YUV ranges
        const val AVCOL_RANGE_JPEG: Byte = 2 ///< the normal     2^n-1   "JPEG" YUV ranges
        @JvmStatic
        fun fourcc(): String {
            return "colr"
        }

        fun createColorExtension(primariesIndex: Short, transferFunctionIndex: Short,
                                 matrixIndex: Short): ColorExtension {
            val c = ColorExtension(Header(fourcc()))
            c.primariesIndex = primariesIndex
            c.transferFunctionIndex = transferFunctionIndex
            c.matrixIndex = matrixIndex
            return c
        }

        @JvmStatic
        fun createColr(): ColorExtension {
            return ColorExtension(Header(fourcc()))
        }
    }
}