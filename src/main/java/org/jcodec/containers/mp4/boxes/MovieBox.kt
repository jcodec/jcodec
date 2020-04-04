package org.jcodec.containers.mp4.boxes

import org.jcodec.common.model.Size
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Creates MP4 file out of a set of samples
 *
 * @author The JCodec project
 */
class MovieBox(atom: Header) : NodeBox(atom) {
    val tracks: Array<TrakBox>
        get() = findAll(this, "trak").map { it as TrakBox }.toTypedArray()

    val videoTrack: TrakBox?
        get() {
            val tracks = tracks
            for (i in tracks.indices) {
                val trakBox = tracks[i]
                if (trakBox.isVideo) return trakBox
            }
            return null
        }

    val timecodeTrack: TrakBox?
        get() {
            val tracks = tracks
            for (i in tracks.indices) {
                val trakBox = tracks[i]
                if (trakBox.isTimecode) return trakBox
            }
            return null
        }

    var timescale: Int
        get() = movieHeader.getTimescale()
        private set(newTs) {
            (findFirst(this, "mvhd") as MovieHeaderBox).setTimescale(newTs)
        }

    fun rescale(tv: Long, ts: Long): Long {
        return tv * timescale / ts
    }

    fun fixTimescale(newTs: Int) {
        val oldTs = timescale
        timescale = newTs
        val tracks = tracks
        for (i in tracks.indices) {
            val trakBox = tracks[i]
            trakBox.duration = rescale(trakBox.duration, oldTs.toLong())
            val edits = trakBox.edits ?: continue
            val lit = edits.toMutableList().listIterator()
            while (lit.hasNext()) {
                val edit = lit.next()
                lit.set(Edit(rescale(edit.duration, oldTs.toLong()), edit.mediaTime, edit.rate))
            }
        }
        duration = rescale(duration, oldTs.toLong())
    }

    val movieHeader: MovieHeaderBox
        get() = findFirst(this, "mvhd") as MovieHeaderBox

    val audioTracks: List<TrakBox>
        get() {
            val result = ArrayList<TrakBox>()
            val tracks = tracks
            for (i in tracks.indices) {
                val trakBox = tracks[i]
                if (trakBox.isAudio) result.add(trakBox)
            }
            return result
        }

    val metaTracks: List<TrakBox>
        get() {
            val result = ArrayList<TrakBox>()
            val tracks = tracks
            for (i in tracks.indices) {
                val trakBox = tracks[i]
                if (trakBox.isMeta) result.add(trakBox)
            }
            return result
        }

    var duration: Long
        get() = movieHeader.getDuration()
        set(movDuration) {
            movieHeader.setDuration(movDuration)
        }

    fun importTrack(movie: MovieBox, track: TrakBox?): TrakBox {
        val newTrack = cloneBox(track!!, 1024 * 1024, _factory) as TrakBox
        val edits = newTrack.edits
        val result = ArrayList<Edit>()
        if (edits != null) {
            for (edit in edits) {
                result.add(Edit(rescale(edit.duration, movie.timescale.toLong()), edit.mediaTime, edit
                        .rate))
            }
        }
        newTrack.edits = result
        return newTrack
    }

    fun appendTrack(newTrack: TrakBox) {
        newTrack.trackHeader.trackId = movieHeader.getNextTrackId()
        movieHeader.setNextTrackId(movieHeader.getNextTrackId() + 1)
        _boxes.add(newTrack)
    }

    val isPureRefMovie: Boolean
        get() {
            var pureRef = true
            val tracks = tracks
            for (i in tracks.indices) {
                val trakBox = tracks[i]
                pureRef = pureRef and trakBox.isPureRef
            }
            return pureRef
        }

    fun updateDuration() {
        val tracks = tracks
        var min = Int.MAX_VALUE.toLong()
        for (i in tracks.indices) {
            val trakBox = tracks[i]
            if (trakBox.duration < min) min = trakBox.duration
        }
        movieHeader.setDuration(min)
    }

    val displaySize: Size?
        get() {
            val videoTrack = videoTrack ?: return null
            val clef = findFirstPath(videoTrack, Box.path("tapt.clef")) as ClearApertureBox?
            if (clef != null) {
                return applyMatrix(videoTrack, Size(clef.width.toInt(), clef.height.toInt()))
            }
            val box = (findFirstPath(videoTrack, Box.path("mdia.minf.stbl.stsd")) as SampleDescriptionBox?)!!.getBoxes().firstOrNull()
            if (box == null || box !is VideoSampleEntry) return null
            val vs = box
            val par = videoTrack.pAR
            return applyMatrix(videoTrack,
                    Size((vs.width * par!!.getNum() / par.getDen()), vs.height.toInt()))
        }

    private fun applyMatrix(videoTrack: TrakBox, size: Size): Size {
        val matrix = videoTrack.trackHeader.matrix
        return Size((size.width.toDouble() * matrix!![0] / 65536).toInt(), (size.height.toDouble()
                * matrix[4] / 65536).toInt())
    }

    val storedSize: Size?
        get() {
            val videoTrack = videoTrack ?: return null
            val enof = findFirstPath(videoTrack, Box.path("tapt.enof")) as EncodedPixelBox?
            if (enof != null) {
                return Size(enof.width.toInt(), enof.height.toInt())
            }
            val box = (findFirstPath(videoTrack, Box.path("mdia.minf.stbl.stsd")) as SampleDescriptionBox?)!!.getBoxes().firstOrNull()
            if (box == null || box !is VideoSampleEntry) return null
            val vs = box
            return Size(vs.width.toInt(), vs.height.toInt())
        }

    fun getTrackById(id: Int): TrakBox? {
        val tracks = tracks
        for (trakBox in tracks) {
            if (trakBox.trackHeader.trackId === id) return trakBox
        }
        return null
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "moov"
        }

        @JvmStatic
        fun createMovieBox(): MovieBox {
            return MovieBox(Header(fourcc()))
        }
    }
}