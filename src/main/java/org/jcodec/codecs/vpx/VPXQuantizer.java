package org.jcodec.codecs.vpx;

import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class VPXQuantizer {

    private int y1_dc_delta_q;
    private int uv_dc_delta_q;
    private int uv_ac_delta_q;
    private int y2_dc_delta_q;
    private int y2_ac_delta_q;

    public final void quantizeY(int[] coeffs, int qp) {
        int factDC = MathUtil.clip(VPXConst.dc_qlookup[qp + y1_dc_delta_q], 8, 132);
        int invFactAC = 8192 / MathUtil.clip(VPXConst.ac_qlookup[qp], 8, 132);
        quantize(coeffs, factDC, invFactAC);
    }

    public final void quantizeUV(int[] coeffs, int qp) {
        int factDC = MathUtil.clip(VPXConst.dc_qlookup[qp + uv_dc_delta_q], 8, 132);
        int invFactAC = 8192 / MathUtil.clip(VPXConst.ac_qlookup[qp + uv_ac_delta_q], 8, 132);
        quantize(coeffs, factDC, invFactAC);
    }

    public final void quantizeY2(int[] coeffs, int qp) {
        int factDC = MathUtil.clip(VPXConst.dc_qlookup[qp + y2_dc_delta_q] * 2, 8, 132);
        int invFactAC = 8192 / MathUtil.clip(VPXConst.ac_qlookup[qp + y2_ac_delta_q] * 155 / 100, 8, 132);
        quantize(coeffs, factDC, invFactAC);
    }

    private final void quantize(int[] coeffs, int factDC, int invFactAC) {
        coeffs[0] /= factDC;
        for (int i = 1; i < 15; i++)
            coeffs[i] = (coeffs[i] * invFactAC) >> 13;
    }

    public final void dequantizeY(int[] coeffs, int qp) {
        int factDC = MathUtil.clip(VPXConst.dc_qlookup[qp + y1_dc_delta_q], 8, 132);
        int factAC = MathUtil.clip(VPXConst.ac_qlookup[qp], 8, 132);
        dequantize(coeffs, factDC, factAC);
    }

    public final void dequantizeUV(int[] coeffs, int qp) {
        int factDC = MathUtil.clip(VPXConst.dc_qlookup[qp + uv_dc_delta_q], 8, 132);
        int factAC = MathUtil.clip(VPXConst.ac_qlookup[qp + uv_ac_delta_q], 8, 132);
        dequantize(coeffs, factDC, factAC);
    }

    public final void dequantizeY2(int[] coeffs, int qp) {
        int factDC = MathUtil.clip(VPXConst.dc_qlookup[qp + y2_dc_delta_q] * 2, 8, 132);
        int factAC = MathUtil.clip(VPXConst.ac_qlookup[qp + y2_ac_delta_q] * 155 / 100, 8, 132);
        dequantize(coeffs, factDC, factAC);
    }

    private final void dequantize(int[] coeffs, int factDC, int factAC) {
        coeffs[0] *= factDC;
        for (int i = 1; i < 15; i++)
            coeffs[i] *= factAC;
    }
}