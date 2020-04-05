package org.jcodec.codecs.h264.io

import org.jcodec.codecs.common.biari.MDecoder
import org.jcodec.codecs.common.biari.MEncoder
import org.jcodec.codecs.h264.H264Const
import org.jcodec.codecs.h264.H264Const.PartPred
import org.jcodec.codecs.h264.H264Utils2
import org.jcodec.codecs.h264.decode.CABACContst
import org.jcodec.codecs.h264.io.model.MBType
import org.jcodec.codecs.h264.io.model.SliceType
import org.jcodec.common.tools.MathUtil

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author JCodec project
 */
class CABAC(mbWidth: Int) {
    class BlockType private constructor(var codedBlockCtxOff: Int, var sigCoeffFlagCtxOff: Int, var lastSigCoeffCtxOff: Int, var sigCoeffFlagFldCtxOff: Int,
                                        lastSigCoeffFldCtxOff: Int, coeffAbsLevelCtxOff: Int, coeffAbsLevelAdjust: Int) {
        var lastSigCoeffFldCtxOff: Int
        var coeffAbsLevelCtxOff: Int
        var coeffAbsLevelAdjust: Int

        companion object {
            @JvmField
            val LUMA_16_DC = BlockType(85, 105, 166, 277, 338, 227, 0)
            @JvmField
            val LUMA_15_AC = BlockType(89, 120, 181, 292, 353, 237, 0)
            @JvmField
            val LUMA_16 = BlockType(93, 134, 195, 306, 367, 247, 0)
            @JvmField
            val CHROMA_DC = BlockType(97, 149, 210, 321, 382, 257, 1)
            @JvmField
            val CHROMA_AC = BlockType(101, 152, 213, 324, 385, 266, 0)
            @JvmField
            val LUMA_64 = BlockType(1012, 402, 417, 436, 451, 426, 0)
            val CB_16_DC = BlockType(460, 484, 572, 776, 864, 952, 0)
            val CB_15x16_AC = BlockType(464, 499, 587, 791, 879, 962, 0)
            val CB_16 = BlockType(468, 513, 601, 805, 893, 972, 0)
            val CB_64 = BlockType(1016, 660, 690, 675, 699, 708, 0)
            val CR_16_DC = BlockType(472, 528, 616, 820, 908, 982, 0)
            val CR_15x16_AC = BlockType(476, 543, 631, 835, 923, 992, 0)
            val CR_16 = BlockType(480, 557, 645, 849, 937, 1002, 0)
            val CR_64 = BlockType(1020, 718, 748, 733, 757, 766, 0)
        }

        init {
            this.lastSigCoeffFldCtxOff = sigCoeffFlagFldCtxOff
            this.coeffAbsLevelCtxOff = coeffAbsLevelCtxOff
            this.coeffAbsLevelAdjust = coeffAbsLevelAdjust
        }
    }

    private var chromaPredModeLeft: Int
    private val chromaPredModeTop: IntArray
    private var prevMbQpDelta = 0
    private var prevCBP = 0
    private val codedBlkLeft: Array<IntArray>
    private val codedBlkTop: Array<IntArray>
    private val codedBlkDCLeft: IntArray
    private val codedBlkDCTop: Array<IntArray>
    private val refIdxLeft: Array<IntArray>
    private val refIdxTop: Array<IntArray>
    private var skipFlagLeft = false
    private val skipFlagsTop: BooleanArray
    private val mvdTop: Array<Array<IntArray>>
    private val mvdLeft: Array<Array<IntArray>>
    var tmp: IntArray
    fun readCoeffs(decoder: MDecoder, blockType: BlockType, out: IntArray, first: Int, num: Int, reorder: IntArray,
                   scMapping: IntArray, lscMapping: IntArray): Int {
        val sigCoeff = BooleanArray(num)
        var numCoeff: Int
        numCoeff = 0
        while (numCoeff < num - 1) {
            sigCoeff[numCoeff] = decoder.decodeBin(blockType.sigCoeffFlagCtxOff + scMapping[numCoeff]) == 1
            if (sigCoeff[numCoeff] && decoder.decodeBin(blockType.lastSigCoeffCtxOff + lscMapping[numCoeff]) == 1) break
            numCoeff++
        }
        sigCoeff[numCoeff++] = true
        var numGt1 = 0
        var numEq1 = 0
        for (j in numCoeff - 1 downTo 0) {
            if (!sigCoeff[j]) continue
            val absLev = readCoeffAbsLevel(decoder, blockType, numGt1, numEq1)
            if (absLev == 0) ++numEq1 else ++numGt1
            out[reorder[j + first]] = MathUtil.toSigned(absLev + 1, -decoder.decodeBinBypass())
        }
        // System.out.print("[");
        // for (int i = 0; i < out.length; i++)
        // System.out.print(out[i] + ",");
        // System.out.println("]");
        return numGt1 + numEq1
    }

