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
public enum AlgoFlags {
    /*
     * !\brief Don't reference the last frame
     *
     * When this flag is set, the encoder will not use the last frame as a
     * predictor. When not set, the encoder will choose whether to use the last
     * frame or not automatically.
     */

    NO_REF_LAST,

    /*
     * !\brief Don't reference the golden frame
     *
     * When this flag is set, the encoder will not use the golden frame as a
     * predictor. When not set, the encoder will choose whether to use the golden
     * frame or not automatically.
     */
    NO_REF_GF,

    /*
     * !\brief Don't reference the alternate reference frame
     *
     * When this flag is set, the encoder will not use the alt ref frame as a
     * predictor. When not set, the encoder will choose whether to use the alt ref
     * frame or not automatically.
     */
    NO_REF_ARF,

    /*
     * !\brief Don't update the last frame
     *
     * When this flag is set, the encoder will not update the last frame with the
     * contents of the current frame.
     */
    NO_UPD_LAST,

    /*
     * !\brief Don't update the golden frame
     *
     * When this flag is set, the encoder will not update the golden frame with the
     * contents of the current frame.
     */
    NO_UPD_GF,

    /*
     * !\brief Don't update the alternate reference frame
     *
     * When this flag is set, the encoder will not update the alt ref frame with the
     * contents of the current frame.
     */
    NO_UPD_ARF,

    /*
     * !\brief Force golden frame update
     *
     * When this flag is set, the encoder copy the contents of the current frame to
     * the golden frame buffer.
     */
    FORCE_GF,

    /*
     * !\brief Force alternate reference frame update
     *
     * When this flag is set, the encoder copy the contents of the current frame to
     * the alternate reference frame buffer.
     */
    FORCE_ARF,

    /*
     * !\brief Disable entropy update
     *
     * When this flag is set, the encoder will not update its internal entropy model
     * based on the entropy of this frame.
     */
    NO_UPD_ENTROPY,
    /** < Force this frame to be a keyframe */
    FORCE_KF

}
