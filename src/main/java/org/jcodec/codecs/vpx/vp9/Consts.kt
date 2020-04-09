package org.jcodec.codecs.vpx.vp9

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
object Consts {
    const val KEY_FRAME = 0
    const val INTER_FRAME = 1

    /**
     * Specifies how the transform size is determined. For tx_mode not equal to
     * 4, the inverse transform will use the largest transform size possible up
     * to the limit set in tx_mode. For tx_mode equal to 4, the choice of size
     * is specified explicitly for each block.
     */
    const val ONLY_4X4 = 0
    const val ALLOW_8X8 = 1
    const val ALLOW_16X16 = 2
    const val ALLOW_32X32 = 3
    const val TX_MODE_SELECT = 4
    const val PARTITION_NONE = 0
    const val PARTITION_HORZ = 1
    const val PARTITION_VERT = 2
    const val PARTITION_SPLIT = 3
    const val BLOCK_INVALID = -1
    const val BLOCK_4X4 = 0
    const val BLOCK_4X8 = 1
    const val BLOCK_8X4 = 2
    const val BLOCK_8X8 = 3
    const val BLOCK_8X16 = 4
    const val BLOCK_16X8 = 5
    const val BLOCK_16X16 = 6
    const val BLOCK_16X32 = 7
    const val BLOCK_32X16 = 8
    const val BLOCK_32X32 = 9
    const val BLOCK_32X64 = 10
    const val BLOCK_64X32 = 11
    const val BLOCK_64X64 = 12
    const val TX_4X4 = 0
    const val TX_8X8 = 1
    const val TX_16X16 = 2
    const val TX_32X32 = 3
    const val INTRA_FRAME = 0
    const val LAST_FRAME = 1
    const val ALTREF_FRAME = 2
    const val GOLDEN_FRAME = 3
    const val DC_PRED = 0
    const val V_PRED = 1
    const val H_PRED = 2
    const val D45_PRED = 3
    const val D135_PRED = 4
    const val D117_PRED = 5
    const val D153_PRED = 6
    const val D207_PRED = 7
    const val D63_PRED = 8
    const val TM_PRED = 9
    const val NEARESTMV = 10
    const val NEARMV = 11
    const val ZEROMV = 12
    const val NEWMV = 13
    const val SINGLE_REF = 0
    const val COMPOUND_REF = 1
    const val REFERENCE_MODE_SELECT = 2
    const val NORMAL = 0
    const val SMOOTH = 1
    const val SHARP = 2
    const val SWITCHABLE = 3
    const val MV_JOINT_ZERO = 0
    const val MV_JOINT_HNZVZ = 1
    const val MV_JOINT_HZVNZ = 2
    const val MV_JOINT_HNZVNZ = 3
    const val ZERO_TOKEN = 0
    const val ONE_TOKEN = 1
    const val TWO_TOKEN = 2
    const val THREE_TOKEN = 3
    const val FOUR_TOKEN = 4
    const val DCT_VAL_CAT1 = 5
    const val DCT_VAL_CAT2 = 6
    const val DCT_VAL_CAT3 = 7
    const val DCT_VAL_CAT4 = 8
    const val DCT_VAL_CAT5 = 9
    const val DCT_VAL_CAT6 = 10
    @JvmField
    val TREE_SEGMENT_ID = intArrayOf(2, 4, 6, 8, 10, 12, 0, -1, -2, -3, -4, -5, -6, -7)
    @JvmField
    val TREE_TX_SIZE = arrayOf(null, intArrayOf(-TX_4X4, -TX_8X8), intArrayOf(-TX_4X4, 2, -TX_8X8, -TX_16X16), intArrayOf(-TX_4X4, 2, -TX_8X8, 4, -TX_16X16, -TX_32X32))
    @JvmField
    val maxTxLookup = intArrayOf(0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 3)
    @JvmField
    val blW = intArrayOf(1, 1, 1, 1, 1, 2, 2, 2, 4, 4, 4, 8, 8)
    @JvmField
    val blH = intArrayOf(1, 1, 1, 1, 2, 1, 2, 4, 2, 4, 8, 4, 8)
    @JvmField
    val TREE_INTRA_MODE = intArrayOf(-DC_PRED, 2, -TM_PRED, 4, -V_PRED, 6, 8, 12, -H_PRED, 10,
            -D135_PRED, -D117_PRED, -D45_PRED, 14, -D63_PRED, 16, -D153_PRED, -D207_PRED)
    @JvmField
    val size_group_lookup = intArrayOf(0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 3)
    @JvmField
    val TREE_INTERP_FILTER = intArrayOf(-NORMAL, 2, -SMOOTH, -SHARP)
    @JvmField
    val TREE_INTER_MODE = intArrayOf(-(ZEROMV - NEARESTMV), 2, -(NEARESTMV - NEARESTMV), 4,
            -(NEARMV - NEARESTMV), -(NEWMV - NEARESTMV))

    // 0 -> (-1,0); 1 -> (0,-1); 2 -> (-1,1); 3 -> (1,-1), 4 -> (-1, 3), 5 ->
    // (3, -1)
    @JvmField
    val mv_ref_blocks_sm = arrayOf(intArrayOf(0, 1), intArrayOf(0, 1), intArrayOf(0, 1), intArrayOf(0, 1), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(0, 1), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(2, 3), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(4, 5))

