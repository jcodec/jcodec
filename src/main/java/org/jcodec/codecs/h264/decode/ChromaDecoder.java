package org.jcodec.codecs.h264.decode;

import org.jcodec.codecs.h264.decode.model.BlockBorder;
import org.jcodec.codecs.h264.decode.model.DecodedChroma;
import org.jcodec.codecs.h264.decode.model.MVMatrix;
import org.jcodec.codecs.h264.decode.model.NearbyPixels;
import org.jcodec.codecs.h264.decode.model.PixelBuffer;
import org.jcodec.codecs.h264.io.model.ChromaFormat;
import org.jcodec.codecs.h264.io.model.CodedChroma;
import org.jcodec.codecs.h264.io.model.ResidualBlock;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Point;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A decoder for chroma coefficients of macroblock
 * 
 * @author Jay Codec
 * 
 */
public class ChromaDecoder {
    static final int[] QP_SCALE_CR = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21,
            22, 23, 24, 25, 26, 27, 28, 29, 29, 30, 31, 32, 32, 33, 34, 34, 35, 35, 36, 36, 37, 37, 37, 38, 38, 38, 39,
            39, 39, 39 };

    private CoeffTransformer transform;
    private ChromaIntraPredictionBuilder intraPredictionBuilder;
    private ChromaInterPredictionBuilder interPredictionBuilder;

    private int[] chromaQpOffset;

    public ChromaDecoder(int[] chromaQpOffset, int bitDepthChroma, ChromaFormat chromaFormat) {
        transform = new CoeffTransformer(null);
        intraPredictionBuilder = new ChromaIntraPredictionBuilder(bitDepthChroma, chromaFormat);
        interPredictionBuilder = new ChromaInterPredictionBuilder();

        this.chromaQpOffset = chromaQpOffset;
    }

    public DecodedChroma decodeChromaIntra(CodedChroma chroma, int chromaMode, int qp, NearbyPixels.Plane nCb,
            NearbyPixels.Plane nCr) {

        int[] crQp = new int[] { calcQpChroma(qp, chromaQpOffset[0]), calcQpChroma(qp, chromaQpOffset[1]) };

        int[] dCb = decodeOneComponent(chroma.getCbDC(), chroma.getCbAC(), nCb, crQp[0], chromaMode);
        int[] dCr = decodeOneComponent(chroma.getCrDC(), chroma.getCrAC(), nCr, crQp[1], chromaMode);

        return new DecodedChroma(dCb, dCr, crQp[0], crQp[1]);
    }

    public DecodedChroma decodeChromaInter(CodedChroma chroma, Picture[] reference, MVMatrix mvMat, Point origin, int qp) {

        int[] crQp = new int[] { calcQpChroma(qp, chromaQpOffset[0]), calcQpChroma(qp, chromaQpOffset[1]) };

        int[] predictionCb = interPredictionBuilder.predictCb(reference, mvMat, origin);
        int[] residualCb = buildResidual(chroma.getCbDC(), chroma.getCbAC(), crQp[0]);

        mergePixels(residualCb, predictionCb);

        int[] predictionCr = interPredictionBuilder.predictCr(reference, mvMat, origin);
        int[] residualCr = buildResidual(chroma.getCrDC(), chroma.getCrAC(), crQp[1]);

        mergePixels(residualCr, predictionCr);

        return new DecodedChroma(residualCb, residualCr, crQp[0], crQp[1]);
    }

    public int[] decodeOneComponent(ResidualBlock dcBlock, ResidualBlock[] acBlocks, NearbyPixels.Plane neighbors,
            int qp, int predictionMode) {

        int[] residual = buildResidual(dcBlock, acBlocks, qp);

        if (predictionMode == 0) {
            int[] pred = new int[16];
            for (int i4x4 = 0; i4x4 < 4; i4x4++) {
                int blkX = i4x4 % 2;
                int blkY = i4x4 / 2;
                BlockBorder border = buildBlockBorder4(neighbors, residual, blkX, blkY);
                intraPredictionBuilder.predictWithMode(predictionMode, blkX, blkY, border, new PixelBuffer(pred, 0, 2));
                mergePixels(residual, pred, blkX, blkY);
            }
        } else {
            int[] pred = new int[64];
            BlockBorder border = buildBlockBorder8(neighbors);
            intraPredictionBuilder.predictWithMode(predictionMode, 0, 0, border, new PixelBuffer(pred, 0, 3));
            mergePixels(residual, pred);
        }

        return residual;
    }

    private int calcQpChroma(int qp, int crQpOffset) {
        int result = qp + crQpOffset;
        if (result > 51)
            result = 51;

        return QP_SCALE_CR[result];
    }

    private int[] buildResidual(ResidualBlock dcBlk, ResidualBlock[] acBlks, int qp) {
        int[] residual = new int[64];

        // DC
        int[] dc;
        if (dcBlk != null) {
            int[] transformed = transform.transformIHadamard2x2(dcBlk.getCoeffs());
            dc = transform.rescaleAfterIHadamard2x2(transformed, qp);
        } else {
            dc = new int[4];
        }

        // AC
        for (int i4x4 = 0; i4x4 < 4; i4x4++) {
            ResidualBlock block = acBlks[i4x4];

            int[] rescaled;
            if (block != null) {
                int[] coeffs = new int[16];
                System.arraycopy(block.getCoeffs(), 0, coeffs, 1, 15);
                int[] reordered = transform.reorderCoeffs(coeffs);
                rescaled = transform.rescaleBeforeIDCT4x4(reordered, qp);
            } else {
                rescaled = new int[16];
            }

            rescaled[0] = dc[i4x4];
            int[] transformed = transform.transformIDCT4x4(rescaled);
            PixelBuffer pt = new PixelBuffer(transformed, 0, 2);

            int pelY = (i4x4 / 2) * 4 * 8;
            int pelX = (i4x4 % 2) * 4;

            PixelBuffer pb = new PixelBuffer(residual, pelX + pelY, 3);
            pb.put(pt, 4, 4);
        }
        return residual;
    }

    private void mergePixels(int[] residual, int[] pred, int blkX, int blkY) {
        int start = blkX * 4 + blkY * 8 * 4;
        for (int j = 0; j < 4; j++) {
            for (int i = 0; i < 4; i++) {
                int posR = start + i + j * 8;
                int posP = i + j * 4;
                int val = residual[posR] + pred[posP];
                val = (val > 255 ? 255 : (val < 0 ? 0 : val));
                residual[posR] = val;
            }
        }
    }

    private BlockBorder buildBlockBorder4(NearbyPixels.Plane neighbors, int[] residual, int blkX, int blkY) {
        int[] top = null;
        if (neighbors.getMbTop() != null) {
            top = new int[4];
            for (int i = 0; i < 4; i++) {
                top[i] = neighbors.getMbTop()[56 + i + blkX * 4];
            }
        }

        int[] left = null;
        if (neighbors.getMbLeft() != null) {
            left = new int[8];
            for (int i = 0; i < 4; i++) {
                left[i] = neighbors.getMbLeft()[7 + i * 8 + blkY * 8 * 4];
            }
        }

        Integer topLeft = null;
        if (blkX == 0 && blkY == 0) {
            if (neighbors.getMbTopLeft() != null)
                topLeft = neighbors.getMbTopLeft()[63];
        } else if (blkX == 0 && blkY == 1) {
            if (neighbors.getMbLeft() != null)
                topLeft = neighbors.getMbLeft()[31];
        } else if (blkX == 1 && blkY == 0) {
            if (neighbors.getMbTop() != null)
                topLeft = neighbors.getMbTop()[3];
        } else {
            topLeft = residual[27];
        }

        return new BlockBorder(left, top, topLeft);
    }

    private void mergePixels(int[] residual, int[] pred) {
        for (int j = 0; j < 8; j++) {
            for (int i = 0; i < 8; i++) {
                int pos = i + j * 8;
                int val = residual[pos] + pred[pos];
                val = (val > 255 ? 255 : (val < 0 ? 0 : val));
                residual[pos] = val;
            }
        }
    }

    private BlockBorder buildBlockBorder8(NearbyPixels.Plane neighbors) {
        int[] top = null;
        if (neighbors.getMbTop() != null) {
            top = new int[8];
            for (int i = 0; i < 8; i++) {
                top[i] = neighbors.getMbTop()[56 + i];
            }
        }

        int[] left = null;
        if (neighbors.getMbLeft() != null) {
            left = new int[8];
            for (int i = 0; i < 8; i++) {
                left[i] = neighbors.getMbLeft()[7 + i * 8];
            }
        }

        Integer topLeft = null;
        if (neighbors.getMbTopLeft() != null) {
            topLeft = neighbors.getMbTopLeft()[63];
        }

        return new BlockBorder(left, top, topLeft);
    }

    public DecodedChroma decodeChromaSkip(Picture[] reference, MVMatrix mvMat, Point origin, int qp) {
        int[] crQp = new int[] { calcQpChroma(qp, chromaQpOffset[0]), calcQpChroma(qp, chromaQpOffset[1]) };

        int[] predictionCb = interPredictionBuilder.predictCb(reference, mvMat, origin);

        int[] predictionCr = interPredictionBuilder.predictCr(reference, mvMat, origin);

        return new DecodedChroma(predictionCb, predictionCr, crQp[0], crQp[1]);
    }
}
