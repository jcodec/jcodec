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
public class TokenValue {
    public final TokenAlphabet token;
    public final int extra;

    public TokenValue(TokenAlphabet token, int extra) {
        this.token = token;
        this.extra = extra;
    }
}
