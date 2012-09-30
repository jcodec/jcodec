package org.jcodec.codecs.h264.decode;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Integer DCT 4x4 base implementation
 * 
 * @author Jay Codec
 * 
 */
public class CoeffTransformer {

    private int[] zigzagScan4x4 = new int[] { 0, 1, 4, 8, 5, 2, 3, 6, 9, 12, 13, 10, 7, 11, 14, 15 };
    private int[] fieldScan4x4 = new int[] { 0, 4, 1, 8, 12, 5, 9, 13, 2, 6, 10, 14, 3, 7, 11, 15 };
    private int[] zigzagScan8x8 = new int[] { 0, 1, 8, 16, 9, 2, 3, 10, 17, 24, 32, 25, 18, 11, 4, 5, 12, 19, 26, 33,
            40, 48, 41, 34, 27, 20, 13, 6, 7, 14, 21, 28, 35, 42, 49, 56, 57, 50, 43, 36, 29, 22, 15, 23, 30, 37, 44,
            51, 58, 59, 52, 45, 38, 31, 39, 46, 53, 60, 61, 54, 47, 55, 62, 63 };
    private int[] fieldScan8x8 = new int[] { 0, 8, 16, 1, 9, 24, 32, 17, 2, 25, 40, 48, 56, 33, 10, 3, 18, 41, 49, 57,
            26, 11, 4, 19, 34, 42, 50, 58, 27, 12, 5, 20, 35, 43, 51, 58, 28, 13, 6, 21, 36, 44, 52, 60, 29, 14, 22,
            37, 45, 53, 61, 30, 7, 15, 38, 46, 54, 62, 23, 31, 39, 47, 55, 63 };

    static int[][] dequantCoef = new int[][] { { 10, 13, 10, 13, 13, 16, 13, 16, 10, 13, 10, 13, 13, 16, 13, 16 },
            { 11, 14, 11, 14, 14, 18, 14, 18, 11, 14, 11, 14, 14, 18, 14, 18 },
            { 13, 16, 13, 16, 16, 20, 16, 20, 13, 16, 13, 16, 16, 20, 16, 20 },
            { 14, 18, 14, 18, 18, 23, 18, 23, 14, 18, 14, 18, 18, 23, 18, 23 },
            { 16, 20, 16, 20, 20, 25, 20, 25, 16, 20, 16, 20, 20, 25, 20, 25 },
            { 18, 23, 18, 23, 23, 29, 23, 29, 18, 23, 18, 23, 23, 29, 23, 29 } };

    private int[][] levelScale;

    public CoeffTransformer(int[][] scalingListMatrix) {
        buildLevelScale(scalingListMatrix);
    }

    /**
     * Inverce integer DCT transform for 4x4 block
     * 
     * @param scaled
     * @return
     */
    public int[] transformIDCT4x4(int[] scaled) {
        int[] trans = new int[16];

        // Horisontal
        for (int i = 0; i < 4; i++) {
            int e0 = scaled[i << 2] + scaled[(i << 2) + 2];
            int e1 = scaled[i << 2] - scaled[(i << 2) + 2];
            int e2 = (scaled[(i << 2) + 1] >> 1) - scaled[(i << 2) + 3];
            int e3 = scaled[(i << 2) + 1] + (scaled[(i << 2) + 3] >> 1);

            trans[(i << 2)] = e0 + e3;
            trans[(i << 2) + 1] = e1 + e2;
            trans[(i << 2) + 2] = e1 - e2;
            trans[(i << 2) + 3] = e0 - e3;
        }

        // Vertical
        for (int i = 0; i < 4; i++) {
            int g0 = trans[i] + trans[i + 8];
            int g1 = trans[i] - trans[i + 8];
            int g2 = (trans[i + 4] >> 1) - trans[i + 12];
            int g3 = trans[i + 4] + (trans[i + 12] >> 1);
            trans[i] = g0 + g3;
            trans[i + 4] = g1 + g2;
            trans[i + 8] = g1 - g2;
            trans[i + 12] = g0 - g3;
        }

        // normalize
        for (int i = 0; i < 16; i++) {
            trans[i] = (trans[i] + 32) >> 6;
        }

        return trans;
    }

