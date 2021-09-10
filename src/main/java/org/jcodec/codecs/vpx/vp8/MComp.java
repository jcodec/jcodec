package org.jcodec.codecs.vpx.vp8;

import org.jcodec.codecs.vpx.vp8.data.Block;
import org.jcodec.codecs.vpx.vp8.data.BlockD;
import org.jcodec.codecs.vpx.vp8.data.EntropyMV;
import org.jcodec.codecs.vpx.vp8.data.MV;
import org.jcodec.codecs.vpx.vp8.data.Macroblock;
import org.jcodec.codecs.vpx.vp8.data.MacroblockD;
import org.jcodec.codecs.vpx.vp8.data.SearchSite;
import org.jcodec.codecs.vpx.vp8.data.Compressor;
import org.jcodec.codecs.vpx.vp8.data.VarWithNum;
import org.jcodec.codecs.vpx.vp8.data.VarianceFNs;
import org.jcodec.codecs.vpx.vp8.data.VarianceResults;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessGenArrPointer;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;
import org.jcodec.codecs.vpx.vp8.pointerhelper.ReadOnlyIntArrPointer;

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
public class MComp {
    public static Compressor.FractionalMVStepIF vp8_find_best_sub_pixel_step_iteratively = new Compressor.FractionalMVStepIF() {
        public long call(Macroblock x, Block b, BlockD d, MV bestmv, MV ref_mv, int error_per_bit, VarianceFNs vfp,
                ReadOnlyIntArrPointer[] mvcost, VarianceResults ret) {
            return find_best_sub_pixel_step_iteratively(x, b, d, bestmv, ref_mv, error_per_bit, vfp, mvcost, ret);
        };
    };
    public static Compressor.FractionalMVStepIF vp8_find_best_sub_pixel_step = new Compressor.FractionalMVStepIF() {
        public long call(Macroblock x, Block b, BlockD d, MV bestmv, MV ref_mv, int error_per_bit, VarianceFNs vfp,
                ReadOnlyIntArrPointer[] mvcost, VarianceResults ret) {
            return find_best_sub_pixel_step(x, b, d, bestmv, ref_mv, error_per_bit, vfp, mvcost, ret, true);
        };
    };
    public static Compressor.FractionalMVStepIF vp8_find_best_half_pixel_step = new Compressor.FractionalMVStepIF() {
        public long call(Macroblock x, Block b, BlockD d, MV bestmv, MV ref_mv, int error_per_bit, VarianceFNs vfp,
                ReadOnlyIntArrPointer[] mvcost, VarianceResults ret) {
            return find_best_sub_pixel_step(x, b, d, bestmv, ref_mv, error_per_bit, vfp, mvcost, ret, false);
        };
    };
    public static Compressor.FractionalMVStepIF vp8_skip_fractional_mv_step = new Compressor.FractionalMVStepIF() {
        public long call(Macroblock x, Block b, BlockD d, MV bestmv, MV ref_mv, int error_per_bit, VarianceFNs vfp,
                ReadOnlyIntArrPointer[] mvcost, VarianceResults ret) {
            bestmv.set(bestmv.mul8());
            return 0;
        };
    };

    public static short MAX_MVSEARCH_STEPS = 8;
    public static short MAX_FULL_PEL_VAL = (short) ((1 << (MAX_MVSEARCH_STEPS)) - 1);
    public static short MAX_FIRST_STEP = (short) (1 << (MAX_MVSEARCH_STEPS - 1));

    static int vp8_mv_bit_cost(MV mv, MV ref, ReadOnlyIntArrPointer[] mvcost, int Weight) {
        /*
         * MV costing is based on the distribution of vectors in the previous frame and
         * as such will tend to over state the cost of vectors. In addition coding a new
         * vector can have a knock on effect on the cost of subsequent vectors and the
         * quality of prediction from NEAR and NEAREST for subsequent blocks. The
         * "Weight" parameter allows, to a limited extent, for some account to be taken
         * of these factors.
         */
        final MV mv_idx = getRelCoords(mv, ref);
        return ((mvcost[0].getRel(mv_idx.row) + mvcost[1].getRel(mv_idx.col)) * Weight) >> 7;
    }

    static int mv_err_cost(MV mv, MV ref, ReadOnlyIntArrPointer[] mvcost, int error_per_bit) {
        final MV mv_idx = getRelCoords(mv, ref);
        return mv_err_cost(mv_idx.row, mv_idx.col, mvcost, error_per_bit);
    }

