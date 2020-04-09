package org.jcodec.codecs.vpx.vp9

import org.jcodec.codecs.vpx.VPXBooleanDecoder
import org.jcodec.codecs.vpx.vp9.Consts.BLOCK_16X16
import org.jcodec.codecs.vpx.vp9.Consts.BLOCK_16X32
import org.jcodec.codecs.vpx.vp9.Consts.BLOCK_16X8
import org.jcodec.codecs.vpx.vp9.Consts.BLOCK_32X16
import org.jcodec.codecs.vpx.vp9.Consts.BLOCK_32X32
import org.jcodec.codecs.vpx.vp9.Consts.BLOCK_32X64
import org.jcodec.codecs.vpx.vp9.Consts.BLOCK_4X4
import org.jcodec.codecs.vpx.vp9.Consts.BLOCK_4X8
import org.jcodec.codecs.vpx.vp9.Consts.BLOCK_64X32
import org.jcodec.codecs.vpx.vp9.Consts.BLOCK_64X64
import org.jcodec.codecs.vpx.vp9.Consts.BLOCK_8X16
import org.jcodec.codecs.vpx.vp9.Consts.BLOCK_8X4
import org.jcodec.codecs.vpx.vp9.Consts.BLOCK_8X8
import org.jcodec.codecs.vpx.vp9.Consts.BLOCK_INVALID
import org.jcodec.codecs.vpx.vp9.Consts.DCT_VAL_CAT1
import org.jcodec.codecs.vpx.vp9.Consts.DCT_VAL_CAT3
import org.jcodec.codecs.vpx.vp9.Consts.PARETO_TABLE
import org.jcodec.codecs.vpx.vp9.Consts.TOKEN_TREE
import org.jcodec.codecs.vpx.vp9.Consts.TX_4X4
import org.jcodec.codecs.vpx.vp9.Consts.blH
import org.jcodec.codecs.vpx.vp9.Consts.blW
import org.jcodec.codecs.vpx.vp9.Consts.cat_probs
import org.jcodec.codecs.vpx.vp9.Consts.coefband_4x4
import org.jcodec.codecs.vpx.vp9.Consts.coefband_8x8plus
import org.jcodec.codecs.vpx.vp9.Consts.extra_bits

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
open class Residual {
    var coefs: Array<Array<IntArray?>?> = emptyArray()
        private set

    constructor(coefs: Array<Array<IntArray?>?>) {
        this.coefs = coefs
    }

    constructor() {}

    fun read(miCol: Int, miRow: Int, blType: Int, decoder: VPXBooleanDecoder?, c: DecodingContext,
             modeInfo: ModeInfo) {
        if (modeInfo.isSkip) return
        val subXRound = (1 shl c.subX) - 1
        val subYRound = (1 shl c.subY) - 1
        val coefs: Array<Array<IntArray?>?> = arrayOfNulls(3)
        for (pl in 0..2) {
            val txSize = if (pl == 0) modeInfo.txSize else Consts.uv_txsize_lookup[blType][modeInfo.txSize][c.subX][c.subY]
            val step4x4 = 1 shl txSize
            var n4w = 1 shl blW.get(blType)
            var n4h = 1 shl blH.get(blType)
            if (pl != 0) {
                n4w = n4w shr c.subX
                n4h = n4h shr c.subY
            }
            var extra4w = (miCol shl 1) + n4w - (c.getFrameWidth() + 3 shr 2)
            var extra4h = (miRow shl 1) + n4h - (c.getFrameHeight() + 3 shr 2)
            var startBlkX = miCol shl 1
            var startBlkY = miRow shl 1
            if (pl != 0) {
                extra4w = extra4w + subXRound shr c.subX
                extra4h = extra4h + subYRound shr c.subY
                startBlkX = startBlkX shr c.subX
                startBlkY = startBlkY shr c.subY
            }
            val max4w = n4w - if (extra4w > 0) extra4w else 0
            val max4h = n4h - if (extra4h > 0) extra4h else 0
            coefs[pl] = arrayOfNulls(n4w * n4h)
            var y = 0
            while (y < max4h) {
                var x = 0
                while (x < max4w) {
                    val blkCol = startBlkX + x
                    val blkRow = startBlkY + y
                    var predMode: Int
                    if (pl == 0) {
                        predMode = modeInfo.yMode
                        if (blType < BLOCK_8X8) predMode = ModeInfo.vect4get(modeInfo.subModes, (y shl 1) + x)
                    } else predMode = modeInfo.uvMode
                    coefs[pl]!![x + n4w * y] = readOneTU(if (pl == 0) 0 else 1, blkCol, blkRow, txSize, modeInfo.isInter,
                            predMode, decoder, c)
                    x += step4x4
                }
                y += step4x4
            }
        }
        this.coefs = coefs
    }

