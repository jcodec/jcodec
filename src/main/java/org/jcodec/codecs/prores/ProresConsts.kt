package org.jcodec.codecs.prores

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 *
 * @author The JCodec project
 */
object ProresConsts {
    @JvmField
    val firstDCCodebook = Codebook(5, 6, 0)

    @JvmField
    val dcCodebooks = arrayOf(
            Codebook(0, 1, 0),
            Codebook(1, 2, 0),
            Codebook(1, 2, 0),
            Codebook(2, 3, 1),
            Codebook(2, 3, 1),
            Codebook(3, 4, 0),
            Codebook(3, 4, 0)
    )

    @JvmField
    val runCodebooks = arrayOf(
            Codebook(0, 1, 2),
            Codebook(0, 1, 2),
            Codebook(0, 1, 1),
            Codebook(0, 1, 1),
            Codebook(0, 1, 0),
            Codebook(1, 2, 1),
            Codebook(1, 2, 1),
            Codebook(1, 2, 1),
            Codebook(1, 2, 1),
            Codebook(1, 2, 0),
            Codebook(1, 2, 0),
            Codebook(1, 2, 0),
            Codebook(1, 2, 0),
            Codebook(1, 2, 0),
            Codebook(1, 2, 0),
            Codebook(2, 3, 0)
    )

    @JvmField
    val levCodebooks = arrayOf(
            Codebook(0, 1, 0),
            Codebook(0, 2, 2),
            Codebook(0, 1, 1),
            Codebook(0, 1, 2),
            Codebook(0, 1, 0),
            Codebook(1, 2, 0),
            Codebook(1, 2, 0),
            Codebook(1, 2, 0),
            Codebook(1, 2, 0),
            Codebook(2, 3, 0)
    )

    @JvmField
    val progressive_scan = intArrayOf(
            0, 1, 8, 9, 2, 3, 10, 11,
            16, 17, 24, 25, 18, 19, 26, 27,
            4, 5, 12, 20, 13, 6, 7, 14,
            21, 28, 29, 22, 15, 23, 30, 31,
            32, 33, 40, 48, 41, 34, 35, 42,
            49, 56, 57, 50, 43, 36, 37, 44,
            51, 58, 59, 52, 45, 38, 39, 46,
            53, 60, 61, 54, 47, 55, 62, 63
    )

    @JvmField
    val interlaced_scan = intArrayOf(
            0, 8, 1, 9, 16, 24, 17, 25,
            2, 10, 3, 11, 18, 26, 19, 27,
            32, 40, 33, 34, 41, 48, 56, 49,
            42, 35, 43, 50, 57, 58, 51, 59,
            4, 12, 5, 6, 13, 20, 28, 21,
            14, 7, 15, 22, 29, 36, 44, 37,
            30, 23, 31, 38, 45, 52, 60, 53,
            46, 39, 47, 54, 61, 62, 55, 63)

    @JvmField
    val QMAT_LUMA_APCH = intArrayOf(4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
            4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 4, 4, 4, 4, 4, 4, 5, 5, 4, 4, 4, 4, 4, 5, 5, 6, 4, 4, 4, 4, 5, 5, 6, 7,
            4, 4, 4, 4, 5, 6, 7, 7)

    @JvmField
    val QMAT_CHROMA_APCH = intArrayOf(4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
            4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 4, 4, 4, 4, 4, 4, 5, 5, 4, 4, 4, 4, 4, 5, 5, 6, 4, 4, 4, 4, 5, 5, 6,
            7, 4, 4, 4, 4, 5, 6, 7, 7)

    @JvmField
    val QMAT_LUMA_APCO = intArrayOf(4, 7, 9, 11, 13, 14, 15, 63, 7, 7, 11, 12, 14, 15, 63, 63,
            9, 11, 13, 14, 15, 63, 63, 63, 11, 11, 13, 14, 63, 63, 63, 63, 11, 13, 14, 63, 63, 63, 63, 63, 13, 14, 63,
            63, 63, 63, 63, 63, 13, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63)

    @JvmField
    val QMAT_CHROMA_APCO = intArrayOf(4, 7, 9, 11, 13, 14, 63, 63, 7, 7, 11, 12, 14, 63, 63, 63,
            9, 11, 13, 14, 63, 63, 63, 63, 11, 11, 13, 14, 63, 63, 63, 63, 11, 13, 14, 63, 63, 63, 63, 63, 13, 14, 63,
            63, 63, 63, 63, 63, 13, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63)

    @JvmField
    val QMAT_LUMA_APCN = intArrayOf(4, 4, 5, 5, 6, 7, 7, 9, 4, 4, 5, 6, 7, 7, 9, 9, 5, 5, 6, 7,
            7, 9, 9, 10, 5, 5, 6, 7, 7, 9, 9, 10, 5, 6, 7, 7, 8, 9, 10, 12, 6, 7, 7, 8, 9, 10, 12, 15, 6, 7, 7, 9, 10,
            11, 14, 17, 7, 7, 9, 10, 11, 14, 17, 21)

    @JvmField
    val QMAT_CHROMA_APCN = intArrayOf(4, 4, 5, 5, 6, 7, 7, 9, 4, 4, 5, 6, 7, 7, 9, 9, 5, 5, 6,
            7, 7, 9, 9, 10, 5, 5, 6, 7, 7, 9, 9, 10, 5, 6, 7, 7, 8, 9, 10, 12, 6, 7, 7, 8, 9, 10, 12, 15, 6, 7, 7, 9,
            10, 11, 14, 17, 7, 7, 9, 10, 11, 14, 17, 21)

    @JvmField
    val QMAT_LUMA_APCS = intArrayOf(4, 5, 6, 7, 9, 11, 13, 15, 5, 5, 7, 8, 11, 13, 15, 17, 6, 7,
            9, 11, 13, 15, 15, 17, 7, 7, 9, 11, 13, 15, 17, 19, 7, 9, 11, 13, 14, 16, 19, 23, 9, 11, 13, 14, 16, 19,
            23, 29, 9, 11, 13, 15, 17, 21, 28, 35, 11, 13, 16, 17, 21, 28, 35, 41)

    @JvmField
    val QMAT_CHROMA_APCS = intArrayOf(4, 5, 6, 7, 9, 11, 13, 15, 5, 5, 7, 8, 11, 13, 15, 17, 6,
            7, 9, 11, 13, 15, 15, 17, 7, 7, 9, 11, 13, 15, 17, 19, 7, 9, 11, 13, 14, 16, 19, 23, 9, 11, 13, 14, 16, 19,
            23, 29, 9, 11, 13, 15, 17, 21, 28, 35, 11, 13, 16, 17, 21, 28, 35, 41)

    class FrameHeader(@JvmField var payloadSize: Int, @JvmField var width: Int, @JvmField var height: Int,
                      @JvmField var frameType: Int,
                      @JvmField var topFieldFirst: Boolean, @JvmField var scan: IntArray, @JvmField var qMatLuma: IntArray,
                      @JvmField var qMatChroma: IntArray, @JvmField var chromaType: Int)

    class PictureHeader(@JvmField var log2SliceMbWidth: Int, @JvmField var sliceSizes: ShortArray)  //    public static void main1(String[] args) {
    //        int[] qmat = new int[] { 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
    //                4, 4, 5, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 7, 7, 7 };
    //        int[] cool = new int[64];
    //        for (int i = 0; i < 64; i++)
    //            cool[progressive_scan[i]] = qmat[i];
    //        for (int i : cool) {
    //            System.out.print(i + ",");
    //        }
    //    }
}