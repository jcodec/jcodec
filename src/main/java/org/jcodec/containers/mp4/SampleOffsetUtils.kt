package org.jcodec.containers.mp4

import org.jcodec.common.io.NIOUtils
import org.jcodec.containers.mp4.MP4Util.parseMovie
import org.jcodec.containers.mp4.boxes.Box.Companion.path
import org.jcodec.containers.mp4.boxes.ChunkOffsetsBox
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirstPath
import org.jcodec.containers.mp4.boxes.SampleSizesBox
import org.jcodec.containers.mp4.boxes.SampleToChunkBox
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
object SampleOffsetUtils {
    @Throws(IOException::class)
    fun getSampleData(sample: Int, file: File?): ByteBuffer {
        val moov = parseMovie(file)
        val minf = moov!!.audioTracks[0].mdia.minf
        val stco = findFirstPath(minf, path("stbl.stco")) as ChunkOffsetsBox?
        val stsc = findFirstPath(minf, path("stbl.stsc")) as SampleToChunkBox?
        val stsz = findFirstPath(minf, path("stbl.stsz")) as SampleSizesBox?
        val sampleOffset = getSampleOffset(sample, stsc, stco, stsz)
        val map = NIOUtils.mapFile(file)
        map.position(sampleOffset.toInt())
        map.limit(map.position() + stsz!!.getSizes()[sample])
        return map
    }

    fun getSampleOffset(sample: Int, stsc: SampleToChunkBox?, stco: ChunkOffsetsBox?, stsz: SampleSizesBox?): Long {
        val chunkBySample = getChunkBySample(sample, stco, stsc)
        val firstSampleAtChunk = getFirstSampleAtChunk(chunkBySample, stsc, stco)
        var offset = stco!!.getChunkOffsets()[chunkBySample - 1]
        val sizes = stsz!!.getSizes()
        for (i in firstSampleAtChunk until sample) {
            offset += sizes[i]
        }
        return offset
    }

    @JvmStatic
    fun getFirstSampleAtChunk(chunk: Int, stsc: SampleToChunkBox?, stco: ChunkOffsetsBox?): Int {
        val chunks: Int = stco!!.getChunkOffsets().size
        var samples = 0
        for (i in 1..chunks) {
            if (i == chunk) {
                break
            }
            val samplesInChunk = getSamplesInChunk(i, stsc)
            samples += samplesInChunk
        }
        return samples
    }

    @JvmStatic
    fun getChunkBySample(sampleOfInterest: Int, stco: ChunkOffsetsBox?, stsc: SampleToChunkBox?): Int {
        val chunks: Int = stco!!.getChunkOffsets().size
        var startSample = 0
        var endSample = 0
        for (i in 1..chunks) {
            val samplesInChunk = getSamplesInChunk(i, stsc)
            endSample = startSample + samplesInChunk
            if (sampleOfInterest >= startSample && sampleOfInterest < endSample) {
                return i
            }
            startSample = endSample
        }
        return -1
    }

    @JvmStatic
    fun getSamplesInChunk(chunk: Int, stsc: SampleToChunkBox?): Int {
        //TODO this is faster with binary search
        val sampleToChunk = stsc!!.getSampleToChunk()
        var sampleCount = 0
        for (sampleToChunkEntry in sampleToChunk) {
            if (sampleToChunkEntry!!.first > chunk) {
                return sampleCount
            }
            sampleCount = sampleToChunkEntry.count
        }
        return sampleCount
    }
}