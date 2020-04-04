package org.jcodec.containers.mp4.boxes

import org.jcodec.platform.Platform
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class MetaValue private constructor(val type: Int, val locale: Int, val data: ByteArray) {
    val int: Int
        get() {
            if (type == TYPE_UINT_V || type == TYPE_INT_V) {
                when (data.size) {
                    1 -> return data[0].toInt()
                    2 -> return toInt16(data)
                    3 -> return toInt24(data)
                    4 -> return toInt32(data)
                }
            }
            if (type == TYPE_INT_8) return data[0].toInt()
            if (type == TYPE_INT_16) return toInt16(data)
            return if (type == TYPE_INT_32) toInt32(data) else 0
        }

    val float: Double
        get() {
            if (type == TYPE_FLOAT_32) return toFloat(data).toDouble()
            return if (type == TYPE_FLOAT_64) toDouble(data) else 0.0
        }

    val string: String?
        get() {
            if (type == TYPE_STRING_UTF8) return Platform.stringFromCharset(data, Platform.UTF_8)
            return if (type == TYPE_STRING_UTF16) {
                Platform.stringFromCharset(data, Platform.UTF_16BE)
            } else null
        }

    fun isInt(): Boolean {
        return type == TYPE_UINT_V || type == TYPE_INT_V || type == TYPE_INT_8 || type == TYPE_INT_16 || type == TYPE_INT_32
    }

    fun isString(): Boolean {
        return type == TYPE_STRING_UTF8 || type == TYPE_STRING_UTF16
    }

    fun isFloat(): Boolean {
        return type == TYPE_FLOAT_32 || type == TYPE_FLOAT_64
    }

    override fun toString(): String {
        return if (isInt()) int.toString() else if (isFloat()) float.toString() else if (isString()) string.toString() else "BLOB"
    }

    private fun toInt16(data: ByteArray): Int {
        val bb = ByteBuffer.wrap(data)
        bb.order(ByteOrder.BIG_ENDIAN)
        return bb.short.toInt()
    }

    private fun toInt24(data: ByteArray): Int {
        val bb = ByteBuffer.wrap(data)
        bb.order(ByteOrder.BIG_ENDIAN)
        return bb.short.toInt() and 0xffff shl 8 or (bb.get().toInt() and 0xff)
    }

    private fun toInt32(data: ByteArray): Int {
        val bb = ByteBuffer.wrap(data)
        bb.order(ByteOrder.BIG_ENDIAN)
        return bb.int
    }

    private fun toFloat(data: ByteArray): Float {
        val bb = ByteBuffer.wrap(data)
        bb.order(ByteOrder.BIG_ENDIAN)
        return bb.float
    }

    private fun toDouble(data: ByteArray): Double {
        val bb = ByteBuffer.wrap(data)
        bb.order(ByteOrder.BIG_ENDIAN)
        return bb.double
    }

    val isBlob: Boolean
        get() = !isFloat() && !isInt() && !isString()

    companion object {
        // All data types below are Big Endian
        const val TYPE_STRING_UTF16 = 2
        const val TYPE_STRING_UTF8 = 1
        const val TYPE_FLOAT_64 = 24
        const val TYPE_FLOAT_32 = 23
        const val TYPE_INT_32 = 67
        const val TYPE_INT_16 = 66
        const val TYPE_INT_8 = 65
        const val TYPE_INT_V = 22
        const val TYPE_UINT_V = 21
        const val TYPE_JPEG = 13
        const val TYPE_PNG = 13
        const val TYPE_BMP = 27
        @JvmStatic
        fun createInt(value: Int): MetaValue {
            return MetaValue(21, 0, fromInt(value))
        }

        @JvmStatic
        fun createFloat(value: Float): MetaValue {
            return MetaValue(23, 0, fromFloat(value))
        }

        @JvmStatic
        fun createString(value: String?): MetaValue {
            return MetaValue(1, 0, Platform.getBytesForCharset(value, Platform.UTF_8))
        }

        fun createStringWithLocale(value: String?, locale: Int): MetaValue {
            return MetaValue(1, locale, Platform.getBytesForCharset(value, Platform.UTF_8))
        }

        @JvmStatic
        fun createOther(type: Int, data: ByteArray): MetaValue {
            return MetaValue(type, 0, data)
        }

        fun createOtherWithLocale(type: Int, locale: Int, data: ByteArray): MetaValue {
            return MetaValue(type, locale, data)
        }

        private fun fromFloat(floatValue: Float): ByteArray {
            val bytes = ByteArray(4)
            val bb = ByteBuffer.wrap(bytes)
            bb.order(ByteOrder.BIG_ENDIAN)
            bb.putFloat(floatValue)
            return bytes
        }

        private fun fromInt(value: Int): ByteArray {
            val bytes = ByteArray(4)
            val bb = ByteBuffer.wrap(bytes)
            bb.order(ByteOrder.BIG_ENDIAN)
            bb.putInt(value)
            return bytes
        }
    }

}