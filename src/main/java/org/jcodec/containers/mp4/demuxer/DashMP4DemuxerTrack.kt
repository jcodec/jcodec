package org.jcodec.containers.mp4.demuxer

import org.jcodec.codecs.aac.AACUtils
import org.jcodec.codecs.h264.H264Utils
import org.jcodec.codecs.h264.io.model.SeqParameterSet
import org.jcodec.common.*
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Packet.FrameType
import org.jcodec.containers.mp4.MP4Packet
import org.jcodec.containers.mp4.MP4TrackType
import org.jcodec.containers.mp4.MP4Util
import org.jcodec.containers.mp4.boxes.*
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findAllPath
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirst
import org.jcodec.containers.mp4.boxes.TrakBox.Companion.getTrackType
import org.jcodec.containers.mp4.demuxer.MP4DemuxerTrackMeta.Companion.getCodecPrivateOpaque
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Demuxes one track out of multiple DASH fragments
 *
 * @author The JCodec project
 */
class DashMP4DemuxerTrack(mov: MovieBox?, trak: TrakBox, fragments: Array<Fragment>) : SeekableDemuxerTrack, Closeable {
    private val sizes: Array<IntArray?>
    private val compOffsets: Array<IntArray?>
    private val chunkOffsets: LongArray
    private val avgDur: IntArray
    private val sampleDurations: Array<IntArray?>
    private var offInChunk: Long = 0
    private val inputs: Array<SeekableByteChannel?>
    private var curFrame = 0
    private var curFrag = 0
    private var globalFrame: Long = 0
    private var pts: Long = 0
    private var frameCount = 0
    private var totalDuration: Long = 0
    private val trak: TrakBox
    private val sampleEntries: Array<SampleEntry?>
    private var durationHint = 0.0

    class Fragment(var frag: TrackFragmentBox, var offset: Long, var input: SeekableByteChannel)

    class FragmentComparator : Comparator<Fragment> {
        override fun compare(arg0: Fragment, arg1: Fragment): Int {
            val a = if (arg0.frag.tfdt == null) 0 else arg0.frag.tfdt.getBaseMediaDecodeTime()
            val b = if (arg1.frag.tfdt == null) 0 else arg1.frag.tfdt.getBaseMediaDecodeTime()
            return if (a < b) -1 else if (a == b) 0 else 1
        }
    }

    private fun sortable(fragments: Array<Fragment>): Boolean {
        for (fragment in fragments) {
            if (fragment.frag.tfdt == null) return false
        }
        return true
    }

    @Synchronized
    @Throws(IOException::class)
    override fun nextFrame(): MP4Packet? {
        if (curFrag >= sizes.size) return null
        if (curFrame >= sizes[curFrag]!!.size) {
            curFrag++
            curFrame = 0
            offInChunk = 0
        }
        if (curFrag >= sizes.size) return null
        val size = sizes[curFrag]!![curFrame]
        return getNextFrame(ByteBuffer.allocate(size))!!
    }

    @Throws(IOException::class)
    protected fun readPacketData(input: SeekableByteChannel?, buffer: ByteBuffer?, offset: Long, size: Int): ByteBuffer {
        val result = buffer!!.duplicate()
        synchronized(input!!) {
            input.setPosition(offset)
            NIOUtils.readL(input, result, size)
        }
        result.flip()
        return result
    }

    @Synchronized
    @Throws(IOException::class)
    fun getNextFrame(storage: ByteBuffer?): MP4Packet? {
        if (curFrag >= sizes.size) return null
        if (curFrame >= sizes[curFrag]!!.size) return null
        val size = sizes[curFrag]!![curFrame]
        require(!(storage != null && storage.remaining() < size)) { "Buffer size is not enough to fit a packet" }
        val pktPos = chunkOffsets[curFrag] + offInChunk
        val result = readPacketData(inputs[curFrag], storage, pktPos, size)
        if (result != null && result.remaining() < size) return null
        val hintedDuration = (durationHint * trak.timescale / frameCount).toInt()
        val duration = if (sampleDurations[curFrag] == null) if (avgDur[curFrag] == 0) hintedDuration else avgDur[curFrag] else sampleDurations[curFrag]!![curFrame]
        val sync = curFrame == 0
        var realPts = pts
        if (compOffsets[curFrag] != null) {
            realPts = pts + compOffsets[curFrag]!![curFrame]
        }
        val ftype = if (sync) FrameType.KEY else FrameType.INTER
        val pkt = MP4Packet(result, realPts, trak.timescale, duration.toLong(), globalFrame, ftype, null, 0,
                realPts, 1, pktPos, size, false)
        offInChunk += size.toLong()
        pts += duration.toLong()
        curFrame++
        globalFrame++
        return pkt
    }

