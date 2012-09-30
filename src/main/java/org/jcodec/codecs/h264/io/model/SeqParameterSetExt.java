package org.jcodec.codecs.h264.io.model;

import static org.jcodec.codecs.h264.io.read.CAVLCReader.readBool;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readTrailingBits;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readU;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readUE;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeTrailingBits;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jcodec.common.io.BitstreamReader;
import org.jcodec.common.io.BitstreamWriter;
import org.jcodec.common.io.InBits;
import org.jcodec.common.io.OutBits;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Sequence Parameter Set Extension entity of H264 bitstream
 * 
 * Capable to serialize / deserialize itself with CAVLC bit stream
 * 
 * @author Jay Codec
 * 
 */
public class SeqParameterSetExt extends BitstreamElement {

    public int seq_parameter_set_id;
    public int aux_format_idc;
    public int bit_depth_aux_minus8;
    public boolean alpha_incr_flag;
    public boolean additional_extension_flag;
    public int alpha_opaque_value;
    public int alpha_transparent_value;

    public static SeqParameterSetExt read(InputStream is) throws IOException {
        InBits in = new BitstreamReader(is);

        SeqParameterSetExt spse = new SeqParameterSetExt();
        spse.seq_parameter_set_id = readUE(in, "SPSE: seq_parameter_set_id");
        spse.aux_format_idc = readUE(in, "SPSE: aux_format_idc");
        if (spse.aux_format_idc != 0) {
            spse.bit_depth_aux_minus8 = readUE(in, "SPSE: bit_depth_aux_minus8");
            spse.alpha_incr_flag = readBool(in, "SPSE: alpha_incr_flag");
            spse.alpha_opaque_value = readU(in, spse.bit_depth_aux_minus8 + 9, "SPSE: alpha_opaque_value");
            spse.alpha_transparent_value = readU(in, spse.bit_depth_aux_minus8 + 9, "SPSE: alpha_transparent_value");
        }
        spse.additional_extension_flag = readBool(in, "SPSE: additional_extension_flag");

        readTrailingBits(in);

        return spse;
    }

    public void write(OutputStream out) throws IOException {
        OutBits writer = new BitstreamWriter(out);
        writeTrailingBits(writer);
    }
}