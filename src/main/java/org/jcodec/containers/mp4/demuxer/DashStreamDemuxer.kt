package org.jcodec.containers.mp4.demuxer

import org.jcodec.api.JCodecException
import org.jcodec.common.*
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.model.Packet
import org.jcodec.containers.mp4.MP4Packet
import org.jcodec.containers.mp4.MPDModel
import org.jcodec.containers.mp4.MPDModel.AdaptationSet
import org.jcodec.containers.mp4.MPDModel.MPD
import org.jcodec.containers.mp4.demuxer.DashMP4DemuxerTrack.Companion.createFromFiles
import java.io.Closeable
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URL
import java.util.*
import java.util.concurrent.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Demuxes one track out of multiple DASH fragments
 *
 * @author The JCodec project
 */
class DashStreamDemuxer(url: URL) : Demuxer {
    private val tracks: MutableList<DashStreamDemuxerTrack>
    private val coded: MutableList<SeekableDemuxerTrack?>
    private var mpd: MPD? = null

    internal class DashStreamDemuxerTrack(private val url: URL, private val adaptationSet: AdaptationSet, private val period: MPDModel.Period) : SeekableDemuxerTrack, Closeable {
        private var initFile: File? = null
        private var selectedRprz: String? = null
        private val fragments: MutableMap<Int, Future<DashMP4DemuxerTrack?>?> = HashMap()
        private var curFragNo = 0
        private val streaming = false
        private val maxDownloadAttampts = 1024
        private val threadPool: ExecutorService
        private var globalFrame = 0
        private var frameRate = 0f
        private var segmentDuration = 0.0
        private var seekFrames: IntArray = IntArray(0)
        var id: Int
        private val rrprz: MPDModel.Representation?
            private get() {
                for (rprz in adaptationSet.representations) {
                    if (rprz.id == selectedRprz) return rprz
                }
                return null
            }

        private val segmentTemplate: MPDModel.SegmentTemplate?
            private get() {
                val rprz = rrprz
                return if (adaptationSet.segmentTemplate == null) rprz!!.segmentTemplate else adaptationSet.segmentTemplate
            }

        @Throws(IOException::class)
        private fun downloadFrag(no: Int): DashMP4DemuxerTrack? {
            val rprz = rrprz
            val stpl = segmentTemplate
            for (i in 0 until maxDownloadAttampts) {
                try {
                    var urlInit: URL? = null
                    if (stpl != null && stpl.media != null) {
                        val vals: MutableMap<String, Any?> = HashMap()
                        vals["RepresentationID"] = selectedRprz
                        vals["Number"] = stpl.startNumber + no
                        val tmp = fillTemplate(stpl.media, vals)
                        urlInit = URL(url, tmp)
                    } else if (rprz!!.segmentList != null && rprz.segmentList.segmentUrls.size > no) {
                        val segmentURL = rprz.segmentList.segmentUrls[no]
                        urlInit = URL(url, segmentURL.media)
                    }
                    if (urlInit != null) {
                        val tempFile = File.createTempFile("org.jcodec", fileName(urlInit.path))
                        println("Fetching fragment: " + urlInit.toExternalForm())
                        NIOUtils.fetchUrl(urlInit, tempFile)
                        val demuxer = createFromFiles(Arrays.asList(*arrayOf(initFile, tempFile)))
                        demuxer.setDurationHint(segmentDuration)
                        return demuxer
                    } else if (rprz!!.baseURL != null) {
                        return createFromFiles(Arrays.asList(initFile, initFile))
                    }
                    break
                } catch (e: FileNotFoundException) {
                    if (!streaming) return null
                    sleepQuiet(100)
                }
            }
            return null
        }

        private fun fillTemplate(media: String, vals: Map<String, Any?>): String {
            val builder = StringBuilder()
            val charArray = media.toCharArray()
            var varStart = 0
            for (i in charArray.indices) {
                if ('$' == charArray[i]) {
                    if (varStart == 0) {
                        varStart = i + 1
                    } else {
                        var `var` = String(charArray, varStart, i - varStart)
                        val formatStart = `var`.indexOf('%')
                        if (formatStart != -1) {
                            val format = `var`.substring(formatStart)
                            `var` = `var`.substring(0, formatStart)
                            val `object` = vals[`var`]
                            if (`object` != null) {
                                val `val` = String.format(format, `object`)
                                builder.append(`val`)
                            }
                        } else {
                            val `object` = vals[`var`]
                            if (`object` != null) {
                                builder.append(`object`.toString())
                            }
                        }
                        varStart = 0
                    }
                } else if (varStart == 0) {
                    builder.append(charArray[i])
                }
            }
            return builder.toString()
        }

