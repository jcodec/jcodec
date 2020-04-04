package org.jcodec.containers.mp4.muxer

import org.jcodec.common.Codec
import org.jcodec.common.model.Packet
import org.jcodec.common.model.Packet.FrameType
import org.jcodec.common.model.Rational
import org.jcodec.common.model.TapeTimecode
import org.jcodec.containers.mp4.MP4Packet
import org.jcodec.containers.mp4.MP4TrackType
import org.jcodec.containers.mp4.boxes.Box
import org.jcodec.containers.mp4.boxes.Edit
import org.jcodec.containers.mp4.boxes.MovieHeaderBox
import org.jcodec.containers.mp4.boxes.TimecodeSampleEntry.Companion.createTimecodeSampleEntry
import org.jcodec.movtool.Util
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Timecode MP4 muxer track
 *
 * @author The JCodec project
 */
class TimecodeMP4MuxerTrack(trackId: Int) : CodecMP4MuxerTrack(trackId, MP4TrackType.TIMECODE, Codec.TIMECODE) {
    private var prevTimecode: TapeTimecode? = null
    private var firstTimecode: TapeTimecode? = null
    private var fpsEstimate = 0
    private var sampleDuration: Long = 0
    private var samplePts: Long = 0
    private var tcFrames = 0
    private val lower: MutableList<Edit>
    private val gop: MutableList<Packet>

    @Throws(IOException::class)
    fun addTimecode(packet: Packet) {
        if (timescale == AbstractMP4MuxerTrack.NO_TIMESCALE_SET) timescale = packet.getTimescale()
        if (timescale != AbstractMP4MuxerTrack.NO_TIMESCALE_SET && timescale != packet.getTimescale()) throw RuntimeException("MP4 timecode track doesn't support timescale switching.")
        if (packet.isKeyFrame) processGop()
        gop.add(Packet.createPacketWithData(packet, null as ByteBuffer?))
    }

    @Throws(IOException::class)
    private fun processGop() {
        if (gop.size > 0) {
            for (pkt in sortByDisplay(gop)) {
                addTimecodeInt(pkt)
            }
            gop.clear()
        }
    }

    private fun sortByDisplay(gop: List<Packet>): List<Packet> {
        val result = ArrayList(gop)
        Collections.sort(result) { o1, o2 ->
            if (o1 == null && o2 == null) 0 else if (o1 == null) -1 else if (o2 == null) 1 else if (o1.getDisplayOrder() > o2.getDisplayOrder()) 1 else if (o1.getDisplayOrder() == o2
                            .getDisplayOrder()) 0 else -1
        }
        return result
    }

    @Throws(IOException::class)
    override fun finish(mvhd: MovieHeaderBox): Box? {
        processGop()
        outTimecodeSample()
        if (sampleEntries.orEmpty().size == 0) return null
        _edits = if (_edits != null) {
            Util.editsOnEdits(Rational(1, 1), lower, _edits)
        } else lower
        return super.finish(mvhd)
    }

    @Throws(IOException::class)
    private fun addTimecodeInt(packet: Packet) {
        val tapeTimecode = packet.getTapeTimecode()
        val gap = isGap(prevTimecode, tapeTimecode)
        prevTimecode = tapeTimecode
        if (gap) {
            outTimecodeSample()
            firstTimecode = tapeTimecode
            fpsEstimate = if (tapeTimecode.isDropFrame) 30 else -1
            samplePts += sampleDuration
            sampleDuration = 0
            tcFrames = 0
        }
        sampleDuration += packet.getDuration()
        tcFrames++
    }

    private fun isGap(prevTimecode: TapeTimecode?, tapeTimecode: TapeTimecode?): Boolean {
        var gap = false
        if (prevTimecode == null && tapeTimecode != null) {
            gap = true
        } else if (prevTimecode != null) {
            gap = if (tapeTimecode == null) true else {
                if (prevTimecode.isDropFrame != tapeTimecode.isDropFrame) {
                    true
                } else {
                    isTimeGap(prevTimecode, tapeTimecode)
                }
            }
        }
        return gap
    }

    private fun isTimeGap(prevTimecode: TapeTimecode, tapeTimecode: TapeTimecode): Boolean {
        var gap = false
        val sec = toSec(tapeTimecode)
        val secDiff = sec - toSec(prevTimecode)
        if (secDiff == 0) {
            var frameDiff = tapeTimecode.frame - prevTimecode.frame
            if (fpsEstimate != -1) frameDiff = (frameDiff + fpsEstimate) % fpsEstimate
            gap = frameDiff != 1
        } else if (secDiff == 1) {
            if (fpsEstimate == -1) {
                if (tapeTimecode.frame.toInt() == 0) fpsEstimate = prevTimecode.frame + 1 else gap = true
            } else {
                val firstFrame = if (tapeTimecode.isDropFrame && sec % 60 == 0 && sec % 600 != 0) 2 else 0
                if (tapeTimecode.frame.toInt() != firstFrame || prevTimecode.frame.toInt() != fpsEstimate - 1) gap = true
            }
        } else {
            gap = true
        }
        return gap
    }

    @Throws(IOException::class)
    private fun outTimecodeSample() {
        if (sampleDuration > 0) {
            if (firstTimecode != null) {
                if (fpsEstimate == -1) fpsEstimate = prevTimecode!!.frame + 1
                val tmcd = createTimecodeSampleEntry(if (firstTimecode!!.isDropFrame) 1 else 0, timescale, (sampleDuration / tcFrames).toInt(), fpsEstimate)
                sampleEntries?.add(tmcd)
                val sample = ByteBuffer.allocate(4)
                sample.putInt(toCounter(firstTimecode!!, fpsEstimate))
                sample.flip()
                addFrame(MP4Packet.createMP4Packet(sample, samplePts, timescale, sampleDuration, 0, FrameType.KEY, null, 0, samplePts, sampleEntries.orEmpty().size - 1))
                lower.add(Edit(sampleDuration, samplePts, 1.0f))
            } else {
                lower.add(Edit(sampleDuration, -1, 1.0f))
            }
        }
    }

    private fun toCounter(tc: TapeTimecode, fps: Int): Int {
        var frames = toSec(tc) * fps + tc.frame
        if (tc.isDropFrame) {
            val D = frames / 18000.toLong()
            val M = frames % 18000.toLong()
            frames -= (18 * D + 2 * ((M - 2) / 1800).toInt()).toInt()
        }
        return frames
    }

    companion object {
        private fun toSec(tc: TapeTimecode): Int {
            return tc.hour * 3600 + tc.minute * 60 + tc.second
        }
    }

    init {
        lower = ArrayList()
        gop = ArrayList()
    }
}