    private fun readCoeffAbsLevel(decoder: MDecoder, blockType: BlockType, numDecodAbsLevelGt1: Int,
                                  numDecodAbsLevelEq1: Int): Int {
        val incB0 = if (numDecodAbsLevelGt1 != 0) 0 else Math.min(4, 1 + numDecodAbsLevelEq1)
        val incBN = 5 + Math.min(4 - blockType.coeffAbsLevelAdjust, numDecodAbsLevelGt1)
        var `val`: Int
        var b = decoder.decodeBin(blockType.coeffAbsLevelCtxOff + incB0)
        `val` = 0
        while (b != 0 && `val` < 13) {
            b = decoder.decodeBin(blockType.coeffAbsLevelCtxOff + incBN)
            `val`++
        }
        `val` += b
        if (`val` == 14) {
            var log = -2
            var add = 0
            var sum = 0
            do {
                log++
                b = decoder.decodeBinBypass()
            } while (b != 0)
            while (log >= 0) {
                add = add or (decoder.decodeBinBypass() shl log)
                sum += 1 shl log
                log--
            }
            `val` += add + sum
        }
        return `val`
    }

    fun writeCoeffs(encoder: MEncoder, blockType: BlockType, _out: IntArray, first: Int, num: Int, reorder: IntArray) {
        for (i in 0 until num) tmp[i] = _out[reorder[first + i]]
        var numCoeff = 0
        for (i in 0 until num) {
            if (tmp[i] != 0) numCoeff = i + 1
        }
        for (i in 0 until Math.min(numCoeff, num - 1)) {
            if (tmp[i] != 0) {
                encoder.encodeBin(blockType.sigCoeffFlagCtxOff + i, 1)
                encoder.encodeBin(blockType.lastSigCoeffCtxOff + i, if (i == numCoeff - 1) 1 else 0)
            } else {
                encoder.encodeBin(blockType.sigCoeffFlagCtxOff + i, 0)
            }
        }
        var numGt1 = 0
        var numEq1 = 0
        for (j in numCoeff - 1 downTo 0) {
            if (tmp[j] == 0) continue
            val absLev = MathUtil.abs(tmp[j]) - 1
            writeCoeffAbsLevel(encoder, blockType, numGt1, numEq1, absLev)
            if (absLev == 0) ++numEq1 else ++numGt1
            encoder.encodeBinBypass(MathUtil.sign(tmp[j]))
        }
    }

    private fun writeCoeffAbsLevel(encoder: MEncoder, blockType: BlockType, numDecodAbsLevelGt1: Int,
                                   numDecodAbsLevelEq1: Int, absLev: Int) {
        var absLev = absLev
        val incB0 = if (numDecodAbsLevelGt1 != 0) 0 else Math.min(4, 1 + numDecodAbsLevelEq1)
        val incBN = 5 + Math.min(4 - blockType.coeffAbsLevelAdjust, numDecodAbsLevelGt1)
        if (absLev == 0) {
            encoder.encodeBin(blockType.coeffAbsLevelCtxOff + incB0, 0)
        } else {
            encoder.encodeBin(blockType.coeffAbsLevelCtxOff + incB0, 1)
            if (absLev < 14) {
                for (i in 1 until absLev) encoder.encodeBin(blockType.coeffAbsLevelCtxOff + incBN, 1)
                encoder.encodeBin(blockType.coeffAbsLevelCtxOff + incBN, 0)
            } else {
                for (i in 1..13) encoder.encodeBin(blockType.coeffAbsLevelCtxOff + incBN, 1)
                absLev -= 14
                var sufLen: Int
                var pow: Int
                sufLen = 0
                pow = 1
                while (absLev >= pow) {
                    encoder.encodeBinBypass(1)
                    absLev -= pow
                    sufLen++
                    pow = 1 shl sufLen
                }
                encoder.encodeBinBypass(0)
                sufLen--
                while (sufLen >= 0) {
                    encoder.encodeBinBypass(absLev shr sufLen and 1)
                    sufLen--
                }
            }
        }
    }