        private fun fileName(path: String): String {
            val split = path.split("/".toRegex()).toTypedArray()
            return split[split.size - 1]
        }

        @Throws(IOException::class)
        private fun downloadInit() {
            val rprz = rrprz
            if (segmentTemplate != null && segmentTemplate!!.initialization != null) {
                val tmp = segmentTemplate!!.initialization.replace("\$RepresentationID$", selectedRprz!!)
                val urlInit = URL(url, tmp)
                val tempFile = File.createTempFile("org.jcodec", fileName(urlInit.path))
                println("Fetching init: " + urlInit.toExternalForm())
                NIOUtils.fetchUrl(urlInit, tempFile)
                initFile = tempFile
            } else if (rprz!!.baseURL != null) {
                val urlInit = URL(url, rprz.baseURL)
                val tempFile = File.createTempFile("org.jcodec", fileName(urlInit.path))
                println("Fetching init: " + urlInit.toExternalForm())
                NIOUtils.fetchUrl(urlInit, tempFile)
                initFile = tempFile
            } else if (rprz.segmentList != null && rprz.segmentList.initialization != null) {
                val urlInit = URL(url, rprz.segmentList.initialization.sourceURL)
                val tempFile = File.createTempFile("org.jcodec", fileName(urlInit.path))
                println("Fetching init: " + urlInit.toExternalForm())
                NIOUtils.fetchUrl(urlInit, tempFile)
                initFile = tempFile
            }
        }

        @Throws(IOException::class)
        override fun nextFrame(): Packet? {
            return try {
                var curFrag = fragments[curFragNo]
                var nextFrame: MP4Packet? = null
                if (curFrag != null) {
                    nextFrame = getCurFrag(curFrag)!!.nextFrame()
                    if (nextFrame == null) {
                        getCurFrag(curFrag)!!.close()
                        fragments[curFragNo] = null
                        curFragNo++
                    }
                }
                if (nextFrame != null) {
                    ++globalFrame
                    return setPts(nextFrame)
                }
                curFrag = fragments[curFragNo]
                if (curFrag == null) {
                    for (i in curFragNo until curFragNo + INIT_SIZE) scheduleFragment(curFragNo)
                    curFrag = fragments[curFragNo]
                }
                if (curFrag == null) return null
                ++globalFrame
                setPts(getCurFrag(curFrag)!!.nextFrame()!!)
            } catch (e: ExecutionException) {
                throw RuntimeException("Execution problem", e)
            }
        }

        @Throws(ExecutionException::class)
        private fun getCurFrag(curFrag: Future<DashMP4DemuxerTrack?>?): DashMP4DemuxerTrack? {
            while (true) {
                try {
                    return curFrag!!.get()
                } catch (e: InterruptedException) {
                }
            }
        }

        private fun setPts(frame: MP4Packet): MP4Packet {
            val off = curFragNo * segmentDuration
            frame.setPts((frame.getPts() + off * frame.getTimescale()).toLong())
            frame.mediaPts = (frame.mediaPts + off * frame.getTimescale()).toLong()
            frame.setFrameNo(globalFrame - 1)
            if (id == 5) println(String.format("[%d] PTS: %f DUR: %s", id, frame.ptsD.toFloat(), frame.durationD.toFloat()))
            return frame
        }

        private fun scheduleFragment(fragNo: Int) {
            if (fragments[fragNo] == null) {
                val future: Future<DashMP4DemuxerTrack?> = threadPool.submit(Callable { downloadFrag(fragNo) })
                fragments[fragNo] = future
            }
        }

