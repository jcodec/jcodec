package org.jcodec.codecs.mpeg12;

import static org.jcodec.codecs.mpeg12.MPEGConst.BLOCK_TO_CC;
import static org.jcodec.codecs.mpeg12.MPEGConst.SQUEEZE_X;
import static org.jcodec.codecs.mpeg12.MPEGConst.SQUEEZE_Y;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcCoeff0;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcDCSizeChroma;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcDCSizeLuma;
import static org.jcodec.codecs.mpeg12.bitstream.SequenceExtension.Chroma420;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.codecs.mpeg12.bitstream.PictureHeader;
import org.jcodec.codecs.mpeg12.bitstream.SequenceHeader;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.dct.IDCT2x2;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.VLC;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * MPEG 1/2 Decoder, downscaled 2x2
 * 
 * @author The JCodec project
 * 
 */
public class Mpeg2Thumb2x2 extends MPEGDecoder {
    private MPEGPred localPred;
    private MPEGPred oldPred;

    protected void blockIntra(BitReader bits, VLC vlcCoeff, int[] block, int[] intra_dc_predictor, int blkIdx,
            int[] scan, int escSize, int intra_dc_mult, int qScale, int[] qmat) {
        int cc = BLOCK_TO_CC[blkIdx];
        int size = (cc == 0 ? vlcDCSizeLuma : vlcDCSizeChroma).readVLC(bits);
        int delta = (size != 0) ? mpegSigned(bits, size) : 0;
        intra_dc_predictor[cc] = intra_dc_predictor[cc] + delta;
        block[0] = intra_dc_predictor[cc] * intra_dc_mult;
        block[1] = block[2] = block[3] = 0;
        int idx, readVLC = 0;
        for (idx = 0; idx < 6;) {
            readVLC = vlcCoeff.readVLC(bits);
            int level;

            if (readVLC == MPEGConst.CODE_END) {
                break;
            } else if (readVLC == MPEGConst.CODE_ESCAPE) {
                idx += bits.readNBit(6) + 1;
                level = twosSigned(bits, escSize) * qScale * qmat[idx];
                level = level >= 0 ? (level >> 4) : -(-level >> 4);
            } else {
                idx += (readVLC >> 6) + 1;
                level = toSigned(((readVLC & 0x3f) * qScale * qmat[idx]) >> 4, bits.read1Bit());
            }
            block[scan[idx]] = level;
        }
        if (readVLC != MPEGConst.CODE_END)
            finishOff(bits, idx, vlcCoeff, escSize);
        IDCT2x2.idct(block, 0);
    }

    private void finishOff(BitReader bits, int idx, VLC vlcCoeff, int escSize) {
        for (; idx < 64;) {
            int readVLC = vlcCoeff.readVLC(bits);

            if (readVLC == MPEGConst.CODE_END) {
                break;
            } else if (readVLC == MPEGConst.CODE_ESCAPE) {
                idx += bits.readNBit(6) + 1;
                bits.readNBit(escSize);
            } else {
                bits.read1Bit();
            }
        }
    }

    protected void blockInter(BitReader bits, VLC vlcCoeff, int[] block, int[] scan, int escSize, int qScale, int[] qmat) {
        block[1] = block[2] = block[3] = 0;

        int idx = -1;
        if (vlcCoeff == vlcCoeff0 && bits.checkNBit(1) == 1) {
            bits.read1Bit();
            block[0] = toSigned(quantInter(1, qScale * qmat[0]), bits.read1Bit());
            idx++;
        } else {
            block[0] = 0;
        }

        int readVLC = 0;
        for (; idx < 6;) {
            readVLC = vlcCoeff.readVLC(bits);
            int ac;
            if (readVLC == MPEGConst.CODE_END) {
                break;
            } else if (readVLC == MPEGConst.CODE_ESCAPE) {
                idx += bits.readNBit(6) + 1;
                ac = quantInterSigned(twosSigned(bits, escSize), qScale * qmat[idx]);
            } else {
                idx += (readVLC >> 6) + 1;
                ac = toSigned(quantInter(readVLC & 0x3f, qScale * qmat[idx]), bits.read1Bit());
            }
            block[scan[idx]] = ac;
        }
        if (readVLC != MPEGConst.CODE_END)
            finishOff(bits, idx, vlcCoeff, escSize);
        IDCT2x2.idct(block, 0);
    }

    static class Pred2x2 extends MPEGPred {
        public Pred2x2(MPEGPred other) {
            super(other);
        }

