package org.jcodec.containers.y4m

import org.jcodec.common.*
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Packet
import org.jcodec.common.model.Packet.FrameType
import org.jcodec.common.model.Rational
import org.jcodec.common.model.Size
import org.jcodec.platform.Platform
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class Y4MDemuxer(private val input: SeekableByteChannel) : DemuxerTrack, Demuxer {
    private val width: Int
    private val height: Int
    var fps: Rational? = null
    private var bufSize: Int
    private var frameNum = 0
    private val totalFrames: Int
    private val totalDuration: Int

    @Throws(IOException::class)
    override fun nextFrame(): Packet? {
        val buf = NIOUtils.fetchFromChannel(input, 2048)
        val frame = readLine(buf)
        if (frame == null || !frame.startsWith("FRAME")) return null
        input.setPosition(input.position() - buf.remaining())
        val pix = NIOUtils.fetchFromChannel(input, bufSize)
        val packet = Packet(pix, (frameNum * fps!!.getDen()).toLong(), fps!!.getNum(), fps!!.getDen().toLong(), frameNum.toLong(), FrameType.KEY, null, frameNum)
        ++frameNum
        return packet
    }

    override fun getMeta(): DemuxerTrackMeta {
        return DemuxerTrackMeta(TrackType.VIDEO, Codec.RAW, totalDuration.toDouble(), null, totalFrames, null,
                VideoCodecMeta.createSimpleVideoCodecMeta(Size(width, height), ColorSpace.YUV420), null)
    }

    @Throws(IOException::class)
    override fun close() {
        input.close()
    }

    override fun getTracks(): List<DemuxerTrack> {
        val list: MutableList<DemuxerTrack> = ArrayList()
        list.add(this)
        return list
    }

    override fun getVideoTracks(): List<DemuxerTrack> {
        return tracks
    }

    override fun getAudioTracks(): List<DemuxerTrack> {
        return ArrayList()
    }

    companion object {
        private fun find(header: Array<String>, c: Char): String? {
            for (i in header.indices) {
                val string = header[i]
                if (string[0] == c) return string.substring(1)
            }
            return null
        }

        private fun readLine(y4m: ByteBuffer): String {
            val duplicate = y4m.duplicate()
            while (y4m.hasRemaining() && y4m.get() != '\n'.toByte());
            if (y4m.hasRemaining()) duplicate.limit(y4m.position() - 1)
            return Platform.stringFromBytes(NIOUtils.toArray(duplicate))
        }
    }

    init {
        val buf = NIOUtils.fetchFromChannel(input, 2048)
        val header = StringUtils.splitC(readLine(buf), ' ')
        require("YUV4MPEG2" == header[0]) { "Not yuv4mpeg stream" }
        val chroma = find(header, 'C')
        require(chroma == null || chroma.startsWith("420")) { "Only yuv420p is supported" }
        width = find(header, 'W')!!.toInt()
        height = find(header, 'H')!!.toInt()
        val fpsStr = find(header, 'F')
        if (fpsStr != null) {
            val numden = StringUtils.splitC(fpsStr, ':')
            fps = Rational(numden[0].toInt(), numden[1].toInt())
        }
        input.setPosition(buf.position().toLong())
        bufSize = width * height
        bufSize += bufSize / 2
        val fileSize = input.size()
        totalFrames = (fileSize / (bufSize + 7)).toInt()
        totalDuration = totalFrames * fps!!.getDen() / fps!!.getNum()
    }
}