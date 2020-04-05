package org.jcodec.movtool

import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.containers.mp4.Chunk
import org.jcodec.containers.mp4.Chunk.Companion.createFrom
import org.jcodec.containers.mp4.ChunkReader
import org.jcodec.containers.mp4.ChunkWriter
import org.jcodec.containers.mp4.MP4Util.Movie
import org.jcodec.containers.mp4.MP4Util.parseFullMovieChannel
import org.jcodec.containers.mp4.MP4Util.writeFullMovie
import org.jcodec.containers.mp4.boxes.*
import org.jcodec.containers.mp4.boxes.Box.Companion.path
import org.jcodec.containers.mp4.boxes.Header.Companion.createHeader
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirstPath
import org.jcodec.platform.Platform
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Self contained movie creator
 *
 * @author The JCodec project
 */
class Flatten {
    interface SampleProcessor {
        @Throws(IOException::class)
        fun processSample(src: ByteBuffer?): ByteBuffer
    }

    var listeners: MutableList<ProgressListener>
    private val sampleProcessors: MutableMap<TrakBox, SampleProcessor> = HashMap()

    interface ProgressListener {
        fun trigger(progress: Int)
    }

    fun addProgressListener(listener: ProgressListener) {
        listeners.add(listener)
    }

    fun setSampleProcessor(trak: TrakBox, processor: SampleProcessor): Boolean {
        // Will not modify individual samples of tracks with equal sample sizes
        if (trak.stsz.defaultSize != 0) return false
        sampleProcessors[trak] = processor
        return true
    }

    @Throws(IOException::class)
    fun flattenChannel(movie: Movie?, out: SeekableByteChannel?) {
        val ftyp = movie!!.ftyp
        val moov = movie.moov
        require(moov.isPureRefMovie) { "movie should be reference" }
        out!!.setPosition(0)
        writeFullMovie(out, movie)
        val extraSpace = calcSpaceReq(moov)
        val buf = ByteBuffer.allocate(extraSpace)
        out.write(buf)
        val mdatOff = out.position()
        writeHeader(createHeader("mdat", 0x100000001L), out)
        val inputs = getInputs(moov)
        val tracks = moov.tracks
        val readers = arrayOfNulls<ChunkReader>(tracks.size)
        val writers = arrayOfNulls<ChunkWriter>(tracks.size)
        val head = arrayOfNulls<Chunk>(tracks.size)
        var totalChunks = 0
        var writtenChunks = 0
        var lastProgress = 0
        val off = LongArray(tracks.size)
        for (i in tracks.indices) {
            readers[i] = ChunkReader(tracks[i], inputs[i])
            totalChunks += readers[i]!!.size()
            writers[i] = ChunkWriter(tracks[i], inputs[i], out)
            head[i] = readers[i]!!.next()
            if (tracks[i].isVideo) off[i] = (2 * moov.timescale).toLong()
        }
        while (true) {
            var min = -1
            for (i in readers.indices) {
                if (head[i] == null) continue
                if (min == -1) min = i else {
                    val iTv = moov.rescale(head[i]!!.startTv, tracks[i].timescale.toLong()) + off[i]
                    val minTv = moov.rescale(head[min]!!.startTv, tracks[min].timescale.toLong()) + off[min]
                    if (iTv < minTv) min = i
                }
            }
            if (min == -1) break
            val processor = sampleProcessors[tracks[min]]
            if (processor != null) {
                val orig = head[min]
                if (orig!!.sampleSize == Chunk.UNEQUAL_SIZES) {
                    writers[min]!!.write(processChunk(processor, orig, tracks[min], moov))
                    writtenChunks++
                }
            } else {
                writers[min]!!.write(head[min]!!)
                writtenChunks++
            }
            head[min] = readers[min]!!.next()
            lastProgress = calcProgress(totalChunks, writtenChunks, lastProgress)
        }
        for (i in tracks.indices) {
            writers[i]!!.apply()
        }
        val mdatSize = out.position() - mdatOff
        out.setPosition(0)
        writeFullMovie(out, movie)
        val extra = mdatOff - out.position()
        if (extra < 0) throw RuntimeException("Not enough space to write the header")
        writeHeader(createHeader("free", extra), out)
        out.setPosition(mdatOff)
        writeHeader(createHeader("mdat", mdatSize), out)
    }

