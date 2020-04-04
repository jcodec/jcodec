package org.jcodec.containers.mp4.muxer

import org.jcodec.common.AudioFormat
import org.jcodec.common.LongArrayList
import org.jcodec.common.Preconditions
import org.jcodec.common.model.Packet
import org.jcodec.common.model.Rational
import org.jcodec.common.model.Unit
import org.jcodec.containers.mp4.MP4TrackType
import org.jcodec.containers.mp4.boxes.AudioSampleEntry.Companion.audioSampleEntryPCM
import org.jcodec.containers.mp4.boxes.Box
import org.jcodec.containers.mp4.boxes.ChunkOffsets64Box.Companion.createChunkOffsets64Box
import org.jcodec.containers.mp4.boxes.HandlerBox.Companion.createHandlerBox
import org.jcodec.containers.mp4.boxes.Header
import org.jcodec.containers.mp4.boxes.MediaBox.Companion.createMediaBox
import org.jcodec.containers.mp4.boxes.MediaHeaderBox.Companion.createMediaHeaderBox
import org.jcodec.containers.mp4.boxes.MediaInfoBox.Companion.createMediaInfoBox
import org.jcodec.containers.mp4.boxes.MovieHeaderBox
import org.jcodec.containers.mp4.boxes.NodeBox
import org.jcodec.containers.mp4.boxes.SampleDescriptionBox.Companion.createSampleDescriptionBox
import org.jcodec.containers.mp4.boxes.SampleSizesBox.Companion.createSampleSizesBox
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.Companion.createSampleToChunkBox
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.SampleToChunkEntry
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.Companion.createTimeToSampleBox
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry
import org.jcodec.containers.mp4.boxes.TrackHeaderBox.Companion.createTrackHeaderBox
import org.jcodec.containers.mp4.boxes.TrakBox.Companion.createTrakBox
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class PCMMP4MuxerTrack(trackId: Int, format: AudioFormat) : AbstractMP4MuxerTrack(trackId, MP4TrackType.SOUND) {
    private val frameDuration: Int
    private val frameSize: Int
    private var framesInCurChunk = 0
    private val chunkOffsets: LongArrayList
    private var totalFrames = 0

    @Throws(IOException::class)
    override fun addFrame(outPacket: Packet) {
        addSamples(outPacket.getData().duplicate())
    }

    @Throws(IOException::class)
    fun addSamples(buffer: ByteBuffer) {
        curChunk.add(buffer)
        val frames = buffer.remaining() / frameSize
        totalFrames += frames
        framesInCurChunk += frames
        chunkDuration += frames * frameDuration.toLong()
        outChunkIfNeeded()
    }

    @Throws(IOException::class)
    private fun outChunkIfNeeded() {
        Preconditions.checkState(tgtChunkDurationUnit == Unit.FRAME || tgtChunkDurationUnit == Unit.SEC, "")
        if (tgtChunkDurationUnit == Unit.FRAME
                && framesInCurChunk * tgtChunkDuration!!.getDen() == tgtChunkDuration!!.getNum()) {
            outChunk()
        } else if (tgtChunkDurationUnit == Unit.SEC && chunkDuration > 0 && chunkDuration * tgtChunkDuration!!.getDen() >= tgtChunkDuration!!.getNum() * timescale) {
            outChunk()
        }
    }

    @Throws(IOException::class)
    private fun outChunk() {
        if (framesInCurChunk == 0) return
        chunkOffsets.add(out!!.position())
        for (b in curChunk) {
            out!!.write(b)
        }
        curChunk.clear()
        if (samplesInLastChunk == -1 || framesInCurChunk != samplesInLastChunk) {
            samplesInChunks.add(SampleToChunkEntry((chunkNo + 1).toLong(), framesInCurChunk, 1))
        }
        samplesInLastChunk = framesInCurChunk
        chunkNo++
        framesInCurChunk = 0
        chunkDuration = 0
    }

    @Throws(IOException::class)
    override fun finish(mvhd: MovieHeaderBox): Box {
        check(!finished) { "The muxer track has finished muxing" }
        outChunk()
        finished = true
        val trak = createTrakBox()
        val dd = displayDimensions
        val tkhd = createTrackHeaderBox(trackId,
                mvhd.getTimescale().toLong() * totalFrames * frameDuration / timescale, dd.width.toFloat(), dd.height.toFloat(),
                Date().time, Date().time, 1.0f, 0.toShort(), 0, intArrayOf(0x10000, 0, 0, 0, 0x10000, 0, 0, 0, 0x40000000))
        tkhd.flags = 0xf
        trak.add(tkhd)
        tapt(trak)
        val media = createMediaBox()
        trak.add(media)
        media.add(createMediaHeaderBox(timescale, totalFrames * frameDuration.toLong(), 0, Date().time,
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
        putEdits(trak)
        putName(trak)
        stbl.add(createSampleDescriptionBox(sampleEntries.orEmpty().toTypedArray()))
        stbl.add(createSampleToChunkBox(samplesInChunks.toTypedArray()))
        stbl.add(createSampleSizesBox(frameSize, totalFrames))
        stbl.add(createTimeToSampleBox(arrayOf(TimeToSampleEntry(totalFrames, frameDuration))))
        stbl.add(createChunkOffsets64Box(chunkOffsets.toArray()))
        return trak
    }

    override val trackTotalDuration: Long
        get() = (totalFrames * frameDuration).toLong()

    init {
        chunkOffsets = LongArrayList.createLongArrayList()
        frameDuration = 1
        frameSize = (format.sampleSizeInBits shr 3) * format.channels
        addSampleEntry(audioSampleEntryPCM(format))
        timescale = format.sampleRate
        setTgtChunkDuration(Rational(1, 2), Unit.SEC)
    }
}