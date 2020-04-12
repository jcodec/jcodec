package org.jcodec.codecs.wav

import org.jcodec.common.*
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.model.Packet
import org.jcodec.common.model.Packet.FrameType
import java.io.IOException
import java.util.*

/**
 * A demuxer for a wav file.
 *
 * @author Stan Vitvitskiy
 */
class WavDemuxer(private val ch: SeekableByteChannel) : Demuxer, DemuxerTrack {
    private val header: WavHeader = WavHeader.readChannel(ch)
    private val dataSize: Long
    private val frameSize: Short
    private var frameNo = 0
    private var pts: Long = 0

    @Throws(IOException::class)
    override fun close() {
        ch.close()
    }

    @Throws(IOException::class)
    override fun nextFrame(): Packet? {
        val data = NIOUtils.fetchFromChannel(ch, frameSize * FRAMES_PER_PKT)
        if (!data.hasRemaining()) return null
        val oldPts = pts
        val duration = data.remaining() / frameSize
        pts += duration.toLong()
        return Packet.createPacket(data, oldPts, header.getFormat().frameRate, data.remaining() / frameSize.toLong(),
                frameNo++.toLong(), FrameType.KEY, null)
    }

    override fun getMeta(): DemuxerTrackMeta {
        val format = header.getFormat()
        val audioCodecMeta = AudioCodecMeta.fromAudioFormat(format)
        val totalFrames = dataSize / format.frameSize
        return DemuxerTrackMeta(TrackType.AUDIO, Codec.PCM, totalFrames.toDouble() / format.frameRate, null,
                totalFrames.toInt(), null, null, audioCodecMeta)
    }

    override fun getTracks(): List<DemuxerTrack> = listOf(this)

    override fun getVideoTracks(): List<DemuxerTrack> = emptyList()

    override fun getAudioTracks(): List<DemuxerTrack> = listOf(this)

    companion object {
        // How many audio frames will be read per packet. One frame is all samples
        // for all channels, i.e. for the 5.1 16bit format one frame will be 12
        // bytes.
        private const val FRAMES_PER_PKT = 1024
    }

    init {
        dataSize = ch.size() - header.dataOffset
        frameSize = header.getFormat().frameSize
        ch.setPosition(header.dataOffset.toLong())
    }
}