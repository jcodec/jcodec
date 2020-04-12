package org.jcodec.codecs.png

import org.jcodec.common.VideoCodecMeta
import org.jcodec.common.VideoDecoder
import org.jcodec.common.and
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.logging.Logger
import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Picture
import org.jcodec.common.model.Size
import org.jcodec.common.tools.MathUtil
import java.nio.ByteBuffer
import java.util.*
import java.util.zip.DataFormatException
import java.util.zip.Inflater

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * PNG image decoder.
 *
 * Supports: RGB, palette, grey, alpha, interlace, transparency.
 *
 * @author Stanislav Vitvitskyy
 */
class PNGDecoder : VideoDecoder() {
    private val ca: ByteArray = ByteArray(4)
    override fun decodeFrame(data: ByteBuffer, buffer: Array<ByteArray>): Picture? {
        if (!ispng(data)) throw RuntimeException("Not a PNG file.")
        var ihdr: IHDR? = null
        var plte: PLTE? = null
        var trns: TRNS? = null
        val list: MutableList<ByteBuffer> = ArrayList()
        while (data.remaining() >= 8) {
            val length = data.int
            val tag = data.int
            if (data.remaining() < length) break
            when (tag) {
                PNGConsts.TAG_IHDR -> {
                    ihdr = IHDR()
                    ihdr.parse(data)
                }
                PNGConsts.TAG_PLTE -> {
                    plte = PLTE()
                    plte.parse(data, length)
                }
                PNGConsts.TAG_tRNS -> {
                    checkNotNull(ihdr) { "tRNS tag before IHDR" }
                    trns = TRNS(ihdr.colorType)
                    trns.parse(data, length)
                }
                PNGConsts.TAG_IDAT -> {
                    list.add(NIOUtils.read(data, length))
                    NIOUtils.skip(data, 4) // CRC
                }
                PNGConsts.TAG_IEND -> NIOUtils.skip(data, 4) // CRC
                else -> data.position(data.position() + length + 4)
            }
        }
        if (ihdr != null) {
            try {
                decodeData(ihdr, plte, trns, list, buffer)
            } catch (e: DataFormatException) {
                return null
            }
            return Picture.createPicture(ihdr.width, ihdr.height, buffer, ihdr.colorSpace())
        }
        throw IllegalStateException("no IHDR tag")
    }

