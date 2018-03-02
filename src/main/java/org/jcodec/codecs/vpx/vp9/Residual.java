package org.jcodec.codecs.vpx.vp9;

import static org.jcodec.codecs.vpx.vp9.Consts.ADST_DCT;
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
import static org.jcodec.codecs.vpx.vp9.Consts.DCT_ADST;
import static org.jcodec.codecs.vpx.vp9.Consts.DCT_VAL_CAT6;
import static org.jcodec.codecs.vpx.vp9.Consts.PARETO_TABLE;
import static org.jcodec.codecs.vpx.vp9.Consts.TOKEN_TREE;
import static org.jcodec.codecs.vpx.vp9.Consts.TX_4X4;
import static org.jcodec.codecs.vpx.vp9.Consts.ZERO_TOKEN;
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
    
    public static Residual read(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder, Probabilities probStore,
            DecodingContext c, ModeInfo mode) {
        int[][][] coefs = new int[3][][];
        for (int pl = 0; pl < 3; pl++) {
            int subW = msb(blW[blSz] / c.getSubX()) - 2;
            int subH = msb(blH[blSz] / c.getSubY()) - 2;
            int uvBlSz = blk_size_lookup[subH][subW - subH + 1];
            int txSize = pl == 0 ? mode.getTxSize() : uvTxSize(c, mode.getTxSize(), blSz, uvBlSz);
            int step4x4 = 1 << txSize;
            int plBlSz = pl == 0 ? blSz : uvBlSz;

            int frameWPix = (c.getMiFrameWidth() << 3) >> c.getSubX();
            int frameHPix = (c.getMiFrameHeight() << 3) >> c.getSubY();
            int blX = (miCol << 3) >> c.getSubX();
            int blY = (miRow << 3) >> c.getSubY();

            coefs[pl] = new int[blH[plBlSz] * blW[plBlSz]][];
            for (int y = 0, blkIdx = 0; y < blH[plBlSz]; y += step4x4) {
                for (int x = 0; x < blW[plBlSz]; x += step4x4, blkIdx++) {
                    int posX = blX + (x << 2);
                    int posY = blY + (y << 2);
                    if (!mode.isSkip() && posX < frameWPix && posY < frameHPix) {
                        coefs[pl][blkIdx] = tokens(pl, posX, posY, txSize, blkIdx, mode.isInter(), decoder, probStore,
                                c);
                    }
                }
            }
        }
        return new Residual(coefs);
    }

    private static int msb(int v) {
        v &= 0xff;
        if ((v & 0xf0) != 0) {
            if ((v & 0xc0) != 0) {
                return 6 | (v >> 7);
            } else {
                return 4 | (v >> 5);
            }
        } else {
            if ((v & 0xc) != 0) {
                return 2 | (v >> 3);
            } else {
                return v >> 1;
            }
        }
    }

    public static int[][] blk_size_lookup = new int[][] { { BLOCK_INVALID, BLOCK_4X4, BLOCK_8X4 },
            { BLOCK_4X8, BLOCK_8X8, BLOCK_16X8 }, { BLOCK_8X16, BLOCK_16X16, BLOCK_32X16 },
            { BLOCK_16X32, BLOCK_32X32, BLOCK_64X32 }, { BLOCK_32X64, BLOCK_64X64, BLOCK_INVALID }, };

    private static int uvTxSize(DecodingContext c, int txSize, int blSz, int uvBlSz) {
        if (blSz < BLOCK_8X8)
            return TX_4X4;

        return Math.min(txSize, maxTxLookup[uvBlSz]);
    }

    public static int[] tokens(int plane, int startX, int startY, int txSz, int blockIdx, boolean isInter,
            VPXBooleanDecoder decoder, Probabilities probStore, DecodingContext c) {
        int maxCoeff = 16 << (txSz << 1);
        boolean expectMoreCoefs = true;
        int[] scan = c.getScan(plane, txSz, blockIdx);
        int txType = c.getTxType(plane, txSz, blockIdx);
        int[] coefs = new int[maxCoeff];
        for (int cf = 0; cf < maxCoeff; cf++) {
            int band = (txSz == TX_4X4) ? coefband_4x4[cf] : coefband_8x8plus[cf];
            int pos = scan[cf];
            if (!expectMoreCoefs) {
                boolean moreCoefs = readMoreCoefs(plane, pos, txSz, startX, startY, txType, band, isInter, decoder,
                        probStore, c);
                if (!moreCoefs)
                    break;
            }
            int token = readToken(plane, pos, txSz, startX, startY, txType, band, isInter, decoder, probStore, c);
            if (token == ZERO_TOKEN) {
                expectMoreCoefs = true;
                coefs[pos] = 0;
            } else {
                int coef = readCoef(token, decoder, c);
                int sign = decoder.readBitEq();
                coefs[pos] = sign == 1 ? -coef : coef;
                expectMoreCoefs = false;
            }
        }
        return coefs;
    }

    private static int readCoef(int token, VPXBooleanDecoder decoder, DecodingContext c) {
        int cat = extra_bits[token][0];
        int numExtra = extra_bits[token][1];
        int coef = extra_bits[token][2];
        if (token == DCT_VAL_CAT6) {
            for (int bit = 0; bit < c.getBitDepth() - 8; bit++) {
                int high_bit = decoder.readBit(255);

                coef += high_bit << (5 + c.getBitDepth() - bit);
            }
        }
        for (int bit = 0; bit < numExtra; bit++) {
            int coef_bit = decoder.readBit(cat_probs[cat][bit]);

            coef += coef_bit << (numExtra - 1 - bit);
        }
        return coef;
    }

    private static int pareto(int bin, int prob) {
        if (bin < 2) {
            return prob;
        }
        int x = (prob - 1) / 2;
        if ((prob & 1) != 0)
            return PARETO_TABLE[x][bin - 2];
        else
            return (PARETO_TABLE[x][bin - 2] + PARETO_TABLE[x + 1][bin - 2]) >> 1;
    }

    private static int readToken(int plane, int coefi, int txSz, int posX, int posY, int txType, int band,
            boolean isInter, VPXBooleanDecoder decoder, Probabilities probStore, DecodingContext c) {
        int ctx = calcTokenContext(plane, coefi, txSz, posX, posY, txType, c);
        int[][][][][][] probs = probStore.getCoefProbs();
        int prob0 = pareto(0, probs[txSz][plane > 0 ? 1 : 0][isInter ? 1 : 0][band][ctx][1]);
        int prob1 = pareto(1, probs[txSz][plane > 0 ? 1 : 0][isInter ? 1 : 0][band][ctx][2]);
        return decoder.readTree(TOKEN_TREE, prob0, prob1);
    }

    private static boolean readMoreCoefs(int plane, int coefi, int txSz, int posX, int posY, int txType, int band,
            boolean isInter, VPXBooleanDecoder decoder, Probabilities probStore, DecodingContext c) {
        int ctx = calcTokenContext(plane, coefi, txSz, posX, posY, txType, c);
        int[][][][][][] probs = probStore.getCoefProbs();

        return decoder.readBit(probs[txSz][plane > 0 ? 1 : 0][isInter ? 1 : 0][band][ctx][0]) == 1;
    }

    private static int calcTokenContext(int plane, int coefi, int txSz, int posX, int posY, int txType,
            DecodingContext c) {
        if (coefi == 0) {
            int[][] aboveNonzeroContext = c.getAboveNonzeroContext();
            int[][] leftNonzeroContext = c.getLeftNonzeroContext();
            int subX = plane > 0 ? c.getSubX() : 0;
            int subY = plane > 0 ? c.getSubY() : 0;
            int max4x = (c.getMiFrameWidth() << 1) >> subX;
            int max4y = (c.getMiFrameHeight() << 1) >> subY;
            int tx4 = 1 << txSz;
            int pos4x = posX >> 2;
            int pos4y = posY >> 2;
            int aboveNz = 0;
            int leftNz = 0;
            for (int i = 0; i < tx4; i++) {
                if (pos4x + i < max4x)
                    aboveNz |= aboveNonzeroContext[plane][pos4x + i];
                if (pos4y + i < max4y)
                    leftNz |= leftNonzeroContext[plane][pos4y + i];
            }
            return aboveNz + leftNz;
        } else {
            int abovePos = 0;
            int leftPos = 0;
            if (coefi != 0) {
                txSz += 2;
                int y = coefi >> txSz;
                int x = coefi & (0x3f >> (6 - txSz));
                abovePos = ((y - 1) << txSz) + x;
                leftPos = (y << txSz) + x - 1;
                if (txType == DCT_ADST || x == 0) {
                    leftPos = abovePos;
                }
                if (txType == ADST_DCT || y == 0) {
                    abovePos = leftPos;
                }
            }

            int[] tokenCache = c.getTokenCache();

            return (1 + tokenCache[abovePos] + tokenCache[leftPos]) >> 1;
        }
    }

}
