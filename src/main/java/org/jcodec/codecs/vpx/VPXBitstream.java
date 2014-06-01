package org.jcodec.codecs.vpx;

import static org.jcodec.codecs.vpx.VPXConst.BLK_TYPE_DCT15;
import static org.jcodec.codecs.vpx.VPXConst.BLK_TYPE_DCT16;
import static org.jcodec.codecs.vpx.VPXConst.BLK_TYPE_UV;
import static org.jcodec.codecs.vpx.VPXConst.BLK_TYPE_WHT;

import org.jcodec.codecs.common.biari.VPxBooleanEncoder;
import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class VPXBitstream {

    public static final int[] coeffBandMapping = { 0, 1, 2, 3, 6, 4, 5, 6, 6, 6, 6, 6, 6, 6, 6, 7 };

    private int[][][][] tokenBinProbs;
    private int whtNzLeft = 0;
    private int[] whtNzTop;
    private int[][] dctNzLeft = new int[][] { new int[4], new int[2], new int[2] };
    private int[][] dctNzTop;

    public VPXBitstream(int[][][][] tokenBinProbs, int mbWidth) {
        this.tokenBinProbs = tokenBinProbs;

        whtNzTop = new int[mbWidth];
        dctNzTop = new int[][] { new int[mbWidth << 2], new int[mbWidth << 1], new int[mbWidth << 1] };
    }

    public void encodeCoeffsWHT(VPxBooleanEncoder bc, int[] coeffs, int mbX) {
        int nCoeff = fastCountCoeffWHT(coeffs);
        encodeCoeffs(bc, coeffs, 0, nCoeff, BLK_TYPE_WHT, (mbX == 0 || whtNzLeft <= 0 ? 0 : 1)
                + (whtNzTop[mbX] > 0 ? 1 : 0));
        whtNzLeft = nCoeff;
        whtNzTop[mbX] = nCoeff;
    }
    
    public void resetWHTPred() {
        whtNzLeft = 0;
    }

    public void encodeCoeffsDCT15(VPxBooleanEncoder bc, int[] coeffs, int mbX, int blkX, int blkY) {
        int nCoeff = countCoeff(coeffs, 16);
        int blkAbsX = (mbX << 2) + blkX;
        encodeCoeffs(bc, coeffs, 1, nCoeff, BLK_TYPE_DCT15, (blkAbsX == 0 || dctNzLeft[0][blkY] <= 0 ? 0 : 1)
                + (dctNzTop[0][blkAbsX] > 0 ? 1 : 0));
        dctNzLeft[0][blkY] = Math.max(nCoeff - 1, 0);
        dctNzTop[0][blkAbsX] = Math.max(nCoeff - 1, 0);
    }

    public void encodeCoeffsDCT16(VPxBooleanEncoder bc, int[] coeffs, int mbX, int blkX, int blkY) {
        int nCoeff = countCoeff(coeffs, 16);
        int blkAbsX = (mbX << 2) + blkX;
        encodeCoeffs(bc, coeffs, 0, nCoeff, BLK_TYPE_DCT16, (blkAbsX == 0 || dctNzLeft[0][blkY] <= 0 ? 0 : 1)
                + (dctNzTop[0][blkAbsX] > 0 ? 1 : 0));
        dctNzLeft[0][blkY] = nCoeff;
        dctNzTop[0][blkAbsX] = nCoeff;
    }

    public void encodeCoeffsDCTUV(VPxBooleanEncoder bc, int[] coeffs, int comp, int mbX, int blkX, int blkY) {
        int nCoeff = countCoeff(coeffs, 16);
        int blkAbsX = (mbX << 1) + blkX;
        encodeCoeffs(bc, coeffs, 0, nCoeff, BLK_TYPE_UV, (blkAbsX == 0 || dctNzLeft[comp][blkY] <= 0 ? 0 : 1)
                + (dctNzTop[comp][blkAbsX] > 0 ? 1 : 0));
        dctNzLeft[comp][blkY] = nCoeff;
        dctNzTop[comp][blkAbsX] = nCoeff;
    }

    public void encodeCoeffsEmpty(VPxBooleanEncoder bc, int mbX, int blkX, int blkY, int blkType, int firstCoeff, int comp) {
        int blkAbsX = (mbX << 2) + blkX;
        int ctx = (blkAbsX == 0 || dctNzLeft[comp][blkY] <= 0 ? 0 : 1) + (dctNzTop[comp][blkAbsX] > 0 ? 1 : 0);
        int[] probs = tokenBinProbs[blkType][coeffBandMapping[firstCoeff]][ctx];
        bc.writeBit(probs[0], 0);
        dctNzLeft[comp][blkY] = dctNzTop[comp][blkAbsX] = 0;
    }

    /**
     * Encodes DCT/WHT coefficients into the provided instance of a boolean
     * encoder
     * 
     * @param bc
     * @param coeffs
     */
    public void encodeCoeffs(VPxBooleanEncoder bc, int[] coeffs, int firstCoeff, int nCoeff, int blkType, int ctx) {
        boolean prevZero = false;

        int i;
        for (i = firstCoeff; i < nCoeff; i++) {
            int[] probs = tokenBinProbs[blkType][coeffBandMapping[i]][ctx];

            int coeffAbs = MathUtil.abs(coeffs[i]);
            if (!prevZero)
                bc.writeBit(probs[0], 1);
            if (coeffAbs == 0) {
                bc.writeBit(probs[1], 0);
                ctx = 0;
            } else {
                bc.writeBit(probs[1], 1);
                if (coeffAbs == 1) {
                    bc.writeBit(probs[2], 0);
                    ctx = 1;
                } else {
                    ctx = 2;
                    bc.writeBit(probs[2], 1);
                    if (coeffAbs <= 4) {
                        bc.writeBit(probs[3], 0);
                        if (coeffAbs == 2)
                            bc.writeBit(probs[4], 0);
                        else {
                            bc.writeBit(probs[4], 1);
                            bc.writeBit(probs[5], coeffAbs - 3);
                        }
                    } else {
                        bc.writeBit(probs[3], 1);
                        if (coeffAbs <= 10) {
                            bc.writeBit(probs[6], 0);
                            if (coeffAbs <= 6) {
                                bc.writeBit(probs[7], 0);
                                bc.writeBit(159, coeffAbs - 5);
                            } else {
                                bc.writeBit(probs[7], 1);
                                int d = coeffAbs - 7;
                                bc.writeBit(165, d >> 1);
                                bc.writeBit(145, d & 1);
                            }
                        } else {
                            bc.writeBit(probs[6], 1);
                            if (coeffAbs <= 34) {
                                bc.writeBit(probs[8], 0);
                                if (coeffAbs <= 18) {
                                    bc.writeBit(probs[9], 0);
                                    writeCat3Ext(bc, coeffAbs);
                                } else {
                                    bc.writeBit(probs[9], 1);
                                    writeCat4Ext(bc, coeffAbs);
                                }
                            } else {
                                bc.writeBit(probs[8], 1);
                                if (coeffAbs <= 66) {
                                    bc.writeBit(probs[10], 0);
                                    writeCatExt(bc, coeffAbs, 35, VPXConst.probCoeffExtCat5);
                                } else {
                                    bc.writeBit(probs[10], 1);
                                    writeCatExt(bc, coeffAbs, 67, VPXConst.probCoeffExtCat6);
                                }
                            }
                        }
                    }
                }
                bc.writeBit(128, MathUtil.sign(coeffs[i]));
            }

            prevZero = coeffAbs == 0;
        }
        if (nCoeff < 16) {
            int[] probs = tokenBinProbs[blkType][coeffBandMapping[i]][ctx];
            bc.writeBit(probs[0], 0);
        }
    }

    private static void writeCat3Ext(VPxBooleanEncoder bc, int coeff) {
        int d = coeff - 11;
        bc.writeBit(173, d >> 2);
        bc.writeBit(148, (d >> 1) & 1);
        bc.writeBit(140, d & 1);
    }

    private static void writeCat4Ext(VPxBooleanEncoder bc, int coeff) {
        int d = coeff - 19;
        bc.writeBit(176, d >> 3);
        bc.writeBit(155, (d >> 2) & 1);
        bc.writeBit(140, (d >> 1) & 1);
        bc.writeBit(135, d & 1);
    }

    private static final void writeCatExt(VPxBooleanEncoder bc, int coeff, int catOff, int[] cat) {
        int d = coeff - catOff;
        for (int b = cat.length - 1, i = 0; b >= 0; b--) {
            bc.writeBit(cat[i++], (d >> b) & 1);
        }
    }

    /**
     * Counts number of non-zero coefficients for a WHT block, with shortcut as
     * most of them are likely to be non-zero
     * 
     * @param coeffs
     * @return
     */
    private int fastCountCoeffWHT(int[] coeffs) {
        if (coeffs[15] != 0)
            return 16;
        else
            return countCoeff(coeffs, 15);
    }

    /**
     * Counts number of non-zero coefficients
     * 
     * @param coeffs
     * @param nCoeff
     * @return
     */
    private int countCoeff(int[] coeffs, int nCoeff) {
        while (nCoeff > 0) {
            --nCoeff;
            if (coeffs[nCoeff] != 0)
                return nCoeff + 1;
        }
        return nCoeff;
    }
}