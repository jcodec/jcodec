package org.jcodec.containers.mp4.demuxer

import org.jcodec.codecs.aac.AACUtils
import org.jcodec.codecs.h264.H264Utils
import org.jcodec.codecs.h264.io.model.SeqParameterSet
import org.jcodec.common.*
import org.jcodec.common.model.ColorSpace
import org.jcodec.containers.mp4.BoxUtil
import org.jcodec.containers.mp4.MP4TrackType
import org.jcodec.containers.mp4.boxes.*
import org.jcodec.containers.mp4.boxes.Box.Companion.path
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirst
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirstPath
import org.jcodec.platform.Platform
import java.nio.ByteBuffer

class MP4DemuxerTrackMeta(type: TrackType?, codec: Codec?, totalDuration: Double, seekFrames: IntArray?, totalFrames: Int,
                          codecPrivate: ByteBuffer?, videoCodecMeta: VideoCodecMeta?, audioCodecMeta: AudioCodecMeta?,
                          val sampleEntries: Array<SampleEntry?>?, val codecPrivateOpaque: ByteBuffer?) : DemuxerTrackMeta(type, codec, totalDuration, seekFrames, totalFrames, codecPrivate, videoCodecMeta, audioCodecMeta) {

    companion object {
        fun fromTrack(track: AbstractMP4DemuxerTrack): DemuxerTrackMeta {
            val trak = track.box
            val stss = findFirstPath(trak, path("mdia.minf.stbl.stss")) as SyncSamplesBox?
            val syncSamples = stss?.syncSamples
            val seekFrames: IntArray
            if (syncSamples == null) {
                // all frames are I-frames
                seekFrames = IntArray(track.frameCount.toInt())
                for (i in seekFrames.indices) {
                    seekFrames[i] = i
                }
            } else {
                seekFrames = Platform.copyOfInt(syncSamples, syncSamples.size)
                for (i in seekFrames.indices) seekFrames[i]--
            }
            val type = track.type
            val t = if (type == null) TrackType.OTHER else type.trackType
            val videoCodecMeta = getVideoCodecMeta(track, trak, type)
            val audioCodecMeta = getAudioCodecMeta(track, type)
            val duration = track.getDuration()
            val sec = duration.num.toDouble() / duration.den
            val frameCount = Ints.checkedCast(track.frameCount)
            val opaque = getCodecPrivateOpaque(Codec.codecByFourcc(track.fourcc),
                    track.sampleEntries!![0])
            val meta = MP4DemuxerTrackMeta(t, Codec.codecByFourcc(track.fourcc), sec, seekFrames,
                    frameCount, getCodecPrivate(track), videoCodecMeta, audioCodecMeta, track.sampleEntries, opaque)
            meta.index = track.box.trackHeader.getNo()
            if (type == MP4TrackType.VIDEO) {
                val tkhd = findFirstPath(trak, path("tkhd")) as TrackHeaderBox?
                val orientation: Orientation
                orientation = if (tkhd!!.isOrientation90) Orientation.D_90 else if (tkhd.isOrientation180) Orientation.D_180 else if (tkhd.isOrientation270) Orientation.D_270 else Orientation.D_0
                meta.orientation = orientation
            }
            return meta
        }

        fun getCodecPrivateOpaque(codec: Codec?, se: SampleEntry?): ByteBuffer? {
            if (codec == Codec.H264) {
                val b = findFirst(se, "avcC")
                return if (b != null) BoxUtil.writeBox(b) else null
            } else if (codec == Codec.AAC) {
                var b = findFirst(se, "esds")
                if (b == null) {
                    b = findFirstPath(se, arrayOf(null, "esds"))
                }
                return if (b != null) BoxUtil.writeBox(b) else null
            }
            return null
        }

        private fun getAudioCodecMeta(track: AbstractMP4DemuxerTrack, type: MP4TrackType?): AudioCodecMeta? {
            var audioCodecMeta: AudioCodecMeta? = null
            if (type == MP4TrackType.SOUND) {
                val ase = track.sampleEntries!![0] as AudioSampleEntry
                audioCodecMeta = AudioCodecMeta.fromAudioFormat(ase.format)
            }
            return audioCodecMeta
        }

        private fun getVideoCodecMeta(track: AbstractMP4DemuxerTrack, trak: TrakBox, type: MP4TrackType?): VideoCodecMeta? {
            var videoCodecMeta: VideoCodecMeta? = null
            if (type == MP4TrackType.VIDEO) {
                videoCodecMeta = VideoCodecMeta.createSimpleVideoCodecMeta(trak.codedSize, getColorInfo(track))
                val pasp = findFirst(track.sampleEntries!![0], "pasp") as PixelAspectExt?
                if (pasp != null) videoCodecMeta.pixelAspectRatio = pasp.rational
            }
            return videoCodecMeta
        }

        protected fun getColorInfo(track: AbstractMP4DemuxerTrack): ColorSpace? {
            val codec = Codec.codecByFourcc(track.fourcc)
            if (codec == Codec.H264) {
                val avcC = H264Utils.parseAVCC(track.sampleEntries!![0] as VideoSampleEntry?)
                if (avcC != null) {
                    val spsList = avcC.getSpsList()
                    if (spsList.size > 0) {
                        val sps = SeqParameterSet.read(spsList[0].duplicate())
                        return sps.chromaFormatIdc
                    }
                }
            }
            return null
        }

        fun getCodecPrivate(track: AbstractMP4DemuxerTrack): ByteBuffer? {
            val codec = Codec.codecByFourcc(track.fourcc)
            if (codec == Codec.H264) {
                val avcC = H264Utils.parseAVCC(track.sampleEntries!![0] as VideoSampleEntry?)
                return if (avcC != null) H264Utils.avcCToAnnexB(avcC) else null
            } else if (codec == Codec.AAC) {
                return AACUtils.getCodecPrivate(track.sampleEntries!![0])
            }
            // This codec does not have private section
            return null
        }
    }

}