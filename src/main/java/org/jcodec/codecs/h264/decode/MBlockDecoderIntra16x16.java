package org.jcodec.codecs.h264.decode;

import static org.jcodec.codecs.h264.decode.CoeffTransformer.reorderDC4x4;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.collectPredictors;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.saveMvsIntra;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.saveVectIntra;

import org.jcodec.codecs.h264.H264Const;
import org.jcodec.codecs.h264.decode.aso.Mapper;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;

/**
 * A decoder for I16x16 macroblocks
 * 
 * @author The JCodec project
 */
public class MBlockDecoderIntra16x16 extends MBlockDecoderBase {

    private Mapper mapper;

    public MBlockDecoderIntra16x16(Mapper mapper, BitstreamParser parser, SliceHeader sh, DeblockerInput di, int poc,
            DecoderState decoderState) {
        super(parser, sh, di, poc, decoderState);
        this.mapper = mapper;
    }

    public void decode(int mbType, int mbIndex, MBType prevMbType, Picture8Bit mb) {

        MBlock mBlock = new MBlock();

        readIntra16x16(mbType, mbIndex, prevMbType, mBlock);

        // RENDERING PART
        int mbX = mapper.getMbX(mbIndex);
        int mbY = mapper.getMbY(mbIndex);
        int address = mapper.getAddress(mbIndex);
        boolean leftAvailable = mapper.leftAvailable(mbIndex);
        boolean topAvailable = mapper.topAvailable(mbIndex);
        s.qp = (s.qp + mBlock.mbQPDelta + 52) % 52;
        di.mbQps[0][address] = s.qp;

        residualLumaI16x16(mBlock, leftAvailable, topAvailable, mbX, mbY);

        Intra16x16PredictionBuilder.predictWithMode(mbType % 4, mBlock.ac[0], leftAvailable, topAvailable,
                s.leftRow[0], s.topLine[0], s.topLeft[0], mbX << 4, mb.getPlaneData(0));

        decodeChroma(mBlock, mbX, mbY, leftAvailable, topAvailable, mb, s.qp, MBType.I_16x16);
        di.mbTypes[address] = s.topMBType[mbX] = s.leftMBType = MBType.I_16x16;
        // System.out.println("idx: " + mbIndex + ", addr: " + address);
        s.topCBPLuma[mbX] = s.leftCBPLuma = mBlock.cbpLuma();
        s.topCBPChroma[mbX] = s.leftCBPChroma = mBlock.cbpChroma();
        s.tf8x8Left = s.tf8x8Top[mbX] = false;

        collectPredictors(s, mb, mbX);
        saveMvsIntra(di, mbX, mbY);
        saveVectIntra(s, mapper.getMbX(mbIndex));
    }

    private void readIntra16x16(int mbType, int mbIndex, MBType prevMbType, MBlock mBlock) {
        int mbX = mapper.getMbX(mbIndex);
        int mbY = mapper.getMbY(mbIndex);
        boolean leftAvailable = mapper.leftAvailable(mbIndex);
        boolean topAvailable = mapper.topAvailable(mbIndex);
        mBlock.cbp((mbType / 12) * 15, (mbType / 4) % 3);
        mBlock.chromaPredictionMode = parser.readChromaPredMode(mbX, leftAvailable, topAvailable);
        mBlock.mbQPDelta = parser.readMBQpDelta(prevMbType);
        parser.read16x16DC(leftAvailable, topAvailable, mbX, mBlock.dc);
        for (int i = 0; i < 16; i++) {
            int blkOffLeft = H264Const.MB_BLK_OFF_LEFT[i];
            int blkOffTop = H264Const.MB_BLK_OFF_TOP[i];
            int blkX = (mbX << 2) + blkOffLeft;
            int blkY = (mbY << 2) + blkOffTop;

            if ((mBlock.cbpLuma() & (1 << (i >> 2))) != 0) {
                parser.read16x16AC(leftAvailable, topAvailable, mbX, mBlock.cbpLuma(), mBlock.ac[0][i], blkOffLeft,
                        blkOffTop, blkX, blkY);
            } else {
                if (!sh.pps.entropy_coding_mode_flag)
                    parser.setZeroCoeff(0, blkX, blkOffTop);
            }
        }

        if (s.chromaFormat != ColorSpace.MONO) {
            readChromaResidual(mBlock, leftAvailable, topAvailable, mbX, mBlock.cbpChroma(), MBType.I_16x16);
        }
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