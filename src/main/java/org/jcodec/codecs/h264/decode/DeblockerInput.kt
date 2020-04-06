package org.jcodec.codecs.h264.decode

import org.jcodec.codecs.h264.H264Utils.MvList2D
import org.jcodec.codecs.h264.io.model.Frame
import org.jcodec.codecs.h264.io.model.MBType
import org.jcodec.codecs.h264.io.model.SeqParameterSet
import org.jcodec.codecs.h264.io.model.SeqParameterSet.Companion.getPicHeightInMbs
import org.jcodec.codecs.h264.io.model.SliceHeader

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Contains an input for deblocking filter
 *
 * @author The JCodec project
 */
class DeblockerInput(activeSps: SeqParameterSet) {
    @JvmField
    var nCoeff: Array<IntArray>
    @JvmField
    var mvs: MvList2D
    @JvmField
    var mbTypes: Array<MBType?>
    @JvmField
    var mbQps: Array<IntArray>
    @JvmField
    var tr8x8Used: BooleanArray
    @JvmField
    var refsUsed: Array<Array<Array<Frame?>?>?>
    @JvmField
    var shs: Array<SliceHeader?>

    init {
        val picWidthInMbs = activeSps.picWidthInMbsMinus1 + 1
        val picHeightInMbs = getPicHeightInMbs(activeSps)
        nCoeff = Array(picHeightInMbs shl 2) { IntArray(picWidthInMbs shl 2) }
        mvs = MvList2D(picWidthInMbs shl 2, picHeightInMbs shl 2)
        mbTypes = arrayOfNulls(picHeightInMbs * picWidthInMbs)
        tr8x8Used = BooleanArray(picHeightInMbs * picWidthInMbs)
        mbQps = Array(3) { IntArray(picHeightInMbs * picWidthInMbs) }
        shs = arrayOfNulls(picHeightInMbs * picWidthInMbs)
        refsUsed = arrayOfNulls(picHeightInMbs * picWidthInMbs)
    }
}