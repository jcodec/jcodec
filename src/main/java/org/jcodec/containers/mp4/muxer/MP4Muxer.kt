package org.jcodec.containers.mp4.muxer

import org.jcodec.common.*
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.containers.mp4.Brand
import org.jcodec.containers.mp4.MP4TrackType
import org.jcodec.containers.mp4.MP4Util
import org.jcodec.containers.mp4.boxes.FileTypeBox
import org.jcodec.containers.mp4.boxes.Header.Companion.createHeader
import org.jcodec.containers.mp4.boxes.MovieBox
import org.jcodec.containers.mp4.boxes.MovieBox.Companion.createMovieBox
import org.jcodec.containers.mp4.boxes.MovieHeaderBox
import org.jcodec.containers.mp4.boxes.MovieHeaderBox.Companion.createMovieHeaderBox
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Creates MP4 file out of a set of samples
 *
 * @author The JCodec project
 */
open class MP4Muxer(output: SeekableByteChannel, ftyp: FileTypeBox) : Muxer {
    private val tracks: MutableList<AbstractMP4MuxerTrack>

    @JvmField
    protected var mdatOffset: Long
    var nextTrackId = 1
        private set

    @JvmField
    protected var out: SeekableByteChannel
    fun addTimecodeTrack(): TimecodeMP4MuxerTrack {
        return addTrack(TimecodeMP4MuxerTrack(nextTrackId++))
    }

    fun addTrackWithId(type: MP4TrackType, codec: Codec?, trackId: Int): CodecMP4MuxerTrack {
        Preconditions.checkArgument(!hasTrackId(trackId), "track with id %s already exists", trackId)
        val track = CodecMP4MuxerTrack(trackId, type, codec!!)
        tracks.add(track)
        nextTrackId = Math.max(nextTrackId, trackId + 1)
        return track
    }

    private fun doAddTrack(type: MP4TrackType, codec: Codec): CodecMP4MuxerTrack {
        return addTrack(CodecMP4MuxerTrack(nextTrackId++, type, codec))
    }

    fun <T : AbstractMP4MuxerTrack?> addTrack(track: T): T {
        Preconditions.checkNotNull(track, "track can not be null")
        val trackId = track!!.trackId
        Preconditions.checkArgument(trackId <= nextTrackId)
        Preconditions.checkArgument(!hasTrackId(trackId), "track with id %s already exists", trackId)
        tracks.add(track.setOut(out))
        nextTrackId = Math.max(trackId + 1, nextTrackId)
        return track
    }

    fun hasTrackId(trackId: Int): Boolean {
        for (t in tracks) {
            if (t.trackId == trackId) {
                return true
            }
        }
        return false
    }

    fun getTracks(): List<AbstractMP4MuxerTrack> {
        return Collections.unmodifiableList(tracks)
    }

    @Throws(IOException::class)
    override fun finish() {
        Preconditions.checkState(tracks.size != 0, "Can not save header with 0 tracks.")
        val movie = finalizeHeader()
        storeHeader(movie)
    }

    @Throws(IOException::class)
    open fun storeHeader(movie: MovieBox?) {
        val mdatSize = out.position() - mdatOffset + 8
        MP4Util.writeMovie(out, movie)
        out.setPosition(mdatOffset)
        NIOUtils.writeLong(out, mdatSize)
    }

    @Throws(IOException::class)
    fun finalizeHeader(): MovieBox {
        val movie = createMovieBox()
        val mvhd = movieHeader()
        movie.addFirst(mvhd)
        for (track in tracks) {
            val trak = track.finish(mvhd)
            if (trak != null) movie.add(trak)
        }
        return movie
    }

    val videoTrack: AbstractMP4MuxerTrack?
        get() {
            for (frameMuxer in tracks) {
                if (frameMuxer.isVideo) {
                    return frameMuxer
                }
            }
            return null
        }

    val timecodeTrack: AbstractMP4MuxerTrack?
        get() {
            for (frameMuxer in tracks) {
                if (frameMuxer.isTimecode) {
                    return frameMuxer
                }
            }
            return null
        }

    val audioTracks: List<AbstractMP4MuxerTrack>
        get() {
            val result = ArrayList<AbstractMP4MuxerTrack>()
            for (frameMuxer in tracks) {
                if (frameMuxer.isAudio) {
                    result.add(frameMuxer)
                }
            }
            return result
        }

    private fun movieHeader(): MovieHeaderBox {
        var timescale = tracks[0].timescale
        var duration = tracks[0].trackTotalDuration
        val videoTrack = videoTrack
        if (videoTrack != null) {
            timescale = videoTrack.timescale
            duration = videoTrack.trackTotalDuration
        }
        return createMovieHeaderBox(timescale, duration, 1.0f, 1.0f, Date().time,
                Date().time, intArrayOf(0x10000, 0, 0, 0, 0x10000, 0, 0, 0, 0x40000000), nextTrackId)
    }

    fun addPCMAudioTrack(format: AudioFormat?): PCMMP4MuxerTrack {
        return addTrack(PCMMP4MuxerTrack(nextTrackId++, format!!))
    }

    fun addCompressedAudioTrack(codec: Codec, format: AudioFormat?): CodecMP4MuxerTrack {
        val track = doAddTrack(MP4TrackType.SOUND, codec)
        track.addAudioSampleEntry(format!!)
        return track
    }

    override fun addVideoTrack(codec: Codec, meta: VideoCodecMeta): MuxerTrack {
        val track = doAddTrack(MP4TrackType.VIDEO, codec)
        Preconditions.checkArgument(meta != null || codec == Codec.H264,
                "VideoCodecMeta is required upfront for all codecs but H.264")
        track.addVideoSampleEntry(meta)
        return track
    }

    override fun addAudioTrack(codec: Codec, meta: AudioCodecMeta): MuxerTrack {
        val format = meta.format
        return if (codec == Codec.PCM) {
            addPCMAudioTrack(format)
        } else {
            addCompressedAudioTrack(codec, format)
        }
    }

    fun addMetaTrack(contentEncoding: String?, contentType: String?): MP4MuxerTrack {
        val track = addTrack(MP4MuxerTrack(nextTrackId++, MP4TrackType.META))
        track.addMetaSampleEntry(contentEncoding, contentType)
        return track
    }

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun createMP4MuxerToChannel(output: SeekableByteChannel): MP4Muxer {
            return MP4Muxer(output, Brand.MP4.fileTypeBox)
        }

        @JvmStatic
        @Throws(IOException::class)
        fun createMP4Muxer(output: SeekableByteChannel, brand: Brand): MP4Muxer {
            return MP4Muxer(output, brand.fileTypeBox)
        }
    }

    init {
        tracks = ArrayList()
        out = output
        val buf = ByteBuffer.allocate(1024)
        ftyp.write(buf)
        createHeader("wide", 8).write(buf)
        createHeader("mdat", 1).write(buf)
        mdatOffset = buf.position().toLong()
        buf.putLong(0)
        buf.flip()
        output.write(buf)
    }
}