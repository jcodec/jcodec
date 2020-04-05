package org.jcodec.containers.mp4

import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.containers.mp4.boxes.*
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.SampleToChunkEntry
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry
import org.jcodec.platform.Platform
import java.io.IOException

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class ChunkReader(trakBox: TrakBox, inputs: Array<SeekableByteChannel>?) {
    private var curChunk = 0
    private var sampleNo = 0
    private var s2cIndex = 0
    private var ttsInd = 0
    private var ttsSubInd = 0
    private var chunkTv: Long = 0
    private val chunkOffsets: LongArray
    private val sampleToChunk: Array<SampleToChunkEntry?>
    private val stsz: SampleSizesBox
    private val tts: Array<TimeToSampleEntry?>
    private val stsd: SampleDescriptionBox
    private val inputs: Array<SeekableByteChannel>?
    private val entries: Array<SampleEntry>
    operator fun hasNext(): Boolean {
        return curChunk < chunkOffsets.size
    }

    @Throws(IOException::class)
    operator fun next(): Chunk? {
        if (curChunk >= chunkOffsets.size) return null
        if (s2cIndex + 1 < sampleToChunk.size && curChunk + 1.toLong() == sampleToChunk[s2cIndex + 1]!!.first) s2cIndex++
        val sampleCount = sampleToChunk[s2cIndex]!!.count
        var samplesDur: IntArray? = null
        var sampleDur = Chunk.UNEQUAL_DUR
        if (ttsSubInd + sampleCount <= tts[ttsInd]!!.sampleCount) {
            sampleDur = tts[ttsInd]!!.sampleDuration
            ttsSubInd += sampleCount
        } else {
            samplesDur = IntArray(sampleCount)
            for (i in 0 until sampleCount) {
                if (ttsSubInd >= tts[ttsInd]!!.sampleCount && ttsInd < tts.size - 1) {
                    ttsSubInd = 0
                    ++ttsInd
                }
                samplesDur[i] = tts[ttsInd]!!.sampleDuration
                ++ttsSubInd
            }
        }
        var size = Chunk.UNEQUAL_SIZES
        var sizes: IntArray? = null
        if (stsz.defaultSize > 0) {
            size = frameSize
        } else {
            sizes = Platform.copyOfRangeI(stsz.getSizes(), sampleNo, sampleNo + sampleCount)
        }
        val dref = sampleToChunk[s2cIndex]!!.entry
        val chunk = Chunk(chunkOffsets[curChunk], chunkTv, sampleCount, size, sizes!!, sampleDur, samplesDur, dref)
        chunkTv += chunk.duration.toLong()
        sampleNo += sampleCount
        ++curChunk
        if (inputs != null) {
            val input = getInput(chunk)
            input.setPosition(chunk.offset)
            chunk.data = NIOUtils.fetchFromChannel(input, chunk.size.toInt())
        }
        return chunk
    }

    private fun getInput(chunk: Chunk): SeekableByteChannel {
        val se = entries[chunk.entry - 1]
        return inputs!![se.drefInd - 1]
    }

    private val frameSize: Int
        private get() {
            val size = stsz.defaultSize
            val box = stsd.getBoxes()[sampleToChunk[s2cIndex]!!.entry - 1]
            return if (box is AudioSampleEntry) {
                box.calcFrameSize()
            } else size
        }

    fun size(): Int {
        return chunkOffsets.size
    }

    init {
        val stts = trakBox.stts
        tts = stts.getEntries()
        val stco = trakBox.stco
        val co64 = trakBox.co64
        stsz = trakBox.stsz
        val stsc = trakBox.stsc
        chunkOffsets = stco?.getChunkOffsets() ?: co64!!.getChunkOffsets()
        sampleToChunk = stsc.getSampleToChunk()
        stsd = trakBox.stsd
        entries = trakBox.sampleEntries
        this.inputs = inputs
    }
}