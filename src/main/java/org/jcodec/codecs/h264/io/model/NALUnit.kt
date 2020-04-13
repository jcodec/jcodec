package org.jcodec.codecs.h264.io.model

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Network abstraction layer (NAL) unit
 *
 * @author The JCodec project
 */
class NALUnit(@JvmField var type: NALUnitType, @JvmField var nal_ref_idc: Int) {
    fun write(out: ByteBuffer) {
        val nalu = type.value or (nal_ref_idc shl 5)
        out.put(nalu.toByte())
    }

    companion object {
        @JvmStatic
        fun read(_in: ByteBuffer): NALUnit? {
            val nalu: Int = _in.get().toInt() and 0xff
            val nal_ref_idc = nalu shr 5 and 0x3
            val nb = nalu and 0x1f
            val type = NALUnitType.fromValue(nb)
            return if (type != null) NALUnit(type, nal_ref_idc) else null
        }
    }

}