    @Throws(DataFormatException::class)
    private fun decodeData(ihdr: IHDR, plte: PLTE?, trns: TRNS?, list: List<ByteBuffer>, buffer: Array<ByteArray>) {
        val bpp = ihdr.bitsPerPixel + 7 shr 3
        val passes = if (ihdr.interlaceType.toInt() == 0) 1 else 7
        val inflater = Inflater()
        val it = list.iterator()
        for (pass in 0 until passes) {
            var rowSize: Int
            var rowStart: Int
            var rowStep: Int
            var colStart: Int
            var colStep: Int
            if (ihdr.interlaceType.toInt() == 0) {
                rowSize = ihdr.rowSize() + 1
                rowStart = 0
                colStart = rowStart
                rowStep = 1
                colStep = rowStep
            } else {
                val round = (1 shl logPassStep[pass]) - 1
                rowSize = (ihdr.width + round shr logPassStep[pass]) + 1
                rowStart = passRowOff[pass]
                rowStep = 1 shl logPassRowStep[pass]
                colStart = passOff[pass]
                colStep = 1 shl logPassStep[pass]
            }
            val lastRow = ByteArray(rowSize - 1)
            val uncompressed = ByteArray(rowSize)
            var bptr = 3 * (ihdr.width * rowStart + colStart)
            var row = rowStart
            while (row < ihdr.height) {
                var count = inflater.inflate(uncompressed)
                if (count < uncompressed.size && inflater.needsInput()) {
                    if (!it.hasNext()) {
                        Logger.warn(String.format("Data truncation at row %d", row))
                        break
                    }
                    val next = it.next()
                    inflater.setInput(NIOUtils.toArray(next))
                    val toRead = uncompressed.size - count
                    count = inflater.inflate(uncompressed, count, toRead)
                    if (count != toRead) {
                        Logger.warn(String.format("Data truncation at row %d", row))
                        break
                    }
                }
                val filter = uncompressed[0].toInt()
                when (filter) {
                    FILTER_VALUE_NONE -> System.arraycopy(uncompressed, 1, lastRow, 0, rowSize - 1)
                    FILTER_VALUE_SUB -> filterSub(uncompressed, rowSize - 1, lastRow, bpp)
                    FILTER_VALUE_UP -> filterUp(uncompressed, rowSize - 1, lastRow)
                    FILTER_VALUE_AVG -> filterAvg(uncompressed, rowSize - 1, lastRow, bpp)
                    FILTER_VALUE_PAETH -> filterPaeth(uncompressed, rowSize - 1, lastRow, bpp)
                }
                val bptrWas = bptr
                if (ihdr.colorType and IHDR.PNG_COLOR_MASK_PALETTE != 0) {
                    var i = 0
                    while (i < rowSize - 1) {
                        val plt = plte!!.palette[lastRow[i] and 0xff]
                        buffer[0][bptr] = ((plt shr 16 and 0xff) - 128).toByte()
                        buffer[0][bptr + 1] = ((plt shr 8 and 0xff) - 128).toByte()
                        buffer[0][bptr + 2] = ((plt and 0xff) - 128).toByte()
                        i += bpp
                        bptr += 3 * colStep
                    }
                } else if (ihdr.colorType and IHDR.PNG_COLOR_MASK_COLOR != 0) {
                    var i = 0
                    while (i < rowSize - 1) {
                        buffer[0][bptr] = ((lastRow[i] and 0xff) - 128).toByte()
                        buffer[0][bptr + 1] = ((lastRow[i + 1] and 0xff) - 128).toByte()
                        buffer[0][bptr + 2] = ((lastRow[i + 2] and 0xff) - 128).toByte()
                        i += bpp
                        bptr += 3 * colStep
                    }
                } else {
                    var i = 0
                    while (i < rowSize - 1) {
                        buffer[0][bptr + 2] = ((lastRow[i] and 0xff) - 128).toByte()
                        buffer[0][bptr
                                + 1] = buffer[0][bptr + 2]
                        buffer[0][bptr] = buffer[0][bptr
                                + 1]
                        i += bpp
                        bptr += 3 * colStep
                    }
                }
                if (ihdr.colorType and IHDR.PNG_COLOR_MASK_ALPHA != 0) {
                    var i = bpp - 1
                    var j = bptrWas
                    while (i < rowSize - 1) {
                        val alpha: Int = lastRow[i] and 0xff
                        val nalpha = 256 - alpha
                        buffer[0][j] = (alphaR * nalpha + buffer[0][j] * alpha shr 8).toByte()
                        buffer[0][j + 1] = (alphaG * nalpha + buffer[0][j + 1] * alpha shr 8).toByte()
                        buffer[0][j + 2] = (alphaB * nalpha + buffer[0][j + 2] * alpha shr 8).toByte()
                        i += bpp
                        j += 3 * colStep
                    }
                } else if (trns != null) {
                    if (ihdr.colorType.toInt() == PNG_COLOR_TYPE_PALETTE) {
                        var i = 0
                        var j = bptrWas
                        while (i < rowSize - 1) {
                            val alpha: Int = trns.alphaPal[lastRow[i] and 0xff] and 0xff
                            val nalpha = 256 - alpha
                            buffer[0][j] = (alphaR * nalpha + buffer[0][j] * alpha shr 8).toByte()
                            buffer[0][j + 1] = (alphaG * nalpha + buffer[0][j + 1] * alpha shr 8).toByte()
                            buffer[0][j + 2] = (alphaB * nalpha + buffer[0][j + 2] * alpha shr 8).toByte()
                            i++
                            j += 3 * colStep
                        }
                    } else if (ihdr.colorType.toInt() == PNG_COLOR_TYPE_RGB) {
                        val ar: Byte = ((trns.alphaR and 0xff) - 128).toByte()
                        val ag: Byte = ((trns.alphaG and 0xff) - 128).toByte()
                        val ab: Byte = ((trns.alphaB and 0xff) - 128).toByte()
                        if (ab != alphaB.toByte() || ag != alphaG.toByte() || ar != alphaR.toByte()) {
                            var i = 0
                            var j = bptrWas
                            while (i < rowSize - 1) {
                                if (buffer[0][j] == ar && buffer[0][j + 1] == ag && buffer[0][j + 2] == ab) {
                                    buffer[0][j] = alphaR.toByte()
                                    buffer[0][j + 1] = alphaG.toByte()
                                    buffer[0][j + 2] = alphaB.toByte()
                                }
                                i += bpp
                                j += 3 * colStep
                            }
                        }
                    } else if (ihdr.colorType.toInt() == PNG_COLOR_TYPE_GRAY) {
                        var i = 0
                        var j = bptrWas
                        while (i < rowSize - 1) {
                            if (lastRow[i] == trns.alphaGrey) {
                                buffer[0][j] = alphaR.toByte()
                                buffer[0][j + 1] = alphaG.toByte()
                                buffer[0][j + 2] = alphaB.toByte()
                            }
                            i++
                            j += 3 * colStep
                        }
                    }
                }
                bptr = bptrWas + 3 * ihdr.width * rowStep
                row += rowStep
            }
        }
    }

