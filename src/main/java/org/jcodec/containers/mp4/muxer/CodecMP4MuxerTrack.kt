package org.jcodec.containers.mp4.muxer

import org.jcodec.codecs.aac.ADTSParser
import org.jcodec.codecs.h264.H264Utils
import org.jcodec.codecs.h264.io.model.SeqParameterSet
import org.jcodec.codecs.mpeg4.mp4.EsdsBox.Companion.fromADTS
import org.jcodec.common.AudioFormat
import org.jcodec.common.Codec
import org.jcodec.common.Preconditions
import org.jcodec.common.VideoCodecMeta
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.logging.Logger
import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Packet
import org.jcodec.common.model.Packet.FrameType
import org.jcodec.containers.mp4.MP4TrackType
import org.jcodec.containers.mp4.boxes.AudioSampleEntry.Companion.compressedAudioSampleEntry
import org.jcodec.containers.mp4.boxes.Box
import org.jcodec.containers.mp4.boxes.Box.Companion.createLeafBox
import org.jcodec.containers.mp4.boxes.Box.LeafBox
import org.jcodec.containers.mp4.boxes.Header.Companion.read
import org.jcodec.containers.mp4.boxes.MovieHeaderBox
import org.jcodec.containers.mp4.boxes.PixelAspectExt.Companion.createPixelAspectExt
import org.jcodec.containers.mp4.boxes.SampleEntry
import org.jcodec.containers.mp4.boxes.VideoSampleEntry.Companion.videoSampleEntry
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
open class CodecMP4MuxerTrack(trackId: Int, type: MP4TrackType, private val codec: Codec) : MP4MuxerTrack(trackId, type) {
    companion object {
        private val codec2fourcc: MutableMap<Codec, String> = HashMap()
        private fun selectUnique(bblist: List<ByteBuffer>): List<ByteBuffer> {
            val all: MutableSet<ByteArrayWrapper> = HashSet()
            for (byteBuffer in bblist) {
                all.add(ByteArrayWrapper(byteBuffer))
            }
            val result: MutableList<ByteBuffer> = ArrayList()
            for (bs in all) {
                result.add(bs.get())
            }
            return result
        }

        init {
            codec2fourcc[Codec.MP1] = ".mp1"
            codec2fourcc[Codec.MP2] = ".mp2"
            codec2fourcc[Codec.MP3] = ".mp3"
            codec2fourcc[Codec.H265] = "hev1"
            codec2fourcc[Codec.H264] = "avc1"
            codec2fourcc[Codec.AAC] = "mp4a"
            codec2fourcc[Codec.PRORES] = "apch"
            codec2fourcc[Codec.JPEG] = "mjpg"
            codec2fourcc[Codec.PNG] = "png "
            codec2fourcc[Codec.V210] = "v210"
        }
    }

    // SPS/PPS lists when h.264 is stored, otherwise these lists are not used.
    private val spsList: MutableList<ByteBuffer>
    private val ppsList: MutableList<ByteBuffer>

    // ADTS header used to construct audio sample entry for AAC
    private var adtsHeader: ADTSParser.Header? = null
    private var codecPrivateOpaque: ByteBuffer? = null
    fun setCodecPrivateOpaque(codecPrivate: ByteBuffer?) {
        codecPrivateOpaque = codecPrivate
    }

    @Throws(IOException::class)
    override fun addFrame(pkt: Packet) {
        var pkt = pkt
        if (codec == Codec.H264) {
            var result = pkt.getData()
            if (pkt.frameType == FrameType.UNKNOWN) {
                pkt.setFrameType(if (H264Utils.isByteBufferIDRSlice(result)) FrameType.KEY else FrameType.INTER)
            }
            H264Utils.wipePSinplace(result, spsList, ppsList)
            result = H264Utils.encodeMOVPacket(result)
            pkt = Packet.createPacketWithData(pkt, result)
        } else if (codec == Codec.AAC) {
            val result = pkt.getData()
            adtsHeader = ADTSParser.read(result)
            //            System.out.println(String.format("crc_absent: %d, num_aac_frames: %d, size: %d, remaining: %d, %d, %d, %d",
//                    adtsHeader.getCrcAbsent(), adtsHeader.getNumAACFrames(), adtsHeader.getSize(), result.remaining(),
//                    adtsHeader.getObjectType(), adtsHeader.getSamplingIndex(), adtsHeader.getChanConfig()));
            pkt = Packet.createPacketWithData(pkt, result)
        }
        super.addFrame(pkt)
    }

