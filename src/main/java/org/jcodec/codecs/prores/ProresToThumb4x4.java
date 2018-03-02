package org.jcodec.codecs.prores;
import org.jcodec.codecs.prores.ProresConsts.FrameHeader;
import org.jcodec.common.dct.IDCT4x4;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rect;
import org.jcodec.common.tools.MathUtil;

import java.nio.ByteBuffer;

import js.nio.ByteBuffer;

/**
 * 
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Decodes a ProRes file in low res. Decodes each 8x8 block as downscaled 4x4
 * block.
 * 
 * @author The JCodec project
 * 
 */
public class ProresToThumb4x4 extends ProresDecoder {

    public ProresToThumb4x4() {
    }

    @Override
    protected void decodeOnePlane(BitReader bits, int blocksPerSlice, int[] out, int[] qMat, int[] scan, int mbX, int mbY,
            int plane) {
        readDCCoeffs(bits, qMat, out, blocksPerSlice, 16);
        readACCoeffs(bits, qMat, out, blocksPerSlice, scan, 16, 4);

        for (int i = 0; i < blocksPerSlice; i++) {
            IDCT4x4.idct(out, i << 4);
        }
    }

    public static int progressive_scan_4x4[] = new int[] { 0, 1, 4, 5, 2, 3, 6, 7, 8, 9, 12, 13, 11, 12, 14, 15 };

    public static int interlaced_scan_4x4[] = new int[] { 0, 4, 1, 5, 8, 12, 9, 13, 2, 6, 3, 7, 10, 14, 11, 15 };

    @Override
    public Picture decodeFrameHiBD(ByteBuffer data, byte[][] target, byte[][] lowBits) {
        FrameHeader fh = readFrameHeader(data);

        int codedWidth = ((fh.width + 15) & ~0xf) >> 1;
        int codedHeight = ((fh.height + 15) & ~0xf) >> 1;

        int lumaSize = codedWidth * codedHeight;
        int chromaSize = lumaSize >> 1;

        if (target == null || target[0].length < lumaSize || target[1].length < chromaSize
                || target[2].length < chromaSize) {
            throw new RuntimeException("Provided output picture won't fit into provided buffer");
        }

        if (fh.frameType == 0) {
            decodePicture(data, target, lowBits, codedWidth, codedHeight, codedWidth >> 3, fh.qMatLuma, fh.qMatChroma,
                    progressive_scan_4x4, 0, fh.chromaType);
        } else {
            decodePicture(data, target, lowBits, codedWidth, codedHeight >> 1, codedWidth >> 3, fh.qMatLuma, fh.qMatChroma,
                    interlaced_scan_4x4, fh.topFieldFirst ? 1 : 2, fh.chromaType);

            decodePicture(data, target, lowBits, codedWidth, codedHeight >> 1, codedWidth >> 3, fh.qMatLuma, fh.qMatChroma,
                    interlaced_scan_4x4, fh.topFieldFirst ? 2 : 1, fh.chromaType);
        }

        ColorSpace color = fh.chromaType == 2 ? ColorSpace.YUV422 : ColorSpace.YUV444;
        return new Picture(codedWidth, codedHeight, target, lowBits, color, lowBits == null ? 0 : 2, new Rect(0,
                0, (fh.width >> 1) & color.getWidthMask(), (fh.height >> 1) & color.getHeightMask()));
    }

    @Override
    protected void putSlice(byte[][] result, byte[][] lowBits, int lumaStride, int mbX, int mbY, int[] y, int[] u,
            int[] v, int dist, int shift, int chromaType, int sliceMbCount) {
        int chromaStride = lumaStride >> 1;

        _putLuma(result[0], lowBits == null ? null : lowBits[0], shift * lumaStride, lumaStride << dist, mbX, mbY,
                y, sliceMbCount, dist, shift);
        if (chromaType == 2) {
            _putChroma(result[1], lowBits == null ? null : lowBits[1], shift * chromaStride, chromaStride << dist,
                    mbX, mbY, u, sliceMbCount, dist, shift);
            _putChroma(result[2], lowBits == null ? null : lowBits[2], shift * chromaStride, chromaStride << dist,
                    mbX, mbY, v, sliceMbCount, dist, shift);
        } else {
            _putLuma(result[1], lowBits == null ? null : lowBits[1], shift * lumaStride, lumaStride << dist, mbX,
                    mbY, u, sliceMbCount, dist, shift);
            _putLuma(result[2], lowBits == null ? null : lowBits[2], shift * lumaStride, lumaStride << dist, mbX,
                    mbY, v, sliceMbCount, dist, shift);
        }
    }