    override fun getMeta(): DemuxerTrackMeta {
        val type = getTrackType(trak)
        val t = if (type == MP4TrackType.VIDEO) TrackType.VIDEO else if (type == MP4TrackType.SOUND) TrackType.AUDIO else TrackType.OTHER
        val seekFrames = IntArray(sizes.size)
        var i = 0
        var numFrames = 0
        while (i < sizes.size) {
            seekFrames[i] = numFrames
            numFrames += sizes[i]!!.size
            i++
        }
        val codecPrivate = codecPrivate
        val videoCodecMeta = videoCodecMeta
        val audioCodecMeta = audioCodecMeta
        val codec = Codec.codecByFourcc(sampleEntries[0]!!.fourcc)
        val opaque = getCodecPrivateOpaque(codec,
                sampleEntries[0])
        return MP4DemuxerTrackMeta(t, codec, totalDuration.toDouble(), seekFrames,
                frameCount, codecPrivate, videoCodecMeta, audioCodecMeta, sampleEntries, opaque)
    }

    protected val colorInfo: ColorSpace?
        protected get() {
            val codec = Codec.codecByFourcc(trak.fourcc)
            if (codec == Codec.H264) {
                val avcC = H264Utils.parseAVCC(sampleEntries[0] as VideoSampleEntry?)
                val spsList = avcC.getSpsList()
                if (spsList.size > 0) {
                    val sps = SeqParameterSet.read(spsList[0].duplicate())
                    return sps.chromaFormatIdc
                }
            }
            return null
        }

    private val audioCodecMeta: AudioCodecMeta?
        private get() {
            val type = getTrackType(trak)
            var audioCodecMeta: AudioCodecMeta? = null
            if (type == MP4TrackType.SOUND) {
                val ase = sampleEntries[0] as AudioSampleEntry?
                audioCodecMeta = AudioCodecMeta.fromAudioFormat(ase!!.format)
            }
            return audioCodecMeta
        }

    private val videoCodecMeta: VideoCodecMeta?
        private get() {
            val type = getTrackType(trak)
            var videoCodecMeta: VideoCodecMeta? = null
            if (type == MP4TrackType.VIDEO) {
                videoCodecMeta = VideoCodecMeta.createSimpleVideoCodecMeta(trak.codedSize, colorInfo)
                val pasp = findFirst(sampleEntries[0], "pasp") as PixelAspectExt?
                if (pasp != null) videoCodecMeta.pixelAspectRatio = pasp.rational
            }
            return videoCodecMeta
        }

    // This codec does not have private section
    val codecPrivate: ByteBuffer?
        get() {
            val codec = Codec.codecByFourcc(sampleEntries[0]!!.fourcc)
            if (codec == Codec.H264) {
                val avcC = H264Utils.parseAVCC(sampleEntries[0] as VideoSampleEntry?)
                return H264Utils.avcCToAnnexB(avcC)
            } else if (codec == Codec.AAC) {
                return AACUtils.getCodecPrivate(sampleEntries[0])
            }
            // This codec does not have private section
            return null
        }

    @Throws(IOException::class)
    override fun gotoFrame(frameNo: Long): Boolean {
        var frameNo = frameNo
        var curFrag = 0
        var globalFrame = 0
        var pts = 0L
        for (`is` in sizes) {
            if (frameNo > `is`!!.size) {
                frameNo -= `is`.size.toLong()
                pts += if (sampleDurations[curFrag] == null) avgDur[curFrag] * `is`.size else ArrayUtil.sumInt(sampleDurations[curFrag])
                curFrag++
                globalFrame += `is`.size
            } else {
                pts += if (sampleDurations[curFrag] == null) avgDur[curFrag] * frameNo else ArrayUtil.sumInt3(sampleDurations[curFrag], 0, frameNo.toInt()).toLong()
                this.curFrag = curFrag
                curFrame = frameNo.toInt()
                this.globalFrame = globalFrame + frameNo
                this.pts = pts.toLong()
                adjustOff()
                return true
            }
        }
        return false
    }

    private fun adjustOff() {
        offInChunk = 0
        for (i in 0 until curFrame) {
            offInChunk += sizes[curFrag]!![i]
        }
    }

    override fun getCurFrame(): Long {
        return globalFrame
    }

