package org.jcodec.movtool

import org.jcodec.common.ArrayUtil
import org.jcodec.common.model.Rational
import org.jcodec.containers.mp4.boxes.*
import org.jcodec.containers.mp4.boxes.Box.Companion.path
import org.jcodec.containers.mp4.boxes.ChunkOffsets64Box.Companion.createChunkOffsets64Box
import org.jcodec.containers.mp4.boxes.ChunkOffsetsBox.Companion.createChunkOffsetsBox
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirst
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirstPath
import org.jcodec.containers.mp4.boxes.SampleDescriptionBox.Companion.createSampleDescriptionBox
import org.jcodec.containers.mp4.boxes.SampleSizesBox.Companion.createSampleSizesBox
import org.jcodec.containers.mp4.boxes.SampleSizesBox.Companion.createSampleSizesBox2
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.Companion.createSampleToChunkBox
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.SampleToChunkEntry
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.Companion.createTimeToSampleBox
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Cut on ref movies
 *
 * @author The JCodec project
 */
object Util {
    fun splitEdits(edits: List<Edit>?, trackByMv: Rational, tvMv: Long): Pair<List<Edit>> {
        var total: Long = 0
        val l: MutableList<Edit> = ArrayList()
        val r: MutableList<Edit> = ArrayList()
        val lit = edits!!.toMutableList().listIterator()
        while (lit.hasNext()) {
            val edit = lit.next()
            if (total + edit.duration > tvMv) {
                val leftDurMV = (tvMv - total).toInt()
                val leftDurMedia = trackByMv.multiplyS(leftDurMV)
                val left = Edit(leftDurMV.toLong(), edit.mediaTime, 1.0f)
                val right = Edit(edit.duration - leftDurMV, leftDurMedia + edit.mediaTime, 1.0f)
                lit.remove()
                if (left.duration > 0) {
                    lit.add(left)
                    l.add(left)
                }
                if (right.duration > 0) {
                    lit.add(right)
                    r.add(right)
                }
                break
            } else {
                l.add(edit)
            }
            total += edit.duration
        }
        while (lit.hasNext()) {
            r.add(lit.next())
        }
        return Pair(l, r)
    }

    /**
     * Splits track on the timevalue specified
     *
     * @param movie
     * @param track
     * @param tvMv
     * @return
     */
    fun split(movie: MovieBox, track: TrakBox, tvMv: Long): Pair<List<Edit>> {
        return splitEdits(track.edits, Rational(track.timescale, movie.timescale), tvMv)
    }

    fun spread(movie: MovieBox, track: TrakBox, tvMv: Long, durationMv: Long) {
        val split = split(movie, track, tvMv)
        track.edits!!.add(split.a.size, Edit(durationMv, -1, 1.0f))
    }

    fun shift(movie: MovieBox?, track: TrakBox, tvMv: Long) {
        track.edits!!.add(0, Edit(tvMv, -1, 1.0f))
    }

    fun getTimevalues(track: TrakBox): LongArray {
        val stts = track.stts
        var count = 0
        val tts = stts.getEntries()
        for (i in tts.indices) count += tts[i]!!.sampleCount
        val tv = LongArray(count + 1)
        var k = 0
        for (i in tts.indices) {
            var j = 0
            while (j < tts[i]!!.sampleCount) {
                tv[k + 1] = tv[k] + tts[i]!!.sampleDuration
                j++
                k++
            }
        }
        return tv
    }

    private fun appendToInternal(movie: MovieBox, dest: TrakBox, src: TrakBox) {
        val off = appendEntries(dest, src)
        appendChunkOffsets(dest, src)
        appendTimeToSamples(dest, src)
        appendSampleToChunk(dest, src, off)
        appendSampleSizes(dest, src)
    }

    private fun updateDuration(dest: TrakBox, src: TrakBox) {
        val mdhd1 = findFirstPath(dest, path("mdia.mdhd")) as MediaHeaderBox?
        val mdhd2 = findFirstPath(src, path("mdia.mdhd")) as MediaHeaderBox?
        mdhd1!!.setDuration(mdhd1.getDuration() + mdhd2!!.getDuration())
    }

    fun appendTo(movie: MovieBox, dest: TrakBox, src: TrakBox) {
        appendToInternal(movie, dest, src)
        appendEdits(dest, src, dest.edits!!.size)
        updateDuration(dest, src)
    }

    fun insertTo(movie: MovieBox, dest: TrakBox, src: TrakBox, tvMv: Long) {
        appendToInternal(movie, dest, src)
        insertEdits(movie, dest, src, tvMv)
        updateDuration(dest, src)
    }

    private fun insertEdits(movie: MovieBox, dest: TrakBox, src: TrakBox, tvMv: Long) {
        val split = split(movie, dest, tvMv)
        appendEdits(dest, src, split.a.size)
    }

    private fun appendEdits(dest: TrakBox, src: TrakBox, ind: Int) {
        for (edit in src.edits!!) {
            edit.shift(dest.mediaDuration)
        }
        dest.edits!!.addAll(ind, src.edits!!)
        dest.edits = dest.edits
    }

