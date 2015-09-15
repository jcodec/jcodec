package org.jcodec.codecs.h264.decode;

import org.jcodec.codecs.h264.H264Const;
import org.jcodec.codecs.h264.decode.aso.Mapper;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.common.model.Picture8Bit;

/**
 * A decoder for I16x16 macroblocks
 * 
 * @author The JCodec project
 */
public class MBlockDecoderIntraNxN extends MBlockDecoderBase {
    private Mapper mapper;

    public MBlockDecoderIntraNxN(Mapper mapper, BitstreamParser parser, SliceHeader sh, DeblockerInput di, int poc, DecoderState decoderState) {
        super(parser, sh, di, poc, decoderState);
        this.mapper = mapper;
    }

    public void decode(int mbIndex, MBType prevMbType, Picture8Bit mb) {
        int mbX = mapper.getMbX(mbIndex);
        int mbY = mapper.getMbY(mbIndex);
        int mbAddr = mapper.getAddress(mbIndex);
        boolean leftAvailable = mapper.leftAvailable(mbIndex);
        boolean topAvailable = mapper.topAvailable(mbIndex);
        boolean topLeftAvailable = mapper.topLeftAvailable(mbIndex);
        boolean topRightAvailable = mapper.topRightAvailable(mbIndex);

        boolean transform8x8Used = false;
        if (s.transform8x8) {
            transform8x8Used = parser.readTransform8x8Flag(leftAvailable, topAvailable,
                    s.leftMBType, s.topMBType[mbX], s.tf8x8Left,
                    s.tf8x8Top[mbX]);
        }

        int[] lumaModes;
        if (!transform8x8Used) {
            lumaModes = new int[16];
            for (int i = 0; i < 16; i++) {
                int blkX = H264Const.MB_BLK_OFF_LEFT[i];
                int blkY = H264Const.MB_BLK_OFF_TOP[i];
                lumaModes[i] = parser.readPredictionI4x4Block(leftAvailable, topAvailable,
                        s.leftMBType, s.topMBType[mbX], blkX, blkY, mbX);
            }
        } else {
            lumaModes = new int[4];
            for (int i = 0; i < 4; i++) {
                int blkX = (i & 1) << 1;
                int blkY = i & 2;
                lumaModes[i] = parser.readPredictionI4x4Block(leftAvailable, topAvailable,
                        s.leftMBType, s.topMBType[mbX], blkX, blkY, mbX);
                s.i4x4PredLeft[blkY + 1] = s.i4x4PredLeft[blkY];
                s.i4x4PredTop[(mbX << 2) + blkX + 1] = s.i4x4PredTop[(mbX << 2) + blkX];
            }
        }
        int chromaMode = parser.readChromaPredMode(mbX, leftAvailable, topAvailable);

        int codedBlockPattern = parser.readCodedBlockPatternIntra(leftAvailable,
                topAvailable, s.leftCBPLuma | (s.leftCBPChroma << 4), s.topCBPLuma[mbX]
                        | (s.topCBPChroma[mbX] << 4), s.leftMBType, s.topMBType[mbX]);

        int cbpLuma = codedBlockPattern & 0xf;
        int cbpChroma = codedBlockPattern >> 4;

        if (cbpLuma > 0 || cbpChroma > 0) {
            s.qp = (s.qp + parser.readMBQpDelta(prevMbType) + 52) % 52;
        }
        di.mbQps[0][mbAddr] = s.qp;

        int[][] lumaResidual = transform8x8Used ? new int[4][64] : new int[16][16];
        residualLuma(leftAvailable, topAvailable, mbX, mbY, codedBlockPattern, MBType.I_NxN, transform8x8Used,
                s.tf8x8Left, s.tf8x8Top[mbX], lumaResidual);

        if (!transform8x8Used) {
            for (int i = 0; i < 16; i++) {
                int blkX = (i & 3) << 2;
                int blkY = i & ~3;

                int bi = H264Const.BLK_INV_MAP[i];
                boolean trAvailable = ((bi == 0 || bi == 1 || bi == 4) && topAvailable)
                        || (bi == 5 && topRightAvailable) || bi == 2 || bi == 6 || bi == 8 || bi == 9 || bi == 10
                        || bi == 12 || bi == 14;

                Intra4x4PredictionBuilder.predictWithMode(lumaModes[bi], lumaResidual[bi], blkX == 0 ? leftAvailable
                        : true, blkY == 0 ? topAvailable : true, trAvailable, s.leftRow[0],
                        s.topLine[0], s.topLeft[0], (mbX << 4), blkX, blkY, mb.getPlaneData(0));
            }
        } else {
            for (int i = 0; i < 4; i++) {
                int blkX = (i & 1) << 1;
                int blkY = i & 2;

                boolean trAvailable = (i == 0 && topAvailable) || (i == 1 && topRightAvailable) || i == 2;
                boolean tlAvailable = i == 0 ? topLeftAvailable : (i == 1 ? topAvailable : (i == 2 ? leftAvailable
                        : true));

                Intra8x8PredictionBuilder.predictWithMode(lumaModes[i], lumaResidual[i], blkX == 0 ? leftAvailable
                        : true, blkY == 0 ? topAvailable : true, tlAvailable, trAvailable, s.leftRow[0],
                        s.topLine[0], s.topLeft[0], (mbX << 4), blkX << 2, blkY << 2, mb
                                .getPlaneData(0));
            }
        }

        decodeChroma(cbpChroma, chromaMode, mbX, mbY, leftAvailable, topAvailable, mb, s.qp,
                MBType.I_NxN);

        di.mbTypes[mbAddr] = s.topMBType[mbX] = s.leftMBType = MBType.I_NxN;
        // System.out.println("idx: " + mbIndex + ", addr: " + address);
        s.topCBPLuma[mbX] = s.leftCBPLuma = cbpLuma;
        s.topCBPChroma[mbX] = s.leftCBPChroma = cbpChroma;
        s.tf8x8Left = s.tf8x8Top[mbX] = transform8x8Used;
        di.tr8x8Used[mbAddr] = transform8x8Used;

        MBlockDecoderUtils.collectChromaPredictors(s, mb, mbX);

        MBlockDecoderUtils.saveMvsIntra(di, mbX, mbY);
        MBlockDecoderUtils.saveVectIntra(s, mapper.getMbX(mbAddr));
    }
}
