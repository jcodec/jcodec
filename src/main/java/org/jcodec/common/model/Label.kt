package org.jcodec.common.model

import java.util.*
import java.util.regex.Pattern

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 *
 * @author The JCodec project
 */
class Label private constructor(val `val`: Int) {
    val bitmapVal: Long

    companion object {
        private val _values: MutableList<Label> = ArrayList()

        /** unknown role or unspecified other use for channel  */
        val Unknown = Label(-0x1)

        /** channel is present, but has no intended role or destination  */
        val Unused = Label(0)

        /** channel is described solely by the mCoordinates fields  */
        val UseCoordinates = Label(100)
        val Left = Label(1)
        val Right = Label(2)
        val Center = Label(3)
        val LFEScreen = Label(4)

        /** WAVE (.wav files): "Back Left"  */
        val LeftSurround = Label(5)

        /** WAVE: "Back Right"  */
        val RightSurround = Label(6)
        val LeftCenter = Label(7)
        val RightCenter = Label(8)

        /** WAVE: "Back  Center or  plain "Rear Surround"  */
        val CenterSurround = Label(9)

        /** WAVE: "Side Left"  */
        val LeftSurroundDirect = Label(10)

        /** WAVE: "Side Right"  */
        val RightSurroundDirect = Label(11)
        val TopCenterSurround = Label(12)

        /** WAVE: "Top Front Left"  */
        val VerticalHeightLeft = Label(13)

        /** WAVE: "Top Front Center"  */
        val VerticalHeightCenter = Label(14)

        /** WAVE: "Top Front Right"  */
        val VerticalHeightRight = Label(15)
        val TopBackLeft = Label(16)
        val TopBackCenter = Label(17)
        val TopBackRight = Label(18)
        val RearSurroundLeft = Label(33)
        val RearSurroundRight = Label(34)
        val LeftWide = Label(35)
        val RightWide = Label(36)
        val LFE2 = Label(37)

        /** matrix encoded 4 channels  */
        val LeftTotal = Label(38)

        /** matrix encoded 4 channels  */
        val RightTotal = Label(39)
        val HearingImpaired = Label(40)
        val Narration = Label(41)
        val Mono = Label(42)
        val DialogCentricMix = Label(43)

        /** center, non diffuse first order ambisonic channels  */
        val CenterSurroundDirect = Label(44)
        val Ambisonic_W = Label(200)
        val Ambisonic_X = Label(201)
        val Ambisonic_Y = Label(202)
        val Ambisonic_Z = Label(203)

        /** Mid/Side Recording  */
        val MS_Mid = Label(204)
        val MS_Side = Label(205)

        /** X-Y Recording  */
        val XY_X = Label(206)
        val XY_Y = Label(207)
        val HeadphonesLeft = Label(301)
        val HeadphonesRight = Label(302)
        val ClickTrack = Label(304)
        val ForeignLanguage = Label(305)

        // generic discrete channel
        val Discrete = Label(400)

        // numbered discrete channel
        val Discrete_0 = Label(1 shl 16 or 0)
        val Discrete_1 = Label(1 shl 16 or 1)
        val Discrete_2 = Label(1 shl 16 or 2)
        val Discrete_3 = Label(1 shl 16 or 3)
        val Discrete_4 = Label(1 shl 16 or 4)
        val Discrete_5 = Label(1 shl 16 or 5)
        val Discrete_6 = Label(1 shl 16 or 6)
        val Discrete_7 = Label(1 shl 16 or 7)
        val Discrete_8 = Label(1 shl 16 or 8)
        val Discrete_9 = Label(1 shl 16 or 9)
        val Discrete_10 = Label(1 shl 16 or 10)
        val Discrete_11 = Label(1 shl 16 or 11)
        val Discrete_12 = Label(1 shl 16 or 12)
        val Discrete_13 = Label(1 shl 16 or 13)
        val Discrete_14 = Label(1 shl 16 or 14)
        val Discrete_15 = Label(1 shl 16 or 15)
        val Discrete_65535 = Label(1 shl 16 or 65535)
        val channelMappingRegex = Pattern.compile("[_\\ \\.][a-zA-Z]+$")
        fun values(): Array<Label> {
            return _values.toTypedArray()
        }

        fun getByVal(`val`: Int): Label {
            val values = values()
            for (i in values.indices) {
                val label = values[i]
                if (label.`val` == `val`) return label
            }
            return Mono
        }
    }

    init {
        bitmapVal = (if (`val` > 18 || `val` < 1) 0x00000000 else 1 shl (`val` - 1).toLong().toInt()).toLong()
        _values.add(this)
    }
}