    // 6 -> (-1, 2), 7 -> (2, -1), 8 -> (-1, 4), 9 -> (4, -1), 10 -> (-1, 6), 11
    // -> (-1, -1), 12 -> (-2, 0)
    // 13 -> (0, -2), 14 -> (-3, 0), 15 -> (0, -3), 16 -> (-2, -1), 17 -> (-1,
    // -2), 18 -> (-2, -2), 19 -> (-3, -3)
    @JvmField
    val mv_ref_blocks = arrayOf(intArrayOf(0, 1, 11, 12, 13, 16, 17, 18), intArrayOf(0, 1, 11, 12, 13, 16, 17, 18), intArrayOf(0, 1, 11, 12, 13, 16, 17, 18), intArrayOf(0, 1, 11, 12, 13, 16, 17, 18), intArrayOf(1, 0, 3, 11, 13, 12, 16, 17), intArrayOf(0, 1, 2, 11, 12, 13, 17, 16), intArrayOf(0, 1, 2, 3, 11, 14, 15, 19), intArrayOf(1, 0, 7, 11, 2, 15, 14, 19), intArrayOf(0, 1, 6, 11, 3, 14, 15, 19), intArrayOf(2, 3, 6, 7, 11, 14, 15, 19), intArrayOf(1, 0, 9, 6, 11, 15, 14, 7), intArrayOf(0, 1, 8, 7, 11, 14, 15, 6), intArrayOf(4, 5, 8, 9, 11, 0, 1, 10))
    @JvmField
    val TREE_MV_JOINT = intArrayOf(-MV_JOINT_ZERO, 2, -MV_JOINT_HNZVZ, 4, -MV_JOINT_HZVNZ,
            -MV_JOINT_HNZVNZ)
    @JvmField
    val MV_CLASS_TREE = intArrayOf(-0, 2, -1, 4, 6, 8, -2, -3, 10, 12, -4, -5, -6, 14, 16, 18,
            -7, -8, -9, -10)
    @JvmField
    val MV_FR_TREE = intArrayOf(-0, 2, -1, 4, -2, -3)
    @JvmField
    val LITERAL_TO_FILTER_TYPE = intArrayOf(SMOOTH, NORMAL, SHARP, SWITCHABLE)
    @JvmField
    val PARETO_TABLE = arrayOf(intArrayOf(3, 86, 128, 6, 86, 23, 88, 29), intArrayOf(6, 86, 128, 11, 87, 42, 91, 52), intArrayOf(9, 86, 129, 17, 88, 61, 94, 76), intArrayOf(12, 86, 129, 22, 88, 77, 97, 93), intArrayOf(15, 87, 129, 28, 89, 93, 100, 110), intArrayOf(17, 87, 129, 33, 90, 105, 103, 123), intArrayOf(20, 88, 130, 38, 91, 118, 106, 136), intArrayOf(23, 88, 130, 43, 91, 128, 108, 146), intArrayOf(26, 89, 131, 48, 92, 139, 111, 156), intArrayOf(28, 89, 131, 53, 93, 147, 114, 163), intArrayOf(31, 90, 131, 58, 94, 156, 117, 171), intArrayOf(34, 90, 131, 62, 94, 163, 119, 177), intArrayOf(37, 90, 132, 66, 95, 171, 122, 184), intArrayOf(39, 90, 132, 70, 96, 177, 124, 189), intArrayOf(42, 91, 132, 75, 97, 183, 127, 194), intArrayOf(44, 91, 132, 79, 97, 188, 129, 198), intArrayOf(47, 92, 133, 83, 98, 193, 132, 202), intArrayOf(49, 92, 133, 86, 99, 197, 134, 205), intArrayOf(52, 93, 133, 90, 100, 201, 137, 208), intArrayOf(54, 93, 133, 94, 100, 204, 139, 211), intArrayOf(57, 94, 134, 98, 101, 208, 142, 214), intArrayOf(59, 94, 134, 101, 102, 211, 144, 216), intArrayOf(62, 94, 135, 105, 103, 214, 146, 218), intArrayOf(64, 94, 135, 108, 103, 216, 148, 220), intArrayOf(66, 95, 135, 111, 104, 219, 151, 222), intArrayOf(68, 95, 135, 114, 105, 221, 153, 223), intArrayOf(71, 96, 136, 117, 106, 224, 155, 225), intArrayOf(73, 96, 136, 120, 106, 225, 157, 226), intArrayOf(76, 97, 136, 123, 107, 227, 159, 228), intArrayOf(78, 97, 136, 126, 108, 229, 160, 229), intArrayOf(80, 98, 137, 129, 109, 231, 162, 231), intArrayOf(82, 98, 137, 131, 109, 232, 164, 232), intArrayOf(84, 98, 138, 134, 110, 234, 166, 233), intArrayOf(86, 98, 138, 137, 111, 235, 168, 234), intArrayOf(89, 99, 138, 140, 112, 236, 170, 235), intArrayOf(91, 99, 138, 142, 112, 237, 171, 235), intArrayOf(93, 100, 139, 145, 113, 238, 173, 236), intArrayOf(95, 100, 139, 147, 114, 239, 174, 237), intArrayOf(97, 101, 140, 149, 115, 240, 176, 238), intArrayOf(99, 101, 140, 151, 115, 241, 177, 238), intArrayOf(101, 102, 140, 154, 116, 242, 179, 239), intArrayOf(103, 102, 140, 156, 117, 242, 180, 239), intArrayOf(105, 103, 141, 158, 118, 243, 182, 240), intArrayOf(107, 103, 141, 160, 118, 243, 183, 240), intArrayOf(109, 104, 141, 162, 119, 244, 185, 241), intArrayOf(111, 104, 141, 164, 119, 244, 186, 241), intArrayOf(113, 104, 142, 166, 120, 245, 187, 242), intArrayOf(114, 104, 142, 168, 121, 245, 188, 242), intArrayOf(116, 105, 143, 170, 122, 246, 190, 243), intArrayOf(118, 105, 143, 171, 122, 246, 191, 243), intArrayOf(120, 106, 143, 173, 123, 247, 192, 244), intArrayOf(121, 106, 143, 175, 124, 247, 193, 244), intArrayOf(123, 107, 144, 177, 125, 248, 195, 244), intArrayOf(125, 107, 144, 178, 125, 248, 196, 244), intArrayOf(127, 108, 145, 180, 126, 249, 197, 245), intArrayOf(128, 108, 145, 181, 127, 249, 198, 245), intArrayOf(130, 109, 145, 183, 128, 249, 199, 245), intArrayOf(132, 109, 145, 184, 128, 249, 200, 245), intArrayOf(134, 110, 146, 186, 129, 250, 201, 246), intArrayOf(135, 110, 146, 187, 130, 250, 202, 246), intArrayOf(137, 111, 147, 189, 131, 251, 203, 246), intArrayOf(138, 111, 147, 190, 131, 251, 204, 246), intArrayOf(140, 112, 147, 192, 132, 251, 205, 247), intArrayOf(141, 112, 147, 193, 132, 251, 206, 247), intArrayOf(143, 113, 148, 194, 133, 251, 207, 247), intArrayOf(144, 113, 148, 195, 134, 251, 207, 247), intArrayOf(146, 114, 149, 197, 135, 252, 208, 248), intArrayOf(147, 114, 149, 198, 135, 252, 209, 248), intArrayOf(149, 115, 149, 199, 136, 252, 210, 248), intArrayOf(150, 115, 149, 200, 137, 252, 210, 248), intArrayOf(152, 115, 150, 201, 138, 252, 211, 248), intArrayOf(153, 115, 150, 202, 138, 252, 212, 248), intArrayOf(155, 116, 151, 204, 139, 253, 213, 249), intArrayOf(156, 116, 151, 205, 139, 253, 213, 249), intArrayOf(158, 117, 151, 206, 140, 253, 214, 249), intArrayOf(159, 117, 151, 207, 141, 253, 215, 249), intArrayOf(161, 118, 152, 208, 142, 253, 216, 249), intArrayOf(162, 118, 152, 209, 142, 253, 216, 249), intArrayOf(163, 119, 153, 210, 143, 253, 217, 249), intArrayOf(164, 119, 153, 211, 143, 253, 217, 249), intArrayOf(166, 120, 153, 212, 144, 254, 218, 250), intArrayOf(167, 120, 153, 212, 145, 254, 219, 250), intArrayOf(168, 121, 154, 213, 146, 254, 220, 250), intArrayOf(169, 121, 154, 214, 146, 254, 220, 250), intArrayOf(171, 122, 155, 215, 147, 254, 221, 250), intArrayOf(172, 122, 155, 216, 147, 254, 221, 250), intArrayOf(173, 123, 155, 217, 148, 254, 222, 250), intArrayOf(174, 123, 155, 217, 149, 254, 222, 250), intArrayOf(176, 124, 156, 218, 150, 254, 223, 250), intArrayOf(177, 124, 156, 219, 150, 254, 223, 250), intArrayOf(178, 125, 157, 220, 151, 254, 224, 251), intArrayOf(179, 125, 157, 220, 151, 254, 224, 251), intArrayOf(180, 126, 157, 221, 152, 254, 225, 251), intArrayOf(181, 126, 157, 221, 152, 254, 225, 251), intArrayOf(183, 127, 158, 222, 153, 254, 226, 251), intArrayOf(184, 127, 158, 223, 154, 254, 226, 251), intArrayOf(185, 128, 159, 224, 155, 255, 227, 251), intArrayOf(186, 128, 159, 224, 155, 255, 227, 251), intArrayOf(187, 129, 160, 225, 156, 255, 228, 251), intArrayOf(188, 130, 160, 225, 156, 255, 228, 251), intArrayOf(189, 131, 160, 226, 157, 255, 228, 251), intArrayOf(190, 131, 160, 226, 158, 255, 228, 251), intArrayOf(191, 132, 161, 227, 159, 255, 229, 251), intArrayOf(192, 132, 161, 227, 159, 255, 229, 251), intArrayOf(193, 133, 162, 228, 160, 255, 230, 252), intArrayOf(194, 133, 162, 229, 160, 255, 230, 252), intArrayOf(195, 134, 163, 230, 161, 255, 231, 252), intArrayOf(196, 134, 163, 230, 161, 255, 231, 252), intArrayOf(197, 135, 163, 231, 162, 255, 231, 252), intArrayOf(198, 135, 163, 231, 162, 255, 231, 252), intArrayOf(199, 136, 164, 232, 163, 255, 232, 252), intArrayOf(200, 136, 164, 232, 164, 255, 232, 252), intArrayOf(201, 137, 165, 233, 165, 255, 233, 252), intArrayOf(201, 137, 165, 233, 165, 255, 233, 252), intArrayOf(202, 138, 166, 233, 166, 255, 233, 252), intArrayOf(203, 138, 166, 233, 166, 255, 233, 252), intArrayOf(204, 139, 166, 234, 167, 255, 234, 252), intArrayOf(205, 139, 166, 234, 167, 255, 234, 252), intArrayOf(206, 140, 167, 235, 168, 255, 235, 252), intArrayOf(206, 140, 167, 235, 168, 255, 235, 252), intArrayOf(207, 141, 168, 236, 169, 255, 235, 252), intArrayOf(208, 141, 168, 236, 170, 255, 235, 252), intArrayOf(209, 142, 169, 237, 171, 255, 236, 252), intArrayOf(209, 143, 169, 237, 171, 255, 236, 252), intArrayOf(210, 144, 169, 237, 172, 255, 236, 252), intArrayOf(211, 144, 169, 237, 172, 255, 236, 252), intArrayOf(212, 145, 170, 238, 173, 255, 237, 252), intArrayOf(213, 145, 170, 238, 173, 255, 237, 252), intArrayOf(214, 146, 171, 239, 174, 255, 237, 253), intArrayOf(214, 146, 171, 239, 174, 255, 237, 253), intArrayOf(215, 147, 172, 240, 175, 255, 238, 253), intArrayOf(215, 147, 172, 240, 175, 255, 238, 253), intArrayOf(216, 148, 173, 240, 176, 255, 238, 253), intArrayOf(217, 148, 173, 240, 176, 255, 238, 253), intArrayOf(218, 149, 173, 241, 177, 255, 239, 253), intArrayOf(218, 149, 173, 241, 178, 255, 239, 253), intArrayOf(219, 150, 174, 241, 179, 255, 239, 253), intArrayOf(219, 151, 174, 241, 179, 255, 239, 253), intArrayOf(220, 152, 175, 242, 180, 255, 240, 253), intArrayOf(221, 152, 175, 242, 180, 255, 240, 253), intArrayOf(222, 153, 176, 242, 181, 255, 240, 253), intArrayOf(222, 153, 176, 242, 181, 255, 240, 253), intArrayOf(223, 154, 177, 243, 182, 255, 240, 253), intArrayOf(223, 154, 177, 243, 182, 255, 240, 253), intArrayOf(224, 155, 178, 244, 183, 255, 241, 253), intArrayOf(224, 155, 178, 244, 183, 255, 241, 253), intArrayOf(225, 156, 178, 244, 184, 255, 241, 253), intArrayOf(225, 157, 178, 244, 184, 255, 241, 253), intArrayOf(226, 158, 179, 244, 185, 255, 242, 253), intArrayOf(227, 158, 179, 244, 185, 255, 242, 253), intArrayOf(228, 159, 180, 245, 186, 255, 242, 253), intArrayOf(228, 159, 180, 245, 186, 255, 242, 253), intArrayOf(229, 160, 181, 245, 187, 255, 242, 253), intArrayOf(229, 160, 181, 245, 187, 255, 242, 253), intArrayOf(230, 161, 182, 246, 188, 255, 243, 253), intArrayOf(230, 162, 182, 246, 188, 255, 243, 253), intArrayOf(231, 163, 183, 246, 189, 255, 243, 253), intArrayOf(231, 163, 183, 246, 189, 255, 243, 253), intArrayOf(232, 164, 184, 247, 190, 255, 243, 253), intArrayOf(232, 164, 184, 247, 190, 255, 243, 253), intArrayOf(233, 165, 185, 247, 191, 255, 244, 253), intArrayOf(233, 165, 185, 247, 191, 255, 244, 253), intArrayOf(234, 166, 185, 247, 192, 255, 244, 253), intArrayOf(234, 167, 185, 247, 192, 255, 244, 253), intArrayOf(235, 168, 186, 248, 193, 255, 244, 253), intArrayOf(235, 168, 186, 248, 193, 255, 244, 253), intArrayOf(236, 169, 187, 248, 194, 255, 244, 253), intArrayOf(236, 169, 187, 248, 194, 255, 244, 253), intArrayOf(236, 170, 188, 248, 195, 255, 245, 253), intArrayOf(236, 170, 188, 248, 195, 255, 245, 253), intArrayOf(237, 171, 189, 249, 196, 255, 245, 254), intArrayOf(237, 172, 189, 249, 196, 255, 245, 254), intArrayOf(238, 173, 190, 249, 197, 255, 245, 254), intArrayOf(238, 173, 190, 249, 197, 255, 245, 254), intArrayOf(239, 174, 191, 249, 198, 255, 245, 254), intArrayOf(239, 174, 191, 249, 198, 255, 245, 254), intArrayOf(240, 175, 192, 249, 199, 255, 246, 254), intArrayOf(240, 176, 192, 249, 199, 255, 246, 254), intArrayOf(240, 177, 193, 250, 200, 255, 246, 254), intArrayOf(240, 177, 193, 250, 200, 255, 246, 254), intArrayOf(241, 178, 194, 250, 201, 255, 246, 254), intArrayOf(241, 178, 194, 250, 201, 255, 246, 254), intArrayOf(242, 179, 195, 250, 202, 255, 246, 254), intArrayOf(242, 180, 195, 250, 202, 255, 246, 254), intArrayOf(242, 181, 196, 250, 203, 255, 247, 254), intArrayOf(242, 181, 196, 250, 203, 255, 247, 254), intArrayOf(243, 182, 197, 251, 204, 255, 247, 254), intArrayOf(243, 183, 197, 251, 204, 255, 247, 254), intArrayOf(244, 184, 198, 251, 205, 255, 247, 254), intArrayOf(244, 184, 198, 251, 205, 255, 247, 254), intArrayOf(244, 185, 199, 251, 206, 255, 247, 254), intArrayOf(244, 185, 199, 251, 206, 255, 247, 254), intArrayOf(245, 186, 200, 251, 207, 255, 247, 254), intArrayOf(245, 187, 200, 251, 207, 255, 247, 254), intArrayOf(246, 188, 201, 252, 207, 255, 248, 254), intArrayOf(246, 188, 201, 252, 207, 255, 248, 254), intArrayOf(246, 189, 202, 252, 208, 255, 248, 254), intArrayOf(246, 190, 202, 252, 208, 255, 248, 254), intArrayOf(247, 191, 203, 252, 209, 255, 248, 254), intArrayOf(247, 191, 203, 252, 209, 255, 248, 254), intArrayOf(247, 192, 204, 252, 210, 255, 248, 254), intArrayOf(247, 193, 204, 252, 210, 255, 248, 254), intArrayOf(248, 194, 205, 252, 211, 255, 248, 254), intArrayOf(248, 194, 205, 252, 211, 255, 248, 254), intArrayOf(248, 195, 206, 252, 212, 255, 249, 254), intArrayOf(248, 196, 206, 252, 212, 255, 249, 254), intArrayOf(249, 197, 207, 253, 213, 255, 249, 254), intArrayOf(249, 197, 207, 253, 213, 255, 249, 254), intArrayOf(249, 198, 208, 253, 214, 255, 249, 254), intArrayOf(249, 199, 209, 253, 214, 255, 249, 254), intArrayOf(250, 200, 210, 253, 215, 255, 249, 254), intArrayOf(250, 200, 210, 253, 215, 255, 249, 254), intArrayOf(250, 201, 211, 253, 215, 255, 249, 254), intArrayOf(250, 202, 211, 253, 215, 255, 249, 254), intArrayOf(250, 203, 212, 253, 216, 255, 249, 254), intArrayOf(250, 203, 212, 253, 216, 255, 249, 254), intArrayOf(251, 204, 213, 253, 217, 255, 250, 254), intArrayOf(251, 205, 213, 253, 217, 255, 250, 254), intArrayOf(251, 206, 214, 254, 218, 255, 250, 254), intArrayOf(251, 206, 215, 254, 218, 255, 250, 254), intArrayOf(252, 207, 216, 254, 219, 255, 250, 254), intArrayOf(252, 208, 216, 254, 219, 255, 250, 254), intArrayOf(252, 209, 217, 254, 220, 255, 250, 254), intArrayOf(252, 210, 217, 254, 220, 255, 250, 254), intArrayOf(252, 211, 218, 254, 221, 255, 250, 254), intArrayOf(252, 212, 218, 254, 221, 255, 250, 254), intArrayOf(253, 213, 219, 254, 222, 255, 250, 254), intArrayOf(253, 213, 220, 254, 222, 255, 250, 254), intArrayOf(253, 214, 221, 254, 223, 255, 250, 254), intArrayOf(253, 215, 221, 254, 223, 255, 250, 254), intArrayOf(253, 216, 222, 254, 224, 255, 251, 254), intArrayOf(253, 217, 223, 254, 224, 255, 251, 254), intArrayOf(253, 218, 224, 254, 225, 255, 251, 254), intArrayOf(253, 219, 224, 254, 225, 255, 251, 254), intArrayOf(254, 220, 225, 254, 225, 255, 251, 254), intArrayOf(254, 221, 226, 254, 225, 255, 251, 254), intArrayOf(254, 222, 227, 255, 226, 255, 251, 254), intArrayOf(254, 223, 227, 255, 226, 255, 251, 254), intArrayOf(254, 224, 228, 255, 227, 255, 251, 254), intArrayOf(254, 225, 229, 255, 227, 255, 251, 254), intArrayOf(254, 226, 230, 255, 228, 255, 251, 254), intArrayOf(254, 227, 230, 255, 229, 255, 251, 254), intArrayOf(255, 228, 231, 255, 230, 255, 251, 254), intArrayOf(255, 229, 232, 255, 230, 255, 251, 254), intArrayOf(255, 230, 233, 255, 231, 255, 252, 254), intArrayOf(255, 231, 234, 255, 231, 255, 252, 254), intArrayOf(255, 232, 235, 255, 232, 255, 252, 254), intArrayOf(255, 233, 236, 255, 232, 255, 252, 254), intArrayOf(255, 235, 237, 255, 233, 255, 252, 254), intArrayOf(255, 236, 238, 255, 234, 255, 252, 254), intArrayOf(255, 238, 240, 255, 235, 255, 252, 255), intArrayOf(255, 239, 241, 255, 235, 255, 252, 254), intArrayOf(255, 241, 243, 255, 236, 255, 252, 254), intArrayOf(255, 243, 245, 255, 237, 255, 252, 254), intArrayOf(255, 246, 247, 255, 239, 255, 253, 255))
    @JvmField
    val extra_bits = arrayOf(intArrayOf(0, 0, 0), intArrayOf(0, 0, 1), intArrayOf(0, 0, 2), intArrayOf(0, 0, 3), intArrayOf(0, 0, 4), intArrayOf(1, 1, 5), intArrayOf(2, 2, 7), intArrayOf(3, 3, 11), intArrayOf(4, 4, 19), intArrayOf(5, 5, 35), intArrayOf(6, 14, 67))
    @JvmField
    val cat_probs = arrayOf(intArrayOf(0), intArrayOf(159), intArrayOf(165, 145), intArrayOf(173, 148, 140), intArrayOf(176, 155, 140, 135), intArrayOf(180, 157, 141, 134, 130), intArrayOf(254, 254, 254, 252, 249, 243, 230, 196, 177, 153, 140, 133, 130, 129))
    @JvmField
    val TOKEN_TREE = intArrayOf(2, 6, -TWO_TOKEN, 4,
            -THREE_TOKEN, -FOUR_TOKEN, 8, 10, -DCT_VAL_CAT1, -DCT_VAL_CAT2, 12, 14, -DCT_VAL_CAT3, -DCT_VAL_CAT4,
            -DCT_VAL_CAT5, -DCT_VAL_CAT6)
    @JvmField
    val coefband_4x4 = intArrayOf(0, 1, 1, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 5, 5, 5)
    @JvmField
    val coefband_8x8plus = intArrayOf(0, 1, 1, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4,
            4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5)
    const val SZ_8x8 = 0
    const val SZ_16x16 = 1
    const val SZ_32x32 = 2
    const val SZ_64x64 = 3
    val blSizeLookup_ = arrayOf(intArrayOf(BLOCK_4X4, BLOCK_4X8, BLOCK_8X4, BLOCK_8X8, BLOCK_8X16, BLOCK_16X8, BLOCK_16X16, BLOCK_16X32,
            BLOCK_32X16, BLOCK_32X32, BLOCK_32X64, BLOCK_64X32, BLOCK_64X64), intArrayOf(-1, -1, -1, BLOCK_8X4, -1, -1, BLOCK_16X8, -1, -1, BLOCK_32X16, -1, -1, BLOCK_64X32), intArrayOf(-1, -1, -1, BLOCK_4X8, -1, 1, BLOCK_8X16, -1, -1, BLOCK_16X32, -1, -1, BLOCK_32X64), intArrayOf(-1, -1, -1, BLOCK_4X4, -1, -1, BLOCK_8X8, -1, -1, BLOCK_16X16, -1, -1, BLOCK_32X32))
    val blSizeLookup = arrayOf(intArrayOf(BLOCK_4X4, BLOCK_4X8), intArrayOf(BLOCK_8X4, BLOCK_8X8, BLOCK_8X16), intArrayOf(-1, BLOCK_16X8, BLOCK_16X16, BLOCK_16X32), intArrayOf(-1, -1, BLOCK_32X16, BLOCK_32X32))
    val sub8x8PartitiontoBlockType = intArrayOf(BLOCK_8X8, BLOCK_8X4, BLOCK_4X8, BLOCK_4X4)
    val TREE_PARTITION = intArrayOf(-PARTITION_NONE, 2, -PARTITION_HORZ, 4, -PARTITION_VERT,
            -PARTITION_SPLIT)
    val TREE_PARTITION_RIGHT_E = intArrayOf(-PARTITION_NONE, -PARTITION_VERT)
    val TREE_PARTITION_BOTTOM_E = intArrayOf(-PARTITION_NONE, -PARTITION_HORZ)
    @JvmField
    val INV_REMAP_TABLE = intArrayOf(7, 20, 33, 46, 59, 72, 85, 98, 111, 124, 137, 150, 163, 176, 189,
            202, 215, 228, 241, 254, 1, 2, 3, 4, 5, 6, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 21, 22, 23, 24,
            25, 26, 27, 28, 29, 30, 31, 32, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 47, 48, 49, 50, 51, 52, 53,
            54, 55, 56, 57, 58, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82,
            83, 84, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108,
            109, 110, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 125, 126, 127, 128, 129, 130, 131,
            132, 133, 134, 135, 136, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 151, 152, 153, 154,
            155, 156, 157, 158, 159, 160, 161, 162, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 177,
            178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199,
            200, 201, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 216, 217, 218, 219, 220, 221, 222,
            223, 224, 225, 226, 227, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 242, 243, 244, 245,
            246, 247, 248, 249, 250, 251, 252, 253, 253)

