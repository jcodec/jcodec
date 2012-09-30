package org.jcodec.codecs.h264.decode;

import org.jcodec.codecs.h264.decode.model.DecodedChroma;
import org.jcodec.codecs.h264.decode.model.DecodedMBlock;
import org.jcodec.codecs.h264.decode.model.MVMatrix;
import org.jcodec.codecs.h264.decode.model.NearbyMotionVectors;
import org.jcodec.codecs.h264.decode.model.PixelBuffer;
import org.jcodec.codecs.h264.io.model.ChromaFormat;
import org.jcodec.codecs.h264.io.model.Inter8x8Prediction;
import org.jcodec.codecs.h264.io.model.MBlockInter;
import org.jcodec.codecs.h264.io.model.MBlockInter8x8;
import org.jcodec.codecs.h264.io.model.ResidualBlock;
import org.jcodec.codecs.h264.io.model.SubMBType;
import org.jcodec.codecs.h264.io.model.Vector;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Point;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Decoder for inter macroblock types
 * 
 * 
 * @author Jay Codec
 * 
 */
public class MBlockDecoderInter {
    private InterPredictionBuilder predictionBuilder;
    private CoeffTransformer transform;
    private MVPredictor mvPredictor;
    private ChromaDecoder chromaDecoder;

    public MBlockDecoderInter(int[] chromaQpOffset) {
        predictionBuilder = new InterPredictionBuilder();
        transform = new CoeffTransformer(null);
        mvPredictor = new MVPredictor();
        chromaDecoder = new ChromaDecoder(chromaQpOffset, 8, ChromaFormat.YUV_420);
    }

    public DecodedMBlock decodeP8x8(Picture[] reference, MBlockInter8x8 mb, NearbyMotionVectors nearMV, Point origin,
            int qp) {

        Inter8x8Prediction prediction = mb.getPrediction();
        Vector[][] predictedMVs = mvPredictor.predictMotionVectors8x8(nearMV, prediction);

        int[] pred = predictionBuilder.predict8x8(reference, prediction.getSubMbTypes(), predictedMVs, origin);

        int[] residualLuma = buildResidualLuma(mb.getLuma(), qp);

        int[] luma = mergePixels(pred, residualLuma);

        MVMatrix mvMat = calcFor8x8(prediction.getSubMbTypes(), predictedMVs);

        DecodedChroma decodeChroma = chromaDecoder.decodeChromaInter(mb.getChroma(), reference, mvMat, origin, qp);

        return new DecodedMBlock(luma, decodeChroma, qp, mvMat, reference, mb);
    }

    public DecodedMBlock decodeP16x8(Picture[] reference, MBlockInter mb, NearbyMotionVectors nearMV, Point origin,
            int qp) {

        Vector[] predictedMVs = mvPredictor.predictMotionVectors16x8(nearMV, mb.getPrediction());

        int[] prediction = predictionBuilder.predict16x8(reference, predictedMVs, origin);

        int[] residualLuma = buildResidualLuma(mb.getLuma(), qp);

        int[] luma = mergePixels(prediction, residualLuma);

        MVMatrix mvMat = calcForInter16x8(predictedMVs);
        DecodedChroma decodeChroma = chromaDecoder.decodeChromaInter(mb.getChroma(), reference, mvMat, origin, qp);

        return new DecodedMBlock(luma, decodeChroma, qp, mvMat, reference, mb);
    }

    public DecodedMBlock decodeP8x16(Picture[] reference, MBlockInter mb, NearbyMotionVectors nearMV, Point origin,
            int qp) {

        Vector[] predictedMVs = mvPredictor.predictMotionVectors8x16(nearMV, mb.getPrediction());

        int[] prediction = predictionBuilder.predict8x16(reference, predictedMVs, origin);

        int[] residualLuma = buildResidualLuma(mb.getLuma(), qp);

        int[] luma = mergePixels(prediction, residualLuma);

        MVMatrix mvMat = calcForInter8x16(predictedMVs);
        DecodedChroma decodeChroma = chromaDecoder.decodeChromaInter(mb.getChroma(), reference, mvMat, origin, qp);

        return new DecodedMBlock(luma, decodeChroma, qp, mvMat, reference, mb);
    }

    public DecodedMBlock decodeP16x16(Picture[] reference, MBlockInter mb, NearbyMotionVectors nearMV, Point origin,
            int qp) {

        Vector[] predictedMVs = mvPredictor.predictMotionVectors16x16(nearMV, mb.getPrediction());

        int[] prediction = predictionBuilder.predict16x16(reference, predictedMVs, origin);

        int[] residualLuma = buildResidualLuma(mb.getLuma(), qp);

        int[] luma = mergePixels(prediction, residualLuma);

        MVMatrix mvMat = calcForInter16x16(predictedMVs);
        DecodedChroma decodeChroma = chromaDecoder.decodeChromaInter(mb.getChroma(), reference, mvMat, origin, qp);

        return new DecodedMBlock(luma, decodeChroma, qp, mvMat, reference, mb);
    }

