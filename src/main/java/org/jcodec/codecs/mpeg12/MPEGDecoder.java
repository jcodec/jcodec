package org.jcodec.codecs.mpeg12;

import static java.lang.Math.min;
import static junit.framework.Assert.assertEquals;
import static org.jcodec.codecs.mpeg12.MPEGConst.BLOCK_TO_CC;
import static org.jcodec.codecs.mpeg12.MPEGConst.EXTENSION_START_CODE;
import static org.jcodec.codecs.mpeg12.MPEGConst.GROUP_START_CODE;
import static org.jcodec.codecs.mpeg12.MPEGConst.PICTURE_START_CODE;
import static org.jcodec.codecs.mpeg12.MPEGConst.SLICE_START_CODE_FIRST;
import static org.jcodec.codecs.mpeg12.MPEGConst.SLICE_START_CODE_LAST;
import static org.jcodec.codecs.mpeg12.MPEGConst.SQUEEZE_X;
import static org.jcodec.codecs.mpeg12.MPEGConst.SQUEEZE_Y;
import static org.jcodec.codecs.mpeg12.MPEGConst.mbTypeVal;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcAddressIncrement;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcCBP;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcCoeff0;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcCoeff1;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcDCSizeChroma;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcDCSizeLuma;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcMBType;
import static org.jcodec.codecs.mpeg12.bitstream.PictureCodingExtension.Frame;
import static org.jcodec.codecs.mpeg12.bitstream.SequenceExtension.Chroma420;
import static org.jcodec.codecs.mpeg12.bitstream.SequenceExtension.Chroma422;
import static org.jcodec.codecs.mpeg12.bitstream.SequenceExtension.Chroma444;
import static org.jcodec.codecs.mpeg12.bitstream.SequenceHeader.Sequence_Display_Extension;
import static org.jcodec.codecs.mpeg12.bitstream.SequenceHeader.Sequence_Extension;
import static org.jcodec.codecs.mpeg12.bitstream.SequenceHeader.Sequence_Scalable_Extension;
import static org.jcodec.common.model.ColorSpace.YUV420;
import static org.jcodec.common.model.ColorSpace.YUV422;
import static org.jcodec.common.model.ColorSpace.YUV444;

import java.io.IOException;

