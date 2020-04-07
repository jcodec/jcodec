package org.jcodec.codecs.h264.io

import org.jcodec.codecs.h264.H264Const
import org.jcodec.codecs.h264.decode.CAVLCReader.readU
import org.jcodec.codecs.h264.decode.CAVLCReader.readZeroBitCount
import org.jcodec.codecs.h264.io.model.MBType
import org.jcodec.codecs.h264.io.model.PictureParameterSet
import org.jcodec.codecs.h264.io.model.SeqParameterSet
import org.jcodec.common.io.BitReader
import org.jcodec.common.io.BitWriter
import org.jcodec.common.io.VLC
import org.jcodec.common.model.ColorSpace
import org.jcodec.common.tools.MathUtil

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Non-CABAC H.264 symbols read/write routines
 *
 * @author Jay Codec
 */
class CAVLC private constructor(private val color: ColorSpace?, mbWidth: Int, mbW: Int, mbH: Int) {
    val coeffTokenVLCForChromaDC: VLC?
    private val tokensLeft: IntArray
    private val tokensTop: IntArray
    private val mbWidth: Int
    private val mbMask: Int
    private val mbW: Int
    private val mbH: Int

    constructor(sps: SeqParameterSet, pps: PictureParameterSet?, mbW: Int, mbH: Int) : this(sps.chromaFormatIdc, sps.picWidthInMbsMinus1 + 1, mbW, mbH) {}

    fun fork(): CAVLC {
        val ret = CAVLC(color, mbWidth, mbW, mbH)
        System.arraycopy(tokensLeft, 0, ret.tokensLeft, 0, tokensLeft.size)
        System.arraycopy(tokensTop, 0, ret.tokensTop, 0, tokensTop.size)
        return ret
    }

    fun writeACBlock(out: BitWriter?, blkIndX: Int, blkIndY: Int, leftMBType: MBType?, topMBType: MBType?, coeff: IntArray?,
                     totalZerosTab: Array<VLC?>?, firstCoeff: Int, maxCoeff: Int, scan: IntArray?): Int {
        val coeffTokenTab = getCoeffTokenVLCForLuma(blkIndX != 0, leftMBType, tokensLeft[blkIndY and mbMask],
                blkIndY != 0, topMBType, tokensTop[blkIndX])
        val coeffToken = writeBlockGen(out, coeff, totalZerosTab, firstCoeff, maxCoeff, scan, coeffTokenTab)
        tokensLeft[blkIndY and mbMask] = coeffToken
        tokensTop[blkIndX] = coeffToken
        return coeffToken
    }

    fun writeChrDCBlock(out: BitWriter?, coeff: IntArray?, totalZerosTab: Array<VLC?>?, firstCoeff: Int, maxCoeff: Int,
                        scan: IntArray?) {
        writeBlockGen(out, coeff, totalZerosTab, firstCoeff, maxCoeff, scan, coeffTokenVLCForChromaDC)
    }

    fun writeLumaDCBlock(out: BitWriter?, blkIndX: Int, blkIndY: Int, leftMBType: MBType?, topMBType: MBType?,
                         coeff: IntArray?, totalZerosTab: Array<VLC?>?, firstCoeff: Int, maxCoeff: Int, scan: IntArray?) {
        val coeffTokenTab = getCoeffTokenVLCForLuma(blkIndX != 0, leftMBType, tokensLeft[blkIndY and mbMask],
                blkIndY != 0, topMBType, tokensTop[blkIndX])
        writeBlockGen(out, coeff, totalZerosTab, firstCoeff, maxCoeff, scan, coeffTokenTab)
    }

    protected fun codeTableChromaDC(): VLC? {
        if (color == ColorSpace.YUV420J) {
            return H264Const.coeffTokenChromaDCY420
        } else if (color == ColorSpace.YUV422) {
            return H264Const.coeffTokenChromaDCY422
        } else if (color == ColorSpace.YUV444) {
            return H264Const.CoeffToken[0]
        }
        return null
    }

    fun readChromaDCBlock(reader: BitReader?, coeff: IntArray, leftAvailable: Boolean, topAvailable: Boolean) {
        val coeffTokenTab = coeffTokenVLCForChromaDC
        readCoeffs(reader!!, coeffTokenTab!!, if (coeff.size == 16) H264Const.totalZeros16 else if (coeff.size == 8) H264Const.totalZeros8 else H264Const.totalZeros4, coeff, 0, coeff.size,
                NO_ZIGZAG)
    }

    fun readLumaDCBlock(reader: BitReader?, coeff: IntArray?, mbX: Int, leftAvailable: Boolean, leftMbType: MBType?,
                        topAvailable: Boolean, topMbType: MBType?, zigzag4x4: IntArray?) {
        val coeffTokenTab = getCoeffTokenVLCForLuma(leftAvailable, leftMbType, tokensLeft[0], topAvailable, topMbType,
                tokensTop[mbX shl 2])
        readCoeffs(reader!!, coeffTokenTab!!, H264Const.totalZeros16, coeff, 0, 16, zigzag4x4)
    }