    open fun readOneTU(plane: Int, blkCol: Int, blkRow: Int, txSz: Int, isInter: Boolean, intraMode: Int,
                       decoder: VPXBooleanDecoder?, c: DecodingContext): IntArray? {
        val decoder = decoder!!
        val tokenCache = IntArray(16 shl (txSz shl 1))
        val maxCoeff = 16 shl (txSz shl 1)
        var expectMoreCoefs = false
        val txType = if (plane == 0 && !isInter) Consts.intra_mode_to_tx_type_lookup[intraMode] else Consts.DCT_DCT
        val scan = if (plane == 0 && !isInter) Scan.vp9_scan_orders[txSz][txType][0] else Scan.vp9_default_scan_orders[txSz][0]
        val neighbors = if (plane == 0 && !isInter) Scan.vp9_scan_orders[txSz][txType][2] else Scan.vp9_default_scan_orders[txSz][2]
        val coefs = IntArray(maxCoeff)
        var ctx = calcTokenContextCoef0(plane, txSz, blkCol, blkRow, c)
        for (cf in 0 until maxCoeff) {
            val band: Int = if (txSz == TX_4X4) coefband_4x4.get(cf) else coefband_8x8plus.get(cf)
            val pos = scan[cf]
            val probs = c.getCoefProbs()[txSz][if (plane > 0) 1 else 0][if (isInter) 1 else 0][band][ctx]
            if (!expectMoreCoefs) {
                val moreCoefs = decoder.readBit(probs[0]) == 1
                if (!moreCoefs) break
            }
            var coef: Int
            if (decoder.readBit(probs[1]) == 0) {
                tokenCache[pos] = 0
                expectMoreCoefs = true
            } else {
                expectMoreCoefs = false
                if (decoder.readBit(probs[2]) == 0) {
                    tokenCache[pos] = 1
                    coef = 1
                } else {
                    val token = decoder.readTree(TOKEN_TREE, PARETO_TABLE.get(probs[2] - 1))
                    if (token < DCT_VAL_CAT1) {
                        coef = token
                        if (token == Consts.TWO_TOKEN) tokenCache[pos] = 2 else tokenCache[pos] = 3
                    } else {
                        if (token < DCT_VAL_CAT3) tokenCache[pos] = 4 else tokenCache[pos] = 5
                        coef = readCoef(token, decoder, c)
                    }
                }
                val sign = decoder.readBitEq()
                coefs[pos] = if (sign == 1) -coef else coef
            }
            ctx = 1 + tokenCache[neighbors[2 * cf + 2]] + tokenCache[neighbors[2 * cf + 3]] shr 1
            println("CTX: $ctx")
        }
        return coefs
    }

    companion object {
        @JvmStatic
        fun readResidual(miCol: Int, miRow: Int, blSz: Int, decoder: VPXBooleanDecoder, c: DecodingContext,
                         mode: ModeInfo): Residual {
            val ret = Residual()
            ret.read(miCol, miRow, blSz, decoder, c, mode)
            return ret
        }

        var blk_size_lookup = arrayOf(intArrayOf(BLOCK_INVALID, BLOCK_4X4, BLOCK_8X4), intArrayOf(BLOCK_4X8, BLOCK_8X8, BLOCK_16X8), intArrayOf(BLOCK_8X16, BLOCK_16X16, BLOCK_32X16), intArrayOf(BLOCK_16X32, BLOCK_32X32, BLOCK_64X32), intArrayOf(BLOCK_32X64, BLOCK_64X64, BLOCK_INVALID))
        private fun readCoef(token: Int, decoder: VPXBooleanDecoder, c: DecodingContext): Int {
            val cat: Int = extra_bits.get(token).get(0)
            val numExtra: Int = extra_bits.get(token).get(1)
            var coef: Int = extra_bits.get(token).get(2)
            // if (token == DCT_VAL_CAT6) {
            // for (int bit = 0; bit < c.getBitDepth() - 8; bit++) {
            // int high_bit = decoder.readBit(255);
            //
            // coef += high_bit << (5 + c.getBitDepth() - bit);
            // }
            // }
            for (bit in 0 until numExtra) {
                val coef_bit = decoder.readBit(cat_probs.get(cat).get(bit))
                coef += coef_bit shl numExtra - 1 - bit
            }
            return coef
        }

        private fun calcTokenContextCoef0(plane: Int, txSz: Int, blkCol: Int, blkRow: Int, c: DecodingContext): Int {
            val aboveNonzeroContext = c.getAboveNonzeroContext()
            val leftNonzeroContext = c.getLeftNonzeroContext()
            val subX = if (plane > 0) c.subX else 0
            val subY = if (plane > 0) c.subY else 0
            val max4x = c.miFrameWidth shl 1 shr subX
            val max4y = c.miFrameHeight shl 1 shr subY
            val tx4 = 1 shl txSz
            var aboveNz = 0
            var leftNz = 0
            for (i in 0 until tx4) {
                if (blkCol + i < max4x) aboveNz = aboveNz or aboveNonzeroContext[plane][blkCol + i]
                if (blkRow + i < max4y) leftNz = leftNz or leftNonzeroContext[plane][blkRow + i and 0xf]
            }
            return aboveNz + leftNz
        }
    }
}