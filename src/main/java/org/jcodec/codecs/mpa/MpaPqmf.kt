package org.jcodec.codecs.mpa

import org.jcodec.codecs.mpa.MpaConst.dp
import org.jcodec.common.tools.MathUtil

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
object MpaPqmf {
    private const val MY_PI = 3.14159265358979323846
    private val cos1_64 = (1.0 / (2.0 * Math.cos(MY_PI / 64.0))).toFloat()
    private val cos3_64 = (1.0 / (2.0 * Math.cos(MY_PI * 3.0 / 64.0))).toFloat()
    private val cos5_64 = (1.0 / (2.0 * Math.cos(MY_PI * 5.0 / 64.0))).toFloat()
    private val cos7_64 = (1.0 / (2.0 * Math.cos(MY_PI * 7.0 / 64.0))).toFloat()
    private val cos9_64 = (1.0 / (2.0 * Math.cos(MY_PI * 9.0 / 64.0))).toFloat()
    private val cos11_64 = (1.0 / (2.0 * Math.cos(MY_PI * 11.0 / 64.0))).toFloat()
    private val cos13_64 = (1.0 / (2.0 * Math.cos(MY_PI * 13.0 / 64.0))).toFloat()
    private val cos15_64 = (1.0 / (2.0 * Math.cos(MY_PI * 15.0 / 64.0))).toFloat()
    private val cos17_64 = (1.0 / (2.0 * Math.cos(MY_PI * 17.0 / 64.0))).toFloat()
    private val cos19_64 = (1.0 / (2.0 * Math.cos(MY_PI * 19.0 / 64.0))).toFloat()
    private val cos21_64 = (1.0 / (2.0 * Math.cos(MY_PI * 21.0 / 64.0))).toFloat()
    private val cos23_64 = (1.0 / (2.0 * Math.cos(MY_PI * 23.0 / 64.0))).toFloat()
    private val cos25_64 = (1.0 / (2.0 * Math.cos(MY_PI * 25.0 / 64.0))).toFloat()
    private val cos27_64 = (1.0 / (2.0 * Math.cos(MY_PI * 27.0 / 64.0))).toFloat()
    private val cos29_64 = (1.0 / (2.0 * Math.cos(MY_PI * 29.0 / 64.0))).toFloat()
    private val cos31_64 = (1.0 / (2.0 * Math.cos(MY_PI * 31.0 / 64.0))).toFloat()
    private val cos1_32 = (1.0 / (2.0 * Math.cos(MY_PI / 32.0))).toFloat()
    private val cos3_32 = (1.0 / (2.0 * Math.cos(MY_PI * 3.0 / 32.0))).toFloat()
    private val cos5_32 = (1.0 / (2.0 * Math.cos(MY_PI * 5.0 / 32.0))).toFloat()
    private val cos7_32 = (1.0 / (2.0 * Math.cos(MY_PI * 7.0 / 32.0))).toFloat()
    private val cos9_32 = (1.0 / (2.0 * Math.cos(MY_PI * 9.0 / 32.0))).toFloat()
    private val cos11_32 = (1.0 / (2.0 * Math.cos(MY_PI * 11.0 / 32.0))).toFloat()
    private val cos13_32 = (1.0 / (2.0 * Math.cos(MY_PI * 13.0 / 32.0))).toFloat()
    private val cos15_32 = (1.0 / (2.0 * Math.cos(MY_PI * 15.0 / 32.0))).toFloat()
    private val cos1_16 = (1.0 / (2.0 * Math.cos(MY_PI / 16.0))).toFloat()
    private val cos3_16 = (1.0 / (2.0 * Math.cos(MY_PI * 3.0 / 16.0))).toFloat()
    private val cos5_16 = (1.0 / (2.0 * Math.cos(MY_PI * 5.0 / 16.0))).toFloat()
    private val cos7_16 = (1.0 / (2.0 * Math.cos(MY_PI * 7.0 / 16.0))).toFloat()
    private val cos1_8 = (1.0 / (2.0 * Math.cos(MY_PI / 8.0))).toFloat()
    private val cos3_8 = (1.0 / (2.0 * Math.cos(MY_PI * 3.0 / 8.0))).toFloat()
    private val cos1_4 = (1.0 / (2.0 * Math.cos(MY_PI / 4.0))).toFloat()
    private val bf32 = floatArrayOf(cos1_64, cos3_64, cos5_64, cos7_64, cos9_64, cos11_64, cos13_64, cos15_64,
            cos17_64, cos19_64, cos21_64, cos23_64, cos25_64, cos27_64, cos29_64, cos31_64)
    private val bf16 = floatArrayOf(cos1_32, cos3_32, cos5_32, cos7_32, cos9_32, cos11_32, cos13_32, cos15_32)
    private val bf8 = floatArrayOf(cos1_16, cos3_16, cos5_16, cos7_16)
    fun computeFilter(sampleOff: Int, samples: FloatArray, out: ShortArray, outOff: Int, scalefactor: Float) {
        var dvp = 0
        for (i in 0..31) {
            var pcm_sample: Float
            val b = i shl 4
            pcm_sample = ((samples[(16 + sampleOff and 0xf) + dvp] * dp[b + 0] + samples[(15 + sampleOff and 0xf) + dvp] * dp[b + 1] + samples[(14 + sampleOff and 0xf) + dvp] * dp[b + 2] + samples[(13 + sampleOff and 0xf) + dvp] * dp[b + 3] + samples[(12 + sampleOff and 0xf) + dvp] * dp[b + 4] + samples[(11 + sampleOff and 0xf) + dvp] * dp[b + 5] + samples[(10 + sampleOff and 0xf) + dvp] * dp[b + 6] + samples[(9 + sampleOff and 0xf) + dvp] * dp[b + 7] + samples[(8 + sampleOff and 0xf) + dvp] * dp[b + 8] + samples[(7 + sampleOff and 0xf) + dvp] * dp[b + 9] + samples[(6 + sampleOff and 0xf) + dvp] * dp[b + 10] + samples[(5 + sampleOff and 0xf) + dvp] * dp[b + 11] + samples[(4 + sampleOff and 0xf) + dvp] * dp.get(b + 12) + samples[(3 + sampleOff and 0xf) + dvp] * dp.get(b + 13) + samples[(2 + sampleOff and 0xf) + dvp] * dp[b + 14] + samples[(1 + sampleOff and 0xf) + dvp] * dp[b + 15]) * scalefactor)
            out[outOff + i] = MathUtil.clip(pcm_sample.toInt(), -0x8000, 0x7fff).toShort()
            dvp += 16
        }
    }