    fun initModels(cm: Array<IntArray>, sliceType: SliceType, cabacIdc: Int, sliceQp: Int) {
        // System.out.println("INIT slice qp: "+sliceQp+", cabac init ids: "+cabacIdc+", slicetype : "
        // + sliceType);
        val tabA = if (sliceType.isIntra) CABACContst.cabac_context_init_I_A else CABACContst.cabac_context_init_PB_A[cabacIdc]
        val tabB = if (sliceType.isIntra) CABACContst.cabac_context_init_I_B else CABACContst.cabac_context_init_PB_B[cabacIdc]
        for (i in 0..1023) {
            val preCtxState = MathUtil.clip((tabA[i] * MathUtil.clip(sliceQp, 0, 51) shr 4) + tabB[i], 1, 126)
            if (preCtxState <= 63) {
                cm[0][i] = 63 - preCtxState
                cm[1][i] = 0
            } else {
                cm[0][i] = preCtxState - 64
                cm[1][i] = 1
            }
        }
    }

    fun readMBTypeI(decoder: MDecoder, left: MBType?, top: MBType?, leftAvailable: Boolean, topAvailable: Boolean): Int {
        var ctx = 3
        ctx += if (!leftAvailable || left == MBType.I_NxN) 0 else 1
        ctx += if (!topAvailable || top == MBType.I_NxN) 0 else 1
        return if (decoder.decodeBin(ctx) == 0) {
            0
        } else {
            if (decoder.decodeFinalBin() == 1) 25 else 1 + readMBType16x16(decoder)
        }
    }

    private fun readMBType16x16(decoder: MDecoder): Int {
        val type = decoder.decodeBin(6) * 12
        return if (decoder.decodeBin(7) == 0) {
            type + (decoder.decodeBin(9) shl 1) + decoder.decodeBin(10)
        } else {
            type + (decoder.decodeBin(8) shl 2) + (decoder.decodeBin(9) shl 1) + decoder.decodeBin(10) + 4
        }
    }

    fun readMBTypeP(decoder: MDecoder): Int {
        return if (decoder.decodeBin(14) == 1) {
            5 + readIntraP(decoder, 17)
        } else {
            if (decoder.decodeBin(15) == 0) {
                if (decoder.decodeBin(16) == 0) 0 else 3
            } else {
                if (decoder.decodeBin(17) == 0) 2 else 1
            }
        }
    }

    private fun readIntraP(decoder: MDecoder, ctxOff: Int): Int {
        return if (decoder.decodeBin(ctxOff) == 0) {
            0
        } else {
            if (decoder.decodeFinalBin() == 1) 25 else 1 + readMBType16x16P(decoder, ctxOff)
        }
    }

    private fun readMBType16x16P(decoder: MDecoder, ctxOff: Int): Int {
        var ctxOff = ctxOff
        ctxOff++
        val type = decoder.decodeBin(ctxOff) * 12
        ctxOff++
        return if (decoder.decodeBin(ctxOff) == 0) {
            ctxOff++
            type + (decoder.decodeBin(ctxOff) shl 1) + decoder.decodeBin(ctxOff)
        } else {
            (type + (decoder.decodeBin(ctxOff) shl 2) + (decoder.decodeBin(ctxOff + 1) shl 1)
                    + decoder.decodeBin(ctxOff + 1) + 4)
        }
    }

    fun readMBTypeB(mDecoder: MDecoder, left: MBType?, top: MBType?, leftAvailable: Boolean, topAvailable: Boolean): Int {
        var ctx = 27
        ctx += if (!leftAvailable || left == null || left == MBType.B_Direct_16x16) 0 else 1
        ctx += if (!topAvailable || top == null || top == MBType.B_Direct_16x16) 0 else 1
        if (mDecoder.decodeBin(ctx) == 0) return 0 // B Direct
        if (mDecoder.decodeBin(30) == 0) return 1 + mDecoder.decodeBin(32)
        val b1 = mDecoder.decodeBin(31)
        if (b1 == 0) {
            return 3 + (mDecoder.decodeBin(32) shl 2 or (mDecoder.decodeBin(32) shl 1) or mDecoder.decodeBin(32))
        } else {
            if (mDecoder.decodeBin(32) == 0) {
                return 12 + (mDecoder.decodeBin(32) shl 2 or (mDecoder.decodeBin(32) shl 1) or mDecoder.decodeBin(32))
            } else {
                when ((mDecoder.decodeBin(32) shl 1) + mDecoder.decodeBin(32)) {
                    0 -> return 20 + mDecoder.decodeBin(32)
                    1 -> return 23 + readIntraP(mDecoder, 32)
                    2 -> return 11
                    3 -> return 22
                }
            }
        }
        return 0
    }

