package org.jcodec.codecs.vpx.vp9;

import static org.jcodec.codecs.vpx.vp9.Consts.BLOCK_16X16;
import static org.jcodec.codecs.vpx.vp9.Consts.BLOCK_16X32;
import static org.jcodec.codecs.vpx.vp9.Consts.BLOCK_16X8;
import static org.jcodec.codecs.vpx.vp9.Consts.BLOCK_32X16;
import static org.jcodec.codecs.vpx.vp9.Consts.BLOCK_32X32;
import static org.jcodec.codecs.vpx.vp9.Consts.BLOCK_32X64;
import static org.jcodec.codecs.vpx.vp9.Consts.BLOCK_4X4;
import static org.jcodec.codecs.vpx.vp9.Consts.BLOCK_4X8;
import static org.jcodec.codecs.vpx.vp9.Consts.BLOCK_64X32;
import static org.jcodec.codecs.vpx.vp9.Consts.BLOCK_64X64;
import static org.jcodec.codecs.vpx.vp9.Consts.BLOCK_8X16;
import static org.jcodec.codecs.vpx.vp9.Consts.BLOCK_8X4;
import static org.jcodec.codecs.vpx.vp9.Consts.BLOCK_8X8;
import static org.jcodec.codecs.vpx.vp9.Consts.BLOCK_INVALID;
import static org.jcodec.codecs.vpx.vp9.Consts.DCT_VAL_CAT1;
import static org.jcodec.codecs.vpx.vp9.Consts.DCT_VAL_CAT3;
import static org.jcodec.codecs.vpx.vp9.Consts.PARETO_TABLE;
import static org.jcodec.codecs.vpx.vp9.Consts.TOKEN_TREE;
import static org.jcodec.codecs.vpx.vp9.Consts.TX_4X4;
import static org.jcodec.codecs.vpx.vp9.Consts.blH;
import static org.jcodec.codecs.vpx.vp9.Consts.blW;
import static org.jcodec.codecs.vpx.vp9.Consts.cat_probs;
import static org.jcodec.codecs.vpx.vp9.Consts.coefband_4x4;
import static org.jcodec.codecs.vpx.vp9.Consts.coefband_8x8plus;
import static org.jcodec.codecs.vpx.vp9.Consts.extra_bits;
import static org.jcodec.codecs.vpx.vp9.Consts.maxTxLookup;

