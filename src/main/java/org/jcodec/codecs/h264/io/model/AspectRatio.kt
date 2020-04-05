package org.jcodec.codecs.h264.io.model

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Aspect ratio
 *
 * dynamic enum
 *
 * @author The JCodec project
 */
class AspectRatio private constructor(val value: Int) {

    companion object {
        @JvmField
        val Extended_SAR = AspectRatio(255)
        @JvmStatic
        fun fromValue(value: Int): AspectRatio {
            return if (value == Extended_SAR.value) {
                Extended_SAR
            } else AspectRatio(value)
        }
    }

}