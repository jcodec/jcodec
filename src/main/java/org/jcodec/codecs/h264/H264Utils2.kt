package org.jcodec.codecs.h264

object H264Utils2 {
    fun golomb2Signed(v: Int): Int {
        var _v = v
        val sign = (_v and 0x1 shl 1) - 1
        _v = ((_v shr 1) + (_v and 0x1)) * sign
        return _v
    }
}