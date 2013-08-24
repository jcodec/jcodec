package org.jcodec.codecs.h264.decode;

import static org.jcodec.codecs.h264.H264Const.ARRAY;
import static org.jcodec.codecs.h264.H264Const.BLK8x8_BLOCKS;
import static org.jcodec.codecs.h264.H264Const.BLK_4x4_MB_OFF_LUMA;
import static org.jcodec.codecs.h264.H264Const.BLK_8x8_IND;
import static org.jcodec.codecs.h264.H264Const.BLK_8x8_MB_OFF_CHROMA;
import static org.jcodec.codecs.h264.H264Const.BLK_8x8_MB_OFF_LUMA;
import static org.jcodec.codecs.h264.H264Const.BLK_INV_MAP;
import static org.jcodec.codecs.h264.H264Const.QP_SCALE_CR;
import static org.jcodec.codecs.h264.H264Const.bPartPredModes;
import static org.jcodec.codecs.h264.H264Const.bSubMbTypes;
import static org.jcodec.codecs.h264.H264Const.identityMapping16;
import static org.jcodec.codecs.h264.H264Const.identityMapping4;
import static org.jcodec.codecs.h264.H264Const.last_sig_coeff_map_8x8;
import static org.jcodec.codecs.h264.H264Const.sig_coeff_map_8x8;
import static org.jcodec.codecs.h264.H264Const.PartPred.Bi;
import static org.jcodec.codecs.h264.H264Const.PartPred.Direct;
import static org.jcodec.codecs.h264.H264Const.PartPred.L0;
import static org.jcodec.codecs.h264.H264Const.PartPred.L1;
import static org.jcodec.codecs.h264.decode.CAVLCReader.moreRBSPData;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readBool;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readNBit;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readSE;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readTE;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readUE;
import static org.jcodec.codecs.h264.decode.CoeffTransformer.reorderDC4x4;
import static org.jcodec.codecs.h264.io.model.MBType.B_8x8;
import static org.jcodec.codecs.h264.io.model.MBType.I_16x16;
import static org.jcodec.codecs.h264.io.model.MBType.P_16x16;
import static org.jcodec.codecs.h264.io.model.MBType.P_16x8;
import static org.jcodec.codecs.h264.io.model.MBType.P_8x16;
import static org.jcodec.codecs.h264.io.model.MBType.P_8x8;
import static org.jcodec.codecs.h264.io.model.SliceType.P;
import static org.jcodec.common.model.ColorSpace.MONO;
import static org.jcodec.common.tools.MathUtil.abs;
import static org.jcodec.common.tools.MathUtil.clip;
import static org.jcodec.common.tools.MathUtil.wrap;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;

import org.jcodec.codecs.common.biari.MDecoder;
import org.jcodec.codecs.h264.H264Const;
import org.jcodec.codecs.h264.H264Const.PartPred;
import org.jcodec.codecs.h264.decode.aso.MapManager;
import org.jcodec.codecs.h264.decode.aso.Mapper;
import org.jcodec.codecs.h264.io.CABAC;
import org.jcodec.codecs.h264.io.CABAC.BlockType;
import org.jcodec.codecs.h264.io.CAVLC;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.common.IntObjectMap;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A decoder for an individual slice
 * 
 * @author Jay Codec
 * 
 */
public class SliceDecoder {

    private static final int[] NULL_VECTOR = new int[] { 0, 0, -1 };
    private SliceHeader sh;
    private CAVLC[] cavlc;
    private CABAC cabac;
    private Mapper mapper;

    private int[] chromaQpOffset;
    private int qp;
    private int[][] leftRow;
    private int[][] topLine;
    private int[][] topLeft;

    private int[] i4x4PredTop;
    private int[] i4x4PredLeft;

    private MBType[] topMBType;
    private MBType leftMBType;
    private ColorSpace chromaFormat;
    private boolean transform8x8;

    private int[][][] mvTop;
    private int[][][] mvLeft;
    private int[][] mvTopLeft;
    private SeqParameterSet activeSps;
    private PictureParameterSet activePps;
    private int[][] nCoeff;
    private int[][][][] mvs;
    private MBType[] mbTypes;
    private int[][] mbQps;
    private Frame thisFrame;
    private Frame[] sRefs;
    private IntObjectMap<Frame> lRefs;
    private MDecoder mDecoder;
    private SliceHeader[] shs;

    private int leftCBPLuma;
    private int[] topCBPLuma;

    private int leftCBPChroma;
    private int[] topCBPChroma;
    private int[] numRef;

    private boolean tf8x8Left;
    private boolean[] tf8x8Top;

    private PartPred[] predModeLeft;
    private PartPred[] predModeTop;
    private boolean[] tr8x8Used;
    private Frame[][][] refsUsed;
    private Prediction prediction;
    private boolean debug;

    public SliceDecoder(SeqParameterSet activeSps, PictureParameterSet activePps, int[][] nCoeff, int[][][][] mvs,
            MBType[] mbTypes, int[][] mbQps, SliceHeader[] shs, boolean[] tr8x8Used, Frame[][][] refsUsed,
            Frame result, Frame[] sRefs, IntObjectMap<Frame> lRefs) {

        this.activeSps = activeSps;
        this.activePps = activePps;
        this.nCoeff = nCoeff;
        this.mvs = mvs;
        this.mbTypes = mbTypes;
        this.mbQps = mbQps;
        this.shs = shs;
        this.thisFrame = result;
        this.sRefs = sRefs;
        this.lRefs = lRefs;
        this.tr8x8Used = tr8x8Used;
        this.refsUsed = refsUsed;
    }

    public void decode(ByteBuffer segment, NALUnit nalUnit) {
        BitReader in = new BitReader(segment);
        SliceHeaderReader shr = new SliceHeaderReader();
        sh = shr.readPart1(in);
        sh.sps = activeSps;
        sh.pps = activePps;

        cavlc = new CAVLC[] { new CAVLC(sh.sps, sh.pps, 2, 2), new CAVLC(sh.sps, sh.pps, 1, 1),
                new CAVLC(sh.sps, sh.pps, 1, 1) };

        int mbWidth = sh.sps.pic_width_in_mbs_minus1 + 1;
        cabac = new CABAC(mbWidth);

        chromaQpOffset = new int[] { sh.pps.chroma_qp_index_offset,
                sh.pps.extended != null ? sh.pps.extended.second_chroma_qp_index_offset : sh.pps.chroma_qp_index_offset };

        chromaFormat = sh.sps.chroma_format_idc;
        transform8x8 = sh.pps.extended == null ? false : sh.pps.extended.transform_8x8_mode_flag;

        i4x4PredLeft = new int[4];
        i4x4PredTop = new int[mbWidth << 2];
        topMBType = new MBType[mbWidth];

        this.topCBPLuma = new int[mbWidth];
        this.topCBPChroma = new int[mbWidth];

        mvTop = new int[2][(mbWidth << 2) + 1][3];
        mvLeft = new int[2][4][3];
        mvTopLeft = new int[2][3];

        leftRow = new int[3][16];
        topLeft = new int[3][4];
        topLine = new int[3][mbWidth << 4];

        this.predModeLeft = new PartPred[2];
        this.predModeTop = new PartPred[mbWidth << 1];

        this.tf8x8Top = new boolean[mbWidth];

        shr.readPart2(sh, nalUnit, sh.sps, sh.pps, in);
        prediction = new Prediction(sh);
        qp = sh.pps.pic_init_qp_minus26 + 26 + sh.slice_qp_delta;
        if (activePps.entropy_coding_mode_flag) {
            in.terminate();
            int[][] cm = new int[2][1024];
            cabac.initModels(cm, sh.slice_type, sh.cabac_init_idc, qp);
            mDecoder = new MDecoder(segment, cm);
        }

        if (sh.num_ref_idx_active_override_flag)
            numRef = new int[] { sh.num_ref_idx_active_minus1[0] + 1, sh.num_ref_idx_active_minus1[1] + 1 };
        else
            numRef = new int[] { activePps.num_ref_idx_active_minus1[0] + 1, activePps.num_ref_idx_active_minus1[1] + 1 };

        debugPrint("============" + thisFrame.getPOC() + "============= " + sh.slice_type.name());

        Frame[][] refList = null;
        if (sh.slice_type == SliceType.P) {
            refList = new Frame[][] { buildRefListP(), null };
        } else if (sh.slice_type == SliceType.B) {
            refList = buildRefListB();
        }

        debugPrint("------");
        if (refList != null) {
            for (int l = 0; l < 2; l++) {
                if (refList[l] != null)
                    for (int i = 0; i < refList[l].length; i++)
                        if (refList[l][i] != null)
                            debugPrint("REF[" + l + "][" + i + "]: " + ((Frame) refList[l][i]).getPOC());
            }
        }

        mapper = new MapManager(sh.sps, sh.pps).getMapper(sh);

        Picture mb = Picture.create(16, 16, sh.sps.chroma_format_idc);

        boolean mbaffFrameFlag = (sh.sps.mb_adaptive_frame_field_flag && !sh.field_pic_flag);

        boolean prevMbSkipped = false;
        int i;
        MBType prevMBType = null;
        for (i = 0;; i++) {
            if (sh.slice_type.isInter() && !activePps.entropy_coding_mode_flag) {
                int mbSkipRun = readUE(in, "mb_skip_run");
                for (int j = 0; j < mbSkipRun; j++, i++) {
                    int mbAddr = mapper.getAddress(i);
                    debugPrint("---------------------- MB (" + (mbAddr % mbWidth) + "," + (mbAddr / mbWidth)
                            + ") ---------------------");
                    decodeSkip(refList, i, mb, sh.slice_type);
                    shs[mbAddr] = sh;
                    refsUsed[mbAddr] = refList;
                    put(thisFrame, mb, mapper.getMbX(i), mapper.getMbY(i));
                    wipe(mb);
                }

                prevMbSkipped = mbSkipRun > 0;
                prevMBType = null;

                if (!moreRBSPData(in))
                    break;
            }

            int mbAddr = mapper.getAddress(i);
            shs[mbAddr] = sh;
            refsUsed[mbAddr] = refList;
            int mbX = mbAddr % mbWidth;
            int mbY = mbAddr / mbWidth;
            debugPrint("---------------------- MB (" + mbX + "," + mbY + ") ---------------------");

            if (sh.slice_type.isIntra()
                    || (!activePps.entropy_coding_mode_flag || !cabac.readMBSkipFlag(mDecoder, sh.slice_type,
                            mapper.leftAvailable(i), mapper.topAvailable(i), mbX))) {

                boolean mb_field_decoding_flag = false;
                if (mbaffFrameFlag && (i % 2 == 0 || (i % 2 == 1 && prevMbSkipped))) {
                    mb_field_decoding_flag = readBool(in, "mb_field_decoding_flag");
                }

                prevMBType = decode(sh.slice_type, i, in, mb_field_decoding_flag, prevMBType, mb, refList);

            } else {
                decodeSkip(refList, i, mb, sh.slice_type);
                prevMBType = null;
            }
            put(thisFrame, mb, mbX, mbY);

            if (activePps.entropy_coding_mode_flag && mDecoder.decodeFinalBin() == 1)
                break;
            else if (!activePps.entropy_coding_mode_flag && !moreRBSPData(in))
                break;

            wipe(mb);
        }
    }

    private Frame[] buildRefListP() {
        int frame_num = sh.frame_num;
        int maxFrames = 1 << (sh.sps.log2_max_frame_num_minus4 + 4);
        // int nLongTerm = Math.min(lRefs.size(), numRef[0] - 1);
        Frame[] result = new Frame[numRef[0]];

        int refs = 0;
        for (int i = frame_num - 1; i >= frame_num - maxFrames && refs < numRef[0]; i--) {
            int fn = i < 0 ? i + maxFrames : i;
            if (sRefs[fn] != null) {
                result[refs] = sRefs[fn] == H264Const.NO_PIC ? null : sRefs[fn];
                ++refs;
            }
        }
        int[] keys = lRefs.keys();
        Arrays.sort(keys);
        for (int i = 0; i < keys.length && refs < numRef[0]; i++) {
            result[refs++] = lRefs.get(keys[i]);
        }

        reorder(result, 0);

        return result;
    }

    private Frame[][] buildRefListB() {

        Frame[] l0 = buildList(Frame.POCDesc, Frame.POCAsc);
        Frame[] l1 = buildList(Frame.POCAsc, Frame.POCDesc);

        if (Arrays.equals(l0, l1) && count(l1) > 1) {
            Frame frame = l1[1];
            l1[1] = l1[0];
            l1[0] = frame;
        }

        Frame[][] result = { Arrays.copyOf(l0, numRef[0]), Arrays.copyOf(l1, numRef[1]) };

        // System.out.println("----------" + thisFrame.getPOC() +
        // "------------");
        // printList("List 0: ", result[0]);
        // printList("List 1: ", result[1]);

        reorder(result[0], 0);
        reorder(result[1], 1);

        // printList("Reorder List 0: ", result[0]);
        // printList("Reorder List 1: ", result[1]);

        return result;
    }

    // private void printList(String label, Frame[] frames) {
    // System.out.print(label);
    // for (Frame frame : frames) {
    // System.out.print(frame.getPOC() + ", ");
    // }
    // System.out.println();
    // }

    private Frame[] buildList(Comparator<Frame> cmpFwd, Comparator<Frame> cmpInv) {
        Frame[] refs = new Frame[sRefs.length + lRefs.size()];
        Frame[] fwd = copySort(cmpFwd, thisFrame);
        Frame[] inv = copySort(cmpInv, thisFrame);
        int nFwd = count(fwd);
        int nInv = count(inv);

        int ref = 0;
        for (int i = 0; i < nFwd; i++, ref++)
            refs[ref] = fwd[i];
        for (int i = 0; i < nInv; i++, ref++)
            refs[ref] = inv[i];

        int[] keys = lRefs.keys();
        Arrays.sort(keys);
        for (int i = 0; i < keys.length; i++, ref++)
            refs[ref] = lRefs.get(keys[i]);

        return refs;
    }

    private int count(Frame[] arr) {
        for (int nn = 0; nn < arr.length; nn++)
            if (arr[nn] == null)
                return nn;
        return arr.length;
    }

    private Frame[] copySort(Comparator<Frame> fwd, Frame dummy) {
        Frame[] copyOf = Arrays.copyOf(sRefs, sRefs.length);
        for (int i = 0; i < copyOf.length; i++)
            if (fwd.compare(dummy, copyOf[i]) > 0)
                copyOf[i] = null;
        Arrays.sort(copyOf, fwd);
        return copyOf;
    }

    private void reorder(Picture[] result, int list) {
        if (sh.refPicReordering[list] == null)
            return;

        int predict = sh.frame_num;
        int maxFrames = 1 << (sh.sps.log2_max_frame_num_minus4 + 4);

        for (int ind = 0; ind < sh.refPicReordering[list][0].length; ind++) {
            switch (sh.refPicReordering[list][0][ind]) {
            case 0:
                predict = wrap(predict - sh.refPicReordering[list][1][ind] - 1, maxFrames);
                break;
            case 1:
                predict = wrap(predict + sh.refPicReordering[list][1][ind] + 1, maxFrames);
                break;
            case 2:
                throw new RuntimeException("long term");
            }
            for (int i = numRef[list] - 1; i > ind; i--)
                result[i] = result[i - 1];
            result[ind] = sRefs[predict];
            for (int i = ind + 1, j = i; i < numRef[list] && result[i] != null; i++) {
                if (result[i] != sRefs[predict])
                    result[j++] = result[i];
            }
        }
    }

    private void wipe(Picture mb) {
        Arrays.fill(mb.getPlaneData(0), 0);
        Arrays.fill(mb.getPlaneData(1), 0);
        Arrays.fill(mb.getPlaneData(2), 0);
    }

    private void collectPredictors(Picture outMB, int mbX) {
        topLeft[0][0] = topLine[0][(mbX << 4) + 15];
        topLeft[0][1] = outMB.getPlaneData(0)[63];
        topLeft[0][2] = outMB.getPlaneData(0)[127];
        topLeft[0][3] = outMB.getPlaneData(0)[191];
        System.arraycopy(outMB.getPlaneData(0), 240, topLine[0], mbX << 4, 16);
        copyCol(outMB.getPlaneData(0), 16, 15, 16, leftRow[0]);

        collectChromaPredictors(outMB, mbX);
    }