    private fun filterPaeth(uncompressed: ByteArray, rowSize: Int, lastRow: ByteArray, bpp: Int) {
        for (i in 0 until bpp) {
            ca[i] = lastRow[i]
            lastRow[i] = ((uncompressed[i + 1] and 0xff) + (lastRow[i] and 0xff)).toByte()
        }
        for (i in bpp until rowSize) {
            val a: Int = lastRow[i - bpp] and 0xff
            val b: Int = lastRow[i] and 0xff
            val c: Int = ca[i % bpp] and 0xff
            var p = b - c
            var pc = a - c
            val pa = MathUtil.abs(p)
            val pb = MathUtil.abs(pc)
            pc = MathUtil.abs(p + pc)
            p = if (pa <= pb && pa <= pc) a else if (pb <= pc) b else c
            ca[i % bpp] = lastRow[i]
            lastRow[i] = (p + (uncompressed[i + 1] and 0xff)).toByte()
        }
    }

    /**
     * Palette descriptor.
     */
    private class PLTE {
        var palette: IntArray = IntArray(0)
        fun parse(data: ByteBuffer, length: Int) {
            if (length % 3 != 0 || length > 256 * 3) throw RuntimeException("Invalid data")
            val n = length / 3
            palette = IntArray(256)
            var i: Int
            i = 0
            while (i < n) {
                palette[i] = (0xff shl 24 or (data.get() and 0xff shl 16) or (data.get() and 0xff shl 8)
                        or (data.get() and 0xff))
                i++
            }
            while (i < 256) {
                palette[i] = 0xff shl 24
                i++
            }
            data.int // crc
        }
    }

    /**
     * Transparency descriptor for paletted data
     */
    class TRNS internal constructor(colorType: Byte) {
        private val colorType: Int
        var alphaPal: ByteArray = ByteArray(0)
        var alphaGrey: Byte = 0
        var alphaR: Byte = 0
        var alphaG: Byte = 0
        var alphaB: Byte = 0
        fun parse(data: ByteBuffer, length: Int) {
            if (colorType == PNG_COLOR_TYPE_PALETTE) {
                alphaPal = ByteArray(256)
                data[alphaPal, 0, length]
                for (i in length..255) {
                    alphaPal[i] = 0xff.toByte()
                }
            } else if (colorType == PNG_COLOR_TYPE_GRAY) {
                val alphaGreyHi = data.get()
                alphaGrey = data.get()
            } else if (colorType == PNG_COLOR_TYPE_RGB) {
                data.get()
                alphaR = data.get()
                data.get()
                alphaG = data.get()
                data.get()
                alphaG = data.get()
            }
            data.int // crc
        }

