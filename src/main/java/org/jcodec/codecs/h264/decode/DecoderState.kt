package org.jcodec.codecs.h264.decode

import org.jcodec.codecs.h264.H264Utils.MvList
import org.jcodec.codecs.h264.io.model.SliceHeader
import org.jcodec.common.model.ColorSpace

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Current state of the decoder, this data is accessed from many methods
 *
 * @author The JCodec project
 */
class DecoderState(sh: SliceHeader) {
    @JvmField
    var chromaQpOffset: IntArray
    @JvmField
    var qp: Int
    @JvmField
    var leftRow: Array<ByteArray>
    @JvmField
    var topLine: Array<ByteArray>
    @JvmField
    var topLeft: Array<ByteArray>
    @JvmField
    var chromaFormat: ColorSpace?
    @JvmField
    var mvTop: MvList
    @JvmField
    var mvLeft: MvList
    @JvmField
    var mvTopLeft: MvList

    init {
        val mbWidth = sh.sps!!.picWidthInMbsMinus1 + 1
        chromaQpOffset = intArrayOf(sh.pps!!.chromaQpIndexOffset,
                if (sh.pps!!.extended != null) sh.pps!!.extended!!.secondChromaQpIndexOffset else sh.pps!!.chromaQpIndexOffset)
        chromaFormat = sh.sps!!.chromaFormatIdc
        mvTop = MvList((mbWidth shl 2) + 1)
        mvLeft = MvList(4)
        mvTopLeft = MvList(1)
        leftRow = Array(3) { ByteArray(16) }
        topLeft = Array(3) { ByteArray(4) }
        topLine = Array(3) { ByteArray(mbWidth shl 4) }
        qp = sh.pps!!.picInitQpMinus26 + 26 + sh.sliceQpDelta
    }
}