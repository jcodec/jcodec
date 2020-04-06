package org.jcodec.codecs.h264.encode

import org.jcodec.codecs.h264.io.model.SliceType
import org.jcodec.common.model.Size
import org.jcodec.common.tools.MathUtil

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * H.264 rate control policy that would produce frames of exactly equal size
 *
 * @author The JCodec project
 */
class H264FixedRateControl(private var perMb: Int) : RateControl {
    private var balance = 0
    private var curQp: Int
    override fun startPicture(sz: Size, maxSize: Int, sliceType: SliceType): Int {
        return INIT_QP + if (sliceType == SliceType.P) 4 else 0
    }

    override fun initialQpDelta(): Int {
        val qpDelta = if (balance < 0) if (balance < -(perMb shr 1)) 2 else 1 else if (balance > perMb) if (balance > perMb shl 2) -2 else -1 else 0
        val prevQp = curQp
        curQp = MathUtil.clip(curQp + qpDelta, 12, 30)
        return curQp - prevQp
    }

    override fun accept(bits: Int): Int {
        balance += perMb - bits
        return 0
    }

    fun reset() {
        balance = 0
        curQp = INIT_QP
    }

    fun calcFrameSize(nMB: Int): Int {
        return (256 + nMB * (perMb + 9) shr 3) + (nMB shr 6)
    }

    fun setRate(rate: Int) {
        perMb = rate
    }

    companion object {
        private const val INIT_QP = 26
    }

    init {
        curQp = INIT_QP
    }
}