    protected MVMatrix calcFor8x8(SubMBType[] subMBTypes, Vector[][] mvs) {
        Vector[] dmvs = new Vector[16];

        for (int blk8x8 = 0; blk8x8 < 4; blk8x8++) {
            if (subMBTypes[blk8x8] == SubMBType.L0_8x8) {
                dmvs[blk8x8 * 4] = dmvs[blk8x8 * 4 + 1] = dmvs[blk8x8 * 4 + 2] = dmvs[blk8x8 * 4 + 3] = mvs[blk8x8][0];
            } else if (subMBTypes[blk8x8] == SubMBType.L0_8x4) {
                dmvs[blk8x8 * 4] = dmvs[blk8x8 * 4 + 1] = mvs[blk8x8][0];
                dmvs[blk8x8 * 4 + 2] = dmvs[blk8x8 * 4 + 3] = mvs[blk8x8][1];
            } else if (subMBTypes[blk8x8] == SubMBType.L0_4x8) {
                dmvs[blk8x8 * 4] = dmvs[blk8x8 * 4 + 2] = mvs[blk8x8][0];
                dmvs[blk8x8 * 4 + 1] = dmvs[blk8x8 * 4 + 3] = mvs[blk8x8][1];
            } else {
                dmvs[blk8x8 * 4] = mvs[blk8x8][0];
                dmvs[blk8x8 * 4 + 1] = mvs[blk8x8][1];
                dmvs[blk8x8 * 4 + 2] = mvs[blk8x8][2];
                dmvs[blk8x8 * 4 + 3] = mvs[blk8x8][3];
            }
        }

        return new MVMatrix(dmvs);
    }

    protected MVMatrix calcForInter16x8(Vector[] mvs) {
        Vector[] dmvs = new Vector[] { mvs[0], mvs[0], mvs[0], mvs[0], mvs[0], mvs[0], mvs[0], mvs[0], mvs[1], mvs[1],
                mvs[1], mvs[1], mvs[1], mvs[1], mvs[1], mvs[1] };
        return new MVMatrix(dmvs);
    }

    protected MVMatrix calcForInter8x16(Vector[] mvs) {
        Vector[] dmvs = new Vector[] { mvs[0], mvs[0], mvs[0], mvs[0], mvs[1], mvs[1], mvs[1], mvs[1], mvs[0], mvs[0],
                mvs[0], mvs[0], mvs[1], mvs[1], mvs[1], mvs[1] };
        return new MVMatrix(dmvs);
    }

    protected MVMatrix calcForInter16x16(Vector[] mvs) {
        Vector[] dmvs = new Vector[] { mvs[0], mvs[0], mvs[0], mvs[0], mvs[0], mvs[0], mvs[0], mvs[0], mvs[0], mvs[0],
                mvs[0], mvs[0], mvs[0], mvs[0], mvs[0], mvs[0] };
        return new MVMatrix(dmvs);
    }

    public DecodedMBlock decodePSkip(Picture[] reference, NearbyMotionVectors nearMV, Point origin, int qp) {

        Vector[] predictedMVs = mvPredictor.predictMotionVectorsSkip(nearMV);

        int[] prediction = predictionBuilder.predict16x16(reference, predictedMVs, origin);

        MVMatrix mvMat = calcForInter16x16(predictedMVs);
        DecodedChroma chroma = chromaDecoder.decodeChromaSkip(reference, mvMat, origin, qp);

        return new DecodedMBlock(prediction, chroma, qp, mvMat, reference, null);
    }

    private int[] buildResidualLuma(ResidualBlock[] luma, int qp) {
        int[] residualLuma = new int[256];
        for (int i8x8 = 0; i8x8 < 4; i8x8++) {
            for (int i4x4 = 0; i4x4 < 4; i4x4++) {
                ResidualBlock block = luma[(i8x8 << 2) + i4x4];
                int[] transformed;
                if (block != null) {
                    int[] reordered = transform.reorderCoeffs(block.getCoeffs());
                    int[] rescaled = transform.rescaleBeforeIDCT4x4(reordered, qp);
                    transformed = transform.transformIDCT4x4(rescaled);
                } else {
                    transformed = new int[16];
                }
                PixelBuffer pt = new PixelBuffer(transformed, 0, 2);

                int pelY = (i8x8 / 2) * 8 * 16 + (i4x4 / 2) * 4 * 16;
                int pelX = (i8x8 % 2) * 8 + (i4x4 % 2) * 4;

                PixelBuffer pb = new PixelBuffer(residualLuma, pelX + pelY, 4);
                pb.put(pt, 4, 4);
            }
        }
        return residualLuma;
    }

    private int[] mergePixels(int[] prediction, int[] residualLuma) {
        for (int i = 0; i < 256; i++) {
            int val = prediction[i] + residualLuma[i];
            val = val < 0 ? 0 : (val > 255 ? 255 : val);
            prediction[i] = val;
        }
        return prediction;
    }

    public DecodedMBlock decodeP(Picture[] reference, MBlockInter mb, NearbyMotionVectors nearMV, Point origin, int qp) {
        switch (mb.getType()) {
        case MB_16x16:
            return decodeP16x16(reference, mb, nearMV, origin, qp);
        case MB_16x8:
            return decodeP16x8(reference, mb, nearMV, origin, qp);
        case MB_8x16:
            return decodeP8x16(reference, mb, nearMV, origin, qp);
        }

        throw new RuntimeException("Shit man!");
    }
}
