package org.jcodec.codecs.h264.io.model;

import java.util.Collection;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class CodedPicture {
    private Collection<CodedSlice> slices;

    public CodedPicture(Collection<CodedSlice> slices) {
        this.slices = slices;
    }

    public Collection<CodedSlice> getSlices() {
        return slices;
    }
}