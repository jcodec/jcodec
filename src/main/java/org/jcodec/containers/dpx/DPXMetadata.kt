package org.jcodec.containers.dpx

import org.jcodec.common.StringUtils

class DPXMetadata {
    @JvmField
    var file: FileHeader? = null
    @JvmField
    var image: ImageHeader? = null
    @JvmField
    var imageSource: ImageSourceHeader? = null
    @JvmField
    var film: FilmHeader? = null
    @JvmField
    var television: TelevisionHeader? = null
    @JvmField
    var userId: String? = null
    val timecodeString: String
        get() = smpteTC(television!!.timecode, false)

    companion object {
        const val V2 = "V2.0"
        const val V1 = "V1.0"
        private fun smpteTC(tcsmpte: Int, prevent_dropframe: Boolean): String {
            val ff = bcd2uint(tcsmpte and 0x3f) // 6-bit hours
            val ss = bcd2uint(tcsmpte shr 8 and 0x7f) // 7-bit minutes
            val mm = bcd2uint(tcsmpte shr 16 and 0x7f) // 7-bit seconds
            val hh = bcd2uint(tcsmpte shr 24 and 0x3f) // 6-bit frames
            val drop = tcsmpte and 1 shl 30 > 0L && !prevent_dropframe // 1-bit drop if not arbitrary bit
            return StringUtils.zeroPad2(hh) + ":" +
                    StringUtils.zeroPad2(mm) + ":" +
                    StringUtils.zeroPad2(ss) + (if (drop) ";" else ":") +
                    StringUtils.zeroPad2(ff)
        }

        private fun bcd2uint(bcd: Int): Int {
            val low = bcd and 0xf
            val high = bcd shr 4
            return if (low > 9 || high > 9) 0 else low + 10 * high
        }
    }
}