    // Each inter frame can use up to 3 frames for reference
    const val REFS_PER_FRAME = 3

    // Number of values that can be decoded for mv_fr
    const val MV_FR_SIZE = 4

    // Number of positions to search in motion vector prediction
    const val MVREF_NEIGHBOURS = 8

    // Number of contexts when decoding intra_mode
    const val BLOCK_SIZE_GROUPS = 4

    // Number of different block sizes used
    const val BLOCK_SIZES = 13

    // Number of contexts when decoding partition
    const val PARTITION_CONTEXTS = 16

    // Smallest size of a mode info block
    const val MI_SIZE = 8

    // Minimum width of a tile in units of superblocks (although
    // tiles on the right hand edge can be narrower)
    const val MIN_TILE_WIDTH_B64 = 4

    // Maximum width of a tile in units of superblocks
    const val MAX_TILE_WIDTH_B64 = 64

    // Number of motion vectors returned by find_mv_refs process
    const val MAX_MV_REF_CANDIDATES = 2

    // Number of frames that can be stored for future reference
    const val NUM_REF_FRAMES = 8

    // Number of values that can be derived for ref_frame
    const val MAX_REF_FRAMES = 4

    // Number of contexts for is_inter
    const val IS_INTER_CONTEXTS = 4

    // Number of contexts for comp_mode
    const val COMP_MODE_CONTEXTS = 5

