package org.jcodec.containers.mp4.boxes.channel

import org.jcodec.common.model.Label
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class ChannelLayout private constructor(val code: Int, val labels: Array<Label?>) {

    companion object {
        private val _values: MutableList<ChannelLayout> = ArrayList()
        val kCAFChannelLayoutTag_UseChannelDescriptions = ChannelLayout(0 shl 16 or 0, arrayOf())
        val kCAFChannelLayoutTag_UseChannelBitmap = ChannelLayout(1 shl 16 or 0, arrayOf())
        val kCAFChannelLayoutTag_Mono = ChannelLayout(100 shl 16 or 1, arrayOf(Label.Mono))
        val kCAFChannelLayoutTag_Stereo = ChannelLayout(101 shl 16 or 2, arrayOf(Label.Left, Label.Right))
        val kCAFChannelLayoutTag_StereoHeadphones = ChannelLayout(102 shl 16 or 2, arrayOf(Label.HeadphonesLeft, Label.HeadphonesRight))
        val kCAFChannelLayoutTag_MatrixStereo = ChannelLayout(103 shl 16 or 2, arrayOf(Label.LeftTotal, Label.RightTotal))
        val kCAFChannelLayoutTag_MidSide = ChannelLayout(104 shl 16 or 2, arrayOf(Label.MS_Mid, Label.MS_Side))
        val kCAFChannelLayoutTag_XY = ChannelLayout(105 shl 16 or 2, arrayOf(Label.XY_X, Label.XY_Y))
        val kCAFChannelLayoutTag_Binaural = ChannelLayout(106 shl 16 or 2, arrayOf(Label.HeadphonesLeft, Label.HeadphonesRight))
        val kCAFChannelLayoutTag_Ambisonic_B_Format = ChannelLayout(107 shl 16 or 4, arrayOf(Label.Ambisonic_W, Label.Ambisonic_X, Label.Ambisonic_Y, Label.Ambisonic_Z))
        val kCAFChannelLayoutTag_Quadraphonic = ChannelLayout(108 shl 16 or 4, arrayOf(Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround))
        val kCAFChannelLayoutTag_Pentagonal = ChannelLayout(109 shl 16 or 5, arrayOf(Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround, Label.Center))
        val kCAFChannelLayoutTag_Hexagonal = ChannelLayout(110 shl 16 or 6, arrayOf(Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround, Label.Center, Label.CenterSurround))
        val kCAFChannelLayoutTag_Octagonal = ChannelLayout(111 shl 16 or 8, arrayOf(Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround, Label.Center, Label.CenterSurround, Label.LeftCenter, Label.RightCenter))
        val kCAFChannelLayoutTag_Cube = ChannelLayout(112 shl 16 or 8, arrayOf(Label.Left,
                Label.Right, Label.LeftSurround, Label.RightSurround, Label.TopBackLeft, Label.TopBackRight, Label.TopBackCenter, Label.TopCenterSurround))
        val kCAFChannelLayoutTag_MPEG_3_0_A = ChannelLayout(113 shl 16 or 3, arrayOf(Label.Left, Label.Right, Label.Center))
        val kCAFChannelLayoutTag_MPEG_3_0_B = ChannelLayout(114 shl 16 or 3, arrayOf(Label.Center, Label.Left, Label.Right))
        val kCAFChannelLayoutTag_MPEG_4_0_A = ChannelLayout(115 shl 16 or 4, arrayOf(Label.Left, Label.Right, Label.Center, Label.CenterSurround))
        val kCAFChannelLayoutTag_MPEG_4_0_B = ChannelLayout(116 shl 16 or 4, arrayOf(Label.Center, Label.Left, Label.Right, Label.CenterSurround))
        val kCAFChannelLayoutTag_MPEG_5_0_A = ChannelLayout(117 shl 16 or 5, arrayOf(Label.Left, Label.Right, Label.Center, Label.LeftSurround, Label.RightSurround))
        val kCAFChannelLayoutTag_MPEG_5_0_B = ChannelLayout(118 shl 16 or 5, arrayOf(Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround, Label.Center))
        val kCAFChannelLayoutTag_MPEG_5_0_C = ChannelLayout(119 shl 16 or 5, arrayOf(Label.Left, Label.Center, Label.Right, Label.LeftSurround, Label.RightSurround))
        val kCAFChannelLayoutTag_MPEG_5_0_D = ChannelLayout(120 shl 16 or 5, arrayOf(Label.Center, Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround))
        val kCAFChannelLayoutTag_MPEG_5_1_A = ChannelLayout(121 shl 16 or 6, arrayOf(Label.Left, Label.Right, Label.Center, Label.LFEScreen, Label.LeftSurround, Label.RightSurround))
        val kCAFChannelLayoutTag_MPEG_5_1_B = ChannelLayout(122 shl 16 or 6, arrayOf(Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround, Label.Center, Label.LFEScreen))
        val kCAFChannelLayoutTag_MPEG_5_1_C = ChannelLayout(123 shl 16 or 6, arrayOf(Label.Left, Label.Center, Label.Right, Label.LeftSurround, Label.RightSurround, Label.LFEScreen))
        val kCAFChannelLayoutTag_MPEG_5_1_D = ChannelLayout(124 shl 16 or 6, arrayOf(Label.Center, Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround, Label.LFEScreen))
        val kCAFChannelLayoutTag_MPEG_6_1_A = ChannelLayout(125 shl 16 or 7, arrayOf(Label.Left, Label.Right, Label.Center, Label.LFEScreen, Label.LeftSurround, Label.RightSurround, Label.Right))
        val kCAFChannelLayoutTag_MPEG_7_1_A = ChannelLayout(126 shl 16 or 8, arrayOf(Label.Left, Label.Right, Label.Center, Label.LFEScreen, Label.LeftSurround, Label.RightSurround, Label.LeftCenter, Label.RightCenter))
        val kCAFChannelLayoutTag_MPEG_7_1_B = ChannelLayout(127 shl 16 or 8, arrayOf(Label.Center, Label.LeftCenter, Label.RightCenter, Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround, Label.LFEScreen))
        val kCAFChannelLayoutTag_MPEG_7_1_C = ChannelLayout(128 shl 16 or 8, arrayOf(
                Label.Left, Label.Right, Label.Center, Label.LFEScreen, Label.LeftSurround, Label.RightSurround, Label.RearSurroundLeft, Label.RearSurroundRight))
        val kCAFChannelLayoutTag_Emagic_Default_7_1 = ChannelLayout(129 shl 16 or 8, arrayOf(Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround, Label.Center, Label.LFEScreen, Label.LeftCenter, Label.RightCenter))
        val kCAFChannelLayoutTag_SMPTE_DTV = ChannelLayout(130 shl 16 or 8, arrayOf(Label.Left, Label.Right, Label.Center, Label.LFEScreen, Label.LeftSurround, Label.RightSurround, Label.LeftTotal, Label.RightTotal))
        val kCAFChannelLayoutTag_ITU_2_1 = ChannelLayout(131 shl 16 or 3, arrayOf(Label.Left, Label.Right, Label.CenterSurround))
        val kCAFChannelLayoutTag_ITU_2_2 = ChannelLayout(132 shl 16 or 4, arrayOf(Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround))
        val kCAFChannelLayoutTag_DVD_4 = ChannelLayout(133 shl 16 or 3, arrayOf(Label.Left, Label.Right, Label.LFEScreen))
        val kCAFChannelLayoutTag_DVD_5 = ChannelLayout(134 shl 16 or 4, arrayOf(Label.Left, Label.Right, Label.LFEScreen, Label.CenterSurround))
        val kCAFChannelLayoutTag_DVD_6 = ChannelLayout(135 shl 16 or 5, arrayOf(Label.Left, Label.Right, Label.LFEScreen, Label.LeftSurround, Label.RightSurround))
        val kCAFChannelLayoutTag_DVD_10 = ChannelLayout(136 shl 16 or 4, arrayOf(Label.Left, Label.Right, Label.Center, Label.LFEScreen))
        val kCAFChannelLayoutTag_DVD_11 = ChannelLayout(137 shl 16 or 5, arrayOf(Label.Left, Label.Right, Label.Center, Label.LFEScreen, Label.CenterSurround))
        val kCAFChannelLayoutTag_DVD_18 = ChannelLayout(138 shl 16 or 5, arrayOf(Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround, Label.LFEScreen))
        val kCAFChannelLayoutTag_AudioUnit_6_0 = ChannelLayout(139 shl 16 or 6, arrayOf(Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround, Label.Center, Label.CenterSurround))
        val kCAFChannelLayoutTag_AudioUnit_7_0 = ChannelLayout(140 shl 16 or 7, arrayOf(Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround, Label.Center, Label.RearSurroundLeft, Label.RearSurroundRight))
        val kCAFChannelLayoutTag_AAC_6_0 = ChannelLayout(141 shl 16 or 6, arrayOf(Label.Center, Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround, Label.CenterSurround))
        val kCAFChannelLayoutTag_AAC_6_1 = ChannelLayout(142 shl 16 or 7, arrayOf(Label.Center, Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround, Label.CenterSurround, Label.LFEScreen))
        val kCAFChannelLayoutTag_AAC_7_0 = ChannelLayout(143 shl 16 or 7, arrayOf(Label.Center, Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround, Label.RearSurroundLeft, Label.RearSurroundRight))
        val kCAFChannelLayoutTag_AAC_Octagonal = ChannelLayout(144 shl 16 or 8, arrayOf(Label.Center, Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround, Label.RearSurroundLeft, Label.RearSurroundRight,
                Label.CenterSurround))
        val kCAFChannelLayoutTag_TMH_10_2_std = ChannelLayout(145 shl 16 or 16, arrayOf(Label.Left, Label.Right, Label.Center, Label.Mono, Label.Mono, Label.Mono, Label.LeftSurround, Label.RightSurround, Label.Mono, Label.Mono, Label.Mono, Label.Mono,
                Label.Mono, Label.CenterSurround, Label.LFEScreen, Label.LFE2))
        val kCAFChannelLayoutTag_TMH_10_2_full = ChannelLayout(146 shl 16 or 21, arrayOf(Label.LeftCenter, Label.RightCenter, Label.Mono, Label.Mono, Label.Mono))
        val kCAFChannelLayoutTag_RESERVED_DO_NOT_USE = ChannelLayout(147 shl 16, arrayOfNulls(0))
        fun values(): Array<ChannelLayout> {
            return _values.toTypedArray()
        }
    }

    init {
        _values.add(this)
    }
}