    fun readACBlock(reader: BitReader?, coeff: IntArray?, blkIndX: Int, blkIndY: Int, leftAvailable: Boolean,
                    leftMbType: MBType?, topAvailable: Boolean, topMbType: MBType?, firstCoeff: Int, nCoeff: Int, zigzag4x4: IntArray?): Int {
        val coeffTokenTab = getCoeffTokenVLCForLuma(leftAvailable, leftMbType, tokensLeft[blkIndY and mbMask],
                topAvailable, topMbType, tokensTop[blkIndX])
        val readCoeffs = readCoeffs(reader!!, coeffTokenTab!!, H264Const.totalZeros16, coeff, firstCoeff, nCoeff, zigzag4x4)
        tokensTop[blkIndX] = readCoeffs
        tokensLeft[blkIndY and mbMask] = tokensTop[blkIndX]
        return totalCoeff(readCoeffs)
    }

    fun setZeroCoeff(blkIndX: Int, blkIndY: Int) {
        tokensTop[blkIndX] = 0
        tokensLeft[blkIndY and mbMask] = tokensTop[blkIndX]
    }

    init {
        coeffTokenVLCForChromaDC = codeTableChromaDC()
        this.mbWidth = mbWidth
        mbMask = (1 shl mbH) - 1
        this.mbW = mbW
        this.mbH = mbH
        tokensLeft = IntArray(4)
        tokensTop = IntArray(mbWidth shl mbW)
    }

    companion object {

        fun Min(i: Int, level_prefix: Int): Int {
            return if (i < level_prefix) i else level_prefix
        }

        fun Abs(i: Int): Int {
            return if (i < 0) -i else i
        }

        fun totalCoeff(coeffToken: Int): Int {
            return coeffToken shr 4
        }

        fun trailingOnes(coeffToken: Int): Int {
            return coeffToken and 0xf
        }

        fun codeTableLuma(leftAvailable: Boolean, leftMBType: MBType?, leftToken: Int, topAvailable: Boolean,
                          topMBType: MBType?, topToken: Int): Int {
            val nA = if (leftMBType == null) 0 else totalCoeff(leftToken)
            val nB = if (topMBType == null) 0 else totalCoeff(topToken)
            return if (leftAvailable && topAvailable) nA + nB + 1 shr 1 else if (leftAvailable) nA else if (topAvailable) nB else 0
        }

        fun getCoeffTokenVLCForLuma(leftAvailable: Boolean, leftMBType: MBType?, leftToken: Int, topAvailable: Boolean,
                                    topMBType: MBType?, topToken: Int): VLC? {
            val nc = codeTableLuma(leftAvailable, leftMBType, leftToken, topAvailable, topMBType, topToken)
            return H264Const.CoeffToken[Math.min(nc, 8)]
        }

        fun writeTrailingOnes(out: BitWriter, levels: IntArray, totalCoeff: Int, trailingOne: Int) {
            for (i in totalCoeff - 1 downTo totalCoeff - trailingOne) out.write1Bit(levels[i] ushr 31)
        }

        fun unsigned(signed: Int): Int {
            val sign = signed ushr 31
            val s = signed shr 31
            return ((signed xor s) - s shl 1) + sign - 2
        }

        fun writeRuns(out: BitWriter?, run: IntArray, totalCoeff: Int, totalZeros: Int) {
            var totalZeros = totalZeros
            var i = totalCoeff - 1
            while (i > 0 && totalZeros > 0) {
                H264Const.run[Math.min(6, totalZeros - 1)].writeVLC(out, run[i])
                totalZeros -= run[i]
                i--
            }
        }

        val NO_ZIGZAG = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)

        fun writeBlockGen(out: BitWriter?, coeff: IntArray?, totalZerosTab: Array<VLC?>?, firstCoeff: Int, maxCoeff: Int,
                          scan: IntArray?, coeffTokenTab: VLC?): Int {
            var trailingOnes = 0
            var totalCoeff = 0
            var totalZeros = 0
            val runBefore = IntArray(maxCoeff)
            val levels = IntArray(maxCoeff)
            for (i in 0 until maxCoeff) {
                val c = coeff!![scan!![i + firstCoeff]]
                if (c == 0) {
                    runBefore[totalCoeff]++
                    totalZeros++
                } else {
                    levels[totalCoeff++] = c
                }
            }
            if (totalCoeff < maxCoeff) totalZeros -= runBefore[totalCoeff]
            trailingOnes = 0
            while (trailingOnes < totalCoeff && trailingOnes < 3 && Math.abs(levels[totalCoeff - trailingOnes - 1]) == 1) {
                trailingOnes++
            }
            val coeffToken = H264Const.coeffToken(totalCoeff, trailingOnes)
            coeffTokenTab!!.writeVLC(out, coeffToken)
            if (totalCoeff > 0) {
                writeTrailingOnes(out!!, levels, totalCoeff, trailingOnes)
                writeLevels(out, levels, totalCoeff, trailingOnes)
                if (totalCoeff < maxCoeff) {
                    totalZerosTab!![totalCoeff - 1]!!.writeVLC(out, totalZeros)
                    writeRuns(out, runBefore, totalCoeff, totalZeros)
                }
            }
            return coeffToken
        }

