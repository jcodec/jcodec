package org.jcodec.containers.mp4.demuxer

import org.jcodec.common.DemuxerTrackMeta
import org.jcodec.common.SeekableDemuxerTrack
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.model.RationalLarge
import org.jcodec.containers.mp4.MP4Packet
import org.jcodec.containers.mp4.MP4TrackType
import org.jcodec.containers.mp4.boxes.*
import org.jcodec.containers.mp4.boxes.Box.Companion.path
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findAllPath
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirst
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirstPath
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.SampleToChunkEntry
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry
import org.jcodec.containers.mp4.boxes.TrakBox.Companion.getTrackType
import java.io.IOException
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Shared routines between PCM and Frames tracks
 *
 * @author The JCodec project
 */
abstract class AbstractMP4DemuxerTrack(trak: TrakBox) : SeekableDemuxerTrack {
    var box: TrakBox
        protected set
    val type: MP4TrackType?
    val no: Int
    var sampleEntries: Array<SampleEntry?>?
        protected set

    @JvmField
    protected var timeToSamples: Array<TimeToSampleEntry?>

    @JvmField
    protected var sampleToChunks: Array<SampleToChunkEntry?>

    @JvmField
    protected var chunkOffsets: LongArray

    @JvmField
    protected var duration: Long = 0

    @JvmField
    protected var sttsInd = 0
    protected var sttsSubInd = 0

    @JvmField
    protected var stcoInd = 0

    @JvmField
    protected var stscInd = 0

    @JvmField
    protected var pts: Long = 0

    @JvmField
    protected var curFrame: Long = 0

    @JvmField
    protected var timescale: Int
    fun pts2Sample(_tv: Long, _timescale: Int): Int {
        var tv = _tv * timescale / _timescale
        var ttsInd: Int
        var sample = 0
        ttsInd = 0
        while (ttsInd < timeToSamples.size - 1) {
            val a = timeToSamples[ttsInd]!!.sampleCount * timeToSamples[ttsInd]!!.sampleDuration
            if (tv < a) break
            tv -= a.toLong()
            sample += timeToSamples[ttsInd]!!.sampleCount
            ttsInd++
        }
        return sample + (tv / timeToSamples[ttsInd]!!.sampleDuration).toInt()
    }

    fun getTimescale(): Long {
        return timescale.toLong()
    }

    protected abstract fun seekPointer(frameNo: Long)
    fun canSeek(pts: Long): Boolean {
        return pts >= 0 && pts < duration
    }

    @Synchronized
    fun seekPts(pts: Long): Boolean {
        require(pts >= 0) { "Seeking to negative pts" }
        if (pts >= duration) return false
        var prevDur: Long = 0
        var frameNo = 0
        sttsInd = 0
        while (pts > prevDur + timeToSamples[sttsInd]!!.sampleCount
                * timeToSamples[sttsInd]!!.sampleDuration
                && sttsInd < timeToSamples.size - 1) {
            prevDur += timeToSamples[sttsInd]!!.sampleCount * timeToSamples[sttsInd]!!.sampleDuration.toLong()
            frameNo += timeToSamples[sttsInd]!!.sampleCount
            sttsInd++
        }
        sttsSubInd = ((pts - prevDur) / timeToSamples[sttsInd]!!.sampleDuration).toInt()
        frameNo += sttsSubInd
        this.pts = prevDur + timeToSamples[sttsInd]!!.sampleDuration * sttsSubInd
        seekPointer(frameNo.toLong())
        return true
    }

    protected fun shiftPts(frames: Long) {
        pts -= sttsSubInd * timeToSamples[sttsInd]!!.sampleDuration.toLong()
        sttsSubInd += frames.toInt()
        while (sttsInd < timeToSamples.size - 1 && sttsSubInd >= timeToSamples[sttsInd]!!.sampleCount) {
            pts += timeToSamples[sttsInd]!!.segmentDuration
            sttsSubInd -= timeToSamples[sttsInd]!!.sampleCount
            sttsInd++
        }
        pts += sttsSubInd * timeToSamples[sttsInd]!!.sampleDuration.toLong()
    }

    protected fun nextChunk() {
        if (stcoInd >= chunkOffsets.size) return
        stcoInd++
        if (stscInd + 1 < sampleToChunks.size && stcoInd + 1.toLong() == sampleToChunks[stscInd + 1]!!.first) {
            stscInd++
        }
    }

    @Synchronized
    override fun gotoFrame(frameNo: Long): Boolean {
        require(frameNo >= 0) { "negative frame number" }
        if (frameNo >= frameCount) return false
        if (frameNo == curFrame) return true
        seekPointer(frameNo)
        seekFrame(frameNo)
        return true
    }

    override fun seek(second: Double) {
        seekPts((second * timescale).toLong())
    }

    private fun seekFrame(frameNo: Long) {
        sttsSubInd = 0
        sttsInd = sttsSubInd
        pts = sttsInd.toLong()
        shiftPts(frameNo)
    }

    fun getDuration(): RationalLarge {
        return RationalLarge(box.mediaDuration, box.timescale.toLong())
    }

    abstract val frameCount: Long
    override fun getCurFrame(): Long {
        return curFrame
    }

    val edits: List<Edit>?
        get() {
            val editListBox = findFirstPath(box, path("edts.elst")) as EditListBox?
            return editListBox?.getEdits()
        }

    val name: String?
        get() {
            val nameBox = findFirstPath(box, path("udta.name")) as NameBox?
            return nameBox?.name
        }

    val fourcc: String?
        get() {
            val entries = sampleEntries
            val se = if (entries == null || entries.size == 0) null else entries[0]
            return se?.header?.fourcc
        }

    @Throws(IOException::class)
    protected fun readPacketData(input: SeekableByteChannel, buffer: ByteBuffer, offset: Long, size: Int): ByteBuffer {
        val result = buffer.duplicate()
        synchronized(input) {
            input.setPosition(offset)
            NIOUtils.readL(input, result, size)
        }
        result.flip()
        return result
    }

    @Throws(IOException::class)
    abstract fun getNextFrame(storage: ByteBuffer?): MP4Packet?
    fun convertPacket(_in: ByteBuffer): ByteBuffer {
        return _in
    }

    override fun getMeta(): DemuxerTrackMeta {
        return MP4DemuxerTrackMeta.fromTrack(this)
    }

    init {
        no = trak.trackHeader.getNo()
        type = getTrackType(trak)
        sampleEntries = findAllPath(trak, arrayOf("mdia", "minf", "stbl", "stsd", null)).map { MP4Demuxer.asSampleEntry(it) }.toTypedArray()
        val stbl = trak.mdia.minf.stbl
        val stts = findFirst(stbl, "stts") as TimeToSampleBox?
        val stsc = findFirst(stbl, "stsc") as SampleToChunkBox?
        val stco = findFirst(stbl, "stco") as ChunkOffsetsBox?
        val co64 = findFirst(stbl, "co64") as ChunkOffsets64Box?
        timeToSamples = stts!!.getEntries()
        sampleToChunks = stsc!!.getSampleToChunk()
        chunkOffsets = stco?.getChunkOffsets() ?: co64!!.getChunkOffsets()
        for (i in timeToSamples.indices) {
            val ttse = timeToSamples[i]
            duration += ttse!!.sampleCount * ttse.sampleDuration.toLong()
        }
        box = trak
        timescale = trak.timescale
    }
}