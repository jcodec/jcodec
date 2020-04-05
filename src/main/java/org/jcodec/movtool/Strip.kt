package org.jcodec.movtool

import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.model.RationalLarge
import org.jcodec.containers.mp4.Chunk
import org.jcodec.containers.mp4.ChunkReader
import org.jcodec.containers.mp4.MP4Util.createRefFullMovie
import org.jcodec.containers.mp4.MP4Util.writeFullMovie
import org.jcodec.containers.mp4.boxes.*
import org.jcodec.containers.mp4.boxes.Box.Companion.path
import org.jcodec.containers.mp4.boxes.ChunkOffsets64Box.Companion.createChunkOffsets64Box
import org.jcodec.containers.mp4.boxes.ChunkOffsetsBox.Companion.createChunkOffsetsBox
import org.jcodec.containers.mp4.boxes.Edit.Companion.createEdit
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirstPath
import org.jcodec.containers.mp4.boxes.SampleSizesBox.Companion.createSampleSizesBox
import org.jcodec.containers.mp4.boxes.SampleSizesBox.Companion.createSampleSizesBox2
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.Companion.createSampleToChunkBox
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.SampleToChunkEntry
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.Companion.createTimeToSampleBox
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry
import org.jcodec.platform.Platform
import java.io.File
import java.io.IOException
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Strips movie to editlist
 *
 * @author The JCodec project
 */
class Strip {
    @Throws(IOException::class)
    fun strip(movie: MovieBox) {
        var maxDuration = RationalLarge.ZERO
        val tracks = movie.tracks
        for (i in tracks.indices) {
            val track = tracks[i]
            val duration = stripTrack(movie, track)
            if (duration.greaterThen(maxDuration)) maxDuration = duration
        }
        movie.duration = movie.rescale(maxDuration.num, maxDuration.den)
    }

    @Throws(IOException::class)
    fun trim(movie: MovieBox, type: String?) {
        var maxDuration = RationalLarge.ZERO
        val tracks = movie.tracks
        for (i in tracks.indices) {
            val track = tracks[i]
            var duration: RationalLarge
            duration = if (type == null || type == track.handlerType) {
                trimEdits(movie, track)
            } else {
                RationalLarge.R(track.duration, movie.timescale.toLong())
            }
            if (duration.greaterThen(maxDuration)) maxDuration = duration
        }
        movie.duration = movie.rescale(maxDuration.num, maxDuration.den)
    }

    @Throws(IOException::class)
    fun stripTrack(movie: MovieBox, track: TrakBox): RationalLarge {
        val chunks = ChunkReader(track, null)
        val edits = track.edits
        val oldEdits = deepCopy(edits)
        val result: MutableList<Chunk?> = ArrayList()
        var chunk: Chunk
        while (chunks.next().also { chunk = it!! } != null) {
            var intersects = false
            for (edit in oldEdits) {
                if (edit.mediaTime == -1L) continue  // track offset, not real edit
                val editS = edit.mediaTime
                val editE = edit.mediaTime + track.rescale(edit.duration, movie.timescale.toLong())
                val chunkS = chunk.startTv
                val chunkE = chunk.startTv + chunk.duration
                intersects = intersects(editS, editE, chunkS, chunkE)
                if (intersects) break
            }
            if (!intersects) {
                for (i in oldEdits.indices) {
                    if (oldEdits[i].mediaTime >= chunk.startTv + chunk.duration) edits!![i].shift(-chunk.duration.toLong())
                }
            } else result.add(chunk)
        }
        val stbl = findFirstPath(track, path("mdia.minf.stbl")) as NodeBox?
        stbl!!.replace("stts", getTimeToSamples(result))
        stbl.replace("stsz", getSampleSizes(result))
        stbl.replace("stsc", getSamplesToChunk(result))
        stbl.removeChildren(arrayOf("stco", "co64"))
        stbl.add(getChunkOffsets(result))
        val duration = totalDuration(result)
        val mdhd = findFirstPath(track, path("mdia.mdhd")) as MediaHeaderBox?
        mdhd!!.setDuration(duration)
        track.duration = movie.rescale(duration, mdhd.getTimescale().toLong())
        return RationalLarge(duration, mdhd.getTimescale().toLong())
    }

    @Throws(IOException::class)
    fun trimEdits(movie: MovieBox, track: TrakBox): RationalLarge {
        val chunks = ChunkReader(track, null)
        val edits = track.edits
        val result: MutableList<Chunk?> = ArrayList()
        for (edit in edits!!) {
            val editS = edit.mediaTime
            val editE = edit.mediaTime + track.rescale(edit.duration, movie.timescale.toLong())
            var chunk: Chunk? = null
            while (chunks.next().also { chunk = it } != null) {
                result.add(chunk)
                val chunkS = chunk!!.startTv
                val chunkE = chunk!!.startTv + chunk!!.duration
                if (editS > chunkS) {
                    val cutDur = editS - chunkS
                    edit.shift(-cutDur)
                    chunk!!.trimFront(cutDur)
                }
                if (editE < chunkE) {
                    val cutDur = chunkE - editE
                    edit.stretch(-movie.rescale(cutDur, track.timescale.toLong()))
                    chunk!!.trimTail(cutDur)
                }
                if (editE <= chunkE) break
            }
        }
        val stbl = findFirstPath(track, path("mdia.minf.stbl")) as NodeBox?
        stbl!!.replace("stts", getTimeToSamples(result))
        stbl.replace("stsz", getSampleSizes(result))
        stbl.replace("stsc", getSamplesToChunk(result))
        stbl.removeChildren(arrayOf("stco", "co64"))
        stbl.add(getChunkOffsets(result))
        val duration = totalDuration(result)
        val mdhd = findFirstPath(track, path("mdia.mdhd")) as MediaHeaderBox?
        mdhd!!.setDuration(duration)
        track.duration = movie.rescale(duration, mdhd.getTimescale().toLong())
        return RationalLarge(duration, mdhd.getTimescale().toLong())
    }

