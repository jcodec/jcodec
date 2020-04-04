package org.jcodec.containers.mp4.demuxer

import org.jcodec.common.*
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.containers.mp4.MP4TrackType
import org.jcodec.containers.mp4.MP4Util
import org.jcodec.containers.mp4.boxes.*
import org.jcodec.containers.mp4.boxes.Box.Companion.path
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findAll
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findAllPath
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirstPath
import org.jcodec.platform.Platform
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Demuxer frontend for MP4
 *
 * @author The JCodec project
 */
open class MP4Demuxer internal constructor(protected var input: SeekableByteChannel) : Demuxer {
    private val tracks: MutableList<SeekableDemuxerTrack>
    var timecodeTrack: TimecodeMP4DemuxerTrack? = null
        private set
    var movie: MovieBox? = null
    private fun fromTrakBox(trak: TrakBox?): SeekableDemuxerTrack {
        val stsz = findFirstPath(trak, path("mdia.minf.stbl.stsz")) as SampleSizesBox?
        val sampleEntries = findAllPath(trak, arrayOf("mdia", "minf", "stbl", "stsd", null)).map { asSampleEntry(it) }
        val isPCM = (sampleEntries[0] is AudioSampleEntry
                && isPCMCodec(Codec.codecByFourcc(sampleEntries[0]!!.fourcc)))
        return if (stsz!!.defaultSize != 0 && isPCM) PCMMP4DemuxerTrack(movie, trak, input) else newTrack(trak)
    }

    private fun isPCMCodec(codec: Codec?): Boolean {
        return codec != null && codec.isPcm
    }

    protected open fun newTrack(trak: TrakBox?): SeekableDemuxerTrack {
        return CodecMP4DemuxerTrack(MP4DemuxerTrack(movie!!, trak, input))
    }

    @Throws(IOException::class)
    private fun findMovieBox(input: SeekableByteChannel) {
        val mv = MP4Util.parseFullMovieChannel(input)
        if (mv == null || mv.moov == null) throw IOException("Could not find movie meta information box")
        movie = mv.moov
        processHeader(movie)
    }

    @Throws(IOException::class)
    private fun processHeader(moov: NodeBox?) {
        var tt: TrakBox? = null
        val trakBoxs = findAll(moov, "trak").map { it as? TrakBox }
        for (i in trakBoxs.indices) {
            val trak = trakBoxs[i]
            val findFirstPath = findFirstPath(trak, arrayOf("mdia", "minf", "stbl", "stsd", null))
            val se = asSampleEntry(findFirstPath)
            if (se != null && "tmcd" == se.fourcc) {
                tt = trak
            } else {
                tracks.add(fromTrakBox(trak))
            }
        }
        if (tt != null) {
            val video = videoTrack
            if (video != null) timecodeTrack = TimecodeMP4DemuxerTrack(movie, tt, input)
        }
    }


    val videoTrack: DemuxerTrack?
        get() {
            for (demuxerTrack in tracks) {
                val meta = demuxerTrack.meta
                if (meta.type == TrackType.VIDEO) return demuxerTrack
            }
            return null
        }

    override fun getTracks(): List<SeekableDemuxerTrack> {
        return ArrayList(tracks)
    }

    override fun getVideoTracks(): List<DemuxerTrack> {
        val result = ArrayList<DemuxerTrack>()
        for (demuxerTrack in tracks) {
            val meta = demuxerTrack.meta
            if (meta.type == TrackType.VIDEO) result.add(demuxerTrack)
        }
        return result
    }

    override fun getAudioTracks(): List<DemuxerTrack> {
        val result = ArrayList<DemuxerTrack>()
        for (demuxerTrack in tracks) {
            val meta = demuxerTrack.meta
            if (meta.type == TrackType.AUDIO) result.add(demuxerTrack)
        }
        return result
    }

    @Throws(IOException::class)
    override fun close() {
        input.close()
    }

    companion object {
        val PROBE = object : DemuxerProbe {
            override fun probe(b: ByteBuffer): Int {
                return MP4Demuxer.Companion.probe(b)
            }
        }

        fun asSampleEntry(box: Box?) =
                box as? SampleEntry? ?: box?.reinterpret(SampleEntry(box.header)) as? SampleEntry?

        // modifies h264 to conform to annexb and aac to contain adts header
        @JvmStatic
        @Throws(IOException::class)
        fun createMP4Demuxer(input: SeekableByteChannel): MP4Demuxer {
            return MP4Demuxer(input)
        }

        // does not modify packets
        @JvmStatic
        @Throws(IOException::class)
        fun createRawMP4Demuxer(input: SeekableByteChannel): MP4Demuxer {
            return object : MP4Demuxer(input) {
                override fun newTrack(trak: TrakBox?): SeekableDemuxerTrack {
                    return MP4DemuxerTrack(movie!!, trak, this.input)
                }
            }
        }

        fun getTrackType(trak: TrakBox?): MP4TrackType {
            val handler = findFirstPath(trak, path("mdia.hdlr")) as HandlerBox?
            return MP4TrackType.fromHandler(handler!!.componentSubType)
        }

        @UsedViaReflection
        fun probe(b: ByteBuffer): Int {
            val fork = b.duplicate()
            var success = 0
            var total = 0
            while (fork.remaining() >= 8) {
                var len = Platform.unsignedInt(fork.int)
                val fcc = fork.int
                var hdrLen = 8
                if (len == 1L) {
                    len = fork.long
                    hdrLen = 16
                } else if (len < 8) break
                if (fcc == Fourcc.ftyp && len < 64 || fcc == Fourcc.moov && len < 100 * 1024 * 1024 || fcc == Fourcc.free || fcc == Fourcc.mdat || fcc == Fourcc.wide) success++
                total++
                if (len >= Int.MAX_VALUE) break
                NIOUtils.skip(fork, (len - hdrLen).toInt())
            }
            return if (total == 0) 0 else success * 100 / total
        }
    }

    init {
        tracks = LinkedList()
        findMovieBox(input)
    }

}