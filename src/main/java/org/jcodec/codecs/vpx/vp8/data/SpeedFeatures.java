package org.jcodec.codecs.vpx.vp8.data;

import org.jcodec.codecs.vpx.vp8.enums.SearchMethods;

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
public class SpeedFeatures {
    public boolean RD;
    public SearchMethods search_method;
    public boolean improved_quant;
    public boolean improved_dct;
    public boolean auto_filter;
    public int recode_loop; //up to 2
    public boolean iterative_sub_pixel;
    public boolean half_pixel_search;
    public boolean quarter_pixel_search;
    public int[] thresh_mult = new int[Block.MAX_MODES];
    public int max_step_search_steps;
    public int first_step;
    public boolean optimize_coefficients;

    public boolean use_fastquant_for_pick;
    public boolean no_skip_block4x4_search;
    public boolean improved_mv_pred;
}
