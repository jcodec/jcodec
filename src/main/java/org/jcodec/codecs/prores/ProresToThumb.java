package org.jcodec.codecs.prores;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.codecs.prores.ProresConsts.FrameHeader;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;

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

    protected int[] decodeOnePlane(BitReader bits, int blocksPerSlice, int[] qMat, int[] scan, int mbX, int mbY,
            int plane) {
        int[] out = new int[blocksPerSlice];
        readDCCoeffs(bits, qMat, out, blocksPerSlice, 1);

        for (int i = 0; i < blocksPerSlice; i++) {
            out[i] >>= 3;
        }

        return out;
    }

    public Picture decodeFrame(ByteBuffer data, int[][] target) {
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
                    new int[] { 0 }, 0);
        } else {
            decodePicture(data, target, codedWidth, codedHeight >> 1, codedWidth >> 1, fh.qMatLuma, fh.qMatChroma,
                    new int[] { 0 }, fh.topFieldFirst ? 1 : 2);

            decodePicture(data, target, codedWidth, codedHeight >> 1, codedWidth >> 1, fh.qMatLuma, fh.qMatChroma,
                    new int[] { 0 }, fh.topFieldFirst ? 2 : 1);
        }

        return new Picture(codedWidth, codedHeight, target, ColorSpace.YUV422_10);
    }

    protected void putSlice(int[][] result, int lumaStride, int mbX, int mbY, int[] y, int[] u, int[] v, int dist,
            int shift) {
        int mbPerSlice = y.length >> 2;

        int chromaStride = lumaStride >> 1;

        putLuma(result[0], shift * lumaStride, lumaStride << dist, mbX, mbY, y, mbPerSlice, dist, shift);
        putChroma(result[1], shift * chromaStride, chromaStride << dist, mbX, mbY, u, mbPerSlice, dist, shift);
        putChroma(result[2], shift * chromaStride, chromaStride << dist, mbX, mbY, v, mbPerSlice, dist, shift);
    }

    private void putLuma(int[] y, int off, int stride, int mbX, int mbY, int[] luma, int mbPerSlice, int dist, int shift) {
        off += (mbX << 1) + (mbY << 1) * stride;
        for (int k = 0, sOff = 0; k < mbPerSlice; k++) {
            y[off] = clip(luma[sOff], 4, 1019);
            y[off + 1] = clip(luma[sOff + 1], 4, 1019);

            off += stride;

            y[off] = clip(luma[sOff + 2], 4, 1019);
            y[off + 1] = clip(luma[sOff + 3], 4, 1019);

            off += 2 - stride;
            sOff += 4;
        }
    }

    private void putChroma(int[] y, int off, int stride, int mbX, int mbY, int[] chroma, int mbPerSlice, int dist,
            int shift) {
        off += mbX + (mbY << 1) * stride;
        for (int k = 0, sOff = 0; k < mbPerSlice; k++) {

            y[off] = clip(chroma[sOff], 4, 1019);

            off += stride;

            y[off] = clip(chroma[sOff + 1], 4, 1019);

            off += 1 - stride;
            sOff += 2;
        }
    }
}