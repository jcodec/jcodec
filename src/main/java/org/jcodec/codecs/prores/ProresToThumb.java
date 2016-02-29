package org.jcodec.codecs.prores;

import java.nio.ByteBuffer;

import org.jcodec.codecs.prores.ProresConsts.FrameHeader;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;

/**
 * 
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Decodes a ProRes file in low res. Decodes each 8x8 block as 1 pixel.
 * 
 * @author The JCodec project
 * 
 */
public class ProresToThumb extends ProresDecoder {

    public ProresToThumb() {
    }

    @Override
    protected void decodeOnePlane(BitReader bits, int blocksPerSlice,  int[] out, int[] qMat, int[] scan, int mbX, int mbY,
            int plane) {
        try {
            readDCCoeffs(bits, qMat, out, blocksPerSlice, 1);
        } catch (RuntimeException e) {
            System.err.println("Suppressing slice error at [" + mbX + ", " + mbY + "].");
        }

        for (int i = 0; i < blocksPerSlice; i++) {
            out[i] >>= 3;
        }
    }

    public Picture8Bit decodeFrame8Bit(ByteBuffer data, byte[][] target) {
        FrameHeader fh = readFrameHeader(data);

        int codedWidth = ((fh.width + 15) & ~0xf) >> 3;
        int codedHeight = ((fh.height + 15) & ~0xf) >> 3;

        int lumaSize = codedWidth * codedHeight;
        int chromaSize = lumaSize >> 1;

        if (target == null || target[0].length < lumaSize || target[1].length < chromaSize
                || target[2].length < chromaSize) {
            throw new RuntimeException("Provided output picture won't fit into provided buffer");
        }

        if (fh.frameType == 0) {
            decodePicture(data, target, codedWidth, codedHeight, codedWidth >> 1, fh.qMatLuma, fh.qMatChroma,
                    new int[] { 0 }, 0, fh.chromaType);
        } else {
            decodePicture(data, target, codedWidth, codedHeight >> 1, codedWidth >> 1, fh.qMatLuma, fh.qMatChroma,
                    new int[] { 0 }, fh.topFieldFirst ? 1 : 2, fh.chromaType);

            decodePicture(data, target, codedWidth, codedHeight >> 1, codedWidth >> 1, fh.qMatLuma, fh.qMatChroma,
                    new int[] { 0 }, fh.topFieldFirst ? 2 : 1, fh.chromaType);
        }

        return Picture8Bit.createPicture8Bit(codedWidth, codedHeight, target, fh.chromaType == 2 ? ColorSpace.YUV422
                : ColorSpace.YUV444);
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
        off += (mbX << 1) + (mbY << 1) * stride;
        for (int k = 0, sOff = 0; k < mbPerSlice; k++) {
            y[off] = clipTo8Bit(luma[sOff], 4, 1019);
            y[off + 1] = clipTo8Bit(luma[sOff + 1], 4, 1019);

            off += stride;

            y[off] = clipTo8Bit(luma[sOff + 2], 4, 1019);
            y[off + 1] = clipTo8Bit(luma[sOff + 3], 4, 1019);

            off += 2 - stride;
            sOff += 4;
        }
    }

    private void _putChroma(byte[] y, int off, int stride, int mbX, int mbY, int[] chroma, int mbPerSlice, int dist,
            int shift) {
        off += mbX + (mbY << 1) * stride;
        for (int k = 0, sOff = 0; k < mbPerSlice; k++) {

            y[off] = clipTo8Bit(chroma[sOff], 4, 1019);

            off += stride;

            y[off] = clipTo8Bit(chroma[sOff + 1], 4, 1019);

            off += 1 - stride;
            sOff += 2;
        }
    }
}