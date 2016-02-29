package org.jcodec.codecs.mpeg12;

import static org.jcodec.codecs.mpeg12.MPEGConst.BLOCK_TO_CC;
import static org.jcodec.codecs.mpeg12.MPEGConst.EXTENSION_START_CODE;
import static org.jcodec.codecs.mpeg12.MPEGConst.GROUP_START_CODE;
import static org.jcodec.codecs.mpeg12.MPEGConst.PICTURE_START_CODE;
import static org.jcodec.codecs.mpeg12.MPEGConst.SEQUENCE_HEADER_CODE;
import static org.jcodec.codecs.mpeg12.MPEGConst.SLICE_START_CODE_FIRST;
import static org.jcodec.codecs.mpeg12.MPEGConst.SLICE_START_CODE_LAST;
import static org.jcodec.codecs.mpeg12.MPEGConst.SQUEEZE_X;
import static org.jcodec.codecs.mpeg12.MPEGConst.SQUEEZE_Y;
import static org.jcodec.codecs.mpeg12.MPEGConst.USER_DATA_START_CODE;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcAddressIncrement;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcCBP;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcCoeff0;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcCoeff1;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcDCSizeChroma;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcDCSizeLuma;
import static org.jcodec.codecs.mpeg12.MPEGUtil.gotoNextMarker;
import static org.jcodec.codecs.mpeg12.MPEGUtil.nextSegment;
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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jcodec.codecs.mpeg12.MPEGConst.MBType;
import org.jcodec.codecs.mpeg12.bitstream.GOPHeader;
import org.jcodec.codecs.mpeg12.bitstream.PictureHeader;
import org.jcodec.codecs.mpeg12.bitstream.SequenceExtension;
import org.jcodec.codecs.mpeg12.bitstream.SequenceHeader;
import org.jcodec.codecs.mpeg12.bitstream.SequenceScalableExtension;
import org.jcodec.common.Assert;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.dct.SparseIDCT;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.VLC;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Rect;
import org.jcodec.common.model.Size;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * MPEG 1/2 Decoder
 * 
 * Supports I/P/B frames, frame/field/interlaced frame encoding
 * 
 * Conforms to H.262 ( ISO/IEC 13818-2, ISO/IEC 11172-2 ) specifications
 * 
 * @author The JCodec project
 * 
 */
public class MPEGDecoder extends VideoDecoder {

    protected SequenceHeader sh;
    protected GOPHeader gh;
    private Picture8Bit[] refFrames;
    private Picture8Bit[] refFields;

    public MPEGDecoder() {
        this.refFrames = new Picture8Bit[2];
        this.refFields = new Picture8Bit[2];
    }

    public static class Context {
        int[] intra_dc_predictor;
        public int mbWidth;
        int mbNo;
        public int codedWidth;
        public int codedHeight;
        public int mbHeight;
        public ColorSpace color;
        public MBType lastPredB;
        public int[][] qMats;
        public int[] scan;
        public int picWidth;
        public int picHeight;
        
        public Context() {
            this.intra_dc_predictor = new int[3];
        }
    }

    @Override
    public Picture8Bit decodeFrame8Bit(ByteBuffer ByteBuffer, byte[][] buf) {

        PictureHeader ph = readHeader(ByteBuffer);
        if (refFrames[0] == null && ph.picture_coding_type > 1 || refFrames[1] == null && ph.picture_coding_type > 2) {
            throw new RuntimeException("Not enough references to decode " + (ph.picture_coding_type == 1 ? "P" : "B")
                    + " frame");
        }
        Context context = initContext(sh, ph);
        Picture8Bit pic = new Picture8Bit(context.codedWidth, context.codedHeight, buf, context.color, new Rect(0, 0,
                context.picWidth, context.picHeight));
        if (ph.pictureCodingExtension != null && ph.pictureCodingExtension.picture_structure != Frame) {
            decodePicture(context, ph, ByteBuffer, buf, ph.pictureCodingExtension.picture_structure - 1, 1);
            ph = readHeader(ByteBuffer);
            context = initContext(sh, ph);
            decodePicture(context, ph, ByteBuffer, buf, ph.pictureCodingExtension.picture_structure - 1, 1);
        } else {
            decodePicture(context, ph, ByteBuffer, buf, 0, 0);
        }

        if (ph.picture_coding_type == MPEGConst.IntraCoded
                || ph.picture_coding_type == MPEGConst.PredictiveCoded) {
            Picture8Bit unused = refFrames[1];
            refFrames[1] = refFrames[0];
            refFrames[0] = copyAndCreateIfNeeded(pic, unused);
        }

        return pic;
    }

