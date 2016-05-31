package org.jcodec.codecs.mpeg12;
import static org.jcodec.codecs.mpeg12.MPEGConst.BLOCK_TO_CC;
import static org.jcodec.codecs.mpeg12.MPEGConst.SQUEEZE_X;
import static org.jcodec.codecs.mpeg12.MPEGConst.SQUEEZE_Y;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcCoeff0;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcDCSizeChroma;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcDCSizeLuma;
import static org.jcodec.codecs.mpeg12.bitstream.SequenceExtension.Chroma420;

import org.jcodec.codecs.mpeg12.bitstream.PictureHeader;
import org.jcodec.codecs.mpeg12.bitstream.SequenceHeader;
import org.jcodec.common.dct.IDCT4x4;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.VLC;

import java.util.Arrays;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * MPEG 1/2 Decoder, downscaled 4x4
 * 
 * @author The JCodec project
 * 
 */
public class Mpeg2Thumb4x4 extends MPEGDecoder {
    private MPEGPred localPred;
    private MPEGPred oldPred;

    protected void blockIntra(BitReader bits, VLC vlcCoeff, int[] block, int[] intra_dc_predictor, int blkIdx,
            int[] scan, int escSize, int intra_dc_mult, int qScale, int[] qmat) {
        int cc = BLOCK_TO_CC[blkIdx];
        int size = (cc == 0 ? vlcDCSizeLuma : vlcDCSizeChroma).readVLC(bits);
        int delta = (size != 0) ? mpegSigned(bits, size) : 0;
        intra_dc_predictor[cc] = intra_dc_predictor[cc] + delta;
        Arrays.fill(block, 1, 16, 0);
        block[0] = intra_dc_predictor[cc] * intra_dc_mult;

        int idx, readVLC = 0;
        for (idx = 0; idx < 19 + (scan == scan4x4[1] ? 7 : 0);) {
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
        IDCT4x4.idct(block, 0);
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
        Arrays.fill(block, 1, 16, 0);

        int idx = -1;
        if (vlcCoeff == vlcCoeff0 && bits.checkNBit(1) == 1) {
            bits.read1Bit();
            block[0] = toSigned(quantInter(1, qScale * qmat[0]), bits.read1Bit());
            idx++;
        } else {
            block[0] = 0;
        }

        int readVLC = 0;
        for (; idx < 19 + (scan == scan4x4[1] ? 7 : 0);) {
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
        IDCT4x4.idct(block, 0);
    }

    @Override
    public int decodeMacroblock(PictureHeader ph, Context context, int prevAddr, int[] qScaleCode, byte[][] buf,
            int stride, BitReader bits, int vertOff, int vertStep, MPEGPred pred) {
        if (localPred == null || oldPred != pred) {
            localPred = new MPEGPredDbl(pred);
            oldPred = pred;
        }

        return super.decodeMacroblock(ph, context, prevAddr, qScaleCode, buf, stride, bits, vertOff, vertStep,
                localPred);
    }

    public static int[] BLOCK_POS_X = new int[] { 0, 4, 0, 4, 0, 0, 0, 0, 4, 4, 4, 4, 0, 0, 0, 0, 0, 4, 0, 4, 0, 0, 0,
            0, 4, 4, 4, 4 };
    public static int[] BLOCK_POS_Y = new int[] { 0, 0, 4, 4, 0, 0, 4, 4, 0, 0, 4, 4, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1,
            1, 0, 0, 1, 1 };

    protected void mapBlock(int[] block, int[] out, int blkIdx, int dctType, int chromaFormat) {
        int stepVert = chromaFormat == Chroma420 && (blkIdx == 4 || blkIdx == 5) ? 0 : dctType;
        int log2stride = blkIdx < 4 ? 3 : 3 - SQUEEZE_X[chromaFormat];

        int blkIdxExt = blkIdx + (dctType << 4);
        int x = BLOCK_POS_X[blkIdxExt];
        int y = BLOCK_POS_Y[blkIdxExt];
        int off = (y << log2stride) + x, stride = 1 << (log2stride + stepVert);

        for (int i = 0; i < 16; i += 4, off += stride) {
            out[off] += block[i];
            out[off + 1] += block[i + 1];
            out[off + 2] += block[i + 2];
            out[off + 3] += block[i + 3];
        }
    }

    protected void put(int[][] mbPix, byte[][] buf, int stride, int chromaFormat, int mbX, int mbY, int width,
            int height, int vertOff, int vertStep) {

        int chromaStride = (stride + (1 << SQUEEZE_X[chromaFormat]) - 1) >> SQUEEZE_X[chromaFormat];
        int chromaMBW = 3 - SQUEEZE_X[chromaFormat];
        int chromaMBH = 3 - SQUEEZE_Y[chromaFormat];

        putSub(buf[0], (mbY << 3) * (stride << vertStep) + vertOff * stride + (mbX << 3), stride << vertStep, mbPix[0],
                3, 3);
        putSub(buf[1], (mbY << chromaMBH) * (chromaStride << vertStep) + vertOff * chromaStride + (mbX << chromaMBW),
                chromaStride << vertStep, mbPix[1], chromaMBW, chromaMBH);
        putSub(buf[2], (mbY << chromaMBH) * (chromaStride << vertStep) + vertOff * chromaStride + (mbX << chromaMBW),
                chromaStride << vertStep, mbPix[2], chromaMBW, chromaMBH);
    }

    @Override
    protected void putSub(byte[] big, int off, int stride, int[] block, int mbW, int mbH) {
        int blOff = 0;

        if (mbW == 2) {
            for (int i = 0; i < (1 << mbH); i++) {
                big[off] = clipTo8Bit(block[blOff]);
                big[off + 1] = clipTo8Bit(block[blOff + 1]);
                big[off + 2] = clipTo8Bit(block[blOff + 2]);
                big[off + 3] = clipTo8Bit(block[blOff + 3]);

                blOff += 4;
                off += stride;
            }
        } else {
            for (int i = 0; i < (1 << mbH); i++) {
                big[off] = clipTo8Bit(block[blOff]);
                big[off + 1] = clipTo8Bit(block[blOff + 1]);
                big[off + 2] = clipTo8Bit(block[blOff + 2]);
                big[off + 3] = clipTo8Bit(block[blOff + 3]);
                big[off + 4] = clipTo8Bit(block[blOff + 4]);
                big[off + 5] = clipTo8Bit(block[blOff + 5]);
                big[off + 6] = clipTo8Bit(block[blOff + 6]);
                big[off + 7] = clipTo8Bit(block[blOff + 7]);

                blOff += 8;
                off += stride;
            }
        }
    }

    public static int[][] scan4x4 = new int[][] {
            new int[] { 0, 1, 4, 8, 5, 2, 3, 6, 9, 12, 16, 13, 10, 7, 16, 16, 16, 11, 14, 16, 16, 16, 16, 16, 15, 16,
                    16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16,
                    16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16 },
            new int[] { 0, 4, 8, 12, 1, 5, 2, 6, 9, 13, 16, 16, 16, 16, 16, 16, 16, 16, 14, 10, 3, 7, 16, 16, 11, 15,
                    16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16,
                    16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16 } };

    protected Context initContext(SequenceHeader sh, PictureHeader ph) {
        Context context = super.initContext(sh, ph);
        context.codedWidth >>= 1;
        context.codedHeight >>= 1;
        context.picWidth >>= 1;
        context.picHeight >>= 1;

        context.scan = scan4x4[ph.pictureCodingExtension == null ? 0 : ph.pictureCodingExtension.alternate_scan];

        return context;
    }
}