    private void collectChromaPredictors(Picture outMB, int mbX) {
        topLeft[1][0] = topLine[1][(mbX << 3) + 7];
        topLeft[2][0] = topLine[2][(mbX << 3) + 7];

        System.arraycopy(outMB.getPlaneData(1), 56, topLine[1], mbX << 3, 8);
        System.arraycopy(outMB.getPlaneData(2), 56, topLine[2], mbX << 3, 8);

        copyCol(outMB.getPlaneData(1), 8, 7, 8, leftRow[1]);
        copyCol(outMB.getPlaneData(2), 8, 7, 8, leftRow[2]);
    }

    private void copyCol(int[] planeData, int n, int off, int stride, int[] out) {
        for (int i = 0; i < n; i++, off += stride) {
            out[i] = planeData[off];
        }
    }

    public MBType decode(SliceType sliceType, int mbAddr, BitReader reader, boolean field, MBType prevMbType,
            Picture mb, Frame[][] references) {
        if (sliceType == SliceType.I) {
            return decodeMBlockI(mbAddr, reader, field, prevMbType, mb);
        } else if (sliceType == SliceType.P) {
            return decodeMBlockP(mbAddr, reader, field, prevMbType, mb, references);
        } else {
            return decodeMBlockB(mbAddr, reader, field, prevMbType, mb, references);
        }
    }

    private MBType decodeMBlockI(int mbIdx, BitReader reader, boolean field, MBType prevMbType, Picture mb) {

        int mbType;
        if (!activePps.entropy_coding_mode_flag)
            mbType = readUE(reader, "MB: mb_type");
        else
            mbType = cabac.readMBTypeI(mDecoder, leftMBType, topMBType[mapper.getMbX(mbIdx)],
                    mapper.leftAvailable(mbIdx), mapper.topAvailable(mbIdx));
        return decodeMBlockIInt(mbType, mbIdx, reader, field, prevMbType, mb);
    }

    private MBType decodeMBlockIInt(int mbType, int mbIdx, BitReader reader, boolean field, MBType prevMbType,
            Picture mb) {
        MBType mbt;
        if (mbType == 0) {
            // System.out.println("NxN");
            decodeMBlockIntraNxN(reader, mbIdx, prevMbType, mb);
            mbt = MBType.I_NxN;
        } else if (mbType >= 1 && mbType <= 24) {
            // System.out.println("16x16");
            mbType--;
            decodeMBlockIntra16x16(reader, mbType, mbIdx, prevMbType, mb);
            mbt = MBType.I_16x16;
        } else {
            System.out.println("IPCM!!!");
            decodeMBlockIPCM(reader, mbIdx, mb);
            mbt = MBType.I_PCM;
        }
        int xx = mapper.getMbX(mbIdx) << 2;

        copyVect(mvTopLeft[0], mvTop[0][xx + 3]);
        copyVect(mvTopLeft[1], mvTop[1][xx + 3]);

        saveVect(mvTop[0], xx, xx + 4, 0, 0, -1);
        saveVect(mvLeft[0], 0, 4, 0, 0, -1);
        saveVect(mvTop[1], xx, xx + 4, 0, 0, -1);
        saveVect(mvLeft[1], 0, 4, 0, 0, -1);
        return mbt;
    }

    private MBType decodeMBlockP(int mbIdx, BitReader reader, boolean field, MBType prevMbType, Picture mb,
            Frame[][] references) {
        int mbType;
        if (!activePps.entropy_coding_mode_flag)
            mbType = readUE(reader, "MB: mb_type");
        else
            mbType = cabac.readMBTypeP(mDecoder);

        switch (mbType) {
        case 0:
            decodeInter16x16(reader, mb, references, mbIdx, prevMbType, L0, P_16x16);
            return MBType.P_16x16;
        case 1:
            decodeInter16x8(reader, mb, references, mbIdx, prevMbType, L0, L0, P_16x8);
            return MBType.P_16x8;
        case 2:
            decodeInter8x16(reader, mb, references, mbIdx, prevMbType, L0, L0, P_8x16);
            return MBType.P_8x16;
        case 3:
            decodeMBInter8x8(reader, mbType, references, mb, P, mbIdx, field, prevMbType, false);
            return MBType.P_8x8;
        case 4:
            decodeMBInter8x8(reader, mbType, references, mb, P, mbIdx, field, prevMbType, true);
            return MBType.P_8x8ref0;
        default:
            return decodeMBlockIInt(mbType - 5, mbIdx, reader, field, prevMbType, mb);
        }
    }

    private MBType decodeMBlockB(int mbIdx, BitReader reader, boolean field, MBType prevMbType, Picture mb,
            Frame[][] references) {
        int mbType;
        if (!activePps.entropy_coding_mode_flag)
            mbType = readUE(reader, "MB: mb_type");
        else
            mbType = cabac.readMBTypeB(mDecoder, leftMBType, topMBType[mapper.getMbX(mbIdx)],
                    mapper.leftAvailable(mbIdx), mapper.topAvailable(mbIdx));
        if (mbType >= 23) {
            return decodeMBlockIInt(mbType - 23, mbIdx, reader, field, prevMbType, mb);
        } else {
            MBType curMBType = H264Const.bMbTypes[mbType];

            if (mbType == 0)
                decodeMBBiDirect(mbIdx, reader, field, prevMbType, mb, references);
            else if (mbType <= 3)
                decodeInter16x16(reader, mb, references, mbIdx, prevMbType, H264Const.bPredModes[mbType][0], curMBType);
            else if (mbType == 22)
                decodeMBInter8x8(reader, mbType, references, mb, SliceType.B, mbIdx, field, prevMbType, false);
            else if ((mbType & 1) == 0)
                decodeInter16x8(reader, mb, references, mbIdx, prevMbType, H264Const.bPredModes[mbType][0],
                        H264Const.bPredModes[mbType][1], curMBType);
            else
                decodeInter8x16(reader, mb, references, mbIdx, prevMbType, H264Const.bPredModes[mbType][0],
                        H264Const.bPredModes[mbType][1], curMBType);

            return curMBType;
        }
    }

    // TODO: optimize this crap
    public void put(Picture tgt, Picture decoded, int mbX, int mbY) {

        int[] luma = tgt.getPlaneData(0);
        int stride = tgt.getPlaneWidth(0);

        int[] cb = tgt.getPlaneData(1);
        int[] cr = tgt.getPlaneData(2);
        int strideChroma = tgt.getPlaneWidth(1);

        int dOff = 0;
        for (int i = 0; i < 16; i++) {
            System.arraycopy(decoded.getPlaneData(0), dOff, luma, (mbY * 16 + i) * stride + mbX * 16, 16);
            dOff += 16;
        }
        for (int i = 0; i < 8; i++) {
            System.arraycopy(decoded.getPlaneData(1), i * 8, cb, (mbY * 8 + i) * strideChroma + mbX * 8, 8);
        }
        for (int i = 0; i < 8; i++) {
            System.arraycopy(decoded.getPlaneData(2), i * 8, cr, (mbY * 8 + i) * strideChroma + mbX * 8, 8);
        }
    }

    public void decodeMBlockIntra16x16(BitReader reader, int mbType, int mbIndex, MBType prevMbType, Picture mb) {

        int mbX = mapper.getMbX(mbIndex);
        int mbY = mapper.getMbY(mbIndex);
        int address = mapper.getAddress(mbIndex);

        int cbpChroma = (mbType / 4) % 3;
        int cbpLuma = (mbType / 12) * 15;

        boolean leftAvailable = mapper.leftAvailable(mbIndex);
        boolean topAvailable = mapper.topAvailable(mbIndex);

        int chromaPredictionMode = readChromaPredMode(reader, mbX, leftAvailable, topAvailable);
        int mbQPDelta = readMBQpDelta(reader, prevMbType);
        qp = (qp + mbQPDelta + 52) % 52;
        mbQps[0][address] = qp;

        residualLumaI16x16(reader, leftAvailable, topAvailable, mbX, mbY, mb, cbpLuma);
        Intra16x16PredictionBuilder.predictWithMode(mbType % 4, mb.getPlaneData(0), leftAvailable, topAvailable,
                leftRow[0], topLine[0], topLeft[0], mbX << 4);

        decodeChroma(reader, cbpChroma, chromaPredictionMode, mbX, mbY, leftAvailable, topAvailable, mb, qp,
                MBType.I_16x16);
        mbTypes[address] = topMBType[mbX] = leftMBType = MBType.I_16x16;
        // System.out.println("idx: " + mbIndex + ", addr: " + address);
        topCBPLuma[mbX] = leftCBPLuma = cbpLuma;
        topCBPChroma[mbX] = leftCBPChroma = cbpChroma;
        tf8x8Left = tf8x8Top[mbX] = false;

        collectPredictors(mb, mbX);
        saveMvsIntra(mbX, mbY);
    }

    private int readMBQpDelta(BitReader reader, MBType prevMbType) {
        int mbQPDelta;
        if (!activePps.entropy_coding_mode_flag) {
            mbQPDelta = readSE(reader, "mb_qp_delta");
        } else {
            mbQPDelta = cabac.readMBQpDelta(mDecoder, prevMbType);
        }
        return mbQPDelta;
    }

    private int readChromaPredMode(BitReader reader, int mbX, boolean leftAvailable, boolean topAvailable) {
        int chromaPredictionMode;
        if (!activePps.entropy_coding_mode_flag) {
            chromaPredictionMode = readUE(reader, "MBP: intra_chroma_pred_mode");
        } else {
            chromaPredictionMode = cabac.readIntraChromaPredMode(mDecoder, mbX, leftMBType, topMBType[mbX],
                    leftAvailable, topAvailable);
        }
        return chromaPredictionMode;
    }

    private void residualLumaI16x16(BitReader reader, boolean leftAvailable, boolean topAvailable, int mbX, int mbY,
            Picture mb, int cbpLuma) {
        int[] dc = new int[16];
        // System.out.println("=================== 16x16 DC ===========================");
        if (!activePps.entropy_coding_mode_flag)
            cavlc[0].readLumaDCBlock(reader, dc, mbX, leftAvailable, leftMBType, topAvailable, topMBType[mbX],
                    CoeffTransformer.zigzag4x4);
        else {
            if (cabac.readCodedBlockFlagLumaDC(mDecoder, mbX, leftMBType, topMBType[mbX], leftAvailable, topAvailable,
                    MBType.I_16x16) == 1)
                cabac.readCoeffs(mDecoder, BlockType.LUMA_16_DC, dc, 0, 16, CoeffTransformer.zigzag4x4,
                        identityMapping16, identityMapping16);
        }

        CoeffTransformer.invDC4x4(dc);
        CoeffTransformer.dequantizeDC4x4(dc, qp);
        reorderDC4x4(dc);

        for (int i = 0; i < 16; i++) {
            int[] ac = new int[16];
            int blkOffLeft = H264Const.MB_BLK_OFF_LEFT[i];
            int blkOffTop = H264Const.MB_BLK_OFF_TOP[i];
            int blkX = (mbX << 2) + blkOffLeft;
            int blkY = (mbY << 2) + blkOffTop;

            if ((cbpLuma & (1 << (i >> 2))) != 0) {
                // System.out.println("=================== 16x16 AC ===========================");

                if (!activePps.entropy_coding_mode_flag) {
                    nCoeff[blkY][blkX] = cavlc[0].readACBlock(reader, ac, blkX, blkOffTop, blkOffLeft != 0
                            || leftAvailable, blkOffLeft == 0 ? leftMBType : I_16x16, blkOffTop != 0 || topAvailable,
                            blkOffTop == 0 ? topMBType[mbX] : I_16x16, 1, 15, CoeffTransformer.zigzag4x4);
                } else {
                    if (cabac.readCodedBlockFlagLumaAC(mDecoder, BlockType.LUMA_15_AC, blkX, blkOffTop, 0, leftMBType,
                            topMBType[mbX], leftAvailable, topAvailable, leftCBPLuma, topCBPLuma[mbX], cbpLuma,
                            MBType.I_16x16) == 1)
                        nCoeff[blkY][blkX] = cabac.readCoeffs(mDecoder, BlockType.LUMA_15_AC, ac, 1, 15,
                                CoeffTransformer.zigzag4x4, identityMapping16, identityMapping16);
                }
                CoeffTransformer.dequantizeAC(ac, qp);
            } else {
                if (!activePps.entropy_coding_mode_flag)
                    cavlc[0].setZeroCoeff(blkX, blkOffTop);
            }
            ac[0] = dc[i];
            CoeffTransformer.idct4x4(ac);
            putBlk(mb.getPlaneData(0), ac, 4, blkOffLeft << 2, blkOffTop << 2);
        }
    }

    private void putBlk(int[] planeData, int[] block, int log2stride, int blkX, int blkY) {
        int stride = 1 << log2stride;
        for (int line = 0, srcOff = 0, dstOff = (blkY << log2stride) + blkX; line < 4; line++) {
            planeData[dstOff] = block[srcOff];
            planeData[dstOff + 1] = block[srcOff + 1];
            planeData[dstOff + 2] = block[srcOff + 2];
            planeData[dstOff + 3] = block[srcOff + 3];
            srcOff += 4;
            dstOff += stride;
        }
    }

    private void putBlk8x8(int[] planeData, int[] block, int log2stride, int blkX, int blkY) {
        int stride = 1 << log2stride;
        for (int line = 0, srcOff = 0, dstOff = (blkY << log2stride) + blkX; line < 8; line++) {
            for (int row = 0; row < 8; row++)
                planeData[dstOff + row] = block[srcOff + row];
            srcOff += 8;
            dstOff += stride;
        }
    }

    public void decodeChroma(BitReader reader, int pattern, int chromaMode, int mbX, int mbY, boolean leftAvailable,
            boolean topAvailable, Picture mb, int qp, MBType curMbType) {

        if (chromaFormat == MONO) {
            Arrays.fill(mb.getPlaneData(1), 128);
            Arrays.fill(mb.getPlaneData(2), 128);
            return;
        }

        int qp1 = calcQpChroma(qp, chromaQpOffset[0]);
        int qp2 = calcQpChroma(qp, chromaQpOffset[1]);
        if (pattern != 0) {
            decodeChromaResidual(reader, leftAvailable, topAvailable, mbX, mbY, pattern, mb, qp1, qp2, curMbType);
        } else if (!activePps.entropy_coding_mode_flag) {
            cavlc[1].setZeroCoeff(mbX << 1, 0);
            cavlc[1].setZeroCoeff((mbX << 1) + 1, 1);
            cavlc[2].setZeroCoeff(mbX << 1, 0);
            cavlc[2].setZeroCoeff((mbX << 1) + 1, 1);
        }
        int addr = mbY * (activeSps.pic_width_in_mbs_minus1 + 1) + mbX;
        mbQps[1][addr] = qp1;
        mbQps[2][addr] = qp2;
        ChromaPredictionBuilder.predictWithMode(mb.getPlaneData(1), chromaMode, mbX, leftAvailable, topAvailable,
                leftRow[1], topLine[1], topLeft[1]);
        ChromaPredictionBuilder.predictWithMode(mb.getPlaneData(2), chromaMode, mbX, leftAvailable, topAvailable,
                leftRow[2], topLine[2], topLeft[2]);
    }

    private void decodeChromaResidual(BitReader reader, boolean leftAvailable, boolean topAvailable, int mbX, int mbY,
            int pattern, Picture mb, int crQp1, int crQp2, MBType curMbType) {
        int[] dc1 = new int[(16 >> chromaFormat.compWidth[1]) >> chromaFormat.compHeight[1]];
        int[] dc2 = new int[(16 >> chromaFormat.compWidth[2]) >> chromaFormat.compHeight[2]];
        if ((pattern & 3) > 0) {
            chromaDC(reader, mbX, leftAvailable, topAvailable, dc1, 1, crQp1, curMbType);
            chromaDC(reader, mbX, leftAvailable, topAvailable, dc2, 2, crQp2, curMbType);
        }
        chromaAC(reader, leftAvailable, topAvailable, mbX, mbY, mb, dc1, 1, crQp1, curMbType, (pattern & 2) > 0);
        chromaAC(reader, leftAvailable, topAvailable, mbX, mbY, mb, dc2, 2, crQp2, curMbType, (pattern & 2) > 0);
    }

    private void chromaDC(BitReader reader, int mbX, boolean leftAvailable, boolean topAvailable, int[] dc, int comp,
            int crQp, MBType curMbType) {
        // System.out.println("============= CHROMA RESIDUAL DC ================");
        if (!activePps.entropy_coding_mode_flag)
            cavlc[comp].readChromaDCBlock(reader, dc, leftAvailable, topAvailable);
        else {
            if (cabac.readCodedBlockFlagChromaDC(mDecoder, mbX, comp, leftMBType, topMBType[mbX], leftAvailable,
                    topAvailable, leftCBPChroma, topCBPChroma[mbX], curMbType) == 1)
                cabac.readCoeffs(mDecoder, BlockType.CHROMA_DC, dc, 0, 4, identityMapping16, identityMapping16,
                        identityMapping16);
        }

        CoeffTransformer.invDC2x2(dc);
        CoeffTransformer.dequantizeDC2x2(dc, crQp);
    }

