package org.jcodec.codecs.h264.io.model

import org.jcodec.codecs.h264.decode.CAVLCReader
import org.jcodec.codecs.h264.io.write.CAVLCWriter.writeTrailingBits
import org.jcodec.common.io.BitReader
import org.jcodec.common.io.BitWriter
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Sequence Parameter Set Extension entity of H264 bitstream
 *
 * Capable to serialize / deserialize itself with CAVLC bit stream
 *
 * @author The JCodec project
 */
class SeqParameterSetExt {
    var seq_parameter_set_id = 0
    var aux_format_idc = 0
    var bit_depth_aux_minus8 = 0
    var alpha_incr_flag = false
    var additional_extension_flag = false
    var alpha_opaque_value = 0
    var alpha_transparent_value = 0
    fun write(out: ByteBuffer?) {
        val writer = BitWriter(out)
        writeTrailingBits(writer)
    }

    companion object {
        fun read(`is`: ByteBuffer?): SeqParameterSetExt {
            val _in = BitReader.createBitReader(`is`)
            val spse = SeqParameterSetExt()
            spse.seq_parameter_set_id = CAVLCReader.readUEtrace(_in, "SPSE: seq_parameter_set_id")
            spse.aux_format_idc = CAVLCReader.readUEtrace(_in, "SPSE: aux_format_idc")
            if (spse.aux_format_idc != 0) {
                spse.bit_depth_aux_minus8 = CAVLCReader.readUEtrace(_in, "SPSE: bit_depth_aux_minus8")
                spse.alpha_incr_flag = CAVLCReader.readBool(_in, "SPSE: alpha_incr_flag")
                spse.alpha_opaque_value = CAVLCReader.readU(_in, spse.bit_depth_aux_minus8 + 9, "SPSE: alpha_opaque_value")
                spse.alpha_transparent_value = CAVLCReader.readU(_in, spse.bit_depth_aux_minus8 + 9, "SPSE: alpha_transparent_value")
            }
            spse.additional_extension_flag = CAVLCReader.readBool(_in, "SPSE: additional_extension_flag")
            return spse
        }
    }
}