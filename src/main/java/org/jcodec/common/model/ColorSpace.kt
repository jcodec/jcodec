package org.jcodec.common.model

/**
 *
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class ColorSpace private constructor(private val _name: String, @JvmField var nComp: Int, @JvmField var compPlane: IntArray?, @JvmField var compWidth: IntArray?, @JvmField var compHeight: IntArray?, @JvmField var planar: Boolean) {
    @JvmField
    var bitsPerPixel: Int
    override fun toString(): String {
        return _name
    }

    val widthMask: Int
        get() = (if (nComp > 1) compWidth!![1] else 0).inv()

    val heightMask: Int
        get() = (if (nComp > 1) compHeight!![1] else 0).inv()

    /**
     * Determines if two colors match. Aside from simply comparing the objects
     * this function also takes into account lables ANY, ANY_INTERLEAVED, ANY
     * PLANAR.
     *
     * @param inputColor
     * @return True if the color is the same or matches the label.
     */
    fun matches(inputColor: ColorSpace): Boolean {
        if (inputColor == this) return true
        if (inputColor == ANY || this == ANY) return true
        return if ((inputColor == ANY_INTERLEAVED || this == ANY_INTERLEAVED || inputColor == ANY_PLANAR || this == ANY_PLANAR)
                && inputColor.planar == planar) true else false
    }

    /**
     * Calculates the component size based on the fullt size and color subsampling of the given component index.
     * @param size
     * @return Component size
     */
    fun compSize(size: Size, comp: Int): Size {
        return if (compWidth!![comp] == 0 && compHeight!![comp] == 0) size else Size(size.width shr compWidth!![comp], size.height shr compHeight!![comp])
    }

    companion object {
        const val MAX_PLANES = 4
        private fun calcBitsPerPixel(nComp: Int, compWidth: IntArray?, compHeight: IntArray?): Int {
            var bitsPerPixel = 0
            for (i in 0 until nComp) {
                bitsPerPixel += 8 shr compWidth!![i] shr compHeight!![i]
            }
            return bitsPerPixel
        }

        private val _000 = intArrayOf(0, 0, 0)
        private val _011 = intArrayOf(0, 1, 1)
        private val _012 = intArrayOf(0, 1, 2)

        @JvmField
        val BGR = ColorSpace("BGR", 3, _000, _000, _000, false)

        @JvmField
        val RGB = ColorSpace("RGB", 3, _000, _000, _000, false)

        @JvmField
        val YUV420 = ColorSpace("YUV420", 3, _012, _011, _011, true)

        @JvmField
        val YUV420J = ColorSpace("YUV420J", 3, _012, _011, _011, true)

        @JvmField
        val YUV422 = ColorSpace("YUV422", 3, _012, _011, _000, true)

        @JvmField
        val YUV422J = ColorSpace("YUV422J", 3, _012, _011, _000, true)

        @JvmField
        val YUV444 = ColorSpace("YUV444", 3, _012, _000, _000, true)

        @JvmField
        val YUV444J = ColorSpace("YUV444J", 3, _012, _000, _000, true)

        @JvmField
        val YUV422_10 = ColorSpace("YUV422_10", 3, _012, _011, _000, true)

        @JvmField
        val GREY = ColorSpace("GREY", 1, intArrayOf(0), intArrayOf(0), intArrayOf(0), true)
        val MONO = ColorSpace("MONO", 1, _000, _000, _000, true)

        @JvmField
        val YUV444_10 = ColorSpace("YUV444_10", 3, _012, _000, _000, true)

        /**
         * Any color space, used in the cases where any color space will do.
         */
        val ANY = ColorSpace("ANY", 0, null, null, null, true)

        /**
         * Any planar color space, used in the cases where any planar color space will do.
         */
        @JvmField
        val ANY_PLANAR = ColorSpace("ANY_PLANAR", 0, null, null, null, true)

        /**
         * Any interleaved color space, used in the cases where any interleaved color space will do.
         */
        val ANY_INTERLEAVED = ColorSpace("ANY_INTERLEAVED", 0, null, null, null, false)

        /**
         * Same color, used in filters to declare that the color stays unchanged.
         */
        @JvmField
        val SAME = ColorSpace("SAME", 0, null, null, null, false)
    }

    init {
        bitsPerPixel = calcBitsPerPixel(nComp, compWidth, compHeight)
    }
}