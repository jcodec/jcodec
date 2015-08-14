package org.jcodec.codecs.h264.io.model;

import org.jcodec.common.tools.ToJSON;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Reference picture marking used for IDR frames
 * 
 * @author Jay Codec
 * 
 */
public class RefPicMarkingIDR {
    boolean discardDecodedPics;
    boolean useForlongTerm;

    public RefPicMarkingIDR(boolean discardDecodedPics, boolean useForlongTerm) {
        this.discardDecodedPics = discardDecodedPics;
        this.useForlongTerm = useForlongTerm;
    }

    public boolean isDiscardDecodedPics() {
        return discardDecodedPics;
    }

    public boolean isUseForlongTerm() {
        return useForlongTerm;
    }
    
    @Override
    public String toString() {
        return ToJSON.toJSON(this);
    }
}