    fun writeMBTypeI(encoder: MEncoder, left: MBType?, top: MBType?, leftAvailable: Boolean, topAvailable: Boolean,
                     mbType: Int) {
        var ctx = 3
        ctx += if (!leftAvailable || left == MBType.I_NxN) 0 else 1
        ctx += if (!topAvailable || top == MBType.I_NxN) 0 else 1
        if (mbType == 0) encoder.encodeBin(ctx, 0) else {
            encoder.encodeBin(ctx, 1)
            if (mbType == 25) encoder.encodeBinFinal(1) else {
                encoder.encodeBinFinal(0)
                writeMBType16x16(encoder, mbType - 1)
            }
        }
    }

    private fun writeMBType16x16(encoder: MEncoder, mbType: Int) {
        var mbType = mbType
        if (mbType < 12) {
            encoder.encodeBin(6, 0)
        } else {
            encoder.encodeBin(6, 1)
            mbType -= 12
        }
        if (mbType < 4) {
            encoder.encodeBin(7, 0)
            encoder.encodeBin(9, mbType shr 1)
            encoder.encodeBin(10, mbType and 1)
        } else {
            mbType -= 4
            encoder.encodeBin(7, 1)
            encoder.encodeBin(8, mbType shr 2)
            encoder.encodeBin(9, mbType shr 1 and 1)
            encoder.encodeBin(10, mbType and 1)
        }
    }

    fun readMBQpDelta(decoder: MDecoder, prevMbType: MBType?): Int {
        var ctx = 60
        ctx += if (prevMbType == null || prevMbType == MBType.I_PCM || prevMbType != MBType.I_16x16 && prevCBP == 0
                || prevMbQpDelta == 0) 0 else 1
        var `val` = 0
        if (decoder.decodeBin(ctx) == 1) {
            `val`++
            if (decoder.decodeBin(62) == 1) {
                `val`++
                while (decoder.decodeBin(63) == 1) `val`++
            }
        }
        prevMbQpDelta = H264Utils2.golomb2Signed(`val`)
        return prevMbQpDelta
    }

    fun writeMBQpDelta(encoder: MEncoder, prevMbType: MBType?, mbQpDelta: Int) {
        var mbQpDelta = mbQpDelta
        var ctx = 60
        ctx += if (prevMbType == null || prevMbType == MBType.I_PCM || prevMbType != MBType.I_16x16 && prevCBP == 0
                || prevMbQpDelta == 0) 0 else 1
        prevMbQpDelta = mbQpDelta
        if (mbQpDelta-- == 0) encoder.encodeBin(ctx, 0) else {
            encoder.encodeBin(ctx, 1)
            if (mbQpDelta-- == 0) encoder.encodeBin(62, 0) else {
                while (mbQpDelta-- > 0) encoder.encodeBin(63, 1)
                encoder.encodeBin(63, 0)
            }
        }
    }

    fun readIntraChromaPredMode(decoder: MDecoder, mbX: Int, left: MBType?, top: MBType?, leftAvailable: Boolean,
                                topAvailable: Boolean): Int {
        var ctx = 64
        ctx += if (!leftAvailable || left == null || !left.isIntra || chromaPredModeLeft == 0) 0 else 1
        ctx += if (!topAvailable || top == null || !top.isIntra || chromaPredModeTop[mbX] == 0) 0 else 1
        val mode: Int
        mode = if (decoder.decodeBin(ctx) == 0) 0 else if (decoder.decodeBin(67) == 0) 1 else if (decoder.decodeBin(67) == 0) 2 else 3
        chromaPredModeTop[mbX] = mode
        chromaPredModeLeft = chromaPredModeTop[mbX]
        return mode
    }

