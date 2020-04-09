package org.jcodec.api.transcode.filters

import org.jcodec.api.transcode.Filter
import org.jcodec.api.transcode.PixelStore
import org.jcodec.api.transcode.PixelStore.LoanerPicture
import org.jcodec.common.logging.Logger
import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Picture
import org.jcodec.scale.ColorUtil
import org.jcodec.scale.Transform

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Color transform filter.
 *
 * @author Stanislav Vitvitskyy
 */
class ColorTransformFilter(private val outputColor: ColorSpace) : Filter {
    private var transform: Transform? = null
    override fun filter(picture: Picture, store: PixelStore): LoanerPicture {
        if (transform == null) {
            transform = ColorUtil.getTransform(picture.color, outputColor)
            Logger.debug("Creating transform: $transform")
        }
        val outFrame = store.getPicture(picture.width, picture.height, outputColor)
        outFrame!!.picture.crop = picture.crop
        transform!!.transform(picture, outFrame.picture)
        return outFrame
    }

    override fun getInputColor(): ColorSpace {
        return ColorSpace.ANY_PLANAR
    }

    override fun getOutputColor(): ColorSpace {
        return outputColor
    }

}