    private static MV getRelCoords(MV mv, MV ref) {
        return new MV(CommonUtils.clamp((short) ((mv.row - ref.row) >> 1), (short) 0, EntropyMV.MVvals),
                CommonUtils.clamp((short) ((mv.col - ref.col) >> 1), (short) 0, EntropyMV.MVvals));
    }

    static int mv_err_cost(int r, int c, ReadOnlyIntArrPointer[] mvcost, int error_per_bit) {
        /* Ignore mv costing if mvcost is NULL */
        if (mvcost != null) {
            return ((mvcost[0].getRel(r) + mvcost[1].getRel(c)) * error_per_bit + 128) >> 8;
        }
        return 0;
    }

    public static int mvsad_err_cost(MV mv, MV ref, ReadOnlyIntArrPointer[] mvsadcost, int error_per_bit) {
        /* Calculate sad error cost on full pixel basis. */
        return mv_err_cost(mv.row - ref.row, mv.col - ref.col, mvsadcost, error_per_bit);
    }

    static long find_best_sub_pixel_step_iteratively(Macroblock x, Block b, BlockD d, MV bestmv, MV ref_mv,
            int error_per_bit, VarianceFNs vfp, ReadOnlyIntArrPointer[] mvcost, VarianceResults ret) {
        SearchForBetterMV cb = new SearchForBetterMV();
        cb.z = b.getSrcPtr();

        cb.rr = ref_mv.row >> 1;
        cb.rc = ref_mv.col >> 1;
        cb.br = (short) (bestmv.row << 2);
        cb.bc = (short) (bestmv.col << 2);
        cb.moveToBest();

        cb.minc = Math.max(x.mv_col_min * 4, (ref_mv.col >> 1) - ((1 << EntropyMV.mvlong_width) - 1));
        cb.maxc = Math.min(x.mv_col_max * 4, (ref_mv.col >> 1) + ((1 << EntropyMV.mvlong_width) - 1));
        cb.minr = Math.max(x.mv_row_min * 4, (ref_mv.row >> 1) - ((1 << EntropyMV.mvlong_width) - 1));
        cb.maxr = Math.min(x.mv_row_max * 4, (ref_mv.row >> 1) + ((1 << EntropyMV.mvlong_width) - 1));

        cb.error_per_bit = error_per_bit;
        cb.vfp = vfp;
        cb.mvcost = mvcost;
        cb.b = b;
        cb.sse1.variance = ret.variance;
        cb.sse1.sse = ret.sse;

        int pre_stride = x.e_mbd.pre.y_stride;

        MacroblockD xd = x.e_mbd;
        /* Clamping to avoid out-of-range data access */
        int buf_r1, buf_r2, buf_c1;
        buf_r1 = ((bestmv.row - 3) < x.mv_row_min) ? (bestmv.row - x.mv_row_min) : 3;
        buf_r2 = ((bestmv.row + 3) > x.mv_row_max) ? (x.mv_row_max - bestmv.row) : 3;
        buf_c1 = ((bestmv.col - 3) < x.mv_col_min) ? (bestmv.col - x.mv_col_min) : 3;
        FullAccessIntArrPointer y_0 = d.getOffsetPointer(x.e_mbd.pre.y_buffer)
                .shallowCopyWithPosInc((bestmv.row) * pre_stride + bestmv.col - buf_c1 - pre_stride * buf_r1);

        /* Copy to intermediate buffer before searching. */
        vfp.copymem.call(y_0, pre_stride, xd.y_buf, cb.y_stride, 16 + buf_r1 + buf_r2);
        cb.y = xd.y_buf.shallowCopyWithPosInc(cb.y_stride * buf_r1 + buf_c1);

        cb.offset = (bestmv.row) * cb.y_stride + bestmv.col;

        /* central mv */
        bestmv.set(bestmv.mul8());

        /* calculate central point error */
        vfp.vf.call(cb.y, cb.y_stride, cb.z, b.src_stride, cb.sse1);
        cb.besterr = cb.sse1.variance;
        ret.variance = cb.besterr;
        cb.besterr += mv_err_cost(bestmv, ref_mv, mvcost, error_per_bit);

        // cb setup is now complete, let's see the sub pixel stepping

        cb.lookAround();

        // the results are in, save them

        bestmv.row = (short) (cb.br << 1);
        bestmv.col = (short) (cb.bc << 1);

        if ((Math.abs(bestmv.col - ref_mv.col) > (MAX_FULL_PEL_VAL << 3))
                || (Math.abs(bestmv.row - ref_mv.row) > (MAX_FULL_PEL_VAL << 3))) {
            return Integer.MAX_VALUE;
        }

        ret.sse = cb.sse1.sse;
        ret.variance = cb.sse1.variance;

        return cb.besterr;
    }

