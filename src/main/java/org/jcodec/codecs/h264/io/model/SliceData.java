package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Slice Data H264 bitstream entity
 * 
 * capable to serialize and deserialize with CAVLC bitstream
 * 
 * @author Jay Codec
 * 
 */
public class SliceData {

    public boolean mb_field_decoding_flag;
    public Macroblock[] mblocks;

}