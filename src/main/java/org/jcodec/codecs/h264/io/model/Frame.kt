package org.jcodec.codecs.h264.io.model

import org.jcodec.codecs.h264.H264Utils.MvList2D
import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Picture
import org.jcodec.common.model.Rect
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Picture extension with frame number, makes it easier to debug reordering
 *
 * @author The JCodec project
 */
class Frame(width: Int, height: Int, data: Array<ByteArray?>?, color: ColorSpace?, crop: Rect?, var frameNo: Int, frameType: SliceType?,
            var mvs: MvList2D, var refsUsed: Array<Array<Array<Frame?>?>?>, var pOC: Int) : Picture(width, height, data, null, color, 0, crop) {
    val frameType: SliceType? = null
    var isShortTerm = true
    override fun cropped(): Frame {
        val cropped = super.cropped()
        return Frame(cropped.width, cropped.height, cropped.data, cropped.color, null, frameNo,
                frameType, mvs, refsUsed, pOC)
    }

    fun copyFromFrame(src: Frame) {
        super.copyFrom(src)
        frameNo = src.frameNo
        mvs = src.mvs
        isShortTerm = src.isShortTerm
        refsUsed = src.refsUsed
        pOC = src.pOC
    }

    /**
     * Creates a cropped clone of this picture.
     *
     * @return
     */
    override fun cloneCropped(): Frame {
        return if (cropNeeded()) {
            cropped()
        } else {
            val clone = createFrame(this)
            clone.copyFrom(this)
            clone
        }
    }

    companion object {
        @JvmStatic
        fun createFrame(pic: Frame): Frame {
            val comp = pic.createCompatible()
            return Frame(comp.width, comp.height, comp.data, comp.color, pic.crop,
                    pic.frameNo, pic.frameType, pic.mvs, pic.refsUsed, pic.pOC)
        }

        @JvmField
        var POCAsc: Comparator<Frame?> = Comparator { o1, o2 -> if (o1 == null && o2 == null) 0 else if (o1 == null) 1 else if (o2 == null) -1 else if (o1.pOC > o2.pOC) 1 else if (o1.pOC == o2.pOC) 0 else -1 }
        @JvmField
        var POCDesc: Comparator<Frame?> = Comparator { o1, o2 -> if (o1 == null && o2 == null) 0 else if (o1 == null) 1 else if (o2 == null) -1 else if (o1.pOC < o2.pOC) 1 else if (o1.pOC == o2.pOC) 0 else -1 }
    }

}