        fun writeLevels(out: BitWriter, levels: IntArray, totalCoeff: Int, trailingOnes: Int) {
            var suffixLen = if (totalCoeff > 10 && trailingOnes < 3) 1 else 0
            for (i in totalCoeff - trailingOnes - 1 downTo 0) {
                var absLev = unsigned(levels[i])
                if (i == totalCoeff - trailingOnes - 1 && trailingOnes < 3) absLev -= 2
                val prefix = absLev shr suffixLen
                if (suffixLen == 0 && prefix < 14 || suffixLen > 0 && prefix < 15) {
                    out.writeNBit(1, prefix + 1)
                    out.writeNBit(absLev, suffixLen)
                } else if (suffixLen == 0 && absLev < 30) {
                    out.writeNBit(1, 15)
                    out.writeNBit(absLev - 14, 4)
                } else {
                    if (suffixLen == 0) absLev -= 15
                    var len: Int
                    var code: Int
                    len = 12
                    while (true) {
                        code = absLev - (len + 3 shl suffixLen) - (1 shl len) + 4096
                        if (code >= 1 shl len) {
                            len++
                        } else {
                            break
                        }
                    }
                    out.writeNBit(1, len + 4)
                    out.writeNBit(code, len)
                }
                if (suffixLen == 0) suffixLen = 1
                if (MathUtil.abs(levels[i]) > 3 shl suffixLen - 1 && suffixLen < 6) suffixLen++
            }
        }
        fun readCoeffs(_in: BitReader, coeffTokenTab: VLC, totalZerosTab: Array<VLC?>?, coeffLevel: IntArray?, firstCoeff: Int,
                       nCoeff: Int, zigzag: IntArray?): Int {
            val coeffToken = coeffTokenTab.readVLC(_in)
            val totalCoeff = totalCoeff(coeffToken)
            val trailingOnes = trailingOnes(coeffToken)
            if (totalCoeff > 0) {
                var suffixLength = if (totalCoeff > 10 && trailingOnes < 3) 1 else 0
                val level = IntArray(totalCoeff)
                var i: Int
                i = 0
                while (i < trailingOnes) {
                    level[i] = 1 - 2 * _in.read1Bit()
                    i++
                }
                while (i < totalCoeff) {
                    val level_prefix = readZeroBitCount(_in, "")
                    var levelSuffixSize = suffixLength
                    if (level_prefix == 14 && suffixLength == 0) levelSuffixSize = 4
                    if (level_prefix >= 15) levelSuffixSize = level_prefix - 3
                    var levelCode = Min(15, level_prefix) shl suffixLength
                    if (levelSuffixSize > 0) {
                        val level_suffix = readU(_in, levelSuffixSize, "RB: level_suffix")
                        levelCode += level_suffix
                    }
                    if (level_prefix >= 15 && suffixLength == 0) levelCode += 15
                    if (level_prefix >= 16) levelCode += (1 shl level_prefix - 3) - 4096
                    if (i == trailingOnes && trailingOnes < 3) levelCode += 2
                    if (levelCode % 2 == 0) level[i] = levelCode + 2 shr 1 else level[i] = -levelCode - 1 shr 1
                    if (suffixLength == 0) suffixLength = 1
                    if (Abs(level[i]) > 3 shl suffixLength - 1 && suffixLength < 6) suffixLength++
                    i++
                }
                var zerosLeft: Int
                zerosLeft = if (totalCoeff < nCoeff) {
                    if (coeffLevel!!.size == 4) {
                        H264Const.totalZeros4[totalCoeff - 1].readVLC(_in)
                    } else if (coeffLevel.size == 8) {
                        H264Const.totalZeros8[totalCoeff - 1].readVLC(_in)
                    } else {
                        H264Const.totalZeros16[totalCoeff - 1].readVLC(_in)
                    }
                } else 0
                val runs = IntArray(totalCoeff)
                var r: Int
                r = 0
                while (r < totalCoeff - 1 && zerosLeft > 0) {
                    val run = H264Const.run[Math.min(6, zerosLeft - 1)].readVLC(_in)
                    zerosLeft -= run
                    runs[r] = run
                    r++
                }
                runs[r] = zerosLeft
                var j = totalCoeff - 1
                var cn = 0
                while (j >= 0 && cn < nCoeff) {
                    cn += runs[j]
                    coeffLevel!![zigzag!![cn + firstCoeff]] = level[j]
                    j--
                    cn++
                }
            }

            // System.out.print("[");
            // for (int i = 0; i < nCoeff; i++)
            // System.out.print(coeffLevel[i + firstCoeff] + ", ");
            // System.out.println("]");
            return coeffToken
        }
    }
}