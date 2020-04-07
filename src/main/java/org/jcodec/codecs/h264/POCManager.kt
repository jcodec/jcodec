package org.jcodec.codecs.h264

import org.jcodec.codecs.h264.io.model.NALUnit
import org.jcodec.codecs.h264.io.model.NALUnitType
import org.jcodec.codecs.h264.io.model.RefPicMarking.InstrType
import org.jcodec.codecs.h264.io.model.SliceHeader

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * POC ( Picture Order Count ) manager
 *
 * Picture Order Count is used to represent an order of picture in a GOP ( Group
 * of Pictures ) this is needed to correctly reorder and B-framed GOPs. POC is
 * also used when building lists of reference pictures ( see 8.2.4.2 ).
 *
 * There are 3 possible ways of assigning POC to decoded pictures:
 *
 * - Explicit, i.e. POC is directly specified in a slice header in form <POC Pred> + <POC Dec>. <POC Pred> is a significant part of POC ( see 8.2.1.1 ). -
 * Frame based type 1 ( see 8.2.1.2 ). - Frame based type 2 ( see 8.2.1.3 ).
 *
 * @author The JCodec project
</POC></POC></POC> */
class POCManager {
    private var prevPOCMsb = 0
    private var prevPOCLsb = 0
    fun calcPOC(firstSliceHeader: SliceHeader, firstNu: NALUnit): Int {
        return when (firstSliceHeader.sps!!.picOrderCntType) {
            0 -> calcPOC0(firstSliceHeader, firstNu)
            1 -> calcPOC1(firstSliceHeader, firstNu)
            2 -> calcPOC2(firstSliceHeader, firstNu)
            else -> throw RuntimeException("POC no!!!")
        }
    }

    private fun calcPOC2(firstSliceHeader: SliceHeader, firstNu: NALUnit): Int {
        return firstSliceHeader.frameNum shl 1
    }

    private fun calcPOC1(firstSliceHeader: SliceHeader, firstNu: NALUnit): Int {
        return firstSliceHeader.frameNum shl 1
    }

    private fun calcPOC0(firstSliceHeader: SliceHeader, firstNu: NALUnit): Int {
        if (firstNu.type == NALUnitType.IDR_SLICE) {
            prevPOCLsb = 0
            prevPOCMsb = prevPOCLsb
        }
        val maxPOCLsbDiv2 = 1 shl firstSliceHeader.sps!!.log2MaxPicOrderCntLsbMinus4 + 3
        val maxPOCLsb = maxPOCLsbDiv2 shl 1
        val POCLsb = firstSliceHeader.picOrderCntLsb
        val POCMsb: Int
        val POC: Int
        POCMsb = if (POCLsb < prevPOCLsb && prevPOCLsb - POCLsb >= maxPOCLsbDiv2) prevPOCMsb + maxPOCLsb else if (POCLsb > prevPOCLsb && POCLsb - prevPOCLsb > maxPOCLsbDiv2) prevPOCMsb - maxPOCLsb else prevPOCMsb
        POC = POCMsb + POCLsb
        if (firstNu.nal_ref_idc > 0) {
            if (hasMMCO5(firstSliceHeader, firstNu)) {
                prevPOCMsb = 0
                prevPOCLsb = POC
            } else {
                prevPOCMsb = POCMsb
                prevPOCLsb = POCLsb
            }
        }
        return POC
    }

    private fun hasMMCO5(firstSliceHeader: SliceHeader, firstNu: NALUnit): Boolean {
        if (firstNu.type != NALUnitType.IDR_SLICE && firstSliceHeader.refPicMarkingNonIDR != null) {
            val instructions = firstSliceHeader.refPicMarkingNonIDR!!.instructions
            for (i in instructions.indices) {
                val instruction = instructions[i]
                if (instruction.type === InstrType.CLEAR) return true
            }
        }
        return false
    }
}