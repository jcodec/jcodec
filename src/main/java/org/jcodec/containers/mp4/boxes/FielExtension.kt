package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class FielExtension(header: Header) : Box(header) {
    private var type = 0
    private var order = 0
    val isInterlaced: Boolean
        get() = type == 2

    fun topFieldFirst(): Boolean {
        return order == 1 || order == 6
    }// 14  T is displayed earliest, B is stored first in the
    // file.
// 9  B is displayed earliest, T is stored first in the file.// 6 B is displayed earliest, B is stored first in the file.

    // 1 T is displayed earliest, T is stored first in the file.
    val orderInterpretation: String
        get() {
            if (isInterlaced) when (order) {
                1 ->                 // 1 T is displayed earliest, T is stored first in the file.
                    return "top"
                6 ->                 // 6 B is displayed earliest, B is stored first in the file.
                    return "bottom"
                9 ->                 // 9  B is displayed earliest, T is stored first in the file.
                    return "bottomtop"
                14 ->                 // 14  T is displayed earliest, B is stored first in the
                    // file.
                    return "topbottom"
            }
            return ""
        }

    override fun parse(input: ByteBuffer) {
        type = input.get().toInt() and 0xff
        if (isInterlaced) {
            order = input.get().toInt() and 0xff
        }
    }

    public override fun doWrite(out: ByteBuffer) {
        out.put(type.toByte())
        out.put(order.toByte())
    }

    override fun estimateSize(): Int {
        return 2 + 8
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "fiel"
        }
    }
}