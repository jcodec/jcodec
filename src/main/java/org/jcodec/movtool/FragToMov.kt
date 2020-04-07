package org.jcodec.movtool

import org.jcodec.common.ArrayUtil
import org.jcodec.common.IntArrayList
import org.jcodec.common.IntObjectMap
import org.jcodec.common.io.FileChannelWrapper
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.tools.MainUtils
import org.jcodec.containers.mp4.Brand
import org.jcodec.containers.mp4.MP4Util.getRootAtoms
import org.jcodec.containers.mp4.MP4Util.mdatPlaceholder
import org.jcodec.containers.mp4.MP4Util.parseMovie
import org.jcodec.containers.mp4.MP4Util.writeBox
import org.jcodec.containers.mp4.MP4Util.writeMdat
import org.jcodec.containers.mp4.MP4Util.writeMovie
import org.jcodec.containers.mp4.boxes.*
import org.jcodec.containers.mp4.boxes.ChunkOffsets64Box.Companion.createChunkOffsets64Box
import org.jcodec.containers.mp4.boxes.CompositionOffsetsBox.Companion.createCompositionOffsetsBox
import org.jcodec.containers.mp4.boxes.MovieBox.Companion.createMovieBox
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirstPath
import org.jcodec.containers.mp4.boxes.SampleSizesBox.Companion.createSampleSizesBox
import org.jcodec.containers.mp4.boxes.SampleSizesBox.Companion.createSampleSizesBox2
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.Companion.createSampleToChunkBox
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.SampleToChunkEntry
import org.jcodec.containers.mp4.boxes.SyncSamplesBox.Companion.createSyncSamplesBox
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.Companion.createTimeToSampleBox
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry
import java.io.File
import java.io.IOException
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Converts a bunch of DASH fragments into a movie.
 *
 * @author The JCodec project
 */
class FragToMov {
    private var init: File? = null
    private val fragments: MutableList<File> = LinkedList()
    private fun addFragment(file: File) {
        fragments.add(file)
    }

    private fun setInit(file: File) {
        init = file
    }

    @Throws(IOException::class)
    fun toMovie(outFile: File?) {
        var initMov: MovieBox? = null
        if (init != null) initMov = parseMovie(init)
        val list: MutableList<Fragment> = LinkedList()
        var out: FileChannelWrapper? = null
        try {
            out = NIOUtils.writableChannel(outFile)
            out.write(writeBox(Brand.MP4.fileTypeBox, 32))
            val mdatPos = out.position()
            mdatPlaceholder(out)
            for (file in fragments) {
                var `in`: FileChannelWrapper? = null
                try {
                    `in` = NIOUtils.readableChannel(file)
                    val frag = createFragment()
                    for (atom in getRootAtoms(`in`)) {
                        if ("moov" == atom.header.fourcc && initMov == null) {
                            initMov = atom.parseBox(`in`) as MovieBox
                        } else if ("moof".equals(atom.header.fourcc, ignoreCase = true)) {
                            frag.addBox(atom.parseBox(`in`) as MovieFragmentBox, atom.offset)
                        } else if ("mdat".equals(atom.header.fourcc, ignoreCase = true)) {
                            frag.addOffset(out.position(), atom.offset + atom.header.headerSize(),
                                    atom.header.size)
                            atom.copyContents(`in`, out)
                        }
                    }
                    list.add(frag)
                } finally {
                    NIOUtils.closeQuietly(`in`)
                }
            }
            val mdatSize = out.position() - mdatPos - 16
            writeMovie(out, getMoov(initMov, list)!!)
            writeMdat(out, mdatPos, mdatSize)
        } finally {
            NIOUtils.closeQuietly(out)
        }
    }

    class Offset(var dstPos: Long, var srcPos: Long, var len: Long)

    class BoxOffset(var box: MovieFragmentBox, var offset: Long)

    class Fragment {
        var boxes: MutableList<BoxOffset>
        var offsets: MutableList<Offset>
        fun addBox(box: MovieFragmentBox, offset: Long) {
            boxes.add(BoxOffset(box, offset))
        }

