package org.jcodec.codecs.h264.encode

import org.jcodec.codecs.h264.io.model.SliceType
import org.jcodec.common.model.Size

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Dumb rate control policy, always maintains the same QP for the whole video
 *
 * @author The JCodec project
 */
class DumbRateControl : RateControl {
    private var bitsPerMb = 0
    private var totalQpDelta = 0
    private var justSwitched = false
    override fun accept(bits: Int): Int {
        return if (bits >= bitsPerMb) {
            totalQpDelta++
            justSwitched = true
            1
        } else {
            // Only decrease qp if we got too few bits (more then 12.5%)
            if (totalQpDelta > 0 && !justSwitched && bitsPerMb - bits > bitsPerMb shr 3) {
                --totalQpDelta
                justSwitched = true
                return -1
            } else {
                justSwitched = false
            }
            0
        }
    }

    override fun startPicture(sz: Size, maxSize: Int, sliceType: SliceType): Int {
        val totalMb = (sz.width + 15 shr 4) * (sz.height + 15 shr 4)
        bitsPerMb = (maxSize shl 3) / totalMb
        totalQpDelta = 0
        justSwitched = false
        return QP + if (sliceType == SliceType.P) 6 else 0
    }

    override fun initialQpDelta(): Int {
        return 0
    }

    companion object {
        private const val QP = 20
    }
}