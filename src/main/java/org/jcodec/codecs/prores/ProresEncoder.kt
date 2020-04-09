package org.jcodec.codecs.prores

import org.jcodec.codecs.prores.ProresConsts.QMAT_CHROMA_APCH
import org.jcodec.codecs.prores.ProresConsts.QMAT_CHROMA_APCN
import org.jcodec.codecs.prores.ProresConsts.QMAT_CHROMA_APCO
import org.jcodec.codecs.prores.ProresConsts.QMAT_CHROMA_APCS
import org.jcodec.codecs.prores.ProresConsts.QMAT_LUMA_APCH
import org.jcodec.codecs.prores.ProresConsts.QMAT_LUMA_APCN
import org.jcodec.codecs.prores.ProresConsts.QMAT_LUMA_APCO
import org.jcodec.codecs.prores.ProresConsts.QMAT_LUMA_APCS
import org.jcodec.codecs.prores.ProresConsts.dcCodebooks
import org.jcodec.codecs.prores.ProresConsts.firstDCCodebook
import org.jcodec.codecs.prores.ProresConsts.interlaced_scan
import org.jcodec.codecs.prores.ProresConsts.levCodebooks
import org.jcodec.codecs.prores.ProresConsts.progressive_scan
import org.jcodec.codecs.prores.ProresConsts.runCodebooks
import org.jcodec.common.VideoEncoder
import org.jcodec.common.dct.SimpleIDCT10Bit.fdctProres10
import org.jcodec.common.io.BitWriter
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Picture
import org.jcodec.common.model.Rect
import org.jcodec.common.tools.ImageOP
import org.jcodec.common.tools.MathUtil
import java.nio.ByteBuffer

/**
 *
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Apple ProRes encoder
 *
 * @author The JCodec project
 */
class ProresEncoder(protected var profile: Profile, interlaced: Boolean) : VideoEncoder() {
    class Profile private constructor(val name: String, val qmatLuma: IntArray, val qmatChroma: IntArray, val fourcc: String, // Per 1024 pixels
                                      val bitrate: Int, val firstQp: Int,
                                      val lastQp: Int) {

        companion object {
            val PROXY = Profile("PROXY", QMAT_LUMA_APCO, QMAT_CHROMA_APCO, "apco", 1000, 4, 8)
            val LT = Profile("LT", QMAT_LUMA_APCS, QMAT_CHROMA_APCS, "apcs", 2100, 1, 9)
            val STANDARD = Profile("STANDARD", QMAT_LUMA_APCN, QMAT_CHROMA_APCN, "apcn", 3500,
                    1, 6)

            @JvmField
            val HQ = Profile("HQ", QMAT_LUMA_APCH, QMAT_CHROMA_APCH, "apch", 5400, 1, 6)
            private val _values = arrayOf(PROXY, LT, STANDARD, HQ)
            fun values(): Array<Profile> {
                return _values
            }

            fun valueOf(name: String): Profile? {
                val nameU = name.toUpperCase()
                for (profile2 in _values) {
                    if (name == nameU) return profile2
                }
                return null
            }
        }

    }

    private val scaledLuma: Array<IntArray?>
    private val scaledChroma: Array<IntArray?>
    private val interlaced: Boolean
    private fun scaleQMat(qmatLuma: IntArray, start: Int, count: Int): Array<IntArray?> {
        val result = arrayOfNulls<IntArray>(count)
        for (i in 0 until count) {
            result[i] = IntArray(qmatLuma.size)
            for (j in qmatLuma.indices) result[i]!![j] = qmatLuma[j] * (i + start)
        }
        return result
    }

    //Wrong usage of Javascript keyword:in
    private fun dctOnePlane(blocksPerSlice: Int, src: ByteArray, hibd: ByteArray?, dst: IntArray) {
        for (i in src.indices) {
            dst[i] = src[i] + 128 shl 2
        }
        if (hibd != null) {
            for (i in src.indices) {
                val x = dst[i]
                dst[i] = x + hibd[i]
            }
        }
        for (i in 0 until blocksPerSlice) {
            fdctProres10(dst, i shl 6)
        }
    }

