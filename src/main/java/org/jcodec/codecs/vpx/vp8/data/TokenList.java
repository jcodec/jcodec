package org.jcodec.codecs.vpx.vp8.data;

import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessGenArrPointer;

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
public class TokenList {
    public FullAccessGenArrPointer<TokenExtra> start;
    public FullAccessGenArrPointer<TokenExtra> stop;
}