import org.jcodec.codecs.vpx.VPXBooleanDecoder;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Residual {

    private int[][][] coefs;

    public Residual(int[][][] coefs) {
        this.coefs = coefs;
    }

    protected Residual() {
    }

    public static Residual readResidual(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder, DecodingContext c,
            ModeInfo mode) {
        Residual ret = new Residual();
        ret.read(miCol, miRow, blSz, decoder, c, mode);
        return ret;
    }

    public void read(int miCol, int miRow, int blType, VPXBooleanDecoder decoder, DecodingContext c,
            ModeInfo modeInfo) {
        if (modeInfo.isSkip())
            return;
        int subXRound = (1 << c.getSubX()) - 1;
        int subYRound = (1 << c.getSubY()) - 1;

        int[][][] coefs = new int[3][][];
        for (int pl = 0; pl < 3; pl++) {
            int txSize = pl == 0 ? modeInfo.getTxSize()
                    : Consts.uv_txsize_lookup[blType][modeInfo.getTxSize()][c.getSubX()][c.getSubY()];
            int step4x4 = 1 << txSize;

            int n4w = 1 << blW[blType];
            int n4h = 1 << blH[blType];
            if (pl != 0) {
                n4w >>= c.getSubX();
                n4h >>= c.getSubY();
            }
            int extra4w = (miCol << 1) + n4w - ((c.getFrameWidth() + 3) >> 2);
            int extra4h = (miRow << 1) + n4h - ((c.getFrameHeight() + 3) >> 2);
            int startBlkX = miCol << 1;
            int startBlkY = miRow << 1;
            if (pl != 0) {
                extra4w = (extra4w + subXRound) >> c.getSubX();
                extra4h = (extra4h + subYRound) >> c.getSubY();
                startBlkX >>= c.getSubX();
                startBlkY >>= c.getSubY();
            }
            int max4w = n4w - (extra4w > 0 ? extra4w : 0);
            int max4h = n4h - (extra4h > 0 ? extra4h : 0);

            coefs[pl] = new int[n4w * n4h][];
            for (int y = 0; y < max4h; y += step4x4) {
                for (int x = 0; x < max4w; x += step4x4) {
                    int blkCol = startBlkX + x;
                    int blkRow = startBlkY + y;
                    int predMode;
                    if (pl == 0) {
                        predMode = modeInfo.getYMode();
                        if (blType < BLOCK_8X8)
                            predMode = ModeInfo.vect4get(modeInfo.getSubModes(), (y << 1) + x);
                    } else
                        predMode = modeInfo.getUvMode();
                    coefs[pl][x + n4w * y] = readOneTU(pl == 0 ? 0 : 1, blkCol, blkRow, txSize, modeInfo.isInter(),
                            predMode, decoder, c);
                }
            }
        }
        this.coefs = coefs;
    }

    public static int[][] blk_size_lookup = new int[][] { { BLOCK_INVALID, BLOCK_4X4, BLOCK_8X4 },
            { BLOCK_4X8, BLOCK_8X8, BLOCK_16X8 }, { BLOCK_8X16, BLOCK_16X16, BLOCK_32X16 },
            { BLOCK_16X32, BLOCK_32X32, BLOCK_64X32 }, { BLOCK_32X64, BLOCK_64X64, BLOCK_INVALID }, };

    public int[] readOneTU(int plane, int blkCol, int blkRow, int txSz, boolean isInter, int intraMode,
            VPXBooleanDecoder decoder, DecodingContext c) {
        int[] tokenCache = new int[16 << (txSz << 1)];
        int maxCoeff = 16 << (txSz << 1);
        boolean expectMoreCoefs = false;
        int txType = plane == 0 && !isInter ? Consts.intra_mode_to_tx_type_lookup[intraMode] : Consts.DCT_DCT;
        int[] scan = plane == 0 && !isInter ? Scan.vp9_scan_orders[txSz][txType][0]
                : Scan.vp9_default_scan_orders[txSz][0];
        int[] neighbors = plane == 0 && !isInter ? Scan.vp9_scan_orders[txSz][txType][2]
                : Scan.vp9_default_scan_orders[txSz][2];
        int[] coefs = new int[maxCoeff];
        int ctx = calcTokenContextCoef0(plane, txSz, blkCol, blkRow, c);
        for (int cf = 0; cf < maxCoeff; cf++) {
            int band = (txSz == TX_4X4) ? coefband_4x4[cf] : coefband_8x8plus[cf];
            int pos = scan[cf];
            int[] probs = c.getCoefProbs()[txSz][plane > 0 ? 1 : 0][isInter ? 1 : 0][band][ctx];

            if (!expectMoreCoefs) {
                boolean moreCoefs = decoder.readBit(probs[0]) == 1;
                if (!moreCoefs)
                    break;
            }
            int coef;
            if (decoder.readBit(probs[1]) == 0) {
                tokenCache[pos] = 0;
                expectMoreCoefs = true;
            } else {
                expectMoreCoefs = false;
                if (decoder.readBit(probs[2]) == 0) {
                    tokenCache[pos] = 1;
                    coef = 1;
                } else {
                    int token = decoder.readTree(TOKEN_TREE, PARETO_TABLE[probs[2] - 1]);
                    if (token < DCT_VAL_CAT1) {
                        coef = token;
                        if (token == Consts.TWO_TOKEN)
                            tokenCache[pos] = 2;
                        else
                            tokenCache[pos] = 3;
                    } else {
                        if (token < DCT_VAL_CAT3)
                            tokenCache[pos] = 4;
                        else
                            tokenCache[pos] = 5;
                        coef = readCoef(token, decoder, c);
                    }
                }
                int sign = decoder.readBitEq();
                coefs[pos] = sign == 1 ? -coef : coef;
            }
            ctx = (1 + tokenCache[neighbors[2 * cf + 2]] + tokenCache[neighbors[2 * cf + 3]]) >> 1;
            System.out.println("CTX: " + ctx);
        }
        return coefs;
    }

    private static int readCoef(int token, VPXBooleanDecoder decoder, DecodingContext c) {
        int cat = extra_bits[token][0];
        int numExtra = extra_bits[token][1];
        int coef = extra_bits[token][2];
        // if (token == DCT_VAL_CAT6) {
        // for (int bit = 0; bit < c.getBitDepth() - 8; bit++) {
        // int high_bit = decoder.readBit(255);
        //
        // coef += high_bit << (5 + c.getBitDepth() - bit);
        // }
        // }
        for (int bit = 0; bit < numExtra; bit++) {
            int coef_bit = decoder.readBit(cat_probs[cat][bit]);

            coef += coef_bit << (numExtra - 1 - bit);
        }
        return coef;
    }

    private static int calcTokenContextCoef0(int plane, int txSz, int blkCol, int blkRow, DecodingContext c) {
        int[][] aboveNonzeroContext = c.getAboveNonzeroContext();
        int[][] leftNonzeroContext = c.getLeftNonzeroContext();
        int subX = plane > 0 ? c.getSubX() : 0;
        int subY = plane > 0 ? c.getSubY() : 0;
        int max4x = (c.getMiFrameWidth() << 1) >> subX;
        int max4y = (c.getMiFrameHeight() << 1) >> subY;
        int tx4 = 1 << txSz;
        int aboveNz = 0;
        int leftNz = 0;
        for (int i = 0; i < tx4; i++) {
            if (blkCol + i < max4x)
                aboveNz |= aboveNonzeroContext[plane][blkCol + i];
            if (blkRow + i < max4y)
                leftNz |= leftNonzeroContext[plane][(blkRow + i) & 0xf];
        }
        return aboveNz + leftNz;
    }

    public int[][][] getCoefs() {
        return coefs;
    }
}