    private static final int srcIncLuma[] = { 4, 4, 4, 20, 4, 4, 4, 20 };

    private void _putLuma(byte[] y, byte[] lowBits, int fieldOffset, int stride, int mbX, int mbY, int[] luma,
            int mbPerSlice, int dist, int shift) {
        int mbTopLeftOff = fieldOffset + (mbX << 3) + (mbY << 3) * stride;
        for (int mb = 0, sOff = 0; mb < mbPerSlice; mb++, mbTopLeftOff += 8) {
            int lineOff = mbTopLeftOff;
            for (int line = 0; line < 8; line++) {
                for (int col = 0; col < 4; col++) {
                    int round = MathUtil.clip((luma[sOff + col] + 2) >> 2, 1, 255);
                    y[lineOff + col] = (byte) (round - 128);
                }
                for (int col = 4; col < 8; col++) {
                    int round = MathUtil.clip((luma[sOff + col + 12] + 2) >> 2, 1, 255);
                    y[lineOff + col] = (byte) (round - 128);
                }

                sOff += srcIncLuma[line];
                lineOff += stride;
            }
        }

        if (lowBits != null) {
            mbTopLeftOff = fieldOffset + (mbX << 3) + (mbY << 3) * stride;
            for (int mb = 0, sOff = 0; mb < mbPerSlice; mb++, mbTopLeftOff += 8) {
                int lineOff = mbTopLeftOff;
                for (int line = 0; line < 4; line++) {
                    for (int col = 0; col < 4; col++) {
                        int val = MathUtil.clip(luma[sOff + col], 4, 1019);
                        int round = MathUtil.clip((luma[sOff + col] + 2) >> 2, 1, 255);
                        lowBits[lineOff + col] = (byte) (val - (round << 2));
                    }
                    for (int col = 4; col < 8; col++) {
                        int val = MathUtil.clip(luma[sOff + col + 12], 4, 1019);
                        int round = MathUtil.clip((luma[sOff + col] + 2) >> 2, 1, 255);
                        lowBits[lineOff + col] = (byte) (val - (round << 2));
                    }

                    sOff += srcIncLuma[line];
                    lineOff += stride;
                }
            }
        }
    }

    private void _putChroma(byte[] y, byte[] lowBits, int fieldOff, int stride, int mbX, int mbY, int[] chroma,
            int mbPerSlice, int dist, int shift) {
        int mbTopLeftOff = fieldOff + (mbX << 2) + (mbY << 3) * stride;
        for (int k = 0, sOff = 0; k < mbPerSlice; k++, mbTopLeftOff += 4) {
            int lineOff = mbTopLeftOff;
            for (int line = 0; line < 8; line++) {
                for (int col = 0; col < 4; col++) {
                    int round = MathUtil.clip((chroma[sOff + col] + 2) >> 2, 1, 255);
                    y[lineOff + col] = (byte) (round - 128);
                }

                sOff += 4;
                lineOff += stride;
            }
        }

        if (lowBits != null) {
            mbTopLeftOff = fieldOff + (mbX << 2) + (mbY << 3) * stride;
            for (int k = 0, sOff = 0; k < mbPerSlice; k++, mbTopLeftOff += 4) {
                int lineOff = mbTopLeftOff;
                for (int line = 0; line < 8; line++) {
                    for (int col = 0; col < 4; col++) {
                        int val = MathUtil.clip(chroma[sOff + col], 4, 1019);
                        int round = MathUtil.clip((chroma[sOff + col] + 2) >> 2, 1, 255);
                        lowBits[lineOff + col] = (byte) (val - (round << 2));
                    }

                    sOff += 4;
                    lineOff += stride;
                }
            }
        }
    }
}