    @Throws(IOException::class)
    override fun addFrameInternal(pkt: Packet, entryNo: Int) {
        Preconditions.checkState(!finished, "The muxer track has finished muxing")
        if (timescale == NO_TIMESCALE_SET) {
            timescale = if (adtsHeader != null) {
                adtsHeader!!.sampleRate
            } else {
                pkt.getTimescale()
            }
        }
        if (adtsHeader != null && pkt.getDuration() == 0L) {
            pkt.setDuration(1024)
        }
        if (timescale != pkt.getTimescale()) {
            pkt.setPts(pkt.getPts() * timescale / pkt.getTimescale())
            pkt.setDuration(pkt.getDuration() * timescale / pkt.getTimescale())
            pkt.setTimescale(timescale)
        }
        super.addFrameInternal(pkt, entryNo)
    }

    @Throws(IOException::class)
    override fun finish(mvhd: MovieHeaderBox): Box? {
        Preconditions.checkState(!finished, "The muxer track has finished muxing")
        if (entries.orEmpty().isEmpty()) {
            if (codec == Codec.H264 && !spsList.isEmpty()) {
                val sps = SeqParameterSet.read(spsList[0].duplicate())
                val size = H264Utils.getPicSize(sps)
                val meta = VideoCodecMeta.createSimpleVideoCodecMeta(size, ColorSpace.YUV420)
                addVideoSampleEntry(meta)
            } else {
                Logger.warn("CodecMP4MuxerTrack: Creating a track without sample entry")
            }
        }
        setCodecPrivateIfNeeded()
        return super.finish(mvhd)
    }

    fun addVideoSampleEntry(meta: VideoCodecMeta) {
        val se: SampleEntry = videoSampleEntry(codec2fourcc[codec], meta.size, "JCodec")
        if (meta.pixelAspectRatio != null) se.add(createPixelAspectExt(meta.pixelAspectRatio))
        addSampleEntry(se)
    }

    fun setCodecPrivateIfNeeded() {
        if (codecPrivateOpaque != null) {
            val dup = codecPrivateOpaque!!.duplicate()
            val childAtom = read(dup)
            val lb: LeafBox = createLeafBox(childAtom!!, dup)
            entries!![0].add(lb)
        } else {
            if (codec == Codec.H264) {
                val sps = selectUnique(spsList)
                val pps = selectUnique(ppsList)
                if (!sps.isEmpty() && !pps.isEmpty()) {
                    entries!![0].add(H264Utils.createAvcCFromPS(sps, pps, 4))
                } else {
                    Logger.warn("CodecMP4MuxerTrack: Not adding a sample entry for h.264 track, missing any SPS/PPS NAL units")
                }
            } else if (codec == Codec.AAC) {
                if (adtsHeader != null) {
                    entries!![0].add(fromADTS(adtsHeader!!))
                } else {
                    Logger.warn("CodecMP4MuxerTrack: Not adding a sample entry for AAC track, missing any ADTS headers.")
                }
            }
        }
    }

    private class ByteArrayWrapper(bytes: ByteBuffer?) {
        private val bytes: ByteArray
        fun get(): ByteBuffer {
            return ByteBuffer.wrap(bytes)
        }

        override fun equals(obj: Any?): Boolean {
            return if (obj !is ByteArrayWrapper) false else Platform.arrayEqualsByte(bytes, obj.bytes)
        }

        override fun hashCode(): Int {
            return Arrays.hashCode(bytes)
        }

        init {
            this.bytes = NIOUtils.toArray(bytes)
        }
    }

    fun addAudioSampleEntry(format: AudioFormat) {
        val ase = compressedAudioSampleEntry(codec2fourcc[codec], 1, 16,
                format.channels, format.sampleRate, 0, 0, 0)
        addSampleEntry(ase)
    }

    init {
        spsList = ArrayList()
        ppsList = ArrayList()
    }
}