    static long find_best_sub_pixel_step(Macroblock x, final Block b, BlockD d, MV bestmv, final MV ref_mv,
            final int error_per_bit, final VarianceFNs vfp, final ReadOnlyIntArrPointer[] mvcost, VarianceResults ret,
            boolean doquarter) {
        final SearchForBetterMV cb = new SearchForBetterMV();
        cb.z = b.getSrcPtr();
        cb.error_per_bit = error_per_bit;
        cb.vfp = vfp;
        cb.b = b;
        cb.sse1.variance = ret.variance;
        cb.sse1.sse = ret.sse;

        MV startmv = new MV();
        int pre_stride = x.e_mbd.pre.y_stride;

        MacroblockD xd = x.e_mbd;
        FullAccessIntArrPointer y_0 = d.getOffsetPointer(x.e_mbd.pre.y_buffer)
                .shallowCopyWithPosInc((bestmv.row) * pre_stride + bestmv.col);

        /* Copy 18 rows x 32 cols area to intermediate buffer before searching. */
        vfp.copymem.call(y_0.shallowCopyWithPosInc(-1 - pre_stride), pre_stride, xd.y_buf, cb.y_stride, 18);
        cb.y = xd.y_buf.shallowCopyWithPosInc(cb.y_stride + 1);

        /* central mv */
        bestmv.set(bestmv.mul8());

        startmv.set(bestmv);

        /* calculate central point error */
        vfp.vf.call(cb.y, cb.y_stride, cb.z, b.src_stride, cb.sse1);
        cb.besterr = cb.sse1.variance;
        ret.variance = cb.besterr;
        cb.besterr += mv_err_cost(bestmv, ref_mv, mvcost, error_per_bit);
        cb.bc = bestmv.col;
        cb.br = bestmv.row;
        cb.rr = ref_mv.row;
        cb.rc = ref_mv.col;

        class DirSpec extends MV {
            FullAccessIntArrPointer y;
            int xoff, yoff;
            long res = Long.MAX_VALUE;

            public DirSpec(int r, int c, int ypshift, int xo, int yo) {
                super(r, c);
                y = cb.y.shallowCopyWithPosInc(ypshift);
                if (xo == -1) {
                    xoff = c & 7;
                } else {
                    xoff = xo;
                }
                if (yo == -1) {
                    yoff = r & 7;
                } else {
                    yoff = yo;
                }
            }

            void dircheck() {
                cb.actualCheck(y, xoff, yoff, row, col);
            }
        }

        DirSpec[] basedirs = new DirSpec[] { new DirSpec(startmv.row, (startmv.col - 8) | 4, -1, 4, 0), // Left
                new DirSpec(startmv.row, ((startmv.col - 8) | 4) + 8, 0, 4, 0), // Right
                new DirSpec((startmv.row - 8) | 4, startmv.col, -cb.y_stride, 0, 4), // Up
                new DirSpec(((startmv.row - 8) | 4) + 8, startmv.col, 0, 0, 4), // Down
        };
        DirSpec[] diags = new DirSpec[] {
                new DirSpec((startmv.row - 8) | 4, (startmv.col - 8) | 4, -1 - cb.y_stride, 4, 4), // up
                                                                                                   // left
                new DirSpec((startmv.row - 8) | 4, startmv.col + 4, -cb.y_stride, 4, 4), // up right
                new DirSpec(startmv.row + 4, (startmv.col - 8) | 4, -1, 4, 4), // down left
                new DirSpec(startmv.row + 4, startmv.col + 4, 0, 4, 4), // down right
        };

        for (DirSpec dir : basedirs) {
            /* "halfpix" horizontal variance */
            dir.dircheck();
        }

        /* now check 1 more diagonal */
        int whichdir = (basedirs[0].res < basedirs[1].res ? 0 : 1) + (basedirs[2].res < basedirs[3].res ? 0 : 2);
        diags[whichdir].dircheck();

        if (doquarter) {

            /* time to check quarter pels. */
            if (bestmv.row < startmv.row)
                cb.y.incBy(-cb.y_stride);

            if (bestmv.col < startmv.col)
                cb.y.dec();

            startmv.set(bestmv);

            if ((startmv.col & 7) != 0) { // left
                basedirs[0] = new DirSpec(startmv.row, startmv.col - 2, 0, -1, -1);
            } else {
                basedirs[0] = new DirSpec(startmv.row, (startmv.col - 8) | 6, -1, 6, -1);
            }

            // right
            basedirs[1] = new DirSpec(basedirs[0].row, basedirs[0].col + 4, 0, -1, -1);

            /* go up then down and check error */
            if ((startmv.row & 7) != 0) { // up
                basedirs[2] = new DirSpec(startmv.row - 2, startmv.col, 0, -1, -1);
            } else {
                basedirs[2] = new DirSpec((startmv.row - 8) | 6, startmv.col, -cb.y_stride, -1, 6);
            }

            basedirs[3] = new DirSpec(basedirs[2].row + 4, basedirs[0].col, 0, -1, -1);

            for (DirSpec dir : basedirs) {
                /* "quarterpix" horizontal variance */
                dir.dircheck();
            }

            /* now check 1 more diagonal */
            whichdir = (basedirs[0].res < basedirs[1].res ? 0 : 1) + (basedirs[2].res < basedirs[3].res ? 0 : 2);

            switch (whichdir) {
            case 0:

                if ((startmv.row & 7) != 0) {
                    if ((startmv.col & 7) != 0) {
                        diags[0] = new DirSpec(startmv.row - 2, startmv.col - 2, 0, -1, -1);
                    } else {
                        diags[0] = new DirSpec(startmv.row - 2, (startmv.col - 8) | 6, -1, 6, -1);
                    }
                } else {
                    if ((startmv.col & 7) != 0) {
                        diags[0] = new DirSpec((startmv.row - 8) | 6, startmv.col - 2, -cb.y_stride, -1, 6);
                    } else {
                        diags[0] = new DirSpec((startmv.row - 8) | 6, (startmv.col - 8) | 6, -1 - cb.y_stride, 6, 6);
                    }
                }

                break;
            case 1:

                if ((startmv.row & 7) != 0) {
                    diags[0] = new DirSpec(startmv.row - 2, startmv.col + 2, 0, -1, -1);
                } else {
                    diags[0] = new DirSpec((startmv.row - 8) | 6, startmv.col + 2, -cb.y_stride, -1, 6);
                }

                break;
            case 2:

                if ((startmv.col & 7) != 0) {
                    diags[0] = new DirSpec(startmv.row + 2, startmv.col - 2, 0, -1, -1);
                } else {
                    diags[0] = new DirSpec(startmv.row + 2, (startmv.col - 8) | 6, -1, 6, -1);
                }

                break;
            case 3:
                diags[0] = new DirSpec(startmv.row + 2, startmv.col + 2, 0, -1, -1);
                break;
            }

            diags[0].dircheck();
        }
        bestmv.row = cb.br;
        bestmv.col = cb.bc;

        ret.variance = cb.sse1.variance;
        ret.sse = cb.sse1.sse;

        return cb.besterr;
    }

