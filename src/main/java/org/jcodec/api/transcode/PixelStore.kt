package org.jcodec.api.transcode

import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Picture

interface PixelStore {
    class LoanerPicture(val picture: Picture, var refCnt: Int) {

        fun decRefCnt() {
            --refCnt
        }

        fun unused(): Boolean {
            return refCnt <= 0
        }

        fun incRefCnt() {
            ++refCnt
        }

    }

    fun getPicture(width: Int, height: Int, color: ColorSpace): LoanerPicture
    fun putBack(frame: LoanerPicture)
    fun retake(frame: LoanerPicture)
}