    private Picture8Bit copyAndCreateIfNeeded(Picture8Bit src, Picture8Bit dst) {
        if (dst == null || !dst.compatible(src)) {
            dst = src.createCompatible();
        }
        dst.copyFrom(src);
        return dst;
    }

    private PictureHeader readHeader(ByteBuffer buffer) {
        PictureHeader ph = null;
        ByteBuffer segment;
        ByteBuffer fork = buffer.duplicate();

        while ((segment = nextSegment(fork)) != null) {
            int code = segment.getInt() & 0xff;
            if (code == SEQUENCE_HEADER_CODE) {
                SequenceHeader newSh = SequenceHeader.read(segment);
                if (sh != null) {
                    newSh.copyExtensions(sh);
                }
                sh = newSh;
            } else if (code == GROUP_START_CODE) {
                gh = GOPHeader.read(segment);
            } else if (code == PICTURE_START_CODE) {
                ph = PictureHeader.read(segment);
            } else if (code == EXTENSION_START_CODE) {
                int extType = segment.get(4) >> 4;
                if (extType == Sequence_Extension || extType == Sequence_Scalable_Extension
                        || extType == Sequence_Display_Extension)
                    SequenceHeader.readExtension(segment, sh);
                else
                    PictureHeader.readExtension(segment, ph, sh);
            } else if (code == USER_DATA_START_CODE) {
                // do nothing
            } else {
                break;
            }
            buffer.position(fork.position());
        }
        return ph;
    }

    protected Context initContext(SequenceHeader sh, PictureHeader ph) {
        Context context = new Context();
        context.codedWidth = (sh.horizontal_size + 15) & ~0xf;
        context.codedHeight = getCodedHeight(sh, ph);
        context.mbWidth = (sh.horizontal_size + 15) >> 4;
        context.mbHeight = (sh.vertical_size + 15) >> 4;
        context.picWidth = sh.horizontal_size;
        context.picHeight = sh.vertical_size;

        int chromaFormat = Chroma420;
        if (sh.sequenceExtension != null)
            chromaFormat = sh.sequenceExtension.chroma_format;

        context.color = getColor(chromaFormat);

        context.scan = MPEGConst.scan[ph.pictureCodingExtension == null ? 0 : ph.pictureCodingExtension.alternate_scan];

        int[] inter = sh.non_intra_quantiser_matrix == null ? zigzag(MPEGConst.defaultQMatInter, context.scan)
                : sh.non_intra_quantiser_matrix;
        int[] intra = sh.intra_quantiser_matrix == null ? zigzag(MPEGConst.defaultQMatIntra, context.scan)
                : sh.intra_quantiser_matrix;
        context.qMats = new int[][] { inter, inter, intra, intra };

        if (ph.quantMatrixExtension != null) {
            if (ph.quantMatrixExtension.non_intra_quantiser_matrix != null)
                context.qMats[0] = ph.quantMatrixExtension.non_intra_quantiser_matrix;
            if (ph.quantMatrixExtension.chroma_non_intra_quantiser_matrix != null)
                context.qMats[1] = ph.quantMatrixExtension.chroma_non_intra_quantiser_matrix;
            if (ph.quantMatrixExtension.intra_quantiser_matrix != null)
                context.qMats[2] = ph.quantMatrixExtension.intra_quantiser_matrix;
            if (ph.quantMatrixExtension.chroma_intra_quantiser_matrix != null)
                context.qMats[3] = ph.quantMatrixExtension.chroma_intra_quantiser_matrix;
        }
        return context;
    }

