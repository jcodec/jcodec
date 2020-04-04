package org.jcodec.containers.mp4.boxes

import org.jcodec.common.JCodecUtil2
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.model.Size
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Describes video payload sample
 *
 * @author The JCodec project
 */
open class VideoSampleEntry(atom: Header) : SampleEntry(atom) {
    var version: Short = 0
        private set
    var revision: Short = 0
        private set
    var vendor: String? = null
        private set
    var temporalQual = 0
        private set
    var spacialQual = 0
        private set
    var width: Short = 0
    var height: Short = 0
    private var hRes = 0f
    private var vRes = 0f
    private var frameCount: Short = 0
    var compressorName: String? = null
        private set
    private var depth: Short = 0
    var clrTbl: Short = 0
        private set

    override fun parse(input: ByteBuffer) {
        super.parse(input)
        version = input!!.short
        revision = input.short
        vendor = NIOUtils.readString(input, 4)
        temporalQual = input.int
        spacialQual = input.int
        width = input.short
        height = input.short
        hRes = input.int.toFloat() / 65536f
        vRes = input.int.toFloat() / 65536f
        input.int // Reserved
        frameCount = input.short
        compressorName = NIOUtils.readPascalStringL(input, 31)
        depth = input.short
        clrTbl = input.short
        parseExtensions(input)
    }

    public override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        out!!.putShort(version)
        out.putShort(revision)
        out.put(JCodecUtil2.asciiString(vendor), 0, 4)
        out.putInt(temporalQual)
        out.putInt(spacialQual)
        out.putShort(width)
        out.putShort(height)
        out.putInt((hRes * 65536).toInt())
        out.putInt((vRes * 65536).toInt())
        out.putInt(0) // data size
        out.putShort(frameCount)
        NIOUtils.writePascalStringL(out, compressorName, 31)
        out.putShort(depth)
        out.putShort(clrTbl)
        writeExtensions(out)
    }

    fun gethRes(): Float {
        return hRes
    }

    fun getvRes(): Float {
        return vRes
    }

    fun getFrameCount(): Long {
        return frameCount.toLong()
    }

    fun getDepth(): Long {
        return depth.toLong()
    }

    companion object {
        @JvmStatic
        fun videoSampleEntry(fourcc: String?, size: Size, encoderName: String?): VideoSampleEntry {
            return createVideoSampleEntry(Header(fourcc), 0.toShort(), 0.toShort(), "jcod", 0, 768,
                    size.width.toShort(), size.height.toShort(), 72, 72, 1.toShort(),
                    encoderName ?: "jcodec", 24.toShort(), 1.toShort(), (-1).toShort())
        }

        fun createVideoSampleEntry(atom: Header, version: Short, revision: Short, vendor: String?,
                                   temporalQual: Int, spacialQual: Int, width: Short, height: Short, hRes: Long, vRes: Long, frameCount: Short,
                                   compressorName: String?, depth: Short, drefInd: Short, clrTbl: Short): VideoSampleEntry {
            val e = VideoSampleEntry(atom)
            e.drefInd = drefInd
            e.version = version
            e.revision = revision
            e.vendor = vendor
            e.temporalQual = temporalQual
            e.spacialQual = spacialQual
            e.width = width
            e.height = height
            e.hRes = hRes.toFloat()
            e.vRes = vRes.toFloat()
            e.frameCount = frameCount
            e.compressorName = compressorName
            e.depth = depth
            e.clrTbl = clrTbl
            return e
        }
    }
}