package org.jcodec.containers.mp4.demuxer

import org.jcodec.common.Demuxer
import org.jcodec.common.DemuxerTrack
import org.jcodec.common.SeekableDemuxerTrack
import org.jcodec.common.TrackType
import org.jcodec.common.model.Packet
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Demuxer frontend for Dash MP4
 *
 * @author The JCodec project
 */
class DashMP4Demuxer private constructor(files: List<List<File>>) : Demuxer {
    private val tracks: MutableList<SeekableDemuxerTrack>

    // f08e80da-bf1d-4e3d-8899-f0f6155f6efa-f08e80da-bf1d-4e3d-8899-f0f6155f6efa.f1080_4800000.mp4.part-Frag0
    class Builder {
        var tracks: MutableList<TrackBuilder> = ArrayList()
        fun addTrack(): TrackBuilder {
            val ret = TrackBuilder(this)
            tracks.add(ret)
            return ret
        }

        @Throws(IOException::class)
        fun build(): DashMP4Demuxer {
            val list: MutableList<List<File>> = LinkedList()
            for (trackBuilder in tracks) {
                list.add(trackBuilder.list)
            }
            return DashMP4Demuxer(list)
        }
    }

    class TrackBuilder(private val builder: Builder) {
        val list: MutableList<File> = LinkedList()
        fun addPattern(pattern: String?): TrackBuilder {
            var i = 0
            while (true) {
                val name = String.format(pattern!!, i)
                val file = File(name)
                if (!file.exists()) break
                list.add(file)
                i++
            }
            return this
        }

        fun addFile(file: File): TrackBuilder {
            list.add(file)
            return this
        }

        fun done(): Builder {
            return builder
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

    override fun getTracks(): List<DemuxerTrack> {
        return ArrayList<DemuxerTrack>(tracks)
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
        for (track in tracks) {
            ((track as CodecMP4DemuxerTrack).other as Closeable).close()
        }
    }

    companion object {
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }

        @Throws(IOException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val demuxer = builder().addTrack().addPattern(args[0]).done().addTrack()
                    .addPattern(args[1]).done().build()
            val videoTrack = demuxer.videoTrack
            val meta = videoTrack!!.meta
            println("Total frames: " + meta.totalFrames)
            var nextFrame: Packet
            while (videoTrack.nextFrame().also { nextFrame = it } != null) {
                println(nextFrame.getPts())
            }
        }
    }

    init {
        tracks = LinkedList()
        for (file in files) {
            tracks.add(CodecMP4DemuxerTrack(DashMP4DemuxerTrack.createFromFiles(file)))
        }
    }
}