package org.jcodec.codecs.h264.decode;

import static org.jcodec.codecs.h264.H264Const.identityMapping4;
import static org.jcodec.codecs.h264.H264Const.PartPred.L0;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.calcMVPredictionMedian;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.collectPredictors;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.copyVect;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.debugPrint;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.saveMvs;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.savePrediction8x8;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.saveVect;
import static org.jcodec.codecs.h264.io.model.SliceType.P;

import java.util.Arrays;

import org.jcodec.codecs.h264.H264Const.PartPred;
import org.jcodec.codecs.h264.decode.aso.Mapper;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.common.model.Picture8Bit;

/**
 * A decoder for P skip macroblocks
 * 
 * @author The JCodec project
 */
public class MBlockSkipDecoder extends MBlockDecoderBase {
    private Mapper mapper;
    private MBlockDecoderBDirect bDirectDecoder;

    public MBlockSkipDecoder(Mapper mapper, BitstreamParser parser, MBlockDecoderBDirect bDirectDecoder,
            SliceHeader sh, DeblockerInput di, int poc, DecoderState sharedState) {
        super(parser, sh, di, poc, sharedState);
        this.mapper = mapper;
        this.bDirectDecoder = bDirectDecoder;
    }

    public void decodeSkip(Frame[][] refs, int mbIdx, Picture8Bit mb, SliceType sliceType) {
        int mbX = mapper.getMbX(mbIdx);
        int mbY = mapper.getMbY(mbIdx);
        int mbAddr = mapper.getAddress(mbIdx);

        int[][][] x = new int[2][16][3];
        PartPred[] pp = new PartPred[4];

        for (int i = 0; i < 16; i++)
            x[0][i][2] = x[1][i][2] = -1;

        if (sliceType == P) {
            predictPSkip(refs, mbX, mbY, mapper.leftAvailable(mbIdx), mapper.topAvailable(mbIdx),
                    mapper.topLeftAvailable(mbIdx), mapper.topRightAvailable(mbIdx), x, mb);
            Arrays.fill(pp, PartPred.L0);
        } else {
            bDirectDecoder.predictBDirect(refs, mbX, mbY, mapper.leftAvailable(mbIdx), mapper.topAvailable(mbIdx),
                    mapper.topLeftAvailable(mbIdx), mapper.topRightAvailable(mbIdx), x, pp, mb, identityMapping4);
            savePrediction8x8(sharedState, mbX, x[0], 0);
            savePrediction8x8(sharedState, mbX, x[1], 1);
        }

        decodeChromaSkip(refs, x, pp, mbX, mbY, mb);

        collectPredictors(sharedState, mb, mbX);

        saveMvs(di, x, mbX, mbY);
        di.mbTypes[mbAddr] = sharedState.topMBType[mbX] = sharedState.leftMBType = null;
        di.mbQps[0][mbAddr] = sharedState.qp;
        di.mbQps[1][mbAddr] = calcQpChroma(sharedState.qp, sharedState.chromaQpOffset[0]);
        di.mbQps[2][mbAddr] = calcQpChroma(sharedState.qp, sharedState.chromaQpOffset[1]);
    }

    public void predictPSkip(Frame[][] refs, int mbX, int mbY, boolean lAvb, boolean tAvb, boolean tlAvb,
            boolean trAvb, int[][][] x, Picture8Bit mb) {
        int mvX = 0, mvY = 0;
        if (lAvb && tAvb) {
            int[] b = sharedState.mvTop[0][mbX << 2];
            int[] a = sharedState.mvLeft[0][0];

            if ((a[0] != 0 || a[1] != 0 || a[2] != 0) && (b[0] != 0 || b[1] != 0 || b[2] != 0)) {
                mvX = calcMVPredictionMedian(a, b, sharedState.mvTop[0][(mbX << 2) + 4], sharedState.mvTopLeft[0],
                        lAvb, tAvb, trAvb, tlAvb, 0, 0);
                mvY = calcMVPredictionMedian(a, b, sharedState.mvTop[0][(mbX << 2) + 4], sharedState.mvTopLeft[0],
                        lAvb, tAvb, trAvb, tlAvb, 0, 1);
            }
        }
        debugPrint("MV_SKIP: (" + mvX + "," + mvY + ")");
        int blk8x8X = mbX << 1;
        sharedState.predModeLeft[0] = sharedState.predModeLeft[1] = sharedState.predModeTop[blk8x8X] = sharedState.predModeTop[blk8x8X + 1] = L0;

        int xx = mbX << 2;
        copyVect(sharedState.mvTopLeft[0], sharedState.mvTop[0][xx + 3]);
        saveVect(sharedState.mvTop[0], xx, xx + 4, mvX, mvY, 0);
        saveVect(sharedState.mvLeft[0], 0, 4, mvX, mvY, 0);

        for (int i = 0; i < 16; i++) {
            x[0][i][0] = mvX;
            x[0][i][1] = mvY;
            x[0][i][2] = 0;
        }
        BlockInterpolator.getBlockLuma(refs[0][0], mb, 0, (mbX << 6) + mvX, (mbY << 6) + mvY, 16, 16);

        PredictionMerger.mergePrediction(sh, 0, 0, L0, 0, mb.getPlaneData(0), null, 0, 16, 16, 16, mb.getPlaneData(0),
                refs, poc);
    }

    public void decodeChromaSkip(Frame[][] reference, int[][][] vectors, PartPred[] pp, int mbX, int mbY, Picture8Bit mb) {
        predictChromaInter(reference, vectors, mbX << 3, mbY << 3, 1, mb, pp);
        predictChromaInter(reference, vectors, mbX << 3, mbY << 3, 2, mb, pp);
    }
}