        init {
            this.colorType = colorType.toInt()
        }
    }

    override fun getCodecMeta(_data: ByteBuffer): VideoCodecMeta? {
        val data = _data.duplicate()
        if (!ispng(data)) throw RuntimeException("Not a PNG file.")
        while (data.remaining() >= 8) {
            val length = data.int
            val tag = data.int
            if (data.remaining() < length) break
            when (tag) {
                PNGConsts.TAG_IHDR -> {
                    val ihdr = IHDR()
                    ihdr.parse(data)
                    return VideoCodecMeta.createSimpleVideoCodecMeta(Size(ihdr.width, ihdr.height), ColorSpace.RGB)
                }
                else -> data.position(data.position() + length + 4)
            }
        }
        return null
    }

    companion object {
        private const val FILTER_TYPE_LOCO = 64
        private const val FILTER_VALUE_NONE = 0
        private const val FILTER_VALUE_SUB = 1
        private const val FILTER_VALUE_UP = 2
        private const val FILTER_VALUE_AVG = 3
        private const val FILTER_VALUE_PAETH = 4
        private const val PNG_COLOR_TYPE_GRAY = 0
        private const val PNG_COLOR_TYPE_PALETTE = IHDR.PNG_COLOR_MASK_COLOR or IHDR.PNG_COLOR_MASK_PALETTE
        private const val PNG_COLOR_TYPE_RGB = IHDR.PNG_COLOR_MASK_COLOR
        private const val alphaR = 0x7f
        private const val alphaG = 0x7f
        private const val alphaB = 0x7f
        private val logPassStep = intArrayOf(3, 3, 2, 2, 1, 1, 0)
        private val logPassRowStep = intArrayOf(3, 3, 3, 2, 2, 1, 1)
        private val passOff = intArrayOf(0, 4, 0, 2, 0, 1, 0)
        private val passRowOff = intArrayOf(0, 0, 4, 0, 2, 0, 1)
        private fun filterSub(uncompressed: ByteArray, rowSize: Int, lastRow: ByteArray, bpp: Int) {
            when (bpp) {
                1 -> filterSub1(uncompressed, lastRow, rowSize)
                2 -> filterSub2(uncompressed, lastRow, rowSize)
                3 -> filterSub3(uncompressed, lastRow, rowSize)
                else -> filterSub4(uncompressed, lastRow, rowSize)
            }
        }

        private fun filterAvg(uncompressed: ByteArray, rowSize: Int, lastRow: ByteArray, bpp: Int) {
            when (bpp) {
                1 -> filterAvg1(uncompressed, lastRow, rowSize)
                2 -> filterAvg2(uncompressed, lastRow, rowSize)
                3 -> filterAvg3(uncompressed, lastRow, rowSize)
                else -> filterAvg4(uncompressed, lastRow, rowSize)
            }
        }

        private fun filterSub1(uncompressed: ByteArray, lastRow: ByteArray, rowSize: Int) {
            lastRow[0] = uncompressed[1]
            var p = lastRow[0]
            for (i in 1 until rowSize) {
                lastRow[i] = ((p and 0xff) + (uncompressed[i + 1] and 0xff)).toByte()
                p = lastRow[i]
            }
        }

        private fun filterUp(uncompressed: ByteArray, rowSize: Int, lastRow: ByteArray) {
            for (i in 0 until rowSize) {
                lastRow[i] = ((lastRow[i] and 0xff) + (uncompressed[i + 1] and 0xff)).toByte()
            }
        }

        private fun filterAvg1(uncompressed: ByteArray, lastRow: ByteArray, rowSize: Int) {
            lastRow[0] = ((uncompressed[1] and 0xff) + (lastRow[0] and 0xff shr 1)).toByte()
            var p = lastRow[0]
            for (i in 1 until rowSize) {
                lastRow[i] = (((lastRow[i] and 0xff) + (p and 0xff) shr 1) + (uncompressed[i + 1] and 0xff)).toByte()
                p = lastRow[i]
            }
        }

        private fun filterSub2(uncompressed: ByteArray, lastRow: ByteArray, rowSize: Int) {
            lastRow[0] = uncompressed[1]
            var p0 = lastRow[0]
            lastRow[1] = uncompressed[2]
            var p1 = lastRow[1]
            var i = 2
            while (i < rowSize) {
                lastRow[i] = ((p0 and 0xff) + (uncompressed[1 + i] and 0xff)).toByte()
                p0 = lastRow[i]
                lastRow[i + 1] = ((p1 and 0xff) + (uncompressed[2 + i] and 0xff)).toByte()
                p1 = lastRow[i + 1]
                i += 2
            }
        }

        private fun filterAvg2(uncompressed: ByteArray, lastRow: ByteArray, rowSize: Int) {
            lastRow[0] = ((uncompressed[1] and 0xff) + (lastRow[0] and 0xff shr 1)).toByte()
            var p0 = lastRow[0]
            lastRow[1] = ((uncompressed[2] and 0xff) + (lastRow[1] and 0xff shr 1)).toByte()
            var p1 = lastRow[1]
            var i = 2
            while (i < rowSize) {
                lastRow[i] = (((lastRow[i] and 0xff) + (p0 and 0xff) shr 1) + (uncompressed[1 + i] and 0xff)).toByte()
                p0 = lastRow[i]
                lastRow[i
                        + 1] = (((lastRow[i + 1] and 0xff) + (p1 and 0xff) shr 1) + (uncompressed[i + 2] and 0xff)).toByte()
                p1 = lastRow[i
                        + 1]
                i += 2
            }
        }

        private fun filterSub3(uncompressed: ByteArray, lastRow: ByteArray, rowSize: Int) {
            lastRow[0] = uncompressed[1]
            var p0 = lastRow[0]
            lastRow[1] = uncompressed[2]
            var p1 = lastRow[1]
            lastRow[2] = uncompressed[3]
            var p2 = lastRow[2]
            var i = 3
            while (i < rowSize) {
                lastRow[i] = ((p0 and 0xff) + (uncompressed[i + 1] and 0xff)).toByte()
                p0 = lastRow[i]
                lastRow[i + 1] = ((p1 and 0xff) + (uncompressed[i + 2] and 0xff)).toByte()
                p1 = lastRow[i + 1]
                lastRow[i + 2] = ((p2 and 0xff) + (uncompressed[i + 3] and 0xff)).toByte()
                p2 = lastRow[i + 2]
                i += 3
            }
        }

        private fun filterAvg3(uncompressed: ByteArray, lastRow: ByteArray, rowSize: Int) {
            lastRow[0] = ((uncompressed[1] and 0xff) + (lastRow[0] and 0xff shr 1)).toByte()
            var p0 = lastRow[0]
            lastRow[1] = ((uncompressed[2] and 0xff) + (lastRow[1] and 0xff shr 1)).toByte()
            var p1 = lastRow[1]
            lastRow[2] = ((uncompressed[3] and 0xff) + (lastRow[2] and 0xff shr 1)).toByte()
            var p2 = lastRow[2]
            var i = 3
            while (i < rowSize) {
                lastRow[i] = (((lastRow[i] and 0xff) + (p0 and 0xff) shr 1) + (uncompressed[i + 1] and 0xff)).toByte()
                p0 = lastRow[i]
                lastRow[i
                        + 1] = (((lastRow[i + 1] and 0xff) + (p1 and 0xff) shr 1) + (uncompressed[i + 2] and 0xff)).toByte()
                p1 = lastRow[i
                        + 1]
                lastRow[i
                        + 2] = (((lastRow[i + 2] and 0xff) + (p2 and 0xff) shr 1) + (uncompressed[i + 3] and 0xff)).toByte()
                p2 = lastRow[i
                        + 2]
                i += 3
            }
        }

        private fun filterSub4(uncompressed: ByteArray, lastRow: ByteArray, rowSize: Int) {
            lastRow[0] = uncompressed[1]
            var p0 = lastRow[0]
            lastRow[1] = uncompressed[2]
            var p1 = lastRow[1]
            lastRow[2] = uncompressed[3]
            var p2 = lastRow[2]
            lastRow[3] = uncompressed[4]
            var p3 = lastRow[3]
            var i = 4
            while (i < rowSize) {
                lastRow[i] = ((p0 and 0xff) + (uncompressed[i + 1] and 0xff)).toByte()
                p0 = lastRow[i]
                lastRow[i + 1] = ((p1 and 0xff) + (uncompressed[i + 2] and 0xff)).toByte()
                p1 = lastRow[i + 1]
                lastRow[i + 2] = ((p2 and 0xff) + (uncompressed[i + 3] and 0xff)).toByte()
                p2 = lastRow[i + 2]
                lastRow[i + 3] = ((p3 and 0xff) + (uncompressed[i + 4] and 0xff)).toByte()
                p3 = lastRow[i + 3]
                i += 4
            }
        }

        private fun filterAvg4(uncompressed: ByteArray, lastRow: ByteArray, rowSize: Int) {
            lastRow[0] = ((uncompressed[1] and 0xff) + (lastRow[0] and 0xff shr 1)).toByte()
            var p0 = lastRow[0]
            lastRow[1] = ((uncompressed[2] and 0xff) + (lastRow[1] and 0xff shr 1)).toByte()
            var p1 = lastRow[1]
            lastRow[2] = ((uncompressed[3] and 0xff) + (lastRow[2] and 0xff shr 1)).toByte()
            var p2 = lastRow[2]
            lastRow[3] = ((uncompressed[4] and 0xff) + (lastRow[3] and 0xff shr 1)).toByte()
            var p3 = lastRow[3]
            var i = 4
            while (i < rowSize) {
                lastRow[i] = (((lastRow[i] and 0xff) + (p0 and 0xff) shr 1) + (uncompressed[i + 1] and 0xff)).toByte()
                p0 = lastRow[i]
                lastRow[i
                        + 1] = (((lastRow[i + 1] and 0xff) + (p1 and 0xff) shr 1) + (uncompressed[i + 2] and 0xff)).toByte()
                p1 = lastRow[i
                        + 1]
                lastRow[i
                        + 2] = (((lastRow[i + 2] and 0xff) + (p2 and 0xff) shr 1) + (uncompressed[i + 3] and 0xff)).toByte()
                p2 = lastRow[i
                        + 2]
                lastRow[i
                        + 3] = (((lastRow[i + 3] and 0xff) + (p3 and 0xff) shr 1) + (uncompressed[i + 4] and 0xff)).toByte()
                p3 = lastRow[i
                        + 3]
                i += 4
            }
        }

        private fun ispng(data: ByteBuffer): Boolean {
            val sighi = data.int
            val siglo = data.int
            return (sighi == PNGConsts.PNGSIGhi || sighi == PNGConsts.MNGSIGhi) && (siglo == PNGConsts.PNGSIGlo || siglo == PNGConsts.MNGSIGlo)
        }

        fun probe(data: ByteBuffer): Int {
            return if (ispng(data)) 100 else 0
        }
    }
}