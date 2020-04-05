package org.jcodec.codecs.h264.io.model

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class MBType private constructor(@JvmField var isIntra: Boolean, var _code: Int) {

    fun code(): Int {
        return _code
    }

    companion object {
        @JvmField
        val I_NxN = MBType(true, 0)
        @JvmField
        val I_16x16 = MBType(true, 1)
        @JvmField
        val I_PCM = MBType(true, 25)
        @JvmField
        val P_16x16 = MBType(false, 0)
        @JvmField
        val P_16x8 = MBType(false, 1)
        @JvmField
        val P_8x16 = MBType(false, 2)
        @JvmField
        val P_8x8 = MBType(false, 3)
        @JvmField
        val P_8x8ref0 = MBType(false, 4)
        @JvmField
        val B_Direct_16x16 = MBType(false, 0)
        @JvmField
        val B_L0_16x16 = MBType(false, 1)
        @JvmField
        val B_L1_16x16 = MBType(false, 2)
        @JvmField
        val B_Bi_16x16 = MBType(false, 3)
        @JvmField
        val B_L0_L0_16x8 = MBType(false, 4)
        @JvmField
        val B_L0_L0_8x16 = MBType(false, 5)
        @JvmField
        val B_L1_L1_16x8 = MBType(false, 6)
        @JvmField
        val B_L1_L1_8x16 = MBType(false, 7)
        @JvmField
        val B_L0_L1_16x8 = MBType(false, 8)
        @JvmField
        val B_L0_L1_8x16 = MBType(false, 9)
        @JvmField
        val B_L1_L0_16x8 = MBType(false, 10)
        @JvmField
        val B_L1_L0_8x16 = MBType(false, 11)
        @JvmField
        val B_L0_Bi_16x8 = MBType(false, 12)
        @JvmField
        val B_L0_Bi_8x16 = MBType(false, 13)
        @JvmField
        val B_L1_Bi_16x8 = MBType(false, 14)
        @JvmField
        val B_L1_Bi_8x16 = MBType(false, 15)
        @JvmField
        val B_Bi_L0_16x8 = MBType(false, 16)
        @JvmField
        val B_Bi_L0_8x16 = MBType(false, 17)
        @JvmField
        val B_Bi_L1_16x8 = MBType(false, 18)
        @JvmField
        val B_Bi_L1_8x16 = MBType(false, 19)
        @JvmField
        val B_Bi_Bi_16x8 = MBType(false, 20)
        @JvmField
        val B_Bi_Bi_8x16 = MBType(false, 21)
        @JvmField
        val B_8x8 = MBType(false, 22)
    }

}