    fun writeIntraChromaPredMode(encoder: MEncoder, mbX: Int, left: MBType?, top: MBType?, leftAvailable: Boolean,
                                 topAvailable: Boolean, mode: Int) {
        var mode = mode
        var ctx = 64
        ctx += if (!leftAvailable || !left!!.isIntra || chromaPredModeLeft == 0) 0 else 1
        ctx += if (!topAvailable || !top!!.isIntra || chromaPredModeTop[mbX] == 0) 0 else 1
        encoder.encodeBin(ctx, if (mode-- == 0) 0 else 1)
        var i = 0
        while (mode >= 0 && i < 2) {
            encoder.encodeBin(67, if (mode-- == 0) 0 else 1)
            i++
        }
        chromaPredModeTop[mbX] = mode
        chromaPredModeLeft = chromaPredModeTop[mbX]
    }

    fun condTerm(mbCur: MBType, nAvb: Boolean, mbN: MBType?, nBlkAvb: Boolean, cbpN: Int): Int {
        if (!nAvb) return if (mbCur.isIntra) 1 else 0
        if (mbN == MBType.I_PCM) return 1
        return if (!nBlkAvb) 0 else cbpN
    }

    fun readCodedBlockFlagLumaDC(decoder: MDecoder, mbX: Int, left: MBType?, top: MBType?, leftAvailable: Boolean,
                                 topAvailable: Boolean, cur: MBType): Int {
        val tLeft = condTerm(cur, leftAvailable, left, left == MBType.I_16x16, codedBlkDCLeft[0])
        val tTop = condTerm(cur, topAvailable, top, top == MBType.I_16x16, codedBlkDCTop[0][mbX])
        val decoded = decoder.decodeBin(BlockType.LUMA_16_DC.codedBlockCtxOff + tLeft + 2 * tTop)
        codedBlkDCLeft[0] = decoded
        codedBlkDCTop[0][mbX] = decoded
        return decoded
    }

    fun readCodedBlockFlagChromaDC(decoder: MDecoder, mbX: Int, comp: Int, left: MBType?, top: MBType?,
                                   leftAvailable: Boolean, topAvailable: Boolean, leftCBPChroma: Int, topCBPChroma: Int, cur: MBType): Int {
        val tLeft = condTerm(cur, leftAvailable, left, left != null && leftCBPChroma != 0, codedBlkDCLeft[comp])
        val tTop = condTerm(cur, topAvailable, top, top != null && topCBPChroma != 0, codedBlkDCTop[comp][mbX])
        val decoded = decoder.decodeBin(BlockType.CHROMA_DC.codedBlockCtxOff + tLeft + 2 * tTop)
        codedBlkDCLeft[comp] = decoded
        codedBlkDCTop[comp][mbX] = decoded
        return decoded
    }

    fun readCodedBlockFlagLumaAC(decoder: MDecoder, blkType: BlockType, blkX: Int, blkY: Int, comp: Int, left: MBType?,
                                 top: MBType?, leftAvailable: Boolean, topAvailable: Boolean, leftCBPLuma: Int, topCBPLuma: Int, curCBPLuma: Int,
                                 cur: MBType): Int {
        val blkOffLeft = blkX and 3
        val blkOffTop = blkY and 3
        val tLeft: Int
        tLeft = if (blkOffLeft == 0) condTerm(cur, leftAvailable, left, left != null && left != MBType.I_PCM && cbp(leftCBPLuma, 3, blkOffTop),
                codedBlkLeft[comp][blkOffTop]) else condTerm(cur, true, cur, cbp(curCBPLuma, blkOffLeft - 1, blkOffTop), codedBlkLeft[comp][blkOffTop])
        val tTop: Int
        tTop = if (blkOffTop == 0) condTerm(cur, topAvailable, top, top != null && top != MBType.I_PCM && cbp(topCBPLuma, blkOffLeft, 3),
                codedBlkTop[comp][blkX]) else condTerm(cur, true, cur, cbp(curCBPLuma, blkOffLeft, blkOffTop - 1), codedBlkTop[comp][blkX])
        val decoded = decoder.decodeBin(blkType.codedBlockCtxOff + tLeft + 2 * tTop)
        codedBlkLeft[comp][blkOffTop] = decoded
        codedBlkTop[comp][blkX] = decoded
        return decoded
    }

