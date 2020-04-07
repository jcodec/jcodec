package org.jcodec.common

import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * The list of formats known to JCodec
 *
 * @author The JCodec project
 */
class Format private constructor(private val name: String, val isVideo: Boolean, val isAudio: Boolean, val isContained: Boolean) {
    companion object {
        @JvmField
        val MOV = Format("MOV", true, true, true)
        @JvmField
        val MPEG_PS = Format("MPEG_PS", true, true, true)
        @JvmField
        val MPEG_TS = Format("MPEG_TS", true, true, true)
        @JvmField
        val MKV = Format("MKV", true, true, true)
        @JvmField
        val H264 = Format("H264", true, false, true)
        @JvmField
        val RAW = Format("RAW", true, true, true)
        @JvmField
        val FLV = Format("FLV", true, true, true)
        @JvmField
        val AVI = Format("AVI", true, true, true)
        @JvmField
        val IMG = Format("IMG", true, false, false)
        @JvmField
        val IVF = Format("IVF", true, false, true)
        @JvmField
        val MJPEG = Format("MJPEG", true, false, true)
        @JvmField
        val Y4M = Format("Y4M", true, false, true)
        @JvmField
        val WAV = Format("WAV", false, true, true)
        @JvmField
        val WEBP = Format("WEBP", true, false, true)
        @JvmField
        val MPEG_AUDIO = Format("MPEG_AUDIO", false, true, true)
        @JvmField
        val DASH = Format("DASH", true, true, false)
        @JvmField
        val DASHURL = Format("DASHURL", true, true, false)
        private val _values: MutableMap<String, Format> = LinkedHashMap()
        @JvmStatic
        fun valueOf(s: String): Format? {
            return _values[s]
        }

        init {
            _values["MOV"] = MOV
            _values["MPEG_PS"] = MPEG_PS
            _values["MPEG_TS"] = MPEG_TS
            _values["MKV"] = MKV
            _values["H264"] = H264
            _values["RAW"] = RAW
            _values["FLV"] = FLV
            _values["AVI"] = AVI
            _values["IMG"] = IMG
            _values["IVF"] = IVF
            _values["MJPEG"] = MJPEG
            _values["Y4M"] = Y4M
            _values["WAV"] = WAV
            _values["WEBP"] = WEBP
            _values["MPEG_AUDIO"] = MPEG_AUDIO
            _values["DASH"] = DASH
            _values["DASHURL"] = DASHURL
        }
    }

}