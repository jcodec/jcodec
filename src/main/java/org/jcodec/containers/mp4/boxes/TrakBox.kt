package org.jcodec.containers.mp4.boxes

import org.jcodec.common.model.Rational
import org.jcodec.common.model.Size
import org.jcodec.containers.mp4.Chunk
import org.jcodec.containers.mp4.MP4TrackType
import org.jcodec.containers.mp4.boxes.ClearApertureBox.Companion.createClearApertureBox
import org.jcodec.containers.mp4.boxes.ClipRegionBox.Companion.createClipRegionBox
import org.jcodec.containers.mp4.boxes.DataInfoBox.Companion.createDataInfoBox
import org.jcodec.containers.mp4.boxes.DataRefBox.Companion.createDataRefBox
import org.jcodec.containers.mp4.boxes.EditListBox.Companion.createEditListBox
import org.jcodec.containers.mp4.boxes.EncodedPixelBox.Companion.createEncodedPixelBox
import org.jcodec.containers.mp4.boxes.NameBox.Companion.createNameBox
import org.jcodec.containers.mp4.boxes.PixelAspectExt.Companion.createPixelAspectExt
import org.jcodec.containers.mp4.boxes.ProductionApertureBox.Companion.createProductionApertureBox
import org.jcodec.containers.mp4.boxes.UrlBox.Companion.createUrlBox
import org.jcodec.containers.mp4.demuxer.MP4Demuxer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Creates MP4 file out of a set of samples
 *
 * @author The JCodec project
 */
class TrakBox(atom: Header) : NodeBox(atom) {
    fun setDataRef(url: String?) {
        val minf = mdia.minf
        var dinf = minf.dinf
        if (dinf == null) {
            dinf = createDataInfoBox()
            minf.add(dinf)
        }
        var dref = dinf.dref
        val urlBox = createUrlBox(url)
        if (dref == null) {
            dref = createDataRefBox()
            dinf.add(dref)
            dref.add(urlBox)
        } else {
            val lit = dref.getBoxes().listIterator()
            while (lit.hasNext()) {
                val box = lit.next() as FullBox
                if (box.flags and 0x1 != 0) lit.set(urlBox)
            }
        }
    }

    val mdia: MediaBox
        get() = findFirst(this, "mdia") as MediaBox

    val trackHeader: TrackHeaderBox
        get() = findFirst(this, "tkhd") as TrackHeaderBox

    var edits: List<Edit>?
        get() {
            val elst = (findFirstPath(this, Box.path("edts.elst")) as EditListBox?) ?: return null
            return elst.getEdits()
        }
        set(edits) {
            var edts = findFirst(this, "edts") as? NodeBox?
            if (edts == null) {
                edts = NodeBox(Header("edts"))
                add(edts)
            }
            edts.removeChildren(arrayOf("elst"))
            edts.add(createEditListBox(edits?.toMutableList()))
            trackHeader.setDuration(getEditedDuration(this))
        }

    val isVideo: Boolean
        get() = "vide" == handlerType

    val isTimecode: Boolean
        get() = "tmcd" == handlerType

    val handlerType: String?
        get() {
            val handlerBox = findFirstPath(this, Box.path("mdia.hdlr")) as HandlerBox? ?: return null
            return handlerBox.componentSubType
        }

    val isAudio: Boolean
        get() = "soun" == handlerType

    val isMeta: Boolean
        get() = "meta" == handlerType

    /**
     * Gets 'media timescale' of this track. This is the timescale used to
     * represent the durations of samples inside mdia/minf/stbl/stts box.
     *
     * @return 'media timescale' of the track.
     */
    /**
     * Sets the 'media timescale' of this track. This is the time timescale used
     * to represent sample durations.
     *
     * @param timescale
     * A new 'media timescale' of this track.
     */
    var timescale: Int
        get() = (findFirstPath(this, Box.path("mdia.mdhd")) as? MediaHeaderBox?)!!.getTimescale()
        set(timescale) {
            (findFirstPath(this, Box.path("mdia.mdhd")) as? MediaHeaderBox?)!!.setTimescale(timescale)
        }

    fun rescale(tv: Long, ts: Long): Long {
        return tv * timescale / ts
    }

    var duration: Long
        get() = trackHeader.getDuration()
        set(duration) {
            trackHeader.setDuration(duration)
        }

    var mediaDuration: Long
        get() = (findFirstPath(this, Box.path("mdia.mdhd")) as? MediaHeaderBox?)!!.getDuration()
        set(duration) {
            (findFirstPath(this, Box.path("mdia.mdhd")) as? MediaHeaderBox?)!!.setDuration(duration)
        }

    val isPureRef: Boolean
        get() {
            val minf = mdia.minf
            val dinf = minf.dinf ?: return false
            val dref = dinf.dref ?: return false
            for (box in dref.getBoxes()) {
                if ((box as FullBox).flags and 0x1 != 0) return false
            }
            return true
        }

    fun hasDataRef(): Boolean {
        val dinf = mdia.minf.dinf ?: return false
        val dref = dinf.dref ?: return false
        var result = false
        for (box in dref.getBoxes()) {
            result = result or ((box as FullBox).flags and 0x1 != 0x1)
        }
        return result
    }

    var pAR: Rational?
        get() {
            val pasp = findFirstPath(this, arrayOf("mdia", "minf", "stbl", "stsd", null, "pasp")) as? PixelAspectExt?
            return pasp?.rational ?: Rational(1, 1)
        }
        set(par) {
            val sampleEntries = sampleEntries
            for (i in sampleEntries.indices) {
                val sampleEntry = sampleEntries[i]
                sampleEntry.removeChildren(arrayOf("pasp"))
                sampleEntry.add(createPixelAspectExt(par!!))
            }
        }