    private void chromaAC(BitReader reader, boolean leftAvailable, boolean topAvailable, int mbX, int mbY, Picture mb,
            int[] dc, int comp, int crQp, MBType curMbType, boolean codedAC) {
        // System.out.println("============= CHROMA RESIDUAL AC ================");
        for (int i = 0; i < dc.length; i++) {
            int[] ac = new int[16];
            int blkOffLeft = H264Const.MB_BLK_OFF_LEFT[i];
            int blkOffTop = H264Const.MB_BLK_OFF_TOP[i];

            int blkX = (mbX << 1) + blkOffLeft;
            int blkY = (mbY << 1) + blkOffTop;

            if (codedAC) {

                if (!activePps.entropy_coding_mode_flag)
                    cavlc[comp].readACBlock(reader, ac, blkX, blkOffTop, blkOffLeft != 0 || leftAvailable,
                            blkOffLeft == 0 ? leftMBType : curMbType, blkOffTop != 0 || topAvailable,
                            blkOffTop == 0 ? topMBType[mbX] : curMbType, 1, 15, CoeffTransformer.zigzag4x4);
                else {
                    if (cabac.readCodedBlockFlagChromaAC(mDecoder, blkX, blkOffTop, comp, leftMBType, topMBType[mbX],
                            leftAvailable, topAvailable, leftCBPChroma, topCBPChroma[mbX], curMbType) == 1)
                        cabac.readCoeffs(mDecoder, BlockType.CHROMA_AC, ac, 1, 15, CoeffTransformer.zigzag4x4,
                                identityMapping16, identityMapping16);
                }
                CoeffTransformer.dequantizeAC(ac, crQp);
            } else {
                if (!activePps.entropy_coding_mode_flag)
                    cavlc[comp].setZeroCoeff(blkX, blkOffTop);
            }
            ac[0] = dc[i];
            CoeffTransformer.idct4x4(ac);
            putBlk(mb.getPlaneData(comp), ac, 3, blkOffLeft << 2, blkOffTop << 2);
        }
    }

    private int calcQpChroma(int qp, int crQpOffset) {
        return QP_SCALE_CR[MathUtil.clip(qp + crQpOffset, 0, 51)];
    }

    public void decodeMBlockIntraNxN(BitReader reader, int mbIndex, MBType prevMbType, Picture mb) {
        int mbX = mapper.getMbX(mbIndex);
        int mbY = mapper.getMbY(mbIndex);
        int mbAddr = mapper.getAddress(mbIndex);
        boolean leftAvailable = mapper.leftAvailable(mbIndex);
        boolean topAvailable = mapper.topAvailable(mbIndex);
        boolean topLeftAvailable = mapper.topLeftAvailable(mbIndex);
        boolean topRightAvailable = mapper.topRightAvailable(mbIndex);

        boolean transform8x8Used = false;
        if (transform8x8) {
            transform8x8Used = readTransform8x8Flag(reader, leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                    tf8x8Left, tf8x8Top[mbX]);
        }

        int[] lumaModes;
        if (!transform8x8Used) {
            lumaModes = new int[16];
            for (int i = 0; i < 16; i++) {
                int blkX = H264Const.MB_BLK_OFF_LEFT[i];
                int blkY = H264Const.MB_BLK_OFF_TOP[i];
                lumaModes[i] = readPredictionI4x4Block(reader, leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                        blkX, blkY, mbX);
            }
        } else {
            lumaModes = new int[4];
            for (int i = 0; i < 4; i++) {
                int blkX = (i & 1) << 1;
                int blkY = i & 2;
                lumaModes[i] = readPredictionI4x4Block(reader, leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                        blkX, blkY, mbX);
                i4x4PredLeft[blkY + 1] = i4x4PredLeft[blkY];
                i4x4PredTop[(mbX << 2) + blkX + 1] = i4x4PredTop[(mbX << 2) + blkX];
            }
        }
        int chromaMode = readChromaPredMode(reader, mbX, leftAvailable, topAvailable);

        int codedBlockPattern = readCodedBlockPatternIntra(reader, leftAvailable, topAvailable, leftCBPLuma
                | (leftCBPChroma << 4), topCBPLuma[mbX] | (topCBPChroma[mbX] << 4), leftMBType, topMBType[mbX]);

        int cbpLuma = codedBlockPattern & 0xf;
        int cbpChroma = codedBlockPattern >> 4;

        if (cbpLuma > 0 || cbpChroma > 0) {
            qp = (qp + readMBQpDelta(reader, prevMbType) + 52) % 52;
        }
        mbQps[0][mbAddr] = qp;

        residualLuma(reader, leftAvailable, topAvailable, mbX, mbY, mb, codedBlockPattern, MBType.I_NxN,
                transform8x8Used, tf8x8Left, tf8x8Top[mbX]);

        if (!transform8x8Used) {
            for (int i = 0; i < 16; i++) {
                int blkX = (i & 3) << 2;
                int blkY = i & ~3;

                int bi = H264Const.BLK_INV_MAP[i];
                boolean trAvailable = ((bi == 0 || bi == 1 || bi == 4) && topAvailable)
                        || (bi == 5 && topRightAvailable) || bi == 2 || bi == 6 || bi == 8 || bi == 9 || bi == 10
                        || bi == 12 || bi == 14;

                Intra4x4PredictionBuilder.predictWithMode(lumaModes[bi], mb.getPlaneData(0), blkX == 0 ? leftAvailable
                        : true, blkY == 0 ? topAvailable : true, trAvailable, leftRow[0], topLine[0], topLeft[0],
                        (mbX << 4), blkX, blkY);
            }
        } else {
            for (int i = 0; i < 4; i++) {
                int blkX = (i & 1) << 1;
                int blkY = i & 2;

                boolean trAvailable = (i == 0 && topAvailable) || (i == 1 && topRightAvailable) || i == 2;
                boolean tlAvailable = i == 0 ? topLeftAvailable : (i == 1 ? topAvailable : (i == 2 ? leftAvailable
                        : true));

                Intra8x8PredictionBuilder.predictWithMode(lumaModes[i], mb.getPlaneData(0), blkX == 0 ? leftAvailable
                        : true, blkY == 0 ? topAvailable : true, tlAvailable, trAvailable, leftRow[0], topLine[0],
                        topLeft[0], (mbX << 4), blkX << 2, blkY << 2);
            }
        }

        decodeChroma(reader, cbpChroma, chromaMode, mbX, mbY, leftAvailable, topAvailable, mb, qp, MBType.I_NxN);

        mbTypes[mbAddr] = topMBType[mbX] = leftMBType = MBType.I_NxN;
        // System.out.println("idx: " + mbIndex + ", addr: " + address);
        topCBPLuma[mbX] = leftCBPLuma = cbpLuma;
        topCBPChroma[mbX] = leftCBPChroma = cbpChroma;
        tf8x8Left = tf8x8Top[mbX] = transform8x8Used;
        tr8x8Used[mbAddr] = transform8x8Used;

        collectChromaPredictors(mb, mbX);

        saveMvsIntra(mbX, mbY);
    }

    private void saveMvsIntra(int mbX, int mbY) {
        for (int j = 0, blkOffY = mbY << 2, blkInd = 0; j < 4; j++, blkOffY++) {
            for (int i = 0, blkOffX = mbX << 2; i < 4; i++, blkOffX++, blkInd++) {
                mvs[0][blkOffY][blkOffX] = NULL_VECTOR;
                mvs[1][blkOffY][blkOffX] = NULL_VECTOR;
            }
        }
    }

    private void residualLuma(BitReader reader, boolean leftAvailable, boolean topAvailable, int mbX, int mbY,
            Picture mb, int codedBlockPattern, MBType mbType, boolean transform8x8Used, boolean is8x8Left,
            boolean is8x8Top) {
        if (!transform8x8Used)
            residualLuma(reader, leftAvailable, topAvailable, mbX, mbY, mb, codedBlockPattern, mbType);
        else if (activePps.entropy_coding_mode_flag)
            residualLuma8x8CABAC(reader, leftAvailable, topAvailable, mbX, mbY, mb, codedBlockPattern, mbType,
                    is8x8Left, is8x8Top);
        else
            residualLuma8x8CAVLC(reader, leftAvailable, topAvailable, mbX, mbY, mb, codedBlockPattern, mbType);
    }

    private boolean readTransform8x8Flag(BitReader reader, boolean leftAvailable, boolean topAvailable,
            MBType leftType, MBType topType, boolean is8x8Left, boolean is8x8Top) {
        if (!activePps.entropy_coding_mode_flag)
            return readBool(reader, "transform_size_8x8_flag");
        else
            return cabac.readTransform8x8Flag(mDecoder, leftAvailable, topAvailable, leftType, topType, is8x8Left,
                    is8x8Top);
    }

    protected int readCodedBlockPatternIntra(BitReader reader, boolean leftAvailable, boolean topAvailable,
            int leftCBP, int topCBP, MBType leftMB, MBType topMB) {

        if (!activePps.entropy_coding_mode_flag)
            return H264Const.CODED_BLOCK_PATTERN_INTRA_COLOR[readUE(reader, "coded_block_pattern")];
        else
            return cabac.codedBlockPatternIntra(mDecoder, leftAvailable, topAvailable, leftCBP, topCBP, leftMB, topMB);
    }

    protected int readCodedBlockPatternInter(BitReader reader, boolean leftAvailable, boolean topAvailable,
            int leftCBP, int topCBP, MBType leftMB, MBType topMB) {
        if (!activePps.entropy_coding_mode_flag)
            return H264Const.CODED_BLOCK_PATTERN_INTER_COLOR[readUE(reader, "coded_block_pattern")];
        else
            return cabac.codedBlockPatternIntra(mDecoder, leftAvailable, topAvailable, leftCBP, topCBP, leftMB, topMB);
    }

    private void residualLuma(BitReader reader, boolean leftAvailable, boolean topAvailable, int mbX, int mbY,
            Picture mb, int codedBlockPattern, MBType curMbType) {

        int cbpLuma = codedBlockPattern & 0xf;

        for (int i = 0; i < 16; i++) {
            int blkOffLeft = H264Const.MB_BLK_OFF_LEFT[i];
            int blkOffTop = H264Const.MB_BLK_OFF_TOP[i];
            int blkX = (mbX << 2) + blkOffLeft;
            int blkY = (mbY << 2) + blkOffTop;

            if ((cbpLuma & (1 << (i >> 2))) == 0) {
                if (!activePps.entropy_coding_mode_flag)
                    cavlc[0].setZeroCoeff(blkX, blkOffTop);
                continue;
            }
            int[] ac = new int[16];

            if (!activePps.entropy_coding_mode_flag) {
                nCoeff[blkY][blkX] = cavlc[0].readACBlock(reader, ac, blkX, blkOffTop,
                        blkOffLeft != 0 || leftAvailable, blkOffLeft == 0 ? leftMBType : curMbType, blkOffTop != 0
                                || topAvailable, blkOffTop == 0 ? topMBType[mbX] : curMbType, 0, 16,
                        CoeffTransformer.zigzag4x4);
            } else {
                if (cabac.readCodedBlockFlagLumaAC(mDecoder, BlockType.LUMA_16, blkX, blkOffTop, 0, leftMBType,
                        topMBType[mbX], leftAvailable, topAvailable, leftCBPLuma, topCBPLuma[mbX], cbpLuma, curMbType) == 1)
                    nCoeff[blkY][blkX] = cabac.readCoeffs(mDecoder, BlockType.LUMA_16, ac, 0, 16,
                            CoeffTransformer.zigzag4x4, identityMapping16, identityMapping16);
            }

            CoeffTransformer.dequantizeAC(ac, qp);
            CoeffTransformer.idct4x4(ac);
            putBlk(mb.getPlaneData(0), ac, 4, blkOffLeft << 2, blkOffTop << 2);
        }

        if (activePps.entropy_coding_mode_flag)
            cabac.setPrevCBP(codedBlockPattern);
    }

    private void residualLuma8x8CABAC(BitReader reader, boolean leftAvailable, boolean topAvailable, int mbX, int mbY,
            Picture mb, int codedBlockPattern, MBType curMbType, boolean is8x8Left, boolean is8x8Top) {

        // System.out.println("8x8 CABAC!!!");

        int cbpLuma = codedBlockPattern & 0xf;

        for (int i = 0; i < 4; i++) {
            int blkOffLeft = (i & 1) << 1;
            int blkOffTop = i & 2;
            int blkX = (mbX << 2) + blkOffLeft;
            int blkY = (mbY << 2) + blkOffTop;

            if ((cbpLuma & (1 << i)) == 0) {
                continue;
            }
            int[] ac = new int[64];

            // System.out.println("============= CABAC RESIDUAL AC ================");
            nCoeff[blkY][blkX] = nCoeff[blkY][blkX + 1] = nCoeff[blkY + 1][blkX] = nCoeff[blkY + 1][blkX + 1] = cabac
                    .readCoeffs(mDecoder, BlockType.LUMA_64, ac, 0, 64, CoeffTransformer.zigzag8x8, sig_coeff_map_8x8,
                            last_sig_coeff_map_8x8);
            cabac.setCodedBlock(blkX, blkY);
            cabac.setCodedBlock(blkX + 1, blkY);
            cabac.setCodedBlock(blkX, blkY + 1);
            cabac.setCodedBlock(blkX + 1, blkY + 1);

            CoeffTransformer.dequantizeAC8x8(ac, qp);
            CoeffTransformer.idct8x8(ac);
            putBlk8x8(mb.getPlaneData(0), ac, 4, blkOffLeft << 2, blkOffTop << 2);
        }

        cabac.setPrevCBP(codedBlockPattern);
    }

    private void residualLuma8x8CAVLC(BitReader reader, boolean leftAvailable, boolean topAvailable, int mbX, int mbY,
            Picture mb, int codedBlockPattern, MBType curMbType) {

        // System.out.println("8x8 CAVLC!!!");

        int cbpLuma = codedBlockPattern & 0xf;

        for (int i = 0; i < 4; i++) {
            int blk8x8OffLeft = (i & 1) << 1;
            int blk8x8OffTop = i & 2;
            int blkX = (mbX << 2) + blk8x8OffLeft;
            int blkY = (mbY << 2) + blk8x8OffTop;

            if ((cbpLuma & (1 << i)) == 0) {
                cavlc[0].setZeroCoeff(blkX, blk8x8OffTop);
                cavlc[0].setZeroCoeff(blkX + 1, blk8x8OffTop);
                cavlc[0].setZeroCoeff(blkX, blk8x8OffTop + 1);
                cavlc[0].setZeroCoeff(blkX + 1, blk8x8OffTop + 1);
                continue;
            }
            int[] ac64 = new int[64];
            int coeffs = 0;
            for (int j = 0; j < 4; j++) {
                int[] ac16 = new int[16];
                int blkOffLeft = blk8x8OffLeft + (j & 1);
                int blkOffTop = blk8x8OffTop + (j >> 1);
                coeffs += cavlc[0].readACBlock(reader, ac16, blkX + (j & 1), blkOffTop, blkOffLeft != 0
                        || leftAvailable, blkOffLeft == 0 ? leftMBType : curMbType, blkOffTop != 0 || topAvailable,
                        blkOffTop == 0 ? topMBType[mbX] : curMbType, 0, 16, H264Const.identityMapping16);
                for (int k = 0; k < 16; k++)
                    ac64[CoeffTransformer.zigzag8x8[(k << 2) + j]] = ac16[k];
            }
            nCoeff[blkY][blkX] = nCoeff[blkY][blkX + 1] = nCoeff[blkY + 1][blkX] = nCoeff[blkY + 1][blkX + 1] = coeffs;

            CoeffTransformer.dequantizeAC8x8(ac64, qp);
            CoeffTransformer.idct8x8(ac64);
            putBlk8x8(mb.getPlaneData(0), ac64, 4, blk8x8OffLeft << 2, blk8x8OffTop << 2);
        }
    }

