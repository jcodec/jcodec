package org.jcodec.containers.mp4

import org.jcodec.common.IntArrayList
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.containers.mp4.boxes.AliasBox.Companion.createSelfRef
import org.jcodec.containers.mp4.boxes.Box.Companion.path
import org.jcodec.containers.mp4.boxes.ChunkOffsets64Box.Companion.createChunkOffsets64Box
import org.jcodec.containers.mp4.boxes.DataInfoBox.Companion.createDataInfoBox
import org.jcodec.containers.mp4.boxes.DataRefBox.Companion.createDataRefBox
import org.jcodec.containers.mp4.boxes.NodeBox
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirstPath
import org.jcodec.containers.mp4.boxes.SampleEntry
import org.jcodec.containers.mp4.boxes.SampleSizesBox.Companion.createSampleSizesBox
import org.jcodec.containers.mp4.boxes.SampleSizesBox.Companion.createSampleSizesBox2
import org.jcodec.containers.mp4.boxes.TrakBox
import java.io.IOException

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class ChunkWriter(trak: TrakBox, inputs: Array<SeekableByteChannel>, out: SeekableByteChannel) {
    private val offsets: LongArray
    private val entries: Array<SampleEntry>
    private val inputs: Array<SeekableByteChannel>
    private var curChunk = 0
    private val out: SeekableByteChannel
    var buf: ByteArray
    private val trak: TrakBox
    private val sampleSizes: IntArrayList
    private var sampleSize = 0
    private var sampleCount = 0
    fun apply() {
        val stbl = findFirstPath(trak, path("mdia.minf.stbl")) as NodeBox?
        stbl!!.removeChildren(arrayOf("stco", "co64"))
        stbl.add(createChunkOffsets64Box(offsets))
        cleanDrefs(trak)
        val stsz = if (sampleCount != 0) createSampleSizesBox(sampleSize, sampleCount) else createSampleSizesBox2(sampleSizes.toArray())
        stbl.replaceBox(stsz)
    }

    private fun cleanDrefs(trak: TrakBox) {
        val minf = trak.mdia.minf
        var dinf = trak.mdia.minf.dinf
        if (dinf == null) {
            dinf = createDataInfoBox()
            minf.add(dinf)
        }
        var dref = dinf.dref
        if (dref == null) {
            dref = createDataRefBox()
            dinf.add(dref)
        }
        dref.getBoxes().clear()
        dref.add(createSelfRef())
        val sampleEntries = trak.sampleEntries
        for (i in sampleEntries.indices) {
            val entry = sampleEntries[i]
            entry.drefInd = 1.toShort()
        }
    }

    private fun getInput(chunk: Chunk): SeekableByteChannel {
        val se = entries[chunk.entry - 1]
        return inputs[se.drefInd - 1]
    }

    @Throws(IOException::class)
    fun write(chunk: Chunk) {
        val pos = out.position()
        var chunkData = chunk.data
        if (chunkData == null) {
            val input = getInput(chunk)
            input.setPosition(chunk.offset)
            chunkData = NIOUtils.fetchFromChannel(input, chunk.size.toInt())
        }
        out.write(chunkData)
        offsets[curChunk++] = pos
        if (chunk.sampleSize == Chunk.UNEQUAL_SIZES) {
            if (sampleCount != 0) throw RuntimeException("Mixed chunks unsupported 1.")
            sampleSizes.addAll(chunk.sampleSizes)
        } else {
            if (sampleSizes.size() != 0) throw RuntimeException("Mixed chunks unsupported 2.")
            if (sampleCount == 0) {
                sampleSize = chunk.sampleSize
            } else if (sampleSize != chunk.sampleSize) {
                throw RuntimeException("Mismatching default sizes")
            }
            sampleCount += chunk.sampleCount
        }
    }

    init {
        buf = ByteArray(8092)
        entries = trak.sampleEntries
        val stco = trak.stco
        val co64 = trak.co64
        val size: Int
        if (stco != null) size = stco.getChunkOffsets().size else size = co64!!.getChunkOffsets().size
        this.inputs = inputs
        offsets = LongArray(size)
        this.out = out
        this.trak = trak
        sampleSizes = IntArrayList.createIntArrayList()
    }
}