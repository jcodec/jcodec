package org.jcodec.codecs.h264.io.model

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class SliceType private constructor(private val _name: String, private val _ordinal: Int) {
    val isIntra: Boolean
        get() = this == I || this == SI

    val isInter: Boolean
        get() = this != I && this != SI

    fun ordinal(): Int {
        return _ordinal
    }

    override fun toString(): String {
        return _name
    }

    fun name(): String {
        return _name
    }

    companion object {
        private val _values = arrayOfNulls<SliceType>(5)
        @JvmField
        val P = SliceType("P", 0)
        @JvmField
        val B = SliceType("B", 1)
        @JvmField
        val I = SliceType("I", 2)
        val SP = SliceType("SP", 3)
        val SI = SliceType("SI", 4)
        fun values(): Array<SliceType?> {
            return _values
        }

        fun fromValue(j: Int): SliceType? {
            return values()[j]
        }
    }

    init {
        _values[_ordinal] = this
    }
}