    public static void vp8_diamond_search_sad(Macroblock x, Block b, BlockD d, MV ref_mv, MV best_mv, int search_param,
            int sad_per_bit, VarWithNum ret, VarianceFNs fn_ptr, ReadOnlyIntArrPointer[] mvcost, MV center_mv) {
        int i, j, step;
        FullAccessIntArrPointer what = b.getSrcPtr();
        int what_stride = b.src_stride;
        FullAccessIntArrPointer in_what;
        int pre_stride = x.e_mbd.pre.y_stride;
        FullAccessIntArrPointer base_pre = x.e_mbd.pre.y_buffer;
        int in_what_stride = pre_stride;
        FullAccessIntArrPointer best_address;

        int tot_steps;
        MV this_mv = new MV();

        long bestsad;
        long thissad;
        int best_site = 0;
        int last_site = 0;

        int ref_row;
        int ref_col;
        short this_row_offset;
        short this_col_offset;
        FullAccessGenArrPointer<SearchSite> ss;

        FullAccessIntArrPointer check_here;

        MV fcenter_mv = center_mv.div8();

        FindNearMV.vp8_clamp_mv(ref_mv, x.mv_col_min, x.mv_col_max, x.mv_row_min, x.mv_row_max);
        ref_row = ref_mv.row;
        ref_col = ref_mv.col;
        ret.num00 = 0;
        best_mv.set(ref_mv);

        /* Work out the start point for the search */
        in_what = d.getOffsetPointer(base_pre).shallowCopyWithPosInc((ref_row * pre_stride) + ref_col);
        best_address = in_what;

        /* Check the starting position */
        bestsad = fn_ptr.sdf.call(what, what_stride, in_what, in_what_stride)
                + mvsad_err_cost(best_mv, fcenter_mv, x.mvsadcost, sad_per_bit);

        /*
         * search_param determines the length of the initial step and hence the number
         * of iterations 0 = initial step (MAX_FIRST_STEP) pel : 1 = (MAX_FIRST_STEP/2)
         * pel, 2 = (MAX_FIRST_STEP/4) pel... etc.
         */
        ss = x.ss.shallowCopyWithPosInc(search_param * x.searches_per_step);
        tot_steps = (x.ss_count / x.searches_per_step) - search_param;

        i = 1;

        for (step = 0; step < tot_steps; ++step) {
            for (j = 0; j < x.searches_per_step; ++j) {
                /* Trap illegal vectors */
                this_row_offset = (short) (best_mv.row + ss.getRel(i).mv.row);
                this_col_offset = (short) (best_mv.col + ss.getRel(i).mv.col);

                if ((this_col_offset > x.mv_col_min) && (this_col_offset < x.mv_col_max)
                        && (this_row_offset > x.mv_row_min) && (this_row_offset < x.mv_row_max))

                {
                    check_here = best_address.shallowCopyWithPosInc(ss.getRel(i).offset);
                    thissad = fn_ptr.sdf.call(what, what_stride, check_here, in_what_stride);

                    if (thissad < bestsad) {
                        this_mv.row = this_row_offset;
                        this_mv.col = this_col_offset;
                        thissad += mvsad_err_cost(this_mv, fcenter_mv, x.mvsadcost, sad_per_bit);

                        if (thissad < bestsad) {
                            bestsad = thissad;
                            best_site = i;
                        }
                    }
                }

                i++;
            }

            if (best_site != last_site) {
                best_mv.set(best_mv.add(ss.getRel(best_site).mv));
                best_address.incBy(ss.getRel(best_site).offset);
                last_site = best_site;
            } else if (best_address == in_what) {
                ret.num00++;
            }
        }

        this_mv.set(best_mv.mul8());
        VarianceResults vr = new VarianceResults();
        fn_ptr.vf.call(what, what_stride, best_address, in_what_stride, vr);
        ret.var = vr.variance + mv_err_cost(this_mv, center_mv, mvcost, x.errorperbit);
    }

