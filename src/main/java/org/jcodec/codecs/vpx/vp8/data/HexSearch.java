package org.jcodec.codecs.vpx.vp8.data;

import org.jcodec.codecs.vpx.vp8.FindNearMV;
import org.jcodec.codecs.vpx.vp8.MComp;
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
public class HexSearch {
    public static final MV[] hex = { new MV(-1, -2), new MV(1, -2), new MV(2, 0), new MV(1, 2), new MV(-1, 2),
            new MV(-2, 0) };
    public static final MV[] neighbors = { new MV(0, -1), new MV(-1, 0), new MV(1, 0), new MV(0, 1) };
    public static final MV[][] next_chkpts = { { new MV(-2, 0), new MV(-1, -2), new MV(1, -2) },
            { new MV(-1, -2), new MV(1, -2), new MV(2, 0) }, { new MV(1, -2), new MV(2, 0), new MV(1, 2) },
            { new MV(2, 0), new MV(1, 2), new MV(-1, 2) }, { new MV(1, 2), new MV(-1, 2), new MV(-2, 0) },
            { new MV(-1, 2), new MV(-2, 0), new MV(-1, -2) } };

    private int best_site;
    private long bestsad;
    private int in_what_stride;
    private int what_stride;
    private FullAccessIntArrPointer what;
    private FullAccessIntArrPointer base_offset;
    private Macroblock x;
    private boolean checkBounds;
    private VarianceFNs vfp;
    private MV fcenter_mv;
    private MV best;
    private ReadOnlyIntArrPointer[] mvsadcost;
    private MV this_mv = new MV();

    public long apply(Macroblock x, boolean costMVs, VarianceFNs vfp, MV ref_mv, MV center_mv, MV best_mv) {
        this.x = x;
        in_what_stride = x.e_mbd.pre.y_stride;
        Block b = x.block.get();
        what = x.block.get().getSrcPtr();
        base_offset = x.e_mbd.block.get().getOffsetPointer(x.e_mbd.pre.y_buffer);
        what_stride = b.src_stride;
        fcenter_mv = center_mv.div8();
        mvsadcost = costMVs ? x.mvsadcost : null;
        this.vfp = vfp;
        best = best_mv;
        bestsad = Long.MAX_VALUE;
        checkBounds = false;
        best_site = -1;
        /* adjust ref_mv to make sure it is within MV range */
        FindNearMV.vp8_clamp_mv(ref_mv, x.mv_col_min, x.mv_col_max, x.mv_row_min, x.mv_row_max);
        best.set(ref_mv);
        /* Work out the start point for the search */
        applySadForLoc(0, 0, -1);

        int k = -1;
        int hex_range = 127;
        int dia_range = 8;

        /* hex search */
        CHECK_BOUNDS(2);
        for (int i = 0; i < 6; ++i) {
            applySadForLoc(hex[i].row, hex[i].col, i);
        }
        if (best_site != -1) {
            best.add(hex[best_site]);
            k = best_site;

            for (int j = 1; j < hex_range; ++j) {
                best_site = -1;
                CHECK_BOUNDS(2);
                for (int i = 0; i < 3; ++i) {
                    applySadForLoc(next_chkpts[k][i].row, next_chkpts[k][i].col, i);
                }
                if (best_site == -1) {
                    break;
                } else {
                    best.add(next_chkpts[k][best_site]);
                    k += 5 + best_site;
                    if (k >= 12) {
                        k -= 12;
                    } else if (k >= 6) {
                        k -= 6;
                    }
                }
            }
        }
        /* check 4 1-away neighbors */
        for (int j = 0; j < dia_range; ++j) {
            best_site = -1;
            CHECK_BOUNDS(1);
            for (int i = 0; i < 4; ++i) {
                applySadForLoc(neighbors[i].row, neighbors[i].col, i);
            }
            if (best_site == -1) {
                break;
            } else {
                best.add(neighbors[best_site]);
            }
        }
        return bestsad;
    }

    private void applySadForLoc(int rowShift, int colShift, int i) {
        final int rowCalc = best.row + rowShift;
        final int colCalc = best.col + colShift;
        if (!checkBounds && (colCalc < x.mv_col_min || colCalc > x.mv_col_max || rowCalc < x.mv_row_min
                || rowCalc > x.mv_row_max))
            return;
        FullAccessIntArrPointer this_offset = base_offset.shallowCopyWithPosInc(rowCalc * in_what_stride + colCalc);
        long thissad = vfp.sdf.call(what, what_stride, this_offset, in_what_stride);
        if (thissad < bestsad) {
            this_mv.setRC(rowCalc, colCalc);
            thissad += MComp.mvsad_err_cost(this_mv, fcenter_mv, mvsadcost, x.sadperbit16);
            if (thissad < bestsad) {
                bestsad = thissad;
                best_site = i;
            }
        }
    }

    private void CHECK_BOUNDS(final int range) {
        checkBounds = ((best.row - range) >= x.mv_row_min) && ((best.row + range) <= x.mv_row_max)
                && ((best.col - range) >= x.mv_col_min) && ((best.col + range) <= x.mv_col_max);
    }

}