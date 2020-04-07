package org.jcodec.containers.mp4.demuxer

import org.jcodec.codecs.aac.AACUtils
import org.jcodec.codecs.aac.ADTSParser
import org.jcodec.codecs.h264.H264Utils
import org.jcodec.codecs.h264.mp4.AvcCBox
import org.jcodec.common.Codec
import org.jcodec.common.DemuxerTrackMeta
import org.jcodec.common.SeekableDemuxerTrack
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.model.Packet
import org.jcodec.containers.mp4.MP4Packet
import org.jcodec.containers.mp4.boxes.VideoSampleEntry
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Regular MP4 track containing frames
 *
 * @author The JCodec project
 */
class CodecMP4DemuxerTrack(val other: SeekableDemuxerTrack) : SeekableDemuxerTrack {
    private val codecPrivate: ByteBuffer?
    private var avcC: AvcCBox? = null
    private val codec: Codec?

    @Throws(IOException::class)
    override fun nextFrame(): Packet? {
        val nextFrame = other.nextFrame() ?: return null
        val newData = convertPacket(nextFrame.getData())
        return MP4Packet.createMP4PacketWithData(nextFrame as MP4Packet, newData)
    }

    fun convertPacket(data: ByteBuffer): ByteBuffer {
        if (codecPrivate != null) {
            if (codec == Codec.H264) {
                val annexbCoded = H264Utils.decodeMOVPacket(data, avcC!!)
                return if (H264Utils.isByteBufferIDRSlice(annexbCoded)) {
                    NIOUtils.combineBuffers(Arrays.asList(codecPrivate, annexbCoded))
                } else {
                    annexbCoded
                }
            } else if (codec == Codec.AAC) {
                val adts = AACUtils.streamInfoToADTS(codecPrivate, true, 1, data.remaining())
                val adtsRaw = ByteBuffer.allocate(7)
                ADTSParser.write(adts, adtsRaw)
                return NIOUtils.combineBuffers(Arrays.asList(adtsRaw, data))
            }
        }
        return data
    }

    override fun getMeta(): DemuxerTrackMeta {
        return other.meta
    }

    @Throws(IOException::class)
    override fun gotoFrame(frameNo: Long): Boolean {
        return other.gotoFrame(frameNo)
    }

    @Throws(IOException::class)
    override fun gotoSyncFrame(frameNo: Long): Boolean {
        return other.gotoSyncFrame(frameNo)
    }

    override fun getCurFrame(): Long {
        return other.curFrame
    }

    @Throws(IOException::class)
    override fun seek(second: Double) {
        other.seek(second)
    }

    init {
        val meta = other.meta
        codec = meta.codec
        if (codec == Codec.H264) {
            avcC = H264Utils.parseAVCC((meta as MP4DemuxerTrackMeta).sampleEntries!![0] as VideoSampleEntry?)
        }
        codecPrivate = meta.codecPrivate
    }
}