    // Number of contexts for single_ref and comp_ref
    const val REF_CONTEXTS = 5

    // Number of segments allowed in segmentation map
    const val MAX_SEGMENTS = 8

    // Index for quantizer segment feature
    const val SEG_LVL_ALT_Q = 0

    // Index for loop filter segment feature
    const val SEG_LVL_ALT_L = 1

    // Index for reference frame segment feature
    const val SEG_LVL_REF_FRAME = 2

    // Index for skip segment feature
    const val SEG_LVL_SKIP = 3

    // Number of segment features
    const val SEG_LVL_MAX = 4

    // Number of different plane types (Y or UV)
    const val BLOCK_TYPES = 2

    // Number of different prediction types (intra or inter)
    const val REF_TYPES = 2

    // Number of coefficient bands
    const val COEF_BANDS = 6

    // Number of contexts for decoding coefficients
    const val PREV_COEF_CONTEXTS = 6

    // Number of coefficient probabilities that are directly
    // transmitted
    const val UNCONSTRAINED_NODES = 3

    // Number of contexts for transform size
    const val TX_SIZE_CONTEXTS = 2

    // Number of values for interp_filter
    const val SWITCHABLE_FILTERS = 3

    // Number of contexts for interp_filter
    const val INTERP_FILTER_CONTEXTS = 4

