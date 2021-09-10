package org.jcodec.codecs.vpx.vp8.data;

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
public class EntropyMV {
    public static final short mv_max = 1023, /* max absolute value of a MV component */
            MVvals = (2 * mv_max) + 1, /* # possible values "" */
            mvfp_max = 255, /* max absolute value of a full pixel MV component */
            MVfpvals = (2 * mvfp_max) + 1, /* # possible full pixel MV values */

            mvlong_width = 10, /* Large MVs have 9 bit magnitudes */
            mvnum_short = 8, /* magnitudes 0 through 7 */

            /* probability offsets for coding each MV component */

            mvpis_short = 0, /* short (<= 7) vs long (>= 8) */
            MVPsign = 1, /* sign for non-zero */
            MVPshort = 2, /* 8 short values = 7-position tree */

            MVPbits = MVPshort + mvnum_short - 1, /* mvlong_width long value bits */
            MVPcount = MVPbits + mvlong_width; /* (with independent probabilities) */
}
