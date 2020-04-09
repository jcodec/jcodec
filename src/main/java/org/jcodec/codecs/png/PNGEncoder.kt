package org.jcodec.codecs.png

import org.jcodec.common.VideoEncoder
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Picture
import java.nio.ByteBuffer
import java.util.zip.CRC32
import java.util.zip.Deflater

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Simplistic PNG encoder, doesn't support anything.
 *
 * @author Stanislav Vitvitskyy
 */
class PNGEncoder : VideoEncoder() {
    override fun encodeFrame(pic: Picture, out: ByteBuffer): EncodedFrame? {
        val _out = out.duplicate()
        _out.putInt(PNGConsts.PNGSIGhi)
        _out.putInt(PNGConsts.PNGSIGlo)
        val ihdr = IHDR()
        ihdr.width = pic.croppedWidth
        ihdr.height = pic.croppedHeight
        ihdr.bitDepth = 8
        ihdr.colorType = IHDR.PNG_COLOR_MASK_COLOR.toByte()
        _out.putInt(13)
        var crcFrom = _out.duplicate()
        _out.putInt(PNGConsts.TAG_IHDR)
        ihdr.write(_out)
        _out.putInt(crc32(crcFrom, _out))
        val deflater = Deflater()
        val rowData = ByteArray(pic.croppedWidth * 3 + 1)
        val pix = pic.getPlaneData(0)
        val buffer = ByteArray(1 shl 15)
        var ptr = 0
        var len = buffer.size

        // We do one extra iteration here to flush the deflator
        val lineStep = (pic.width - pic.croppedWidth) * 3
        var row = 0
        var bptr = 0
        while (row < pic.croppedHeight + 1) {
            var count: Int
            while (deflater.deflate(buffer, ptr, len).also { count = it } > 0) {
                ptr += count
                len -= count
                if (len == 0) {
                    _out.putInt(ptr)
                    crcFrom = _out.duplicate()
                    _out.putInt(PNGConsts.TAG_IDAT)
                    _out.put(buffer, 0, ptr)
                    _out.putInt(crc32(crcFrom, _out))
                    ptr = 0
                    len = buffer.size
                }
            }
            if (row >= pic.croppedHeight) break
            rowData[0] = 0 // no filter
            var i = 1
            while (i <= pic.croppedWidth * 3) {
                rowData[i] = (pix[bptr] + 128).toByte()
                rowData[i + 1] = (pix[bptr + 1] + 128).toByte()
                rowData[i + 2] = (pix[bptr + 2] + 128).toByte()
                i += 3
                bptr += 3
            }
            bptr += lineStep
            deflater.setInput(rowData)
            if (row >= pic.croppedHeight - 1) deflater.finish()
            row++
        }
        if (ptr > 0) {
            _out.putInt(ptr)
            crcFrom = _out.duplicate()
            _out.putInt(PNGConsts.TAG_IDAT)
            _out.put(buffer, 0, ptr)
            _out.putInt(crc32(crcFrom, _out))
        }
        _out.putInt(0)
        _out.putInt(PNGConsts.TAG_IEND)
        _out.putInt(-0x51bd9f7e)
        _out.flip()
        return EncodedFrame(_out, true)
    }

    override fun getSupportedColorSpaces(): Array<ColorSpace> {
        return arrayOf(ColorSpace.RGB)
    }

    override fun estimateBufferSize(frame: Picture): Int {
        return frame.croppedWidth * frame.croppedHeight * 4
    }

    override fun finish() {
        // TODO Auto-generated method stub
    }

    companion object {
        private fun crc32(from: ByteBuffer, to: ByteBuffer): Int {
            from.limit(to.position())
            val crc32 = CRC32()
            crc32.update(NIOUtils.toArray(from))
            return crc32.value.toInt()
        }
    }
}