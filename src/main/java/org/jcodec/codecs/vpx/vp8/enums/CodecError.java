package org.jcodec.codecs.vpx.vp8.enums;

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
public enum CodecError {
    /*
     * !\brief The given bitstream is not supported.
     *
     * The bitstream was unable to be parsed at the highest level. The decoder is
     * unable to proceed. This error \ref SHOULD be treated as fatal to the stream.
     */
    UNSUP_BITSTREAM,

    /*
     * !\brief Encoded bitstream uses an unsupported feature
     *
     * The decoder does not implement a feature required by the encoder. This return
     * code should only be used for features that prevent future pictures from being
     * properly decoded. This error \ref MAY be treated as fatal to the stream or
     * \ref MAY be treated as fatal to the current GOP.
     */
    UNSUP_FEATURE,

    /*
     * !\brief The coded data for this stream is corrupt or incomplete
     *
     * There was a problem decoding the current frame. This return code should only
     * be used for failures that prevent future pictures from being properly
     * decoded. This error \ref MAY be treated as fatal to the stream or \ref MAY be
     * treated as fatal to the current GOP. If decoding is continued for the current
     * GOP, artifacts may be present.
     */
    CORRUPT_FRAME,

    /*
     * !\brief An application-supplied parameter is not valid.
     *
     */
    INVALID_PARAM

}
