package org.jcodec.codecs.vpx

import org.jcodec.common.and
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Containes boolean encoder from VPx codecs
 *
 * @author The JCodec project
 */
open class VPXBooleanEncoder(private val out: ByteBuffer) {
    private var lowvalue = 0
    private var range = 255
    private var count: Int
    open fun writeBit(prob: Int, bb: Int) {
        val split = 1 + ((range - 1) * prob shr 8)
        if (bb != 0) {
            lowvalue += split
            range -= split
        } else {
            range = split
        }
        var shift = VPXConst.vp8Norm[range]
        range = range shl shift
        count += shift
        if (count >= 0) {
            val offset = shift - count
            if (lowvalue shl offset - 1 and -0x80000000 != 0) {
                var x = out.position() - 1
                while (x >= 0 && out[x].toInt() == -1) {
                    out.put(x, 0.toByte())
                    x--
                }
                out.put(x, ((out[x] and 0xff) + 1).toByte())
            }
            out.put((lowvalue shr 24 - offset).toByte())
            lowvalue = lowvalue shl offset
            shift = count
            lowvalue = lowvalue and 0xffffff
            count -= 8
        }
        lowvalue = lowvalue shl shift
    }

    fun stop() {
        var i: Int
        i = 0
        while (i < 32) {
            writeBit(128, 0)
            i++
        }
    }

    fun position(): Int {
        return out.position() + (count + 24 shr 3)
    }

    init {
        count = -24
    }
}