import org.jcodec.codecs.mpeg12.MPEGConst.MBType;
import org.jcodec.codecs.mpeg12.bitstream.GOPHeader;
import org.jcodec.codecs.mpeg12.bitstream.PictureHeader;
import org.jcodec.codecs.mpeg12.bitstream.SequenceExtension;
import org.jcodec.codecs.mpeg12.bitstream.SequenceHeader;
import org.jcodec.codecs.mpeg12.bitstream.SequenceScalableExtension;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.dct.SparseIDCT;
import org.jcodec.common.io.BitstreamReaderBB;
import org.jcodec.common.io.Buffer;
import org.jcodec.common.io.InBits;
import org.jcodec.common.io.VLC;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MPEGDecoder implements VideoDecoder {

    private SequenceHeader sh;
    private GOPHeader gh;
    private int maxCoeff;
    private Picture[] refFrames = new Picture[2];
    private Picture[] refFields = new Picture[2];

    public MPEGDecoder(SequenceHeader sh, GOPHeader gh) {
        this.sh = sh;
        this.gh = gh;
    }

    public MPEGDecoder() {
        this(64);
    }

    public MPEGDecoder(int maxCoeff) {
        this.maxCoeff = maxCoeff;
    }

    public class Context {
        int[] intra_dc_predictor = new int[3];
        public int mbWidth;
        int mbNo;
        public int codedWidth;
        public int codedHeight;
        public int mbHeight;
        public ColorSpace color;
        public int clipVal;
    }

    public Picture decodeFrame(Buffer buffer, int[][] buf) {

        try {
            PictureHeader ph = readHeader(buffer);
            Context context = initContext(sh);
            Picture pic = new Picture(sh.horizontal_size, sh.vertical_size, buf, context.color);
            if (ph.pictureCodingExtension != null && ph.pictureCodingExtension.picture_structure != Frame) {
                decodePicture(context, ph, buffer, buf, ph.pictureCodingExtension.picture_structure - 1, 1);
                ph = readHeader(buffer);
                context = initContext(sh);
                decodePicture(context, ph, buffer, buf, ph.pictureCodingExtension.picture_structure - 1, 1);
            } else {
                decodePicture(context, ph, buffer, buf, 0, 0);
            }

            if (ph.picture_coding_type == PictureHeader.IntraCoded
                    || ph.picture_coding_type == PictureHeader.PredictiveCoded) {
                Picture unused = refFrames[1];
                refFrames[1] = refFrames[0];
                refFrames[0] = copyAndCreateIfNeeded(pic, unused);
            }

            return pic;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Picture copyAndCreateIfNeeded(Picture src, Picture dst) {
        if (dst == null || !dst.compatible(src))
            dst = src.createCompatible();
        dst.copyFrom(src);
        return dst;
    }

    private PictureHeader readHeader(Buffer buffer) throws IOException {
        PictureHeader ph = null;
        Buffer segment;
        while ((segment = nextSegment(buffer)) != null) {
            if (segment.get(3) == MPEGConst.SEQUENCE_HEADER_CODE) {
                SequenceHeader newSh = SequenceHeader.read(segment.from(4));
                if (sh != null) {
                    newSh.copyExtensions(sh);
                }
                sh = newSh;
            } else if (segment.get(3) == GROUP_START_CODE) {
                gh = GOPHeader.read(segment.from(4));
            } else if (segment.get(3) == PICTURE_START_CODE) {
                ph = PictureHeader.read(segment.from(4));
            } else if (segment.get(3) == EXTENSION_START_CODE) {
                int extType = segment.get(4) >> 4;
                if (extType == Sequence_Extension || extType == Sequence_Scalable_Extension
                        || extType == Sequence_Display_Extension)
                    SequenceHeader.readExtension(segment.from(4), sh);
                else
                    PictureHeader.readExtension(segment.from(4), ph, sh);
            } else {
                buffer.pos = segment.pos;
                break;
            }
        }
        return ph;
    }

    private Context initContext(SequenceHeader sh) {
        Context context = new Context();
        context.codedWidth = (sh.horizontal_size + 15) & ~0xf;
        context.codedHeight = (sh.vertical_size + 15) & ~0xf;
        context.mbWidth = (sh.horizontal_size + 15) >> 4;
        context.mbHeight = (sh.vertical_size + 15) >> 4;

        int chromaFormat = Chroma420;
        if (sh.sequenceExtension != null)
            chromaFormat = sh.sequenceExtension.chroma_format;

        context.color = getColor(chromaFormat);

        context.clipVal = (1 << (10 - chromaFormat)) - 1;

        return context;
    }

    public Picture decodePicture(Context context, PictureHeader ph, Buffer buffer, int[][] buf, int vertOff,
            int vertStep) {

        int planeSize = context.codedWidth * context.codedHeight;
        if (buf.length < 3 || buf[0].length < planeSize || buf[1].length < planeSize || buf[2].length < planeSize) {
            throw new RuntimeException("Buffer too small to hold output picture");
        }

        try {
            Buffer segment;
            while ((segment = nextSegment(buffer)) != null) {
                if (segment.get(3) >= SLICE_START_CODE_FIRST && segment.get(3) <= SLICE_START_CODE_LAST) {
                    decodeSlice(ph, segment.get(3) & 0xff, context, buf, new BitstreamReaderBB(segment.from(4)),
                            vertOff, vertStep);
                } else if (segment.get(3) >= 0xB3 && segment.get(3) != 0xB6) {
                    throw new RuntimeException("Unexpected start code " + segment.get(3));
                }
            }

            Picture pic = new Picture(sh.horizontal_size, sh.vertical_size, buf, context.color);
            if ((ph.picture_coding_type == PictureHeader.IntraCoded || ph.picture_coding_type == PictureHeader.PredictiveCoded)
                    && ph.pictureCodingExtension != null && ph.pictureCodingExtension.picture_structure != Frame) {
                refFields[ph.pictureCodingExtension.picture_structure - 1] = copyAndCreateIfNeeded(pic,
                        refFields[ph.pictureCodingExtension.picture_structure - 1]);
            }

            return pic;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ColorSpace getColor(int chromaFormat) {
        switch (chromaFormat) {
        case Chroma420:
            return YUV420;
        case Chroma422:
            return YUV422;
        case Chroma444:
            return YUV444;
        }

        return null;
    }

    public static final Buffer nextSegment(Buffer buf) {
        if (buf.get(0) != 0 || buf.get(1) != 0 || buf.get(2) != 1) {
            int ind = buf.search(0, 0, 1);
            if (ind == -1)
                return null;
            buf.read(ind);
        }
        if (buf.remaining() < 4)
            return null;
        int ind = buf.from(4).search(0, 0, 1);
        return ind == -1 ? buf.read(buf.remaining()) : buf.read(ind + 4);
    }

    public void decodeSlice(PictureHeader ph, int verticalPos, Context context, int[][] buf, InBits in, int vertOff,
            int vertStep) throws IOException {

        int stride = sh.horizontal_size;

        resetDCPredictors(context, ph);

        int[] scan = MPEGConst.scan[ph.pictureCodingExtension == null ? 0 : ph.pictureCodingExtension.alternate_scan];

        // System.out.print(verticalPos + ", ");
        int mbRow = verticalPos - 1;
        if (sh.vertical_size > 2800) {
            mbRow += (in.readNBit(3) << 7);
        }
        if (sh.sequenceScalableExtension != null
                && sh.sequenceScalableExtension.scalable_mode == SequenceScalableExtension.DATA_PARTITIONING) {
            int priorityBreakpoint = in.readNBit(7);
        }
        int qScaleCode = in.readNBit(5);
        if (in.read1Bit() == 1) {
            int intraSlice = in.read1Bit();
            in.skip(7);
            while (in.read1Bit() == 1)
                in.readNBit(8);
        }

        MPEGPred pred = new MPEGPred(ph.pictureCodingExtension != null ? ph.pictureCodingExtension.f_code
                : new int[][] { new int[] { ph.forward_f_code, ph.forward_f_code },
                        new int[] { ph.backward_f_code, ph.backward_f_code } },
                sh.sequenceExtension != null ? sh.sequenceExtension.chroma_format : Chroma420);

        for (int prevAddr = mbRow * context.mbWidth - 1; in.checkNBit(23) != 0;) {
            // TODO: decode skipped!!!
            prevAddr = decodeMacroblock(ph, context, prevAddr, qScaleCode, scan, buf, stride, in, vertOff, vertStep,
                    pred);
            context.mbNo++;
        }
    }

    private void resetDCPredictors(Context context, PictureHeader ph) {
        int rval = 1 << 7;
        if (ph.pictureCodingExtension != null)
            rval = 1 << (7 + ph.pictureCodingExtension.intra_dc_precision);
        context.intra_dc_predictor[0] = context.intra_dc_predictor[1] = context.intra_dc_predictor[2] = rval;
    }

    public int decodeMacroblock(PictureHeader ph, Context context, int prevAddr, int qScaleCode, int[] scan,
            int[][] buf, int stride, InBits in, int vertOff, int vertStep, MPEGPred pred) throws IOException {
        int mbAddr = prevAddr;
        while (in.checkNBit(11) == 0x8) {
            in.skip(11);
            mbAddr += 33;
        }
        mbAddr += vlcAddressIncrement.readVLC(in) + 1;

        for (int i = prevAddr + 1; i < mbAddr; i++) {
            mvZero(ph, pred, i % context.mbWidth, i / context.mbWidth, buf);
        }

        VLC vlcMBType = vlcMBType(ph.picture_coding_type, sh.sequenceScalableExtension);
        MBType[] mbTypeVal = mbTypeVal(ph.picture_coding_type, sh.sequenceScalableExtension);

        MBType mbType = mbTypeVal[vlcMBType.readVLC(in)];

        // System.out.println(mbAddr
        // + ": "
        // + (mbType.macroblock_intra == 1 ? "intra" : "inter, "
        // + (mbType.macroblock_motion_forward == 1 ? "forward" : "")
        // + (mbType.macroblock_motion_backward == 1 ? "backward" : "")));

        if (mbType.macroblock_intra != 1 || (mbAddr - prevAddr) > 1) {
            resetDCPredictors(context, ph);
        }

        int spatial_temporal_weight_code = 0;
        if (mbType.spatial_temporal_weight_code_flag == 1 && ph.pictureSpatialScalableExtension != null
                && ph.pictureSpatialScalableExtension.spatial_temporal_weight_code_table_index != 0) {
            spatial_temporal_weight_code = in.readNBit(2);
        }

        int motion_type = -1;
        if (mbType.macroblock_motion_forward != 0 || mbType.macroblock_motion_backward != 0) {
            if (ph.pictureCodingExtension == null || ph.pictureCodingExtension.picture_structure == Frame
                    && ph.pictureCodingExtension.frame_pred_frame_dct == 1)
                motion_type = 2;
            else
                motion_type = in.readNBit(2);
        }

        int dctType = 0;
        if (ph.pictureCodingExtension != null && ph.pictureCodingExtension.picture_structure == Frame
                && ph.pictureCodingExtension.frame_pred_frame_dct == 0
                && (mbType.macroblock_intra != 0 || mbType.macroblock_pattern != 0)) {
            dctType = in.read1Bit();
        }
        // buf[3][mbAddr] = dctType;

        if (mbType.macroblock_quant != 0) {
            qScaleCode = in.readNBit(5);
        }
        boolean concealmentMv = ph.pictureCodingExtension != null
                && ph.pictureCodingExtension.concealment_motion_vectors != 0;

        int chromaFormat = Chroma420;
        if (sh.sequenceExtension != null)
            chromaFormat = sh.sequenceExtension.chroma_format;

        int mbX = mbAddr % context.mbWidth;
        int mbY = mbAddr / context.mbWidth;
        int[][] mbPix = new int[][] { new int[256], new int[1 << (chromaFormat + 5)], new int[1 << (chromaFormat + 5)] };
        if (mbType.macroblock_intra == 1) {
            if (concealmentMv) {
                // TODO read consealment vectors
            } else
                pred.reset();
        } else if (mbType.macroblock_motion_forward != 0) {
            if (ph.pictureCodingExtension == null || ph.pictureCodingExtension.picture_structure == Frame) {
                pred.predictInFrame(refFrames[0], mbX << 4, mbY << 4, mbPix, in, motion_type, 0,
                        spatial_temporal_weight_code);
            } else {
                if (ph.picture_coding_type == PictureHeader.PredictiveCoded) {
                    pred.predictInField(refFields, mbX << 4, mbY << 4, mbPix, in, motion_type, 0,
                            ph.pictureCodingExtension.picture_structure - 1);
                } else {
                    pred.predictInField(new Picture[] { refFrames[0], refFrames[0] }, mbX << 4, mbY << 4, mbPix, in,
                            motion_type, 0, ph.pictureCodingExtension.picture_structure - 1);
                }
            }
        } else if (ph.picture_coding_type == PictureHeader.PredictiveCoded) {
            mvZero(ph, pred, mbX, mbY, mbPix);
        }

        if (mbType.macroblock_motion_backward != 0) {
            if (ph.pictureCodingExtension == null || ph.pictureCodingExtension.picture_structure == Frame) {
                pred.predictInFrame(refFrames[1], mbX << 4, mbY << 4, mbPix, in, motion_type, 1,
                        spatial_temporal_weight_code);
            } else {
                pred.predictInField(new Picture[] { refFrames[1], refFrames[1] }, mbX << 4, mbY << 4, mbPix, in,
                        motion_type, 1, ph.pictureCodingExtension.picture_structure - 1);
            }
        }
        if (mbType.macroblock_intra != 0 && concealmentMv)
            assertEquals(1, in.read1Bit()); // Marker

        int cbp = mbType.macroblock_intra == 1 ? 0xfff : 0;
        if (mbType.macroblock_pattern != 0) {
            cbp = readCbPattern(in);
        }

        VLC vlcCoeff = vlcCoeff0;
        if (ph.pictureCodingExtension != null && mbType.macroblock_intra == 1
                && ph.pictureCodingExtension.intra_vlc_format == 1)
            vlcCoeff = vlcCoeff1;

        int[] qScaleTab = ph.pictureCodingExtension != null && ph.pictureCodingExtension.q_scale_type == 1 ? MPEGConst.qScaleTab2
                : MPEGConst.qScaleTab1;
        int qScale = qScaleTab[qScaleCode];

        int intra_dc_mult = 8;
        if (ph.pictureCodingExtension != null)
            intra_dc_mult = 8 >> ph.pictureCodingExtension.intra_dc_precision;

        // int chromaStride = (stride >> SQUEEZE_X[chromaFormat]);
        int blkCount = 6 + (chromaFormat == Chroma420 ? 0 : (chromaFormat == Chroma422 ? 2 : 6));
        for (int i = 0, cbpMask = 1 << (blkCount - 1); i < blkCount; i++, cbpMask >>= 1) {
            if ((cbp & cbpMask) == 0)
                continue;
            int[] qmat = getQmat(i < 4, mbType.macroblock_intra == 1, ph);

            if (mbType.macroblock_intra == 1)
                blockIntra(in, vlcCoeff, mbPix[BLOCK_TO_CC[i]], context.intra_dc_predictor, i, dctType, scan,
                        sh.hasExtensions() || ph.hasExtensions() ? 12 : 8, intra_dc_mult, qScale, qmat, chromaFormat);
            else
                blockInter(in, vlcCoeff, mbPix[BLOCK_TO_CC[i]], i, dctType, scan,
                        sh.hasExtensions() || ph.hasExtensions() ? 12 : 8, qScale, qmat, chromaFormat);
        }

        put(mbPix, buf, stride, chromaFormat, mbX, mbY, context.clipVal, sh.horizontal_size >> vertStep,
                sh.vertical_size >> vertStep, vertOff, vertStep);

        return mbAddr;
    }

    private void mvZero(PictureHeader ph, MPEGPred pred, int mbX, int mbY, int[][] mbPix) {
        pred.reset();
        pred.predict16x16NoMV(refFrames[0], mbX << 4, mbY << 4, ph.pictureCodingExtension == null ? Frame
                : ph.pictureCodingExtension.picture_structure, mbPix);
    }

    protected void put(int[][] mbPix, int[][] buf, int stride, int chromaFormat, int mbX, int mbY, int clipVal,
            int width, int height, int vertOff, int vertStep) {

        int chromaStride = (stride + (1 << SQUEEZE_X[chromaFormat]) - 1) >> SQUEEZE_X[chromaFormat];
        int chromaMBW = 4 - SQUEEZE_X[chromaFormat];
        int chromaMBH = 4 - SQUEEZE_Y[chromaFormat];

        if (width > ((mbX + 1) << 4) && height > ((mbY + 1) << 4)) {
            putSub(buf[0], (mbY << 4) * (stride << vertStep) + vertOff * stride + (mbX << 4), stride << vertStep,
                    mbPix[0], clipVal, 4, 4);
            putSub(buf[1], (mbY << chromaMBH) * (chromaStride << vertStep) + vertOff * chromaStride
                    + (mbX << chromaMBW), chromaStride << vertStep, mbPix[1], clipVal, chromaMBW, chromaMBH);
            putSub(buf[2], (mbY << chromaMBH) * (chromaStride << vertStep) + vertOff * chromaStride
                    + (mbX << chromaMBW), chromaStride << vertStep, mbPix[2], clipVal, chromaMBW, chromaMBH);
        } else {
            int chromaHeight = (height + (1 << SQUEEZE_Y[chromaFormat]) - 1) >> SQUEEZE_Y[chromaFormat];
            putEdge(buf[0], (mbY << 4) * (stride << vertStep) + vertOff * stride + (mbX << 4), stride << vertStep,
                    mbPix[0], 16, min(16, width - (mbX << 4)), min(16, height - (mbY << 4)), clipVal);
            putEdge(buf[1], (mbY << chromaMBH) * (chromaStride << vertStep) + vertOff * chromaStride
                    + (mbX << chromaMBW), chromaStride << vertStep, mbPix[1], 1 << chromaMBW,
                    min(1 << chromaMBW, chromaStride - (mbX << chromaMBW)),
                    min(1 << chromaMBH, chromaHeight - (mbY << chromaMBH)), clipVal);
            putEdge(buf[2], (mbY << chromaMBH) * (chromaStride << vertStep) + vertOff * chromaStride
                    + (mbX << chromaMBW), chromaStride << vertStep, mbPix[2], 1 << chromaMBW,
                    min(1 << chromaMBW, chromaStride - (mbX << chromaMBW)),
                    min(1 << chromaMBH, chromaHeight - (mbY << chromaMBH)), clipVal);
        }
    }

    private final void putEdge(int[] tgt, int tgtOff, int tgtStride, int[] blk, int blkStride, int blkW, int blkH,
            int clipVal) {
        int blkOff = 0, blkLf = blkStride - blkW, picLf = tgtStride - blkW;
        for (int i = 0; i < blkH; i++) {
            for (int j = 0; j < blkW; j++) {
                tgt[tgtOff++] = clip(blk[blkOff++], clipVal);
            }
            blkOff += blkLf;
            tgtOff += picLf;
        }
    }

    private final void putSub(int[] big, int off, int stride, int[] block, int clipVal, int mbW, int mbH) {
        int blOff = 0;

        if (mbW == 3) {
            for (int i = 0; i < (1 << mbH); i++) {
                big[off] = clip(block[blOff], clipVal);
                big[off + 1] = clip(block[blOff + 1], clipVal);
                big[off + 2] = clip(block[blOff + 2], clipVal);
                big[off + 3] = clip(block[blOff + 3], clipVal);
                big[off + 4] = clip(block[blOff + 4], clipVal);
                big[off + 5] = clip(block[blOff + 5], clipVal);
                big[off + 6] = clip(block[blOff + 6], clipVal);
                big[off + 7] = clip(block[blOff + 7], clipVal);

                blOff += 8;
                off += stride;
            }
        } else {
            for (int i = 0; i < (1 << mbH); i++) {
                big[off] = clip(block[blOff], clipVal);
                big[off + 1] = clip(block[blOff + 1], clipVal);
                big[off + 2] = clip(block[blOff + 2], clipVal);
                big[off + 3] = clip(block[blOff + 3], clipVal);
                big[off + 4] = clip(block[blOff + 4], clipVal);
                big[off + 5] = clip(block[blOff + 5], clipVal);
                big[off + 6] = clip(block[blOff + 6], clipVal);
                big[off + 7] = clip(block[blOff + 7], clipVal);
                big[off + 8] = clip(block[blOff + 8], clipVal);
                big[off + 9] = clip(block[blOff + 9], clipVal);
                big[off + 10] = clip(block[blOff + 10], clipVal);
                big[off + 11] = clip(block[blOff + 11], clipVal);
                big[off + 12] = clip(block[blOff + 12], clipVal);
                big[off + 13] = clip(block[blOff + 13], clipVal);
                big[off + 14] = clip(block[blOff + 14], clipVal);
                big[off + 15] = clip(block[blOff + 15], clipVal);

                blOff += 16;
                off += stride;
            }
        }
    }

    private static final int clip(int val, int clipVal) {
        return val < 0 ? 0 : (val > clipVal ? clipVal : val);
    }

    private final int[] getQmat(boolean luma, boolean intra, PictureHeader ph) {
        int[] mat = null;
        if (ph.quantMatrixExtension != null) {
            mat = luma ? (intra ? ph.quantMatrixExtension.intra_quantiser_matrix
                    : ph.quantMatrixExtension.non_intra_quantiser_matrix)
                    : (intra ? ph.quantMatrixExtension.chroma_intra_quantiser_matrix
                            : ph.quantMatrixExtension.chroma_non_intra_quantiser_matrix);
        }

        return mat != null ? mat : (intra ? (sh.intra_quantiser_matrix != null ? sh.intra_quantiser_matrix
                : MPEGConst.defaultQMatIntra) : (sh.non_intra_quantiser_matrix != null ? sh.non_intra_quantiser_matrix
                : MPEGConst.defaultQMatInter));
    }

    private void printBlk(int[] block, int off) {
        for (int i = 0; i < 64; i++) {
            System.out.print(block[off + i] + ",");
        }
        System.out.println();
    }

    private static final int quantInter(int level, int quant) {
        return level == 0 ? 0 : (((level << 1) + ((level >> 31) << 1) + 1) * quant) >> 5;
    }

    private final void blockIntra(InBits in, VLC vlcCoeff, int[] block, int[] intra_dc_predictor, int blkIdx,
            int dctType, int[] scan, int escSize, int intra_dc_mult, int qScale, int[] qmat, int chromaFormat)
            throws IOException {
        int cc = BLOCK_TO_CC[blkIdx];
        int blkStride = blkIdx < 4 ? 4 : 4 - SQUEEZE_X[chromaFormat];
        int size = (cc == 0 ? vlcDCSizeLuma : vlcDCSizeChroma).readVLC(in);
        int delta = (size != 0) ? mpegSigned(in, size) : 0;
        intra_dc_predictor[cc] = intra_dc_predictor[cc] + delta;
        int dc = intra_dc_predictor[cc] * intra_dc_mult;
        SparseIDCT.dc(block, blkStride, MPEGConst.BLOCK_POS_X[blkIdx], MPEGConst.BLOCK_POS_Y[blkIdx], dctType, dc);

        for (int idx = 0; idx < maxCoeff;) {
            int readVLC = vlcCoeff.readVLC(in);
            if (readVLC >= 0) {
                idx += (readVLC >> 12) + 1;
                int ridx = scan[idx];
                SparseIDCT.ac(block, blkStride, MPEGConst.BLOCK_POS_X[blkIdx], MPEGConst.BLOCK_POS_Y[blkIdx], dctType,
                        ridx, (toSigned(readVLC & 0xfff, in.read1Bit()) * qScale * qmat[ridx]) >> 4);
            } else if (readVLC == -2) {
                idx += in.readNBit(6) + 1;
                int ridx = scan[idx];
                SparseIDCT.ac(block, blkStride, MPEGConst.BLOCK_POS_X[blkIdx], MPEGConst.BLOCK_POS_Y[blkIdx], dctType,
                        ridx, (twosSigned(in, escSize) * qScale * qmat[ridx]) >> 4);
            } else
                break;
        }
    }

    private final void blockInter(InBits in, VLC vlcCoeff, int[] block, int blkIdx, int dctType, int[] scan,
            int escSize, int qScale, int[] qmat, int chromaFormat) throws IOException {

        int blkStride = blkIdx < 4 ? 4 : 4 - SQUEEZE_X[chromaFormat];
        if (vlcCoeff == vlcCoeff0 && in.checkNBit(1) == 1) {
            in.read1Bit();
            SparseIDCT.dc(block, blkStride, MPEGConst.BLOCK_POS_X[blkIdx], MPEGConst.BLOCK_POS_Y[blkIdx], dctType,
                    quantInter(1 - (in.read1Bit() << 1), qScale * qmat[0]));
        } else {
            int readVLC = vlcCoeff.readVLC(in);
            if (readVLC >= 0) {
                SparseIDCT.dc(block, blkStride, MPEGConst.BLOCK_POS_X[blkIdx], MPEGConst.BLOCK_POS_Y[blkIdx], dctType,
                        quantInter(toSigned(readVLC & 0xfff, in.read1Bit()), qScale * qmat[0]));
            } else {
                in.readNBit(6);
                SparseIDCT.dc(block, blkStride, MPEGConst.BLOCK_POS_X[blkIdx], MPEGConst.BLOCK_POS_Y[blkIdx], dctType,
                        quantInter(twosSigned(in, escSize), qScale * qmat[0]));
            }
        }

        for (int idx = 0; idx < maxCoeff;) {
            int readVLC = vlcCoeff.readVLC(in);
            if (readVLC >= 0) {
                idx += (readVLC >> 12) + 1;
                int rind = scan[idx];
                SparseIDCT.ac(block, blkStride, MPEGConst.BLOCK_POS_X[blkIdx], MPEGConst.BLOCK_POS_Y[blkIdx], dctType,
                        rind, quantInter(toSigned(readVLC & 0xfff, in.read1Bit()), qScale * qmat[rind]));
            } else if (readVLC == -2) {
                int j = in.readNBit(6) + 1;
                idx += j;
                int rind = scan[idx];
                SparseIDCT.ac(block, blkStride, MPEGConst.BLOCK_POS_X[blkIdx], MPEGConst.BLOCK_POS_Y[blkIdx], dctType,
                        rind, quantInter(twosSigned(in, escSize), qScale * qmat[rind]));
            } else
                break;
        }
    }

    private static final int twosSigned(InBits in, int size) throws IOException {
        int shift = 32 - size;
        return (in.readNBit(size) << shift) >> shift;
    }

    private static final int mpegSigned(InBits in, int size) throws IOException {
        int val = in.readNBit(size);
        int sign = (val >>> (size - 1)) ^ 0x1;
        return val + sign - (sign << size);
    }

    private static final int toSigned(int val, int s) {
        int sign = (s << 31) >> 31;
        return (val ^ sign) - sign;
    }

    private final int readCbPattern(InBits in) throws IOException {
        int cbp420 = vlcCBP.readVLC(in);
        if (sh.sequenceExtension.chroma_format == SequenceExtension.Chroma420)
            return cbp420;
        else if (sh.sequenceExtension.chroma_format == SequenceExtension.Chroma422)
            return (cbp420 << 2) | in.readNBit(2);
        else if (sh.sequenceExtension.chroma_format == SequenceExtension.Chroma444)
            return (cbp420 << 6) | in.readNBit(6);
        throw new RuntimeException("Unsupported chroma format: " + sh.sequenceExtension.chroma_format);
    }

    @Override
    public int probe(Buffer data) {
        if (data.get(0) == 0 && data.get(1) == 0 && data.get(2) == 1)
            if (data.get(3) == 0 || (data.get(3) >= 0xb0 && data.get(3) <= 0xb8))
                return 50;
            else if (data.get(3) > 0 && data.get(3) < 0xB0)
                return 20;
        Buffer buf = nextSegment(data.from(0));
        if (buf == null)
            return 0;
        if (buf.get(3) == 0 || (buf.get(3) >= 0xb0 && buf.get(3) <= 0xb8))
            return 40;
        else if (buf.get(3) > 0 && buf.get(3) < 0xB0)
            return 10;

        return 0;
    }

    public static Size getSize(Buffer data) {
        SequenceHeader sh = getSequenceHeader(data);
        return new Size(sh.horizontal_size, sh.vertical_size);
    }

    private static SequenceHeader getSequenceHeader(Buffer data) {
        Buffer segment = nextSegment(data);
        while (segment != null) {
            if (segment.get(3) == MPEGConst.SEQUENCE_HEADER_CODE) {
                return SequenceHeader.read(segment.from(4));
            }
            segment = nextSegment(data);
        }
        return null;
    }

    public Size getSize() {
        return new Size(sh.horizontal_size, sh.vertical_size);
    }
}