package org.jcodec.containers.mp4.demuxer

import org.jcodec.common.IntArrayList
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.model.TapeTimecode
import org.jcodec.containers.mp4.MP4Packet
import org.jcodec.containers.mp4.QTTimeUtil
import org.jcodec.containers.mp4.boxes.*
import org.jcodec.containers.mp4.boxes.Box.Companion.path
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirst
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirstPath
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.SampleToChunkEntry
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry
import java.io.IOException
import java.util.regex.Pattern

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Timecode track, provides timecode information for video track
 *
 * @author The JCodec project
 */
class TimecodeMP4DemuxerTrack(private val movie: MovieBox, val box: TrakBox, private val input: SeekableByteChannel) {
    private val timeToSamples: Array<TimeToSampleEntry?>
    private var sampleCache: IntArray? = null
    private val tse: TimecodeSampleEntry
    private val chunkOffsets: LongArray
    private val sampleToChunks: Array<SampleToChunkEntry?>

    @Throws(IOException::class)
    fun getTimecode(pkt: MP4Packet): MP4Packet {
        var tv = QTTimeUtil.editedToMedia(box, box.rescale(pkt.getPts(), pkt.getTimescale().toLong()), movie.timescale)
        var sample: Int
        var ttsInd = 0
        var ttsSubInd = 0
        sample = 0
        while (sample < sampleCache!!.size - 1) {
            val dur = timeToSamples[ttsInd]!!.sampleDuration
            if (tv < dur) break
            tv -= dur.toLong()
            ttsSubInd++
            if (ttsInd < timeToSamples.size - 1 && ttsSubInd >= timeToSamples[ttsInd]!!.sampleCount) ttsInd++
            sample++
        }
        val frameNo = (2 * tv * tse.timescale / box.timescale / tse.frameDuration + 1).toInt() / 2
        return MP4Packet.createMP4PacketWithTimecode(pkt, _getTimecode(getTimecodeSample(sample), frameNo, tse))
    }

    @Throws(IOException::class)
    private fun getTimecodeSample(sample: Int): Int {
        return if (sampleCache != null) sampleCache!![sample] else {
            synchronized(input) {
                var stscInd: Int
                var stscSubInd: Int
                stscInd = 0
                stscSubInd = sample
                while (stscInd < sampleToChunks.size
                        && stscSubInd >= sampleToChunks[stscInd]!!.count) {
                    stscSubInd -= sampleToChunks[stscInd]!!.count
                    stscInd++
                }
                val offset = (chunkOffsets[stscInd]
                        + (Math.min(stscSubInd, sampleToChunks[stscInd]!!.count - 1) shl 2))
                if (input.position() != offset) input.setPosition(offset)
                val buf = NIOUtils.fetchFromChannel(input, 4)
                return buf.int
            }
        }
    }

    @Throws(IOException::class)
    private fun cacheSamples(sampleToChunks: Array<SampleToChunkEntry?>, chunkOffsets: LongArray) {
        synchronized(input) {
            var stscInd = 0
            val ss = IntArrayList.createIntArrayList()
            for (chunkNo in chunkOffsets.indices) {
                val nSamples = sampleToChunks[stscInd]!!.count
                if (stscInd < sampleToChunks.size - 1 && chunkNo + 1 >= sampleToChunks[stscInd + 1]!!.first) stscInd++
                val offset = chunkOffsets[chunkNo]
                input.setPosition(offset)
                val buf = NIOUtils.fetchFromChannel(input, nSamples * 4)
                for (i in 0 until nSamples) {
                    ss.add(buf.int)
                }
            }
            sampleCache = ss.toArray()
        }
    }

    /**
     *
     * @return
     * @throws IOException
     */
    @get:Throws(IOException::class)
    @get:Deprecated("""Use getTimecode to automatically populate tape timecode for
                  each frame""")
    val startTimecode: Int
        get() = getTimecodeSample(0)

    fun parseTimecode(tc: String): Int {
        val split = tc.split(":".toRegex()).toTypedArray()
        val tmcd = findFirstPath(box, path("mdia.minf.stbl.stsd.tmcd")) as TimecodeSampleEntry?
        val nf = tmcd!!.numFrames
        return split[3].toInt() + split[2].toInt() * nf + split[1].toInt() * 60 * nf + split[0].toInt() * 3600 * nf
    }

    @Throws(Exception::class)
    fun timeCodeToFrameNo(timeCode: String): Int {
        if (isValidTimeCode(timeCode)) {
            val movieFrame = parseTimecode(timeCode.trim { it <= ' ' }) - sampleCache!![0]
            val frameRate = tse.numFrames.toInt()
            val framesInTimescale = movieFrame * tse.timescale.toLong()
            val mediaToEdited = (QTTimeUtil.mediaToEdited(box, framesInTimescale / frameRate, movie.timescale)
                    * frameRate)
            return (mediaToEdited / box.timescale).toInt()
        }
        return -1
    }

    companion object {
        private fun _getTimecode(startCounter: Int, frameNo: Int, entry: TimecodeSampleEntry): TapeTimecode {
            return TapeTimecode.tapeTimecode(frameNo + startCounter.toLong(), entry.isDropFrame, entry.numFrames.toInt() and 0xff)
        }

        private fun isValidTimeCode(input: String?): Boolean {
            val p = Pattern.compile("[0-9][0-9]:[0-5][0-9]:[0-5][0-9]:[0-2][0-9]")
            val m = p.matcher(input)
            return if (input != null && input.trim { it <= ' ' } != "" && m.matches()) {
                true
            } else false
        }
    }

    init {
        val stbl = box.mdia.minf.stbl
        val stts = findFirst(stbl, "stts") as TimeToSampleBox?
        val stsc = findFirst(stbl, "stsc") as SampleToChunkBox?
        val stco = findFirst(stbl, "stco") as ChunkOffsetsBox?
        val co64 = findFirst(stbl, "co64") as ChunkOffsets64Box?
        timeToSamples = stts!!.getEntries()
        chunkOffsets = stco?.getChunkOffsets() ?: co64!!.getChunkOffsets()
        sampleToChunks = stsc!!.getSampleToChunk()
        if (chunkOffsets.size == 1) {
            cacheSamples(sampleToChunks, chunkOffsets)
        }
        tse = box.sampleEntries[0] as TimecodeSampleEntry
    }
}