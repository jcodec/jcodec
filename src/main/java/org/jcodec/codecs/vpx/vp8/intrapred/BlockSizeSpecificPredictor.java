package org.jcodec.codecs.vpx.vp8.intrapred;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License.
 * 
 * The class is a direct java port of libvpx's
 * (https://github.com/webmproject/libvpx) relevant VP8 code with significant
 * java oriented refactoring.
 * 
 * @author The JCodec project
 * 
 */
public abstract class BlockSizeSpecificPredictor implements IntraPredFN {
    public final int bs;

    public BlockSizeSpecificPredictor(int bs) {
        this.bs = bs;
    }
}