    private int readPredictionI4x4Block(BitReader reader, boolean leftAvailable, boolean topAvailable,
            MBType leftMBType, MBType topMBType, int blkX, int blkY, int mbX) {
        int mode = 2;
        if ((leftAvailable || blkX > 0) && (topAvailable || blkY > 0)) {
            int predModeB = topMBType == MBType.I_NxN || blkY > 0 ? i4x4PredTop[(mbX << 2) + blkX] : 2;
            int predModeA = leftMBType == MBType.I_NxN || blkX > 0 ? i4x4PredLeft[blkY] : 2;
            mode = Math.min(predModeB, predModeA);
        }
        if (!prev4x4PredMode(reader)) {
            int rem_intra4x4_pred_mode = rem4x4PredMode(reader);
            mode = rem_intra4x4_pred_mode + (rem_intra4x4_pred_mode < mode ? 0 : 1);
        }
        i4x4PredTop[(mbX << 2) + blkX] = i4x4PredLeft[blkY] = mode;
        return mode;
    }

    private int rem4x4PredMode(BitReader reader) {
        if (!activePps.entropy_coding_mode_flag)
            return readNBit(reader, 3, "MB: rem_intra4x4_pred_mode");
        else
            return cabac.rem4x4PredMode(mDecoder);
    }

    private boolean prev4x4PredMode(BitReader reader) {
        if (!activePps.entropy_coding_mode_flag)
            return readBool(reader, "MBP: prev_intra4x4_pred_mode_flag");
        else
            return cabac.prev4x4PredModeFlag(mDecoder);
    }

    private void decodeInter16x8(BitReader reader, Picture mb, Frame[][] refs, int mbIdx, MBType prevMbType,
            PartPred p0, PartPred p1, MBType curMBType) {
        int mbX = mapper.getMbX(mbIdx);
        int mbY = mapper.getMbY(mbIdx);
        boolean leftAvailable = mapper.leftAvailable(mbIdx);
        boolean topAvailable = mapper.topAvailable(mbIdx);
        boolean topLeftAvailable = mapper.topLeftAvailable(mbIdx);
        boolean topRightAvailable = mapper.topRightAvailable(mbIdx);
        int address = mapper.getAddress(mbIdx);

        int xx = mbX << 2;
        int[] refIdx1 = { 0, 0 }, refIdx2 = { 0, 0 };
        int[][][] x = new int[2][][];

        for (int list = 0; list < 2; list++) {
            if (p0.usesList(list) && numRef[list] > 1)
                refIdx1[list] = readRefIdx(reader, leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                        predModeLeft[0], predModeTop[(mbX << 1)], p0, mbX, 0, 0, 4, 2, list);
            if (p1.usesList(list) && numRef[list] > 1)
                refIdx2[list] = readRefIdx(reader, leftAvailable, true, leftMBType, curMBType, predModeLeft[1], p0, p1,
                        mbX, 0, 2, 4, 2, list);
        }

        Picture[] mbb = { Picture.create(16, 16, chromaFormat), Picture.create(16, 16, chromaFormat) };
        for (int list = 0; list < 2; list++) {
            predictInter16x8(reader, mbb[list], refs, mbX, mbY, leftAvailable, topAvailable, topLeftAvailable,
                    topRightAvailable, xx, refIdx1, refIdx2, x, p0, p1, list);
        }

        prediction.mergePrediction(x[0][0][2], x[1][0][2], p0, 0, mbb[0].getPlaneData(0), mbb[1].getPlaneData(0), 0,
                16, 16, 8, mb.getPlaneData(0), refs, thisFrame);
        prediction.mergePrediction(x[0][8][2], x[1][8][2], p1, 0, mbb[0].getPlaneData(0), mbb[1].getPlaneData(0), 128,
                16, 16, 8, mb.getPlaneData(0), refs, thisFrame);

        predModeLeft[0] = p0;
        predModeLeft[1] = predModeTop[mbX << 1] = predModeTop[(mbX << 1) + 1] = p1;

        residualInter(reader, mb, refs, leftAvailable, topAvailable, mbX, mbY, x, new PartPred[] { p0, p0, p1, p1 },
                mapper.getAddress(mbIdx), prevMbType, curMBType);

        collectPredictors(mb, mbX);

        mbTypes[address] = topMBType[mbX] = leftMBType = curMBType;
    }

    private void predictInter16x8(BitReader reader, Picture mb, Picture[][] references, int mbX, int mbY,
            boolean leftAvailable, boolean topAvailable, boolean tlAvailable, boolean trAvailable, int xx,
            int[] refIdx1, int[] refIdx2, int[][][] x, PartPred p0, PartPred p1, int list) {

        int blk8x8X = mbX << 1;
        int mvX1 = 0, mvY1 = 0, mvX2 = 0, mvY2 = 0, r1 = -1, r2 = -1;
        if (p0.usesList(list)) {

            int mvdX1 = readMVD(reader, 0, leftAvailable, topAvailable, leftMBType, topMBType[mbX], predModeLeft[0],
                    predModeTop[blk8x8X], p0, mbX, 0, 0, 4, 2, list);
            int mvdY1 = readMVD(reader, 1, leftAvailable, topAvailable, leftMBType, topMBType[mbX], predModeLeft[0],
                    predModeTop[blk8x8X], p0, mbX, 0, 0, 4, 2, list);

            int mvpX1 = calcMVPrediction16x8Top(mvLeft[list][0], mvTop[list][mbX << 2], mvTop[list][(mbX << 2) + 4],
                    mvTopLeft[list], leftAvailable, topAvailable, trAvailable, tlAvailable, refIdx1[list], 0);
            int mvpY1 = calcMVPrediction16x8Top(mvLeft[list][0], mvTop[list][mbX << 2], mvTop[list][(mbX << 2) + 4],
                    mvTopLeft[list], leftAvailable, topAvailable, trAvailable, tlAvailable, refIdx1[list], 1);

            mvX1 = mvdX1 + mvpX1;
            mvY1 = mvdY1 + mvpY1;

            debugPrint("MVP: (" + mvpX1 + ", " + mvpY1 + "), MVD: (" + mvdX1 + ", " + mvdY1 + "), MV: (" + mvX1 + ","
                    + mvY1 + "," + refIdx1[list] + ")");

            BlockInterpolator.getBlockLuma(references[list][refIdx1[list]], mb, 0, (mbX << 6) + mvX1,
                    (mbY << 6) + mvY1, 16, 8);
            r1 = refIdx1[list];
        }
        int[] v1 = { mvX1, mvY1, r1 };

        if (p1.usesList(list)) {
            int mvdX2 = readMVD(reader, 0, leftAvailable, true, leftMBType, MBType.P_16x8, predModeLeft[1], p0, p1,
                    mbX, 0, 2, 4, 2, list);
            int mvdY2 = readMVD(reader, 1, leftAvailable, true, leftMBType, MBType.P_16x8, predModeLeft[1], p0, p1,
                    mbX, 0, 2, 4, 2, list);

            int mvpX2 = calcMVPrediction16x8Bottom(mvLeft[list][2], v1, null, mvLeft[list][1], leftAvailable, true,
                    false, leftAvailable, refIdx2[list], 0);
            int mvpY2 = calcMVPrediction16x8Bottom(mvLeft[list][2], v1, null, mvLeft[list][1], leftAvailable, true,
                    false, leftAvailable, refIdx2[list], 1);

            mvX2 = mvdX2 + mvpX2;
            mvY2 = mvdY2 + mvpY2;

            debugPrint("MVP: (" + mvpX2 + ", " + mvpY2 + "), MVD: (" + mvdX2 + ", " + mvdY2 + "), MV: (" + mvX2 + ","
                    + mvY2 + "," + refIdx2[list] + ")");

            BlockInterpolator.getBlockLuma(references[list][refIdx2[list]], mb, 128, (mbX << 6) + mvX2, (mbY << 6) + 32
                    + mvY2, 16, 8);
            r2 = refIdx2[list];
        }
        int[] v2 = { mvX2, mvY2, r2 };

        copyVect(mvTopLeft[list], mvTop[list][xx + 3]);
        saveVect(mvLeft[list], 0, 2, mvX1, mvY1, r1);
        saveVect(mvLeft[list], 2, 4, mvX2, mvY2, r2);
        saveVect(mvTop[list], xx, xx + 4, mvX2, mvY2, r2);

        x[list] = new int[][] { v1, v1, v1, v1, v1, v1, v1, v1, v2, v2, v2, v2, v2, v2, v2, v2 };
    }

    private void residualInter(BitReader reader, Picture mb, Frame[][] refs, boolean leftAvailable,
            boolean topAvailable, int mbX, int mbY, int[][][] x, PartPred[] pp, int mbAddr, MBType prevMbType,
            MBType curMbType) {
        int codedBlockPattern = readCodedBlockPatternInter(reader, leftAvailable, topAvailable, leftCBPLuma
                | (leftCBPChroma << 4), topCBPLuma[mbX] | (topCBPChroma[mbX] << 4), leftMBType, topMBType[mbX]);
        int cbpLuma = codedBlockPattern & 0xf;
        int cbpChroma = codedBlockPattern >> 4;

        Picture mb1 = Picture.create(16, 16, chromaFormat);

        boolean transform8x8Used = false;
        if (cbpLuma != 0 && transform8x8) {
            transform8x8Used = readTransform8x8Flag(reader, leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                    tf8x8Left, tf8x8Top[mbX]);
        }

        if (cbpLuma > 0 || cbpChroma > 0) {
            int mbQpDelta = readMBQpDelta(reader, prevMbType);
            qp = (qp + mbQpDelta + 52) % 52;
        }
        mbQps[0][mbAddr] = qp;

        residualLuma(reader, leftAvailable, topAvailable, mbX, mbY, mb1, codedBlockPattern, curMbType,
                transform8x8Used, tf8x8Left, tf8x8Top[mbX]);

        saveMvs(x, mbX, mbY);

        if (chromaFormat == MONO) {
            Arrays.fill(mb.getPlaneData(1), 128);
            Arrays.fill(mb.getPlaneData(2), 128);
        } else {
            decodeChromaInter(reader, cbpChroma, refs, x, pp, leftAvailable, topAvailable, mbX, mbY, mbAddr, qp, mb,
                    mb1);
        }

        mergeResidual(mb, mb1);

        // System.out.println("idx: " + mbIndex + ", addr: " + address);
        topCBPLuma[mbX] = leftCBPLuma = cbpLuma;
        topCBPChroma[mbX] = leftCBPChroma = cbpChroma;
        tf8x8Left = tf8x8Top[mbX] = transform8x8Used;
        tr8x8Used[mbAddr] = transform8x8Used;
    }

    private void mergeResidual(Picture mb, Picture mb1) {
        for (int j = 0; j < 3; j++) {
            int[] to = mb.getPlaneData(j), from = mb1.getPlaneData(j);
            for (int i = 0; i < to.length; i++) {
                to[i] = clip(to[i] + from[i], 0, 255);
            }
        }
    }

    private void decodeInter8x16(BitReader reader, Picture mb, Frame[][] refs, int mbIdx, MBType prevMbType,
            PartPred p0, PartPred p1, MBType curMBType) {
        int mbX = mapper.getMbX(mbIdx);
        int mbY = mapper.getMbY(mbIdx);
        boolean leftAvailable = mapper.leftAvailable(mbIdx);
        boolean topAvailable = mapper.topAvailable(mbIdx);
        boolean topLeftAvailable = mapper.topLeftAvailable(mbIdx);
        boolean topRightAvailable = mapper.topRightAvailable(mbIdx);
        int address = mapper.getAddress(mbIdx);

        int[][][] x = new int[2][][];

        int[] refIdx1 = { 0, 0 }, refIdx2 = { 0, 0 };
        for (int list = 0; list < 2; list++) {
            if (p0.usesList(list) && numRef[list] > 1)
                refIdx1[list] = readRefIdx(reader, leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                        predModeLeft[0], predModeTop[mbX << 1], p0, mbX, 0, 0, 2, 4, list);
            if (p1.usesList(list) && numRef[list] > 1)
                refIdx2[list] = readRefIdx(reader, true, topAvailable, curMBType, topMBType[mbX], p0,
                        predModeTop[(mbX << 1) + 1], p1, mbX, 2, 0, 2, 4, list);
        }

        Picture[] mbb = { Picture.create(16, 16, chromaFormat), Picture.create(16, 16, chromaFormat) };

        for (int list = 0; list < 2; list++) {
            predictInter8x16(reader, mbb[list], refs, mbX, mbY, leftAvailable, topAvailable, topLeftAvailable,
                    topRightAvailable, x, refIdx1, refIdx2, list, p0, p1);
        }

        prediction.mergePrediction(x[0][0][2], x[1][0][2], p0, 0, mbb[0].getPlaneData(0), mbb[1].getPlaneData(0), 0,
                16, 8, 16, mb.getPlaneData(0), refs, thisFrame);
        prediction.mergePrediction(x[0][2][2], x[1][2][2], p1, 0, mbb[0].getPlaneData(0), mbb[1].getPlaneData(0), 8,
                16, 8, 16, mb.getPlaneData(0), refs, thisFrame);

        predModeTop[mbX << 1] = p0;
        predModeTop[(mbX << 1) + 1] = predModeLeft[0] = predModeLeft[1] = p1;

        residualInter(reader, mb, refs, leftAvailable, topAvailable, mbX, mbY, x, new PartPred[] { p0, p1, p0, p1 },
                mapper.getAddress(mbIdx), prevMbType, curMBType);

        collectPredictors(mb, mbX);

        mbTypes[address] = topMBType[mbX] = leftMBType = curMBType;
    }

    private void predictInter8x16(BitReader reader, Picture mb, Picture[][] references, int mbX, int mbY,
            boolean leftAvailable, boolean topAvailable, boolean tlAvailable, boolean trAvailable, int[][][] x,
            int[] refIdx1, int[] refIdx2, int list, PartPred p0, PartPred p1) {
        int xx = mbX << 2;

        int blk8x8X = (mbX << 1);

        int mvX1 = 0, mvY1 = 0, r1 = -1, mvX2 = 0, mvY2 = 0, r2 = -1;
        if (p0.usesList(list)) {
            int mvdX1 = readMVD(reader, 0, leftAvailable, topAvailable, leftMBType, topMBType[mbX], predModeLeft[0],
                    predModeTop[blk8x8X], p0, mbX, 0, 0, 2, 4, list);
            int mvdY1 = readMVD(reader, 1, leftAvailable, topAvailable, leftMBType, topMBType[mbX], predModeLeft[0],
                    predModeTop[blk8x8X], p0, mbX, 0, 0, 2, 4, list);

            int mvpX1 = calcMVPrediction8x16Left(mvLeft[list][0], mvTop[list][mbX << 2], mvTop[list][(mbX << 2) + 2],
                    mvTopLeft[list], leftAvailable, topAvailable, topAvailable, tlAvailable, refIdx1[list], 0);
            int mvpY1 = calcMVPrediction8x16Left(mvLeft[list][0], mvTop[list][mbX << 2], mvTop[list][(mbX << 2) + 2],
                    mvTopLeft[list], leftAvailable, topAvailable, topAvailable, tlAvailable, refIdx1[list], 1);

            mvX1 = mvdX1 + mvpX1;
            mvY1 = mvdY1 + mvpY1;

            debugPrint("MVP: (" + mvpX1 + ", " + mvpY1 + "), MVD: (" + mvdX1 + ", " + mvdY1 + "), MV: (" + mvX1 + ","
                    + mvY1 + "," + refIdx1[list] + ")");

            BlockInterpolator.getBlockLuma(references[list][refIdx1[list]], mb, 0, (mbX << 6) + mvX1,
                    (mbY << 6) + mvY1, 8, 16);
            r1 = refIdx1[list];
        }
        int[] v1 = { mvX1, mvY1, r1 };

        if (p1.usesList(list)) {
            int mvdX2 = readMVD(reader, 0, true, topAvailable, MBType.P_8x16, topMBType[mbX], p0,
                    predModeTop[blk8x8X + 1], p1, mbX, 2, 0, 2, 4, list);
            int mvdY2 = readMVD(reader, 1, true, topAvailable, MBType.P_8x16, topMBType[mbX], p0,
                    predModeTop[blk8x8X + 1], p1, mbX, 2, 0, 2, 4, list);

            int mvpX2 = calcMVPrediction8x16Right(v1, mvTop[list][(mbX << 2) + 2], mvTop[list][(mbX << 2) + 4],
                    mvTop[list][(mbX << 2) + 1], true, topAvailable, trAvailable, topAvailable, refIdx2[list], 0);
            int mvpY2 = calcMVPrediction8x16Right(v1, mvTop[list][(mbX << 2) + 2], mvTop[list][(mbX << 2) + 4],
                    mvTop[list][(mbX << 2) + 1], true, topAvailable, trAvailable, topAvailable, refIdx2[list], 1);

            mvX2 = mvdX2 + mvpX2;
            mvY2 = mvdY2 + mvpY2;

            debugPrint("MVP: (" + mvpX2 + ", " + mvpY2 + "), MVD: (" + mvdX2 + ", " + mvdY2 + "), MV: (" + mvX2 + ","
                    + mvY2 + "," + refIdx2[list] + ")");

            BlockInterpolator.getBlockLuma(references[list][refIdx2[list]], mb, 8, (mbX << 6) + 32 + mvX2, (mbY << 6)
                    + mvY2, 8, 16);
            r2 = refIdx2[list];
        }
        int[] v2 = { mvX2, mvY2, r2 };

        copyVect(mvTopLeft[list], mvTop[list][xx + 3]);
        saveVect(mvTop[list], xx, xx + 2, mvX1, mvY1, r1);
        saveVect(mvTop[list], xx + 2, xx + 4, mvX2, mvY2, r2);
        saveVect(mvLeft[list], 0, 4, mvX2, mvY2, r2);

        x[list] = new int[][] { v1, v1, v2, v2, v1, v1, v2, v2, v1, v1, v2, v2, v1, v1, v2, v2 };
    }

