package org.jcodec.movtool

import org.jcodec.common.io.FileChannelWrapper
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.tools.MainUtils
import org.jcodec.containers.mp4.BoxFactory.Companion.default
import org.jcodec.containers.mp4.MP4Util.getRootAtoms
import org.jcodec.containers.mp4.MP4Util.mdatPlaceholder
import org.jcodec.containers.mp4.MP4Util.writeMdat
import org.jcodec.containers.mp4.MP4Util.writeMovie
import org.jcodec.containers.mp4.boxes.Box.Companion.path
import org.jcodec.containers.mp4.boxes.ChunkOffsets64Box.Companion.createChunkOffsets64Box
import org.jcodec.containers.mp4.boxes.MovieBox
import org.jcodec.containers.mp4.boxes.NodeBox
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.cloneBox
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirstPath
import org.jcodec.containers.mp4.boxes.SampleSizesBox.Companion.createSampleSizesBox
import org.jcodec.containers.mp4.boxes.SampleSizesBox.Companion.createSampleSizesBox2
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.Companion.createSampleToChunkBox
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.SampleToChunkEntry
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.Companion.createTimeToSampleBox
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry
import java.io.File
import java.io.IOException

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Concatenates multiple identically encoded MP4 files into a single MP4 file
 *
 * @author The JCodec project
 */
class Concat {
    @Throws(IOException::class)
    fun concat(movies: Array<MovieBox?>, offsets: LongArray): MovieBox? {
        val result = cloneBox(movies[0]!!, 16 * 1024 * 1024, default) as MovieBox?
        var prevTracks = 0
        var totalDuration: Long = 0
        for (i in movies.indices) {
            val tracks = movies[i]!!.tracks
            if (i != 0 && prevTracks != tracks.size) {
                throw RuntimeException("Incompatible movies. Movie " + i + " has different number of tracks ("
                        + tracks.size + " vs " + prevTracks + ").")
            }
            prevTracks = tracks.size
            // TODO: check sample entries
            totalDuration += movies[i]!!.duration
        }
        for (i in 0 until prevTracks) {
            offsetTrack(result, movies, offsets, i)
        }
        result!!.duration = totalDuration
        return result
    }

    private fun offsetTrack(result: MovieBox?, movies: Array<MovieBox?>, offsets: LongArray, index: Int) {
        val rtracks = result!!.tracks
        val rstbl = findFirstPath(rtracks[index], path("mdia.minf.stbl")) as NodeBox?
        var totalChunks = 0
        var totalTts = 0
        var totalSizes = 0
        var defaultSize = 0
        var totalCount = 0
        var totalStsc = 0
        var totalDuration: Long = 0
        for (i in movies.indices) {
            val trakBox = movies[i]!!.tracks[index]
            val stco = trakBox.stco
            val co64 = trakBox.co64
            val entries = trakBox.stts.getEntries()
            val stsz = trakBox.stsz
            totalStsc += trakBox.stsc.getSampleToChunk().size
            if (stsz.defaultSize != 0) {
                defaultSize = stsz.defaultSize
                totalCount += stsz.getCount()
            } else {
                val sizes = stsz.getSizes()
                totalSizes += sizes.size
            }
            totalTts += entries.size
            val chunkOffsets = stco?.getChunkOffsets() ?: co64!!.getChunkOffsets()
            totalChunks += chunkOffsets.size
            totalDuration += trakBox.duration
        }
        val rOffsets = LongArray(totalChunks)
        var rSizes: IntArray? = null
        if (defaultSize == 0) rSizes = IntArray(totalSizes)
        val rTts = arrayOfNulls<TimeToSampleEntry>(totalTts)
        val rStsc = arrayOfNulls<SampleToChunkEntry>(totalStsc)
        var lastChunks = 0
        var i = 0
        var rc = 0
        var rt = 0
        var rs = 0
        var rsc = 0
        while (i < movies.size) {
            val trakBox = movies[i]!!.tracks[index]
            val stco = trakBox.stco
            val co64 = trakBox.co64
            val chunkOffsets = stco?.getChunkOffsets() ?: co64!!.getChunkOffsets()
            var c = 0
            while (c < chunkOffsets.size) {
                rOffsets[rc] = chunkOffsets[c] + offsets[i]
                c++
                rc++
            }
            val entries = trakBox.stts.getEntries()
            var t = 0
            while (t < entries.size) {
                rTts[rt] = entries[t]
                t++
                rt++
            }
            if (defaultSize == 0) {
                val sizes = trakBox.stsz.getSizes()
                var s = 0
                while (s < sizes.size) {
                    rSizes!![rs] = sizes[s]
                    s++
                    rs++
                }
            }
            val stscE = trakBox.stsc.getSampleToChunk()
            var sc = 0
            while (sc < stscE.size) {
                rStsc[rsc] = stscE[sc]
                rStsc[rsc]!!.first = rStsc[rsc]!!.first + lastChunks
                sc++
                rsc++
            }
            lastChunks += chunkOffsets.size
            i++
        }
        rstbl!!.replace("stts", createTimeToSampleBox(rTts))
        rstbl.replace("stsz", if (defaultSize == 0) createSampleSizesBox2(rSizes!!) else createSampleSizesBox(defaultSize, totalCount))
        rstbl.replace("stsc", createSampleToChunkBox(rStsc))
        rstbl.removeChildren(arrayOf("stco", "co64"))
        rstbl.add(createChunkOffsets64Box(rOffsets))
        rtracks[index].duration = totalDuration
    }

    companion object {
        private val flags = arrayOf<MainUtils.Flag>()

        @Throws(IOException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val cmd = MainUtils.parseArguments(args, flags)
            if (cmd.argsLength() < 2) {
                MainUtils.printHelpArgs(flags, arrayOf("output", "input*"))
                return
            }
            var out: FileChannelWrapper? = null
            try {
                out = NIOUtils.writableChannel(File(cmd.getArg(0)))
                val mv = arrayOfNulls<MovieBox>(args.size - 1)
                val offsets = LongArray(args.size - 1)
                var prevOff: Long = 0
                var mdatPos: Long = 0
                var mdatSize: Long = 0
                for (i in 1 until cmd.argsLength()) {
                    val file = File(cmd.getArg(i))
                    offsets[i - 1] = prevOff
                    var `in`: FileChannelWrapper? = null
                    try {
                        `in` = NIOUtils.readableChannel(file)
                        for (atom in getRootAtoms(`in`)) {
                            if ("ftyp" == atom.header.fourcc && i == 1) {
                                atom.copy(`in`, out)
                                offsets[i - 1] += atom.header.size
                            } else if ("mdat" == atom.header.fourcc) {
                                if (i == 1) {
                                    mdatPos = mdatPlaceholder(out)
                                    offsets[i - 1] = offsets[i - 1] + 16
                                }
                                atom.copyContents(`in`, out)
                                mdatSize += atom.header.bodySize
                                offsets[i - 1] -= atom.offset + atom.header.headerSize()
                            } else if ("moov" == atom.header.fourcc) {
                                mv[i - 1] = atom.parseBox(`in`) as MovieBox
                            }
                        }
                        prevOff = out.position()
                    } finally {
                        NIOUtils.closeQuietly(`in`)
                    }
                }
                val movieBox = Concat().concat(mv, offsets)
                writeMovie(out, movieBox!!)
                writeMdat(out, mdatPos, mdatSize)
            } finally {
                NIOUtils.closeQuietly(out)
            }
        }
    }
}