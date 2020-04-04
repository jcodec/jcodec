package org.jcodec.containers.mp4.muxer

import org.jcodec.common.IntArrayList
import org.jcodec.common.Ints
import org.jcodec.common.LongArrayList
import org.jcodec.common.Preconditions
import org.jcodec.common.model.Packet
import org.jcodec.common.model.Rational
import org.jcodec.common.model.Unit
import org.jcodec.containers.mp4.MP4TrackType
import org.jcodec.containers.mp4.boxes.*
import org.jcodec.containers.mp4.boxes.ChunkOffsets64Box.Companion.createChunkOffsets64Box
import org.jcodec.containers.mp4.boxes.CompositionOffsetsBox.Companion.createCompositionOffsetsBox
import org.jcodec.containers.mp4.boxes.CompositionOffsetsBox.LongEntry
import org.jcodec.containers.mp4.boxes.HandlerBox.Companion.createHandlerBox
import org.jcodec.containers.mp4.boxes.MediaBox.Companion.createMediaBox
import org.jcodec.containers.mp4.boxes.MediaHeaderBox.Companion.createMediaHeaderBox
import org.jcodec.containers.mp4.boxes.MediaInfoBox.Companion.createMediaInfoBox
import org.jcodec.containers.mp4.boxes.SampleDescriptionBox.Companion.createSampleDescriptionBox
import org.jcodec.containers.mp4.boxes.SampleSizesBox.Companion.createSampleSizesBox2
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.Companion.createSampleToChunkBox
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.SampleToChunkEntry
import org.jcodec.containers.mp4.boxes.SyncSamplesBox.Companion.createSyncSamplesBox
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.Companion.createTimeToSampleBox
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry
import org.jcodec.containers.mp4.boxes.TrackHeaderBox.Companion.createTrackHeaderBox
import org.jcodec.containers.mp4.boxes.TrakBox.Companion.createTrakBox
import java.io.IOException
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
open class MP4MuxerTrack(trackId: Int, type: MP4TrackType) : AbstractMP4MuxerTrack(trackId, type) {
    private val sampleDurations: MutableList<TimeToSampleEntry>
    private var sameDurCount: Long = 0
    private var curDuration: Long = -1
    private val chunkOffsets: LongArrayList
    private val sampleSizes: IntArrayList
    private val iframes: IntArrayList
    private val compositionOffsets: MutableList<LongEntry>
    private var lastCompositionOffset: Long = 0
    private var lastCompositionSamples: Long = 0
    private var ptsEstimate: Long = 0
    private var lastEntry = -1
     override var trackTotalDuration: Long = 0
    private var curFrame = 0
    private var allIframes = true
    var timecodeTrack: TimecodeMP4MuxerTrack? = null
        private set

    @Throws(IOException::class)
    override fun addFrame(pkt: Packet) {
        addFrameInternal(pkt, 1)
        processTimecode(pkt)
    }

    @Throws(IOException::class)
    open fun addFrameInternal(pkt: Packet, entryNo: Int) {
        check(!finished) { "The muxer track has finished muxing" }
        if (timescale == NO_TIMESCALE_SET) {
            timescale = pkt.getTimescale()
        }
        if (timescale != pkt.getTimescale()) {
            pkt.setPts(pkt.getPts() * timescale / pkt.getTimescale())
            pkt.setDuration(pkt.getDuration() * timescale / pkt.getTimescale())
            pkt.setTimescale(timescale)
        }
        if (type == MP4TrackType.VIDEO) {
            val compositionOffset = pkt.getPts() - ptsEstimate
            if (compositionOffset != lastCompositionOffset) {
                if (lastCompositionSamples > 0) compositionOffsets.add(LongEntry(lastCompositionSamples, lastCompositionOffset))
                lastCompositionOffset = compositionOffset
                lastCompositionSamples = 0
            }
            lastCompositionSamples++
            ptsEstimate += pkt.getDuration()
        }
        if (lastEntry != -1 && lastEntry != entryNo) {
            outChunk(lastEntry)
            samplesInLastChunk = -1
        }
        curChunk.add(pkt.getData())
        if (pkt.isKeyFrame) iframes.add(curFrame + 1) else allIframes = false
        curFrame++
        chunkDuration += pkt.getDuration()
        if (curDuration != -1L && pkt.getDuration() != curDuration) {
            sampleDurations.add(TimeToSampleEntry(sameDurCount.toInt(), curDuration.toInt()))
            sameDurCount = 0
        }
        curDuration = pkt.getDuration()
        sameDurCount++
        trackTotalDuration += pkt.getDuration()
        outChunkIfNeeded(entryNo)
        lastEntry = entryNo
    }

    @Throws(IOException::class)
    private fun processTimecode(pkt: Packet) {
        if (timecodeTrack != null) timecodeTrack!!.addTimecode(pkt)
    }

    @Throws(IOException::class)
    private fun outChunkIfNeeded(entryNo: Int) {
        Preconditions.checkState(tgtChunkDurationUnit == Unit.FRAME || tgtChunkDurationUnit == Unit.SEC)
        val tgtChunkDuration = tgtChunkDuration!!
        if (tgtChunkDurationUnit == Unit.FRAME
                && curChunk.size * tgtChunkDuration.getDen() == tgtChunkDuration.getNum()) {
            outChunk(entryNo)
        } else if (tgtChunkDurationUnit == Unit.SEC && chunkDuration > 0 && chunkDuration * tgtChunkDuration.getDen() >= tgtChunkDuration.getNum() * timescale) {
            outChunk(entryNo)
        }
    }