    // Number of contexts for decoding skip
    const val SKIP_CONTEXTS = 3

    // Number of values for partition
    const val PARTITION_TYPES = 4

    // Number of values for tx_size
    const val TX_SIZES = 4

    // Number of values for tx_mode
    const val TX_MODES = 5

    // Inverse transform rows with DCT and columns with DCT
    const val DCT_DCT = 0

    // Inverse transform rows with DCT and columns with ADST
    const val ADST_DCT = 1

    // Inverse transform rows with ADST and columns with DCT
    const val DCT_ADST = 2

    // Inverse transform rows with ADST and columns with ADST
    const val ADST_ADST = 3

    // Number of values for y_mode
    const val MB_MODE_COUNT = 14

    // Number of values for intra_mode
    const val INTRA_MODES = 10

    // Number of values for inter_mode
    const val INTER_MODES = 4

    // Number of contexts for inter_mode
    const val INTER_MODE_CONTEXTS = 7

    // Number of values for mv_joint
    const val MV_JOINTS = 4

    // Number of values for mv_class
    const val MV_CLASSES = 11

    // Number of values for mv_class0_bit
    const val CLASS0_SIZE = 2

    // Maximum number of bits for decoding motion vectors
    const val MV_OFFSET_BITS = 10

    // Number of values allowed for a probability adjustment
    const val MAX_PROB = 255

