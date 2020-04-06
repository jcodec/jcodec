package org.jcodec.codecs.h264.decode

import org.jcodec.codecs.h264.H264Const.PartPred
import org.jcodec.codecs.h264.H264Utils.MvList
import org.jcodec.codecs.h264.io.model.MBType
import org.jcodec.common.model.ColorSpace
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * A codec macroblock
 *
 * @author The JCodec project
 */
class MBlock(chromaFormat: ColorSpace) {
    @JvmField
    var chromaPredictionMode = 0

    @JvmField
    var mbQPDelta = 0

    @JvmField
    var dc: IntArray

    @JvmField
    var ac: Array<Array<IntArray>>

    @JvmField
    var transform8x8Used = false

    @JvmField
    var lumaModes: IntArray

    @JvmField
    var dc1: IntArray

    @JvmField
    var dc2: IntArray

    @JvmField
    var _cbp = 0

    @JvmField
    var mbType = 0

    @JvmField
    var curMbType: MBType? = null

    @JvmField
    var pb16x16: PB16x16

    @JvmField
    var pb168x168: PB168x168

    @JvmField
    var pb8x8: PB8x8

    @JvmField
    var ipcm: IPCM

    @JvmField
    var mbIdx = 0

    @JvmField
    var fieldDecoding = false

    @JvmField
    var prevMbType: MBType? = null

    @JvmField
    var luma16x16Mode = 0

    @JvmField
    var x: MvList

    @JvmField
    var partPreds: Array<PartPred?>

    @JvmField
    var skipped = false

    // Number of coefficients in AC blocks, stored in 8x8 encoding order: 0 1 4 5 2 3 6 7 8 9 12 13 10 11 14 15
    @JvmField
    var nCoeff: IntArray
    fun cbpLuma(): Int {
        return _cbp and 0xf
    }

    fun cbpChroma(): Int {
        return _cbp shr 4
    }

    fun cbp(cbpLuma: Int, cbpChroma: Int) {
        _cbp = cbpLuma and 0xf or (cbpChroma shl 4)
    }

    class PB16x16 {
        @JvmField
        var refIdx: IntArray

        @JvmField
        var mvdX: IntArray

        @JvmField
        var mvdY: IntArray
        fun clean() {
            refIdx[1] = 0
            refIdx[0] = refIdx[1]
            mvdX[1] = 0
            mvdX[0] = mvdX[1]
            mvdY[1] = 0
            mvdY[0] = mvdY[1]
        }

        init {
            refIdx = IntArray(2)
            mvdX = IntArray(2)
            mvdY = IntArray(2)
        }
    }

    class PB168x168 {
        @JvmField
        var refIdx1: IntArray

        @JvmField
        var refIdx2: IntArray

        @JvmField
        var mvdX1: IntArray

        @JvmField
        var mvdY1: IntArray

        @JvmField
        var mvdX2: IntArray

        @JvmField
        var mvdY2: IntArray
        fun clean() {
            refIdx1[1] = 0
            refIdx1[0] = refIdx1[1]
            refIdx2[1] = 0
            refIdx2[0] = refIdx2[1]
            mvdX1[1] = 0
            mvdX1[0] = mvdX1[1]
            mvdY1[1] = 0
            mvdY1[0] = mvdY1[1]
            mvdX2[1] = 0
            mvdX2[0] = mvdX2[1]
            mvdY2[1] = 0
            mvdY2[0] = mvdY2[1]
        }

        init {
            refIdx1 = IntArray(2)
            refIdx2 = IntArray(2)
            mvdX1 = IntArray(2)
            mvdY1 = IntArray(2)
            mvdX2 = IntArray(2)
            mvdY2 = IntArray(2)
        }
    }

    class PB8x8 {
        @JvmField
        var refIdx: Array<IntArray>

        @JvmField
        var subMbTypes: IntArray

        @JvmField
        var mvdX1: Array<IntArray>

        @JvmField
        var mvdY1: Array<IntArray>

        @JvmField
        var mvdX2: Array<IntArray>

        @JvmField
        var mvdY2: Array<IntArray>

        @JvmField
        var mvdX3: Array<IntArray>

        @JvmField
        var mvdY3: Array<IntArray>

        @JvmField
        var mvdX4: Array<IntArray>

