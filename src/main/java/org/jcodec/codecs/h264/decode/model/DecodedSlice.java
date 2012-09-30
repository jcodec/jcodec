package org.jcodec.codecs.h264.decode.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class DecodedSlice {

    private DecodedMBlock[] mblocks;

    public DecodedSlice(DecodedMBlock[] mblocks) {
        super();
        this.mblocks = mblocks;
    }

    public DecodedMBlock[] getMblocks() {
        return mblocks;
    }

}