        @Override
        public void predictPlane(int[] ref, int refX, int refY, int refW, int refH, int refVertStep, int refVertOff,
                int[] tgt, int tgtY, int tgtW, int tgtH, int tgtVertStep) {
            super.predictPlane(ref, refX >> 2, refY >> 2, refW, refH, refVertStep, refVertOff, tgt, tgtY, tgtW >> 2,
                    tgtH >> 2, tgtVertStep);
        }
    }

    @Override
    public int decodeMacroblock(PictureHeader ph, Context context, int prevAddr, int[] qScaleCode, int[][] buf,
            int stride, BitReader bits, int vertOff, int vertStep, MPEGPred pred) {
        if (localPred == null || oldPred != pred) {
            localPred = new Pred2x2(pred);
            oldPred = pred;
        }

        return super.decodeMacroblock(ph, context, prevAddr, qScaleCode, buf, stride, bits, vertOff, vertStep,
                localPred);
    }

    protected void mapBlock(int[] block, int[] out, int blkIdx, int dctType, int chromaFormat) {
        int stepVert = chromaFormat == Chroma420 && (blkIdx == 4 || blkIdx == 5) ? 0 : dctType;
        int log2stride = blkIdx < 4 ? 2 : 2 - SQUEEZE_X[chromaFormat];

        int blkIdxExt = blkIdx + (dctType << 4);
        int x = MPEGConst.BLOCK_POS_X[blkIdxExt] >> 2;
        int y = MPEGConst.BLOCK_POS_Y[blkIdxExt] >> 2;
        int off = (y << log2stride) + x, stride = 1 << (log2stride + stepVert);

        out[off] += block[0];
        out[off + 1] += block[1];
        out[off + stride] += block[2];
        out[off + stride + 1] += block[3];
    }

    protected void put(int[][] mbPix, int[][] buf, int stride, int chromaFormat, int mbX, int mbY, int width,
            int height, int vertOff, int vertStep) {

        int chromaStride = (stride + (1 << SQUEEZE_X[chromaFormat]) - 1) >> SQUEEZE_X[chromaFormat];
        int chromaMBW = 2 - SQUEEZE_X[chromaFormat];
        int chromaMBH = 2 - SQUEEZE_Y[chromaFormat];

        putSub(buf[0], (mbY << 2) * (stride << vertStep) + vertOff * stride + (mbX << 2), stride << vertStep, mbPix[0],
                2, 2);
        putSub(buf[1], (mbY << chromaMBH) * (chromaStride << vertStep) + vertOff * chromaStride + (mbX << chromaMBW),
                chromaStride << vertStep, mbPix[1], chromaMBW, chromaMBH);
        putSub(buf[2], (mbY << chromaMBH) * (chromaStride << vertStep) + vertOff * chromaStride + (mbX << chromaMBW),
                chromaStride << vertStep, mbPix[2], chromaMBW, chromaMBH);
    }

    private final void putSub(int[] big, int off, int stride, int[] block, int mbW, int mbH) {
        int blOff = 0;

        if (mbW == 1) {
            big[off] = clip(block[blOff]);
            big[off + 1] = clip(block[blOff + 1]);
            big[off + stride] = clip(block[blOff + 2]);
            big[off + stride + 1] = clip(block[blOff + 3]);

            if (mbH == 2) {
                off += stride << 1;

                big[off] = clip(block[blOff + 4]);
                big[off + 1] = clip(block[blOff + 5]);
                big[off + stride] = clip(block[blOff + 6]);
                big[off + stride + 1] = clip(block[blOff + 7]);
            }
        } else {
            for (int i = 0; i < 4; i++) {
                big[off] = clip(block[blOff]);
                big[off + 1] = clip(block[blOff + 1]);
                big[off + 2] = clip(block[blOff + 2]);
                big[off + 3] = clip(block[blOff + 3]);

                off += stride;
                blOff += 4;
            }
        }
    }

    public static int[][] scan2x2 = new int[][] {
            new int[] { 0, 1, 2, 4, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                    4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4 },
            new int[] { 0, 2, 4, 4, 1, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                    4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4 } };

    protected Context initContext(SequenceHeader sh, PictureHeader ph) {
        Context context = super.initContext(sh, ph);
        context.codedWidth >>= 2;
        context.codedHeight >>= 2;
        context.picWidth >>= 2;
        context.picHeight >>= 2;

        context.scan = scan2x2[ph.pictureCodingExtension == null ? 0 : ph.pictureCodingExtension.alternate_scan];

        return context;
    }
}