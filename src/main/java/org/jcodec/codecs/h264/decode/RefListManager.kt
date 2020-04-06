package org.jcodec.codecs.h264.decode

import org.jcodec.codecs.h264.H264Const
import org.jcodec.codecs.h264.io.model.Frame
import org.jcodec.codecs.h264.io.model.SliceHeader
import org.jcodec.codecs.h264.io.model.SliceType
import org.jcodec.common.IntObjectMap
import org.jcodec.common.model.Picture
import org.jcodec.common.tools.MathUtil
import org.jcodec.platform.Platform
import java.util.*

/**
 * Contains reference picture list management logic
 *
 * @author The JCodec Project
 */
class RefListManager(private val sh: SliceHeader, private val sRefs: Array<Frame?>, private val lRefs: IntObjectMap<Frame>, frameOut: Frame) {
    private val numRef: IntArray
    private val frameOut: Frame
    val refList: Array<Array<Frame?>?>?
        get() {
            var refList: Array<Array<Frame?>?>? = null
            if (sh.sliceType == SliceType.P) {
                refList = arrayOf(buildRefListP(), null)
            } else if (sh.sliceType == SliceType.B) {
                refList = buildRefListB()
            }
            MBlockDecoderUtils.debugPrint("------")
            if (refList != null) {
                for (l in 0..1) {
                    if (refList[l] != null) for (i in 0 until refList[l]!!.size) if (refList[l]!![i] != null) MBlockDecoderUtils.debugPrint("REF[%d][%d]: ", l, i, refList[l]!![i]!!.pOC)
                }
            }
            return refList
        }

    private fun buildRefListP(): Array<Frame?> {
        val frame_num = sh.frameNum
        val maxFrames = 1 shl sh.sps!!.log2MaxFrameNumMinus4 + 4
        // int nLongTerm = Math.min(lRefs.size(), numRef[0] - 1);
        val result = arrayOfNulls<Frame>(numRef[0])
        var refs = 0
        run {
            var i = frame_num - 1
            while (i >= frame_num - maxFrames && refs < numRef[0]) {
                val fn = if (i < 0) i + maxFrames else i
                if (sRefs[fn] != null) {
                    result[refs] = if (sRefs[fn] === H264Const.NO_PIC) null else sRefs[fn]
                    ++refs
                }
                i--
            }
        }
        val keys = lRefs.keys()
        Arrays.sort(keys)
        var i = 0
        while (i < keys.size && refs < numRef[0]) {
            result[refs++] = lRefs[keys[i]]
            i++
        }
        reorder(result, 0)
        return result
    }

    private fun buildRefListB(): Array<Array<Frame?>?> {
        val l0 = buildList(Frame.POCDesc, Frame.POCAsc)
        val l1 = buildList(Frame.POCAsc, Frame.POCDesc)
        if (Platform.arrayEqualsObj(l0, l1) && count(l1) > 1) {
            val frame = l1[1]
            l1[1] = l1[0]
            l1[0] = frame
        }
        val result = arrayOf(Platform.copyOfObj(l0, numRef[0]), Platform.copyOfObj(l1, numRef[1]))
        reorder(result[0], 0)
        reorder(result[1], 1)
        return result
    }

    private fun buildList(cmpFwd: Comparator<Frame?>, cmpInv: Comparator<Frame?>): Array<Frame?> {
        val refs = arrayOfNulls<Frame>(sRefs.size + lRefs.size())
        val fwd = copySort(cmpFwd, frameOut)
        val inv = copySort(cmpInv, frameOut)
        val nFwd = count(fwd)
        val nInv = count(inv)
        var ref = 0
        run {
            var i = 0
            while (i < nFwd) {
                refs[ref] = fwd[i]
                i++
                ref++
            }
        }
        run {
            var i = 0
            while (i < nInv) {
                refs[ref] = inv[i]
                i++
                ref++
            }
        }
        val keys = lRefs.keys()
        Arrays.sort(keys)
        var i = 0
        while (i < keys.size) {
            refs[ref] = lRefs[keys[i]]
            i++
            ref++
        }
        return refs
    }

    private fun count(arr: Array<Frame?>): Int {
        for (nn in arr.indices) if (arr[nn] == null) return nn
        return arr.size
    }

    private fun copySort(fwd: Comparator<Frame?>, dummy: Frame): Array<Frame?> {
        val copyOf = Platform.copyOfObj(sRefs, sRefs.size)
        for (i in copyOf.indices) if (fwd.compare(dummy, copyOf[i]) > 0) copyOf[i] = null
        Arrays.sort(copyOf, fwd)
        return copyOf
    }

    private fun reorder(result: Array<Frame?>?, list: Int) {
        if (sh.refPicReordering!![list] == null) return
        var predict = sh.frameNum
        val maxFrames = 1 shl sh.sps!!.log2MaxFrameNumMinus4 + 4
        for (ind in 0 until sh.refPicReordering!![list]!![0]!!.size) {
            val refType = sh.refPicReordering!![list]!![0]!![ind]
            val refIdx = sh.refPicReordering!![list]!![1]!![ind]
            for (i in numRef[list] - 1 downTo ind + 1) result!![i] = result[i - 1]
            if (refType == 2) {
                result!![ind] = lRefs[refIdx]
            } else {
                predict = if (refType == 0) MathUtil.wrap(predict - refIdx - 1, maxFrames) else MathUtil.wrap(predict + refIdx + 1, maxFrames)
                result!![ind] = sRefs[predict]
            }
            var i = ind + 1
            var j = i
            while (i < numRef[list] && result[i] != null) {
                if (result[i] !== sRefs[predict]) result[j++] = result[i]
                i++
            }
        }
    }

    init {
        numRef = if (sh.numRefIdxActiveOverrideFlag) intArrayOf(sh.numRefIdxActiveMinus1[0] + 1, sh.numRefIdxActiveMinus1[1] + 1) else intArrayOf(sh.pps!!.numRefIdxActiveMinus1[0] + 1, sh.pps!!.numRefIdxActiveMinus1[1] + 1)
        this.frameOut = frameOut
    }
}