    fun readCodedBlockFlagLuma64(decoder: MDecoder, blkX: Int, blkY: Int, comp: Int, left: MBType?, top: MBType?,
                                 leftAvailable: Boolean, topAvailable: Boolean, leftCBPLuma: Int, topCBPLuma: Int, curCBPLuma: Int, cur: MBType,
                                 is8x8Left: Boolean, is8x8Top: Boolean): Int {
        val blkOffLeft = blkX and 3
        val blkOffTop = blkY and 3
        val tLeft: Int
        tLeft = if (blkOffLeft == 0) condTerm(cur, leftAvailable, left,
                left != null && left != MBType.I_PCM && is8x8Left && cbp(leftCBPLuma, 3, blkOffTop),
                codedBlkLeft[comp][blkOffTop]) else condTerm(cur, true, cur, cbp(curCBPLuma, blkOffLeft - 1, blkOffTop), codedBlkLeft[comp][blkOffTop])
        val tTop: Int
        tTop = if (blkOffTop == 0) condTerm(cur, topAvailable, top,
                top != null && top != MBType.I_PCM && is8x8Top && cbp(topCBPLuma, blkOffLeft, 3), codedBlkTop[comp][blkX]) else condTerm(cur, true, cur, cbp(curCBPLuma, blkOffLeft, blkOffTop - 1), codedBlkTop[comp][blkX])
        val decoded = decoder.decodeBin(BlockType.LUMA_64.codedBlockCtxOff + tLeft + 2 * tTop)
        codedBlkLeft[comp][blkOffTop] = decoded
        codedBlkTop[comp][blkX] = decoded
        return decoded
    }

    private fun cbp(cbpLuma: Int, blkX: Int, blkY: Int): Boolean {
        val x8x8 = (blkY and 2) + (blkX shr 1)
        return cbpLuma shr x8x8 and 1 == 1
    }

    fun readCodedBlockFlagChromaAC(decoder: MDecoder, blkX: Int, blkY: Int, comp: Int, left: MBType?, top: MBType?,
                                   leftAvailable: Boolean, topAvailable: Boolean, leftCBPChroma: Int, topCBPChroma: Int, cur: MBType): Int {
        val blkOffLeft = blkX and 1
        val blkOffTop = blkY and 1
        val tLeft: Int
        tLeft = if (blkOffLeft == 0) condTerm(cur, leftAvailable, left, left != null && left != MBType.I_PCM && leftCBPChroma and 2 != 0,
                codedBlkLeft[comp][blkOffTop]) else condTerm(cur, true, cur, true, codedBlkLeft[comp][blkOffTop])
        val tTop: Int
        tTop = if (blkOffTop == 0) condTerm(cur, topAvailable, top, top != null && top != MBType.I_PCM && topCBPChroma and 2 != 0,
                codedBlkTop[comp][blkX]) else condTerm(cur, true, cur, true, codedBlkTop[comp][blkX])
        val decoded = decoder.decodeBin(BlockType.CHROMA_AC.codedBlockCtxOff + tLeft + 2 * tTop)
        codedBlkLeft[comp][blkOffTop] = decoded
        codedBlkTop[comp][blkX] = decoded
        return decoded
    }

    fun prev4x4PredModeFlag(decoder: MDecoder): Boolean {
        return decoder.decodeBin(68) == 1
    }

    fun rem4x4PredMode(decoder: MDecoder): Int {
        return decoder.decodeBin(69) or (decoder.decodeBin(69) shl 1) or (decoder.decodeBin(69) shl 2)
    }

    fun codedBlockPatternIntra(mDecoder: MDecoder, leftAvailable: Boolean, topAvailable: Boolean, cbpLeft: Int,
                               cbpTop: Int, mbLeft: MBType?, mbTop: MBType?): Int {
        val cbp0 = mDecoder.decodeBin(73 + _condTerm(leftAvailable, mbLeft, cbpLeft shr 1 and 1) + (2
                * _condTerm(topAvailable, mbTop, cbpTop shr 2 and 1)))
        val cbp1 = mDecoder.decodeBin(73 + (1 - cbp0) + 2 * _condTerm(topAvailable, mbTop, cbpTop shr 3 and 1))
        val cbp2 = mDecoder.decodeBin(73 + _condTerm(leftAvailable, mbLeft, cbpLeft shr 3 and 1) + 2 * (1 - cbp0))
        val cbp3 = mDecoder.decodeBin(73 + (1 - cbp2) + 2 * (1 - cbp1))
        val cr0 = mDecoder.decodeBin(77 + condTermCr0(leftAvailable, mbLeft, cbpLeft shr 4) + (2
                * condTermCr0(topAvailable, mbTop, cbpTop shr 4)))
        val cr1 = if (cr0 != 0) mDecoder.decodeBin(81 + condTermCr1(leftAvailable, mbLeft, cbpLeft shr 4) + (2
                * condTermCr1(topAvailable, mbTop, cbpTop shr 4))) else 0
        return cbp0 or (cbp1 shl 1) or (cbp2 shl 2) or (cbp3 shl 3) or (cr0 shl 4) or (cr1 shl 5)
    }