    // Number of different mode types for loop filtering
    const val MAX_MODE_LF_DELTAS = 2

    // Threshold at which motion vectors are considered large
    const val COMPANDED_MVREF_THRESH = 8

    // Maximum value used for loop filtering
    const val MAX_LOOP_FILTER = 63

    // Number of bits of precision when scaling reference frames
    const val REF_SCALE_SHIFT = 14

    // Number of bits of precision when performing inter prediction
    const val SUBPEL_BITS = 4

    // 1 << SUBPEL_BITS
    const val SUBPEL_SHIFTS = 16

    // SUBPEL_SHIFTS - 1
    const val SUBPEL_MASK = 15

    // Value used when clipping motion vectors
    const val MV_BORDER = 128

    // Value used when clipping motion vectors
    const val INTERP_EXTEND = 4

    // Value used when clipping motion vectors
    const val BORDERINPIXELS = 160

    // Value used in adapting probabilities
    const val MAX_UPDATE_FACTOR = 128

    // Value used in adapting probabilities
    const val COUNT_SAT = 20

    // Both candidates use ZEROMV
    const val BOTH_ZERO = 0

    // One candidate uses ZEROMV, one uses NEARMV or NEARESTMV
    const val ZERO_PLUS_PREDICTED = 1

    // Both candidates use NEARMV or NEARESTMV
    const val BOTH_PREDICTED = 2