    protected fun encodeSlice(out: ByteBuffer, scaledLuma: Array<IntArray?>, scaledChroma: Array<IntArray?>, scan: IntArray, sliceMbCount: Int,
                              mbX: Int, mbY: Int, result: Picture, prevQp: Int, mbWidth: Int, mbHeight: Int, unsafe: Boolean, vStep: Int,
                              vOffset: Int): Int {
        val striped = splitSlice(result, mbX, mbY, sliceMbCount, unsafe, vStep, vOffset)
        val ac = arrayOf(IntArray(sliceMbCount shl 8), IntArray(sliceMbCount shl 7), IntArray(sliceMbCount shl 7))
        val data = striped.data
        val lowBits = striped.lowBits
        dctOnePlane(sliceMbCount shl 2, data[0], lowBits?.get(0), ac[0])
        dctOnePlane(sliceMbCount shl 1, data[1], lowBits?.get(1), ac[1])
        dctOnePlane(sliceMbCount shl 1, data[2], lowBits?.get(2), ac[2])
        val est = (sliceMbCount shr 2) * profile.bitrate
        val low = est - (est shr 3) // 12% bitrate fluctuation
        val high = est + (est shr 3)
        var qp = prevQp
        out.put((6 shl 3).toByte()) // hdr size
        val fork = out.duplicate()
        NIOUtils.skip(out, 5)
        val rem = out.position()
        val sizes = IntArray(3)
        encodeSliceData(out, scaledLuma[qp - 1], scaledChroma[qp - 1], scan, sliceMbCount, ac, qp, sizes)
        if (bits(sizes) > high && qp < profile.lastQp) {
            do {
                ++qp
                out.position(rem)
                encodeSliceData(out, scaledLuma[qp - 1], scaledChroma[qp - 1], scan, sliceMbCount, ac, qp, sizes)
            } while (bits(sizes) > high && qp < profile.lastQp)
        } else if (bits(sizes) < low && qp > profile.firstQp) {
            do {
                --qp
                out.position(rem)
                encodeSliceData(out, scaledLuma[qp - 1], scaledChroma[qp - 1], scan, sliceMbCount, ac, qp, sizes)
            } while (bits(sizes) < low && qp > profile.firstQp)
        }
        fork.put(qp.toByte())
        fork.putShort(sizes[0].toShort())
        fork.putShort(sizes[1].toShort())
        return qp
    }

    protected fun encodePicture(out: ByteBuffer, scaledLuma: Array<IntArray?>, scaledChroma: Array<IntArray?>, scan: IntArray,
                                picture: Picture, vStep: Int, vOffset: Int) {
        val mbWidth = picture.width + 15 shr 4
        val shift = 4 + vStep
        val round = (1 shl shift) - 1
        val mbHeight = picture.height + round shr shift
        var qp = profile.firstQp
        val nSlices = calcNSlices(mbWidth, mbHeight)
        writePictureHeader(LOG_DEFAULT_SLICE_MB_WIDTH, nSlices, out)
        val fork = out.duplicate()
        NIOUtils.skip(out, nSlices shl 1)
        var i = 0
        val total = IntArray(nSlices)
        for (mbY in 0 until mbHeight) {
            var mbX = 0
            var sliceMbCount = DEFAULT_SLICE_MB_WIDTH
            while (mbX < mbWidth) {
                while (mbWidth - mbX < sliceMbCount) sliceMbCount = sliceMbCount shr 1
                val sliceStart = out.position()
                val unsafeBottom = picture.height % 16 != 0 && mbY == mbHeight - 1
                val unsafeRight = picture.width % 16 != 0 && mbX + sliceMbCount == mbWidth
                qp = encodeSlice(out, scaledLuma, scaledChroma, scan, sliceMbCount, mbX, mbY, picture, qp, mbWidth,
                        mbHeight, unsafeBottom || unsafeRight, vStep, vOffset)
                fork.putShort((out.position() - sliceStart).toShort())
                total[i++] = (out.position() - sliceStart).toShort().toInt()
                mbX += sliceMbCount
            }
        }
    }