        override fun getMeta(): DemuxerTrackMeta? {
            val future = fragments[curFragNo] ?: return null
            return try {
                val frag = getCurFrag(future)!!
                val fragMeta = frag.meta as MP4DemuxerTrackMeta
                val totalDuration = period.duration.sec
                val totalFrames = (period.duration.sec * frameRate).toInt()
                MP4DemuxerTrackMeta(fragMeta.type, fragMeta.codec, totalDuration, seekFrames,
                        totalFrames, fragMeta.codecPrivate, fragMeta.videoCodecMeta,
                        fragMeta.audioCodecMeta, fragMeta.sampleEntries, fragMeta.codecPrivateOpaque)
            } catch (e: ExecutionException) {
                throw RuntimeException("Execution problem", e)
            }
        }

        @Throws(IOException::class)
        override fun close() {
            val entrySet: Set<MutableMap.MutableEntry<Int, Future<DashMP4DemuxerTrack?>?>> = fragments.entries
            for (entry in entrySet) {
                if (entry.value != null) {
                    try {
                        getCurFrag(entry.value)!!.close()
                    } catch (e: ExecutionException) {
                        throw IOException("Execution problem", e)
                    }
                    entry.setValue(null)
                }
            }
        }

        @Throws(IOException::class)
        override fun gotoFrame(frameNo: Long): Boolean {
            if (frameNo != 0L) return false
            curFragNo = 0
            globalFrame = 0
            for (i in 0 until INIT_SIZE) {
                scheduleFragment(i)
            }
            return true
        }

        @Throws(IOException::class)
        override fun gotoSyncFrame(frameNo: Long): Boolean {
            return false
        }

        override fun getCurFrame(): Long {
            return globalFrame.toLong()
        }

        @Throws(IOException::class)
        override fun seek(second: Double) {
            throw RuntimeException("unimpl")
        }

        companion object {
            var next_id = 0
            private fun sleepQuiet(millis: Int) {
                try {
                    Thread.sleep(millis.toLong())
                } catch (e: InterruptedException) {
                }
            }
        }

        init {
            threadPool = Executors.newFixedThreadPool(INIT_SIZE) { r ->
                val t = Executors.defaultThreadFactory().newThread(r)
                t.isDaemon = true
                t
            }
            if (adaptationSet.representations.size > 0) {
                val rprz = adaptationSet.representations[0]
                selectedRprz = rprz.id
                frameRate = if (rprz.frameRate == null) 0f else rprz.frameRate.scalar()
                val stpl = segmentTemplate
                segmentDuration = if (stpl != null) stpl.duration.toDouble() / stpl.timescale else period.duration.sec
                downloadInit()
                for (i in 0 until INIT_SIZE) {
                    scheduleFragment(i)
                }
                val numSeg = (period.duration.sec / segmentDuration).toInt()
                val segmentFrames = (segmentDuration * frameRate).toInt()
                seekFrames = IntArray(numSeg)
                var i = 0
                var tmp = 0
                while (i < numSeg) {
                    seekFrames[i] = tmp + 1
                    i++
                    tmp += segmentFrames
                }
            }
            id = next_id++
        }
    }

    @Throws(IOException::class)
    override fun close() {
        for (track in tracks) {
            track.close()
        }
    }

    override fun getTracks(): List<DemuxerTrack?> {
        return coded
    }

    override fun getVideoTracks(): List<DemuxerTrack?> {
        val result = ArrayList<SeekableDemuxerTrack?>()
        for (demuxerTrack in coded) {
            val meta = demuxerTrack!!.meta
            if (meta.type == TrackType.VIDEO) result.add(demuxerTrack)
        }
        return result
    }

    override fun getAudioTracks(): List<DemuxerTrack?> {
        val result = ArrayList<SeekableDemuxerTrack?>()
        for (demuxerTrack in coded) {
            val meta = demuxerTrack!!.meta
            if (meta.type == TrackType.AUDIO) result.add(demuxerTrack)
        }
        return result
    }

    companion object {
        var INIT_SIZE = 3
    }

    init {
        tracks = LinkedList()
        coded = LinkedList()
        try {
            val _mpd = MPDModel.parse(url)
            if (_mpd != null && _mpd.periods != null && _mpd.periods.size > 0) {
                val period = _mpd.periods[0]
                for (adaptationSet in period.adaptationSets) {
                    val tr = DashStreamDemuxerTrack(url, adaptationSet, period)
                    tracks.add(tr)
                    coded.add(CodecMP4DemuxerTrack(tr))
                }
            }
            mpd = _mpd
        } catch (e: JCodecException) {
            throw IOException(e)
        }
    }
}