    private fun condTermCr0(avb: Boolean, mbt: MBType?, cbpChroma: Int): Int {
        return if (avb && (mbt == MBType.I_PCM || mbt != null && cbpChroma != 0)) 1 else 0
    }

    private fun condTermCr1(avb: Boolean, mbt: MBType?, cbpChroma: Int): Int {
        return if (avb && (mbt == MBType.I_PCM || mbt != null && cbpChroma and 2 != 0)) 1 else 0
    }

    private fun _condTerm(avb: Boolean, mbt: MBType?, cbp: Int): Int {
        return if (!avb || mbt == MBType.I_PCM || mbt != null && cbp == 1) 0 else 1
    }

    fun setPrevCBP(prevCBP: Int) {
        this.prevCBP = prevCBP
    }

    fun readMVD(decoder: MDecoder, comp: Int, leftAvailable: Boolean, topAvailable: Boolean, leftType: MBType?,
                topType: MBType?, leftPred: PartPred?, topPred: PartPred?, curPred: PartPred, mbX: Int, partX: Int, partY: Int,
                partW: Int, partH: Int, list: Int): Int {
        val ctx = if (comp == 0) 40 else 47
        val partAbsX = (mbX shl 2) + partX
        val predEqA = leftPred != null && leftPred != PartPred.Direct && (leftPred == PartPred.Bi || leftPred == curPred || curPred == PartPred.Bi && H264Const.usesList(leftPred, list))
        val predEqB = topPred != null && topPred != PartPred.Direct && (topPred == PartPred.Bi || topPred == curPred || curPred == PartPred.Bi && H264Const.usesList(topPred, list))

        // prefix and suffix as given by UEG3 with signedValFlag=1, uCoff=9
        var absMvdComp = if (!leftAvailable || leftType == null || leftType.isIntra || !predEqA) 0 else Math
                .abs(mvdLeft[list][comp][partY])
        absMvdComp += if (!topAvailable || topType == null || topType.isIntra || !predEqB) 0 else Math
                .abs(mvdTop[list][comp][partAbsX])
        var `val`: Int
        var b = decoder.decodeBin(ctx + if (absMvdComp < 3) 0 else if (absMvdComp > 32) 2 else 1)
        `val` = 0
        while (b != 0 && `val` < 8) {
            b = decoder.decodeBin(Math.min(ctx + `val` + 3, ctx + 6))
            `val`++
        }
        `val` += b
        if (`val` != 0) {
            if (`val` == 9) {
                var log = 2
                var add = 0
                var sum = 0
                var leftover = 0
                do {
                    sum += leftover
                    log++
                    b = decoder.decodeBinBypass()
                    leftover = 1 shl log
                } while (b != 0)
                --log
                while (log >= 0) {
                    add = add or (decoder.decodeBinBypass() shl log)
                    log--
                }
                `val` += add + sum
            }
            `val` = MathUtil.toSigned(`val`, -decoder.decodeBinBypass())
        }
        for (i in 0 until partW) {
            mvdTop[list][comp][partAbsX + i] = `val`
        }
        for (i in 0 until partH) {
            mvdLeft[list][comp][partY + i] = `val`
        }
        return `val`
    }