    public static long vp8_full_search_sad(Macroblock x, Block b, BlockD d, MV ref_mv, int sad_per_bit, int distance,
            VarianceFNs fn_ptr, ReadOnlyIntArrPointer[] mvcost, MV center_mv) {
        FullAccessIntArrPointer what = b.getSrcPtr();
        int what_stride = b.src_stride;
        FullAccessIntArrPointer in_what;
        int pre_stride = x.e_mbd.pre.y_stride;
        int in_what_stride = pre_stride;
        int mv_stride = pre_stride;
        FullAccessIntArrPointer bestaddress;
        MV best_mv = d.bmi.mv.copy();
        MV this_mv = new MV();
        long bestsad;
        long thissad;
        short r, c;

        FullAccessIntArrPointer check_here;

        int ref_row = ref_mv.row;
        int ref_col = ref_mv.col;
        best_mv.set(ref_mv);

        short row_min = (short) (ref_row - distance);
        short row_max = (short) (ref_row + distance);
        short col_min = (short) (ref_col - distance);
        short col_max = (short) (ref_col + distance);

        ReadOnlyIntArrPointer[] mvsadcost = new ReadOnlyIntArrPointer[2];
        MV fcenter_mv = center_mv.div8();

        mvsadcost[0] = x.mvsadcost[0];
        mvsadcost[1] = x.mvsadcost[1];

        /* Work out the mid point for the search */
        in_what = d.getOffsetPointer(x.e_mbd.pre.y_buffer);
        bestaddress = in_what.shallowCopyWithPosInc((ref_row * pre_stride) + ref_col);

        /* Baseline value at the centre */
        bestsad = fn_ptr.sdf.call(what, what_stride, bestaddress, in_what_stride)
                + mvsad_err_cost(best_mv, fcenter_mv, mvsadcost, sad_per_bit);

        /*
         * Apply further limits to prevent us looking using vectors that stretch beyiond
         * the UMV border
         */
        if (col_min < x.mv_col_min)
            col_min = x.mv_col_min;

        if (col_max > x.mv_col_max)
            col_max = x.mv_col_max;

        if (row_min < x.mv_row_min)
            row_min = x.mv_row_min;

        if (row_max > x.mv_row_max)
            row_max = x.mv_row_max;

        for (r = row_min; r < row_max; ++r) {
            this_mv.row = r;
            check_here = in_what.shallowCopyWithPosInc(r * mv_stride + col_min);

            for (c = col_min; c < col_max; ++c) {
                thissad = fn_ptr.sdf.call(what, what_stride, check_here, in_what_stride);

                this_mv.col = c;
                thissad += mvsad_err_cost(this_mv, fcenter_mv, mvsadcost, sad_per_bit);

                if (thissad < bestsad) {
                    bestsad = thissad;
                    best_mv.row = r;
                    best_mv.col = c;
                    bestaddress = check_here.shallowCopy();
                }

                check_here.inc();
            }
        }

        this_mv.set(best_mv.mul8());
        VarianceResults vr = new VarianceResults();
        fn_ptr.vf.call(what, what_stride, bestaddress, in_what_stride, vr);
        return vr.variance + mv_err_cost(this_mv, center_mv, mvcost, x.errorperbit);
    }

