package org.jcodec.codecs.h264.decode;

import org.jcodec.codecs.h264.decode.model.BlockBorder;
import org.jcodec.codecs.h264.decode.model.DecodedChroma;
import org.jcodec.codecs.h264.decode.model.DecodedMBlock;
import org.jcodec.codecs.h264.decode.model.NearbyPixels;
import org.jcodec.codecs.h264.decode.model.PixelBuffer;
import org.jcodec.codecs.h264.io.model.ChromaFormat;
import org.jcodec.codecs.h264.io.model.IntraNxNPrediction;
import org.jcodec.codecs.h264.io.model.MBlockIntraNxN;
import org.jcodec.codecs.h264.io.model.ResidualBlock;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Macroblock decoder
 * 
 * Note: field macroblocks are not supported
 * 
 * @author Jay Codec
 * 
 */
public class MBlockDecoderI4x4 {

    private CoeffTransformer transform;
    private Intra4x4PredictionBuilder intraPredictionBuilder;
    private ChromaDecoder chromaDecoder;

    int[][] indicesLeft = { { 260, 261, 262, 263 }, { 3, 19, 35, 51 }, { 264, 265, 266, 267 }, { 67, 83, 99, 115 },

    { 7, 23, 39, 55 }, { 11, 27, 43, 59 }, { 71, 87, 103, 119 }, { 75, 91, 107, 123 },

    { 268, 269, 270, 271 }, { 131, 147, 163, 179 }, { 272, 273, 274, 275 }, { 195, 211, 227, 243 },

    { 135, 151, 167, 183 }, { 139, 155, 171, 187 }, { 199, 215, 231, 247 }, { 203, 219, 235, 251 } };

    int[][] indicesTop = { { 276, 277, 278, 279, 280, 281, 282, 283 }, { 280, 281, 282, 283, 284, 285, 286, 287 },
            { 48, 49, 50, 51, 52, 53, 54, 55 }, { 52, 53, 54, 55, 55, 55, 55, 55 },

            { 284, 285, 286, 287, 288, 289, 290, 291 }, { 288, 289, 290, 291, 292, 293, 294, 295 },
            { 56, 57, 58, 59, 60, 61, 62, 63 }, { 60, 61, 62, 63, 63, 63, 63, 63 },

            { 112, 113, 114, 115, 116, 117, 118, 119 }, { 116, 117, 118, 119, 120, 121, 122, 123 },
            { 176, 177, 178, 179, 180, 181, 182, 183 }, { 180, 181, 182, 183, 183, 183, 183, 183 },

            { 120, 121, 122, 123, 124, 125, 126, 127 }, { 124, 125, 126, 127, 127, 127, 127, 127 },
            { 184, 185, 186, 187, 188, 189, 190, 191 }, { 188, 189, 190, 191, 191, 191, 191, 191 } };

    int[] indicesTopLeft = { 256, 279, 257, 51, 283, 287, 55, 59, 258, 115, 259, 179, 119, 123, 183, 187 };

    public MBlockDecoderI4x4(int[] chromaQpOffset, int bitDepthLuma, int bitDepthChroma) {
        transform = new CoeffTransformer(null);
        intraPredictionBuilder = new Intra4x4PredictionBuilder(bitDepthLuma);
        chromaDecoder = new ChromaDecoder(chromaQpOffset, bitDepthChroma, ChromaFormat.YUV_420);
    }