    @Throws(IOException::class)
    private fun processChunk(processor: SampleProcessor, orig: Chunk?, track: TrakBox, moov: MovieBox): Chunk {
        val src = NIOUtils.duplicate(orig!!.data)
        val sampleSizes = orig.sampleSizes
        val modSamples: MutableList<ByteBuffer> = LinkedList()
        var totalSize = 0
        for (ss in sampleSizes.indices) {
            val sample = NIOUtils.read(src, sampleSizes[ss])
            val modSample = processor.processSample(sample)
            modSamples.add(modSample)
            totalSize += modSample.remaining()
        }
        val result = ByteArray(totalSize)
        println("total size: $totalSize")
        val modSizes = IntArray(modSamples.size)
        var ss = 0
        val tmp = ByteBuffer.wrap(result)
        for (byteBuffer in modSamples) {
            modSizes[ss++] = byteBuffer.remaining()
            tmp.put(byteBuffer)
        }
        val mod = createFrom(orig)
        mod.sampleSizes = modSizes
        mod.data = ByteBuffer.wrap(result)
        return mod
    }

    @Throws(IOException::class)
    private fun writeHeader(header: Header, out: SeekableByteChannel?) {
        val bb = ByteBuffer.allocate(16)
        header.write(bb)
        bb.flip()
        out!!.write(bb)
    }

    private fun calcProgress(totalChunks: Int, writtenChunks: Int, lastProgress: Int): Int {
        var lastProgress = lastProgress
        val curProgress = 100 * writtenChunks / totalChunks
        if (lastProgress < curProgress) {
            lastProgress = curProgress
            for (pl in listeners) pl.trigger(lastProgress)
        }
        return lastProgress
    }

    @Throws(IOException::class)
    protected fun getInputs(movie: MovieBox): Array<Array<SeekableByteChannel>> {
        val tracks = movie.tracks
        return tracks.map { track ->
            val drefs = findFirstPath(track, path("mdia.minf.dinf.dref")) as DataRefBox?
                    ?: throw RuntimeException("No data references")
            val entries: List<Box> = drefs.getBoxes()
            val inputs = entries.map { resolveDataRef(it) }.toTypedArray()
            inputs
        }.toTypedArray()
    }

    private fun calcSpaceReq(movie: MovieBox): Int {
        var sum = 0
        val tracks = movie.tracks
        for (i in tracks.indices) {
            val trakBox = tracks[i]
            val stco = trakBox.stco
            if (stco != null) sum += stco.getChunkOffsets().size * 4
        }
        return sum
    }

    @Throws(IOException::class)
    fun resolveDataRef(box: Box): SeekableByteChannel {
        return if (box is UrlBox) {
            val url = box.url
            if (!url!!.startsWith("file://")) throw RuntimeException("Only file:// urls are supported in data reference")
            NIOUtils.readableChannel(File(url.substring(7)))
        } else if (box is AliasBox) {
            val uxPath = box.unixPath ?: throw RuntimeException("Could not resolve alias")
            NIOUtils.readableChannel(File(uxPath))
        } else {
            throw RuntimeException(box.header.fourcc + " dataref type is not supported")
        }
    }

    @Throws(IOException::class)
    fun flatten(movie: Movie?, video: File?) {
        Platform.deleteFile(video)
        var out: SeekableByteChannel? = null
        try {
            out = NIOUtils.writableChannel(video)
            flattenChannel(movie, out)
        } finally {
            out?.close()
        }
    }

    companion object {
        @Throws(Exception::class)
        fun main1(args: Array<String?>) {
            if (args.size < 2) {
                println("Syntax: self <ref movie> <out movie>")
                System.exit(-1)
            }
            val outFile = File(args[1])
            Platform.deleteFile(outFile)
            var input: SeekableByteChannel? = null
            try {
                input = NIOUtils.readableChannel(File(args[0]))
                val movie = parseFullMovieChannel(input)
                Flatten().flatten(movie, outFile)
            } finally {
                input?.close()
            }
        }
    }

    init {
        listeners = ArrayList()
    }
}