    private fun calcNSlices(mbWidth: Int, mbHeight: Int): Int {
        var nSlices = mbWidth shr LOG_DEFAULT_SLICE_MB_WIDTH
        for (i in 0 until LOG_DEFAULT_SLICE_MB_WIDTH) {
            nSlices += mbWidth shr i and 0x1
        }
        return nSlices * mbHeight
    }

    private fun splitSlice(result: Picture, mbX: Int, mbY: Int, sliceMbCount: Int, unsafe: Boolean, vStep: Int,
                           vOffset: Int): Picture {
        val out = Picture.createCroppedHiBD(sliceMbCount shl 4, 16, result.lowBitsNum, ColorSpace.YUV422, null)
        if (unsafe) {
            val mbHeightPix = 16 shl vStep
            val filled = Picture.create(sliceMbCount shl 4, mbHeightPix, ColorSpace.YUV422)
            ImageOP.subImageWithFillPic8(result, filled,
                    Rect(mbX shl 4, mbY shl 4 + vStep, sliceMbCount shl 4, mbHeightPix))
            split(filled, out, 0, 0, sliceMbCount, vStep, vOffset)
        } else {
            split(result, out, mbX, mbY, sliceMbCount, vStep, vOffset)
        }
        return out
    }

    //Wrong usage of Javascript keyword:in
    private fun split(src: Picture, dst: Picture, mbX: Int, mbY: Int, sliceMbCount: Int, vStep: Int, vOffset: Int) {
        val inData = src.data
        val inhbdData = src.lowBits
        val outData = dst.data
        val outhbdData = dst.lowBits
        doSplit(inData[0], outData[0], src.getPlaneWidth(0), mbX, mbY, sliceMbCount, 0, vStep, vOffset)
        doSplit(inData[1], outData[1], src.getPlaneWidth(1), mbX, mbY, sliceMbCount, 1, vStep, vOffset)
        doSplit(inData[2], outData[2], src.getPlaneWidth(2), mbX, mbY, sliceMbCount, 1, vStep, vOffset)
        if (src.lowBits != null) {
            doSplit(inhbdData[0], outhbdData[0], src.getPlaneWidth(0), mbX, mbY, sliceMbCount, 0, vStep, vOffset)
            doSplit(inhbdData[1], outhbdData[1], src.getPlaneWidth(1), mbX, mbY, sliceMbCount, 1, vStep, vOffset)
            doSplit(inhbdData[2], outhbdData[2], src.getPlaneWidth(2), mbX, mbY, sliceMbCount, 1, vStep, vOffset)
        }
    }

    private fun doSplit(_in: ByteArray, out: ByteArray, stride: Int, mbX: Int, mbY: Int, sliceMbCount: Int, chroma: Int, vStep: Int,
                        vOffset: Int) {
        var stride = stride
        var outOff = 0
        var off = (mbY shl 4) * (stride shl vStep) + (mbX shl 4 - chroma) + stride * vOffset
        stride = stride shl vStep
        for (i in 0 until sliceMbCount) {
            splitBlock(_in, stride, off, out, outOff)
            splitBlock(_in, stride, off + (stride shl 3), out, outOff + (128 shr chroma))
            if (chroma == 0) {
                splitBlock(_in, stride, off + 8, out, outOff + 64)
                splitBlock(_in, stride, off + (stride shl 3) + 8, out, outOff + 192)
            }
            outOff += 256 shr chroma
            off += 16 shr chroma
        }
    }

    private fun splitBlock(y: ByteArray, stride: Int, off: Int, out: ByteArray, outOff: Int) {
        var off = off
        var outOff = outOff
        for (i in 0..7) {
            for (j in 0..7) out[outOff++] = y[off++]
            off += stride - 8
        }
    }

