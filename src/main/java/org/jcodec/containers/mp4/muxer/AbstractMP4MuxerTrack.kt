package org.jcodec.containers.mp4.muxer

import org.jcodec.api.UnhandledStateException
import org.jcodec.common.MuxerTrack
import org.jcodec.common.Preconditions
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.model.Rational
import org.jcodec.common.model.Size
import org.jcodec.common.model.Unit
import org.jcodec.containers.mp4.MP4TrackType
import org.jcodec.containers.mp4.boxes.*
import org.jcodec.containers.mp4.boxes.Box.Companion.createLeafBox
import org.jcodec.containers.mp4.boxes.Box.LeafBox
import org.jcodec.containers.mp4.boxes.ClearApertureBox.Companion.createClearApertureBox
import org.jcodec.containers.mp4.boxes.DataInfoBox.Companion.createDataInfoBox
import org.jcodec.containers.mp4.boxes.DataRefBox.Companion.createDataRefBox
import org.jcodec.containers.mp4.boxes.EditListBox.Companion.createEditListBox
import org.jcodec.containers.mp4.boxes.EncodedPixelBox.Companion.createEncodedPixelBox
import org.jcodec.containers.mp4.boxes.GenericMediaInfoBox.Companion.createGenericMediaInfoBox
import org.jcodec.containers.mp4.boxes.Header.Companion.createHeader
import org.jcodec.containers.mp4.boxes.NameBox.Companion.createNameBox
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirst
import org.jcodec.containers.mp4.boxes.PixelAspectExt.Companion.fourcc
import org.jcodec.containers.mp4.boxes.ProductionApertureBox.Companion.createProductionApertureBox
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.SampleToChunkEntry
import org.jcodec.containers.mp4.boxes.SoundMediaHeaderBox.Companion.createSoundMediaHeaderBox
import org.jcodec.containers.mp4.boxes.TimecodeMediaInfoBox.Companion.createTimecodeMediaInfoBox
import org.jcodec.containers.mp4.boxes.VideoMediaHeaderBox.Companion.createVideoMediaHeaderBox
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
abstract class AbstractMP4MuxerTrack(trackId: Int, type: MP4TrackType) : MuxerTrack {
    var trackId: Int
        protected set
    var type: MP4TrackType
        protected set
    var timescale: Int
    protected var tgtChunkDuration: Rational? = null
    protected var tgtChunkDurationUnit: Unit? = null
    protected var chunkDuration: Long = 0
    protected var curChunk: MutableList<ByteBuffer>
    protected var samplesInChunks: MutableList<SampleToChunkEntry>
    protected var samplesInLastChunk = -1
    protected var chunkNo = 0
    protected var finished = false
    protected var sampleEntries: MutableList<SampleEntry>?
    protected var _edits: MutableList<Edit>? = null
    private var name: String? = null
    protected var out: SeekableByteChannel? = null
    fun setOut(out: SeekableByteChannel?): AbstractMP4MuxerTrack {
        this.out = out
        return this
    }

    fun setTgtChunkDuration(duration: Rational?, unit: Unit?) {
        tgtChunkDuration = duration
        tgtChunkDurationUnit = unit
    }

    abstract val trackTotalDuration: Long

    @Throws(IOException::class)
    abstract fun finish(mvhd: MovieHeaderBox): Box?
    val isVideo: Boolean
        get() = type == MP4TrackType.VIDEO

    val isTimecode: Boolean
        get() = type == MP4TrackType.TIMECODE

    val isAudio: Boolean
        get() = type == MP4TrackType.SOUND

    val displayDimensions: Size
        get() {
            var width = 0
            var height = 0
            if (sampleEntries != null && !sampleEntries!!.isEmpty() && sampleEntries!![0] is VideoSampleEntry) {
                val vse = sampleEntries!![0] as VideoSampleEntry
                val paspBox = findFirst(vse, fourcc()) as PixelAspectExt?
                val pasp = paspBox?.rational ?: Rational(1, 1)
                width = pasp.getNum() * vse.width / pasp.getDen()
                height = vse.height.toInt()
            }
            return Size(width, height)
        }

    fun tapt(trak: TrakBox) {
        val dd = displayDimensions
        if (type == MP4TrackType.VIDEO) {
            val tapt = NodeBox(Header("tapt"))
            tapt.add(createClearApertureBox(dd.width, dd.height))
            tapt.add(createProductionApertureBox(dd.width, dd.height))
            tapt.add(createEncodedPixelBox(dd.width, dd.height))
            trak.add(tapt)
        }
    }

    fun addSampleEntry(se: SampleEntry): AbstractMP4MuxerTrack {
        Preconditions.checkState(!finished, "The muxer track has finished muxing")
        sampleEntries!!.add(se)
        return this
    }

    val entries: List<SampleEntry>?
        get() = sampleEntries

    fun setEdits(edits: List<Edit>?) {
        this._edits = edits?.toMutableList()
    }

    protected fun putEdits(trak: TrakBox) {
        if (_edits != null) {
            val edts = NodeBox(Header("edts"))
            edts.add(createEditListBox(_edits))
            trak.add(edts)
        }
    }

    fun setName(name: String?) {
        this.name = name
    }

    protected fun putName(trak: TrakBox) {
        if (name != null) {
            val udta = NodeBox(Header("udta"))
            udta.add(createNameBox(name))
            trak.add(udta)
        }
    }

    protected fun mediaHeader(minf: MediaInfoBox, type: MP4TrackType) {
        if (MP4TrackType.VIDEO == type) {
            val vmhd = createVideoMediaHeaderBox(0, 0, 0, 0)
            vmhd.flags = 1
            minf.add(vmhd)
        } else if (MP4TrackType.SOUND == type) {
            val smhd = createSoundMediaHeaderBox()
            smhd.flags = 1
            minf.add(smhd)
        } else if (MP4TrackType.META == type) {
            val nmhd: LeafBox = createLeafBox(createHeader("nmhd", 0), ByteBuffer.allocate(4))
            minf.add(nmhd)
        } else if (MP4TrackType.TIMECODE == type) {
            val gmhd = NodeBox(Header("gmhd"))
            gmhd.add(createGenericMediaInfoBox())
            val tmcd = NodeBox(Header("tmcd"))
            gmhd.add(tmcd)
            tmcd.add(createTimecodeMediaInfoBox(0.toShort(), 0.toShort(), 12.toShort(), shortArrayOf(0, 0, 0), shortArrayOf(
                    0xff, 0xff, 0xff), "Lucida Grande"))
            minf.add(gmhd)
        } else if (MP4TrackType.DATA == type) {
            //do nothing
        } else {
            throw UnhandledStateException("Handler " + type.handler + " not supported")
        }
    }

    protected fun addDref(minf: NodeBox) {
        val dinf = createDataInfoBox()
        minf.add(dinf)
        val dref = createDataRefBox()
        dinf.add(dref)
        dref.add(createLeafBox(createHeader("alis", 0), ByteBuffer.wrap(byteArrayOf(0, 0, 0, 1))))
    }

    companion object {
        const val NO_TIMESCALE_SET = -1
    }

    init {
        curChunk = ArrayList()
        samplesInChunks = ArrayList()
        sampleEntries = ArrayList()
        this.trackId = trackId
        this.type = type
        timescale = NO_TIMESCALE_SET
    }
}