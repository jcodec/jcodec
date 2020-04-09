package org.jcodec.codecs.mpeg4

import org.jcodec.codecs.mpeg4.Macroblock.Companion.vec

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
object MPEG4Consts {
    @JvmField
    val ZERO_MV = vec()
    const val BS_VERSION_BUGGY_DC_CLIP = 34
    const val MODE_INTER = 0
    const val MODE_INTER_Q = 1
    const val MODE_INTER4V = 2
    const val MODE_INTRA = 3
    const val MODE_INTRA_Q = 4
    const val MODE_NOT_CODED = 16
    const val MODE_NOT_CODED_GMC = 17
    const val MODE_DIRECT = 0
    const val MODE_INTERPOLATE = 1
    const val MODE_BACKWARD = 2
    const val MODE_FORWARD = 3
    const val MODE_DIRECT_NONE_MV = 4
    const val MODE_DIRECT_NO4V = 5
    @JvmField
    val ROUNDTAB_76 = intArrayOf(0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1)
    @JvmField
    val ROUNDTAB_79 = intArrayOf(0, 1, 0, 0)
    const val ALT_CHROMA_ROUNDING = 1

    //@formatter:off
    @JvmField
    val INTRA_DC_THRESHOLD_TABLE = intArrayOf(32, 13, 15, 17, 19, 21, 23, 1)
    @JvmField
    val DEFAULT_INTRA_MATRIX = shortArrayOf(
            8, 17, 18, 19, 21, 23, 25, 27,
            17, 18, 19, 21, 23, 25, 27, 28,
            20, 21, 22, 23, 24, 26, 28, 30,
            21, 22, 23, 24, 26, 28, 30, 32,
            22, 23, 24, 26, 28, 30, 32, 35,
            23, 24, 26, 28, 30, 32, 35, 38,
            25, 26, 28, 30, 32, 35, 38, 41,
            27, 28, 30, 32, 35, 38, 41, 45
    )
    @JvmField
    val DEFAULT_INTER_MATRIX = shortArrayOf(
            16, 17, 18, 19, 20, 21, 22, 23,
            17, 18, 19, 20, 21, 22, 23, 24,
            18, 19, 20, 21, 22, 23, 24, 25,
            19, 20, 21, 22, 23, 24, 26, 27,
            20, 21, 22, 23, 25, 26, 27, 28,
            21, 22, 23, 24, 26, 27, 28, 30,
            22, 23, 24, 26, 27, 28, 30, 31,
            23, 24, 25, 27, 28, 30, 31, 33
    )
    @JvmField
    val MCBPC_INTRA_TABLE = arrayOf(intArrayOf(-1, 0), intArrayOf(20, 6), intArrayOf(36, 6), intArrayOf(52, 6), intArrayOf(4, 4), intArrayOf(4, 4), intArrayOf(4, 4), intArrayOf(4, 4), intArrayOf(19, 3), intArrayOf(19, 3), intArrayOf(19, 3), intArrayOf(19, 3), intArrayOf(19, 3), intArrayOf(19, 3), intArrayOf(19, 3), intArrayOf(19, 3), intArrayOf(35, 3), intArrayOf(35, 3), intArrayOf(35, 3), intArrayOf(35, 3), intArrayOf(35, 3), intArrayOf(35, 3), intArrayOf(35, 3), intArrayOf(35, 3), intArrayOf(51, 3), intArrayOf(51, 3), intArrayOf(51, 3), intArrayOf(51, 3), intArrayOf(51, 3), intArrayOf(51, 3), intArrayOf(51, 3), intArrayOf(51, 3), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1), intArrayOf(3, 1))
    @JvmField
    val MCBPC_INTER_TABLE = arrayOf(intArrayOf(-1, 0), intArrayOf(255, 9), intArrayOf(52, 9), intArrayOf(36, 9), intArrayOf(20, 9), intArrayOf(49, 9), intArrayOf(35, 8), intArrayOf(35, 8), intArrayOf(19, 8), intArrayOf(19, 8), intArrayOf(50, 8), intArrayOf(50, 8), intArrayOf(51, 7), intArrayOf(51, 7), intArrayOf(51, 7), intArrayOf(51, 7), intArrayOf(34, 7), intArrayOf(34, 7), intArrayOf(34, 7), intArrayOf(34, 7), intArrayOf(18, 7), intArrayOf(18, 7), intArrayOf(18, 7), intArrayOf(18, 7), intArrayOf(33, 7), intArrayOf(33, 7), intArrayOf(33, 7), intArrayOf(33, 7), intArrayOf(17, 7), intArrayOf(17, 7), intArrayOf(17, 7), intArrayOf(17, 7), intArrayOf(4, 6), intArrayOf(4, 6), intArrayOf(4, 6), intArrayOf(4, 6), intArrayOf(4, 6), intArrayOf(4, 6), intArrayOf(4, 6), intArrayOf(4, 6), intArrayOf(48, 6), intArrayOf(48, 6), intArrayOf(48, 6), intArrayOf(48, 6), intArrayOf(48, 6), intArrayOf(48, 6), intArrayOf(48, 6), intArrayOf(48, 6), intArrayOf(3, 5), intArrayOf(3, 5), intArrayOf(3, 5), intArrayOf(3, 5), intArrayOf(3, 5), intArrayOf(3, 5), intArrayOf(3, 5), intArrayOf(3, 5), intArrayOf(3, 5), intArrayOf(3, 5), intArrayOf(3, 5), intArrayOf(3, 5), intArrayOf(3, 5), intArrayOf(3, 5), intArrayOf(3, 5), intArrayOf(3, 5), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(32, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(16, 4), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(1, 3), intArrayOf(0, 1))
    @JvmField
    val CBPY_TABLE = arrayOf(intArrayOf(-1, 0), intArrayOf(-1, 0), intArrayOf(6, 6), intArrayOf(9, 6), intArrayOf(8, 5), intArrayOf(8, 5), intArrayOf(4, 5), intArrayOf(4, 5), intArrayOf(2, 5), intArrayOf(2, 5), intArrayOf(1, 5), intArrayOf(1, 5), intArrayOf(0, 4), intArrayOf(0, 4), intArrayOf(0, 4), intArrayOf(0, 4), intArrayOf(12, 4), intArrayOf(12, 4), intArrayOf(12, 4), intArrayOf(12, 4), intArrayOf(10, 4), intArrayOf(10, 4), intArrayOf(10, 4), intArrayOf(10, 4), intArrayOf(14, 4), intArrayOf(14, 4), intArrayOf(14, 4), intArrayOf(14, 4), intArrayOf(5, 4), intArrayOf(5, 4), intArrayOf(5, 4), intArrayOf(5, 4), intArrayOf(13, 4), intArrayOf(13, 4), intArrayOf(13, 4), intArrayOf(13, 4), intArrayOf(3, 4), intArrayOf(3, 4), intArrayOf(3, 4), intArrayOf(3, 4), intArrayOf(11, 4), intArrayOf(11, 4), intArrayOf(11, 4), intArrayOf(11, 4), intArrayOf(7, 4), intArrayOf(7, 4), intArrayOf(7, 4), intArrayOf(7, 4), intArrayOf(15, 2), intArrayOf(15, 2), intArrayOf(15, 2), intArrayOf(15, 2), intArrayOf(15, 2), intArrayOf(15, 2), intArrayOf(15, 2), intArrayOf(15, 2), intArrayOf(15, 2), intArrayOf(15, 2), intArrayOf(15, 2), intArrayOf(15, 2), intArrayOf(15, 2), intArrayOf(15, 2), intArrayOf(15, 2), intArrayOf(15, 2))
    @JvmField
    val TMNMV_TAB_0 = arrayOf(intArrayOf(3, 4), intArrayOf(-3, 4), intArrayOf(2, 3), intArrayOf(2, 3), intArrayOf(-2, 3), intArrayOf(-2, 3), intArrayOf(1, 2), intArrayOf(1, 2), intArrayOf(1, 2), intArrayOf(1, 2), intArrayOf(-1, 2), intArrayOf(-1, 2), intArrayOf(-1, 2), intArrayOf(-1, 2))
    @JvmField
    val TMNMV_TAB_1 = arrayOf(intArrayOf(12, 10), intArrayOf(-12, 10), intArrayOf(11, 10), intArrayOf(-11, 10), intArrayOf(10, 9), intArrayOf(10, 9), intArrayOf(-10, 9), intArrayOf(-10, 9), intArrayOf(9, 9), intArrayOf(9, 9), intArrayOf(-9, 9), intArrayOf(-9, 9), intArrayOf(8, 9), intArrayOf(8, 9), intArrayOf(-8, 9), intArrayOf(-8, 9), intArrayOf(7, 7), intArrayOf(7, 7), intArrayOf(7, 7), intArrayOf(7, 7), intArrayOf(7, 7), intArrayOf(7, 7), intArrayOf(7, 7), intArrayOf(7, 7), intArrayOf(-7, 7), intArrayOf(-7, 7), intArrayOf(-7, 7), intArrayOf(-7, 7), intArrayOf(-7, 7), intArrayOf(-7, 7), intArrayOf(-7, 7), intArrayOf(-7, 7), intArrayOf(6, 7), intArrayOf(6, 7), intArrayOf(6, 7), intArrayOf(6, 7), intArrayOf(6, 7), intArrayOf(6, 7), intArrayOf(6, 7), intArrayOf(6, 7), intArrayOf(-6, 7), intArrayOf(-6, 7), intArrayOf(-6, 7), intArrayOf(-6, 7), intArrayOf(-6, 7), intArrayOf(-6, 7), intArrayOf(-6, 7), intArrayOf(-6, 7), intArrayOf(5, 7), intArrayOf(5, 7), intArrayOf(5, 7), intArrayOf(5, 7), intArrayOf(5, 7), intArrayOf(5, 7), intArrayOf(5, 7), intArrayOf(5, 7), intArrayOf(-5, 7), intArrayOf(-5, 7), intArrayOf(-5, 7), intArrayOf(-5, 7), intArrayOf(-5, 7), intArrayOf(-5, 7), intArrayOf(-5, 7), intArrayOf(-5, 7), intArrayOf(4, 6), intArrayOf(4, 6), intArrayOf(4, 6), intArrayOf(4, 6), intArrayOf(4, 6), intArrayOf(4, 6), intArrayOf(4, 6), intArrayOf(4, 6), intArrayOf(4, 6), intArrayOf(4, 6), intArrayOf(4, 6), intArrayOf(4, 6), intArrayOf(4, 6), intArrayOf(4, 6), intArrayOf(4, 6), intArrayOf(4, 6), intArrayOf(-4, 6), intArrayOf(-4, 6), intArrayOf(-4, 6), intArrayOf(-4, 6), intArrayOf(-4, 6), intArrayOf(-4, 6), intArrayOf(-4, 6), intArrayOf(-4, 6), intArrayOf(-4, 6), intArrayOf(-4, 6), intArrayOf(-4, 6), intArrayOf(-4, 6), intArrayOf(-4, 6), intArrayOf(-4, 6), intArrayOf(-4, 6), intArrayOf(-4, 6))
    @JvmField
    val TMNMV_TAB_2 = arrayOf(intArrayOf(32, 12), intArrayOf(-32, 12), intArrayOf(31, 12), intArrayOf(-31, 12), intArrayOf(30, 11), intArrayOf(30, 11), intArrayOf(-30, 11), intArrayOf(-30, 11), intArrayOf(29, 11), intArrayOf(29, 11), intArrayOf(-29, 11), intArrayOf(-29, 11), intArrayOf(28, 11), intArrayOf(28, 11), intArrayOf(-28, 11), intArrayOf(-28, 11), intArrayOf(27, 11), intArrayOf(27, 11), intArrayOf(-27, 11), intArrayOf(-27, 11), intArrayOf(26, 11), intArrayOf(26, 11), intArrayOf(-26, 11), intArrayOf(-26, 11), intArrayOf(25, 11), intArrayOf(25, 11), intArrayOf(-25, 11), intArrayOf(-25, 11), intArrayOf(24, 10), intArrayOf(24, 10), intArrayOf(24, 10), intArrayOf(24, 10), intArrayOf(-24, 10), intArrayOf(-24, 10), intArrayOf(-24, 10), intArrayOf(-24, 10), intArrayOf(23, 10), intArrayOf(23, 10), intArrayOf(23, 10), intArrayOf(23, 10), intArrayOf(-23, 10), intArrayOf(-23, 10), intArrayOf(-23, 10), intArrayOf(-23, 10), intArrayOf(22, 10), intArrayOf(22, 10), intArrayOf(22, 10), intArrayOf(22, 10), intArrayOf(-22, 10), intArrayOf(-22, 10), intArrayOf(-22, 10), intArrayOf(-22, 10), intArrayOf(21, 10), intArrayOf(21, 10), intArrayOf(21, 10), intArrayOf(21, 10), intArrayOf(-21, 10), intArrayOf(-21, 10), intArrayOf(-21, 10), intArrayOf(-21, 10), intArrayOf(20, 10), intArrayOf(20, 10), intArrayOf(20, 10), intArrayOf(20, 10), intArrayOf(-20, 10), intArrayOf(-20, 10), intArrayOf(-20, 10), intArrayOf(-20, 10), intArrayOf(19, 10), intArrayOf(19, 10), intArrayOf(19, 10), intArrayOf(19, 10), intArrayOf(-19, 10), intArrayOf(-19, 10), intArrayOf(-19, 10), intArrayOf(-19, 10), intArrayOf(18, 10), intArrayOf(18, 10), intArrayOf(18, 10), intArrayOf(18, 10), intArrayOf(-18, 10), intArrayOf(-18, 10), intArrayOf(-18, 10), intArrayOf(-18, 10), intArrayOf(17, 10), intArrayOf(17, 10), intArrayOf(17, 10), intArrayOf(17, 10), intArrayOf(-17, 10), intArrayOf(-17, 10), intArrayOf(-17, 10), intArrayOf(-17, 10), intArrayOf(16, 10), intArrayOf(16, 10), intArrayOf(16, 10), intArrayOf(16, 10), intArrayOf(-16, 10), intArrayOf(-16, 10), intArrayOf(-16, 10), intArrayOf(-16, 10), intArrayOf(15, 10), intArrayOf(15, 10), intArrayOf(15, 10), intArrayOf(15, 10), intArrayOf(-15, 10), intArrayOf(-15, 10), intArrayOf(-15, 10), intArrayOf(-15, 10), intArrayOf(14, 10), intArrayOf(14, 10), intArrayOf(14, 10), intArrayOf(14, 10), intArrayOf(-14, 10), intArrayOf(-14, 10), intArrayOf(-14, 10), intArrayOf(-14, 10), intArrayOf(13, 10), intArrayOf(13, 10), intArrayOf(13, 10), intArrayOf(13, 10), intArrayOf(-13, 10), intArrayOf(-13, 10), intArrayOf(-13, 10), intArrayOf(-13, 10))
    @JvmField
    val MAX_LEVEL = arrayOf(arrayOf(byteArrayOf(
            12, 6, 4, 3, 3, 3, 3, 2,
            2, 2, 2, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0
    ), byteArrayOf(
            3, 2, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0
    )), arrayOf(byteArrayOf(
            27, 10, 5, 4, 3, 3, 3, 3,
            2, 2, 1, 1, 1, 1, 1, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0
    ), byteArrayOf(
            8, 3, 2, 2, 2, 2, 2, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0
    )))
    @JvmField
    val MAX_RUN = arrayOf(arrayOf(byteArrayOf(
            0, 26, 10, 6, 2, 1, 1, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0), byteArrayOf(
            0, 40, 1, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0)), arrayOf(byteArrayOf(
            0, 14, 9, 7, 3, 2, 1, 1,
            1, 1, 1, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0), byteArrayOf(
            0, 20, 6, 1, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0)))
    @JvmField
    val COEFF_TAB = arrayOf(arrayOf(intArrayOf(2, 2, 0, 0, 1), intArrayOf(15, 4, 0, 0, 2), intArrayOf(21, 6, 0, 0, 3), intArrayOf(23, 7, 0, 0, 4), intArrayOf(31, 8, 0, 0, 5), intArrayOf(37, 9, 0, 0, 6), intArrayOf(36, 9, 0, 0, 7), intArrayOf(33, 10, 0, 0, 8), intArrayOf(32, 10, 0, 0, 9), intArrayOf(7, 11, 0, 0, 10), intArrayOf(6, 11, 0, 0, 11), intArrayOf(32, 11, 0, 0, 12), intArrayOf(6, 3, 0, 1, 1), intArrayOf(20, 6, 0, 1, 2), intArrayOf(30, 8, 0, 1, 3), intArrayOf(15, 10, 0, 1, 4), intArrayOf(33, 11, 0, 1, 5), intArrayOf(80, 12, 0, 1, 6), intArrayOf(14, 4, 0, 2, 1), intArrayOf(29, 8, 0, 2, 2), intArrayOf(14, 10, 0, 2, 3), intArrayOf(81, 12, 0, 2, 4), intArrayOf(13, 5, 0, 3, 1), intArrayOf(35, 9, 0, 3, 2), intArrayOf(13, 10, 0, 3, 3), intArrayOf(12, 5, 0, 4, 1), intArrayOf(34, 9, 0, 4, 2), intArrayOf(82, 12, 0, 4, 3), intArrayOf(11, 5, 0, 5, 1), intArrayOf(12, 10, 0, 5, 2), intArrayOf(83, 12, 0, 5, 3), intArrayOf(19, 6, 0, 6, 1), intArrayOf(11, 10, 0, 6, 2), intArrayOf(84, 12, 0, 6, 3), intArrayOf(18, 6, 0, 7, 1), intArrayOf(10, 10, 0, 7, 2), intArrayOf(17, 6, 0, 8, 1), intArrayOf(9, 10, 0, 8, 2), intArrayOf(16, 6, 0, 9, 1), intArrayOf(8, 10, 0, 9, 2), intArrayOf(22, 7, 0, 10, 1), intArrayOf(85, 12, 0, 10, 2), intArrayOf(21, 7, 0, 11, 1), intArrayOf(20, 7, 0, 12, 1), intArrayOf(28, 8, 0, 13, 1), intArrayOf(27, 8, 0, 14, 1), intArrayOf(33, 9, 0, 15, 1), intArrayOf(32, 9, 0, 16, 1), intArrayOf(31, 9, 0, 17, 1), intArrayOf(30, 9, 0, 18, 1), intArrayOf(29, 9, 0, 19, 1), intArrayOf(28, 9, 0, 20, 1), intArrayOf(27, 9, 0, 21, 1), intArrayOf(26, 9, 0, 22, 1), intArrayOf(34, 11, 0, 23, 1), intArrayOf(35, 11, 0, 24, 1), intArrayOf(86, 12, 0, 25, 1), intArrayOf(87, 12, 0, 26, 1), intArrayOf(7, 4, 1, 0, 1), intArrayOf(25, 9, 1, 0, 2), intArrayOf(5, 11, 1, 0, 3), intArrayOf(15, 6, 1, 1, 1), intArrayOf(4, 11, 1, 1, 2), intArrayOf(14, 6, 1, 2, 1), intArrayOf(13, 6, 1, 3, 1), intArrayOf(12, 6, 1, 4, 1), intArrayOf(19, 7, 1, 5, 1), intArrayOf(18, 7, 1, 6, 1), intArrayOf(17, 7, 1, 7, 1), intArrayOf(16, 7, 1, 8, 1), intArrayOf(26, 8, 1, 9, 1), intArrayOf(25, 8, 1, 10, 1), intArrayOf(24, 8, 1, 11, 1), intArrayOf(23, 8, 1, 12, 1), intArrayOf(22, 8, 1, 13, 1), intArrayOf(21, 8, 1, 14, 1), intArrayOf(20, 8, 1, 15, 1), intArrayOf(19, 8, 1, 16, 1), intArrayOf(24, 9, 1, 17, 1), intArrayOf(23, 9, 1, 18, 1), intArrayOf(22, 9, 1, 19, 1), intArrayOf(21, 9, 1, 20, 1), intArrayOf(20, 9, 1, 21, 1), intArrayOf(19, 9, 1, 22, 1), intArrayOf(18, 9, 1, 23, 1), intArrayOf(17, 9, 1, 24, 1), intArrayOf(7, 10, 1, 25, 1), intArrayOf(6, 10, 1, 26, 1), intArrayOf(5, 10, 1, 27, 1), intArrayOf(4, 10, 1, 28, 1), intArrayOf(36, 11, 1, 29, 1), intArrayOf(37, 11, 1, 30, 1), intArrayOf(38, 11, 1, 31, 1), intArrayOf(39, 11, 1, 32, 1), intArrayOf(88, 12, 1, 33, 1), intArrayOf(89, 12, 1, 34, 1), intArrayOf(90, 12, 1, 35, 1), intArrayOf(91, 12, 1, 36, 1), intArrayOf(92, 12, 1, 37, 1), intArrayOf(93, 12, 1, 38, 1), intArrayOf(94, 12, 1, 39, 1), intArrayOf(95, 12, 1, 40, 1)), arrayOf(intArrayOf(2, 2, 0, 0, 1), intArrayOf(15, 4, 0, 0, 3), intArrayOf(21, 6, 0, 0, 6), intArrayOf(23, 7, 0, 0, 9), intArrayOf(31, 8, 0, 0, 10), intArrayOf(37, 9, 0, 0, 13), intArrayOf(36, 9, 0, 0, 14), intArrayOf(33, 10, 0, 0, 17), intArrayOf(32, 10, 0, 0, 18), intArrayOf(7, 11, 0, 0, 21), intArrayOf(6, 11, 0, 0, 22), intArrayOf(32, 11, 0, 0, 23), intArrayOf(6, 3, 0, 0, 2), intArrayOf(20, 6, 0, 1, 2), intArrayOf(30, 8, 0, 0, 11), intArrayOf(15, 10, 0, 0, 19), intArrayOf(33, 11, 0, 0, 24), intArrayOf(80, 12, 0, 0, 25), intArrayOf(14, 4, 0, 1, 1), intArrayOf(29, 8, 0, 0, 12), intArrayOf(14, 10, 0, 0, 20), intArrayOf(81, 12, 0, 0, 26), intArrayOf(13, 5, 0, 0, 4), intArrayOf(35, 9, 0, 0, 15), intArrayOf(13, 10, 0, 1, 7), intArrayOf(12, 5, 0, 0, 5), intArrayOf(34, 9, 0, 4, 2), intArrayOf(82, 12, 0, 0, 27), intArrayOf(11, 5, 0, 2, 1), intArrayOf(12, 10, 0, 2, 4), intArrayOf(83, 12, 0, 1, 9), intArrayOf(19, 6, 0, 0, 7), intArrayOf(11, 10, 0, 3, 4), intArrayOf(84, 12, 0, 6, 3), intArrayOf(18, 6, 0, 0, 8), intArrayOf(10, 10, 0, 4, 3), intArrayOf(17, 6, 0, 3, 1), intArrayOf(9, 10, 0, 8, 2), intArrayOf(16, 6, 0, 4, 1), intArrayOf(8, 10, 0, 5, 3), intArrayOf(22, 7, 0, 1, 3), intArrayOf(85, 12, 0, 1, 10), intArrayOf(21, 7, 0, 2, 2), intArrayOf(20, 7, 0, 7, 1), intArrayOf(28, 8, 0, 1, 4), intArrayOf(27, 8, 0, 3, 2), intArrayOf(33, 9, 0, 0, 16), intArrayOf(32, 9, 0, 1, 5), intArrayOf(31, 9, 0, 1, 6), intArrayOf(30, 9, 0, 2, 3), intArrayOf(29, 9, 0, 3, 3), intArrayOf(28, 9, 0, 5, 2), intArrayOf(27, 9, 0, 6, 2), intArrayOf(26, 9, 0, 7, 2), intArrayOf(34, 11, 0, 1, 8), intArrayOf(35, 11, 0, 9, 2), intArrayOf(86, 12, 0, 2, 5), intArrayOf(87, 12, 0, 7, 3), intArrayOf(7, 4, 1, 0, 1), intArrayOf(25, 9, 0, 11, 1), intArrayOf(5, 11, 1, 0, 6), intArrayOf(15, 6, 1, 1, 1), intArrayOf(4, 11, 1, 0, 7), intArrayOf(14, 6, 1, 2, 1), intArrayOf(13, 6, 0, 5, 1), intArrayOf(12, 6, 1, 0, 2), intArrayOf(19, 7, 1, 5, 1), intArrayOf(18, 7, 0, 6, 1), intArrayOf(17, 7, 1, 3, 1), intArrayOf(16, 7, 1, 4, 1), intArrayOf(26, 8, 1, 9, 1), intArrayOf(25, 8, 0, 8, 1), intArrayOf(24, 8, 0, 9, 1), intArrayOf(23, 8, 0, 10, 1), intArrayOf(22, 8, 1, 0, 3), intArrayOf(21, 8, 1, 6, 1), intArrayOf(20, 8, 1, 7, 1), intArrayOf(19, 8, 1, 8, 1), intArrayOf(24, 9, 0, 12, 1), intArrayOf(23, 9, 1, 0, 4), intArrayOf(22, 9, 1, 1, 2), intArrayOf(21, 9, 1, 10, 1), intArrayOf(20, 9, 1, 11, 1), intArrayOf(19, 9, 1, 12, 1), intArrayOf(18, 9, 1, 13, 1), intArrayOf(17, 9, 1, 14, 1), intArrayOf(7, 10, 0, 13, 1), intArrayOf(6, 10, 1, 0, 5), intArrayOf(5, 10, 1, 1, 3), intArrayOf(4, 10, 1, 2, 2), intArrayOf(36, 11, 1, 3, 2), intArrayOf(37, 11, 1, 4, 2), intArrayOf(38, 11, 1, 15, 1), intArrayOf(39, 11, 1, 16, 1), intArrayOf(88, 12, 0, 14, 1), intArrayOf(89, 12, 1, 0, 8), intArrayOf(90, 12, 1, 5, 2), intArrayOf(91, 12, 1, 6, 2), intArrayOf(92, 12, 1, 17, 1), intArrayOf(93, 12, 1, 18, 1), intArrayOf(94, 12, 1, 19, 1), intArrayOf(95, 12, 1, 20, 1)))
    @JvmField
    val SCAN_TABLES = arrayOf(shortArrayOf(0, 1, 8, 16, 9, 2, 3, 10,
            17, 24, 32, 25, 18, 11, 4, 5,
            12, 19, 26, 33, 40, 48, 41, 34,
            27, 20, 13, 6, 7, 14, 21, 28,
            35, 42, 49, 56, 57, 50, 43, 36,
            29, 22, 15, 23, 30, 37, 44, 51,
            58, 59, 52, 45, 38, 31, 39, 46,
            53, 60, 61, 54, 47, 55, 62, 63), shortArrayOf(0, 1, 2, 3, 8, 9, 16, 17,
            10, 11, 4, 5, 6, 7, 15, 14,
            13, 12, 19, 18, 24, 25, 32, 33,
            26, 27, 20, 21, 22, 23, 28, 29,
            30, 31, 34, 35, 40, 41, 48, 49,
            42, 43, 36, 37, 38, 39, 44, 45,
            46, 47, 50, 51, 56, 57, 58, 59,
            52, 53, 54, 55, 60, 61, 62, 63), shortArrayOf(0, 8, 16, 24, 1, 9, 2, 10,
            17, 25, 32, 40, 48, 56, 57, 49,
            41, 33, 26, 18, 3, 11, 4, 12,
            19, 27, 34, 42, 50, 58, 35, 43,
            51, 59, 20, 28, 5, 13, 6, 14,
            21, 29, 36, 44, 52, 60, 37, 45,
            53, 61, 22, 30, 7, 15, 23, 31,
            38, 46, 54, 62, 39, 47, 55, 63))
    @JvmField
    val DEFAULT_ACDC_VALUES = shortArrayOf(1024, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    @JvmField
    val DC_LUM_TAB = arrayOf(intArrayOf(0, 0), intArrayOf(4, 3), intArrayOf(3, 3), intArrayOf(0, 3), intArrayOf(2, 2), intArrayOf(2, 2), intArrayOf(1, 2), intArrayOf(1, 2))
    @JvmField
    val FILTER_TAB = arrayOf(intArrayOf(14, 23, -7, 3, -1), intArrayOf(-3, 19, 20, -6, 3, -1), intArrayOf(2, -6, 20, 20, -6, 3, -1), intArrayOf(-1, 3, -6, 20, 20, -6, 3, -1))
    @JvmField
    val SPRITE_TRAJECTORY_LEN = arrayOf(intArrayOf(0x00, 2), intArrayOf(0x02, 3), intArrayOf(0x03, 3), intArrayOf(0x04, 3), intArrayOf(0x05, 3), intArrayOf(0x06, 3), intArrayOf(0x0E, 4), intArrayOf(0x1E, 5), intArrayOf(0x3E, 6), intArrayOf(0x7E, 7), intArrayOf(0xFE, 8), intArrayOf(0x1FE, 9), intArrayOf(0x3FE, 10), intArrayOf(0x7FE, 11), intArrayOf(0xFFE, 12)) //@formatter:on
}