    override fun encodeFrame(pic: Picture, buffer: ByteBuffer): EncodedFrame? {
        val out = buffer.duplicate()
        val fork = out.duplicate()
        val scan: IntArray = if (interlaced) interlaced_scan else progressive_scan
        writeFrameHeader(out, ProresConsts.FrameHeader(0, pic.croppedWidth, pic.croppedHeight,
                if (interlaced) 1 else 0, true, scan, profile.qmatLuma, profile.qmatChroma, 2))
        encodePicture(out, scaledLuma, scaledChroma, scan, pic, if (interlaced) 1 else 0, 0)
        if (interlaced) encodePicture(out, scaledLuma, scaledChroma, scan, pic, if (interlaced) 1 else 0, 1)
        out.flip()
        fork.putInt(out.remaining())
        return EncodedFrame(out, true)
    }

    override fun getSupportedColorSpaces(): Array<ColorSpace> {
        return arrayOf(ColorSpace.YUV422)
    }

    override fun estimateBufferSize(frame: Picture): Int {
        return 3 * frame.width * frame.height / 2
    }

    override fun finish() {
        // TODO Auto-generated method stub
    }

    companion object {
        private const val LOG_DEFAULT_SLICE_MB_WIDTH = 3
        private const val DEFAULT_SLICE_MB_WIDTH = 1 shl LOG_DEFAULT_SLICE_MB_WIDTH

        @JvmStatic
        fun createProresEncoder(profile: String?, interlaced: Boolean): ProresEncoder {
            return ProresEncoder((if (profile == null) Profile.HQ else Profile.valueOf(profile))!!, interlaced)
        }

        @JvmStatic
        fun writeCodeword(writer: BitWriter, codebook: Codebook, `val`: Int) {
            var `val` = `val`
            val firstExp = codebook.switchBits + 1 shl codebook.riceOrder
            if (`val` >= firstExp) {
                `val` -= firstExp
                `val` += 1 shl codebook.expOrder // Offset to zero
                val exp = MathUtil.log2(`val`)
                val zeros = exp - codebook.expOrder + codebook.switchBits + 1
                for (i in 0 until zeros) writer.write1Bit(0)
                writer.write1Bit(1)
                writer.writeNBit(`val`, exp)
            } else if (codebook.riceOrder > 0) {
                for (i in 0 until (`val` shr codebook.riceOrder)) writer.write1Bit(0)
                writer.write1Bit(1)
                writer.writeNBit(`val` and (1 shl codebook.riceOrder) - 1, codebook.riceOrder)
            } else {
                for (i in 0 until `val`) writer.write1Bit(0)
                writer.write1Bit(1)
            }
        }

        private fun qScale(qMat: IntArray?, ind: Int, `val`: Int): Int {
            return `val` / qMat!![ind]
        }

        private fun toGolumb(`val`: Int): Int {
            return `val` shl 1 xor (`val` shr 31)
        }

        private fun toGolumbSign(`val`: Int, sign: Int): Int {
            return if (`val` == 0) 0 else (`val` shl 1) + sign
        }

        private fun diffSign(`val`: Int, sign: Int): Int {
            return `val` shr 31 xor sign
        }

        @JvmStatic
        fun getLevel(`val`: Int): Int {
            val sign = `val` shr 31
            return (`val` xor sign) - sign
        }

        @JvmStatic
        fun writeDCCoeffs(bits: BitWriter, qMat: IntArray?, _in: IntArray, blocksPerSlice: Int) {
            var prevDc = qScale(qMat, 0, _in[0] - 16384)
            writeCodeword(bits, firstDCCodebook, toGolumb(prevDc))
            var code = 5
            var sign = 0
            var idx = 64
            var i = 1
            while (i < blocksPerSlice) {
                val newDc = qScale(qMat, 0, _in[idx] - 16384)
                val delta = newDc - prevDc
                val newCode = toGolumbSign(getLevel(delta), diffSign(delta, sign))
                writeCodeword(bits, dcCodebooks.get(Math.min(code, 6)), newCode)
                code = newCode
                sign = delta shr 31
                prevDc = newDc
                i++
                idx += 64
            }
        }

        @JvmStatic
        fun writeACCoeffs(bits: BitWriter, qMat: IntArray?, _in: IntArray, blocksPerSlice: Int, scan: IntArray,
                          maxCoeff: Int) {
            var prevRun = 4
            var prevLevel = 2
            var run = 0
            for (i in 1 until maxCoeff) {
                val indp = scan[i]
                for (j in 0 until blocksPerSlice) {
                    val `val` = qScale(qMat, indp, _in[(j shl 6) + indp])
                    if (`val` == 0) run++ else {
                        writeCodeword(bits, runCodebooks[Math.min(prevRun, 15)], run)
                        prevRun = run
                        run = 0
                        val level = getLevel(`val`)
                        writeCodeword(bits, levCodebooks[Math.min(prevLevel, 9)], level - 1)
                        prevLevel = level
                        bits.write1Bit(MathUtil.sign(`val`))
                    }
                }
            }
        }

        fun encodeOnePlane(bits: BitWriter, blocksPerSlice: Int, qMat: IntArray?, scan: IntArray, _in: IntArray) {
            writeDCCoeffs(bits, qMat, _in, blocksPerSlice)
            writeACCoeffs(bits, qMat, _in, blocksPerSlice, scan, 64)
        }

        fun bits(sizes: IntArray): Int {
            return sizes[0] + sizes[1] + sizes[2] shl 3
        }

        protected fun encodeSliceData(out: ByteBuffer, qmatLuma: IntArray?, qmatChroma: IntArray?, scan: IntArray,
                                      sliceMbCount: Int, ac: Array<IntArray>, qp: Int, sizes: IntArray) {
            sizes[0] = onePlane(out, sliceMbCount shl 2, qmatLuma, scan, ac[0])
            sizes[1] = onePlane(out, sliceMbCount shl 1, qmatChroma, scan, ac[1])
            sizes[2] = onePlane(out, sliceMbCount shl 1, qmatChroma, scan, ac[2])
        }

        fun onePlane(out: ByteBuffer, blocksPerSlice: Int, qmatLuma: IntArray?, scan: IntArray, data: IntArray): Int {
            val rem = out.position()
            val bits = BitWriter(out)
            encodeOnePlane(bits, blocksPerSlice, qmatLuma, scan, data)
            bits.flush()
            return out.position() - rem
        }

        @JvmStatic
        fun writePictureHeader(logDefaultSliceMbWidth: Int, nSlices: Int, out: ByteBuffer) {
            val headerLen = 8
            out.put((headerLen shl 3).toByte())
            out.putInt(0)
            out.putShort(nSlices.toShort())
            out.put((logDefaultSliceMbWidth shl 4).toByte())
        }

        @JvmStatic
        fun writeFrameHeader(outp: ByteBuffer, header: ProresConsts.FrameHeader) {
            val headerSize: Short = 148
            outp.putInt(headerSize + 8 + header.payloadSize)
            outp.put(byteArrayOf('i'.toByte(), 'c'.toByte(), 'p'.toByte(), 'f'.toByte()))
            outp.putShort(headerSize) // header size
            outp.putShort(0.toShort())
            outp.put(byteArrayOf('a'.toByte(), 'p'.toByte(), 'l'.toByte(), '0'.toByte()))
            outp.putShort(header.width.toShort())
            outp.putShort(header.height.toShort())
            outp.put((if (header.frameType == 0) 0x83 else 0x87).toByte()) // {10}(422){00}[{00}(frame),{01}(field)}{11}
            outp.put(byteArrayOf(0, 2, 2, 6, 32, 0))
            outp.put(3.toByte()) // flags2
            writeQMat(outp, header.qMatLuma)
            writeQMat(outp, header.qMatChroma)
        }

        fun writeQMat(out: ByteBuffer, qmat: IntArray) {
            for (i in 0..63) out.put(qmat[i].toByte())
        }
    }

    init {
        scaledLuma = scaleQMat(profile.qmatLuma, 1, 16)
        scaledChroma = scaleQMat(profile.qmatChroma, 1, 16)
        this.interlaced = interlaced
    }
}