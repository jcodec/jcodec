package org.jcodec.codecs.h264.decode;

import static org.jcodec.codecs.h264.decode.CoeffTransformer.reorderDC4x4;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.collectPredictors;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.saveMvsIntra;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.saveVectIntra;

import org.jcodec.codecs.h264.decode.aso.Mapper;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.common.model.Picture8Bit;

/**
 * A decoder for I16x16 macroblocks
 * 
 * @author The JCodec project
 */
public class MBlockDecoderIntra16x16 extends MBlockDecoderBase {

    private Mapper mapper;

    public MBlockDecoderIntra16x16(Mapper mapper, SliceHeader sh, DeblockerInput di, int poc,
            DecoderState decoderState) {
        super(sh, di, poc, decoderState);
        this.mapper = mapper;
    }

    public void decode(MBlock mBlock, Picture8Bit mb) {
        int mbX = mapper.getMbX(mBlock.mbIdx);
        int mbY = mapper.getMbY(mBlock.mbIdx);
        int address = mapper.getAddress(mBlock.mbIdx);
        boolean leftAvailable = mapper.leftAvailable(mBlock.mbIdx);
        boolean topAvailable = mapper.topAvailable(mBlock.mbIdx);
        s.qp = (s.qp + mBlock.mbQPDelta + 52) % 52;
        di.mbQps[0][address] = s.qp;

        residualLumaI16x16(mBlock, leftAvailable, topAvailable, mbX, mbY);

        Intra16x16PredictionBuilder.predictWithMode(mBlock.luma16x16Mode, mBlock.ac[0], leftAvailable, topAvailable,
                s.leftRow[0], s.topLine[0], s.topLeft[0], mbX << 4, mb.getPlaneData(0));

        decodeChroma(mBlock, mbX, mbY, leftAvailable, topAvailable, mb, s.qp);
        di.mbTypes[address] = mBlock.curMbType;

        collectPredictors(s, mb, mbX);
        saveMvsIntra(di, mbX, mbY);
        saveVectIntra(s, mapper.getMbX(mBlock.mbIdx));
    }

    private void residualLumaI16x16(MBlock mBlock, boolean leftAvailable, boolean topAvailable, int mbX, int mbY) {
        CoeffTransformer.invDC4x4(mBlock.dc);
        CoeffTransformer.dequantizeDC4x4(mBlock.dc, s.qp);
        reorderDC4x4(mBlock.dc);

        for (int i = 0; i < 16; i++) {
            if ((mBlock.cbpLuma() & (1 << (i >> 2))) != 0) {
                CoeffTransformer.dequantizeAC(mBlock.ac[0][i], s.qp);
            }
            mBlock.ac[0][i][0] = mBlock.dc[i];
            CoeffTransformer.idct4x4(mBlock.ac[0][i]);
        }
    }
}