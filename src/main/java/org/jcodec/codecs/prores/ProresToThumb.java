package org.jcodec.codecs.prores;
import org.jcodec.codecs.prores.ProresConsts.FrameHeader;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rect;
import org.jcodec.common.tools.MathUtil;

import java.lang.System;
import java.nio.ByteBuffer;

import js.lang.System;
import js.nio.ByteBuffer;

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
    protected void decodeOnePlane(BitReader bits, int blocksPerSlice, int[] out, int[] qMat, int[] scan, int mbX, int mbY,
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

    public Picture decodeFrameHiBD(ByteBuffer data, byte[][] target, byte[][] lowBits) {
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
            decodePicture(data, target, lowBits, codedWidth, codedHeight, codedWidth >> 1, fh.qMatLuma,
                    fh.qMatChroma, new int[] { 0 }, 0, fh.chromaType);
        } else {
            decodePicture(data, target, lowBits, codedWidth, codedHeight >> 1, codedWidth >> 1, fh.qMatLuma,
                    fh.qMatChroma, new int[] { 0 }, fh.topFieldFirst ? 1 : 2, fh.chromaType);

            decodePicture(data, target, lowBits, codedWidth, codedHeight >> 1, codedWidth >> 1, fh.qMatLuma,
                    fh.qMatChroma, new int[] { 0 }, fh.topFieldFirst ? 2 : 1, fh.chromaType);
        }

        ColorSpace color = fh.chromaType == 2 ? ColorSpace.YUV422 : ColorSpace.YUV444;
        return new Picture(codedWidth, codedHeight, target, lowBits, color, lowBits == null ? 0 : 2, new Rect(0,
                0, (fh.width >> 3) & color.getWidthMask(), (fh.height >> 3) & color.getHeightMask()));
    }

    @Override
    protected void putSlice(byte[][] result, byte[][] lowBits, int lumaStride, int mbX, int mbY, int[] y, int[] u, int[] v, int dist,
            int shift, int chromaType, int sliceMbCount) {
        int chromaStride = lumaStride >> 1;

        _putLuma(result[0], lowBits != null ? lowBits[0] : null, shift * lumaStride, lumaStride << dist, mbX, mbY,
                y, sliceMbCount, dist, shift);
        if (chromaType == 2) {
            _putChroma(result[1], lowBits != null ? lowBits[1] : null, shift * chromaStride, chromaStride << dist,
                    mbX, mbY, u, sliceMbCount, dist, shift);
            _putChroma(result[2], lowBits != null ? lowBits[2] : null, shift * chromaStride, chromaStride << dist,
                    mbX, mbY, v, sliceMbCount, dist, shift);
        } else {
            _putLuma(result[1], lowBits != null ? lowBits[1] : null, shift * lumaStride, lumaStride << dist, mbX,
                    mbY, u, sliceMbCount, dist, shift);
            _putLuma(result[2], lowBits != null ? lowBits[2] : null, shift * lumaStride, lumaStride << dist, mbX,
                    mbY, v, sliceMbCount, dist, shift);
        }
    }

    private void _putLuma(byte[] y, byte[] lowBits, int _off, int stride, int mbX, int mbY, int[] luma, int mbPerSlice, int dist, int shift) {
        int off = _off + (mbX << 1) + (mbY << 1) * stride;
        for (int k = 0, sOff = 0; k < mbPerSlice; k++) {
            int round0 = MathUtil.clip((luma[sOff] + 2) >> 2, 1, 255);
            y[off] = (byte) (round0 - 128);
            
            int round1 = MathUtil.clip((luma[sOff + 1] + 2) >> 2, 1, 255);
            y[off + 1] = (byte) (round1 - 128);
            
            off += stride;
            
            int round2 = MathUtil.clip((luma[sOff + 2] + 2) >> 2, 1, 255);
            y[off] = (byte) (round2 - 128);
            
            int round3 = MathUtil.clip((luma[sOff + 3] + 2) >> 2, 1, 255);
            y[off + 1] = (byte) (round3 - 128);

            off += 2 - stride;
            sOff += 4;
        }
        
        if (lowBits != null) {
            off = _off + (mbX << 1) + (mbY << 1) * stride;
            for (int k = 0, sOff = 0; k < mbPerSlice; k++) {
                int val0 = MathUtil.clip(luma[sOff], 4, 1019);
                int round0 = MathUtil.clip((luma[sOff] + 2) >> 2, 1, 255);
                lowBits[off] = (byte) (val0 - (round0 << 2));
                
                int val1 = MathUtil.clip(luma[sOff + 1], 4, 1019);
                int round1 = MathUtil.clip((luma[sOff + 1] + 2) >> 2, 1, 255);
                lowBits[off + 1] = (byte) (val1 - (round1 << 2));
                
                off += stride;
                
                int val2 = MathUtil.clip(luma[sOff + 2], 4, 1019);
                int round2 = MathUtil.clip((luma[sOff + 2] + 2) >> 2, 1, 255);
                lowBits[off] = (byte) (val2 - (round2 << 2));
                
                int val3 = MathUtil.clip(luma[sOff + 3], 4, 1019);
                int round3 = MathUtil.clip((luma[sOff + 3] + 2) >> 2, 1, 255);
                lowBits[off + 1] = (byte) (val3 - (round3 << 2));

                off += 2 - stride;
                sOff += 4;
            }
        }
    }

    private void _putChroma(byte[] y, byte[] lowBits, int _off, int stride, int mbX, int mbY, int[] chroma, int mbPerSlice, int dist,
            int shift) {
        int off = _off + mbX + (mbY << 1) * stride;
        for (int k = 0, sOff = 0; k < mbPerSlice; k++) {
            int round0 = MathUtil.clip((chroma[sOff] + 2) >> 2, 1, 255);
            y[off] = (byte) (round0 - 128);

            off += stride;

            int round1 = MathUtil.clip((chroma[sOff + 1] + 2) >> 2, 1, 255);
            y[off] = (byte) (round1 - 128);
            
            off += 1 - stride;
            sOff += 2;
        }
        
        if (lowBits != null) {
            off = _off + mbX + (mbY << 1) * stride;
            for (int k = 0, sOff = 0; k < mbPerSlice; k++) {
                
                int val0 = MathUtil.clip(chroma[sOff], 4, 1019);
                int round0 = MathUtil.clip((chroma[sOff] + 2) >> 2, 1, 255);
                lowBits[off] = (byte) (val0 - (round0 << 2));

                off += stride;

                int val1 = MathUtil.clip(chroma[sOff + 1], 4, 1019);
                int round1 = MathUtil.clip((chroma[sOff + 1] + 2) >> 2, 1, 255);
                lowBits[off] = (byte) (val1 - (round1 << 2));
                
                off += 1 - stride;
                sOff += 2;
            }
        }
    }
}