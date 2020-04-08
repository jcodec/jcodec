package org.jcodec.containers.mxf.model

import org.jcodec.common.and
import org.jcodec.common.io.NIOUtils
import org.jcodec.platform.Platform
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
abstract class MXFMetadata(@JvmField var ul: UL) {
    @JvmField
    var uid: UL? = null

    abstract fun readBuf(bb: ByteBuffer)
    protected fun readUtf16String(_bb: ByteBuffer): String {
        val array: ByteArray
        array = if (_bb.getShort(_bb.limit() - 2).toInt() != 0) {
            NIOUtils.toArray(_bb)
        } else {
            NIOUtils.toArray(_bb.limit(_bb.limit() - 2) as ByteBuffer)
        }
        return Platform.stringFromCharset(array, Platform.UTF_16)
    }

    companion object {
        /**
         * Utility method to read a batch of ULS
         *
         * @param _bb
         * @return
         */
        @JvmStatic
        protected fun readULBatch(_bb: ByteBuffer): Array<UL?> {
            val count = _bb.int
            _bb.int
            val result = arrayOfNulls<UL>(count)
            for (i in 0 until count) {
                result[i] = UL.read(_bb)
            }
            return result
        }

        /**
         * Utility method to read a batch of int32
         *
         * @param _bb
         * @return
         */
        @JvmStatic
        protected fun readInt32Batch(_bb: ByteBuffer): IntArray {
            val count = _bb.int
            _bb.int
            val result = IntArray(count)
            for (i in 0 until count) {
                result[i] = _bb.int
            }
            return result
        }

        @JvmStatic
        protected fun readDate(_bb: ByteBuffer): Date {
            val calendar = Calendar.getInstance()
            calendar[Calendar.YEAR] = _bb.short.toInt()
            calendar[Calendar.MONTH] = _bb.get().toInt()
            calendar[Calendar.DAY_OF_MONTH] = _bb.get().toInt()
            calendar[Calendar.HOUR] = _bb.get().toInt()
            calendar[Calendar.MINUTE] = _bb.get().toInt()
            calendar[Calendar.SECOND] = _bb.get().toInt()
            calendar[Calendar.MILLISECOND] = _bb.get() and 0xff shl 2
            return calendar.time
        }
    }

}