    @Throws(IOException::class)
    fun outChunk(entryNo: Int) {
        if (curChunk.size == 0) return
        chunkOffsets.add(out!!.position())
        for (bs in curChunk) {
            sampleSizes.add(bs.remaining())
            out!!.write(bs)
        }
        if (samplesInLastChunk == -1 || samplesInLastChunk != curChunk.size) {
            samplesInChunks.add(SampleToChunkEntry((chunkNo + 1).toLong(), curChunk.size, entryNo))
        }
        samplesInLastChunk = curChunk.size
        chunkNo++
        chunkDuration = 0
        curChunk.clear()
    }

    @Throws(IOException::class)
    override fun finish(mvhd: MovieHeaderBox): Box? {
        Preconditions.checkState(!finished, "The muxer track has finished muxing")
        outChunk(lastEntry)
        if (sameDurCount > 0) {
            sampleDurations.add(TimeToSampleEntry(sameDurCount.toInt(), curDuration.toInt()))
        }
        finished = true
        val trak = createTrakBox()
        val dd = displayDimensions
        val tkhd = createTrackHeaderBox(trackId,
                mvhd.getTimescale() * trackTotalDuration / timescale, dd.width.toFloat(), dd.height.toFloat(),
                Date().time, Date().time, 1.0f, 0.toShort(), 0, intArrayOf(0x10000, 0, 0, 0, 0x10000, 0, 0, 0, 0x40000000))
        tkhd.flags = 0xf
        trak.add(tkhd)
        tapt(trak)
        val media = createMediaBox()
        trak.add(media)
        media.add(createMediaHeaderBox(timescale, trackTotalDuration, 0, Date().time,
                Date().time, 0))
        val hdlr = createHandlerBox("mhlr", type.handler, "appl", 0, 0)
        media.add(hdlr)
        val minf = createMediaInfoBox()
        media.add(minf)
        mediaHeader(minf, type)
        minf.add(createHandlerBox("dhlr", "url ", "appl", 0, 0))
        addDref(minf)
        val stbl = NodeBox(Header("stbl"))
        minf.add(stbl)
        putCompositionOffsets(stbl)
        putEdits(trak)
        putName(trak)
        stbl.add(createSampleDescriptionBox(sampleEntries.orEmpty().toTypedArray()))
        stbl.add(createSampleToChunkBox(samplesInChunks.toTypedArray()))
        stbl.add(createSampleSizesBox2(sampleSizes.toArray()))
        stbl.add(createTimeToSampleBox(sampleDurations.toTypedArray()))
        stbl.add(createChunkOffsets64Box(chunkOffsets.toArray()))
        if (!allIframes && iframes.size() > 0) stbl.add(createSyncSamplesBox(iframes.toArray()))
        return trak
    }

    private fun putCompositionOffsets(stbl: NodeBox) {
        if (compositionOffsets.size > 0) {
            compositionOffsets.add(LongEntry(lastCompositionSamples, lastCompositionOffset))
            val min = minLongOffset(compositionOffsets)
            if (min > 0) {
                for (entry in compositionOffsets) {
                    entry.offset = entry.offset - min
                }
            }
            val first = compositionOffsets[0]
            if (first.offset > 0) {
                if (_edits == null) {
                    _edits = ArrayList()
                    _edits?.add(Edit(trackTotalDuration, first.offset, 1.0f))
                } else {
                    for (edit in _edits.orEmpty()) {
                        edit.mediaTime = edit.mediaTime + first.offset
                    }
                }
            }

            val intEntries = compositionOffsets.map { longEntry -> CompositionOffsetsBox.Entry(Ints.checkedCast(longEntry.count), Ints.checkedCast(longEntry.offset)) }.toTypedArray()
            stbl.add(createCompositionOffsetsBox(intEntries))
        }
    }

    fun setTimecode(timecodeTrack: TimecodeMP4MuxerTrack?) {
        this.timecodeTrack = timecodeTrack
    }

    fun addMetaSampleEntry(contentEncoding: String?, contentType: String?) {
        val se = TextMetaDataSampleEntry(contentEncoding, contentType)
        addSampleEntry(se)
    }

    companion object {
        fun minLongOffset(offs: List<LongEntry>): Long {
            var min = Long.MAX_VALUE
            for (entry in offs) {
                min = Math.min(min, entry.offset)
            }
            return min
        }

        fun minOffset(offs: List<CompositionOffsetsBox.Entry>): Int {
            var min = Int.MAX_VALUE
            for (entry in offs) {
                min = Math.min(min, entry.offset)
            }
            return min
        }
    }

    init {
        sampleDurations = ArrayList()
        chunkOffsets = LongArrayList.createLongArrayList()
        sampleSizes = IntArrayList.createIntArrayList()
        iframes = IntArrayList.createIntArrayList()
        compositionOffsets = ArrayList()
        setTgtChunkDuration(Rational(1, 1), Unit.FRAME)
    }
}