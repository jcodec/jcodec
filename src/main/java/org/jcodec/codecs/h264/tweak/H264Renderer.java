package org.jcodec.codecs.h264.tweak;

import static org.jcodec.codecs.h264.decode.CoeffTransformer.reorderDC4x4;

import org.jcodec.codecs.h264.decode.CoeffTransformer;
import org.jcodec.common.tools.MathUtil;

public class H264Renderer {

    /**
     * Renders luma component of 16x16 DC predicted macroblock
     * @param lumaDC
     * @param lumaAC
     * @param mbLeft
     * @param mbTop
     * @param qp
     * @param out
     */
    public static void renderMacroblockI16x16LumaDC(int[] lumaDC, int[][] lumaAC, int[] mbLeft, int[] mbTop, int qp,
            int[] out) {
        int sum = 0;
        if (mbLeft != null) {
            for (int i = 15; i < 256; i += 16)
                sum += mbLeft[i];
        }

        if (mbTop != null) {
            for (int i = 0; i < 16; i++)
                sum += mbTop[i];
        }

        int pred;
        if (mbLeft != null && mbTop != null)
            pred = (sum + 16) >> 5;
        else if (mbLeft == null && mbTop == null)
            pred = 128;
        else
            pred = (sum + 8) >> 3;

        CoeffTransformer.invDC4x4(lumaDC);

        CoeffTransformer.dequantizeDC4x4(lumaDC, qp);
        reorderDC4x4(lumaDC);

        for (int i = 0; i < 16; i++) {
            if (lumaAC[i] != null) {
                CoeffTransformer.dequantizeAC(lumaAC[i], qp);
                lumaAC[i][0] = lumaDC[i];
                CoeffTransformer.idct4x4(lumaAC[i]);
                for (int j = 0, off = j << 4; j < 16; j++, off++)
                    out[off] = MathUtil.clip(lumaAC[i][j] + pred, 0, 255);
            } else {
                for (int j = 0, off = j << 4; j < 16; j++, off++)
                    out[off] = pred;
            }
        }
    }

    public static int predictChromaDCInside(int[] mbLeft, int[] mbTop, int ox, int oy) {

        int sum = 0;
        if (mbLeft != null) {
            for (int i = 0, off = 7 + (oy << 3); i < 4; i++, off++)
                sum += mbLeft[off];
        }

        if (mbTop != null) {
            for (int i = 0; i < 4; i++)
                sum += mbTop[i + ox];
        }

        int pred;
        if (mbLeft != null && mbTop != null)
            pred = (sum + 4) >> 3;
        else if (mbLeft == null && mbTop == null)
            pred = 128;
        else
            pred = (sum + 2) >> 2;

        return pred;
    }

    public static int predictDCTopBorder(int[] mbLeft, int[] mbTop, int ox, int oy) {

        int sum = 0;
        if (mbTop != null) {
            for (int i = 0; i < 4; i++)
                sum += mbTop[i + ox];
            return (sum + 2) >> 2;
        } else if (mbLeft != null) {
            for (int i = 0, off = 7 + (oy << 3); i < 4; i++, off++)
                sum += mbLeft[off];
            return (sum + 2) >> 2;
        } else
            return 128;
    }

    public static int predictDCLeftBorder(int[] mbLeft, int[] mbTop, int ox, int oy) {

        int sum = 0;
        if (mbLeft != null) {
            for (int i = 0, off = 7 + (oy << 3); i < 4; i++, off++)
                sum += mbLeft[off];
            return (sum + 2) >> 2;
        } else if (mbTop != null) {
            for (int i = 0; i < 4; i++)
                sum += mbTop[i + ox];
            return (sum + 2) >> 2;
        } else
            return 128;
    }