    private int[] zigzag(int[] array, int[] scan) {
        int[] result = new int[64];
        for (int i = 0; i < scan.length; i++)
            result[i] = array[scan[i]];
        return result;
    }

    public static int getCodedHeight(SequenceHeader sh, PictureHeader ph) {
        int field = ph.pictureCodingExtension != null && ph.pictureCodingExtension.picture_structure != Frame ? 1 : 0;

        return (((sh.vertical_size >> field) + 15) & ~0xf) << field;
    }

    public Picture8Bit decodePicture(Context context, PictureHeader ph, ByteBuffer buffer, byte[][] buf, int vertOff,
            int vertStep) {

        int planeSize = context.codedWidth * context.codedHeight;
        if (buf.length < 3 || buf[0].length < planeSize || buf[1].length < planeSize || buf[2].length < planeSize) {
            throw new RuntimeException("ByteBuffer too small to hold output picture [" + context.codedWidth + "x"
                    + context.codedHeight + "]");
        }

        try {
            ByteBuffer segment;
            while ((segment = nextSegment(buffer)) != null) {
                int startCode = segment.get(3) & 0xff;
                if (startCode >= SLICE_START_CODE_FIRST && startCode <= SLICE_START_CODE_LAST) {
                    doDecodeSlice(context, ph, buf, vertOff, vertStep, segment);
                } else if (startCode >= 0xB3 && startCode != 0xB6 && startCode != 0xB7) {
                    throw new RuntimeException("Unexpected start code " + startCode);
                } else if (startCode == 0x0) {
                    buffer.reset();
                    break;
                }
            }

            Picture8Bit pic = Picture8Bit.createPicture8Bit(context.codedWidth, context.codedHeight, buf, context.color);
            if ((ph.picture_coding_type == MPEGConst.IntraCoded || ph.picture_coding_type == MPEGConst.PredictiveCoded)
                    && ph.pictureCodingExtension != null && ph.pictureCodingExtension.picture_structure != Frame) {
                refFields[ph.pictureCodingExtension.picture_structure - 1] = copyAndCreateIfNeeded(pic,
                        refFields[ph.pictureCodingExtension.picture_structure - 1]);
            }

            return pic;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void doDecodeSlice(Context context, PictureHeader ph, byte[][] buf, int vertOff, int vertStep,
            ByteBuffer segment) throws IOException {
        int startCode = segment.get(3) & 0xff;
        ByteBuffer dup = segment.duplicate();
        dup.position(4);
        try {
            decodeSlice(ph, startCode, context, buf, BitReader.createBitReader(dup), vertOff, vertStep);
        } catch (RuntimeException e) {
            e.printStackTrace();
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

    public void decodeSlice(PictureHeader ph, int verticalPos, Context context, byte[][] buf, BitReader _in, int vertOff,
            int vertStep) throws IOException {

        int stride = context.codedWidth;

        resetDCPredictors(context, ph);

        int mbRow = verticalPos - 1;
        if (sh.vertical_size > 2800) {
            mbRow += (_in.readNBit(3) << 7);
        }
        if (sh.sequenceScalableExtension != null
                && sh.sequenceScalableExtension.scalable_mode == SequenceScalableExtension.DATA_PARTITIONING) {
            int priorityBreakpoint = _in.readNBit(7);
        }
        int qScaleCode = _in.readNBit(5);
        if (_in.read1Bit() == 1) {
            int intraSlice = _in.read1Bit();
            _in.skip(7);
            while (_in.read1Bit() == 1)
                _in.readNBit(8);
        }

        MPEGPred pred = new MPEGPred(ph.pictureCodingExtension != null ? ph.pictureCodingExtension.f_code
                : new int[][] { new int[] { ph.forward_f_code, ph.forward_f_code },
                        new int[] { ph.backward_f_code, ph.backward_f_code } },
                sh.sequenceExtension != null ? sh.sequenceExtension.chroma_format : Chroma420,
                ph.pictureCodingExtension != null && ph.pictureCodingExtension.top_field_first == 0 ? false : true);

        int[] ctx = new int[] { qScaleCode };

        for (int prevAddr = mbRow * context.mbWidth - 1; _in.checkNBit(23) != 0;) {
            // TODO: decode skipped!!!
            prevAddr = decodeMacroblock(ph, context, prevAddr, ctx, buf, stride, _in, vertOff, vertStep, pred);
            context.mbNo++;
        }
    }

    private void resetDCPredictors(Context context, PictureHeader ph) {
        int rval = 1 << 7;
        if (ph.pictureCodingExtension != null)
            rval = 1 << (7 + ph.pictureCodingExtension.intra_dc_precision);
        context.intra_dc_predictor[0] = context.intra_dc_predictor[1] = context.intra_dc_predictor[2] = rval;
    }

    public int decodeMacroblock(PictureHeader ph, Context context, int prevAddr, int[] qScaleCode, byte[][] buf,
            int stride, BitReader bits, int vertOff, int vertStep, MPEGPred pred) {
        int mbAddr = prevAddr;
        while (bits.checkNBit(11) == 0x8) {
            bits.skip(11);
            mbAddr += 33;
        }
        mbAddr += vlcAddressIncrement.readVLC(bits) + 1;

        int chromaFormat = Chroma420;
        if (sh.sequenceExtension != null)
            chromaFormat = sh.sequenceExtension.chroma_format;

        for (int i = prevAddr + 1; i < mbAddr; i++) {
            int[][] predFwd = new int[][] { new int[256], new int[1 << (chromaFormat + 5)],
                    new int[1 << (chromaFormat + 5)] };
            int mbX = i % context.mbWidth;
            int mbY = i / context.mbWidth;
            if (ph.picture_coding_type == MPEGConst.PredictiveCoded)
                pred.reset();
            mvZero(context, ph, pred, mbX, mbY, predFwd);
            put(predFwd, buf, stride, chromaFormat, mbX, mbY, context.codedWidth, context.codedHeight >> vertStep,
                    vertOff, vertStep);
        }

        VLC vlcMBType = SequenceScalableExtension.vlcMBType(ph.picture_coding_type, sh.sequenceScalableExtension);
        MBType[] mbTypeVal = SequenceScalableExtension.mbTypeVal(ph.picture_coding_type, sh.sequenceScalableExtension);

        MBType mbType = mbTypeVal[vlcMBType.readVLC(bits)];

        if (mbType.macroblock_intra != 1 || (mbAddr - prevAddr) > 1) {
            resetDCPredictors(context, ph);
        }

        int spatial_temporal_weight_code = 0;
        if (mbType.spatial_temporal_weight_code_flag == 1 && ph.pictureSpatialScalableExtension != null
                && ph.pictureSpatialScalableExtension.spatial_temporal_weight_code_table_index != 0) {
            spatial_temporal_weight_code = bits.readNBit(2);
        }

        int motion_type = -1;
        if (mbType.macroblock_motion_forward != 0 || mbType.macroblock_motion_backward != 0) {
            if (ph.pictureCodingExtension == null || ph.pictureCodingExtension.picture_structure == Frame
                    && ph.pictureCodingExtension.frame_pred_frame_dct == 1)
                motion_type = 2;
            else
                motion_type = bits.readNBit(2);
        }

        int dctType = 0;
        if (ph.pictureCodingExtension != null && ph.pictureCodingExtension.picture_structure == Frame
                && ph.pictureCodingExtension.frame_pred_frame_dct == 0
                && (mbType.macroblock_intra != 0 || mbType.macroblock_pattern != 0)) {
            dctType = bits.read1Bit();
        }
        // buf[3][mbAddr] = dctType;

        if (mbType.macroblock_quant != 0) {
            qScaleCode[0] = bits.readNBit(5);
        }
        boolean concealmentMv = ph.pictureCodingExtension != null
                && ph.pictureCodingExtension.concealment_motion_vectors != 0;

        int[][] predFwd = null;
        int mbX = mbAddr % context.mbWidth;
        int mbY = mbAddr / context.mbWidth;
        if (mbType.macroblock_intra == 1) {
            if (concealmentMv) {
                // TODO read consealment vectors
            } else
                pred.reset();
        } else if (mbType.macroblock_motion_forward != 0) {
            int refIdx = ph.picture_coding_type == MPEGConst.PredictiveCoded ? 0 : 1;
            predFwd = new int[][] { new int[256], new int[1 << (chromaFormat + 5)], new int[1 << (chromaFormat + 5)] };
            if (ph.pictureCodingExtension == null || ph.pictureCodingExtension.picture_structure == Frame) {
                pred.predictInFrame(refFrames[refIdx], mbX << 4, mbY << 4, predFwd, bits, motion_type, 0,
                        spatial_temporal_weight_code);
            } else {
                if (ph.picture_coding_type == MPEGConst.PredictiveCoded) {
                    pred.predictInField(refFields, mbX << 4, mbY << 4, predFwd, bits, motion_type, 0,
                            ph.pictureCodingExtension.picture_structure - 1);
                } else {
                    pred.predictInField(new Picture8Bit[] { refFrames[refIdx], refFrames[refIdx] }, mbX << 4, mbY << 4,
                            predFwd, bits, motion_type, 0, ph.pictureCodingExtension.picture_structure - 1);
                }
            }
        } else if (ph.picture_coding_type == MPEGConst.PredictiveCoded) {
            predFwd = new int[][] { new int[256], new int[1 << (chromaFormat + 5)], new int[1 << (chromaFormat + 5)] };
            pred.reset();
            mvZero(context, ph, pred, mbX, mbY, predFwd);
        }

        int[][] predBack = null;
        if (mbType.macroblock_motion_backward != 0) {
            predBack = new int[][] { new int[256], new int[1 << (chromaFormat + 5)], new int[1 << (chromaFormat + 5)] };
            if (ph.pictureCodingExtension == null || ph.pictureCodingExtension.picture_structure == Frame) {
                pred.predictInFrame(refFrames[0], mbX << 4, mbY << 4, predBack, bits, motion_type, 1,
                        spatial_temporal_weight_code);
            } else {
                pred.predictInField(new Picture8Bit[] { refFrames[0], refFrames[0] }, mbX << 4, mbY << 4, predBack, bits,
                        motion_type, 1, ph.pictureCodingExtension.picture_structure - 1);
            }
        }
        context.lastPredB = mbType;
        int[][] pp = mbType.macroblock_intra == 1 ? new int[][] { new int[256], new int[1 << (chromaFormat + 5)],
                new int[1 << (chromaFormat + 5)] } : buildPred(predFwd, predBack);

        if (mbType.macroblock_intra != 0 && concealmentMv)
            Assert.assertEquals(1, bits.read1Bit()); // Marker

        int cbp = mbType.macroblock_intra == 1 ? 0xfff : 0;
        if (mbType.macroblock_pattern != 0) {
            cbp = readCbPattern(bits);
        }

        VLC vlcCoeff = vlcCoeff0;
        if (ph.pictureCodingExtension != null && mbType.macroblock_intra == 1
                && ph.pictureCodingExtension.intra_vlc_format == 1)
            vlcCoeff = vlcCoeff1;

        int[] qScaleTab = ph.pictureCodingExtension != null && ph.pictureCodingExtension.q_scale_type == 1 ? MPEGConst.qScaleTab2
                : MPEGConst.qScaleTab1;
        int qScale = qScaleTab[qScaleCode[0]];

        int intra_dc_mult = 8;
        if (ph.pictureCodingExtension != null)
            intra_dc_mult = 8 >> ph.pictureCodingExtension.intra_dc_precision;

        int blkCount = 6 + (chromaFormat == Chroma420 ? 0 : (chromaFormat == Chroma422 ? 2 : 6));
        int[] block = new int[64];
//        System.out.print(mbAddr + ": ");
        for (int i = 0, cbpMask = 1 << (blkCount - 1); i < blkCount; i++, cbpMask >>= 1) {
            if ((cbp & cbpMask) == 0)
                continue;
            int[] qmat = context.qMats[(i >= 4 ? 1 : 0) + (mbType.macroblock_intra << 1)];

            if (mbType.macroblock_intra == 1)
                blockIntra(bits, vlcCoeff, block, context.intra_dc_predictor, i, context.scan,
                        sh.hasExtensions() || ph.hasExtensions() ? 12 : 8, intra_dc_mult, qScale, qmat);
            else
                blockInter(bits, vlcCoeff, block, context.scan,
                        sh.hasExtensions() || ph.hasExtensions() ? 12 : 8, qScale, qmat);

            mapBlock(block, pp[BLOCK_TO_CC[i]], i, dctType, chromaFormat);
        }

        put(pp, buf, stride, chromaFormat, mbX, mbY, context.codedWidth, context.codedHeight >> vertStep, vertOff,
                vertStep);

        return mbAddr;
    }

    protected void mapBlock(int[] block, int[] out, int blkIdx, int dctType, int chromaFormat) {
        int stepVert = chromaFormat == Chroma420 && (blkIdx == 4 || blkIdx == 5) ? 0 : dctType;
        int log2stride = blkIdx < 4 ? 4 : 4 - SQUEEZE_X[chromaFormat];

        int blkIdxExt = blkIdx + (dctType << 4);
        int x = MPEGConst.BLOCK_POS_X[blkIdxExt];
        int y = MPEGConst.BLOCK_POS_Y[blkIdxExt];
        int off = (y << log2stride) + x, stride = 1 << (log2stride + stepVert);
        for (int i = 0, coeff = 0; i < 8; i++, coeff += 8) {
            out[off] += block[coeff];
            out[off + 1] += block[coeff + 1];
            out[off + 2] += block[coeff + 2];
            out[off + 3] += block[coeff + 3];
            out[off + 4] += block[coeff + 4];
            out[off + 5] += block[coeff + 5];
            out[off + 6] += block[coeff + 6];
            out[off + 7] += block[coeff + 7];

            off += stride;
        }
    }

    private static final int[][] buildPred(int[][] predFwd, int[][] predBack) {
        if (predFwd != null && predBack != null) {
            avgPred(predFwd, predBack);
            return predFwd;
        } else if (predFwd != null)
            return predFwd;
        else if (predBack != null)
            return predBack;
        else
            throw new RuntimeException("Omited pred _in B-frames --> invalid");
    }

    private static final void avgPred(int[][] predFwd, int[][] predBack) {
        for (int i = 0; i < predFwd.length; i++) {
            for (int j = 0; j < predFwd[i].length; j += 4) {
                predFwd[i][j] = (predFwd[i][j] + predBack[i][j] + 1) >> 1;
                predFwd[i][j + 1] = (predFwd[i][j + 1] + predBack[i][j + 1] + 1) >> 1;
                predFwd[i][j + 2] = (predFwd[i][j + 2] + predBack[i][j + 2] + 1) >> 1;
                predFwd[i][j + 3] = (predFwd[i][j + 3] + predBack[i][j + 3] + 1) >> 1;
            }
        }
    }

    private void mvZero(Context context, PictureHeader ph, MPEGPred pred, int mbX, int mbY, int[][] mbPix) {
        if (ph.picture_coding_type == MPEGConst.PredictiveCoded) {
            pred.predict16x16NoMV(refFrames[0], mbX << 4, mbY << 4, ph.pictureCodingExtension == null ? Frame
                    : ph.pictureCodingExtension.picture_structure, 0, mbPix);
        } else {
            int[][] pp = mbPix;
            if (context.lastPredB.macroblock_motion_backward == 1) {
                pred.predict16x16NoMV(refFrames[0], mbX << 4, mbY << 4, ph.pictureCodingExtension == null ? Frame
                        : ph.pictureCodingExtension.picture_structure, 1, pp);
                pp = new int[][] { new int[mbPix[0].length], new int[mbPix[1].length], new int[mbPix[2].length] };
            }
            if (context.lastPredB.macroblock_motion_forward == 1) {
                pred.predict16x16NoMV(refFrames[1], mbX << 4, mbY << 4, ph.pictureCodingExtension == null ? Frame
                        : ph.pictureCodingExtension.picture_structure, 0, pp);
                if (mbPix != pp)
                    avgPred(mbPix, pp);
            }
        }
    }

    protected void put(int[][] mbPix, byte[][] buf, int stride, int chromaFormat, int mbX, int mbY, int width,
            int height, int vertOff, int vertStep) {

        int chromaStride = (stride + (1 << SQUEEZE_X[chromaFormat]) - 1) >> SQUEEZE_X[chromaFormat];
        int chromaMBW = 4 - SQUEEZE_X[chromaFormat];
        int chromaMBH = 4 - SQUEEZE_Y[chromaFormat];

        putSub(buf[0], (mbY << 4) * (stride << vertStep) + vertOff * stride + (mbX << 4), stride << vertStep, mbPix[0],
                4, 4);
        putSub(buf[1], (mbY << chromaMBH) * (chromaStride << vertStep) + vertOff * chromaStride + (mbX << chromaMBW),
                chromaStride << vertStep, mbPix[1], chromaMBW, chromaMBH);
        putSub(buf[2], (mbY << chromaMBH) * (chromaStride << vertStep) + vertOff * chromaStride + (mbX << chromaMBW),
                chromaStride << vertStep, mbPix[2], chromaMBW, chromaMBH);
    }

    protected void putSub(byte[] big, int off, int stride, int[] block, int mbW, int mbH) {
        int blOff = 0;

        if (mbW == 3) {
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
                big[off + 8] = clipTo8Bit(block[blOff + 8]);
                big[off + 9] = clipTo8Bit(block[blOff + 9]);
                big[off + 10] = clipTo8Bit(block[blOff + 10]);
                big[off + 11] = clipTo8Bit(block[blOff + 11]);
                big[off + 12] = clipTo8Bit(block[blOff + 12]);
                big[off + 13] = clipTo8Bit(block[blOff + 13]);
                big[off + 14] = clipTo8Bit(block[blOff + 14]);
                big[off + 15] = clipTo8Bit(block[blOff + 15]);

                blOff += 16;
                off += stride;
            }
        }
    }

    protected static final byte clipTo8Bit(int val) {
        return (byte)((val < 0 ? 0 : (val > 255 ? 255 : val)) - 128);
    }
    
    protected static final int clip(int val) {
        return val < 0 ? 0 : (val > 255 ? 255 : val);
    }

    protected static final int quantInter(int level, int quant) {
        return (((level << 1) + 1) * quant) >> 5;
    }

    protected static final int quantInterSigned(int level, int quant) {
        return level >= 0 ? quantInter(level, quant) : -quantInter(-level, quant);
    }

    protected void blockIntra(BitReader bits, VLC vlcCoeff, int[] block, int[] intra_dc_predictor, int blkIdx,
            int[] scan, int escSize, int intra_dc_mult, int qScale, int[] qmat) {
        int cc = BLOCK_TO_CC[blkIdx];
        int size = (cc == 0 ? vlcDCSizeLuma : vlcDCSizeChroma).readVLC(bits);
        int delta = (size != 0) ? mpegSigned(bits, size) : 0;
        intra_dc_predictor[cc] = intra_dc_predictor[cc] + delta;
        int dc = intra_dc_predictor[cc] * intra_dc_mult;
        SparseIDCT.start(block, dc);

        for (int idx = 0; idx < 64;) {
            int readVLC = vlcCoeff.readVLC(bits);
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
            SparseIDCT.coeff(block, scan[idx], level);
        }
        SparseIDCT.finish(block);
    }

    protected void blockInter(BitReader bits, VLC vlcCoeff, int[] block, int[] scan, int escSize, int qScale,
            int[] qmat) {
        
//        System.out.println();

        int idx = -1;
        if (vlcCoeff == vlcCoeff0 && bits.checkNBit(1) == 1) {
            bits.read1Bit();
            int dc = toSigned(quantInter(1, qScale * qmat[0]), bits.read1Bit());
            SparseIDCT.start(block, dc);
            idx++;
        } else {
            SparseIDCT.start(block, 0);
        }

        for (; idx < 64;) {
            int readVLC = vlcCoeff.readVLC(bits);
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
            SparseIDCT.coeff(block, scan[idx], ac);
//            System.out.print(ac + ",");
        }
        SparseIDCT.finish(block);
    }

    public static final int twosSigned(BitReader bits, int size) {
        int shift = 32 - size;
        return (bits.readNBit(size) << shift) >> shift;
    }

    public static final int mpegSigned(BitReader bits, int size) {
        int val = bits.readNBit(size);
        int sign = (val >>> (size - 1)) ^ 0x1;
        return val + sign - (sign << size);
    }

    public static final int toSigned(int val, int s) {
        int sign = (s << 31) >> 31;
        return (val ^ sign) - sign;
    }

    private final int readCbPattern(BitReader bits) {
        int cbp420 = vlcCBP.readVLC(bits);
        if (sh.sequenceExtension == null || sh.sequenceExtension.chroma_format == SequenceExtension.Chroma420)
            return cbp420;
        else if (sh.sequenceExtension.chroma_format == SequenceExtension.Chroma422)
            return (cbp420 << 2) | bits.readNBit(2);
        else if (sh.sequenceExtension.chroma_format == SequenceExtension.Chroma444)
            return (cbp420 << 6) | bits.readNBit(6);
        throw new RuntimeException("Unsupported chroma format: " + sh.sequenceExtension.chroma_format);
    }

    @Override
    public int probe(ByteBuffer data) {
        data = data.duplicate();
        data.order(ByteOrder.BIG_ENDIAN);

        for (int i = 0; i < 2; i++) {
            if (gotoNextMarker(data) == null)
                break;
            if (!data.hasRemaining())
                break;
            int marker = data.getInt();

            if (marker == 0x100 || (marker >= 0x1b0 && marker <= 0x1b8))
                return 50 - i * 10;
            else if (marker > 0x100 && marker < 0x1b0)
                return 20 - i * 10;
        }
        return 0;
    }

    public static Size getSize(ByteBuffer data) {
        SequenceHeader sh = getSequenceHeader(data.duplicate());
        return new Size(sh.horizontal_size, sh.vertical_size);
    }

    private static SequenceHeader getSequenceHeader(ByteBuffer data) {
        ByteBuffer segment = nextSegment(data);
        while (segment != null) {
            int marker = segment.getInt();
            if (marker == (0x100 | SEQUENCE_HEADER_CODE)) {
                return SequenceHeader.read(segment);
            }
            segment = nextSegment(data);
        }
        return null;
    }

    @Override
    public VideoDecoder downscaled(int ratio) {
        if(ratio == 1)
            return this;
        return null;
    }
}