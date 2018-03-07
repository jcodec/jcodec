package org.jcodec.codecs.vpx;
import static java.lang.Math.abs;
import static org.jcodec.codecs.vpx.FilterUtil.Segment.horizontal;
import static org.jcodec.codecs.vpx.FilterUtil.Segment.vertical;

import org.jcodec.api.NotImplementedException;
import org.jcodec.codecs.vpx.VPXMacroblock.Subblock;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class FilterUtil {

    /**
     *  Clamp, then convert signed number back to pixel value. 
     */
    private static int clipPlus128(int v) {
        return (int) (clipSigned(v) + 128);
    }
        
    public static class Segment {
        /**
         * pixels before edge
         */
        int p0, p1, p2, p3;
        /**
         * pixels after edge
         */
        int q0, q1, q2, q3;
        
        /**
         * All functions take (among other things) a segment (of length at most 4 + 4 = 8) symmetrically 
         * straddling an edge. The pixel values (or pointers) are always given in order, from the 
         * "beforemost" to the "aftermost". So, for a horizontal edge (written "|"), an 8-pixel segment 
         * would be ordered p3 p2 p1 p0 | q0 q1 q2 q3.
         *
         * Filtering is disabled if the difference between any two adjacent "interior" pixels in the 
         * 8-pixel segment exceeds the relevant threshold (I). A more complex thresholding calculation 
         * is done for the group of four pixels that straddle the edge, in line with the calculation in simple_segment() above.
         * @interior limit on interior differences 
         * @edge limit at the edge 
         */
        public boolean isFilterRequired(int interior, int edge) {
            return ((abs(p0 - q0)<<2) + (abs(p1 - q1)>>2)) <= edge 
                    && abs(p3 - p2) <= interior 
                    && abs(p2 - p1) <= interior 
                    && abs(p1 - p0) <= interior 
                    && abs(q3 - q2) <= interior 
                    && abs(q2 - q1) <= interior 
                    && abs(q1 - q0) <= interior;
        }
        
        /**
         *  HEV - Hight Edge Variance. Filtering is altered 
         *  if (at least) one of the differences on either side of 
         *  the edge exceeds a threshold (we have "high edge variance").
         * @param threshold
         * @param p1 before
         * @param p0 before
         * @param q0 after
         * @param q1 after
         * @return
         */
        public boolean isHighVariance(int threshold) {
            return abs(p1 - p0) > threshold || abs(q1 - q0) > threshold;
        }
        
        public Segment getSigned() {
            Segment seg = new Segment();
            seg.p3 = minus128(this.p3);
            seg.p2 = minus128(this.p2);
            seg.p1 = minus128(this.p1);
            seg.p0 = minus128(this.p0);
            seg.q0 = minus128(this.q0);
            seg.q1 = minus128(this.q1); 
            seg.q2 = minus128(this.q2);
            seg.q3 = minus128(this.q3);
            return seg;
        }
        
        public static Segment horizontal(Subblock right, Subblock left, int a) {
            Segment seg = new Segment();
            seg.p0 = left.val[3*4+a];
            seg.p1 = left.val[2*4+a];
            seg.p2 = left.val[1*4+a];
            seg.p3 = left.val[0*4+a];
            seg.q0 = right.val[0*4+a];
            seg.q1 = right.val[1*4+a];
            seg.q2 = right.val[2*4+a];
            seg.q3 = right.val[3*4+a];
            return seg;
        }

        public static Segment vertical(Subblock lower, Subblock upper, int a) {
            Segment seg = new Segment();
            seg.p0 = upper.val[a*4+3];
            seg.p1 = upper.val[a*4+2];
            seg.p2 = upper.val[a*4+1];
            seg.p3 = upper.val[a*4+0];
            seg.q0 = lower.val[a*4+0];
            seg.q1 = lower.val[a*4+1];
            seg.q2 = lower.val[a*4+2];
            seg.q3 = lower.val[a*4+3];
            return seg;
        }
        
        public void applyHorizontally(Subblock right, Subblock left, int a) {
            left.val[3*4+a] = this.p0;
            left.val[2*4+a] = this.p1;
            left.val[1*4+a] = this.p2;
            left.val[0*4+a] = this.p3;
            right.val[0*4+a] = this.q0;
            right.val[1*4+a] = this.q1;
            right.val[2*4+a] = this.q2;
            right.val[3*4+a] = this.q3;

        }

        public void applyVertically(Subblock lower, Subblock upper, int a) {
            upper.val[a*4+3] = this.p0;
            upper.val[a*4+2] = this.p1;
            upper.val[a*4+1] = this.p2;
            upper.val[a*4+0] = this.p3;
            lower.val[a*4+0] = this.q0;
            lower.val[a*4+1] = this.q1;
            lower.val[a*4+2] = this.q2;
            lower.val[a*4+3] = this.q3;

        }
        
        /**
         * 
         * @param hevThreshold detect high edge variance
         * @param interiorLimit possibly disable filter
         * @param edgeLimit
         * @param this
         */
         void filterMb(int hevThreshold, int interiorLimit, int edgeLimit) {
            Segment signedSeg = this.getSigned();
            if (signedSeg.isFilterRequired(interiorLimit, edgeLimit)) {
                if (!signedSeg.isHighVariance(hevThreshold)) {
                    // Same as the initial calculation in "common_adjust",
                    // w is something like twice the edge difference
                    int w = clipSigned(clipSigned(signedSeg.p1 - signedSeg.q1) + 3 * (signedSeg.q0 - signedSeg.p0));

                    // 9/64 is approximately 9/63 = 1/7 and 1<<7 = 128 = 2*64.
                    // So this a, used to adjust the pixels adjacent to the edge,
                    // is something like 3/7 the edge difference.
                    int a = (27 * w + 63) >> 7;

                    q0 = clipPlus128(signedSeg.q0 - a);
                    p0 = clipPlus128(signedSeg.p0 + a);
                    // Next two are adjusted by 2/7 the edge difference
                    a = (18 * w + 63) >> 7;
                    // System.out.println("a: "+a);
                    q1 = clipPlus128(signedSeg.q1 - a);
                    p1 = clipPlus128(signedSeg.p1 + a);
                    // Last two are adjusted by 1/7 the edge difference
                    a = (9 * w + 63) >> 7;
                    q2 = clipPlus128(signedSeg.q2 - a);
                    p2 = clipPlus128(signedSeg.p2 + a);
                } else
                    // if hev, do simple filter
                    this.adjust(true); // using outer taps
            }
        }
         
         /**
          * 
          * @param hev_threshold detect high edge variance
          * @param interior_limit disable filter
          * @param edge_limit
          * @param this
          */
         public void filterSb(int hev_threshold, 
                 int interior_limit, 
                 int edge_limit) {
             Segment signedSeg = this.getSigned();
             if (signedSeg.isFilterRequired(interior_limit, edge_limit)) {
                 boolean hv = signedSeg.isHighVariance(hev_threshold);
                 int a = (this.adjust(hv) + 1) >> 1;
                 if (!hv) {
                     this.q1 = clipPlus128(signedSeg.q1 - a);
                     this.p1 = clipPlus128(signedSeg.p1 + a);
                 }
             } 
         }
         
         /**
          * filter is 2 or 4 taps wide
          */
         private  int adjust(boolean use_outer_taps) {
             /**
              *  retrieve and convert all 4 pixels 
              */
             int p1 = minus128(this.p1); 
             int p0 = minus128(this.p0);
             int q0 = minus128(this.q0);
             int q1 = minus128(this.q1);
             
             /**
              * Disregarding clamping, when "use_outer_taps" is false, "a" is 3*(q0-p0). 
              * Since we are about to divide "a" by 8, in this case we end up multiplying 
              * the edge difference by 5/8. When "use_outer_taps" is true (as for the 
              * simple filter), "a" is p1 - 3*p0 + 3*q0 - q1, which can be thought of 
              * as a refinement of 2*(q0 - p0) and the adjustment is something like 
              * (q0 - p0)/4.
              */
             int a = clipSigned((use_outer_taps ? clipSigned(p1 - q1) : 0) + 3 * (q0 - p0));
             
             /**
              * b is used to balance the rounding of a/8 in the case where the "fractional" 
              * part "f" of a/8 is exactly 1/2.
              */
             int b = (clipSigned(a + 3)) >> 3;
             
             /**
              * Divide a by 8, rounding up when f >= 1/2. Although not strictly part 
              * of the "C" language, the right-shift is assumed to propagate the sign bit.
              */
             a = clipSigned(a + 4) >> 3;
             
             /**
              *  Subtract "a" from q0, "bringing it closer" to p0. 
              */
             this.q0 = clipPlus128(q0 - a);
             
             /**
              * Add "a" (with adjustment "b") to p0, "bringing it closer" to q0. T
              * he clamp of "a+b", while present in the reference decoder, is 
              * superfluous; we have -16 <= a <= 15 at this point.
              */
             this.p0 = clipPlus128(p0 + b);
             
             return a;
         }
    }
    
    
    private static int clipSigned(int v) {
         return (int) (v < -128 ? -128 : (v > 127 ? 127 : v));
    }

    /* Convert pixel value (0 <= v <= 255) to an 8-bit signed number. */
    private static int minus128(int v) {
        return (int) (v - 128);
    }

    public static void loopFilterUV(VPXMacroblock[][] mbs, int sharpnessLevel, boolean keyFrame) {
                for (int y = 0; y < (mbs.length-2); y++) {
                    for (int x = 0; x < (mbs[0].length-2); x++) {
                        VPXMacroblock rmb = mbs[y+1][x+1];
                        VPXMacroblock bmb = mbs[y+1][x+1];
                        int loop_filter_level = rmb.filterLevel;
                        if (loop_filter_level != 0) {
                            int interior_limit = rmb.filterLevel;
                            if (sharpnessLevel > 0) {
                                interior_limit >>= sharpnessLevel > 4 ? 2 : 1;
                                if (interior_limit > 9 - sharpnessLevel)
                                    interior_limit = 9 - sharpnessLevel;
                            }
                            if (interior_limit == 0)
                                interior_limit = 1;
    
                            int hev_threshold = 0;
                            if (keyFrame) /* current frame is a key frame */ {
                                if (loop_filter_level >= 40)
                                    hev_threshold = 2;
                                else if (loop_filter_level >= 15)
                                    hev_threshold = 1;
                            } else /* current frame is an interframe */ {
                                throw new NotImplementedException("TODO: non-key frames are not supported yet.");
//                                if (loop_filter_level >= 40)
//                                    hev_threshold = 3;
//                                else if (loop_filter_level >= 20)
//                                    hev_threshold = 2;
//                                else if (loop_filter_level >= 15)
//                                    hev_threshold = 1;
                            }
    
                            /* Luma and Chroma use the same inter-macroblock edge limit */
                            int mbedge_limit = ((loop_filter_level + 2) * 2) + interior_limit;
                            /* Luma and Chroma use the same inter-subblock edge limit */
                            int sub_bedge_limit = (loop_filter_level * 2) + interior_limit;
    
                            if (x > 0) {
                                VPXMacroblock lmb = mbs[y+1][x+1-1];
                                for (int b = 0; b < 2; b++) {
                                    Subblock rsbU = rmb.uSubblocks[b][0];
                                    Subblock lsbU = lmb.uSubblocks[b][1];
                                    Subblock rsbV = rmb.vSubblocks[b][0];
                                    Subblock lsbV = lmb.vSubblocks[b][1];
                                    for (int a = 0; a < 4; a++) {
                                        Segment seg = horizontal(rsbU, lsbU, a);
                                        seg.filterMb(hev_threshold, interior_limit, mbedge_limit);
                                        seg.applyHorizontally(rsbU, lsbU, a);
                                        seg = horizontal(rsbV, lsbV, a);
                                        seg.filterMb(hev_threshold, interior_limit, mbedge_limit);
                                        seg.applyHorizontally(rsbV, lsbV, a);
    
                                    }
                                }
                            }
                            // sb left
    
                            if (!rmb.skipFilter) {
                                for (int a = 1; a < 2; a++) {
                                    for (int b = 0; b < 2; b++) {
                                        Subblock lsbU = rmb.uSubblocks[b][a - 1];
                                        Subblock rsbU = rmb.uSubblocks[b][a];
                                        Subblock lsbV = rmb.vSubblocks[b][a - 1];
                                        Subblock rsbV = rmb.vSubblocks[b][a];
                                        for (int c = 0; c < 4; c++) {
                                            Segment seg = horizontal(rsbU, lsbU, c);
                                            seg.filterSb(hev_threshold, interior_limit, sub_bedge_limit);
                                            seg.applyHorizontally(rsbU, lsbU, c);
                                            seg = horizontal(rsbV, lsbV, c);
                                            seg.filterSb(hev_threshold, interior_limit, sub_bedge_limit);
                                            seg.applyHorizontally(rsbV, lsbV, c);
                                        }
                                    }
                                }
                            }
                            // top
                            if (y > 0) {
                                VPXMacroblock tmb = mbs[y+1-1][x+1];
                                for (int b = 0; b < 2; b++) {
                                    Subblock tsbU = tmb.uSubblocks[1][b];
                                    Subblock bsbU = bmb.uSubblocks[0][b];
                                    Subblock tsbV = tmb.vSubblocks[1][b];
                                    Subblock bsbV = bmb.vSubblocks[0][b];
                                    for (int a = 0; a < 4; a++) {
                                        // System.out.println("l");
                                        Segment seg = vertical(bsbU, tsbU, a);
                                        seg.filterMb(hev_threshold, interior_limit, mbedge_limit);
                                        seg.applyVertically(bsbU, tsbU, a);
                                        seg = vertical(bsbV, tsbV, a);
                                        seg.filterMb(hev_threshold, interior_limit, mbedge_limit);
                                        seg.applyVertically(bsbV, tsbV, a);
                                    }
                                }
                            }
                            // sb top
    
                            if (!rmb.skipFilter) {
                                for (int a = 1; a < 2; a++) {
                                    for (int b = 0; b < 2; b++) {
                                        Subblock tsbU = bmb.uSubblocks[a-1][b];
                                        Subblock bsbU = bmb.uSubblocks[a][b];
                                        Subblock tsbV = bmb.vSubblocks[a-1][b];
                                        Subblock bsbV = bmb.vSubblocks[a][b];
                                        for (int c = 0; c < 4; c++) {
                                            Segment seg = vertical(bsbU, tsbU, c);
                                            seg.filterSb(hev_threshold, interior_limit, sub_bedge_limit);
                                            seg.applyVertically(bsbU, tsbU, c);
                                            seg = vertical(bsbV, tsbV, c);
                                            seg.filterSb(hev_threshold, interior_limit, sub_bedge_limit);
                                            seg.applyVertically(bsbV, tsbV, c);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

    public static void loopFilterY(VPXMacroblock[][] mbs, int sharpnessLevel, boolean keyFrame) {
                for (int y = 0; y < (mbs.length-2); y++) {
                    for (int x = 0; x < (mbs[0].length-2); x++) {
                        VPXMacroblock rmb = mbs[y+1][x+1];
                        VPXMacroblock bmb = mbs[y+1][x+1];
                        int loopFilterLevel = rmb.filterLevel;
    
                        if (loopFilterLevel != 0) {
                            int interiorLimit = rmb.filterLevel;
    
                            if (sharpnessLevel > 0) {
                                interiorLimit >>= sharpnessLevel > 4 ? 2 : 1;
                                if (interiorLimit > 9 - sharpnessLevel)
                                    interiorLimit = 9 - sharpnessLevel;
                            }
                            if (interiorLimit == 0)
                                interiorLimit = 1;
    
                            int varianceThreshold = 0;
                            if (keyFrame) /* current frame is a key frame */ {
                                if (loopFilterLevel >= 40)
                                    varianceThreshold = 2;
                                else if (loopFilterLevel >= 15)
                                    varianceThreshold = 1;
                            } else /* current frame is an interframe */ {
                                throw new NotImplementedException("TODO: non-key frames are not supported yet");
//                                if (loopFilterLevel >= 40)
//                                    varianceThreshold = 3;
//                                else if (loop_filter_level >= 20)
//                                    varianceThreshold = 2;
//                                else if (loop_filter_level >= 15)
//                                    varianceThreshold = 1;
                            }
    
                            /**
                             *  Luma and Chroma use the same inter-macroblock edge limit 
                             */
                            int edgeLimitMb = ((loopFilterLevel + 2) * 2) + interiorLimit;
                            /**
                             *  Luma and Chroma use the same inter-subblock edge limit 
                             */
                            int edgeLimitSb = (loopFilterLevel * 2) + interiorLimit;
    
                            // left
                            if (x > 0) {
                                VPXMacroblock lmb = mbs[y+1][x-1+1];
                                for (int b = 0; b < 4; b++) {
                                    Subblock rsb = rmb.ySubblocks[b][0];
                                    Subblock lsb = lmb.ySubblocks[b][3];
                                    for (int a = 0; a < 4; a++) {
                                        Segment seg = horizontal(rsb, lsb, a);
                                        seg.filterMb(varianceThreshold, interiorLimit, edgeLimitMb);
                                        seg.applyHorizontally(rsb, lsb, a);
                                    }
                                }
                            }
                            // sb left
                            if (!rmb.skipFilter) {
                                for (int a = 1; a < 4; a++) {
                                    for (int b = 0; b < 4; b++) {
                                        Subblock lsb = rmb.ySubblocks[b][a-1];
                                        Subblock rsb = rmb.ySubblocks[b][a];
                                        for (int c = 0; c < 4; c++) {
                                            Segment seg = horizontal(rsb, lsb, c);
                                            seg.filterSb(varianceThreshold, interiorLimit, edgeLimitSb);
                                            seg.applyHorizontally(rsb, lsb, c);
                                        }
                                    }
                                }
                            }
                            // top
                            if (y > 0) {
                                VPXMacroblock tmb = mbs[y-1+1][x+1];
                                for (int b = 0; b < 4; b++) {
                                    Subblock tsb = tmb.ySubblocks[3][b];
                                    Subblock bsb = bmb.ySubblocks[0][b];
                                    for (int a = 0; a < 4; a++) {
                                        Segment seg = vertical(bsb, tsb, a);
                                        seg.filterMb(varianceThreshold, interiorLimit, edgeLimitMb);
                                        seg.applyVertically(bsb, tsb, a);
                                    }
                                }
                            }
                            // sb top
                            if (!rmb.skipFilter) {
                                for (int a = 1; a < 4; a++) {
                                    for (int b = 0; b < 4; b++) {
                                        Subblock tsb = bmb.ySubblocks[a-1][b];
                                        Subblock bsb = bmb.ySubblocks[a][b];
                                        for (int c = 0; c < 4; c++) {
                                            Segment seg = vertical(bsb, tsb, c);
                                            seg.filterSb(varianceThreshold, interiorLimit, edgeLimitSb);
                                            seg.applyVertically(bsb, tsb, c);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

}