    private void decodeInter16x16(BitReader reader, Picture mb, Frame[][] refs, int mbIdx, MBType prevMbType,
            PartPred p0, MBType curMBType) {
        int mbX = mapper.getMbX(mbIdx);
        int mbY = mapper.getMbY(mbIdx);
        boolean leftAvailable = mapper.leftAvailable(mbIdx);
        boolean topAvailable = mapper.topAvailable(mbIdx);
        boolean topLeftAvailable = mapper.topLeftAvailable(mbIdx);
        boolean topRightAvailable = mapper.topRightAvailable(mbIdx);
        int address = mapper.getAddress(mbIdx);
        int[][][] x = new int[2][][];

        int xx = mbX << 2;
        int[] refIdx = { 0, 0 };
        for (int list = 0; list < 2; list++) {
            if (p0.usesList(list) && numRef[list] > 1)
                refIdx[list] = readRefIdx(reader, leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                        predModeLeft[0], predModeTop[(mbX << 1)], p0, mbX, 0, 0, 4, 4, list);
        }
        Picture[] mbb = { Picture.create(16, 16, chromaFormat), Picture.create(16, 16, chromaFormat) };
        for (int list = 0; list < 2; list++) {
            predictInter16x16(reader, mbb[list], refs, mbX, mbY, leftAvailable, topAvailable, topLeftAvailable,
                    topRightAvailable, x, xx, refIdx, list, p0);
        }

        prediction.mergePrediction(x[0][0][2], x[1][0][2], p0, 0, mbb[0].getPlaneData(0), mbb[1].getPlaneData(0), 0,
                16, 16, 16, mb.getPlaneData(0), refs, thisFrame);

        predModeLeft[0] = predModeLeft[1] = predModeTop[mbX << 1] = predModeTop[(mbX << 1) + 1] = p0;

        residualInter(reader, mb, refs, leftAvailable, topAvailable, mbX, mbY, x, new PartPred[] { p0, p0, p0, p0 },
                mapper.getAddress(mbIdx), prevMbType, curMBType);

        collectPredictors(mb, mbX);

        mbTypes[address] = topMBType[mbX] = leftMBType = curMBType;
    }

    private void predictInter16x16(BitReader reader, Picture mb, Picture[][] references, int mbX, int mbY,
            boolean leftAvailable, boolean topAvailable, boolean tlAvailable, boolean trAvailable, int[][][] x, int xx,
            int[] refIdx, int list, PartPred curPred) {
        int blk8x8X = (mbX << 1);

        int mvX = 0, mvY = 0, r = -1;
        if (curPred.usesList(list)) {
            int mvpX = calcMVPredictionMedian(mvLeft[list][0], mvTop[list][mbX << 2], mvTop[list][(mbX << 2) + 4],
                    mvTopLeft[list], leftAvailable, topAvailable, trAvailable, tlAvailable, refIdx[list], 0);
            int mvpY = calcMVPredictionMedian(mvLeft[list][0], mvTop[list][mbX << 2], mvTop[list][(mbX << 2) + 4],
                    mvTopLeft[list], leftAvailable, topAvailable, trAvailable, tlAvailable, refIdx[list], 1);
            int mvdX = readMVD(reader, 0, leftAvailable, topAvailable, leftMBType, topMBType[mbX], predModeLeft[0],
                    predModeTop[blk8x8X], curPred, mbX, 0, 0, 4, 4, list);
            int mvdY = readMVD(reader, 1, leftAvailable, topAvailable, leftMBType, topMBType[mbX], predModeLeft[0],
                    predModeTop[blk8x8X], curPred, mbX, 0, 0, 4, 4, list);

            mvX = mvdX + mvpX;
            mvY = mvdY + mvpY;

            debugPrint("MVP: (" + mvpX + ", " + mvpY + "), MVD: (" + mvdX + ", " + mvdY + "), MV: (" + mvX + "," + mvY
                    + "," + refIdx[list] + ")");
            r = refIdx[list];

            BlockInterpolator.getBlockLuma(references[list][r], mb, 0, (mbX << 6) + mvX, (mbY << 6) + mvY, 16, 16);
        }

        copyVect(mvTopLeft[list], mvTop[list][xx + 3]);
        saveVect(mvTop[list], xx, xx + 4, mvX, mvY, r);
        saveVect(mvLeft[list], 0, 4, mvX, mvY, r);

        int[] v = { mvX, mvY, r };
        x[list] = new int[][] { v, v, v, v, v, v, v, v, v, v, v, v, v, v, v, v };
    }

    private int readRefIdx(BitReader reader, boolean leftAvailable, boolean topAvailable, MBType leftType,
            MBType topType, PartPred leftPred, PartPred topPred, PartPred curPred, int mbX, int partX, int partY,
            int partW, int partH, int list) {
        if (!activePps.entropy_coding_mode_flag)
            return readTE(reader, numRef[list] - 1);
        else
            return cabac.readRefIdx(mDecoder, leftAvailable, topAvailable, leftType, topType, leftPred, topPred,
                    curPred, mbX, partX, partY, partW, partH, list);
    }

    private void saveVect(int[][] mv, int from, int to, int x, int y, int r) {
        for (int i = from; i < to; i++) {
            mv[i][0] = x;
            mv[i][1] = y;
            mv[i][2] = r;
        }
    }

    public int calcMVPredictionMedian(int[] a, int[] b, int[] c, int[] d, boolean aAvb, boolean bAvb, boolean cAvb,
            boolean dAvb, int ref, int comp) {

        if (!cAvb) {
            c = d;
            cAvb = dAvb;
        }

        if (aAvb && !bAvb && !cAvb) {
            b = c = a;
            bAvb = cAvb = aAvb;
        }

        a = aAvb ? a : NULL_VECTOR;
        b = bAvb ? b : NULL_VECTOR;
        c = cAvb ? c : NULL_VECTOR;

        if (a[2] == ref && b[2] != ref && c[2] != ref)
            return a[comp];
        else if (b[2] == ref && a[2] != ref && c[2] != ref)
            return b[comp];
        else if (c[2] == ref && a[2] != ref && b[2] != ref)
            return c[comp];

        return a[comp] + b[comp] + c[comp] - min(a[comp], b[comp], c[comp]) - max(a[comp], b[comp], c[comp]);
    }

    private int max(int x, int x2, int x3) {
        return x > x2 ? (x > x3 ? x : x3) : (x2 > x3 ? x2 : x3);
    }

    private int min(int x, int x2, int x3) {
        return x < x2 ? (x < x3 ? x : x3) : (x2 < x3 ? x2 : x3);
    }

    public int calcMVPrediction16x8Top(int[] a, int[] b, int[] c, int[] d, boolean aAvb, boolean bAvb, boolean cAvb,
            boolean dAvb, int refIdx, int comp) {
        if (bAvb && b[2] == refIdx)
            return b[comp];
        else
            return calcMVPredictionMedian(a, b, c, d, aAvb, bAvb, cAvb, dAvb, refIdx, comp);
    }

    public int calcMVPrediction16x8Bottom(int[] a, int[] b, int[] c, int[] d, boolean aAvb, boolean bAvb, boolean cAvb,
            boolean dAvb, int refIdx, int comp) {

        if (aAvb && a[2] == refIdx)
            return a[comp];
        else
            return calcMVPredictionMedian(a, b, c, d, aAvb, bAvb, cAvb, dAvb, refIdx, comp);
    }

    public int calcMVPrediction8x16Left(int[] a, int[] b, int[] c, int[] d, boolean aAvb, boolean bAvb, boolean cAvb,
            boolean dAvb, int refIdx, int comp) {

        if (aAvb && a[2] == refIdx)
            return a[comp];
        else
            return calcMVPredictionMedian(a, b, c, d, aAvb, bAvb, cAvb, dAvb, refIdx, comp);
    }

    public int calcMVPrediction8x16Right(int[] a, int[] b, int[] c, int[] d, boolean aAvb, boolean bAvb, boolean cAvb,
            boolean dAvb, int refIdx, int comp) {
        int[] lc = cAvb ? c : (dAvb ? d : NULL_VECTOR);

        if (lc[2] == refIdx)
            return lc[comp];
        else
            return calcMVPredictionMedian(a, b, c, d, aAvb, bAvb, cAvb, dAvb, refIdx, comp);
    }

    public void decodeMBInter8x8(BitReader reader, int mb_type, Frame[][] references, Picture mb, SliceType sliceType,
            int mbIdx, boolean mb_field_decoding_flag, MBType prevMbType, boolean ref0) {

        int mbX = mapper.getMbX(mbIdx);
        int mbY = mapper.getMbY(mbIdx);
        int mbAddr = mapper.getAddress(mbIdx);
        boolean leftAvailable = mapper.leftAvailable(mbIdx);
        boolean topAvailable = mapper.topAvailable(mbIdx);
        boolean topLeftAvailable = mapper.topLeftAvailable(mbIdx);
        boolean topRightAvailable = mapper.topRightAvailable(mbIdx);

        int[][][] x = new int[2][16][3];
        PartPred[] pp = new PartPred[4];
        for (int i = 0; i < 16; i++)
            x[0][i][2] = x[1][i][2] = -1;

        Picture mb1 = Picture.create(16, 16, chromaFormat);

        MBType curMBType;
        boolean noSubMBLessThen8x8;
        if (sliceType == SliceType.P) {
            noSubMBLessThen8x8 = predict8x8P(reader, references[0], mb1, ref0, mbX, mbY, leftAvailable, topAvailable,
                    topLeftAvailable, topRightAvailable, x, pp);
            curMBType = P_8x8;
        } else {
            noSubMBLessThen8x8 = predict8x8B(reader, references, mb1, ref0, mbX, mbY, leftAvailable, topAvailable,
                    topLeftAvailable, topRightAvailable, x, pp);
            curMBType = B_8x8;
        }

        int codedBlockPattern = readCodedBlockPatternInter(reader, leftAvailable, topAvailable, leftCBPLuma
                | (leftCBPChroma << 4), topCBPLuma[mbX] | (topCBPChroma[mbX] << 4), leftMBType, topMBType[mbX]);

        int cbpLuma = codedBlockPattern & 0xf;
        int cbpChroma = codedBlockPattern >> 4;

        boolean transform8x8Used = false;
        if (transform8x8 && cbpLuma != 0 && noSubMBLessThen8x8) {
            transform8x8Used = readTransform8x8Flag(reader, leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                    tf8x8Left, tf8x8Top[mbX]);
        }

        if (cbpLuma > 0 || cbpChroma > 0) {
            qp = (qp + readMBQpDelta(reader, prevMbType) + 52) % 52;
        }
        mbQps[0][mbAddr] = qp;

        residualLuma(reader, leftAvailable, topAvailable, mbX, mbY, mb, codedBlockPattern, curMBType, transform8x8Used,
                tf8x8Left, tf8x8Top[mbX]);

        saveMvs(x, mbX, mbY);

        decodeChromaInter(reader, codedBlockPattern >> 4, references, x, pp, leftAvailable, topAvailable, mbX, mbY,
                mbAddr, qp, mb, mb1);

        mergeResidual(mb, mb1);

        collectPredictors(mb, mbX);

        mbTypes[mbAddr] = topMBType[mbX] = leftMBType = curMBType;
        topCBPLuma[mbX] = leftCBPLuma = cbpLuma;
        topCBPChroma[mbX] = leftCBPChroma = cbpChroma;
        tf8x8Left = tf8x8Top[mbX] = transform8x8Used;
        tr8x8Used[mbAddr] = transform8x8Used;
    }

    private boolean predict8x8P(BitReader reader, Picture[] references, Picture mb, boolean ref0, int mbX, int mbY,
            boolean leftAvailable, boolean topAvailable, boolean tlAvailable, boolean topRightAvailable, int[][][] x,
            PartPred[] pp) {
        int[] subMbTypes = new int[4];
        for (int i = 0; i < 4; i++) {
            subMbTypes[i] = readSubMBTypeP(reader);
        }
        Arrays.fill(pp, L0);
        int blk8x8X = mbX << 1;

        int[] refIdx = new int[4];
        if (numRef[0] > 1 && !ref0) {
            refIdx[0] = readRefIdx(reader, leftAvailable, topAvailable, leftMBType, topMBType[mbX], L0, L0, L0, mbX, 0,
                    0, 2, 2, 0);
            refIdx[1] = readRefIdx(reader, true, topAvailable, P_8x8, topMBType[mbX], L0, L0, L0, mbX, 2, 0, 2, 2, 0);
            refIdx[2] = readRefIdx(reader, leftAvailable, true, leftMBType, P_8x8, L0, L0, L0, mbX, 0, 2, 2, 2, 0);
            refIdx[3] = readRefIdx(reader, true, true, P_8x8, P_8x8, L0, L0, L0, mbX, 2, 2, 2, 2, 0);
        }

        decodeSubMb8x8(reader, subMbTypes[0], references, mbX << 6, mbY << 6, x[0], mvTopLeft[0], mvTop[0][mbX << 2],
                mvTop[0][(mbX << 2) + 1], mvTop[0][(mbX << 2) + 2], mvLeft[0][0], mvLeft[0][1], tlAvailable,
                topAvailable, topAvailable, leftAvailable, x[0][0], x[0][1], x[0][4], x[0][5], refIdx[0], mb, 0, 0, 0,
                mbX, leftMBType, topMBType[mbX], P_8x8, L0, L0, L0, 0);

        decodeSubMb8x8(reader, subMbTypes[1], references, (mbX << 6) + 32, mbY << 6, x[0], mvTop[0][(mbX << 2) + 1],
                mvTop[0][(mbX << 2) + 2], mvTop[0][(mbX << 2) + 3], mvTop[0][(mbX << 2) + 4], x[0][1], x[0][5],
                topAvailable, topAvailable, topRightAvailable, true, x[0][2], x[0][3], x[0][6], x[0][7], refIdx[1], mb,
                8, 2, 0, mbX, P_8x8, topMBType[mbX], P_8x8, L0, L0, L0, 0);

        decodeSubMb8x8(reader, subMbTypes[2], references, mbX << 6, (mbY << 6) + 32, x[0], mvLeft[0][1], x[0][4],
                x[0][5], x[0][6], mvLeft[0][2], mvLeft[0][3], leftAvailable, true, true, leftAvailable, x[0][8],
                x[0][9], x[0][12], x[0][13], refIdx[2], mb, 128, 0, 2, mbX, leftMBType, P_8x8, P_8x8, L0, L0, L0, 0);

        decodeSubMb8x8(reader, subMbTypes[3], references, (mbX << 6) + 32, (mbY << 6) + 32, x[0], x[0][5], x[0][6],
                x[0][7], null, x[0][9], x[0][13], true, true, false, true, x[0][10], x[0][11], x[0][14], x[0][15],
                refIdx[3], mb, 136, 2, 2, mbX, P_8x8, P_8x8, P_8x8, L0, L0, L0, 0);

        savePrediction8x8(mbX, x[0], 0);

        predModeLeft[0] = predModeLeft[1] = predModeTop[blk8x8X] = predModeTop[blk8x8X + 1] = L0;

        return subMbTypes[0] == 0 && subMbTypes[1] == 0 && subMbTypes[2] == 0 && subMbTypes[3] == 0;
    }