    fun readRefIdx(mDecoder: MDecoder, leftAvailable: Boolean, topAvailable: Boolean, leftType: MBType?,
                   topType: MBType?, leftPred: PartPred?, topPred: PartPred?, curPred: PartPred, mbX: Int, partX: Int, partY: Int,
                   partW: Int, partH: Int, list: Int): Int {
        val partAbsX = (mbX shl 2) + partX
        val predEqA = leftPred != null && leftPred != PartPred.Direct && (leftPred == PartPred.Bi || leftPred == curPred || curPred == PartPred.Bi && H264Const.usesList(leftPred, list))
        val predEqB = topPred != null && topPred != PartPred.Direct && (topPred == PartPred.Bi || topPred == curPred || curPred == PartPred.Bi && H264Const.usesList(topPred, list))
        val ctA = if (!leftAvailable || leftType == null || leftType.isIntra || !predEqA || refIdxLeft[list][partY] == 0) 0 else 1
        val ctB = if (!topAvailable || topType == null || topType.isIntra || !predEqB || refIdxTop[list][partAbsX] == 0) 0 else 1
        val b0 = mDecoder.decodeBin(54 + ctA + 2 * ctB)
        var `val`: Int
        if (b0 == 0) `val` = 0 else {
            val b1 = mDecoder.decodeBin(58)
            if (b1 == 0) `val` = 1 else {
                `val` = 2
                while (mDecoder.decodeBin(59) == 1) {
                    `val`++
                }
            }
        }
        for (i in 0 until partW) {
            refIdxTop[list][partAbsX + i] = `val`
        }
        for (i in 0 until partH) {
            refIdxLeft[list][partY + i] = `val`
        }
        return `val`
    }

    fun readMBSkipFlag(mDecoder: MDecoder, slType: SliceType, leftAvailable: Boolean, topAvailable: Boolean,
                       mbX: Int): Boolean {
        val base = if (slType == SliceType.P) 11 else 24
        val ret = mDecoder.decodeBin(base + (if (leftAvailable && !skipFlagLeft) 1 else 0)
                + if (topAvailable && !skipFlagsTop[mbX]) 1 else 0) == 1
        skipFlagsTop[mbX] = ret
        skipFlagLeft = skipFlagsTop[mbX]
        return ret
    }

    fun readSubMbTypeP(mDecoder: MDecoder): Int {
        return if (mDecoder.decodeBin(21) == 1) 0 else if (mDecoder.decodeBin(22) == 0) 1 else if (mDecoder.decodeBin(23) == 1) 2 else 3
    }

    fun readSubMbTypeB(mDecoder: MDecoder): Int {
        if (mDecoder.decodeBin(36) == 0) return 0 // direct
        if (mDecoder.decodeBin(37) == 0) return 1 + mDecoder.decodeBin(39)
        if (mDecoder.decodeBin(38) == 0) return 3 + (mDecoder.decodeBin(39) shl 1) + mDecoder.decodeBin(39)
        return if (mDecoder.decodeBin(39) == 0) 7 + (mDecoder.decodeBin(39) shl 1) + mDecoder.decodeBin(39) else 11 + mDecoder.decodeBin(39)
    }

    fun readTransform8x8Flag(mDecoder: MDecoder, leftAvailable: Boolean, topAvailable: Boolean,
                             leftType: MBType?, topType: MBType?, is8x8Left: Boolean, is8x8Top: Boolean): Boolean {
        val ctx = (399 + (if (leftAvailable && leftType != null && is8x8Left) 1 else 0)
                + if (topAvailable && topType != null && is8x8Top) 1 else 0)
        return mDecoder.decodeBin(ctx) == 1
    }

    fun setCodedBlock(blkX: Int, blkY: Int) {
        codedBlkTop[0][blkX] = 1
        codedBlkLeft[0][blkY and 0x3] = codedBlkTop[0][blkX]
    }

    init {
        tmp = IntArray(16)
        chromaPredModeLeft = 0
        chromaPredModeTop = IntArray(mbWidth)
        codedBlkLeft = arrayOf(IntArray(4), IntArray(2), IntArray(2))
        codedBlkTop = arrayOf(IntArray(mbWidth shl 2), IntArray(mbWidth shl 1), IntArray(mbWidth shl 1))
        codedBlkDCLeft = IntArray(3)
        codedBlkDCTop = Array(3) { IntArray(mbWidth) }
        refIdxLeft = Array(2) { IntArray(4) }
        refIdxTop = Array(2) { IntArray(mbWidth shl 2) }
        skipFlagsTop = BooleanArray(mbWidth)
        mvdTop = Array(2) { Array(2) { IntArray(mbWidth shl 2) } }
        mvdLeft = Array(2) { Array(2) { IntArray(4) } }
    }
}