        @JvmField
        var mvdY4: Array<IntArray>
        fun clean() {
            mvdX1[0][3] = 0
            mvdX1[0][2] = mvdX1[0][3]
            mvdX1[0][1] = mvdX1[0][2]
            mvdX1[0][0] = mvdX1[0][1]
            mvdX2[0][3] = 0
            mvdX2[0][2] = mvdX2[0][3]
            mvdX2[0][1] = mvdX2[0][2]
            mvdX2[0][0] = mvdX2[0][1]
            mvdX3[0][3] = 0
            mvdX3[0][2] = mvdX3[0][3]
            mvdX3[0][1] = mvdX3[0][2]
            mvdX3[0][0] = mvdX3[0][1]
            mvdX4[0][3] = 0
            mvdX4[0][2] = mvdX4[0][3]
            mvdX4[0][1] = mvdX4[0][2]
            mvdX4[0][0] = mvdX4[0][1]
            mvdY1[0][3] = 0
            mvdY1[0][2] = mvdY1[0][3]
            mvdY1[0][1] = mvdY1[0][2]
            mvdY1[0][0] = mvdY1[0][1]
            mvdY2[0][3] = 0
            mvdY2[0][2] = mvdY2[0][3]
            mvdY2[0][1] = mvdY2[0][2]
            mvdY2[0][0] = mvdY2[0][1]
            mvdY3[0][3] = 0
            mvdY3[0][2] = mvdY3[0][3]
            mvdY3[0][1] = mvdY3[0][2]
            mvdY3[0][0] = mvdY3[0][1]
            mvdY4[0][3] = 0
            mvdY4[0][2] = mvdY4[0][3]
            mvdY4[0][1] = mvdY4[0][2]
            mvdY4[0][0] = mvdY4[0][1]
            mvdX1[1][3] = 0
            mvdX1[1][2] = mvdX1[1][3]
            mvdX1[1][1] = mvdX1[1][2]
            mvdX1[1][0] = mvdX1[1][1]
            mvdX2[1][3] = 0
            mvdX2[1][2] = mvdX2[1][3]
            mvdX2[1][1] = mvdX2[1][2]
            mvdX2[1][0] = mvdX2[1][1]
            mvdX3[1][3] = 0
            mvdX3[1][2] = mvdX3[1][3]
            mvdX3[1][1] = mvdX3[1][2]
            mvdX3[1][0] = mvdX3[1][1]
            mvdX4[1][3] = 0
            mvdX4[1][2] = mvdX4[1][3]
            mvdX4[1][1] = mvdX4[1][2]
            mvdX4[1][0] = mvdX4[1][1]
            mvdY1[1][3] = 0
            mvdY1[1][2] = mvdY1[1][3]
            mvdY1[1][1] = mvdY1[1][2]
            mvdY1[1][0] = mvdY1[1][1]
            mvdY2[1][3] = 0
            mvdY2[1][2] = mvdY2[1][3]
            mvdY2[1][1] = mvdY2[1][2]
            mvdY2[1][0] = mvdY2[1][1]
            mvdY3[1][3] = 0
            mvdY3[1][2] = mvdY3[1][3]
            mvdY3[1][1] = mvdY3[1][2]
            mvdY3[1][0] = mvdY3[1][1]
            mvdY4[1][3] = 0
            mvdY4[1][2] = mvdY4[1][3]
            mvdY4[1][1] = mvdY4[1][2]
            mvdY4[1][0] = mvdY4[1][1]
            subMbTypes[3] = 0
            subMbTypes[2] = subMbTypes[3]
            subMbTypes[1] = subMbTypes[2]
            subMbTypes[0] = subMbTypes[1]
            refIdx[0][3] = 0
            refIdx[0][2] = refIdx[0][3]
            refIdx[0][1] = refIdx[0][2]
            refIdx[0][0] = refIdx[0][1]
            refIdx[1][3] = 0
            refIdx[1][2] = refIdx[1][3]
            refIdx[1][1] = refIdx[1][2]
            refIdx[1][0] = refIdx[1][1]
        }

        init {
            refIdx = Array(2) { IntArray(4) }
            subMbTypes = IntArray(4)
            mvdX1 = Array(2) { IntArray(4) }
            mvdY1 = Array(2) { IntArray(4) }
            mvdX2 = Array(2) { IntArray(4) }
            mvdY2 = Array(2) { IntArray(4) }
            mvdX3 = Array(2) { IntArray(4) }
            mvdY3 = Array(2) { IntArray(4) }
            mvdX4 = Array(2) { IntArray(4) }
            mvdY4 = Array(2) { IntArray(4) }
        }
    }

    class IPCM(chromaFormat: ColorSpace) {
        @JvmField
        var samplesLuma: IntArray

        @JvmField
        var samplesChroma: IntArray
        fun clean() {
            Arrays.fill(samplesLuma, 0)
            Arrays.fill(samplesChroma, 0)
        }

        init {
            samplesLuma = IntArray(256)
            val MbWidthC = 16 shr chromaFormat.compWidth[1]
            val MbHeightC = 16 shr chromaFormat.compHeight[1]
            samplesChroma = IntArray(2 * MbWidthC * MbHeightC)
        }
    }

    fun clear() {
        chromaPredictionMode = 0
        mbQPDelta = 0
        Arrays.fill(dc, 0)
        for (i in ac.indices) {
            val aci = ac[i]
            for (j in aci.indices) {
                Arrays.fill(aci[j], 0)
            }
        }
        transform8x8Used = false
        Arrays.fill(lumaModes, 0)
        Arrays.fill(dc1, 0)
        Arrays.fill(dc2, 0)
        Arrays.fill(nCoeff, 0)
        _cbp = 0
        mbType = 0
        pb16x16.clean()
        pb168x168.clean()
        pb8x8.clean()
        if (curMbType == MBType.I_PCM) ipcm.clean()
        mbIdx = 0
        fieldDecoding = false
        prevMbType = null
        luma16x16Mode = 0
        skipped = false
        curMbType = null
        x.clear()
        partPreds[3] = null
        partPreds[2] = partPreds[3]
        partPreds[1] = partPreds[2]
        partPreds[0] = partPreds[1]
    }

    init {
        pb8x8 = PB8x8()
        pb16x16 = PB16x16()
        pb168x168 = PB168x168()
        dc = IntArray(16)
        ac = arrayOf(Array(16) { IntArray(64) }, Array(4) { IntArray(16) }, Array(4) { IntArray(16) })
        lumaModes = IntArray(16)
        nCoeff = IntArray(16)
        dc1 = IntArray(16 shr chromaFormat.compWidth[1] shr chromaFormat.compHeight[1])
        dc2 = IntArray(16 shr chromaFormat.compWidth[2] shr chromaFormat.compHeight[2])
        ipcm = IPCM(chromaFormat)
        x = MvList(16)
        partPreds = arrayOfNulls(4)
    }
}