    public DecodedMBlock decodeINxN(MBlockIntraNxN coded, int qp, NearbyPixels nearPixels) {
        int[] residualLuma = new int[256];
        for (int i8x8 = 0; i8x8 < 4; i8x8++) {
            for (int i4x4 = 0; i4x4 < 4; i4x4++) {
                ResidualBlock block = coded.getLuma()[(i8x8 << 2) + i4x4];
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
        int[] pixelsLuma = predict4x4(coded.getPrediction(), residualLuma, nearPixels.getLuma());

        DecodedChroma decodedChroma = chromaDecoder.decodeChromaIntra(coded.getChroma(), coded.getPrediction()
                .getChromaMode(), qp, nearPixels.getCb(), nearPixels.getCr());

        return new DecodedMBlock(pixelsLuma, decodedChroma, qp, null, null, coded);
    }

    public int[] predict4x4(IntraNxNPrediction pred, int[] residual, NearbyPixels.Plane neighbours) {

        int[] pixels = new int[308];

        collectPixelsFromBorder(neighbours, pixels);

        int[] lumaModes = pred.getLumaModes();
        for (int i8x8 = 0; i8x8 < 4; i8x8++) {
            for (int i4x4 = 0; i4x4 < 4; i4x4++) {
                BlockBorder border = getBorderPixels(i4x4, i8x8, pixels, neighbours);

                int pelY = (i8x8 / 2) * 8 * 16 + (i4x4 / 2) * 4 * 16;
                int pelX = (i8x8 % 2) * 8 + (i4x4 % 2) * 4;

                PixelBuffer pb = new PixelBuffer(pixels, pelY + pelX, 4);

                intraPredictionBuilder.predictWithMode(lumaModes[(i8x8 << 2) + i4x4], border, pb);

                mergeResidualIn(pixels, residual, pelY + pelX);
            }
        }

        return pixels;
    }

    private BlockBorder getBorderPixels(int blkX, int blkY, int[] result, NearbyPixels.Plane neighbours) {

        int blkIdx = (blkY << 2) + blkX;

        int[] top = new int[] { result[indicesTop[blkIdx][0]], result[indicesTop[blkIdx][1]],
                result[indicesTop[blkIdx][2]], result[indicesTop[blkIdx][3]], result[indicesTop[blkIdx][4]],
                result[indicesTop[blkIdx][5]], result[indicesTop[blkIdx][6]], result[indicesTop[blkIdx][7]] };

        int[] left = new int[] { result[indicesLeft[blkIdx][0]], result[indicesLeft[blkIdx][1]],
                result[indicesLeft[blkIdx][2]], result[indicesLeft[blkIdx][3]] };

        int topLeft = result[indicesTopLeft[blkIdx]];

        return new BlockBorder(left, top, topLeft);
    }

    private void mergeResidualIn(int[] prev, int[] residual, int blkTopLeft) {

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                int offset = blkTopLeft + x + (y << 4);
                int val = prev[offset] + residual[offset];
                val = val < 0 ? 0 : (val > 255 ? 255 : val);
                prev[offset] = val;
            }
        }

    }

    private void collectPixelsFromBorder(NearbyPixels.Plane neighbours, int[] prev) {
        if (neighbours.getMbTopLeft() != null) {
            prev[256] = neighbours.getMbTopLeft()[255];
        } else {
            prev[256] = -1;
        }

        if (neighbours.getMbLeft() != null) {
            prev[257] = neighbours.getMbLeft()[63];
            prev[258] = neighbours.getMbLeft()[127];
            prev[259] = neighbours.getMbLeft()[191];
        } else {
            prev[257] = -1;
            prev[258] = -1;
            prev[259] = -1;
        }

        if (neighbours.getMbLeft() != null) {
            for (int i = 0; i < 16; i++) {
                prev[260 + i] = neighbours.getMbLeft()[15 + (i << 4)];
            }
        } else {
            for (int i = 0; i < 16; i++) {
                prev[260 + i] = -1;
            }
        }

        if (neighbours.getMbTop() != null) {
            for (int i = 0; i < 16; i++) {
                prev[276 + i] = neighbours.getMbTop()[240 + i];
            }

            if (neighbours.getMbTopRight() != null) {
                prev[292] = neighbours.getMbTopRight()[240];
                prev[293] = neighbours.getMbTopRight()[241];
                prev[294] = neighbours.getMbTopRight()[242];
                prev[295] = neighbours.getMbTopRight()[243];
            } else {
                prev[292] = neighbours.getMbTop()[255];
                prev[293] = neighbours.getMbTop()[255];
                prev[294] = neighbours.getMbTop()[255];
                prev[295] = neighbours.getMbTop()[255];
            }
        } else {
            for (int i = 0; i < 16; i++) {
                prev[276 + i] = -1;
            }
            prev[292] = -1;
            prev[293] = -1;
            prev[294] = -1;
            prev[295] = -1;
        }
    }
}