    private fun appendSampleSizes(trakBox1: TrakBox, trakBox2: TrakBox) {
        val stsz1 = trakBox1.stsz
        val stsz2 = trakBox2.stsz
        require(stsz1.defaultSize == stsz2.defaultSize) { "Can't append to track that has different default sample size" }
        val stszr: SampleSizesBox
        stszr = if (stsz1.defaultSize > 0) {
            createSampleSizesBox(stsz1.defaultSize, stsz1.getCount() + stsz2.getCount())
        } else {
            createSampleSizesBox2(ArrayUtil.addAllInt(stsz1.getSizes(), stsz2.getSizes()))
        }
        (findFirstPath(trakBox1, path("mdia.minf.stbl")) as NodeBox?)!!.replace("stsz", stszr)
    }

    private fun appendSampleToChunk(trakBox1: TrakBox, trakBox2: TrakBox, off: Int) {
        val stsc1 = trakBox1.stsc
        val stsc2 = trakBox2.stsc
        val orig = stsc2.getSampleToChunk()
        val shifted = arrayOfNulls<SampleToChunkEntry>(orig.size)
        for (i in orig.indices) {
            shifted[i] = SampleToChunkEntry(orig[i]!!.first + stsc1.getSampleToChunk().size,
                    orig[i]!!.count, orig[i]!!.entry + off)
        }
        val n = findFirstPath(trakBox1, path("mdia.minf.stbl")) as NodeBox?
        n!!.replace("stsc",
                createSampleToChunkBox((ArrayUtil.addAllObj(stsc1.getSampleToChunk(), shifted) as Array<SampleToChunkEntry?>)))
    }

    private fun appendEntries(trakBox1: TrakBox, trakBox2: TrakBox): Int {
        appendDrefs(trakBox1, trakBox2)
        val ent1: Array<SampleEntry> = trakBox1.sampleEntries
        val ent2 = trakBox2.sampleEntries
        val stsd = createSampleDescriptionBox(ent1)
        for (i in ent2.indices) {
            val se = ent2[i]
            se.drefInd = (se.drefInd + ent1.size).toShort()
            stsd.add(se)
        }
        val n = findFirstPath(trakBox1, path("mdia.minf.stbl")) as NodeBox?
        n!!.replace("stsd", stsd)
        return ent1.size
    }

    private fun appendDrefs(trakBox1: TrakBox, trakBox2: TrakBox) {
        val dref1 = findFirstPath(trakBox1, path("mdia.minf.dinf.dref")) as DataRefBox?
        val dref2 = findFirstPath(trakBox2, path("mdia.minf.dinf.dref")) as DataRefBox?
        dref1!!.getBoxes().addAll(dref2!!.getBoxes())
    }

    private fun appendTimeToSamples(trakBox1: TrakBox, trakBox2: TrakBox) {
        val stts1 = trakBox1.stts
        val stts2 = trakBox2.stts
        val sttsNew = createTimeToSampleBox((ArrayUtil.addAllObj(stts1.getEntries(),
                stts2.getEntries()) as Array<TimeToSampleEntry?>))
        val n = findFirstPath(trakBox1, path("mdia.minf.stbl")) as NodeBox?
        n!!.replace("stts", sttsNew)
    }

    private fun appendChunkOffsets(trakBox1: TrakBox, trakBox2: TrakBox) {
        val stco1 = trakBox1.stco
        val co641 = trakBox1.co64
        val stco2 = trakBox2.stco
        val co642 = trakBox2.co64
        val off1 = stco1?.getChunkOffsets() ?: co641!!.getChunkOffsets()
        val off2 = stco2?.getChunkOffsets() ?: co642!!.getChunkOffsets()
        val stbl1 = findFirstPath(trakBox1, path("mdia.minf.stbl")) as NodeBox?
        stbl1!!.removeChildren(arrayOf("stco", "co64"))
        stbl1.add(if (co641 == null && co642 == null) createChunkOffsetsBox(ArrayUtil.addAllLong(off1, off2)) else createChunkOffsets64Box(ArrayUtil.addAllLong(off1, off2)))
    }

    fun forceEditList(movie: MovieBox?, trakBox: TrakBox) {
        var edits = trakBox.edits
        if (edits == null || edits.size == 0) {
            val mvhd = findFirst(movie, "mvhd") as MovieHeaderBox?
            edits = ArrayList()
            trakBox.edits = edits
            edits.add(Edit(mvhd!!.getDuration().toInt().toLong(), 0, 1.0f))
            trakBox.edits = edits
        }
    }

    fun forceEditListMov(movie: MovieBox) {
        val tracks = movie.tracks
        for (i in tracks.indices) {
            val trakBox = tracks[i]
            forceEditList(movie, trakBox)
        }
    }

    /**
     * Applies edits on top of existing edits. This function will split existing edits
     * as necessary to reflect the current set of edits.
     * @param mvByTrack Movie and track timescale in one quantity (movie / track).
     * @param lower Original edits
     * @param higher New edits
     * @return Resulting edits
     */
    fun editsOnEdits(mvByTrack: Rational, lower: List<Edit>?, higher: List<Edit>): List<Edit> {
        val result: MutableList<Edit> = ArrayList()
        var next: List<Edit> = ArrayList(lower)
        for (edit in higher) {
            val startMv = mvByTrack.multiplyLong(edit.mediaTime)
            val split = splitEdits(next, mvByTrack.flip(), startMv)
            val split2 = splitEdits(split.b, mvByTrack.flip(), startMv + edit.duration)
            result.addAll(split2.a)
            next = split2.b
        }
        return result
    }

    class Pair<T>(val a: T, val b: T)
}