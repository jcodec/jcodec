package org.jcodec.movtool

import org.jcodec.common.IntArrayList
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.containers.mp4.ChunkReader
import org.jcodec.containers.mp4.boxes.Box
import org.jcodec.containers.mp4.boxes.Edit
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.Companion.createTimeToSampleBox
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry
import org.jcodec.containers.mp4.boxes.TrakBox
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * A hacky way to fix timestamps without rewriting the file
 *
 * @author The JCodec project
 */
abstract class FixTimestamp {
    @Throws(IOException::class)
    fun fixTimestamp(trakBox: TrakBox, ch: SeekableByteChannel) {
        val inputs = arrayOf(ch)
        val chr = ChunkReader(trakBox, inputs)
        var prevPts: Long = -1
        var oldPts: Long = 0
        var oldDur = -1
        var editStart: Long = 0
        var totalDur: Long = 0
        val durations = IntArrayList.createIntArrayList()
        while (chr.hasNext()) {
            val next = chr.next()
            val data = next!!.data!!.duplicate()
            val sampleSizes = next.sampleSizes
            val sampleDurs = next.sampleDurs
            for (i in sampleSizes.indices) {
                val sz = sampleSizes[i]
                oldDur = sampleDurs?.get(i) ?: next.sampleDur
                val sampleData = NIOUtils.read(data, sz)
                val pts = (getPts(sampleData, oldPts.toDouble(), trakBox) * trakBox.timescale).toLong()
                totalDur = pts
                println("old: $oldPts, new: $pts")
                oldPts += oldDur.toLong()
                if (prevPts != -1L && pts >= prevPts) {
                    val dur = pts - prevPts
                    durations.add(dur.toInt())
                    prevPts = pts
                } else if (prevPts == -1L) {
                    prevPts = pts
                    editStart = pts
                }
            }
        }
        if (oldDur != -1) {
            durations.add(oldDur)
            totalDur += oldDur.toLong()
        }
        trakBox.stbl.replaceBox(createStts(durations))
        if (editStart != 0L) {
            val edits = ArrayList<Edit>()
            edits.add(Edit(-editStart, totalDur - editStart, 1f))
            trakBox.edits = edits
        }
    }

    private fun createStts(durations: IntArrayList): Box {
        val entries = arrayOfNulls<TimeToSampleEntry>(durations.size())
        for (i in 0 until durations.size()) entries[i] = TimeToSampleEntry(1, durations[i])
        return createTimeToSampleBox(entries)
    }

    @Throws(IOException::class)
    protected abstract fun getPts(sampleData: ByteBuffer?, orig: Double, trakBox: TrakBox?): Double
}