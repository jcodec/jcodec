package org.jcodec.codecs.vpx

import org.jcodec.common.tools.MathUtil

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class VPXBitstream(tokenBinProbs: Array<Array<Array<IntArray>>>, mbWidth: Int) {
    private val tokenBinProbs: Array<Array<Array<IntArray>>>
    private var whtNzLeft = 0
    private val whtNzTop: IntArray
    private val dctNzLeft: Array<IntArray>
    private val dctNzTop: Array<IntArray>
    fun encodeCoeffsWHT(bc: VPXBooleanEncoder, coeffs: IntArray, mbX: Int) {
        val nCoeff = fastCountCoeffWHT(coeffs)
        encodeCoeffs(bc, coeffs, 0, nCoeff, 1, (if (mbX == 0 || whtNzLeft <= 0) 0 else 1) + if (whtNzTop[mbX] > 0) 1 else 0)
        whtNzLeft = nCoeff
        whtNzTop[mbX] = nCoeff
    }

    fun encodeCoeffsDCT15(bc: VPXBooleanEncoder, coeffs: IntArray, mbX: Int, blkX: Int, blkY: Int) {
        val nCoeff = countCoeff(coeffs, 16)
        val blkAbsX = (mbX shl 2) + blkX
        encodeCoeffs(bc, coeffs, 1, nCoeff, 0, (if (blkAbsX == 0 || dctNzLeft[0][blkY] <= 0) 0 else 1) + if (dctNzTop[0][blkAbsX] > 0) 1 else 0)
        dctNzLeft[0][blkY] = Math.max(nCoeff - 1, 0)
        dctNzTop[0][blkAbsX] = Math.max(nCoeff - 1, 0)
    }

    fun encodeCoeffsDCT16(bc: VPXBooleanEncoder, coeffs: IntArray, mbX: Int, blkX: Int, blkY: Int) {
        val nCoeff = countCoeff(coeffs, 16)
        val blkAbsX = (mbX shl 2) + blkX
        encodeCoeffs(bc, coeffs, 0, nCoeff, 3, (if (blkAbsX == 0 || dctNzLeft[0][blkY] <= 0) 0 else 1) + if (dctNzTop[0][blkAbsX] > 0) 1 else 0)
        dctNzLeft[0][blkY] = nCoeff
        dctNzTop[0][blkAbsX] = nCoeff
    }

    fun encodeCoeffsDCTUV(bc: VPXBooleanEncoder, coeffs: IntArray, comp: Int, mbX: Int, blkX: Int, blkY: Int) {
        val nCoeff = countCoeff(coeffs, 16)
        val blkAbsX = (mbX shl 1) + blkX
        encodeCoeffs(bc, coeffs, 0, nCoeff, 2, (if (blkAbsX == 0 || dctNzLeft[comp][blkY] <= 0) 0 else 1)
                + if (dctNzTop[comp][blkAbsX] > 0) 1 else 0)
        dctNzLeft[comp][blkY] = nCoeff
        dctNzTop[comp][blkAbsX] = nCoeff
    }

    /**
     * Encodes DCT/WHT coefficients into the provided instance of a boolean
     * encoder
     *
     * @param bc
     * @param coeffs
     */
    fun encodeCoeffs(bc: VPXBooleanEncoder, coeffs: IntArray, firstCoeff: Int, nCoeff: Int, blkType: Int, ctx: Int) {
        var ctx = ctx
        var prevZero = false
        var i: Int
        i = firstCoeff
        while (i < nCoeff) {
            val probs = tokenBinProbs[blkType][coeffBandMapping[i]][ctx]
            val coeffAbs = MathUtil.abs(coeffs[i])
            if (!prevZero) bc.writeBit(probs[0], 1)
            if (coeffAbs == 0) {
                bc.writeBit(probs[1], 0)
                ctx = 0
            } else {
                bc.writeBit(probs[1], 1)
                if (coeffAbs == 1) {
                    bc.writeBit(probs[2], 0)
                    ctx = 1
                } else {
                    ctx = 2
                    bc.writeBit(probs[2], 1)
                    if (coeffAbs <= 4) {
                        bc.writeBit(probs[3], 0)
                        if (coeffAbs == 2) bc.writeBit(probs[4], 0) else {
                            bc.writeBit(probs[4], 1)
                            bc.writeBit(probs[5], coeffAbs - 3)
                        }
                    } else {
                        bc.writeBit(probs[3], 1)
                        if (coeffAbs <= 10) {
                            bc.writeBit(probs[6], 0)
                            if (coeffAbs <= 6) {
                                bc.writeBit(probs[7], 0)
                                bc.writeBit(159, coeffAbs - 5)
                            } else {
                                bc.writeBit(probs[7], 1)
                                val d = coeffAbs - 7
                                bc.writeBit(165, d shr 1)
                                bc.writeBit(145, d and 1)
                            }
                        } else {
                            bc.writeBit(probs[6], 1)
                            if (coeffAbs <= 34) {
                                bc.writeBit(probs[8], 0)
                                if (coeffAbs <= 18) {
                                    bc.writeBit(probs[9], 0)
                                    writeCat3Ext(bc, coeffAbs)
                                } else {
                                    bc.writeBit(probs[9], 1)
                                    writeCat4Ext(bc, coeffAbs)
                                }
                            } else {
                                bc.writeBit(probs[8], 1)
                                if (coeffAbs <= 66) {
                                    bc.writeBit(probs[10], 0)
                                    writeCatExt(bc, coeffAbs, 35, VPXConst.probCoeffExtCat5)
                                } else {
                                    bc.writeBit(probs[10], 1)
                                    writeCatExt(bc, coeffAbs, 67, VPXConst.probCoeffExtCat6)
                                }
                            }
                        }
                    }
                }
                bc.writeBit(128, MathUtil.sign(coeffs[i]))
            }
            prevZero = coeffAbs == 0
            i++
        }
        if (nCoeff < 16) {
            val probs = tokenBinProbs[blkType][coeffBandMapping[i]][ctx]
            bc.writeBit(probs[0], 0)
        }
    }

    /**
     * Counts number of non-zero coefficients for a WHT block, with shortcut as
     * most of them are likely to be non-zero
     *
     * @param coeffs
     * @return
     */
    private fun fastCountCoeffWHT(coeffs: IntArray): Int {
        return if (coeffs[15] != 0) 16 else countCoeff(coeffs, 15)
    }

    /**
     * Counts number of non-zero coefficients
     *
     * @param coeffs
     * @param nCoeff
     * @return
     */
    private fun countCoeff(coeffs: IntArray, nCoeff: Int): Int {
        var nCoeff = nCoeff
        while (nCoeff > 0) {
            --nCoeff
            if (coeffs[nCoeff] != 0) return nCoeff + 1
        }
        return nCoeff
    }

    companion object {
        val coeffBandMapping = intArrayOf(0, 1, 2, 3, 6, 4, 5, 6, 6, 6, 6, 6, 6, 6, 6, 7)
        private fun writeCat3Ext(bc: VPXBooleanEncoder, coeff: Int) {
            val d = coeff - 11
            bc.writeBit(173, d shr 2)
            bc.writeBit(148, d shr 1 and 1)
            bc.writeBit(140, d and 1)
        }

        private fun writeCat4Ext(bc: VPXBooleanEncoder, coeff: Int) {
            val d = coeff - 19
            bc.writeBit(176, d shr 3)
            bc.writeBit(155, d shr 2 and 1)
            bc.writeBit(140, d shr 1 and 1)
            bc.writeBit(135, d and 1)
        }

        private fun writeCatExt(bc: VPXBooleanEncoder, coeff: Int, catOff: Int, cat: IntArray) {
            val d = coeff - catOff
            var b = cat.size - 1
            var i = 0
            while (b >= 0) {
                bc.writeBit(cat[i++], d shr b and 1)
                b--
            }
        }
    }

    init {
        dctNzLeft = arrayOf(IntArray(4), IntArray(2), IntArray(2))
        this.tokenBinProbs = tokenBinProbs
        whtNzTop = IntArray(mbWidth)
        dctNzTop = arrayOf(IntArray(mbWidth shl 2), IntArray(mbWidth shl 1), IntArray(mbWidth shl 1))
    }
}