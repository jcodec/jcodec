package org.jcodec.codecs.prores;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * Codebook for ProRes codes
 * 
 * @author The JCodec project
 *
 */
public class Codebook {
    int riceOrder;
    int expOrder;
    int switchBits;

    public Codebook(int riceOrder, int expOrder, int switchBits) {
        this.riceOrder = riceOrder;
        this.expOrder = expOrder;
        this.switchBits = switchBits;
    }
}
