package org.jcodec.codecs.h264.decode.model;

import org.jcodec.codecs.h264.io.model.Vector;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Decoded motion vectors used in the deblocking filter
 * 
 * 
 * @author Jay Codec
 * 
 */
public class MVMatrix {

    private Vector[] vectors;

    public MVMatrix(Vector[] vectors) {
        this.vectors = vectors;
    }

    public Vector[] getVectors() {
        return vectors;
    }
}
