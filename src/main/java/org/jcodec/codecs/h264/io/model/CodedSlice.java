package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Slice H264 bitstream entity
 * 
 * capable to serialize and deserialize with CAVLC bitstream
 * 
 * @author Jay Codec
 * 
 */
public class CodedSlice {
    private SliceHeader header;
    private Macroblock[] mblocks;

    public CodedSlice(SliceHeader header, Macroblock[] mblocks) {
        this.header = header;
        this.mblocks = mblocks;
    }

    public SliceHeader getHeader() {
        return header;
    }

    public Macroblock[] getMblocks() {
        return mblocks;
    }
}