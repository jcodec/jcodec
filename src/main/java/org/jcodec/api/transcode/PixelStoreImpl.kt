package org.jcodec.api.transcode

import org.jcodec.api.transcode.PixelStore.LoanerPicture
import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Picture
import java.util.*

class PixelStoreImpl : PixelStore {
    private val buffers: MutableList<Picture>
    override fun getPicture(width: Int, height: Int, color: ColorSpace): LoanerPicture {
        for (picture in buffers) {
            if (picture.width == width && picture.height == height && picture.color == color) {
                buffers.remove(picture)
                return LoanerPicture(picture, 1)
            }
        }
        return LoanerPicture(Picture.create(width, height, color), 1)
    }

    override fun putBack(frame: LoanerPicture) {
        frame.decRefCnt()
        if (frame.unused()) {
            val pixels = frame.picture
            pixels.crop = null
            buffers.add(pixels)
        }
    }

    override fun retake(frame: LoanerPicture) {
        frame.incRefCnt()
    }

    init {
        buffers = ArrayList()
    }
}