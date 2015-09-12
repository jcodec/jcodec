package org.jcodec.codecs.h264.decode;

import static org.jcodec.codecs.h264.decode.CoeffTransformer.reorderDC4x4;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.collectPredictors;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.saveMvsIntra;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.saveVectIntra;

import org.jcodec.codecs.h264.H264Const;
import org.jcodec.codecs.h264.decode.aso.Mapper;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.model.Picture8Bit;

/**
 * A decoder for I16x16 macroblocks
 * 
 * @author The JCodec project
 */
public class MBlockDecoderIntra16x16 extends MBlockDecoderBase {

    private Mapper mapper;

    public MBlockDecoderIntra16x16(Mapper mapper, BitstreamParser parser, SliceHeader sh, DeblockerInput di, int poc, DecoderState sharedState) {
        super(parser, sh, di, poc, sharedState);
        this.mapper = mapper;
    }

    public void decode(BitReader reader, int mbType, int mbIndex, MBType prevMbType, Picture8Bit mb) {

        int mbX = mapper.getMbX(mbIndex);
        int mbY = mapper.getMbY(mbIndex);
        int address = mapper.getAddress(mbIndex);

        int cbpChroma = (mbType / 4) % 3;
        int cbpLuma = (mbType / 12) * 15;

        boolean leftAvailable = mapper.leftAvailable(mbIndex);
        boolean topAvailable = mapper.topAvailable(mbIndex);

        int chromaPredictionMode = parser.readChromaPredMode(reader, mbX, leftAvailable, topAvailable);
        int mbQPDelta = parser.readMBQpDelta(reader, prevMbType);
        sharedState.qp = (sharedState.qp + mbQPDelta + 52) % 52;
        di.mbQps[0][address] = sharedState.qp;

        int[][] residual = new int[16][16];

        residualLumaI16x16(reader, leftAvailable, topAvailable, mbX, mbY, cbpLuma, residual);

        Intra16x16PredictionBuilder.predictWithMode(mbType % 4, residual, leftAvailable, topAvailable,
                sharedState.leftRow[0], sharedState.topLine[0], sharedState.topLeft[0], mbX << 4, mb.getPlaneData(0));

        decodeChroma(reader, cbpChroma, chromaPredictionMode, mbX, mbY, leftAvailable, topAvailable, mb,
                sharedState.qp, MBType.I_16x16);
        di.mbTypes[address] = sharedState.topMBType[mbX] = sharedState.leftMBType = MBType.I_16x16;
        // System.out.println("idx: " + mbIndex + ", addr: " + address);
        sharedState.topCBPLuma[mbX] = sharedState.leftCBPLuma = cbpLuma;
        sharedState.topCBPChroma[mbX] = sharedState.leftCBPChroma = cbpChroma;
        sharedState.tf8x8Left = sharedState.tf8x8Top[mbX] = false;

        collectPredictors(sharedState, mb, mbX);
        saveMvsIntra(di, mbX, mbY);
        saveVectIntra(sharedState, mapper.getMbX(mbIndex));
    }

    private void residualLumaI16x16(BitReader reader, boolean leftAvailable, boolean topAvailable, int mbX, int mbY,
            int cbpLuma, int[][] residualOut) {
        int[] dc = new int[16];
        parser.read16x16DC(leftAvailable, topAvailable, mbX, dc);

        CoeffTransformer.invDC4x4(dc);
        CoeffTransformer.dequantizeDC4x4(dc, sharedState.qp);
        reorderDC4x4(dc);

        for (int i = 0; i < 16; i++) {
            int[] ac = residualOut[i];
            int blkOffLeft = H264Const.MB_BLK_OFF_LEFT[i];
            int blkOffTop = H264Const.MB_BLK_OFF_TOP[i];
            int blkX = (mbX << 2) + blkOffLeft;
            int blkY = (mbY << 2) + blkOffTop;

            if ((cbpLuma & (1 << (i >> 2))) != 0) {
                parser.read16x16AC(leftAvailable, topAvailable, mbX, cbpLuma, ac, blkOffLeft, blkOffTop, blkX, blkY);
                CoeffTransformer.dequantizeAC(ac, sharedState.qp);
            } else {
                if (!sh.pps.entropy_coding_mode_flag)
                    parser.setZeroCoeff(0, blkX, blkOffTop);
            }
            ac[0] = dc[i];
            CoeffTransformer.idct4x4(ac);
        }
    }
}