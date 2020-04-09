package org.jcodec.codecs.png

import org.jcodec.common.and
import org.jcodec.common.model.ColorSpace
import java.nio.ByteBuffer

internal class IHDR {
    @JvmField
    var width = 0
    @JvmField
    var height = 0
    @JvmField
    var bitDepth: Byte = 0
    @JvmField
    var colorType: Byte = 0
    private var compressionType: Byte = 0
    private var filterType: Byte = 0
    @JvmField
    var interlaceType: Byte = 0
    fun write(data: ByteBuffer) {
        data.putInt(width)
        data.putInt(height)
        data.put(bitDepth)
        data.put(colorType)
        data.put(compressionType)
        data.put(filterType)
        data.put(interlaceType)
    }

    fun parse(data: ByteBuffer) {
        width = data.int
        height = data.int
        bitDepth = data.get()
        colorType = data.get()
        compressionType = data.get()
        filterType = data.get()
        interlaceType = data.get()
        data.int
    }

    fun rowSize(): Int {
        return width * bitsPerPixel + 7 shr 3
    }

    private val nBChannels: Int
        private get() {
            var channels: Int
            channels = 1
            if (colorType and (PNG_COLOR_MASK_COLOR or PNG_COLOR_MASK_PALETTE) == PNG_COLOR_MASK_COLOR) channels = 3
            if (colorType and PNG_COLOR_MASK_ALPHA != 0) channels++
            return channels
        }

    val bitsPerPixel: Int
        get() = bitDepth * nBChannels

    fun colorSpace(): ColorSpace {
        return ColorSpace.RGB
    }

    companion object {
        const val PNG_COLOR_MASK_ALPHA = 4
        const val PNG_COLOR_MASK_COLOR = 2
        const val PNG_COLOR_MASK_PALETTE = 1
    }
}