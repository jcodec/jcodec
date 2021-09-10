package org.jcodec.codecs.vpx;

import static org.jcodec.codecs.vpx.VP8Util.PRED_BLOCK_127;
import static org.jcodec.codecs.vpx.VP8Util.pickDefaultPrediction;

import java.util.Arrays;

import org.jcodec.api.NotImplementedException;
import org.jcodec.codecs.vpx.VP8Util.QuantizationParams;
import org.jcodec.codecs.vpx.VP8Util.SubblockConstants;
import org.jcodec.codecs.vpx.vp8.CommonUtils;
import org.jcodec.codecs.vpx.vp8.enums.BPredictionMode;
import org.jcodec.codecs.vpx.vp8.intrapred.AllIntraPred;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;
import org.jcodec.codecs.vpx.vp8.pointerhelper.ReadOnlyIntArrPointer;
import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class VPXMacroblock {

    public int filterLevel;
    public int chromaMode;
    public int skipCoeff;
    public final Subblock[][] ySubblocks;
    public final Subblock y2;
    public final Subblock[][] uSubblocks;
    public final Subblock[][] vSubblocks;
    public final int Rrow;
    public final int column;
    public int lumaMode;
    boolean skipFilter;
    public int segment = 0;
    public boolean debug = true;
    public QuantizationParams quants;

    public VPXMacroblock(int y, int x) {
        this.ySubblocks = new Subblock[4][4];
        this.y2 = new Subblock(this, 0, 0, VP8Util.PLANE.Y2);
        this.uSubblocks = new Subblock[2][2];
        this.vSubblocks = new Subblock[2][2];

        this.Rrow = y;
        this.column = x;
        for (int row = 0; row < 4; row++)
            for (int col = 0; col < 4; col++)
                this.ySubblocks[row][col] = new Subblock(this, row, col, VP8Util.PLANE.Y1);
        for (int row = 0; row < 2; row++)
            for (int col = 0; col < 2; col++) {
                uSubblocks[row][col] = new Subblock(this, row, col, VP8Util.PLANE.U);
                vSubblocks[row][col] = new Subblock(this, row, col, VP8Util.PLANE.V);
            }
    }

    public void dequantMacroBlock(VPXMacroblock[][] mbs) {
        QuantizationParams p = this.quants;
        if (this.lumaMode != SubblockConstants.B_PRED) {
            short acQValue = p.y2AC;
            short dcQValue = p.y2DC;

            short input[] = new short[16];
            input[0] = (short) (this.y2.tokens[0] * dcQValue);

            for (int x = 1; x < 16; x++)
                input[x] = (short) (this.y2.tokens[x] * acQValue);

            /**
             * if (plane == PLANE.U || plane == PLANE.V) { QValue = p.chromaAC; if (i == 0)
             * QValue = p.chromaDC; } else { QValue = p.yAC; if (i == 0) QValue = p.yDC; }
             */
            this.y2.residue = VP8DCT.decodeWHT(input);
            for (int row = 0; row < 4; row++)
                for (int col = 0; col < 4; col++)
                    this.ySubblocks[row][col].dequantSubblock(p.yDC, p.yAC, y2.residue[row * 4 + col]);

            this.predictY(mbs);
            this.predictUV(mbs);
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 2; col++) {
                    this.uSubblocks[row][col].dequantSubblock(p.chromaDC, p.chromaAC, null);
                    this.vSubblocks[row][col].dequantSubblock(p.chromaDC, p.chromaAC, null);
                }
            }
            this.reconstruct();

        } else {
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    Subblock sb = this.ySubblocks[row][col];
                    sb.dequantSubblock(p.yDC, p.yAC, null);
                    sb.predict(mbs);
                    sb.reconstruct();
                }
            }
            this.predictUV(mbs);
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 2; col++) {
                    Subblock sb = this.uSubblocks[row][col];
                    sb.dequantSubblock(p.chromaDC, p.chromaAC, null);
                    sb.reconstruct();
                }
            }
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 2; col++) {
                    Subblock sb = this.vSubblocks[row][col];
                    sb.dequantSubblock(p.chromaDC, p.chromaAC, null);
                    sb.reconstruct();
                }
            }
        }

    }

    public void reconstruct() {
        for (int row = 0; row < 4; row++)
            for (int col = 0; col < 4; col++)
                ySubblocks[row][col].reconstruct();

        for (int row = 0; row < 2; row++)
            for (int col = 0; col < 2; col++)
                uSubblocks[row][col].reconstruct();

        for (int row = 0; row < 2; row++)
            for (int col = 0; col < 2; col++)
                vSubblocks[row][col].reconstruct();

    }

    public void predictUV(VPXMacroblock[][] mbs) {
        VPXMacroblock aboveMb = mbs[Rrow - 1][column];
        VPXMacroblock leftMb = mbs[Rrow][column - 1];

        switch (this.chromaMode) {
        case SubblockConstants.DC_PRED:
            // System.out.println("UV DC_PRED");

            boolean up_available = false;
            boolean left_available = false;
            int uAvg = 0;
            int vAvg = 0;
            short expected_udc = 128;
            short expected_vdc = 128;
            if (column > 1)
                left_available = true;
            if (Rrow > 1)
                up_available = true;
            if (up_available || left_available) {
                if (up_available) {
                    for (int j = 0; j < 2; j++) {
                        Subblock usb = aboveMb.uSubblocks[1][j];
                        Subblock vsb = aboveMb.vSubblocks[1][j];
                        for (int i = 0; i < 4; i++) {
                            uAvg += usb.val[3 * 4 + i];
                            vAvg += vsb.val[3 * 4 + i];
                        }
                    }
                }

                if (left_available) {
                    for (int j = 0; j < 2; j++) {
                        Subblock usb = leftMb.uSubblocks[j][1];
                        Subblock vsb = leftMb.vSubblocks[j][1];
                        for (int i = 0; i < 4; i++) {
                            uAvg += usb.val[i * 4 + 3];
                            vAvg += vsb.val[i * 4 + 3];
                        }
                    }
                }

                int shift = 2;
                if (up_available)
                    shift++;
                if (left_available)
                    shift++;

                expected_udc = (short) ((uAvg + (1 << (shift - 1))) >> shift);
                expected_vdc = (short) ((vAvg + (1 << (shift - 1))) >> shift);
            }

            short ufill[] = new short[16];
            for (int aRow = 0; aRow < 4; aRow++)
                for (int aCol = 0; aCol < 4; aCol++)
                    ufill[aRow * 4 + aCol] = expected_udc;

            short vfill[] = new short[16];
            for (int aRow = 0; aRow < 4; aRow++)
                for (int aCol = 0; aCol < 4; aCol++)
                    vfill[aRow * 4 + aCol] = expected_vdc;

            for (int aRow = 0; aRow < 2; aRow++) {
                for (int aCol = 0; aCol < 2; aCol++) {
                    Subblock usb = uSubblocks[aRow][aCol];
                    Subblock vsb = vSubblocks[aRow][aCol];
                    usb._predict = ufill;
                    vsb._predict = vfill;
                }
            }

            break;
        case SubblockConstants.V_PRED:
            // System.out.println("UV V_PRED");

            Subblock[] aboveUSb = new Subblock[2];
            Subblock[] aboveVSb = new Subblock[2];
            for (int aCol = 0; aCol < 2; aCol++) {
                aboveUSb[aCol] = aboveMb.uSubblocks[1][aCol];
                aboveVSb[aCol] = aboveMb.vSubblocks[1][aCol];
            }

            for (int aRow = 0; aRow < 2; aRow++)
                for (int aCol = 0; aCol < 2; aCol++) {
                    Subblock usb = uSubblocks[aRow][aCol];
                    Subblock vsb = vSubblocks[aRow][aCol];
                    short ublock[] = new short[16];
                    short vblock[] = new short[16];
                    for (int pRow = 0; pRow < 4; pRow++)
                        // pRow for pixel row index
                        for (int pCol = 0; pCol < 4; pCol++) { // pCol for pixel column index
                            ublock[pRow * 4 + pCol] = aboveUSb[aCol].val != null ? aboveUSb[aCol].val[3 * 4 + pCol]
                                    : 127;
                            vblock[pRow * 4 + pCol] = aboveVSb[aCol].val != null ? aboveVSb[aCol].val[3 * 4 + pCol]
                                    : 127;
                        }
                    usb._predict = ublock;
                    vsb._predict = vblock;
                }

            break;

        case SubblockConstants.H_PRED:
            // System.out.println("UV H_PRED");

            Subblock[] leftUSb = new Subblock[2];
            Subblock[] leftVSb = new Subblock[2];
            for (int aCol = 0; aCol < 2; aCol++) {
                leftUSb[aCol] = leftMb.uSubblocks[aCol][1];
                leftVSb[aCol] = leftMb.vSubblocks[aCol][1];
            }

            for (int aRow = 0; aRow < 2; aRow++)
                for (int aCol = 0; aCol < 2; aCol++) {
                    Subblock usb = uSubblocks[aRow][aCol];
                    Subblock vsb = vSubblocks[aRow][aCol];
                    short ublock[] = new short[16];
                    short vblock[] = new short[16];
                    for (int pRow = 0; pRow < 4; pRow++)
                        for (int pCol = 0; pCol < 4; pCol++) {
                            ublock[pRow * 4 + pCol] = leftUSb[aRow].val != null ? leftUSb[aRow].val[pRow * 4 + 3] : 129;
                            vblock[pRow * 4 + pCol] = leftVSb[aRow].val != null ? leftVSb[aRow].val[pRow * 4 + 3] : 129;
                        }
                    usb._predict = ublock;
                    vsb._predict = vblock;
                }

            break;
        case SubblockConstants.TM_PRED:
            // TODO:
            // System.out.println("UV TM_PRED MB");
            VPXMacroblock ALMb = mbs[Rrow - 1][column - 1];
            Subblock ALUSb = ALMb.uSubblocks[1][1];
            int alu = ALUSb.val[3 * 4 + 3];
            Subblock ALVSb = ALMb.vSubblocks[1][1];
            int alv = ALVSb.val[3 * 4 + 3];

            aboveUSb = new Subblock[2];
            leftUSb = new Subblock[2];
            aboveVSb = new Subblock[2];
            leftVSb = new Subblock[2];
            for (int x = 0; x < 2; x++) {
                aboveUSb[x] = aboveMb.uSubblocks[1][x];
                leftUSb[x] = leftMb.uSubblocks[x][1];
                aboveVSb[x] = aboveMb.vSubblocks[1][x];
                leftVSb[x] = leftMb.vSubblocks[x][1];
            }

            for (int sbRow = 0; sbRow < 2; sbRow++) {
                for (int pRow = 0; pRow < 4; pRow++) {
                    for (int sbCol = 0; sbCol < 2; sbCol++) {
                        if (uSubblocks[sbRow][sbCol].val == null)
                            uSubblocks[sbRow][sbCol].val = new short[16];
                        if (vSubblocks[sbRow][sbCol].val == null)
                            vSubblocks[sbRow][sbCol].val = new short[16];
                        for (int pCol = 0; pCol < 4; pCol++) {

                            short upred = (short) (leftUSb[sbRow].val[pRow * 4 + 3] + aboveUSb[sbCol].val[3 * 4 + pCol]
                                    - alu);
                            upred = CommonUtils.clipPixel(upred);
                            uSubblocks[sbRow][sbCol].val[pRow * 4 + pCol] = upred;

                            short vpred = (short) (leftVSb[sbRow].val[pRow * 4 + 3] + aboveVSb[sbCol].val[3 * 4 + pCol]
                                    - alv);
                            vpred = CommonUtils.clipPixel(vpred);
                            vSubblocks[sbRow][sbCol].val[pRow * 4 + pCol] = vpred;

                        }
                    }

                }
            }

            break;
        default:
            System.err.println("TODO predict_mb_uv: " + this.lumaMode);
            System.exit(0);
        }
    }

    private void predictY(VPXMacroblock[][] mbs) {
        VPXMacroblock aboveMb = mbs[Rrow - 1][column];
        VPXMacroblock leftMb = mbs[Rrow][column - 1];

        switch (this.lumaMode) {
        case SubblockConstants.DC_PRED:
            predictLumaDC(aboveMb, leftMb);
            break;

        case SubblockConstants.V_PRED:
            predictLumaV(aboveMb);
            break;

        case SubblockConstants.H_PRED:
            predictLumaH(leftMb);
            break;

        case SubblockConstants.TM_PRED:
            VPXMacroblock upperLeft = mbs[Rrow - 1][column - 1];
            Subblock ALSb = upperLeft.ySubblocks[3][3];
            int aboveLeft = ALSb.val[3 * 4 + 3];
            predictLumaTM(aboveMb, leftMb, aboveLeft);

            break;
        default:
            System.err.println("TODO predict_mb_y: " + this.lumaMode);
            System.exit(0);
        }
    }

    private void predictLumaDC(VPXMacroblock above, VPXMacroblock left) {
        boolean hasAbove = Rrow > 1;
        boolean hasLeft = column > 1;

        short expected_dc = 128;

        if (hasAbove || hasLeft) {
            int average = 0;
            if (hasAbove) {
                for (int j = 0; j < 4; j++) {
                    Subblock sb = above.ySubblocks[3][j];
                    for (int i = 0; i < 4; i++)
                        average += sb.val[3 * 4 + i];

                }
            }

            if (hasLeft) {
                for (int j = 0; j < 4; j++) {
                    Subblock sb = left.ySubblocks[j][3];
                    for (int i = 0; i < 4; i++)
                        average += sb.val[i * 4 + 3];

                }
            }

            int shift = 3;
            if (hasAbove)
                shift++;
            if (hasLeft)
                shift++;

            expected_dc = (short) ((average + (1 << (shift - 1))) >> shift);
        }

        short fill[] = new short[16];
        for (int i = 0; i < 16; i++)
            fill[i] = expected_dc;

        for (int y = 0; y < 4; y++)
            for (int x = 0; x < 4; x++)
                ySubblocks[y][x]._predict = fill;
    }

    private void predictLumaH(VPXMacroblock leftMb) {
        Subblock[] leftYSb = new Subblock[4];
        for (int row = 0; row < 4; row++)
            leftYSb[row] = leftMb.ySubblocks[row][3];

        for (int row = 0; row < 4; row++)
            for (int col = 0; col < 4; col++) {
                Subblock sb = ySubblocks[row][col];
                short block[] = new short[16];
                for (int bRow = 0; bRow < 4; bRow++)
                    for (int bCol = 0; bCol < 4; bCol++) {
                        block[bRow * 4 + bCol] = leftYSb[row].val != null ? leftYSb[row].val[bRow * 4 + 3] : 129;
                    }
                sb._predict = block;
            }
    }

    private void predictLumaTM(VPXMacroblock above, VPXMacroblock left, int aboveLeft) {
        Subblock[] leftYSb;
        Subblock[] aboveYSb = new Subblock[4];
        leftYSb = new Subblock[4];
        for (int col = 0; col < 4; col++)
            aboveYSb[col] = above.ySubblocks[3][col];
        for (int row = 0; row < 4; row++)
            leftYSb[row] = left.ySubblocks[row][3];

        for (int row = 0; row < 4; row++)
            for (int pRow = 0; pRow < 4; pRow++)
                for (int col = 0; col < 4; col++) {
                    if (ySubblocks[row][col].val == null)
                        ySubblocks[row][col].val = new short[16];

                    for (int pCol = 0; pCol < 4; pCol++) {

                        short pred = (short) (leftYSb[row].val[pRow * 4 + 3] + aboveYSb[col].val[3 * 4 + pCol]
                                - aboveLeft);

                        ySubblocks[row][col].val[pRow * 4 + pCol] = CommonUtils.clipPixel(pred);

                    }
                }
    }

    private void predictLumaV(VPXMacroblock above) {
        Subblock[] aboveYSb = new Subblock[4];
        for (int col = 0; col < 4; col++)
            aboveYSb[col] = above.ySubblocks[3][col];

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                Subblock sb = ySubblocks[row][col];
                short block[] = new short[16];
                for (int j = 0; j < 4; j++)
                    for (int i = 0; i < 4; i++) {
                        block[j * 4 + i] = aboveYSb[col].val != null ? aboveYSb[col].val[3 * 4 + i] : 127;
                        // block[j*4+i] = aboveYSb[x].getPredict(SubBlock.B_VE_PRED, false)[3*4+i];
                    }
                sb._predict = block;

            }
        }
    }

    public Subblock getBottomSubblock(int x, VP8Util.PLANE plane) {
        if (plane == VP8Util.PLANE.Y1) {
            return ySubblocks[3][x];
        } else if (plane == VP8Util.PLANE.U) {
            return uSubblocks[1][x];
        } else if (plane == VP8Util.PLANE.V) {
            return vSubblocks[1][x];
        } else if (plane == VP8Util.PLANE.Y2) {
            return y2;
        }
        return null;
    }

    public Subblock getRightSubBlock(int y, VP8Util.PLANE plane) {
        if (plane == VP8Util.PLANE.Y1) {
            return ySubblocks[y][3];
        } else if (plane == VP8Util.PLANE.U) {
            return uSubblocks[y][1];
        } else if (plane == VP8Util.PLANE.V) {
            return vSubblocks[y][1];
        } else if (plane == VP8Util.PLANE.Y2) {
            return y2;
        }
        return null;
    }

    public void decodeMacroBlock(VPXMacroblock[][] mbs, VPXBooleanDecoder tockenDecoder, short[][][][] coefProbs) {
        if (this.skipCoeff > 0) {
            this.skipFilter = this.lumaMode != SubblockConstants.B_PRED;
        } else if (this.lumaMode != SubblockConstants.B_PRED)
            decodeMacroBlockTokens(true, mbs, tockenDecoder, coefProbs);
        else
            decodeMacroBlockTokens(false, mbs, tockenDecoder, coefProbs);
    }

    private void decodeMacroBlockTokens(boolean withY2, VPXMacroblock[][] mbs, VPXBooleanDecoder decoder,
            short[][][][] coefProbs) {
        skipFilter = false;
        if (withY2) {
            skipFilter = skipFilter | decodePlaneTokens(1, VP8Util.PLANE.Y2, false, mbs, decoder, coefProbs);
        }
        skipFilter = skipFilter | decodePlaneTokens(4, VP8Util.PLANE.Y1, withY2, mbs, decoder, coefProbs);
        skipFilter = skipFilter | decodePlaneTokens(2, VP8Util.PLANE.U, false, mbs, decoder, coefProbs);
        skipFilter = skipFilter | decodePlaneTokens(2, VP8Util.PLANE.V, false, mbs, decoder, coefProbs);
        skipFilter = !skipFilter;
    }

    private boolean decodePlaneTokens(int dimentions, VP8Util.PLANE plane, boolean withY2, VPXMacroblock[][] mbs,
            VPXBooleanDecoder decoder, short[][][][] coefProbs) {
        boolean r = false;
        for (int row = 0; row < dimentions; row++) {
            for (int col = 0; col < dimentions; col++) {
                int lc = 0;
                Subblock sb = null; // this.ySubblocks[row][col];
                if (VP8Util.PLANE.Y1.equals(plane)) {
                    sb = ySubblocks[row][col];
                } else if (VP8Util.PLANE.U.equals(plane)) {
                    sb = uSubblocks[row][col];
                } else if (VP8Util.PLANE.V.equals(plane)) {
                    sb = vSubblocks[row][col];
                } else if (VP8Util.PLANE.Y2.equals(plane)) {
                    sb = y2;
                } else {
                    throw new IllegalStateException("unknown plane type " + plane);
                }
                // System.out.println("mb["+mb.x+"]["+mb.y+"];");
                // System.out.println("sb["+sb.x+"]["+sb.y+"];");
                // System.out.println("int[] sb = "+sb.toString()+";");
                Subblock l = sb.getLeft(plane, mbs);
                Subblock a = sb.getAbove(plane, mbs);

                lc = (l.someValuePresent ? 1 : 0) + (a.someValuePresent ? 1 : 0);

                sb.decodeSubBlock(decoder, coefProbs, lc, VP8Util.planeToType(plane, withY2), withY2);

                // System.out.println("int[] sb = "+sb.toString()+";");
                r = r | sb.someValuePresent;
            }
        }
        return r;
    }

    public static class Subblock {

        public short[] val;
        public short[] _predict;
        public short[] residue;
        private int col;
        private int row;
        private VP8Util.PLANE plane;
        public BPredictionMode mode;
        public boolean someValuePresent;
        private short[] tokens;
        private VPXMacroblock self;

        public Subblock(VPXMacroblock self, int row, int col, VP8Util.PLANE plane) {
            this.self = self;
            this.row = row;
            this.col = col;
            this.plane = plane;
            this.tokens = new short[16];
        }

        public void predict(VPXMacroblock[][] mbs) {
            Subblock aboveSb = getAbove(plane, mbs);
            Subblock leftSb = getLeft(plane, mbs);

            short[] above = new short[1 + 4 + 4];
            short[] left = new short[4];

            short[] aboveValues = aboveSb.val != null ? aboveSb.val : PRED_BLOCK_127;
            above[1] = aboveValues[0 + 4 * 3];
            above[2] = aboveValues[1 + 4 * 3];
            above[3] = aboveValues[2 + 4 * 3];
            above[4] = aboveValues[3 + 4 * 3];
            short ar[] = getAboveRightLowestRow(mbs);
            above[5] = ar[0];
            above[6] = ar[1];
            above[7] = ar[2];
            above[8] = ar[3];
            short[] leftValues = leftSb.val != null ? leftSb.val : pickDefaultPrediction(this.mode);
            left[0] = leftValues[3 + 4 * 0];
            left[1] = leftValues[3 + 4 * 1];
            left[2] = leftValues[3 + 4 * 2];
            left[3] = leftValues[3 + 4 * 3];
            Subblock aboveLeftSb = aboveSb.getLeft(this.plane, mbs);

            if (leftSb.val == null && aboveSb.val == null) {

                above[0] = 127; // AL.getPredict(this.getMode(), false)[3][3];
            } else if (aboveSb.val == null) {

                above[0] = 127; // AL.getPredict(this.getMode(), false)[3][3];
            } else {
                above[0] = aboveLeftSb.val != null ? aboveLeftSb.val[3 + 4 * 3]
                        : pickDefaultPrediction(this.mode)[3 + 4 * 3];
            }

            AllIntraPred.bpred[this.mode.ordinal()].call(FullAccessIntArrPointer.toPointer(_predict), 4,
                    new ReadOnlyIntArrPointer(above, 1), new ReadOnlyIntArrPointer(left, 0));
        }

        public void reconstruct() {

            int aRow, aCol;
            short p[] = this.val != null ? this.val : this._predict;
            short[] dest = new short[16];

            for (aRow = 0; aRow < 4; aRow++) {
                for (aCol = 0; aCol < 4; aCol++) {
                    short a = CommonUtils.clipPixel((short) (this.residue[aRow * 4 + aCol] + p[aRow * 4 + aCol]));
                    dest[aRow * 4 + aCol] = a;
                }
            }

            this.val = dest;
        }

        public Subblock getAbove(VP8Util.PLANE plane, VPXMacroblock[][] mbs) {
            if (this.row > 0)
                if (VP8Util.PLANE.Y1.equals(this.plane))
                    return self.ySubblocks[this.row - 1][this.col];
                else if (VP8Util.PLANE.U.equals(this.plane))
                    return self.uSubblocks[this.row - 1][this.col];
                else if (VP8Util.PLANE.V.equals(this.plane))
                    return self.vSubblocks[this.row - 1][this.col];

            int x = this.col;

            VPXMacroblock mb2 = mbs[self.Rrow - 1][self.column];
            if (plane == VP8Util.PLANE.Y2) {
                while (mb2.lumaMode == SubblockConstants.B_PRED)
                    mb2 = mbs[mb2.Rrow - 1][mb2.column];
            }
            return mb2.getBottomSubblock(x, plane);

        }

        public Subblock getLeft(VP8Util.PLANE p, VPXMacroblock[][] mbs) {
            if (this.col > 0)
                if (VP8Util.PLANE.Y1.equals(this.plane))
                    return self.ySubblocks[this.row][this.col - 1];
                else if (VP8Util.PLANE.U.equals(this.plane))
                    return self.uSubblocks[this.row][this.col - 1];
                else if (VP8Util.PLANE.V.equals(this.plane))
                    return self.vSubblocks[this.row][this.col - 1];

            int y = this.row;
            VPXMacroblock mb2 = mbs[self.Rrow][self.column - 1];

            if (p == VP8Util.PLANE.Y2)
                while (mb2.lumaMode == SubblockConstants.B_PRED)
                    mb2 = mbs[mb2.Rrow][mb2.column - 1];

            return mb2.getRightSubBlock(y, p);

        }

        private short[] getAboveRightLowestRow(VPXMacroblock[][] mbs) {
            // this might break at right edge
            if (!VP8Util.PLANE.Y1.equals(this.plane))
                throw new NotImplementedException("Decoder.getAboveRight: not implemented for Y2 and chroma planes");

            short[] aboveRightDistValues;

            if (row == 0 && col < 3) {
                // top row
                VPXMacroblock mb2 = mbs[self.Rrow - 1][self.column];
                Subblock aboveRight = mb2.ySubblocks[3][col + 1];
                aboveRightDistValues = aboveRight.val;

            } else if (row > 0 && col < 3) {
                // not right edge or top row
                Subblock aboveRight = self.ySubblocks[row - 1][col + 1];
                aboveRightDistValues = aboveRight.val;

            } else if (row == 0 && col == 3) {
                // top right
                VPXMacroblock aboveRightMb = mbs[self.Rrow - 1][self.column + 1];
                if (aboveRightMb.column < (mbs[0].length - 1)) {
                    Subblock aboveRightSb = aboveRightMb.ySubblocks[3][0];
                    aboveRightDistValues = aboveRightSb.val;
                } else {
                    aboveRightDistValues = new short[16];
                    short fillVal = aboveRightMb.Rrow == 0 ? 127
                            : mbs[self.Rrow - 1][self.column].ySubblocks[3][3].val[3 * 4 + 3];

                    Arrays.fill(aboveRightDistValues, fillVal);
                }

            } else {
                // else use top right
                Subblock sb2 = self.ySubblocks[0][3];
                return sb2.getAboveRightLowestRow(mbs);
            }

            if (aboveRightDistValues == null)
                aboveRightDistValues = PRED_BLOCK_127;

            short ar[] = new short[4];
            ar[0] = aboveRightDistValues[0 + 4 * 3];
            ar[1] = aboveRightDistValues[1 + 4 * 3];
            ar[2] = aboveRightDistValues[2 + 4 * 3];
            ar[3] = aboveRightDistValues[3 + 4 * 3];
            return ar;

        }

        public void decodeSubBlock(VPXBooleanDecoder decoder, short[][][][] allProbs, int ilc, int type,
                boolean withY2) {
            int startAt = 0;
            if (withY2)
                startAt = 1;
            int lc = ilc;
            int count = 0;
            short v = 1;

            boolean skip = false;

            someValuePresent = false;
            while (!(v == SubblockConstants.dct_eob) && count + startAt < 16) {

                short[] probs = allProbs[type][SubblockConstants.vp8CoefBands[count + startAt]][lc];
                if (!skip) {
                    v = decoder.readTree(SubblockConstants.vp8CoefTree, probs);
                } else {
                    v = decoder.readTreeSkip(SubblockConstants.vp8CoefTree, probs, (short) 1);
                }

                short dv = decodeToken(decoder, v);
                lc = 0;
                skip = false;
                if (dv == 1 || dv == -1)
                    lc = 1;
                else if (dv > 1 || dv < -1)
                    lc = 2;
                else if (dv == SubblockConstants.DCT_0)
                    skip = true;

                if (v != SubblockConstants.dct_eob)
                    tokens[VPXConst.zigzag[count + startAt]] = dv;

                count++;
            }

            for (int x = 0; x < 16; x++)
                if (tokens[x] != 0)
                    someValuePresent = true;
        }

        private short decodeToken(VPXBooleanDecoder decoder, short initialValue) {
            short token = initialValue;

            if (initialValue == SubblockConstants.cat_5_6) {
                token = (short) (5 + DCTextra(decoder, SubblockConstants.Pcat1));
            }
            if (initialValue == SubblockConstants.cat_7_10) {
                token = (short) (7 + DCTextra(decoder, SubblockConstants.Pcat2));
            }
            if (initialValue == SubblockConstants.cat_11_18) {
                token = (short) (11 + DCTextra(decoder, SubblockConstants.Pcat3));
            }
            if (initialValue == SubblockConstants.cat_19_34) {
                token = (short) (19 + DCTextra(decoder, SubblockConstants.Pcat4));
            }
            if (initialValue == SubblockConstants.cat_35_66) {
                token = (short) (35 + DCTextra(decoder, SubblockConstants.Pcat5));
            }
            if (initialValue == SubblockConstants.cat_67_2048) {
                token = (short) (67 + DCTextra(decoder, SubblockConstants.Pcat6));
            }
            if (initialValue != SubblockConstants.DCT_0 && initialValue != SubblockConstants.dct_eob) {
                if (decoder.readBitEq() > 0)
                    token = (short) -token;
            }

            return token;
        }

        private int DCTextra(VPXBooleanDecoder decoder, short p[]) {
            int v = 0;
            int offset = 0;
            do {
                v += v + decoder.readBit(p[offset]);
                offset++;
            } while (p[offset] > 0);
            return v;
        }

        public void dequantSubblock(short dc, short ac, Short Dc) {
            short[] adjustedValues = new short[16];

            adjustedValues[0] = (short) (tokens[0] * dc);
            for (int i = 1; i < 16; i++)
                adjustedValues[i] = (short) (tokens[i] * ac);

            if (Dc != null)
                adjustedValues[0] = Dc;

            residue = VP8DCT.decodeDCT(adjustedValues);

        }
    }

    public void put(int mbRow, int mbCol, Picture p) {
        byte[] luma = p.getPlaneData(0);
        byte[] cb = p.getPlaneData(1);
        byte[] cr = p.getPlaneData(2);
        int strideLuma = p.getPlaneWidth(0);
        int strideChroma = p.getPlaneWidth(1);
        for (int lumaRow = 0; lumaRow < 4; lumaRow++)
            for (int lumaCol = 0; lumaCol < 4; lumaCol++)
                for (int lumaPRow = 0; lumaPRow < 4; lumaPRow++)
                    for (int lumaPCol = 0; lumaPCol < 4; lumaPCol++) {
                        int y = (mbRow << 4) + (lumaRow << 2) + lumaPRow;
                        int x = (mbCol << 4) + (lumaCol << 2) + lumaPCol;
                        if (x >= strideLuma || y >= luma.length / strideLuma)
                            continue;

                        int yy = ySubblocks[lumaRow][lumaCol].val[lumaPRow * 4 + lumaPCol];
                        luma[strideLuma * y + x] = (byte) (yy - 128);
                    }

        for (int chromaRow = 0; chromaRow < 2; chromaRow++)
            for (int chromaCol = 0; chromaCol < 2; chromaCol++)
                for (int chromaPRow = 0; chromaPRow < 4; chromaPRow++)
                    for (int chromaPCol = 0; chromaPCol < 4; chromaPCol++) {
                        int y = (mbRow << 3) + (chromaRow << 2) + chromaPRow;
                        int x = (mbCol << 3) + (chromaCol << 2) + chromaPCol;
                        if (x >= strideChroma || y >= cb.length / strideChroma)
                            continue;

                        int u = uSubblocks[chromaRow][chromaCol].val[chromaPRow * 4 + chromaPCol];
                        int v = vSubblocks[chromaRow][chromaCol].val[chromaPRow * 4 + chromaPCol];
                        cb[strideChroma * y + x] = (byte) (u - 128);
                        cr[strideChroma * y + x] = (byte) (v - 128);
                    }
    }
}