    private boolean predict8x8B(BitReader reader, Frame[][] refs, Picture mb, boolean ref0, int mbX, int mbY,
            boolean leftAvailable, boolean topAvailable, boolean tlAvailable, boolean topRightAvailable, int[][][] x,
            PartPred[] p) {

        int[] subMbTypes = new int[4];
        for (int i = 0; i < 4; i++) {
            subMbTypes[i] = readSubMBTypeB(reader);
            p[i] = bPartPredModes[subMbTypes[i]];
        }

        int[][] refIdx = new int[2][4];
        for (int list = 0; list < 2; list++) {
            if (numRef[list] <= 1)
                continue;
            if (p[0].usesList(list))
                refIdx[list][0] = readRefIdx(reader, leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                        predModeLeft[0], predModeTop[mbX << 1], p[0], mbX, 0, 0, 2, 2, list);
            if (p[1].usesList(list))
                refIdx[list][1] = readRefIdx(reader, true, topAvailable, B_8x8, topMBType[mbX], p[0],
                        predModeTop[(mbX << 1) + 1], p[1], mbX, 2, 0, 2, 2, list);
            if (p[2].usesList(list))
                refIdx[list][2] = readRefIdx(reader, leftAvailable, true, leftMBType, B_8x8, predModeLeft[1], p[0],
                        p[2], mbX, 0, 2, 2, 2, list);
            if (p[3].usesList(list))
                refIdx[list][3] = readRefIdx(reader, true, true, B_8x8, B_8x8, p[2], p[1], p[3], mbX, 2, 2, 2, 2, list);
        }

        Picture[] mbb = { Picture.create(16, 16, chromaFormat), Picture.create(16, 16, chromaFormat) };

        PartPred[] _pp = new PartPred[4];
        for (int i = 0; i < 4; i++) {
            if (p[i] == Direct)
                predictBDirect(refs, mbX, mbY, leftAvailable, topAvailable, tlAvailable, topRightAvailable, x, _pp, mb,
                        ARRAY[i]);
        }

        int blk8x8X = mbX << 1;
        for (int list = 0; list < 2; list++) {
            if (p[0].usesList(list))
                decodeSubMb8x8(reader, bSubMbTypes[subMbTypes[0]], refs[list], mbX << 6, mbY << 6, x[list],
                        mvTopLeft[list], mvTop[list][mbX << 2], mvTop[list][(mbX << 2) + 1],
                        mvTop[list][(mbX << 2) + 2], mvLeft[list][0], mvLeft[list][1], tlAvailable, topAvailable,
                        topAvailable, leftAvailable, x[list][0], x[list][1], x[list][4], x[list][5], refIdx[list][0],
                        mbb[list], 0, 0, 0, mbX, leftMBType, topMBType[mbX], B_8x8, predModeLeft[0],
                        predModeTop[blk8x8X], p[0], list);

            if (p[1].usesList(list))
                decodeSubMb8x8(reader, bSubMbTypes[subMbTypes[1]], refs[list], (mbX << 6) + 32, mbY << 6, x[list],
                        mvTop[list][(mbX << 2) + 1], mvTop[list][(mbX << 2) + 2], mvTop[list][(mbX << 2) + 3],
                        mvTop[list][(mbX << 2) + 4], x[list][1], x[list][5], topAvailable, topAvailable,
                        topRightAvailable, true, x[list][2], x[list][3], x[list][6], x[list][7], refIdx[list][1],
                        mbb[list], 8, 2, 0, mbX, B_8x8, topMBType[mbX], B_8x8, p[0], predModeTop[blk8x8X + 1], p[1],
                        list);

            if (p[2].usesList(list))
                decodeSubMb8x8(reader, bSubMbTypes[subMbTypes[2]], refs[list], mbX << 6, (mbY << 6) + 32, x[list],
                        mvLeft[list][1], x[list][4], x[list][5], x[list][6], mvLeft[list][2], mvLeft[list][3],
                        leftAvailable, true, true, leftAvailable, x[list][8], x[list][9], x[list][12], x[list][13],
                        refIdx[list][2], mbb[list], 128, 0, 2, mbX, leftMBType, B_8x8, B_8x8, predModeLeft[1], p[0],
                        p[2], list);

            if (p[3].usesList(list))
                decodeSubMb8x8(reader, bSubMbTypes[subMbTypes[3]], refs[list], (mbX << 6) + 32, (mbY << 6) + 32,
                        x[list], x[list][5], x[list][6], x[list][7], null, x[list][9], x[list][13], true, true, false,
                        true, x[list][10], x[list][11], x[list][14], x[list][15], refIdx[list][3], mbb[list], 136, 2,
                        2, mbX, B_8x8, B_8x8, B_8x8, p[2], p[1], p[3], list);
        }

        for (int i = 0; i < 4; i++) {
            int blk4x4 = BLK8x8_BLOCKS[i][0];
            prediction.mergePrediction(x[0][blk4x4][2], x[1][blk4x4][2], p[i], 0, mbb[0].getPlaneData(0),
                    mbb[1].getPlaneData(0), BLK_8x8_MB_OFF_LUMA[i], 16, 8, 8, mb.getPlaneData(0), refs, thisFrame);
        }

        predModeLeft[0] = p[1];
        predModeTop[blk8x8X] = p[2];
        predModeLeft[1] = predModeTop[blk8x8X + 1] = p[3];

        savePrediction8x8(mbX, x[0], 0);
        savePrediction8x8(mbX, x[1], 1);

        for (int i = 0; i < 4; i++)
            if (p[i] == Direct)
                p[i] = _pp[i];

        return bSubMbTypes[subMbTypes[0]] == 0 && bSubMbTypes[subMbTypes[1]] == 0 && bSubMbTypes[subMbTypes[2]] == 0
                && bSubMbTypes[subMbTypes[3]] == 0;
    }

    private void decodeMBBiDirect(int mbIdx, BitReader reader, boolean field, MBType prevMbType, Picture mb,
            Frame[][] references) {
        int mbX = mapper.getMbX(mbIdx);
        int mbY = mapper.getMbY(mbIdx);
        int mbAddr = mapper.getAddress(mbIdx);
        boolean lAvb = mapper.leftAvailable(mbIdx);
        boolean tAvb = mapper.topAvailable(mbIdx);
        boolean tlAvb = mapper.topLeftAvailable(mbIdx);
        boolean trAvb = mapper.topRightAvailable(mbIdx);

        int[][][] x = new int[2][16][3];
        for (int i = 0; i < 16; i++)
            x[0][i][2] = x[1][i][2] = -1;

        Picture mb1 = Picture.create(16, 16, chromaFormat);
        PartPred[] pp = new PartPred[4];

        predictBDirect(references, mbX, mbY, lAvb, tAvb, tlAvb, trAvb, x, pp, mb1, identityMapping4);

        int codedBlockPattern = readCodedBlockPatternInter(reader, lAvb, tAvb, leftCBPLuma | (leftCBPChroma << 4),
                topCBPLuma[mbX] | (topCBPChroma[mbX] << 4), leftMBType, topMBType[mbX]);

        int cbpLuma = codedBlockPattern & 0xf;
        int cbpChroma = codedBlockPattern >> 4;

        boolean transform8x8Used = false;
        if (transform8x8 && cbpLuma != 0 && activeSps.direct_8x8_inference_flag) {
            transform8x8Used = readTransform8x8Flag(reader, lAvb, tAvb, leftMBType, topMBType[mbX], tf8x8Left,
                    tf8x8Top[mbX]);
        }

        if (cbpLuma > 0 || cbpChroma > 0) {
            qp = (qp + readMBQpDelta(reader, prevMbType) + 52) % 52;
        }
        mbQps[0][mbAddr] = qp;

        residualLuma(reader, lAvb, tAvb, mbX, mbY, mb, codedBlockPattern, MBType.P_8x8, transform8x8Used, tf8x8Left,
                tf8x8Top[mbX]);

        savePrediction8x8(mbX, x[0], 0);
        savePrediction8x8(mbX, x[1], 1);
        saveMvs(x, mbX, mbY);

        decodeChromaInter(reader, codedBlockPattern >> 4, references, x, pp, lAvb, tAvb, mbX, mbY, mbAddr, qp, mb, mb1);

        mergeResidual(mb, mb1);

        collectPredictors(mb, mbX);

        mbTypes[mbAddr] = topMBType[mbX] = leftMBType = MBType.B_Direct_16x16;
        topCBPLuma[mbX] = leftCBPLuma = cbpLuma;
        topCBPChroma[mbX] = leftCBPChroma = cbpChroma;
        tf8x8Left = tf8x8Top[mbX] = transform8x8Used;
        tr8x8Used[mbAddr] = transform8x8Used;
        predModeTop[mbX << 1] = predModeTop[(mbX << 1) + 1] = predModeLeft[0] = predModeLeft[1] = Direct;
    }

    private int readSubMBTypeP(BitReader reader) {
        if (!activePps.entropy_coding_mode_flag)
            return readUE(reader, "SUB: sub_mb_type");
        else
            return cabac.readSubMbTypeP(mDecoder);
    }

    private int readSubMBTypeB(BitReader reader) {
        if (!activePps.entropy_coding_mode_flag)
            return readUE(reader, "SUB: sub_mb_type");
        else
            return cabac.readSubMbTypeB(mDecoder);
    }

    private void savePrediction8x8(int mbX, int[][] x, int list) {
        copyVect(mvTopLeft[list], mvTop[list][(mbX << 2) + 3]);
        copyVect(mvLeft[list][0], x[3]);
        copyVect(mvLeft[list][1], x[7]);
        copyVect(mvLeft[list][2], x[11]);
        copyVect(mvLeft[list][3], x[15]);
        copyVect(mvTop[list][mbX << 2], x[12]);
        copyVect(mvTop[list][(mbX << 2) + 1], x[13]);
        copyVect(mvTop[list][(mbX << 2) + 2], x[14]);
        copyVect(mvTop[list][(mbX << 2) + 3], x[15]);
    }

    private void copyVect(int[] to, int[] from) {
        to[0] = from[0];
        to[1] = from[1];
        to[2] = from[2];
    }

    private void decodeSubMb8x8(BitReader reader, int subMbType, Picture[] references, int offX, int offY, int[][] x,
            int[] tl, int[] t0, int[] t1, int[] tr, int[] l0, int[] l1, boolean tlAvb, boolean tAvb, boolean trAvb,
            boolean lAvb, int[] x00, int[] x01, int[] x10, int[] x11, int refIdx, Picture mb, int off, int blk8x8X,
            int blk8x8Y, int mbX, MBType leftMBType, MBType topMBType, MBType curMBType, PartPred leftPred,
            PartPred topPred, PartPred partPred, int list) {

        x00[2] = x01[2] = x10[2] = x11[2] = refIdx;

        switch (subMbType) {
        case 3:
            decodeSub4x4(reader, references, offX, offY, tl, t0, t1, tr, l0, l1, tlAvb, tAvb, trAvb, lAvb, x00, x01,
                    x10, x11, refIdx, mb, off, blk8x8X, blk8x8Y, mbX, leftMBType, topMBType, curMBType, leftPred,
                    topPred, partPred, list);
            break;
        case 2:
            decodeSub4x8(reader, references, offX, offY, tl, t0, t1, tr, l0, tlAvb, tAvb, trAvb, lAvb, x00, x01, x10,
                    x11, refIdx, mb, off, blk8x8X, blk8x8Y, mbX, leftMBType, topMBType, curMBType, leftPred, topPred,
                    partPred, list);
            break;
        case 1:
            decodeSub8x4(reader, references, offX, offY, tl, t0, tr, l0, l1, tlAvb, tAvb, trAvb, lAvb, x00, x01, x10,
                    x11, refIdx, mb, off, blk8x8X, blk8x8Y, mbX, leftMBType, topMBType, curMBType, leftPred, topPred,
                    partPred, list);
            break;
        case 0:
            decodeSub8x8(reader, references, offX, offY, tl, t0, tr, l0, tlAvb, tAvb, trAvb, lAvb, x00, x01, x10, x11,
                    refIdx, mb, off, blk8x8X, blk8x8Y, mbX, leftMBType, topMBType, curMBType, leftPred, topPred,
                    partPred, list);
        }
    }

    private void decodeSub8x8(BitReader reader, Picture[] references, int offX, int offY, int[] tl, int[] t0, int[] tr,
            int[] l0, boolean tlAvb, boolean tAvb, boolean trAvb, boolean lAvb, int[] x00, int[] x01, int[] x10,
            int[] x11, int refIdx, Picture mb, int off, int blk8x8X, int blk8x8Y, int mbX, MBType leftMBType,
            MBType topMBType, MBType curMBType, PartPred leftPred, PartPred topPred, PartPred partPred, int list) {

        int mvdX = readMVD(reader, 0, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX, blk8x8X,
                blk8x8Y, 2, 2, list);
        int mvdY = readMVD(reader, 1, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX, blk8x8X,
                blk8x8Y, 2, 2, list);

        int mvpX = calcMVPredictionMedian(l0, t0, tr, tl, lAvb, tAvb, trAvb, tlAvb, refIdx, 0);
        int mvpY = calcMVPredictionMedian(l0, t0, tr, tl, lAvb, tAvb, trAvb, tlAvb, refIdx, 1);

        x00[0] = x01[0] = x10[0] = x11[0] = mvdX + mvpX;
        x00[1] = x01[1] = x10[1] = x11[1] = mvpY + mvdY;

        debugPrint("MVP: (" + mvpX + ", " + mvpY + "), MVD: (" + mvdX + ", " + mvdY + "), MV: (" + x00[0] + ","
                + x00[1] + "," + refIdx + ")");
        BlockInterpolator.getBlockLuma(references[refIdx], mb, off, offX + x00[0], offY + x00[1], 8, 8);
    }

    private void decodeSub8x4(BitReader reader, Picture[] references, int offX, int offY, int[] tl, int[] t0, int[] tr,
            int[] l0, int[] l1, boolean tlAvb, boolean tAvb, boolean trAvb, boolean lAvb, int[] x00, int[] x01,
            int[] x10, int[] x11, int refIdx, Picture mb, int off, int blk8x8X, int blk8x8Y, int mbX,
            MBType leftMBType, MBType topMBType, MBType curMBType, PartPred leftPred, PartPred topPred,
            PartPred partPred, int list) {

        int mvdX1 = readMVD(reader, 0, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX, blk8x8X,
                blk8x8Y, 2, 1, list);
        int mvdY1 = readMVD(reader, 1, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX, blk8x8X,
                blk8x8Y, 2, 1, list);

        int mvpX1 = calcMVPredictionMedian(l0, t0, tr, tl, lAvb, tAvb, trAvb, tlAvb, refIdx, 0);
        int mvpY1 = calcMVPredictionMedian(l0, t0, tr, tl, lAvb, tAvb, trAvb, tlAvb, refIdx, 1);

        x00[0] = x01[0] = mvdX1 + mvpX1;
        x00[1] = x01[1] = mvdY1 + mvpY1;

        debugPrint("MVP: (" + mvpX1 + ", " + mvpY1 + "), MVD: (" + mvdX1 + ", " + mvdY1 + "), MV: (" + x00[0] + ","
                + x00[1] + "," + refIdx + ")");

        int mvdX2 = readMVD(reader, 0, lAvb, true, leftMBType, curMBType, leftPred, partPred, partPred, mbX, blk8x8X,
                blk8x8Y + 1, 2, 1, list);
        int mvdY2 = readMVD(reader, 1, lAvb, true, leftMBType, curMBType, leftPred, partPred, partPred, mbX, blk8x8X,
                blk8x8Y + 1, 2, 1, list);

        int mvpX2 = calcMVPredictionMedian(l1, x00, NULL_VECTOR, l0, lAvb, true, false, lAvb, refIdx, 0);
        int mvpY2 = calcMVPredictionMedian(l1, x00, NULL_VECTOR, l0, lAvb, true, false, lAvb, refIdx, 1);

        x10[0] = x11[0] = mvdX2 + mvpX2;
        x10[1] = x11[1] = mvdY2 + mvpY2;

        debugPrint("MVP: (" + mvpX2 + ", " + mvpY2 + "), MVD: (" + mvdX2 + ", " + mvdY2 + "), MV: (" + x10[0] + ","
                + x10[1] + "," + refIdx + ")");

        BlockInterpolator.getBlockLuma(references[refIdx], mb, off, offX + x00[0], offY + x00[1], 8, 4);
        BlockInterpolator.getBlockLuma(references[refIdx], mb, off + mb.getWidth() * 4, offX + x10[0], offY + x10[1]
                + 16, 8, 4);
    }

