package org.jcodec.codecs.h264.encode

import org.jcodec.codecs.h264.io.model.SliceType
import org.jcodec.common.model.Size

class CQPRateControl(private val qp: Int) : RateControl {
    override fun startPicture(sz: Size, maxSize: Int, sliceType: SliceType): Int {
        return qp
    }

    override fun initialQpDelta(): Int {
        return 0
    }

    override fun accept(bits: Int): Int {
        return 0
    }

}