    public static long vp8_refining_search_sad(Macroblock x, Block b, BlockD d, MV ref_mv, int error_per_bit,
            int search_range, VarianceFNs fn_ptr, ReadOnlyIntArrPointer[] mvcost, MV center_mv) {
        MV[] neighbors = { new MV(-1, 0), new MV(0, -1), new MV(0, 1), new MV(1, 0) };
        int i, j;
        short this_row_offset, this_col_offset;

        int what_stride = b.src_stride;
        int pre_stride = x.e_mbd.pre.y_stride;
        int in_what_stride = pre_stride;
        FullAccessIntArrPointer what = b.getSrcPtr();
        FullAccessIntArrPointer best_address = d.getOffsetPointer(x.e_mbd.pre.y_buffer)
                .shallowCopyWithPosInc((ref_mv.row * pre_stride) + ref_mv.col);
        FullAccessIntArrPointer check_here;
        MV this_mv = new MV();
        long bestsad;
        long thissad;

        ReadOnlyIntArrPointer[] mvsadcost = new ReadOnlyIntArrPointer[2];
        MV fcenter_mv = center_mv.div8();

        mvsadcost[0] = x.mvsadcost[0];
        mvsadcost[1] = x.mvsadcost[1];

        bestsad = fn_ptr.sdf.call(what, what_stride, best_address, in_what_stride)
                + mvsad_err_cost(ref_mv, fcenter_mv, mvsadcost, error_per_bit);

        for (i = 0; i < search_range; ++i) {
            int best_site = -1;

            for (j = 0; j < 4; ++j) {
                this_row_offset = (short) (ref_mv.row + neighbors[j].row);
                this_col_offset = (short) (ref_mv.col + neighbors[j].col);

                if ((this_col_offset > x.mv_col_min) && (this_col_offset < x.mv_col_max)
                        && (this_row_offset > x.mv_row_min) && (this_row_offset < x.mv_row_max)) {
                    check_here = best_address
                            .shallowCopyWithPosInc((neighbors[j].row) * in_what_stride + neighbors[j].col);
                    thissad = fn_ptr.sdf.call(what, what_stride, check_here, in_what_stride);

                    if (thissad < bestsad) {
                        this_mv.row = this_row_offset;
                        this_mv.col = this_col_offset;
                        thissad += mvsad_err_cost(this_mv, fcenter_mv, mvsadcost, error_per_bit);

                        if (thissad < bestsad) {
                            bestsad = thissad;
                            best_site = j;
                        }
                    }
                }
            }

            if (best_site == -1) {
                break;
            } else {
                ref_mv.set(ref_mv.add(neighbors[best_site]));
                best_address.incBy((neighbors[best_site].row) * in_what_stride + neighbors[best_site].col);
            }
        }

        this_mv.set(ref_mv.mul8());
        VarianceResults vr = new VarianceResults();
        fn_ptr.vf.call(what, what_stride, best_address, in_what_stride, vr);
        return vr.variance + mv_err_cost(this_mv, center_mv, mvcost, x.errorperbit);
    }

}
