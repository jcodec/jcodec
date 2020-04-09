package org.jcodec.api

import org.jcodec.api.transcode.*
import org.jcodec.api.transcode.PixelStore.LoanerPicture
import org.jcodec.common.Codec
import org.jcodec.common.Format
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Packet
import org.jcodec.common.model.Packet.FrameType
import org.jcodec.common.model.Picture
import org.jcodec.common.model.Rational
import org.jcodec.scale.ColorUtil
import org.jcodec.scale.Transform
import java.io.File
import java.io.IOException

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Encodes a sequence of images as a video.
 *
 * @author The JCodec project
 */
class SequenceEncoder(out: SeekableByteChannel?, private val fps: Rational, outputFormat: Format?, outputVideoCodec: Codec?,
                      outputAudioCodec: Codec?) {
    private var transform: Transform? = null
    private var frameNo = 0
    private var timestamp = 0
    private val sink: Sink
    private val pixelStore: PixelStore

    /**
     * Encodes a frame into a movie.
     *
     * @param pic
     * @throws IOException
     */
    @Throws(IOException::class)
    fun encodeNativeFrame(pic: Picture) {
        require(pic.color == ColorSpace.RGB) { "The input images is expected in RGB color." }
        val sinkColor = sink.inputColor
        val toEncode: LoanerPicture
        if (sinkColor != null) {
            toEncode = pixelStore.getPicture(pic.width, pic.height, sinkColor)!!
            transform!!.transform(pic, toEncode.picture)
        } else {
            toEncode = LoanerPicture(pic, 0)
        }
        val pkt = Packet.createPacket(null, timestamp.toLong(), fps.getNum(), fps.getDen().toLong(), frameNo.toLong(), FrameType.KEY, null)
        sink.outputVideoFrame(VideoFrameWithPacket(pkt, toEncode))
        if (sinkColor != null) pixelStore.putBack(toEncode)
        timestamp += fps.getDen()
        frameNo++
    }

    @Throws(IOException::class)
    fun finish() {
        sink.finish()
    }

    companion object {
        @Throws(IOException::class)
        fun createSequenceEncoder(out: File?, fps: Int): SequenceEncoder {
            return SequenceEncoder(NIOUtils.writableChannel(out), Rational.R(fps, 1), Format.MOV, Codec.H264, null)
        }

        @Throws(IOException::class)
        fun create25Fps(out: File?): SequenceEncoder {
            return SequenceEncoder(NIOUtils.writableChannel(out), Rational.R(25, 1), Format.MOV, Codec.H264, null)
        }

        @Throws(IOException::class)
        fun create30Fps(out: File?): SequenceEncoder {
            return SequenceEncoder(NIOUtils.writableChannel(out), Rational.R(30, 1), Format.MOV, Codec.H264, null)
        }

        @Throws(IOException::class)
        fun create2997Fps(out: File?): SequenceEncoder {
            return SequenceEncoder(NIOUtils.writableChannel(out), Rational.R(30000, 1001), Format.MOV, Codec.H264, null)
        }

        @Throws(IOException::class)
        fun create24Fps(out: File?): SequenceEncoder {
            return SequenceEncoder(NIOUtils.writableChannel(out), Rational.R(24, 1), Format.MOV, Codec.H264, null)
        }

        @JvmStatic
        @Throws(IOException::class)
        fun createWithFps(out: SeekableByteChannel?, fps: Rational): SequenceEncoder {
            return SequenceEncoder(out, fps, Format.MOV, Codec.H264, null)
        }
    }

    init {
        sink = SinkImpl.createWithStream(out, outputFormat, outputVideoCodec, outputAudioCodec)
        sink.init(false, false)
        if (sink.inputColor != null) transform = ColorUtil.getTransform(ColorSpace.RGB, sink.inputColor)
        pixelStore = PixelStoreImpl()
    }
}