    public int[] transformIHadamard4x4(int[] scaled) {
        int[] trans = new int[16];

        // Horisontal
        for (int i = 0; i < 4; i++) {
            int e0 = scaled[i << 2] + scaled[(i << 2) + 2];
            int e1 = scaled[i << 2] - scaled[(i << 2) + 2];
            int e2 = scaled[(i << 2) + 1] - scaled[(i << 2) + 3];
            int e3 = scaled[(i << 2) + 1] + scaled[(i << 2) + 3];

            trans[(i << 2)] = e0 + e3;
            trans[(i << 2) + 1] = e1 + e2;
            trans[(i << 2) + 2] = e1 - e2;
            trans[(i << 2) + 3] = e0 - e3;
        }

        // Vertical
        for (int i = 0; i < 4; i++) {
            int g0 = trans[i] + trans[i + 8];
            int g1 = trans[i] - trans[i + 8];
            int g2 = trans[i + 4] - trans[i + 12];
            int g3 = trans[i + 4] + trans[i + 12];
            trans[i] = g0 + g3;
            trans[i + 4] = g1 + g2;
            trans[i + 8] = g1 - g2;
            trans[i + 12] = g0 - g3;
        }

        return trans;
    }

    private void buildLevelScale(int[][] scalingListMatrix) {
        if (scalingListMatrix != null) {
            throw new UnsupportedOperationException("Custom scaling lists are not supported yet.");
        }

        levelScale = new int[6][];
        for (int i = 0; i < 6; i++) {
            levelScale[i] = new int[16];
            for (int j = 0; j < 16; j++) {
                levelScale[i][j] = 16 * dequantCoef[i][j];
            }
        }
    }

    public int[] rescaleBeforeIDCT4x4(int[] coeffs, int qp) {
        int[] scaled = new int[16];
        int group = qp % 6;

        if (qp >= 24) {
            int qbits = qp / 6 - 4;
            for (int i = 0; i < 16; i++)
                scaled[i] = (coeffs[i] * levelScale[group][i]) << qbits;
        } else {
            int qbits = 4 - qp / 6;
            int addition = 1 << (3 - qp / 6);
            for (int i = 0; i < 16; i++)
                scaled[i] = (coeffs[i] * levelScale[group][i] + addition) >> qbits;
        }

        return scaled;
    }

    public int[] reorderCoeffs(int[] coeffs) {
        int[] tab;
        if (coeffs.length == 16) {
            tab = zigzagScan4x4;
        } else if (coeffs.length == 64) {
            tab = zigzagScan8x8;
        } else
            throw new IllegalArgumentException("Coefficients array should be of either 16 or 64 length.");

        int[] result = new int[coeffs.length];
        for (int i = 0; i < coeffs.length; i++) {
            result[tab[i]] = coeffs[i];
        }

        return result;
    }

    public int[] rescaleAfterIHadamard4x4(int[] coeffs, int qp) {
        int[] scaled = new int[16];
        int group = qp % 6;

        if (qp >= 36) {
            int qbits = qp / 6 - 6;
            for (int i = 0; i < 16; i++)
                scaled[i] = (coeffs[i] * levelScale[group][0]) << qbits;
        } else {
            int qbits = 6 - qp / 6;
            int addition = 1 << (5 - qp / 6);
            for (int i = 0; i < 16; i++)
                scaled[i] = (coeffs[i] * levelScale[group][0] + addition) >> qbits;
        }

        return scaled;
    }

    public int[] transformIHadamard2x2(int[] tblock) {
        int[] block = new int[4];

        int t0, t1, t2, t3;

        t0 = tblock[0] + tblock[1];
        t1 = tblock[0] - tblock[1];
        t2 = tblock[2] + tblock[3];
        t3 = tblock[2] - tblock[3];

        block[0] = (t0 + t2);
        block[1] = (t1 + t3);
        block[2] = (t0 - t2);
        block[3] = (t1 - t3);

        return block;
    }

    public int[] rescaleAfterIHadamard2x2(int[] transformed, int qp) {

        int[] scaled = new int[4];
        int group = qp % 6;
        int shift = qp / 6;

        for (int i = 0; i < 4; i++) {
            scaled[i] = ((transformed[i] * levelScale[group][0]) << shift) >> 5;
        }

        return scaled;
    }
}