    private fun totalDuration(result: List<Chunk?>): Long {
        var duration: Long = 0
        for (chunk in result) {
            duration += chunk!!.duration.toLong()
        }
        return duration
    }

    private fun deepCopy(edits: List<Edit>?): List<Edit> {
        val newList = ArrayList<Edit>()
        for (edit in edits!!) {
            newList.add(createEdit(edit))
        }
        return newList
    }

    fun getChunkOffsets(chunks: List<Chunk?>): Box {
        val result = LongArray(chunks.size)
        var longBox = false
        var i = 0
        for (chunk in chunks) {
            if (chunk!!.offset >= 0x100000000L) longBox = true
            result[i++] = chunk.offset
        }
        return if (longBox) createChunkOffsets64Box(result) else createChunkOffsetsBox(result)
    }

    fun getTimeToSamples(chunks: List<Chunk?>): TimeToSampleBox {
        val tts = ArrayList<TimeToSampleEntry>()
        var curTts = -1
        var cnt = 0
        for (chunk in chunks) {
            if (chunk!!.sampleDur != Chunk.UNEQUAL_DUR) {
                if (curTts == -1 || curTts != chunk.sampleDur) {
                    if (curTts != -1) tts.add(TimeToSampleEntry(cnt, curTts))
                    cnt = 0
                    curTts = chunk.sampleDur
                }
                cnt += chunk.sampleCount
            } else {
                for (dur in chunk.sampleDurs!!) {
                    if (curTts == -1 || curTts != dur) {
                        if (curTts != -1) tts.add(TimeToSampleEntry(cnt, curTts))
                        cnt = 0
                        curTts = dur
                    }
                    ++cnt
                }
            }
        }
        if (cnt > 0) tts.add(TimeToSampleEntry(cnt, curTts))
        return createTimeToSampleBox(tts.toTypedArray())
    }

    fun getSampleSizes(chunks: List<Chunk?>): SampleSizesBox {
        var nSamples = 0
        val prevSize = if (chunks.size != 0) chunks[0]!!.sampleSize else 0
        for (chunk in chunks) {
            nSamples += chunk!!.sampleCount
            if (prevSize == 0 && chunk.sampleSize != 0) throw RuntimeException("Mixed sample sizes not supported")
        }
        if (prevSize > 0) return createSampleSizesBox(prevSize, nSamples)
        val sizes = IntArray(nSamples)
        var startSample = 0
        for (chunk in chunks) {
            System.arraycopy(chunk!!.sampleSizes, 0, sizes, startSample, chunk.sampleCount)
            startSample += chunk.sampleCount
        }
        return createSampleSizesBox2(sizes)
    }

    fun getSamplesToChunk(chunks: List<Chunk?>): SampleToChunkBox {
        val result = ArrayList<SampleToChunkEntry>()
        val it = chunks.iterator()
        if (it.hasNext()) {
            var chunk = it.next()
            var curSz = chunk!!.sampleCount
            var curEntry = chunk.entry
            var first = 1
            var cnt = 1
            while (it.hasNext()) {
                chunk = it.next()
                val newSz = chunk!!.sampleCount
                val newEntry = chunk.entry
                if (curSz != newSz || curEntry != newEntry) {
                    result.add(SampleToChunkEntry(first.toLong(), curSz, curEntry))
                    curSz = newSz
                    curEntry = newEntry
                    first += cnt
                    cnt = 0
                }
                ++cnt
            }
            result.add(SampleToChunkEntry(first.toLong(), curSz, curEntry))
        }
        return createSampleToChunkBox(result.toTypedArray())
    }

    private fun intersects(a: Long, b: Long, c: Long, d: Long): Boolean {
        return a >= c && a < d || b >= c && b < d || c >= a && c < b || d >= a && d < b
    }

    companion object {
        @Throws(Exception::class)
        fun main1(args: Array<String?>) {
            if (args.size < 2) {
                println("Syntax: strip <ref movie> <out movie>")
                System.exit(-1)
            }
            var input: SeekableByteChannel? = null
            var out: SeekableByteChannel? = null
            try {
                input = NIOUtils.readableChannel(File(args[0]))
                val file = File(args[1])
                Platform.deleteFile(file)
                out = NIOUtils.writableChannel(file)
                val movie = createRefFullMovie(input, "file://" + File(args[0]).absolutePath)
                Strip().strip(movie!!.moov)
                writeFullMovie(out, movie)
            } finally {
                input?.close()
                out?.close()
            }
        }
    }
}