    private void decodeSub4x8(BitReader reader, Picture[] references, int offX, int offY, int[] tl, int[] t0, int[] t1,
            int[] tr, int[] l0, boolean tlAvb, boolean tAvb, boolean trAvb, boolean lAvb, int[] x00, int[] x01,
            int[] x10, int[] x11, int refIdx, Picture mb, int off, int blk8x8X, int blk8x8Y, int mbX,
            MBType leftMBType, MBType topMBType, MBType curMBType, PartPred leftPred, PartPred topPred,
            PartPred partPred, int list) {

        int mvdX1 = readMVD(reader, 0, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX, blk8x8X,
                blk8x8Y, 1, 2, list);
        int mvdY1 = readMVD(reader, 1, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX, blk8x8X,
                blk8x8Y, 1, 2, list);

        int mvpX1 = calcMVPredictionMedian(l0, t0, t1, tl, lAvb, tAvb, tAvb, tlAvb, refIdx, 0);
        int mvpY1 = calcMVPredictionMedian(l0, t0, t1, tl, lAvb, tAvb, tAvb, tlAvb, refIdx, 1);

        x00[0] = x10[0] = mvdX1 + mvpX1;
        x00[1] = x10[1] = mvdY1 + mvpY1;

        debugPrint("MVP: (" + mvpX1 + ", " + mvpY1 + "), MVD: (" + mvdX1 + ", " + mvdY1 + "), MV: (" + x00[0] + ","
                + x00[1] + "," + refIdx + ")");

        int mvdX2 = readMVD(reader, 0, true, tAvb, curMBType, topMBType, partPred, topPred, partPred, mbX, blk8x8X + 1,
                blk8x8Y, 1, 2, list);
        int mvdY2 = readMVD(reader, 1, true, tAvb, curMBType, topMBType, partPred, topPred, partPred, mbX, blk8x8X + 1,
                blk8x8Y, 1, 2, list);

        int mvpX2 = calcMVPredictionMedian(x00, t1, tr, t0, true, tAvb, trAvb, tAvb, refIdx, 0);
        int mvpY2 = calcMVPredictionMedian(x00, t1, tr, t0, true, tAvb, trAvb, tAvb, refIdx, 1);

        x01[0] = x11[0] = mvdX2 + mvpX2;
        x01[1] = x11[1] = mvdY2 + mvpY2;

        debugPrint("MVP: (" + mvpX2 + ", " + mvpY2 + "), MVD: (" + mvdX2 + ", " + mvdY2 + "), MV: (" + x01[0] + ","
                + x01[1] + "," + refIdx + ")");

        BlockInterpolator.getBlockLuma(references[refIdx], mb, off, offX + x00[0], offY + x00[1], 4, 8);
        BlockInterpolator.getBlockLuma(references[refIdx], mb, off + 4, offX + x01[0] + 16, offY + x01[1], 4, 8);
    }

    private void decodeSub4x4(BitReader reader, Picture[] references, int offX, int offY, int[] tl, int[] t0, int[] t1,
            int[] tr, int[] l0, int[] l1, boolean tlAvb, boolean tAvb, boolean trAvb, boolean lAvb, int[] x00,
            int[] x01, int[] x10, int[] x11, int refIdx, Picture mb, int off, int blk8x8X, int blk8x8Y, int mbX,
            MBType leftMBType, MBType topMBType, MBType curMBType, PartPred leftPred, PartPred topPred,
            PartPred partPred, int list) {
        int mvdX1 = readMVD(reader, 0, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX, blk8x8X,
                blk8x8Y, 1, 1, list);
        int mvdY1 = readMVD(reader, 1, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX, blk8x8X,
                blk8x8Y, 1, 1, list);

        int mvpX1 = calcMVPredictionMedian(l0, t0, t1, tl, lAvb, tAvb, tAvb, tlAvb, refIdx, 0);
        int mvpY1 = calcMVPredictionMedian(l0, t0, t1, tl, lAvb, tAvb, tAvb, tlAvb, refIdx, 1);

        x00[0] = mvdX1 + mvpX1;
        x00[1] = mvdY1 + mvpY1;
        debugPrint("MVP: (" + mvpX1 + ", " + mvpY1 + "), MVD: (" + mvdX1 + ", " + mvdY1 + "), MV: (" + x00[0] + ","
                + x00[1] + "," + refIdx + ")");

        int mvdX2 = readMVD(reader, 0, true, tAvb, curMBType, topMBType, partPred, topPred, partPred, mbX, blk8x8X + 1,
                blk8x8Y, 1, 1, list);
        int mvdY2 = readMVD(reader, 1, true, tAvb, curMBType, topMBType, partPred, topPred, partPred, mbX, blk8x8X + 1,
                blk8x8Y, 1, 1, list);

        int mvpX2 = calcMVPredictionMedian(x00, t1, tr, t0, true, tAvb, trAvb, tAvb, refIdx, 0);
        int mvpY2 = calcMVPredictionMedian(x00, t1, tr, t0, true, tAvb, trAvb, tAvb, refIdx, 1);

        x01[0] = mvdX2 + mvpX2;
        x01[1] = mvdY2 + mvpY2;
        debugPrint("MVP: (" + mvpX2 + ", " + mvpY2 + "), MVD: (" + mvdX2 + ", " + mvdY2 + "), MV: (" + x01[0] + ","
                + x01[1] + "," + refIdx + ")");

        int mvdX3 = readMVD(reader, 0, lAvb, true, leftMBType, curMBType, leftPred, partPred, partPred, mbX, blk8x8X,
                blk8x8Y + 1, 1, 1, list);
        int mvdY3 = readMVD(reader, 1, lAvb, true, leftMBType, curMBType, leftPred, partPred, partPred, mbX, blk8x8X,
                blk8x8Y + 1, 1, 1, list);

        int mvpX3 = calcMVPredictionMedian(l1, x00, x01, l0, lAvb, true, true, lAvb, refIdx, 0);
        int mvpY3 = calcMVPredictionMedian(l1, x00, x01, l0, lAvb, true, true, lAvb, refIdx, 1);

        x10[0] = mvdX3 + mvpX3;
        x10[1] = mvdY3 + mvpY3;

        debugPrint("MVP: (" + mvpX3 + ", " + mvpY3 + "), MVD: (" + mvdX3 + ", " + mvdY3 + "), MV: (" + x10[0] + ","
                + x10[1] + "," + refIdx + ")");

        int mvdX4 = readMVD(reader, 0, true, true, curMBType, curMBType, partPred, partPred, partPred, mbX,
                blk8x8X + 1, blk8x8Y + 1, 1, 1, list);
        int mvdY4 = readMVD(reader, 1, true, true, curMBType, curMBType, partPred, partPred, partPred, mbX,
                blk8x8X + 1, blk8x8Y + 1, 1, 1, list);

        int mvpX4 = calcMVPredictionMedian(x10, x01, NULL_VECTOR, x00, true, true, false, true, refIdx, 0);
        int mvpY4 = calcMVPredictionMedian(x10, x01, NULL_VECTOR, x00, true, true, false, true, refIdx, 1);

        x11[0] = mvdX4 + mvpX4;
        x11[1] = mvdY4 + mvpY4;

        debugPrint("MVP: (" + mvpX4 + ", " + mvpY4 + "), MVD: (" + mvdX4 + ", " + mvdY4 + "), MV: (" + x11[0] + ","
                + x11[1] + "," + refIdx + ")");

        BlockInterpolator.getBlockLuma(references[refIdx], mb, off, offX + x00[0], offY + x00[1], 4, 4);
        BlockInterpolator.getBlockLuma(references[refIdx], mb, off + 4, offX + x01[0] + 16, offY + x01[1], 4, 4);
        BlockInterpolator.getBlockLuma(references[refIdx], mb, off + mb.getWidth() * 4, offX + x10[0], offY + x10[1]
                + 16, 4, 4);
        BlockInterpolator.getBlockLuma(references[refIdx], mb, off + mb.getWidth() * 4 + 4, offX + x11[0] + 16, offY
                + x11[1] + 16, 4, 4);
    }

    private int readMVD(BitReader reader, int comp, boolean leftAvailable, boolean topAvailable, MBType leftType,
            MBType topType, PartPred leftPred, PartPred topPred, PartPred curPred, int mbX, int partX, int partY,
            int partW, int partH, int list) {
        if (!activePps.entropy_coding_mode_flag)
            return readSE(reader, "mvd_l0_x");
        else
            return cabac.readMVD(mDecoder, comp, leftAvailable, topAvailable, leftType, topType, leftPred, topPred,
                    curPred, mbX, partX, partY, partW, partH, list);
    }

    public void decodeChromaInter(BitReader reader, int pattern, Frame[][] refs, int[][][] x, PartPred[] predType,
            boolean leftAvailable, boolean topAvailable, int mbX, int mbY, int mbAddr, int qp, Picture mb, Picture mb1) {

        predictChromaInter(refs, x, mbX << 3, mbY << 3, 1, mb1, predType);
        predictChromaInter(refs, x, mbX << 3, mbY << 3, 2, mb1, predType);

        int qp1 = calcQpChroma(qp, chromaQpOffset[0]);
        int qp2 = calcQpChroma(qp, chromaQpOffset[1]);

        decodeChromaResidual(reader, leftAvailable, topAvailable, mbX, mbY, pattern, mb, qp1, qp2, MBType.P_16x16);

        mbQps[1][mbAddr] = qp1;
        mbQps[2][mbAddr] = qp2;
        // throw new RuntimeException("Merge prediction and residual");
    }

    private void saveMvs(int[][][] x, int mbX, int mbY) {
        for (int j = 0, blkOffY = mbY << 2, blkInd = 0; j < 4; j++, blkOffY++) {
            for (int i = 0, blkOffX = mbX << 2; i < 4; i++, blkOffX++, blkInd++) {
                mvs[0][blkOffY][blkOffX] = x[0][blkInd];
                mvs[1][blkOffY][blkOffX] = x[1][blkInd];
            }
        }
    }

    public void predictChromaInter(Frame[][] refs, int[][][] vectors, int x, int y, int comp, Picture mb,
            PartPred[] predType) {

        Picture[] mbb = { Picture.create(16, 16, chromaFormat), Picture.create(16, 16, chromaFormat) };

        for (int blk8x8 = 0; blk8x8 < 4; blk8x8++) {
            for (int list = 0; list < 2; list++) {
                if (!predType[blk8x8].usesList(list))
                    continue;
                for (int blk4x4 = 0; blk4x4 < 4; blk4x4++) {
                    int i = BLK_INV_MAP[(blk8x8 << 2) + blk4x4];
                    int[] mv = vectors[list][i];
                    Picture ref = refs[list][mv[2]];

                    int blkPox = (i & 3) << 1;
                    int blkPoy = (i >> 2) << 1;

                    int xx = ((x + blkPox) << 3) + mv[0];
                    int yy = ((y + blkPoy) << 3) + mv[1];

                    BlockInterpolator.getBlockChroma(ref.getPlaneData(comp), ref.getPlaneWidth(comp),
                            ref.getPlaneHeight(comp), mbb[list].getPlaneData(comp), blkPoy * mb.getPlaneWidth(comp)
                                    + blkPox, mb.getPlaneWidth(comp), xx, yy, 2, 2);
                }
            }

            int blk4x4 = BLK8x8_BLOCKS[blk8x8][0];
            prediction.mergePrediction(vectors[0][blk4x4][2], vectors[1][blk4x4][2], predType[blk8x8], comp,
                    mbb[0].getPlaneData(comp), mbb[1].getPlaneData(comp), BLK_8x8_MB_OFF_CHROMA[blk8x8],
                    mb.getPlaneWidth(comp), 4, 4, mb.getPlaneData(comp), refs, thisFrame);
        }
    }

    public void decodeMBlockIPCM(BitReader reader, int mbIndex, Picture mb) {
        int mbX = mapper.getMbX(mbIndex);

        reader.align();

        int[] samplesLuma = new int[256];
        for (int i = 0; i < 256; i++) {
            samplesLuma[i] = reader.readNBit(8);
        }
        int MbWidthC = 16 >> chromaFormat.compWidth[1];
        int MbHeightC = 16 >> chromaFormat.compHeight[1];

        int[] samplesChroma = new int[2 * MbWidthC * MbHeightC];
        for (int i = 0; i < 2 * MbWidthC * MbHeightC; i++) {
            samplesChroma[i] = reader.readNBit(8);
        }
        collectPredictors(mb, mbX);
    }

    public void decodeSkip(Frame[][] refs, int mbIdx, Picture mb, SliceType sliceType) {
        int mbX = mapper.getMbX(mbIdx);
        int mbY = mapper.getMbY(mbIdx);
        int mbAddr = mapper.getAddress(mbIdx);

        int[][][] x = new int[2][16][3];
        PartPred[] pp = new PartPred[4];

        for (int i = 0; i < 16; i++)
            x[0][i][2] = x[1][i][2] = -1;

        if (sliceType == P) {
            predictPSkip(refs, mbX, mbY, mapper.leftAvailable(mbIdx), mapper.topAvailable(mbIdx),
                    mapper.topLeftAvailable(mbIdx), mapper.topRightAvailable(mbIdx), x, mb);
            Arrays.fill(pp, PartPred.L0);
        } else {
            predictBDirect(refs, mbX, mbY, mapper.leftAvailable(mbIdx), mapper.topAvailable(mbIdx),
                    mapper.topLeftAvailable(mbIdx), mapper.topRightAvailable(mbIdx), x, pp, mb, identityMapping4);
            savePrediction8x8(mbX, x[0], 0);
            savePrediction8x8(mbX, x[1], 1);
        }

        decodeChromaSkip(refs, x, pp, mbX, mbY, mb);

        collectPredictors(mb, mbX);

        saveMvs(x, mbX, mbY);
        mbTypes[mbAddr] = topMBType[mbX] = leftMBType = null;
        mbQps[0][mbAddr] = qp;
        mbQps[1][mbAddr] = calcQpChroma(qp, chromaQpOffset[0]);
        mbQps[2][mbAddr] = calcQpChroma(qp, chromaQpOffset[1]);
    }

    public void predictBDirect(Frame[][] refs, int mbX, int mbY, boolean lAvb, boolean tAvb, boolean tlAvb,
            boolean trAvb, int[][][] x, PartPred[] pp, Picture mb, int[] blocks) {
        if (sh.direct_spatial_mv_pred_flag)
            predictBSpatialDirect(refs, mbX, mbY, lAvb, tAvb, tlAvb, trAvb, x, pp, mb, blocks);
        else
            predictBTemporalDirect(refs, mbX, mbY, lAvb, tAvb, tlAvb, trAvb, x, pp, mb, blocks);
    }

    private void predictBTemporalDirect(Frame[][] refs, int mbX, int mbY, boolean lAvb, boolean tAvb, boolean tlAvb,
            boolean trAvb, int[][][] x, PartPred[] pp, Picture mb, int[] blocks8x8) {

        Picture mb0 = Picture.create(16, 16, chromaFormat), mb1 = Picture.create(16, 16, chromaFormat);
        for (int blk8x8 : blocks8x8) {
            int blk4x4_0 = H264Const.BLK8x8_BLOCKS[blk8x8][0];
            pp[blk8x8] = Bi;

            if (!activeSps.direct_8x8_inference_flag)
                for (int blk4x4 : BLK8x8_BLOCKS[blk8x8]) {
                    predTemp4x4(refs, mbX, mbY, x, blk4x4);

                    int blkIndX = blk4x4 & 3;
                    int blkIndY = blk4x4 >> 2;

                    debugPrint("DIRECT_4x4 [" + blkIndY + ", " + blkIndX + "]: (" + x[0][blk4x4][0] + ","
                            + x[0][blk4x4][1] + "," + x[0][blk4x4][2] + "), (" + x[1][blk4x4][0] + ","
                            + x[1][blk4x4][1] + "," + x[1][blk4x4][2] + ")");

                    int blkPredX = (mbX << 6) + (blkIndX << 4);
                    int blkPredY = (mbY << 6) + (blkIndY << 4);

                    BlockInterpolator.getBlockLuma(refs[0][x[0][blk4x4][2]], mb0, BLK_4x4_MB_OFF_LUMA[blk4x4], blkPredX
                            + x[0][blk4x4][0], blkPredY + x[0][blk4x4][1], 4, 4);
                    BlockInterpolator.getBlockLuma(refs[1][0], mb1, BLK_4x4_MB_OFF_LUMA[blk4x4], blkPredX
                            + x[1][blk4x4][0], blkPredY + x[1][blk4x4][1], 4, 4);
                }
            else {
                int blk4x4Pred = BLK_INV_MAP[blk8x8 * 5];
                predTemp4x4(refs, mbX, mbY, x, blk4x4Pred);
                propagatePred(x, blk8x8, blk4x4Pred);

                int blkIndX = blk4x4_0 & 3;
                int blkIndY = blk4x4_0 >> 2;

                debugPrint("DIRECT_8x8 [" + blkIndY + ", " + blkIndX + "]: (" + x[0][blk4x4_0][0] + ","
                        + x[0][blk4x4_0][1] + "," + x[0][blk4x4_0][2] + "), (" + x[1][blk4x4_0][0] + ","
                        + x[1][blk4x4_0][1] + "," + x[0][blk4x4_0][2] + ")");

                int blkPredX = (mbX << 6) + (blkIndX << 4);
                int blkPredY = (mbY << 6) + (blkIndY << 4);

                BlockInterpolator.getBlockLuma(refs[0][x[0][blk4x4_0][2]], mb0, BLK_4x4_MB_OFF_LUMA[blk4x4_0], blkPredX
                        + x[0][blk4x4_0][0], blkPredY + x[0][blk4x4_0][1], 8, 8);
                BlockInterpolator.getBlockLuma(refs[1][0], mb1, BLK_4x4_MB_OFF_LUMA[blk4x4_0], blkPredX
                        + x[1][blk4x4_0][0], blkPredY + x[1][blk4x4_0][1], 8, 8);
            }
            prediction.mergePrediction(x[0][blk4x4_0][2], x[1][blk4x4_0][2], Bi, 0, mb0.getPlaneData(0),
                    mb1.getPlaneData(0), BLK_4x4_MB_OFF_LUMA[blk4x4_0], 16, 8, 8, mb.getPlaneData(0), refs, thisFrame);
        }
    }

