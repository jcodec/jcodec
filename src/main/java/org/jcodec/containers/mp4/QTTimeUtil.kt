package org.jcodec.containers.mp4

import org.jcodec.common.model.RationalLarge
import org.jcodec.containers.mp4.BoxUtil.containsBox2
import org.jcodec.containers.mp4.boxes.Box.Companion.path
import org.jcodec.containers.mp4.boxes.MovieBox
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirstPath
import org.jcodec.containers.mp4.boxes.TimeToSampleBox
import org.jcodec.containers.mp4.boxes.TimecodeSampleEntry
import org.jcodec.containers.mp4.boxes.TrakBox
import org.jcodec.containers.mp4.demuxer.TimecodeMP4DemuxerTrack
import java.io.IOException

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Quicktime time conversion utilities
 *
 * @author The JCodec project
 */
object QTTimeUtil {
    /**
     * Calculates track duration considering edits
     *
     * @param track
     * @return
     */
    fun getEditedDuration(track: TrakBox): Long {
        val edits = track.edits ?: return track.duration
        var duration: Long = 0
        for (edit in edits) {
            duration += edit.duration
        }
        return duration
    }

    /**
     * Finds timevalue of a frame number
     *
     * might be an expensive operation sinse it traverses compressed time to
     * sample table
     *
     * @param frameNumber
     * @return
     */
    fun frameToTimevalue(trak: TrakBox?, frameNumber: Int): Long {
        val stts = findFirstPath(trak, path("mdia.minf.stbl.stts")) as TimeToSampleBox?
        val timeToSamples = stts!!.getEntries()
        var pts: Long = 0
        var sttsInd = 0
        var sttsSubInd = frameNumber
        while (sttsSubInd >= timeToSamples[sttsInd]!!.sampleCount) {
            sttsSubInd -= timeToSamples[sttsInd]!!.sampleCount
            pts += timeToSamples[sttsInd]!!.sampleCount * timeToSamples[sttsInd]!!.sampleDuration.toLong()
            sttsInd++
        }
        return pts + timeToSamples[sttsInd]!!.sampleDuration * sttsSubInd
    }

    /**
     * Finds frame by timevalue
     *
     * @param tv
     * @return
     */
    fun timevalueToFrame(trak: TrakBox?, tv: Long): Int {
        var tv = tv
        val tts = (findFirstPath(trak, path("mdia.minf.stbl.stts")) as TimeToSampleBox?)!!.getEntries()
        var frame = 0
        var i = 0
        while (tv > 0 && i < tts.size) {
            val rem = tv / tts[i]!!.sampleDuration
            tv -= tts[i]!!.sampleCount * tts[i]!!.sampleDuration.toLong()
            frame += if (tv > 0) tts[i]!!.sampleCount else rem.toInt()
            i++
        }
        return frame
    }

    /**
     * Converts media timevalue to edited timevalue
     *
     * @param trak
     * @param mediaTv
     * @param movieTimescale
     * @return
     */
    fun mediaToEdited(trak: TrakBox?, mediaTv: Long, movieTimescale: Int): Long {
        if (trak!!.edits == null) return mediaTv
        var accum: Long = 0
        for (edit in trak.edits!!) {
            if (mediaTv < edit.mediaTime) return accum
            val duration = trak.rescale(edit.duration, movieTimescale.toLong())
            if (edit.mediaTime != -1L && mediaTv >= edit.mediaTime && mediaTv < edit.mediaTime + duration) {
                accum += mediaTv - edit.mediaTime
                break
            }
            accum += duration
        }
        return accum
    }

    /**
     * Converts edited timevalue to media timevalue
     *
     * @param trak
     * @param movieTimescale
     * @return
     */
    fun editedToMedia(trak: TrakBox, editedTv: Long, movieTimescale: Int): Long {
        if (trak.edits == null) return editedTv
        var accum: Long = 0
        for (edit in trak.edits!!) {
            val duration = trak.rescale(edit.duration, movieTimescale.toLong())
            if (accum + duration > editedTv) {
                return edit.mediaTime + editedTv - accum
            }
            accum += duration
        }
        return accum
    }