    // One candidate uses NEWMV, one uses ZEROMV
    const val NEW_PLUS_NON_INTRA = 3

    // Both candidates use NEWMV
    const val BOTH_NEW = 4

    // One candidate uses intra prediction, one uses inter prediction
    const val INTRA_PLUS_NON_INTRA = 5

    // Both candidates use intra prediction
    const val BOTH_INTRA = 6

    // Sentinel value marking a case that can never occur
    const val INVALID_CASE = 9

    // Unknown (in this case the color space must be signaled outside the VP9
    // bitstream).
    const val CS_UNKNOWN = 0

    // Rec. ITU-R BT.601-7
    const val CS_BT_601 = 1

    // Rec. ITU-R BT.709-6
    const val CS_BT_709 = 2

    // SMPTE-170
    const val CS_SMPTE_170 = 3

    // SMPTE-240
    const val CS_SMPTE_240 = 4

    // Rec. ITU-R BT.2020-2
    const val CS_BT_2020 = 5

    // Reserved
    const val CS_RESERVED = 6

    // sRGB (IEC 61966-2-1)
    const val CS_RGB = 7
    @JvmField
    val SEGMENTATION_FEATURE_BITS = intArrayOf(8, 6, 2, 0)
    @JvmField
    val SEGMENTATION_FEATURE_SIGNED = intArrayOf(1, 1, 0, 0)
    @JvmField
    val tx_mode_to_biggest_tx_size = intArrayOf(TX_4X4, TX_8X8, TX_16X16, TX_32X32, TX_32X32)
    @JvmField
    val intra_mode_to_tx_type_lookup = intArrayOf(
            DCT_DCT,  // DC
            ADST_DCT,  // V
            DCT_ADST,  // H
            DCT_DCT,  // D45
            ADST_ADST,  // D135
            ADST_DCT,  // D117
            DCT_ADST,  // D153
            DCT_ADST,  // D207
            ADST_DCT,  // D63
            ADST_ADST)
    const val CAT1_MIN_VAL = 5
    const val CAT2_MIN_VAL = 7
    const val CAT3_MIN_VAL = 11
    const val CAT4_MIN_VAL = 19
    const val CAT5_MIN_VAL = 35
    const val CAT6_MIN_VAL = 67
    val catMinVal = intArrayOf(CAT1_MIN_VAL, CAT2_MIN_VAL, CAT3_MIN_VAL, CAT4_MIN_VAL, CAT5_MIN_VAL,
            CAT6_MIN_VAL)
    @JvmField
    val uv_txsize_lookup = arrayOf(arrayOf(arrayOf(intArrayOf(TX_4X4, TX_4X4), intArrayOf(TX_4X4, TX_4X4)), arrayOf(intArrayOf(TX_4X4, TX_4X4), intArrayOf(TX_4X4, TX_4X4)), arrayOf(intArrayOf(TX_4X4, TX_4X4), intArrayOf(TX_4X4, TX_4X4)), arrayOf(intArrayOf(TX_4X4, TX_4X4), intArrayOf(TX_4X4, TX_4X4))), arrayOf(arrayOf(intArrayOf(TX_4X4, TX_4X4), intArrayOf(TX_4X4, TX_4X4)), arrayOf(intArrayOf(TX_4X4, TX_4X4), intArrayOf(TX_4X4, TX_4X4)), arrayOf(intArrayOf(TX_4X4, TX_4X4), intArrayOf(TX_4X4, TX_4X4)), arrayOf(intArrayOf(TX_4X4, TX_4X4), intArrayOf(TX_4X4, TX_4X4))), arrayOf(arrayOf(intArrayOf(TX_4X4, TX_4X4), intArrayOf(TX_4X4, TX_4X4)), arrayOf(intArrayOf(TX_4X4, TX_4X4), intArrayOf(TX_4X4, TX_4X4)), arrayOf(intArrayOf(TX_4X4, TX_4X4), intArrayOf(TX_4X4, TX_4X4)), arrayOf(intArrayOf(TX_4X4, TX_4X4), intArrayOf(TX_4X4, TX_4X4))), arrayOf(arrayOf(intArrayOf(TX_4X4, TX_4X4), intArrayOf(TX_4X4, TX_4X4)), arrayOf(intArrayOf(TX_8X8, TX_4X4), intArrayOf(TX_4X4, TX_4X4)), arrayOf(intArrayOf(TX_8X8, TX_4X4), intArrayOf(TX_4X4, TX_4X4)), arrayOf(intArrayOf(TX_8X8, TX_4X4), intArrayOf(TX_4X4, TX_4X4))), arrayOf(arrayOf(intArrayOf(TX_4X4, TX_4X4), intArrayOf(TX_4X4, TX_4X4)), arrayOf(intArrayOf(TX_8X8, TX_8X8), intArrayOf(TX_4X4, TX_4X4)), arrayOf(intArrayOf(TX_8X8, TX_8X8), intArrayOf(TX_4X4, TX_4X4)), arrayOf(intArrayOf(TX_8X8, TX_8X8), intArrayOf(TX_4X4, TX_4X4))), arrayOf(arrayOf(intArrayOf(TX_4X4, TX_4X4), intArrayOf(TX_4X4, TX_4X4)), arrayOf(intArrayOf(TX_8X8, TX_4X4), intArrayOf(TX_8X8, TX_4X4)), arrayOf(intArrayOf(TX_8X8, TX_4X4), intArrayOf(TX_8X8, TX_8X8)), arrayOf(intArrayOf(TX_8X8, TX_4X4), intArrayOf(TX_8X8, TX_8X8))), arrayOf(arrayOf(intArrayOf(TX_4X4, TX_4X4), intArrayOf(TX_4X4, TX_4X4)), arrayOf(intArrayOf(TX_8X8, TX_8X8), intArrayOf(TX_8X8, TX_8X8)), arrayOf(intArrayOf(TX_16X16, TX_8X8), intArrayOf(TX_8X8, TX_8X8)), arrayOf(intArrayOf(TX_16X16, TX_8X8), intArrayOf(TX_8X8, TX_8X8))), arrayOf(arrayOf(intArrayOf(TX_4X4, TX_4X4), intArrayOf(TX_4X4, TX_4X4)), arrayOf(intArrayOf(TX_8X8, TX_8X8), intArrayOf(TX_8X8, TX_8X8)), arrayOf(intArrayOf(TX_16X16, TX_16X16), intArrayOf(TX_8X8, TX_8X8)), arrayOf(intArrayOf(TX_16X16, TX_16X16), intArrayOf(TX_8X8, TX_8X8))), arrayOf(arrayOf(intArrayOf(TX_4X4, TX_4X4), intArrayOf(TX_4X4, TX_4X4)), arrayOf(intArrayOf(TX_8X8, TX_8X8), intArrayOf(TX_8X8, TX_8X8)), arrayOf(intArrayOf(TX_16X16, TX_8X8), intArrayOf(TX_16X16, TX_8X8)), arrayOf(intArrayOf(TX_16X16, TX_8X8), intArrayOf(TX_16X16, TX_8X8))), arrayOf(arrayOf(intArrayOf(TX_4X4, TX_4X4), intArrayOf(TX_4X4, TX_4X4)), arrayOf(intArrayOf(TX_8X8, TX_8X8), intArrayOf(TX_8X8, TX_8X8)), arrayOf(intArrayOf(TX_16X16, TX_16X16), intArrayOf(TX_16X16, TX_16X16)), arrayOf(intArrayOf(TX_32X32, TX_16X16), intArrayOf(TX_16X16, TX_16X16))), arrayOf(arrayOf(intArrayOf(TX_4X4, TX_4X4), intArrayOf(TX_4X4, TX_4X4)), arrayOf(intArrayOf(TX_8X8, TX_8X8), intArrayOf(TX_8X8, TX_8X8)), arrayOf(intArrayOf(TX_16X16, TX_16X16), intArrayOf(TX_16X16, TX_16X16)), arrayOf(intArrayOf(TX_32X32, TX_32X32), intArrayOf(TX_16X16, TX_16X16))), arrayOf(arrayOf(intArrayOf(TX_4X4, TX_4X4), intArrayOf(TX_4X4, TX_4X4)), arrayOf(intArrayOf(TX_8X8, TX_8X8), intArrayOf(TX_8X8, TX_8X8)), arrayOf(intArrayOf(TX_16X16, TX_16X16), intArrayOf(TX_16X16, TX_16X16)), arrayOf(intArrayOf(TX_32X32, TX_16X16), intArrayOf(TX_32X32, TX_16X16))), arrayOf(arrayOf(intArrayOf(TX_4X4, TX_4X4), intArrayOf(TX_4X4, TX_4X4)), arrayOf(intArrayOf(TX_8X8, TX_8X8), intArrayOf(TX_8X8, TX_8X8)), arrayOf(intArrayOf(TX_16X16, TX_16X16), intArrayOf(TX_16X16, TX_16X16)), arrayOf(intArrayOf(TX_32X32, TX_32X32), intArrayOf(TX_32X32, TX_32X32))))
}