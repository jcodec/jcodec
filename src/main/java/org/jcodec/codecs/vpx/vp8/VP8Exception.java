package org.jcodec.codecs.vpx.vp8;

import org.jcodec.codecs.vpx.vp8.enums.CodecError;

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
public class VP8Exception extends RuntimeException {
    private static final long serialVersionUID = 3348410786441217867L;
    public final CodecError err;
    public final boolean has_detail;

    public VP8Exception(String message, CodecError type) {
        super(message);
        has_detail = message != null;
        err = type;
    }

    public VP8Exception(CodecError type) {
        super();
        has_detail = false;
        err = type;
    }

    public static void ERROR(String str) {
        throw new VP8Exception(str, CodecError.INVALID_PARAM);
    }

    public static void vp8_internal_error(CodecError error, String fmt, Object... rest) {
        if (fmt != null) {
            throw new VP8Exception(String.format(fmt, rest), error);
        } else {
            throw new VP8Exception(error);
        }
    
    }
}