    @Throws(IOException::class)
    override fun seek(second: Double) {
        var second = second
        var curFrag = 0
        var globalFrame = 0
        var pts = 0
        for (`is` in sizes) {
            val fragDur = if (sampleDurations[curFrag] != null) ArrayUtil.sumInt(sampleDurations[curFrag]) else avgDur[curFrag] * `is`!!.size
            if (second > fragDur) {
                second -= fragDur.toDouble()
                pts += fragDur
                curFrag++
                globalFrame += `is`!!.size
            } else {
                this.curFrag = curFrag
                if (sampleDurations[curFrag] != null) {
                    curFrame = 0
                    while (curFrame < sampleDurations[curFrag]!!.size) {
                        if (second < sampleDurations[curFrag]!![curFrame]) break
                        second -= sampleDurations[curFrag]!![curFrame]
                        curFrame++
                    }
                } else {
                    curFrame = (second / avgDur[this.curFrag]).toInt()
                    adjustOff()
                }
                pts += if (sampleDurations[curFrag] == null) avgDur[curFrag] * curFrame else ArrayUtil.sumInt3(sampleDurations[curFrag], 0, curFrame)
                this.globalFrame = globalFrame + curFrame.toLong()
            }
        }
    }

    @Throws(IOException::class)
    override fun gotoSyncFrame(frameNo: Long): Boolean {
        var frameNo = frameNo
        var curFrag = 0
        var globalFrame = 0
        var pts = 0
        for (`is` in sizes) {
            if (frameNo > `is`!!.size) {
                frameNo -= `is`.size.toLong()
                pts += if (sampleDurations[curFrag] == null) avgDur[curFrag] * `is`.size else ArrayUtil.sumInt(sampleDurations[curFrag])
                curFrag++
                globalFrame += `is`.size
            } else {
                this.curFrag = curFrag
                curFrame = 0
                this.globalFrame = globalFrame.toLong()
                this.pts = pts.toLong()
                offInChunk = 0
                return true
            }
        }
        return false
    }

    val trackType: MP4TrackType?
        get() = getTrackType(trak)

    @Throws(IOException::class)
    override fun close() {
        var ex: IOException? = null
        for (channel in inputs) {
            try {
                channel!!.close()
            } catch (e: IOException) {
                ex = e
            }
        }
        if (ex != null) throw ex
    }

    val no: Int
        get() = trak.trackHeader.trackId

    fun setDurationHint(arg: Double) {
        durationHint = arg
    }

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun createFromFiles(files: List<File?>): DashMP4DemuxerTrack {
            val fragments: MutableList<Fragment> = ArrayList()
            var moov: MovieBox? = null
            for (file in files) {
                val channel: SeekableByteChannel = NIOUtils.readableChannel(file)
                for (atom in MP4Util.getRootAtoms(channel)) {
                    if ("moov" == atom.header.fourcc) {
                        moov = atom.parseBox(channel) as MovieBox
                    } else if ("moof".equals(atom.header.fourcc, ignoreCase = true)) {
                        val mfra = atom.parseBox(channel) as MovieFragmentBox
                        val tfra = mfra.tracks[0]
                        fragments.add(Fragment(tfra, atom.offset, channel))
                    }
                }
            }
            return DashMP4DemuxerTrack(moov, moov!!.tracks[0], fragments.toTypedArray())
        }
    }

    init {
        var prevOffset: Long = 0
        var prevSampleCount = 0
        sizes = arrayOfNulls(fragments.size)
        compOffsets = arrayOfNulls(fragments.size)
        chunkOffsets = LongArray(fragments.size)
        avgDur = IntArray(fragments.size)
        sampleDurations = arrayOfNulls(fragments.size)
        this.trak = trak
        inputs = arrayOfNulls(fragments.size)
        sampleEntries = findAllPath(trak, arrayOf("mdia", "minf", "stbl", "stsd", null)) as Array<SampleEntry?>
        if (sortable(fragments)) Arrays.sort(fragments, 0, fragments.size, FragmentComparator())
        var i = 0
        for (fragment in fragments) {
            val frag = fragment.frag
            val trun = frag.trun
            val tfdt = frag.tfdt
            if (tfdt != null) {
                if (i > 0) {
                    avgDur[i - 1] = ((tfdt.getBaseMediaDecodeTime() - prevOffset) / prevSampleCount).toInt()
                    totalDuration = tfdt.getBaseMediaDecodeTime()
                }
                prevOffset = tfdt.getBaseMediaDecodeTime()
            }
            prevSampleCount = trun.getSampleCount().toInt()
            sizes[i] = trun.sampleSizes
            compOffsets[i] = trun.sampleCompositionOffsets
            chunkOffsets[i] = fragment.offset + frag.tfhd.baseDataOffset + frag.trun.getDataOffset()
            if (trun.isSampleDurationAvailable) {
                sampleDurations[i] = trun.sampleDurations
                totalDuration += ArrayUtil.sumInt(sampleDurations[i]).toLong()
            }
            frameCount += sizes[i]!!.size
            inputs[i] = fragment.input
            i++
        }
        if (avgDur.size > 1) {
            avgDur[avgDur.size - 1] = avgDur[avgDur.size - 2]
            totalDuration += avgDur[avgDur.size - 1] * prevSampleCount.toLong()
        }
    }
}