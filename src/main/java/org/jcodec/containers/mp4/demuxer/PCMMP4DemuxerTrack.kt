package org.jcodec.containers.mp4.demuxer

import org.jcodec.common.AudioCodecMeta
import org.jcodec.common.Codec
import org.jcodec.common.DemuxerTrackMeta
import org.jcodec.common.TrackType
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.model.Packet
import org.jcodec.common.model.Packet.FrameType
import org.jcodec.containers.mp4.MP4Packet
import org.jcodec.containers.mp4.QTTimeUtil
import org.jcodec.containers.mp4.boxes.AudioSampleEntry
import org.jcodec.containers.mp4.boxes.Box.Companion.path
import org.jcodec.containers.mp4.boxes.MovieBox
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirstPath
import org.jcodec.containers.mp4.boxes.SampleSizesBox
import org.jcodec.containers.mp4.boxes.TrakBox
import java.io.IOException
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Specialized demuxer track for PCM audio samples
 *
 * Always reads one chunk of frames at a time, except for after seek. After seek
 * the beginning of chunk before the seek point is not read effectivaly reading
 * PCM frame from exactly the frame seek was performed to.
 *
 * Packet size depends on underlying container PCM chunk sizes.
 *
 * @author The JCodec project
 */
class PCMMP4DemuxerTrack(private val movie: MovieBox, trak: TrakBox?, private val input: SeekableByteChannel) : AbstractMP4DemuxerTrack(trak!!) {
    private val defaultSampleSize: Int
    private var posShift = 0
    protected var totalFrames: Int = 0

    @Throws(IOException::class)
    override fun nextFrame(): Packet {
        val frameSize = frameSize
        val chSize = sampleToChunks[stscInd]!!.count * frameSize - posShift
        return getNextFrame(ByteBuffer.allocate(chSize))!!
    }

    @Synchronized
    @Throws(IOException::class)
    override fun getNextFrame(buffer: ByteBuffer?): MP4Packet? {
        if (stcoInd >= chunkOffsets.size) return null
        val frameSize = frameSize
        val se = sampleToChunks[stscInd]!!.entry
        val chSize = sampleToChunks[stscInd]!!.count * frameSize
        val pktOff = chunkOffsets[stcoInd] + posShift
        val pktSize = chSize - posShift
        val result = readPacketData(input, buffer!!, pktOff, pktSize)
        val ptsRem = pts
        val doneFrames = pktSize / frameSize
        shiftPts(doneFrames.toLong())
        val pkt = MP4Packet(result, QTTimeUtil.mediaToEdited(box, ptsRem, movie.timescale), timescale,
                (pts - ptsRem).toInt().toLong(), _curFrame, FrameType.KEY, null, 0, ptsRem, se - 1, pktOff, pktSize, true)
        _curFrame += doneFrames.toLong()
        posShift = 0
        ++stcoInd
        if (stscInd < sampleToChunks.size - 1 && (stcoInd + 1).toLong() == sampleToChunks[stscInd + 1]!!.first) stscInd++
        return pkt
    }

    override fun gotoSyncFrame(frameNo: Long): Boolean {
        return gotoFrame(frameNo)
    }

    val frameSize: Int
        get() {
            val entry = sampleEntries!![sampleToChunks[stscInd]!!.entry - 1]!!
            return if (entry is AudioSampleEntry && defaultSampleSize == 0) {
                entry.calcFrameSize()
            } else {
                defaultSampleSize
            }
        }

    override fun seekPointer(frameNo: Long) {
        stcoInd = 0
        stscInd = 0
        _curFrame = 0
        while (true) {
            val nextFrame = _curFrame + sampleToChunks[stscInd]!!.count
            if (nextFrame > frameNo) break
            _curFrame = nextFrame
            nextChunk()
        }
        posShift = ((frameNo - _curFrame) * frameSize).toInt()
        _curFrame = frameNo
    }

    override val frameCount: Long
        get() = totalFrames.toLong()

    override fun getMeta(): DemuxerTrackMeta? {
        return if (sampleEntries!![0] is AudioSampleEntry) {
            val ase = sampleEntries!![0] as AudioSampleEntry
            val audioCodecMeta = AudioCodecMeta.fromAudioFormat(ase.format)
            DemuxerTrackMeta(TrackType.AUDIO, Codec.codecByFourcc(fourcc), duration.toDouble() / timescale, null, totalFrames,
                    null, null, audioCodecMeta)
        } else {
            println("stan")
            null
        }
    }

    init {
        val stsz = findFirstPath(trak, path("mdia.minf.stbl.stsz")) as SampleSizesBox?
        defaultSampleSize = stsz!!.defaultSize
        var chunks = 0
        for (i in 1 until sampleToChunks.size) {
            val ch = (sampleToChunks[i]!!.first - sampleToChunks[i - 1]!!.first).toInt()
            totalFrames += ch * sampleToChunks[i - 1]!!.count
            chunks += ch
        }
        totalFrames += sampleToChunks[sampleToChunks.size - 1]!!.count * (chunkOffsets.size - chunks)
    }
}