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
public class TokenState {
    public int rate;
    public int error;
    public int next;
    public TokenAlphabet token;
    public int qc;

    public TokenState(int r, int e, int n, TokenAlphabet t, int q) {
        rate = r;
        error = e;
        next = n;
        token = t;
        qc = q;
    }

    public TokenState(TokenState other) {
        rate = other.rate;
        error = other.error;
        next = other.next;
        token = other.token;
        qc = other.qc;
    }
}