        fun addOffset(dstPos: Long, srcPos: Long, len: Long) {
            offsets.add(Offset(dstPos, srcPos, len))
        }

        init {
            boxes = LinkedList()
            offsets = LinkedList()
        }
    }

    companion object {
        private val FLAG_INIT = MainUtils.Flag.flag("init", "i", "A file that contains global sequence headers")
        private val FLAG_OUT = MainUtils.Flag.flag("out", "o", "Output file")
        private val flags = arrayOf(FLAG_INIT, FLAG_OUT)

        @Throws(IOException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val cmd = MainUtils.parseArguments(args, flags)
            if (cmd.argsLength() < 1) {
                MainUtils.printHelpCmd("frag2mov", flags, Arrays.asList(*arrayOf("...out movie")))
                System.exit(-1)
                return
            }
            val fragToMov = FragToMov()
            if (cmd.getStringFlag(FLAG_INIT) != null) {
                fragToMov.setInit(File(cmd.getStringFlag(FLAG_INIT)))
            }
            for (i in 0 until cmd.argsLength()) {
                fragToMov.addFragment(File(cmd.getArg(i)))
            }
            fragToMov.toMovie(File(cmd.getStringFlagD(FLAG_OUT, "out.mp4")))
        }

        private fun createFragment(): Fragment {
            return Fragment()
        }

        fun getMoov(init: MovieBox?, fragments: List<Fragment>): MovieBox? {
            if (init == null) return null
            val movieBox = createMovieBox()
            val tracks = IntObjectMap<MutableList<TrackFragmentBox>>()
            // Applying offset to fragments
            for (frag in fragments) {
                for (bo in frag.boxes) {
                    val no = 0
                    for (traf in bo.box.tracks) {
                        val baseOff = traf.tfhd.baseDataOffset + bo.offset
                        val trun = traf.trun
                        for (offset in frag.offsets) {
                            val dataOff = baseOff + trun.getDataOffset()
                            if (dataOff >= offset.srcPos && dataOff < offset.srcPos + offset.len) {
                                trun.setDataOffset(dataOff - offset.srcPos + offset.dstPos)
                                break
                            }
                        }
                        val trackId = traf.tfhd.getTrackId()
                        var list = tracks[trackId]
                        if (list == null) {
                            list = ArrayList()
                            tracks.put(trackId, list)
                        }
                        list.add(traf)
                    }
                }
            }
            movieBox.addFirst(init.movieHeader)
            val keys = tracks.keys()
            for (i in keys.indices) {
                val trak = createTrack(movieBox, init.getTrackById(keys[i]), tracks[keys[i]]!!)
                movieBox.add(trak!!)
                if (trak.duration > movieBox.duration) movieBox.duration = trak.duration
            }
            return movieBox
        }

        private fun createTrack(movie: MovieBox, trakBox: TrakBox?, list: List<TrackFragmentBox>): TrakBox? {
            var defaultSampleSize = -1
            var defaultSampleDuration = -1
            val sampleSizes: MutableList<IntArray> = LinkedList()
            val compOffsets: MutableList<IntArray> = LinkedList()
            val sampleDurations: MutableList<IntArray> = LinkedList()
            val sync = IntArrayList(list.size)
            val co = LongArray(list.size)
            val sampleToCh = arrayOfNulls<SampleToChunkEntry>(list.size)
            var nSamples = 0
            val avgSampleDur = arrayOfNulls<TimeToSampleEntry>(list.size)
            var i = 0
            var prevDecodeTime: Long = 0
            for (traf in list) {
                val trun = traf.trun
                val tfdt = traf.tfdt
                if (tfdt != null) {
                    if (i > 0) {
                        avgSampleDur[i - 1] = TimeToSampleEntry(trun.getSampleCount().toInt(),
                                ((tfdt.getBaseMediaDecodeTime() - prevDecodeTime) / trun.getSampleCount()).toInt())
                    }
                    prevDecodeTime = tfdt.getBaseMediaDecodeTime()
                }
                co[i] = trun.getDataOffset()
                sampleToCh[i] = SampleToChunkEntry((i + 1).toLong(), trun.getSampleCount().toInt(), 1)
                sync.add(nSamples + 1)
                nSamples += trun.getSampleCount().toInt()
                i++
                val tfhd = traf.tfhd
                if (tfhd.isDefaultSampleSizeAvailable) {
                    val ss = tfhd.defaultSampleSize
                    if (defaultSampleSize == -1) {
                        defaultSampleSize = ss
                    } else if (ss != defaultSampleSize) {
                        throw RuntimeException("Incompatible fragments, default sample sizes differ.")
                    }
                } else {
                    sampleSizes.add(trun.sampleSizes)
                }
                if (tfhd.isDefaultSampleDurationAvailable) {
                    val ss = tfhd.defaultSampleDuration
                    if (defaultSampleDuration == -1) defaultSampleDuration = tfhd.defaultSampleDuration else if (ss != defaultSampleDuration) {
                        throw RuntimeException("Incompatible fragments, default sample durations differ.")
                    }
                } else {
                    if (trun.isSampleDurationAvailable) sampleDurations.add(trun.sampleDurations)
                }
                if (trun.isSampleCompositionOffsetAvailable) {
                    compOffsets.add(trun.sampleCompositionOffsets)
                }
            }
            if (avgSampleDur.size > 1) avgSampleDur[avgSampleDur.size - 1] = avgSampleDur[avgSampleDur.size - 2]
            val stsz = if (defaultSampleSize != -1) createSampleSizesBox(defaultSampleSize, nSamples) else createSampleSizesBox2(ArrayUtil.flatten2DL(sampleSizes))
            var tts = getStts(defaultSampleDuration, nSamples, sampleDurations)
            if (tts == null) tts = avgSampleDur
            val stts = createTimeToSampleBox(tts)
            setTrackDuration(movie, trakBox, tts)
            val co64 = createChunkOffsets64Box(co)
            val stsc = createSampleToChunkBox(sampleToCh)
            trakBox!!.stbl.replaceBox(stsz)
            trakBox.stbl.replaceBox(stts)
            trakBox.stbl.replaceBox(co64)
            trakBox.stbl.replaceBox(stsc)
            trakBox.stbl
                    .replaceBox(createCompositionOffsetsBox(compactCompOffsets(compOffsets)))
            trakBox.stbl.removeChildren(arrayOf("stco"))
            trakBox.stbl.replaceBox(createSyncSamplesBox(sync.toArray()))
            return trakBox
        }

        private fun compactCompOffsets(compOffsets: List<IntArray>): Array<CompositionOffsetsBox.Entry> {
            val res: MutableList<CompositionOffsetsBox.Entry> = LinkedList()
            val prev = 0
            var count = 0
            for (`is` in compOffsets) {
                for (i in `is`.indices) {
                    if (count == 0 || `is`[i] == prev) {
                        count++
                    } else {
                        res.add(CompositionOffsetsBox.Entry(count, prev))
                        count = 1
                    }
                }
            }
            if (count != 0) res.add(CompositionOffsetsBox.Entry(count, prev))
            return res.toTypedArray()
        }

        private fun setTrackDuration(movie: MovieBox, trakBox: TrakBox?, tts: Array<TimeToSampleEntry?>?) {
            var totalDur: Long = 0
            for (tt in tts!!) {
                totalDur += tt!!.segmentDuration
            }
            trakBox!!.duration = movie.rescale(totalDur, trakBox.timescale.toLong())
            val mdhd = findFirstPath(trakBox, arrayOf("mdia", "mdhd")) as MediaHeaderBox?
            mdhd!!.setDuration(totalDur)
        }

        private fun getStts(defaultSampleDuration: Int, nSamples: Int, sampleDurations: List<IntArray>): Array<TimeToSampleEntry?>? {
            val tts: MutableList<TimeToSampleEntry> = LinkedList()
            if (defaultSampleDuration != -1) {
                tts.add(TimeToSampleEntry(nSamples, defaultSampleDuration))
            } else if (sampleDurations.size > 0) {
                for (`is` in sampleDurations) {
                    for (i in `is`.indices) {
                        tts.add(TimeToSampleEntry(1, `is`[i]))
                    }
                }
            }
            return null
        }
    }
}