    val sampleEntries: Array<SampleEntry>
        get() = findAllPath(this, arrayOf("mdia", "minf", "stbl", "stsd", null)).map { MP4Demuxer.asSampleEntry(it)!! }.toTypedArray()

    fun setClipRect(x: Short, y: Short, width: Short, height: Short) {
        var clip = findFirst(this, "clip") as? NodeBox?
        if (clip == null) {
            clip = NodeBox(Header("clip"))
            add(clip)
        }
        clip.replace("crgn", createClipRegionBox(x, y, width, height))
    }

    val sampleCount: Long
        get() = (findFirstPath(this, Box.path("mdia.minf.stbl.stsz")) as? SampleSizesBox?)!!.getCount().toLong()

    fun setAperture(sar: Size, dar: Size) {
        removeChildren(arrayOf("tapt"))
        val tapt = NodeBox(Header("tapt"))
        tapt.add(createClearApertureBox(dar.width, dar.height))
        tapt.add(createProductionApertureBox(dar.width, dar.height))
        tapt.add(createEncodedPixelBox(sar.width, sar.height))
        add(tapt)
    }

    fun setDimensions(dd: Size) {
        trackHeader.setWidth(dd.width.toFloat())
        trackHeader.setHeight(dd.height.toFloat())
    }

    val frameCount: Int
        get() {
            val stsz = (findFirstPath(this, Box.path("mdia.minf.stbl.stsz")) as? SampleSizesBox?)!!
            return if (stsz.defaultSize != 0) stsz.getCount() else stsz.getSizes().size
        }

    var name: String?
        get() {
            val nb = findFirstPath(this, Box.path("udta.name")) as? NameBox?
            return nb?.name
        }
        set(string) {
            var udta = findFirst(this, "udta") as? NodeBox?
            if (udta == null) {
                udta = NodeBox(Header("udta"))
                add(udta)
            }
            udta.removeChildren(arrayOf("name"))
            udta.add(createNameBox(string))
        }

    fun fixMediaTimescale(ts: Int) {
        val mdhd = (findFirstPath(this, Box.path("mdia.mdhd")) as? MediaHeaderBox?)!!
        val oldTs = mdhd.getTimescale()
        mdhd.setTimescale(ts)
        mdhd.setDuration(ts * mdhd.getDuration() / oldTs)
        val edits = edits
        if (edits != null) {
            for (edit in edits) {
                edit.mediaTime = ts * edit.mediaTime / oldTs
            }
        }
        val tts = (findFirstPath(this, Box.path("mdia.minf.stbl.stts")) as? TimeToSampleBox?)!!
        val entries = tts.getEntries()
        for (i in entries.indices) {
            val tte = entries[i]
            tte!!.sampleDuration = ts * tte.sampleDuration / oldTs
        }
    }

    /**
     * Retrieves coded size of this video track.
     *
     * Note: May be different from video display dimension.
     *
     * @return
     */
    val codedSize: Size
        get() {
            val se = sampleEntries[0]
            require(se is VideoSampleEntry) { "Not a video track" }
            val vse = se
            return Size(vse.width.toInt(), vse.height.toInt())
        }

    val stts: TimeToSampleBox
        get() = (findFirstPath(this, Box.path("mdia.minf.stbl.stts")) as? TimeToSampleBox?)!!

    val stco: ChunkOffsetsBox
        get() = (findFirstPath(this, Box.path("mdia.minf.stbl.stco")) as? ChunkOffsetsBox?)!!

    val co64: ChunkOffsets64Box?
        get() = (findFirstPath(this, Box.path("mdia.minf.stbl.co64")) as? ChunkOffsets64Box?)

    val stsz: SampleSizesBox
        get() = (findFirstPath(this, Box.path("mdia.minf.stbl.stsz")) as? SampleSizesBox)!!

    val stbl: NodeBox
        get() = findFirstPath(this, Box.path("mdia.minf.stbl")) as NodeBox

    val stsc: SampleToChunkBox
        get() = findFirstPath(this, Box.path("mdia.minf.stbl.stsc")) as SampleToChunkBox

    val stsd: SampleDescriptionBox
        get() = findFirstPath(this, Box.path("mdia.minf.stbl.stsd")) as SampleDescriptionBox

    val stss: SyncSamplesBox
        get() = findFirstPath(this, Box.path("mdia.minf.stbl.stss")) as SyncSamplesBox

    val ctts: CompositionOffsetsBox
        get() = findFirstPath(this, Box.path("mdia.minf.stbl.ctts")) as CompositionOffsetsBox

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "trak"
        }

        @JvmStatic
        fun createTrakBox(): TrakBox {
            return TrakBox(Header(fourcc()))
        }

        @JvmStatic
        fun getTrackType(trak: TrakBox?): MP4TrackType? {
            val handler = findFirstPath(trak, Box.path("mdia.hdlr")) as? HandlerBox?
            return if (handler == null) null else MP4TrackType.fromHandler(handler.componentSubType)
        }

        /**
         * Calculates track duration considering edits
         *
         * @param track
         * @return
         */
        fun getEditedDuration(track: TrakBox): Long {
            val edits = track.edits ?: return track.duration
            var duration: Long = 0
            for (edit in edits) {
                duration += edit.duration
            }
            return duration
        }
    }
}