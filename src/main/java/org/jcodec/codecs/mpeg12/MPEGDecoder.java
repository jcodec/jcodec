package org.jcodec.codecs.mpeg12;

import static junit.framework.Assert.assertEquals;
import static org.jcodec.codecs.mpeg12.MPEGConst.EXTENSION_START_CODE;
import static org.jcodec.codecs.mpeg12.MPEGConst.GROUP_START_CODE;
import static org.jcodec.codecs.mpeg12.MPEGConst.PICTURE_START_CODE;
import static org.jcodec.codecs.mpeg12.MPEGConst.SLICE_START_CODE_FIRST;
import static org.jcodec.codecs.mpeg12.MPEGConst.SLICE_START_CODE_LAST;
import static org.jcodec.codecs.mpeg12.MPEGConst.mbTypeVal;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcAddressIncrement;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcCBP;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcCoeff0;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcCoeff1;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcDCSizeChroma;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcDCSizeLuma;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcDualPrime;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcMBType;
import static org.jcodec.codecs.mpeg12.MPEGConst.vlcMotionCode;
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

    protected SequenceHeader sh;
    protected GOPHeader gh;
    private int maxCoeff;

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

        Context context = new Context();

        boolean contextInit = false;
        try {
            PictureHeader ph = null;
            Buffer segment = nextSegment(buffer);
            while (segment != null) {
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
                } else if (segment.get(3) >= SLICE_START_CODE_FIRST && segment.get(3) <= SLICE_START_CODE_LAST) {
                    if (sh == null || ph == null) {
                        System.out.println("Skipping slice, no header information.");
                    } else if (!contextInit) {
                        initContext(context, buf);
                        contextInit = true;
                    }

                    decodeSlice(ph, segment.get(3) & 0xff, context, buf, new BitstreamReaderBB(segment.from(4)));
                } else if (segment.get(3) >= 0xB3 && segment.get(3) != 0xB6) {
                    throw new RuntimeException("Unexpected start code " + segment.get(3));
                }
                segment = nextSegment(buffer);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (!contextInit)
            throw new RuntimeException("Frame not decoded!");

        return new Picture(context.codedWidth, context.codedHeight, buf, context.color);
    }

    private void initContext(Context context, int[][] buf) {
        context.codedWidth = (sh.horizontal_size + 15) & ~0xf;
        context.codedHeight = (sh.vertical_size + 15) & ~0xf;
        context.mbWidth = (sh.horizontal_size + 15) >> 4;
        context.mbHeight = (sh.vertical_size + 15) >> 4;

        int chromaFormat = Chroma420;
        if (sh.sequenceExtension != null)
            chromaFormat = sh.sequenceExtension.chroma_format;

        context.color = getColor(chromaFormat);
        int planeSize = context.codedWidth * context.codedHeight;
        if (buf.length < 3 || buf[0].length < planeSize || buf[1].length < planeSize || buf[2].length < planeSize) {
            throw new RuntimeException("Output picture");
        }

        context.clipVal = (1 << (10 - chromaFormat)) - 1;
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

    public void decodeSlice(PictureHeader ph, int verticalPos, Context context, int[][] buf, InBits in)
            throws IOException {

        int stride = context.codedWidth;

        resetDCPredictors(context, ph);

        int[] scan = ph.pictureCodingExtension == null || ph.pictureCodingExtension.alternate_scan == 0 ? MPEGConst.zigzagFrame
                : MPEGConst.zigzagAlternate;

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

        for (int prevAddr = mbRow * context.mbWidth - 1; in.checkNBit(23) != 0;) {
            // System.out.println(context.mbNo);
            prevAddr = decodeMacroblock(ph, context, prevAddr, qScaleCode, scan, buf, stride, in);
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
            int[][] buf, int stride, InBits in) throws IOException {
        int mbAddr = prevAddr;
        while (in.checkNBit(11) == 0x8) {
            in.skip(11);
            mbAddr += 33;
        }
        mbAddr += vlcAddressIncrement.readVLC(in) + 1;

        VLC vlcMBType = vlcMBType(ph.picture_coding_type, sh.sequenceScalableExtension);
        MBType[] mbTypeVal = mbTypeVal(ph.picture_coding_type, sh.sequenceScalableExtension);

        MBType mbType = mbTypeVal[vlcMBType.readVLC(in)];

        if (mbType.macroblock_intra != 1 || (mbAddr - prevAddr) > 1) {
            resetDCPredictors(context, ph);
        }

        if (mbType.spatial_temporal_weight_code_flag == 1 && ph.pictureSpatialScalableExtension != null
                && ph.pictureSpatialScalableExtension.spatial_temporal_weight_code_table_index != 0) {
            int spatial_temporal_weight_code = in.readNBit(2);
        }

        int mvCount = 0;
        boolean mvField = false, dualPrime = false;
        if (mbType.macroblock_motion_forward != 0 || mbType.macroblock_motion_backward != 0) {
            if (ph.pictureCodingExtension.picture_structure == Frame) {
                if (ph.pictureCodingExtension.frame_pred_frame_dct == 0) {
                    int frame_motion_type = in.readNBit(2);
                    mvCount = frame_motion_type == 1 ? 2 : 1;
                    mvField = frame_motion_type != 2;
                    dualPrime = frame_motion_type == 3;
                }
            } else {
                int field_motion_type = in.readNBit(2);
                mvCount = field_motion_type == 2 ? 2 : 1;
                mvField = true;
                dualPrime = field_motion_type == 3;
            }
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

        if (mbType.macroblock_motion_forward != 0 || (mbType.macroblock_intra != 0 && concealmentMv)) {
            readMv(in, ph, mvCount, mvField, dualPrime, 0);
        }
        if (mbType.macroblock_motion_backward != 0) {
            readMv(in, ph, mvCount, mvField, dualPrime, 2);
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

        int chromaFormat = Chroma420;
        if (sh.sequenceExtension != null)
            chromaFormat = sh.sequenceExtension.chroma_format;

        int[] qScaleTab = ph.pictureCodingExtension != null && ph.pictureCodingExtension.q_scale_type == 1 ? MPEGConst.qScaleTab2
                : MPEGConst.qScaleTab1;
        int qScale = qScaleTab[qScaleCode];

        int intra_dc_mult = 8;
        if (ph.pictureCodingExtension != null)
            intra_dc_mult = 8 >> ph.pictureCodingExtension.intra_dc_precision;

        int[] block = new int[64];
        // int chromaStride = (stride >> SQUEEZE_X[chromaFormat]);
        int blkCount = 6 + (chromaFormat == Chroma420 ? 0 : (chromaFormat == Chroma422 ? 2 : 6));
        int mbX = mbAddr % context.mbWidth;
        int mbY = mbAddr / context.mbWidth;
        for (int i = 0; i < blkCount; i++) {
            if ((cbp >> (blkCount - i - 1)) == 0)
                continue;
            int[] qmat = getQmat(i < 4, mbType.macroblock_intra == 1, ph);

            if (mbType.macroblock_intra == 1)
                blockIntra(in, vlcCoeff, block, context.intra_dc_predictor, i, scan,
                        sh.hasExtensions() || ph.hasExtensions() ? 12 : 8, intra_dc_mult, qScale, qmat);
            else
                blockInter(in, vlcCoeff, block, context.intra_dc_predictor, i, scan,
                        sh.hasExtensions() || ph.hasExtensions() ? 12 : 8, qScale, qmat);
            put(block, buf, stride, chromaFormat, i, mbX, mbY, dctType, context.clipVal);
        }

        return mbAddr;
    }

    protected void put(int[] block, int[][] buf, int stride, int chromaFormat, int blkNo, int mbX, int mbY,
            int dctType, int clipVal) {
        int chromaStride = (stride >> SQUEEZE_X[chromaFormat]);

        int strd = stride, mbW = 4, mbH = 4;
        if (blkNo >= 4) {
            strd = chromaStride;
            mbW -= SQUEEZE_X[chromaFormat];
            mbH -= SQUEEZE_Y[chromaFormat];
        }

        putSub(buf[BLOCK_TO_CC[blkNo]], ((mbY << mbH) + BLOCK_POS_Y[blkNo + (dctType << 4)]) * strd + (mbX << mbW)
                + BLOCK_POS_X[blkNo + (dctType << 4)], strd, block, dctType, clipVal);
    }

    private final void putSub(int[] big, int off, int stride, int[] block, int dctType, int clipVal) {
        int blOff = 0;
        stride = (stride << dctType) - 8;
        for (int i = 0; i < 8; i++) {
            big[off++] = clip(block[blOff++], clipVal);
            big[off++] = clip(block[blOff++], clipVal);
            big[off++] = clip(block[blOff++], clipVal);
            big[off++] = clip(block[blOff++], clipVal);
            big[off++] = clip(block[blOff++], clipVal);
            big[off++] = clip(block[blOff++], clipVal);
            big[off++] = clip(block[blOff++], clipVal);
            big[off++] = clip(block[blOff++], clipVal);

            off += stride;
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

    public static int[] BLOCK_TO_CC = new int[] { 0, 0, 0, 0, 1, 2, 1, 2, 1, 2, 1, 2 };
    public static int[] BLOCK_POS_X = new int[] { 0, 8, 0, 8, 0, 0, 0, 0, 8, 8, 8, 8, 0, 0, 0, 0, 0, 8, 0, 8, 0, 0, 0,
            0, 8, 8, 8, 8 };
    public static int[] BLOCK_POS_Y = new int[] { 0, 0, 8, 8, 0, 0, 8, 8, 0, 0, 8, 8, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1,
            1, 0, 0, 1, 1 };
    public static int[] SQUEEZE_X = new int[] { 0, 1, 1, 0 };
    public static int[] SQUEEZE_Y = new int[] { 0, 1, 0, 0 };

    private static final int quantInter(int level, int quant) {
        return level == 0 ? 0 : (((level << 1) + ((level >> 31) << 1) + 1) * quant) >> 5;
    }

    private final void blockIntra(InBits in, VLC vlcCoeff, int[] block, int[] intra_dc_predictor, int blkIdx,
            int[] scan, int escSize, int intra_dc_mult, int qScale, int[] qmat) throws IOException {
        int cc = BLOCK_TO_CC[blkIdx];
        int size = (cc == 0 ? vlcDCSizeLuma : vlcDCSizeChroma).readVLC(in);
        int delta = (size != 0) ? mpegSigned(in, size) : 0;
        intra_dc_predictor[cc] = intra_dc_predictor[cc] + delta;
        int dc = intra_dc_predictor[cc] * intra_dc_mult;
        SparseIDCT.dc(block, dc);

        for (int idx = 0; idx < maxCoeff;) {
            int readVLC = vlcCoeff.readVLC(in);
            if (readVLC >= 0) {
                idx += (readVLC >> 12) + 1;
                int ridx = scan[idx];
                SparseIDCT.ac(block, ridx, (toSigned(readVLC & 0xfff, in.read1Bit()) * qScale * qmat[ridx]) >> 4);
            } else if (readVLC == -2) {
                idx += in.readNBit(6) + 1;
                int ridx = scan[idx];
                SparseIDCT.ac(block, ridx, (twosSigned(in, escSize) * qScale * qmat[ridx]) >> 4);
            } else
                break;
        }
    }

    private final void blockInter(InBits in, VLC vlcCoeff, int[] block, int[] intra_dc_predictor, int blkIdx,
            int[] scan, int escSize, int qScale, int[] qmat) throws IOException {

        if (vlcCoeff == vlcCoeff0 && in.checkNBit(1) == 1) {
            SparseIDCT.dc(block, quantInter(1, qScale * qmat[0]));
            in.read1Bit();
        } else {
            int readVLC = vlcCoeff.readVLC(in);
            if (readVLC >= 0) {
                SparseIDCT.dc(block, quantInter(toSigned(readVLC & 0xfff, in.read1Bit()), qScale * qmat[0]));
            } else {
                in.readNBit(6);
                SparseIDCT.dc(block, quantInter(twosSigned(in, escSize), qScale * qmat[0]));
            }
        }

        for (int idx = 0; idx < maxCoeff;) {
            int readVLC = vlcCoeff.readVLC(in);
            if (readVLC >= 0) {
                idx += (readVLC >> 12) + 1;
                int rind = scan[idx];
                SparseIDCT.ac(block, rind, quantInter(toSigned(readVLC & 0xfff, in.read1Bit()), qScale * qmat[rind]));
            } else if (readVLC == -2) {
                idx += in.readNBit(6) + 1;
                int rind = scan[idx];
                SparseIDCT.ac(block, rind, quantInter(twosSigned(in, escSize), qScale * qmat[rind]));
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

    private final void readMv(InBits in, PictureHeader ph, int mvCount, boolean mvField, boolean dualPrime, int s)
            throws IOException {
        if (mvCount == 1) {
            if (mvField && !dualPrime) {
                int mvFieldSelect = in.read1Bit();
            }
            mv(in, ph, dualPrime, s);
        } else {
            int mv1FieldSelect = in.read1Bit();
            mv(in, ph, dualPrime, s);
            int mv2FieldSelect = in.read1Bit();
            mv(in, ph, dualPrime, s);
        }
    }

    private final void mv(InBits in, PictureHeader ph, boolean dualPrime, int s) throws IOException {
        int x = vlcMotionCode.readVLC(in) - 16;
        if (ph.fCode(s) != 1 && x != 0)
            in.readNBit(ph.fCode(s) - 1);
        if (dualPrime) {
            int dmvX = vlcDualPrime.readVLC(in) - 1;
        }

        int y = vlcMotionCode.readVLC(in) - 16;
        if (ph.fCode(s + 1) != 1 && y != 0)
            in.readNBit(ph.fCode(s + 1) - 1);
        if (dualPrime) {
            int dmvY = vlcDualPrime.readVLC(in) - 1;
        }
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
        int codedWidth = (sh.horizontal_size + 15) & ~0xf;
        int codedHeight = (sh.vertical_size + 15) & ~0xf;
        return new Size(codedWidth, codedHeight);
    }
}