    fun computeButterfly(pos: Int, s: FloatArray) {
        butterfly32(s)
        butterfly16L(s)
        butterfly16H(s)
        butterfly8L(s, 0)
        butterfly8H(s, 0)
        butterfly8L(s, 16)
        butterfly8H(s, 16)
        run {
            var i = 0
            while (i < 32) {
                butterfly4L(s, i)
                butterfly4H(s, i)
                i += 8
            }
        }
        var i = 0
        while (i < 32) {
            butterfly2L(s, i)
            butterfly2H(s, i)
            i += 4
        }
        val k0 = -s[14] - s[15] - s[10] - s[11]
        val k1 = s[29] + s[31] + s[25]
        val k2 = k1 + s[17]
        val k3 = k1 + s[21] + s[23]
        val k4 = s[15] + s[11]
        val k5 = s[15] + s[13] + s[9]
        val k6 = s[7] + s[5]
        val k7 = s[31] + s[23]
        val k8 = k7 + s[27]
        val k9 = s[31] + s[27] + s[19]
        val k10 = -s[26] - s[27] - s[30] - s[31]
        val k11 = -s[24] - s[28] - s[30] - s[31]
        val k12 = s[20] + s[22] + s[23]
        val k13 = s[21] + s[29]
        val s0 = s[0]
        val s1 = s[1]
        val s2 = s[2]
        val s3 = s[3]
        val s4 = s[4]
        val s6 = s[6]
        val s7 = s[7]
        val s8 = s[8]
        val s12 = s[12]
        val s13 = s[13]
        val s14 = s[14]
        val s15 = s[15]
        val s16 = s[16]
        val s18 = s[18]
        val s19 = s[19]
        val s21 = s[21]
        val s22 = s[22]
        val s23 = s[23]
        val s28 = s[28]
        val s29 = s[29]
        val s30 = s[30]
        val s31 = s[31]
        s[0] = s1
        s[1] = k2
        s[2] = k5
        s[3] = k3
        s[4] = k6
        s[5] = k8 + k13
        s[6] = k4 + s13
        s[7] = k9 + s29
        s[8] = s3
        s[9] = k9
        s[10] = k4
        s[11] = k8
        s[12] = s7
        s[13] = k7
        s[14] = s15
        s[15] = s31
        s[16] = -k2 - s30
        s[17] = -k5 - s14
        s[18] = -k3 - s22 - s30
        s[19] = -k6 - s6
        s[20] = k10 - s29 - s21 - s22 - s23
        s[21] = k0 - s13
        s[22] = k10 - s29 - s18 - s19
        s[23] = -s3 - s2
        s[24] = k10 - s28 - s18 - s19
        s[25] = k0 - s12
        s[26] = k10 - s28 - k12
        s[27] = -s6 - s7 - s4
        s[28] = k11 - k12
        s[29] = -s14 - s15 - s12 - s8
        s[30] = k11 - s16
        s[31] = -s0
    }

