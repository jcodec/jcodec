package org.jcodec.common

import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class Codec(private val _name: String, val type: TrackType?, val isPcm: Boolean) {
    companion object {
        @JvmField
        val H265 = Codec("H265", TrackType.VIDEO, false)

        @JvmField
        val H264 = Codec("H264", TrackType.VIDEO, false)

        @JvmField
        val MPEG2 = Codec("MPEG2", TrackType.VIDEO, false)

        @JvmField
        val MPEG4 = Codec("MPEG4", TrackType.VIDEO, false)

        @JvmField
        val PRORES = Codec("PRORES", TrackType.VIDEO, false)
        val DV = Codec("DV", TrackType.VIDEO, false)
        val VC1 = Codec("VC1", TrackType.VIDEO, false)
        val VC3 = Codec("VC3", TrackType.VIDEO, false)
        val V210 = Codec("V210", TrackType.VIDEO, false)

        @JvmField
        val SORENSON = Codec("SORENSON", TrackType.VIDEO, false)

        @JvmField
        val FLASH_SCREEN_VIDEO = Codec("FLASH_SCREEN_VIDEO", TrackType.VIDEO, false)

        @JvmField
        val FLASH_SCREEN_V2 = Codec("FLASH_SCREEN_V2", TrackType.VIDEO, false)

        @JvmField
        val PNG = Codec("PNG", TrackType.VIDEO, false)

        @JvmField
        val JPEG = Codec("JPEG", TrackType.VIDEO, false)
        val J2K = Codec("J2K", TrackType.VIDEO, false)

        @JvmField
        val VP6 = Codec("VP6", TrackType.VIDEO, false)

        @JvmField
        val VP8 = Codec("VP8", TrackType.VIDEO, false)

        @JvmField
        val VP9 = Codec("VP9", TrackType.VIDEO, false)

        @JvmField
        val VORBIS = Codec("VORBIS", TrackType.VIDEO, false)

        @JvmField
        val AAC = Codec("AAC", TrackType.AUDIO, false)

        @JvmField
        val MP3 = Codec("MP3", TrackType.AUDIO, false)

        @JvmField
        val MP2 = Codec("MP2", TrackType.AUDIO, false)

        @JvmField
        val MP1 = Codec("MP1", TrackType.AUDIO, false)

        @JvmField
        val AC3 = Codec("AC3", TrackType.AUDIO, false)

        @JvmField
        val DTS = Codec("DTS", TrackType.AUDIO, false)

        @JvmField
        val TRUEHD = Codec("TRUEHD", TrackType.AUDIO, false)

        @JvmField
        val PCM_DVD = Codec("PCM_DVD", TrackType.AUDIO, true)

        @JvmField
        val PCM = Codec("PCM", TrackType.AUDIO, true)

        @JvmField
        val ADPCM = Codec("ADPCM", TrackType.AUDIO, false)
        val ALAW = Codec("ALAW", TrackType.AUDIO, true)

        @JvmField
        val NELLYMOSER = Codec("NELLYMOSER", TrackType.AUDIO, false)

        @JvmField
        val G711 = Codec("G711", TrackType.AUDIO, false)

        @JvmField
        val SPEEX = Codec("SPEEX", TrackType.AUDIO, false)

        @JvmField
        val RAW = Codec("RAW", null, false)
        val TIMECODE = Codec("TIMECODE", TrackType.OTHER, false)
        private val _values: MutableMap<String, Codec> = LinkedHashMap()
        fun codecByFourcc(fourcc: String?): Codec? {
            return when (fourcc) {
                "hev1" -> H265
                "avc1" -> H264
                "m1v1", "m2v1" -> MPEG2
                "apco", "apcs", "apcn", "apch", "ap4h" -> PRORES
                "mp4a" -> AAC
                "jpeg" -> JPEG
                else -> null
            }
        }

        @JvmStatic
        fun valueOf(s: String): Codec? {
            return _values[s]
        }

        init {
            _values["H264"] = H264
            _values["MPEG2"] = MPEG2
            _values["MPEG4"] = MPEG4
            _values["PRORES"] = PRORES
            _values["DV"] = DV
            _values["VC1"] = VC1
            _values["VC3"] = VC3
            _values["V210"] = V210
            _values["SORENSON"] = SORENSON
            _values["FLASH_SCREEN_VIDEO"] = FLASH_SCREEN_VIDEO
            _values["FLASH_SCREEN_V2"] = FLASH_SCREEN_V2
            _values["PNG"] = PNG
            _values["JPEG"] = JPEG
            _values["J2K"] = J2K
            _values["VP6"] = VP6
            _values["VP8"] = VP8
            _values["VP9"] = VP9
            _values["VORBIS"] = VORBIS
            _values["AAC"] = AAC
            _values["MP3"] = MP3
            _values["MP2"] = MP2
            _values["MP1"] = MP1
            _values["AC3"] = AC3
            _values["DTS"] = DTS
            _values["TRUEHD"] = TRUEHD
            _values["PCM_DVD"] = PCM_DVD
            _values["PCM"] = PCM
            _values["ADPCM"] = ADPCM
            _values["ALAW"] = ALAW
            _values["NELLYMOSER"] = NELLYMOSER
            _values["G711"] = G711
            _values["SPEEX"] = SPEEX
            _values["RAW"] = RAW
            _values["TIMECODE"] = TIMECODE
        }
    }

    override fun toString(): String {
        return _name
    }

    fun name(): String {
        return _name
    }

}