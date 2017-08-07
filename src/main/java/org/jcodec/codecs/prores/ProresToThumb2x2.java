package org.jcodec.codecs.prores;
import org.jcodec.codecs.prores.ProresConsts.FrameHeader;
import org.jcodec.common.dct.IDCT2x2;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rect;
import org.jcodec.common.tools.MathUtil;

import java.nio.ByteBuffer;

/**
 * 
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Decodes a ProRes file in low res. Decodes each 8x8 block as a downscaled 2x2
 * block.
 * 
 * @author The JCodec project
 * 
 */
public class ProresToThumb2x2 extends ProresDecoder {

    public ProresToThumb2x2() {
    }

    @Override
    protected void decodeOnePlane(BitReader bits, int blocksPerSlice, int[] out, int[] qMat, int[] scan, int mbX,
            int mbY, int plane) {

        readDCCoeffs(bits, qMat, out, blocksPerSlice, 4);
        readACCoeffs(bits, qMat, out, blocksPerSlice, scan, 4, 2);

        for (int i = 0; i < blocksPerSlice; i++) {
            IDCT2x2.idct(out, i << 2);
        }
    }

    @Override
    public Picture decodeFrameHiBD(ByteBuffer data, byte[][] target, byte[][] lowBits) {
        FrameHeader fh = readFrameHeader(data);

        int codedWidth = ((fh.width + 15) & ~0xf) >> 2;
        int codedHeight = ((fh.height + 15) & ~0xf) >> 2;

        int lumaSize = codedWidth * codedHeight;
        int chromaSize = lumaSize >> 1;

        if (target == null || target[0].length < lumaSize || target[1].length < chromaSize
                || target[2].length < chromaSize) {
            throw new RuntimeException("Provided output picture won't fit into provided buffer");
        }

        if (fh.frameType == 0) {
            decodePicture(data, target, lowBits, codedWidth, codedHeight, codedWidth >> 2, fh.qMatLuma, fh.qMatChroma,
                    new int[] { 0, 1, 2, 3 }, 0, fh.chromaType);
        } else {
            decodePicture(data, target, lowBits, codedWidth, codedHeight >> 1, codedWidth >> 2, fh.qMatLuma, fh.qMatChroma,
                    new int[] { 0, 2, 1, 3 }, fh.topFieldFirst ? 1 : 2, fh.chromaType);

            decodePicture(data, target, lowBits, codedWidth, codedHeight >> 1, codedWidth >> 2, fh.qMatLuma, fh.qMatChroma,
                    new int[] { 0, 2, 1, 3 }, fh.topFieldFirst ? 2 : 1, fh.chromaType);
        }

        ColorSpace color = fh.chromaType == 2 ? ColorSpace.YUV422 : ColorSpace.YUV444;
        return new Picture(codedWidth, codedHeight, target, lowBits, color, lowBits == null ? 0 : 2, new Rect(0,
                0, (fh.width >> 2) & color.getWidthMask(), (fh.height >> 2) & color.getHeightMask()));
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

    private void _putLuma(byte[] y, byte[] lowBits, int off, int stride, int mbX, int mbY, int[] luma,
            int mbPerSlice, int dist, int shift) {
        off += (mbX << 2) + (mbY << 2) * stride;
        int tstride = stride * 3;
        for (int k = 0, sOff = 0; k < mbPerSlice; k++) {
            putGroup(y, lowBits, off, luma, sOff);
            off += stride;
            putGroup(y, lowBits, off, luma, sOff + 2);
            off += stride;
            putGroup(y, lowBits, off, luma, sOff + 8);
            off += stride;
            putGroup(y, lowBits, off, luma, sOff + 10);

            off += 4 - tstride;
            sOff += 16;
        }
    }

    private void putGroup(byte[] y, byte[] lowBits, int off, int[] luma, int sOff) {
        int round0 = MathUtil.clip((luma[sOff] + 2) >> 2, 1, 255);
        int round1 = MathUtil.clip((luma[sOff + 1] + 2) >> 2, 1, 255);
        int round2 = MathUtil.clip((luma[sOff + 4] + 2) >> 2, 1, 255);
        int round3 = MathUtil.clip((luma[sOff + 5] + 2) >> 2, 1, 255);

        y[off] = (byte) (round0 - 128);
        y[off + 1] = (byte) (round1 - 128);
        y[off + 2] = (byte) (round2 - 128);
        y[off + 3] = (byte) (round3 - 128);
        
        if (lowBits != null) {
            int val0 = MathUtil.clip(luma[sOff], 4, 1019);
            int val1 = MathUtil.clip(luma[sOff + 1], 4, 1019);
            int val2 = MathUtil.clip(luma[sOff + 4], 4, 1019);
            int val3 = MathUtil.clip(luma[sOff + 5], 4, 1019);
            
            lowBits[off] = (byte) (val0 - (round0 << 2));
            lowBits[off + 1] = (byte) (val1 - (round1 << 2));
            lowBits[off + 2] = (byte) (val2 - (round2 << 2));
            lowBits[off + 3] = (byte) (val3 - (round3 << 2));
        }
    }

    private void _putChroma(byte[] y, byte[] lowBits, int off_, int stride, int mbX, int mbY, int[] chroma,
            int mbPerSlice, int dist, int shift) {
        int off = off_ + (mbX << 1) + (mbY << 2) * stride;
        for (int k = 0, sOff = 0; k < mbPerSlice; k++) {

            for (int row = 0, rowOff = off; row < 4; row++, rowOff += stride, sOff += 2) {
                int round0 = MathUtil.clip((chroma[sOff] + 2) >> 2, 1, 255);
                int round1 = MathUtil.clip((chroma[sOff + 1] + 2) >> 2, 1, 255);

                y[rowOff] = (byte) (round0 - 128);
                y[rowOff + 1] = (byte) (round1 - 128);
            }

            off += 2;
        }

        if (lowBits != null) {
            off = off_ + (mbX << 1) + (mbY << 2) * stride;
            for (int k = 0, sOff = 0; k < mbPerSlice; k++) {

                for (int row = 0, rowOff = off; row < 4; row++, rowOff += stride, sOff += 2) {
                    int val0 = MathUtil.clip(chroma[sOff], 4, 1019);
                    int val1 = MathUtil.clip(chroma[sOff + 1], 4, 1019);
                    int round0 = MathUtil.clip((chroma[sOff] + 2) >> 2, 1, 255);
                    int round1 = MathUtil.clip((chroma[sOff + 1] + 2) >> 2, 1, 255);

                    lowBits[rowOff] = (byte) (val0 - (round0 << 2));
                    lowBits[rowOff + 1] = (byte) (val1 - (round1 << 2));
                }

                off += 2;
            }
        }
    }
}