    /**
     * Calculates frame number as it shows in quicktime player
     *
     * @param movie
     * @param mediaFrameNo
     * @return
     */
    fun qtPlayerFrameNo(movie: MovieBox, mediaFrameNo: Int): Int {
        val videoTrack = movie.videoTrack
        val editedTv = mediaToEdited(videoTrack, frameToTimevalue(videoTrack, mediaFrameNo), movie.timescale)
        return tv2QTFrameNo(movie, editedTv)
    }

    fun tv2QTFrameNo(movie: MovieBox, tv: Long): Int {
        val videoTrack = movie.videoTrack
        val timecodeTrack = movie.timecodeTrack
        return if (timecodeTrack != null && containsBox2(videoTrack, "tref", "tmcd")) {
            timevalueToTimecodeFrame(timecodeTrack, RationalLarge(tv, videoTrack!!.timescale.toLong()),
                    movie.timescale)
        } else {
            timevalueToFrame(videoTrack, tv)
        }
    }

    /**
     * Calculates and formats standard time as in Quicktime player
     *
     * @param movie
     * @param mediaFrameNo
     * @return
     */
    fun qtPlayerTime(movie: MovieBox, mediaFrameNo: Int): String {
        val videoTrack = movie.videoTrack
        val editedTv = mediaToEdited(videoTrack, frameToTimevalue(videoTrack, mediaFrameNo), movie.timescale)
        val sec = (editedTv / videoTrack!!.timescale).toInt()
        return (String.format("%02d", sec / 3600) + "_" + String.format("%02d", sec % 3600 / 60) + "_"
                + String.format("%02d", sec % 60))
    }

    /**
     * Calculates and formats tape timecode as in Quicktime player
     *
     * @param timecodeTrack
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    fun qtPlayerTimecodeFromMovie(movie: MovieBox, timecodeTrack: TimecodeMP4DemuxerTrack, mediaFrameNo: Int): String {
        val videoTrack = movie.videoTrack
        val editedTv = mediaToEdited(videoTrack, frameToTimevalue(videoTrack, mediaFrameNo), movie.timescale)
        val tt = timecodeTrack.box
        val ttTimescale = tt.timescale
        val ttTv = editedToMedia(tt, editedTv * ttTimescale / videoTrack!!.timescale, movie.timescale)
        return formatTimecode(
                timecodeTrack.box, timecodeTrack.startTimecode
                + timevalueToTimecodeFrame(timecodeTrack.box, RationalLarge(ttTv, ttTimescale.toLong()),
                movie.timescale))
    }

    /**
     * Calculates and formats tape timecode as in Quicktime player
     *
     * @param timecodeTrack
     * @param tv
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    fun qtPlayerTimecode(timecodeTrack: TimecodeMP4DemuxerTrack, tv: RationalLarge, movieTimescale: Int): String {
        val tt = timecodeTrack.box
        val ttTimescale = tt.timescale
        val ttTv = editedToMedia(tt, tv.multiplyS(ttTimescale.toLong()), movieTimescale)
        return formatTimecode(
                timecodeTrack.box, timecodeTrack.startTimecode
                + timevalueToTimecodeFrame(timecodeTrack.box, RationalLarge(ttTv, ttTimescale.toLong()),
                movieTimescale))
    }

    /**
     * Converts timevalue to frame number based on timecode track
     *
     * @param timecodeTrack
     * @param tv
     * @return
     */
    fun timevalueToTimecodeFrame(timecodeTrack: TrakBox, tv: RationalLarge, movieTimescale: Int): Int {
        val se = timecodeTrack.sampleEntries[0] as TimecodeSampleEntry
        return (2 * tv.multiplyS(se.timescale.toLong()) / se.frameDuration + 1).toInt() / 2
    }

    /**
     * Formats tape timecode based on frame counter
     *
     * @param timecodeTrack
     * @param counter
     * @return
     */
    fun formatTimecode(timecodeTrack: TrakBox?, counter: Int): String {
        var counter = counter
        val tmcd = findFirstPath(timecodeTrack, path("mdia.minf.stbl.stsd.tmcd")) as TimecodeSampleEntry?
        val nf = tmcd!!.numFrames
        var tc = String.format("%02d", counter % nf)
        counter /= nf.toInt()
        tc = String.format("%02d", counter % 60) + ":" + tc
        counter /= 60
        tc = String.format("%02d", counter % 60) + ":" + tc
        counter /= 60
        tc = String.format("%02d", counter) + ":" + tc
        return tc
    }
}