    /**
     * Renders chroma component of DC predicted intra macroblock
     * @param chromaDC
     * @param chromaAC
     * @param mbLeft
     * @param mbTop
     * @param qp
     * @param out
     */
    public static void renderMacroblockChromaDC(int[] chromaDC, int[][] chromaAC, int[] mbLeft, int[] mbTop,
            int qp, int[] out) {
        int[] pred = new int[] { predictChromaDCInside(mbLeft, mbTop, 0, 0), predictDCTopBorder(mbLeft, mbTop, 8, 0),
                predictDCLeftBorder(mbLeft, mbTop, 0, 8), predictChromaDCInside(mbLeft, mbTop, 8, 8) };

        CoeffTransformer.invDC2x2(chromaDC);
        CoeffTransformer.dequantizeDC2x2(chromaDC, qp);

        for (int i = 0; i < 4; i++) {
            if (chromaAC[i] != null) {
                CoeffTransformer.dequantizeAC(chromaAC[i], qp);
                chromaAC[i][0] = chromaDC[0];
                CoeffTransformer.idct4x4(chromaAC[i]);
                for (int j = 0, off = i << 4; j < 16; j++, off++) {
                    out[off] = MathUtil.clip(chromaAC[i][j] + pred[i], 0, 255);
                }
            } else {
                for (int j = 0, off = i << 4; j < 16; j++, off++) {
                    out[off] = pred[i];
                }
            }
        }
    }
    
    /**
     * Renders 4x4 DC predicted block ( part of a 
     * @param lumaDC
     * @param lumaAC
     * @param mbLeft
     * @param mbTop
     * @param qp
     * @param out
     */
    public static void renderMacroblockI4x4DC(int[] lumaDC, int[][] lumaAC, int[] mbLeft, int[] mbTop, int qp,
            int[] out) {
        int sum = 0;
        if (mbLeft != null) {
            for (int i = 15; i < 256; i += 16)
                sum += mbLeft[i];
        }

        if (mbTop != null) {
            for (int i = 0; i < 16; i++)
                sum += mbTop[i];
        }

        int pred;
        if (mbLeft != null && mbTop != null)
            pred = (sum + 16) >> 5;
        else if (mbLeft == null && mbTop == null)
            pred = 128;
        else
            pred = (sum + 8) >> 3;

        CoeffTransformer.invDC4x4(lumaDC);

        CoeffTransformer.dequantizeDC4x4(lumaDC, qp);
        reorderDC4x4(lumaDC);

        for (int i = 0; i < 16; i++) {
            if (lumaAC[i] != null) {
                CoeffTransformer.dequantizeAC(lumaAC[i], qp);
                lumaAC[i][0] = lumaDC[i];
                CoeffTransformer.idct4x4(lumaAC[i]);
                for (int j = 0, off = j << 4; j < 16; j++, off++)
                    out[off] = MathUtil.clip(lumaAC[i][j] + pred, 0, 255);
            } else {
                for (int j = 0, off = j << 4; j < 16; j++, off++)
                    out[off] = pred;
            }
        }
    }
            

    private void tweakDc16x16Chroma(int[] chromaDC, int[][] chromaAC, int[] mbLeft, int[] mbTop, boolean nLeftAvb,
            boolean nTopAvb) {

    }

    private void tweakDc16x16Luma(int[] lumaDC, int[][] lumaAC, int[] mbLeft, int[] mbTop, boolean nLeftAvb,
            boolean nTopAvb) {
        int[] out = new int[256];
        H264Renderer.renderMacroblockI16x16LumaDC(lumaDC, lumaAC, mbLeft, mbTop, 2, out);
        int dcL = 0, plus = 0, shift = 3;
        if (nTopAvb) {
            for (int i = 0; i < 16; i++)
                dcL += out[i];
            plus += 8;
            ++shift;
        }

        if (nLeftAvb) {
            if (mbLeft == null) {
                for (int i = 0; i < 256; i += 16)
                    dcL += out[i];
            } else {
                for (int i = 15; i < 256; i += 16)
                    dcL += mbLeft[i];
            }
            plus += 8;
            ++shift;
        }
        dcL = (dcL + plus) >> shift;

        for (int i = 0; i < lumaDC.length; i++) {
            lumaDC[i] += (dcL << 3) - 1024;
        }
    }

    public int[] TOP_BORDER = new int[] { 1, 4, 5 };
    public int[] LEFT_BORDER = new int[] { 2, 8, 10 };

    private void tweakDcNxN(int[][] lumaResidual, int[] lumaModes, boolean leftAvb, boolean topAvb) {

        if (!leftAvb && !topAvb) {
            if (lumaModes[0] == 2) {

            }
        }
        if (!leftAvb) {
            for (int i = 0; i < LEFT_BORDER.length; i++) {
                if (lumaModes[LEFT_BORDER[i]] == 2) {

                }

            }
        }
        if (!topAvb) {
            for (int i = 0; i < TOP_BORDER.length; i++) {
                if (lumaModes[TOP_BORDER[i]] == 2) {

                }
            }
        }
    }

}
