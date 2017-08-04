package org.jcodec.codecs.prores;
import org.jcodec.codecs.prores.ProresConsts.FrameHeader;
import org.jcodec.common.dct.IDCT2x2;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rect;

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
    protected void decodeOnePlane(BitReader bits, int blocksPerSlice, int[] out, int[] qMat, int[] scan, int mbX, int mbY,
            int plane) {

        readDCCoeffs(bits, qMat, out, blocksPerSlice, 4);
        readACCoeffs(bits, qMat, out, blocksPerSlice, scan, 4, 2);

        for (int i = 0; i < blocksPerSlice; i++) {
            IDCT2x2.idct(out, i << 2);
        }
    }

    @Override
    public Picture decodeFrame(ByteBuffer data, byte[][] target) {
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
            decodePicture(data, target, codedWidth, codedHeight, codedWidth >> 2, fh.qMatLuma, fh.qMatChroma,
                    new int[] { 0, 1, 2, 3 }, 0, fh.chromaType);
        } else {
            decodePicture(data, target, codedWidth, codedHeight >> 1, codedWidth >> 2, fh.qMatLuma, fh.qMatChroma,
                    new int[] { 0, 2, 1, 3 }, fh.topFieldFirst ? 1 : 2, fh.chromaType);

            decodePicture(data, target, codedWidth, codedHeight >> 1, codedWidth >> 2, fh.qMatLuma, fh.qMatChroma,
                    new int[] { 0, 2, 1, 3 }, fh.topFieldFirst ? 2 : 1, fh.chromaType);
        }

        ColorSpace color = fh.chromaType == 2 ? ColorSpace.YUV422 : ColorSpace.YUV444;
        return new Picture(codedWidth, codedHeight, target, color, new Rect(0, 0, (fh.width >> 2)
                & color.getWidthMask(), (fh.height >> 2) & color.getHeightMask()));
    }

    @Override
    protected void putSlice(byte[][] result, int lumaStride, int mbX, int mbY, int[] y, int[] u, int[] v, int dist,
            int shift, int chromaType, int sliceMbCount) {
        int chromaStride = lumaStride >> 1;

        _putLuma(result[0], shift * lumaStride, lumaStride << dist, mbX, mbY, y, sliceMbCount, dist, shift);
        if (chromaType == 2) {
            _putChroma(result[1], shift * chromaStride, chromaStride << dist, mbX, mbY, u, sliceMbCount, dist, shift);
            _putChroma(result[2], shift * chromaStride, chromaStride << dist, mbX, mbY, v, sliceMbCount, dist, shift);
        } else {
            _putLuma(result[1], shift * lumaStride, lumaStride << dist, mbX, mbY, u, sliceMbCount, dist, shift);
            _putLuma(result[2], shift * lumaStride, lumaStride << dist, mbX, mbY, v, sliceMbCount, dist, shift);
        }
    }

    private void _putLuma(byte[] y, int off, int stride, int mbX, int mbY, int[] luma, int mbPerSlice, int dist, int shift) {
        off += (mbX << 2) + (mbY << 2) * stride;
        int tstride = stride * 3;
        for (int k = 0, sOff = 0; k < mbPerSlice; k++) {
            y[off] = clipTo8Bit(luma[sOff], 4, 1019);
            y[off + 1] = clipTo8Bit(luma[sOff + 1], 4, 1019);
            y[off + 2] = clipTo8Bit(luma[sOff + 4], 4, 1019);
            y[off + 3] = clipTo8Bit(luma[sOff + 5], 4, 1019);

            off += stride;

            y[off] = clipTo8Bit(luma[sOff + 2], 4, 1019);
            y[off + 1] = clipTo8Bit(luma[sOff + 3], 4, 1019);
            y[off + 2] = clipTo8Bit(luma[sOff + 6], 4, 1019);
            y[off + 3] = clipTo8Bit(luma[sOff + 7], 4, 1019);

            off += stride;

            y[off] = clipTo8Bit(luma[sOff + 8], 4, 1019);
            y[off + 1] = clipTo8Bit(luma[sOff + 9], 4, 1019);
            y[off + 2] = clipTo8Bit(luma[sOff + 12], 4, 1019);
            y[off + 3] = clipTo8Bit(luma[sOff + 13], 4, 1019);

            off += stride;

            y[off] = clipTo8Bit(luma[sOff + 10], 4, 1019);
            y[off + 1] = clipTo8Bit(luma[sOff + 11], 4, 1019);
            y[off + 2] = clipTo8Bit(luma[sOff + 14], 4, 1019);
            y[off + 3] = clipTo8Bit(luma[sOff + 15], 4, 1019);

            off += 4 - tstride;
            sOff += 16;
        }
    }

    private void _putChroma(byte[] y, int off, int stride, int mbX, int mbY, int[] chroma, int mbPerSlice, int dist,
            int shift) {
        int tstride = stride * 3;
        off += (mbX << 1) + (mbY << 2) * stride;
        for (int k = 0, sOff = 0; k < mbPerSlice; k++) {

            y[off] = clipTo8Bit(chroma[sOff], 4, 1019);
            y[off + 1] = clipTo8Bit(chroma[sOff + 1], 4, 1019);

            off += stride;

            y[off] = clipTo8Bit(chroma[sOff + 2], 4, 1019);
            y[off + 1] = clipTo8Bit(chroma[sOff + 3], 4, 1019);

            off += stride;

            y[off] = clipTo8Bit(chroma[sOff + 4], 4, 1019);
            y[off + 1] = clipTo8Bit(chroma[sOff + 5], 4, 1019);

            off += stride;

            y[off] = clipTo8Bit(chroma[sOff + 6], 4, 1019);
            y[off + 1] = clipTo8Bit(chroma[sOff + 7], 4, 1019);

            off += 2 - tstride;
            sOff += 8;
        }
    }
}