    private void predTemp4x4(Frame[][] refs, int mbX, int mbY, int[][][] x, int blk4x4) {
        int mbWidth = activeSps.pic_width_in_mbs_minus1 + 1;

        Frame picCol = refs[1][0];
        int blkIndX = blk4x4 & 3;
        int blkIndY = blk4x4 >> 2;

        int blkPosX = (mbX << 2) + blkIndX;
        int blkPosY = (mbY << 2) + blkIndY;

        int[] mvCol = picCol.getMvs()[0][blkPosY][blkPosX];
        Frame refL0;
        int refIdxL0;
        if (mvCol[2] == -1) {
            mvCol = picCol.getMvs()[1][blkPosY][blkPosX];
            if (mvCol[2] == -1) {
                refIdxL0 = 0;
                refL0 = refs[0][0];
            } else {
                refL0 = picCol.getRefsUsed()[mbY * mbWidth + mbX][1][mvCol[2]];
                refIdxL0 = findPic(refs[0], refL0);
            }
        } else {
            refL0 = picCol.getRefsUsed()[mbY * mbWidth + mbX][0][mvCol[2]];
            refIdxL0 = findPic(refs[0], refL0);
        }

        x[0][blk4x4][2] = refIdxL0;
        x[1][blk4x4][2] = 0;

        int td = MathUtil.clip(picCol.getPOC() - refL0.getPOC(), -128, 127);
        if (!refL0.isShortTerm() || td == 0) {
            x[0][blk4x4][0] = mvCol[0];
            x[0][blk4x4][1] = mvCol[1];
            x[1][blk4x4][0] = 0;
            x[1][blk4x4][1] = 0;
        } else {
            int tb = MathUtil.clip(thisFrame.getPOC() - refL0.getPOC(), -128, 127);
            int tx = (16384 + Math.abs(td / 2)) / td;
            int dsf = clip((tb * tx + 32) >> 6, -1024, 1023);

            x[0][blk4x4][0] = (dsf * mvCol[0] + 128) >> 8;
            x[0][blk4x4][1] = (dsf * mvCol[1] + 128) >> 8;
            x[1][blk4x4][0] = (x[0][blk4x4][0] - mvCol[0]);
            x[1][blk4x4][1] = (x[0][blk4x4][1] - mvCol[1]);
        }
    }

    private int findPic(Frame[] frames, Frame refL0) {
        for (int i = 0; i < frames.length; i++)
            if (frames[i] == refL0)
                return i;
        System.out.println("ERROR: RefPicList0 shall contain refPicCol");
        return 0;
    }

    private void predictBSpatialDirect(Frame[][] refs, int mbX, int mbY, boolean lAvb, boolean tAvb, boolean tlAvb,
            boolean trAvb, int[][][] x, PartPred[] pp, Picture mb, int[] blocks8x8) {

        int[] a0 = mvLeft[0][0], a1 = mvLeft[1][0];
        int[] b0 = mvTop[0][mbX << 2], b1 = mvTop[1][mbX << 2];
        int[] c0 = mvTop[0][(mbX << 2) + 4], c1 = mvTop[1][(mbX << 2) + 4];
        int[] d0 = mvTopLeft[0];
        int[] d1 = mvTopLeft[1];

        int refIdxL0 = calcRef(a0, b0, c0, d0, lAvb, tAvb, tlAvb, trAvb, mbX);
        int refIdxL1 = calcRef(a1, b1, c1, d1, lAvb, tAvb, tlAvb, trAvb, mbX);

        Picture mb0 = Picture.create(16, 16, chromaFormat), mb1 = Picture.create(16, 16, chromaFormat);

        if (refIdxL0 < 0 && refIdxL1 < 0) {
            for (int blk8x8 : blocks8x8) {
                for (int blk4x4 : BLK8x8_BLOCKS[blk8x8]) {
                    x[0][blk4x4][0] = x[0][blk4x4][1] = x[0][blk4x4][2] = x[1][blk4x4][0] = x[1][blk4x4][1] = x[1][blk4x4][2] = 0;
                }
                pp[blk8x8] = Bi;

                int blkOffX = (blk8x8 & 1) << 5;
                int blkOffY = (blk8x8 >> 1) << 5;
                BlockInterpolator.getBlockLuma(refs[0][0], mb0, BLK_8x8_MB_OFF_LUMA[blk8x8], (mbX << 6) + blkOffX,
                        (mbY << 6) + blkOffY, 8, 8);
                BlockInterpolator.getBlockLuma(refs[1][0], mb1, BLK_8x8_MB_OFF_LUMA[blk8x8], (mbX << 6) + blkOffX,
                        (mbY << 6) + blkOffY, 8, 8);
                prediction.mergePrediction(0, 0, PartPred.Bi, 0, mb0.getPlaneData(0), mb1.getPlaneData(0),
                        BLK_8x8_MB_OFF_LUMA[blk8x8], 16, 8, 8, mb.getPlaneData(0), refs, thisFrame);
                debugPrint("DIRECT_8x8 [" + (blk8x8 & 2) + ", " + ((blk8x8 << 1) & 2) + "]: (0,0,0), (0,0,0)");
            }
            return;
        }
        int mvX0 = calcMVPredictionMedian(a0, b0, c0, d0, lAvb, tAvb, trAvb, tlAvb, refIdxL0, 0);
        int mvY0 = calcMVPredictionMedian(a0, b0, c0, d0, lAvb, tAvb, trAvb, tlAvb, refIdxL0, 1);
        int mvX1 = calcMVPredictionMedian(a1, b1, c1, d1, lAvb, tAvb, trAvb, tlAvb, refIdxL1, 0);
        int mvY1 = calcMVPredictionMedian(a1, b1, c1, d1, lAvb, tAvb, trAvb, tlAvb, refIdxL1, 1);

        Frame col = refs[1][0];
        PartPred partPred = refIdxL0 >= 0 && refIdxL1 >= 0 ? Bi : (refIdxL0 >= 0 ? L0 : L1);
        for (int blk8x8 : blocks8x8) {
            int blk4x4_0 = H264Const.BLK8x8_BLOCKS[blk8x8][0];

            if (!activeSps.direct_8x8_inference_flag)
                for (int blk4x4 : BLK8x8_BLOCKS[blk8x8]) {
                    pred4x4(mbX, mbY, x, pp, refIdxL0, refIdxL1, mvX0, mvY0, mvX1, mvY1, col, partPred, blk4x4);

                    int blkIndX = blk4x4 & 3;
                    int blkIndY = blk4x4 >> 2;

                    debugPrint("DIRECT_4x4 [" + blkIndY + ", " + blkIndX + "]: (" + x[0][blk4x4][0] + ","
                            + x[0][blk4x4][1] + "," + refIdxL0 + "), (" + x[1][blk4x4][0] + "," + x[1][blk4x4][1] + ","
                            + refIdxL1 + ")");

                    int blkPredX = (mbX << 6) + (blkIndX << 4);
                    int blkPredY = (mbY << 6) + (blkIndY << 4);

                    if (refIdxL0 >= 0)
                        BlockInterpolator.getBlockLuma(refs[0][refIdxL0], mb0, BLK_4x4_MB_OFF_LUMA[blk4x4], blkPredX
                                + x[0][blk4x4][0], blkPredY + x[0][blk4x4][1], 4, 4);
                    if (refIdxL1 >= 0)
                        BlockInterpolator.getBlockLuma(refs[1][refIdxL1], mb1, BLK_4x4_MB_OFF_LUMA[blk4x4], blkPredX
                                + x[1][blk4x4][0], blkPredY + x[1][blk4x4][1], 4, 4);
                }
            else {
                int blk4x4Pred = BLK_INV_MAP[blk8x8 * 5];
                pred4x4(mbX, mbY, x, pp, refIdxL0, refIdxL1, mvX0, mvY0, mvX1, mvY1, col, partPred, blk4x4Pred);
                propagatePred(x, blk8x8, blk4x4Pred);

                int blkIndX = blk4x4_0 & 3;
                int blkIndY = blk4x4_0 >> 2;

                debugPrint("DIRECT_8x8 [" + blkIndY + ", " + blkIndX + "]: (" + x[0][blk4x4_0][0] + ","
                        + x[0][blk4x4_0][1] + "," + refIdxL0 + "), (" + x[1][blk4x4_0][0] + "," + x[1][blk4x4_0][1]
                        + "," + refIdxL1 + ")");

                int blkPredX = (mbX << 6) + (blkIndX << 4);
                int blkPredY = (mbY << 6) + (blkIndY << 4);

                if (refIdxL0 >= 0)
                    BlockInterpolator.getBlockLuma(refs[0][refIdxL0], mb0, BLK_4x4_MB_OFF_LUMA[blk4x4_0], blkPredX
                            + x[0][blk4x4_0][0], blkPredY + x[0][blk4x4_0][1], 8, 8);
                if (refIdxL1 >= 0)
                    BlockInterpolator.getBlockLuma(refs[1][refIdxL1], mb1, BLK_4x4_MB_OFF_LUMA[blk4x4_0], blkPredX
                            + x[1][blk4x4_0][0], blkPredY + x[1][blk4x4_0][1], 8, 8);
            }
            prediction.mergePrediction(x[0][blk4x4_0][2], x[1][blk4x4_0][2], refIdxL0 >= 0 ? (refIdxL1 >= 0 ? Bi : L0)
                    : L1, 0, mb0.getPlaneData(0), mb1.getPlaneData(0), BLK_4x4_MB_OFF_LUMA[blk4x4_0], 16, 8, 8, mb
                    .getPlaneData(0), refs, thisFrame);
        }
    }

    private int calcRef(int[] a0, int[] b0, int[] c0, int[] d0, boolean lAvb, boolean tAvb, boolean tlAvb,
            boolean trAvb, int mbX) {
        return minPos(minPos(lAvb ? a0[2] : -1, tAvb ? b0[2] : -1), trAvb ? c0[2] : (tlAvb ? d0[2] : -1));
    }

    private void propagatePred(int[][][] x, int blk8x8, int blk4x4Pred) {
        int b0 = BLK8x8_BLOCKS[blk8x8][0];
        int b1 = BLK8x8_BLOCKS[blk8x8][1];
        int b2 = BLK8x8_BLOCKS[blk8x8][2];
        int b3 = BLK8x8_BLOCKS[blk8x8][3];
        x[0][b0][0] = x[0][b1][0] = x[0][b2][0] = x[0][b3][0] = x[0][blk4x4Pred][0];
        x[0][b0][1] = x[0][b1][1] = x[0][b2][1] = x[0][b3][1] = x[0][blk4x4Pred][1];
        x[0][b0][2] = x[0][b1][2] = x[0][b2][2] = x[0][b3][2] = x[0][blk4x4Pred][2];
        x[1][b0][0] = x[1][b1][0] = x[1][b2][0] = x[1][b3][0] = x[1][blk4x4Pred][0];
        x[1][b0][1] = x[1][b1][1] = x[1][b2][1] = x[1][b3][1] = x[1][blk4x4Pred][1];
        x[1][b0][2] = x[1][b1][2] = x[1][b2][2] = x[1][b3][2] = x[1][blk4x4Pred][2];
    }

    private void pred4x4(int mbX, int mbY, int[][][] x, PartPred[] pp, int refL0, int refL1, int mvX0, int mvY0,
            int mvX1, int mvY1, Frame col, PartPred partPred, int blk4x4) {
        int blkIndX = blk4x4 & 3;
        int blkIndY = blk4x4 >> 2;

        int blkPosX = (mbX << 2) + blkIndX;
        int blkPosY = (mbY << 2) + blkIndY;

        x[0][blk4x4][2] = refL0;
        x[1][blk4x4][2] = refL1;

        int[] mvCol = col.getMvs()[0][blkPosY][blkPosX];
        if (mvCol[2] == -1)
            mvCol = col.getMvs()[1][blkPosY][blkPosX];

        boolean colZero = col.isShortTerm() && mvCol[2] == 0 && (abs(mvCol[0]) >> 1) == 0 && (abs(mvCol[1]) >> 1) == 0;

        if (refL0 > 0 || !colZero) {
            x[0][blk4x4][0] = mvX0;
            x[0][blk4x4][1] = mvY0;
        }
        if (refL1 > 0 || !colZero) {
            x[1][blk4x4][0] = mvX1;
            x[1][blk4x4][1] = mvY1;
        }

        pp[BLK_8x8_IND[blk4x4]] = partPred;
    }

    private int minPos(int a, int b) {
        return a >= 0 && b >= 0 ? Math.min(a, b) : Math.max(a, b);
    }

    public void predictPSkip(Frame[][] refs, int mbX, int mbY, boolean lAvb, boolean tAvb, boolean tlAvb,
            boolean trAvb, int[][][] x, Picture mb) {
        int mvX = 0, mvY = 0;
        if (lAvb && tAvb) {
            int[] b = mvTop[0][mbX << 2];
            int[] a = mvLeft[0][0];

            if ((a[0] != 0 || a[1] != 0 || a[2] != 0) && (b[0] != 0 || b[1] != 0 || b[2] != 0)) {
                mvX = calcMVPredictionMedian(a, b, mvTop[0][(mbX << 2) + 4], mvTopLeft[0], lAvb, tAvb, trAvb, tlAvb, 0,
                        0);
                mvY = calcMVPredictionMedian(a, b, mvTop[0][(mbX << 2) + 4], mvTopLeft[0], lAvb, tAvb, trAvb, tlAvb, 0,
                        1);
            }
        }
        debugPrint("MV_SKIP: (" + mvX + "," + mvY + ")");
        int blk8x8X = mbX << 1;
        predModeLeft[0] = predModeLeft[1] = predModeTop[blk8x8X] = predModeTop[blk8x8X + 1] = L0;

        int xx = mbX << 2;
        copyVect(mvTopLeft[0], mvTop[0][xx + 3]);
        saveVect(mvTop[0], xx, xx + 4, mvX, mvY, 0);
        saveVect(mvLeft[0], 0, 4, mvX, mvY, 0);

        for (int i = 0; i < 16; i++) {
            x[0][i][0] = mvX;
            x[0][i][1] = mvY;
            x[0][i][2] = 0;
        }
        BlockInterpolator.getBlockLuma(refs[0][0], mb, 0, (mbX << 6) + mvX, (mbY << 6) + mvY, 16, 16);

        prediction.mergePrediction(0, 0, L0, 0, mb.getPlaneData(0), null, 0, 16, 16, 16, mb.getPlaneData(0), refs,
                thisFrame);
    }

    public void decodeChromaSkip(Frame[][] reference, int[][][] vectors, PartPred[] pp, int mbX, int mbY, Picture mb) {
        predictChromaInter(reference, vectors, mbX << 3, mbY << 3, 1, mb, pp);
        predictChromaInter(reference, vectors, mbX << 3, mbY << 3, 2, mb, pp);
    }

    private void debugPrint(String str) {
        if (debug)
            System.out.println(str);
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}