package org.jcodec.codecs.h264.decode;

import org.jcodec.codecs.h264.decode.model.BlockBorder;
import org.jcodec.codecs.h264.decode.model.DecodedChroma;
import org.jcodec.codecs.h264.decode.model.DecodedMBlock;
import org.jcodec.codecs.h264.decode.model.NearbyPixels;
import org.jcodec.codecs.h264.decode.model.PixelBuffer;
import org.jcodec.codecs.h264.io.model.ChromaFormat;
import org.jcodec.codecs.h264.io.model.MBlockIntra16x16;
import org.jcodec.codecs.h264.io.model.ResidualBlock;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Macroblock decoder for I16x16 macroblocks
 * 
 * 
 * @author Jay Codec
 * 
 */
public class MBlockDecoderI16x16 {

    private CoeffTransformer transform;
    private Intra16x16PredictionBuilder intraPredictionBuilder;
    private ChromaDecoder chromaDecoder;

    public MBlockDecoderI16x16(int[] chromaQpOffset, int bitDepthLuma, int bitDepthChroma) {
        transform = new CoeffTransformer(null);
        intraPredictionBuilder = new Intra16x16PredictionBuilder(bitDepthLuma);
        chromaDecoder = new ChromaDecoder(chromaQpOffset, bitDepthChroma, ChromaFormat.YUV_420);
    }

    public DecodedMBlock decodeI16x16(MBlockIntra16x16 coded, int qp, NearbyPixels nearPixels) {

        // Prediction
        int[] pixelsLuma = predict16x16(coded.getLumaMode(), nearPixels.getLuma());

        // DC
        int[] reorderedDC = transform.reorderCoeffs(coded.getLumaDC().getCoeffs());
        int[] transformedDC = transform.transformIHadamard4x4(reorderedDC);
        int[] dco = transform.rescaleAfterIHadamard4x4(transformedDC, qp);
        int[] dc = new int[] { dco[0], dco[1], dco[4], dco[5], dco[2], dco[3], dco[6], dco[7], dco[8], dco[9], dco[12],
                dco[13], dco[10], dco[11], dco[14], dco[15] };

        // AC

        // //////
        int[] residualLuma = new int[256];
        for (int i8x8 = 0; i8x8 < 4; i8x8++) {
            for (int i4x4 = 0; i4x4 < 4; i4x4++) {
                ResidualBlock block = coded.getLumaAC()[(i8x8 << 2) + i4x4];

                int[] rescaled;
                if (block != null) {
                    int[] coeffs = new int[16];
                    coeffs[0] = 0;
                    System.arraycopy(block.getCoeffs(), 0, coeffs, 1, 15);

                    int[] reordered = transform.reorderCoeffs(coeffs);
                    rescaled = transform.rescaleBeforeIDCT4x4(reordered, qp);
                } else {
                    rescaled = new int[16];
                }
                rescaled[0] = dc[(i8x8 << 2) + i4x4];
                int[] transformed = transform.transformIDCT4x4(rescaled);

                PixelBuffer pt = new PixelBuffer(transformed, 0, 2);

                int pelY = (i8x8 / 2) * 8 * 16 + (i4x4 / 2) * 4 * 16;
                int pelX = (i8x8 % 2) * 8 + (i4x4 % 2) * 4;

                PixelBuffer pb = new PixelBuffer(residualLuma, pelX + pelY, 4);
                pb.put(pt, 4, 4);
            }
        }

        mergePixels(pixelsLuma, residualLuma);

        DecodedChroma decodedChroma = chromaDecoder.decodeChromaIntra(coded.getChroma(), coded.getChromaMode(), qp,
                nearPixels.getCb(), nearPixels.getCr());

        return new DecodedMBlock(pixelsLuma, decodedChroma, qp, null, null, coded);
    }

    private void mergePixels(int[] pixelsLuma, int[] residualLuma) {
        for (int i = 0; i < 256; i++) {
            int val = pixelsLuma[i] + residualLuma[i];
            val = val < 0 ? 0 : (val > 255 ? 255 : val);

            pixelsLuma[i] = val;
        }

    }

    public int[] predict16x16(int predMode, NearbyPixels.Plane neighbours) {

        int[] pixels = new int[256];

        BlockBorder border = collectPixelsFromBorder(neighbours, pixels);

        PixelBuffer pb = new PixelBuffer(pixels, 0, 4);

        intraPredictionBuilder.predictWithMode(predMode, border, pb);

        return pixels;
    }

    private BlockBorder collectPixelsFromBorder(NearbyPixels.Plane neighbours, int[] prev) {
        int[] left = null;
        if (neighbours.getMbLeft() != null) {
            left = new int[16];
            for (int i = 0; i < 16; i++) {
                left[i] = neighbours.getMbLeft()[15 + (i << 4)];
            }
        }

        int[] top = null;
        if (neighbours.getMbTop() != null) {
            top = new int[16];
            for (int i = 0; i < 16; i++) {
                top[i] = neighbours.getMbTop()[240 + i];
            }
        }

        Integer topLeft = null;
        if (neighbours.getMbTopLeft() != null) {
            topLeft = neighbours.getMbTopLeft()[255];
        }

        return new BlockBorder(left, top, topLeft);
    }
}
