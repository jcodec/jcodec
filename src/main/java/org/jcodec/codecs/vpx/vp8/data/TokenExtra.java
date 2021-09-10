package org.jcodec.codecs.vpx.vp8.data;

import org.jcodec.codecs.vpx.vp8.enums.TokenAlphabet;

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
public class TokenExtra {
    public short[] context_tree;
    public int Extra;
    public TokenAlphabet Token; // uchar
    public boolean skip_eob_node;

}
