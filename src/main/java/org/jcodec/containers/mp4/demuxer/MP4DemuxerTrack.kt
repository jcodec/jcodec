package org.jcodec.containers.mp4.demuxer

import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.model.Packet.FrameType
import org.jcodec.containers.mp4.MP4Packet
import org.jcodec.containers.mp4.QTTimeUtil
import org.jcodec.containers.mp4.boxes.*
import org.jcodec.containers.mp4.boxes.Box.Companion.path
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirstPath
import java.io.IOException
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Regular MP4 track containing frames
 *
 * @author The JCodec project
 */
class MP4DemuxerTrack(private val movie: MovieBox, trak: TrakBox?, private val input: SeekableByteChannel) : AbstractMP4DemuxerTrack(trak!!) {
    private val sizes: IntArray
    private var offInChunk: Long = 0
    private var noInChunk = 0
    private var syncSamples: IntArray? = null
    private var partialSync: IntArray? = null
    private var ssOff = 0
    private var psOff = 0
    private val compOffsets: Array<CompositionOffsetsBox.Entry>?
    private var cttsInd = 0
    private var cttsSubInd = 0

    @Synchronized
    @Throws(IOException::class)
    override fun nextFrame(): MP4Packet? {
        if (curFrame >= sizes.size) return null
        val size = sizes[curFrame.toInt()]
        return getNextFrame(ByteBuffer.allocate(size))!!
    }

    @Synchronized
    @Throws(IOException::class)
    override fun getNextFrame(storage: ByteBuffer?): MP4Packet? {
        if (curFrame >= sizes.size) return null
        val size = sizes[curFrame.toInt()]
        require(!(storage != null && storage.remaining() < size)) { "Buffer size is not enough to fit a packet" }
        val pktPos = chunkOffsets[Math.min(chunkOffsets.size - 1, stcoInd)] + offInChunk
        val result = readPacketData(input, storage!!, pktPos, size)
        if (result != null && result.remaining() < size) return null
        val duration = timeToSamples[sttsInd]!!.sampleDuration
        val _ss = syncSamples
        var sync = _ss == null
        if (_ss != null && ssOff < _ss.size && curFrame + 1 == _ss[ssOff].toLong()) {
            sync = true
            ssOff++
        }
        var psync = false
        val _ps = partialSync
        if (_ps != null && psOff < _ps.size && curFrame + 1 == _ps[psOff].toLong()) {
            psync = true
            psOff++
        }
        var realPts = pts
        if (compOffsets != null) {
            realPts = pts + compOffsets[cttsInd].offset
            cttsSubInd++
            if (cttsInd < compOffsets.size - 1 && cttsSubInd == compOffsets[cttsInd].count) {
                cttsInd++
                cttsSubInd = 0
            }
        }
        val data = result.let { convertPacket(it) }
        val _pts = QTTimeUtil.mediaToEdited(box, realPts, movie.timescale)
        val ftype = if (sync) FrameType.KEY else FrameType.INTER
        val entryNo = sampleToChunks[stscInd]!!.entry - 1
        val pkt = MP4Packet(data, _pts, timescale, duration.toLong(), curFrame, ftype, null, 0, realPts, entryNo,
                pktPos, size, psync)
        offInChunk += size.toLong()
        _curFrame++
        noInChunk++
        if (noInChunk >= sampleToChunks[stscInd]!!.count) {
            noInChunk = 0
            offInChunk = 0
            nextChunk()
        }
        shiftPts(1)
        return pkt
    }

    override fun gotoSyncFrame(frameNo: Long): Boolean {
        val _ss = syncSamples
        if (_ss == null) return gotoFrame(frameNo)
        require(frameNo >= 0) { "negative frame number" }
        if (frameNo >= frameCount) return false
        if (frameNo == curFrame) return true
        for (i in _ss.indices) {
            if (_ss[i] - 1 > frameNo) return gotoFrame(_ss[i - 1] - 1.toLong())
        }
        return gotoFrame(_ss[_ss.size - 1] - 1.toLong())
    }

    override fun seekPointer(frameNo: Long) {
        if (compOffsets != null) {
            cttsSubInd = frameNo.toInt()
            cttsInd = 0
            while (cttsSubInd >= compOffsets[cttsInd].count) {
                cttsSubInd -= compOffsets[cttsInd].count
                cttsInd++
            }
        }
        _curFrame = frameNo.toInt().toLong()
        stcoInd = 0
        stscInd = 0
        noInChunk = frameNo.toInt()
        offInChunk = 0
        while (noInChunk >= sampleToChunks[stscInd]!!.count) {
            noInChunk -= sampleToChunks[stscInd]!!.count
            nextChunk()
        }
        for (i in 0 until noInChunk) {
            offInChunk += sizes[frameNo.toInt() - noInChunk + i]
        }
        val _ss = syncSamples
        if (_ss != null) {
            ssOff = 0
            while (ssOff < _ss.size && _ss[ssOff] < curFrame + 1) {
                ssOff++
            }
        }
        val _ps = partialSync
        if (_ps != null) {
            psOff = 0
            while (psOff < _ps.size && _ps[psOff] < curFrame + 1) {
                psOff++
            }
        }
    }

    override val frameCount: Long
        get() = sizes.size.toLong()

    init {
        val stsz = findFirstPath(trak, path("mdia.minf.stbl.stsz")) as SampleSizesBox?
        val stss = findFirstPath(trak, path("mdia.minf.stbl.stss")) as SyncSamplesBox?
        val stps = findFirstPath(trak, path("mdia.minf.stbl.stps")) as SyncSamplesBox?
        val ctts = findFirstPath(trak, path("mdia.minf.stbl.ctts")) as CompositionOffsetsBox?
        compOffsets = ctts?.entries
        if (stss != null) {
            syncSamples = stss.syncSamples
        }
        if (stps != null) {
            partialSync = stps.syncSamples
        }
        if (stsz!!.defaultSize != 0) {
            sizes = IntArray(1)
            sizes[0] = stsz.defaultSize
        } else sizes = stsz.getSizes()
    }
}