package org.jcodec.common

import org.jcodec.platform.Platform

fun Int.fourcc(): String = Fourcc.dwToFourCC(this)
fun String.fourcc(): Int = Fourcc.intFourcc(this)

object Fourcc {
    fun makeInt(b3: Byte, b2: Byte, b1: Byte, b0: Byte): Int {
        return b3 shl 24 or
                (b2 and 0xff shl 16) or
                (b1 and 0xff shl 8) or
                (b0 and 0xff)
    }

    fun intFourcc(string: String?): Int {
        val b = Platform.getBytes(string)
        return makeInt(b[0], b[1], b[2], b[3])
    }

    fun dwToFourCC(fourCC: Int): String {
        val ch = CharArray(4)
        ch[0] = (fourCC shr 24 and 0xff).toChar()
        ch[1] = (fourCC shr 16 and 0xff).toChar()
        ch[2] = (fourCC shr 8 and 0xff).toChar()
        ch[3] = (fourCC shr 0 and 0xff).toChar()
        return Platform.stringFromChars(ch)
    }

    val ftyp = intFourcc("ftyp")
    val free = intFourcc("free")
    val moov = intFourcc("moov")
    val mdat = intFourcc("mdat")
    val wide = intFourcc("wide")
}