    private fun butterfly16H(s: FloatArray) {
        for (i in 0..7) {
            val tmp0 = s[16 + i]
            val tmp1 = s[31 - i]
            s[16 + i] = tmp0 + tmp1
            s[31 - i] = -(tmp0 - tmp1) * bf16[i]
        }
    }

    private fun butterfly16L(s: FloatArray) {
        for (i in 0..7) {
            val tmp0 = s[i]
            val tmp1 = s[15 - i]
            s[i] = tmp0 + tmp1
            s[15 - i] = (tmp0 - tmp1) * bf16[i]
        }
    }

    private fun butterfly8H(s: FloatArray, o: Int) {
        for (i in 0..3) {
            val tmp0 = s[o + 8 + i]
            val tmp1 = s[o + 15 - i]
            s[o + 8 + i] = tmp0 + tmp1
            s[o + 15 - i] = -(tmp0 - tmp1) * bf8[i]
        }
    }

    private fun butterfly8L(s: FloatArray, o: Int) {
        for (i in 0..3) {
            val tmp0 = s[o + i]
            val tmp1 = s[o + 7 - i]
            s[o + i] = tmp0 + tmp1
            s[o + 7 - i] = (tmp0 - tmp1) * bf8[i]
        }
    }

    private fun butterfly4H(s: FloatArray, o: Int) {
        val tmp0 = s[o + 4]
        val tmp1 = s[o + 7]
        s[o + 4] = tmp0 + tmp1
        s[o + 7] = -(tmp0 - tmp1) * cos1_8
        val tmp2 = s[o + 5]
        val tmp3 = s[o + 6]
        s[o + 5] = tmp2 + tmp3
        s[o + 6] = -(tmp2 - tmp3) * cos3_8
    }

    private fun butterfly4L(s: FloatArray, o: Int) {
        val tmp0 = s[o]
        val tmp1 = s[o + 3]
        s[o + 0] = tmp0 + tmp1
        s[o + 3] = (tmp0 - tmp1) * cos1_8
        val tmp2 = s[o + 1]
        val tmp3 = s[o + 2]
        s[o + 1] = tmp2 + tmp3
        s[o + 2] = (tmp2 - tmp3) * cos3_8
    }

    private fun butterfly2H(s: FloatArray, o: Int) {
        val tmp0 = s[o + 2]
        val tmp1 = s[o + 3]
        s[o + 2] = tmp0 + tmp1
        s[o + 3] = -(tmp0 - tmp1) * cos1_4
    }

    private fun butterfly2L(s: FloatArray, o: Int) {
        val tmp0 = s[o]
        val tmp1 = s[o + 1]
        s[o + 0] = tmp0 + tmp1
        s[o + 1] = (tmp0 - tmp1) * cos1_4
    }

    private fun butterfly32(s: FloatArray) {
        for (i in 0..15) {
            val tmp0 = s[i]
            val tmp1 = s[31 - i]
            s[i] = tmp0 + tmp1
            s[31 - i] = (tmp0 - tmp1) * bf32[i]
        }
    }
}