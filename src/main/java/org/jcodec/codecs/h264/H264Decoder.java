package org.jcodec.codecs.h264;

import static org.jcodec.codecs.h264.H264Utils.getPicHeightInMbs;
import static org.jcodec.codecs.h264.H264Utils.unescapeNAL;
import static org.jcodec.common.tools.MathUtil.wrap;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.codecs.h264.decode.SliceDecoder;
import org.jcodec.codecs.h264.decode.SliceHeaderReader;
import org.jcodec.codecs.h264.decode.deblock.DeblockingFilter;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.RefPicMarking;
import org.jcodec.codecs.h264.io.model.RefPicMarkingIDR;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.common.IntObjectMap;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Rect;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * MPEG 4 AVC ( H.264 ) Decoder
 * 
 * Conforms to H.264 ( ISO/IEC 14496-10 ) specifications
 * 
 * @author The JCodec project
 * 
 */
public class H264Decoder implements VideoDecoder {

    private IntObjectMap<SeqParameterSet> sps = new IntObjectMap<SeqParameterSet>();
    private IntObjectMap<PictureParameterSet> pps = new IntObjectMap<PictureParameterSet>();
    private Frame[] sRefs;
    private IntObjectMap<Frame> lRefs;
    private List<Frame> pictureBuffer;
    private POCManager poc;
    private boolean debug;

    public H264Decoder() {
        pictureBuffer = new ArrayList<Frame>();
        poc = new POCManager();
    }

    @Override
    public Frame decodeFrame(ByteBuffer data, int[][] buffer) {
        return new FrameDecoder().decodeFrame(H264Utils.splitFrame(data), buffer);
    }

    public Frame decodeFrame(List<ByteBuffer> nalUnits, int[][] buffer) {
        return new FrameDecoder().decodeFrame(nalUnits, buffer);
    }

    class FrameDecoder {
        private SliceHeaderReader shr;
        private PictureParameterSet activePps;
        private SeqParameterSet activeSps;
        private DeblockingFilter filter;
        private SliceHeader firstSliceHeader;
        private NALUnit firstNu;
        private SliceDecoder decoder;
        private int[][][][] mvs;

        public Frame decodeFrame(List<ByteBuffer> nalUnits, int[][] buffer) {
            Frame result = null;

            for (ByteBuffer nalUnit : nalUnits) {
                NALUnit marker = NALUnit.read(nalUnit);

                unescapeNAL(nalUnit);

                switch (marker.type) {
                case NON_IDR_SLICE:
                case IDR_SLICE:
                    if (result == null)
                        result = init(buffer, nalUnit, marker);
                    decoder.decode(nalUnit, marker);
                    break;
                case SPS:
                    SeqParameterSet _sps = SeqParameterSet.read(nalUnit);
                    sps.put(_sps.seq_parameter_set_id, _sps);
                    break;
                case PPS:
                    PictureParameterSet _pps = PictureParameterSet.read(nalUnit);
                    pps.put(_pps.pic_parameter_set_id, _pps);
                    break;
                default:
                }
            }

            filter.deblockFrame(result);

            updateReferences(result);

            return result;
        }

        private void updateReferences(Frame picture) {
            if (firstNu.nal_ref_idc != 0) {
                if (firstNu.type == NALUnitType.IDR_SLICE) {
                    performIDRMarking(firstSliceHeader.refPicMarkingIDR, picture);
                } else {
                    performMarking(firstSliceHeader.refPicMarkingNonIDR, picture);
                }
            }
        }

        private Frame init(int[][] buffer, ByteBuffer segment, NALUnit marker) {
            firstNu = marker;

            shr = new SliceHeaderReader();
            BitReader br = new BitReader(segment.duplicate());
            firstSliceHeader = shr.readPart1(br);
            activePps = pps.get(firstSliceHeader.pic_parameter_set_id);
            activeSps = sps.get(activePps.seq_parameter_set_id);
            shr.readPart2(firstSliceHeader, marker, activeSps, activePps, br);
            int picWidthInMbs = activeSps.pic_width_in_mbs_minus1 + 1;
            int picHeightInMbs = getPicHeightInMbs(activeSps);

            int[][] nCoeff = new int[picHeightInMbs << 2][picWidthInMbs << 2];
            mvs = new int[2][picHeightInMbs << 2][picWidthInMbs << 2][3];
            MBType[] mbTypes = new MBType[picHeightInMbs * picWidthInMbs];
            boolean[] tr8x8Used = new boolean[picHeightInMbs * picWidthInMbs];
            int[][] mbQps = new int[3][picHeightInMbs * picWidthInMbs];
            SliceHeader[] shs = new SliceHeader[picHeightInMbs * picWidthInMbs];
            Frame[][][] refsUsed = new Frame[picHeightInMbs * picWidthInMbs][][];

            if (sRefs == null) {
                sRefs = new Frame[1 << (firstSliceHeader.sps.log2_max_frame_num_minus4 + 4)];
                lRefs = new IntObjectMap<Frame>();
            }

            Frame result = createFrame(activeSps, buffer, firstSliceHeader.frame_num, mvs, refsUsed,
                    poc.calcPOC(firstSliceHeader, firstNu));

            decoder = new SliceDecoder(activeSps, activePps, nCoeff, mvs, mbTypes, mbQps, shs, tr8x8Used, refsUsed,
                    result, sRefs, lRefs);
            decoder.setDebug(debug);

            filter = new DeblockingFilter(picWidthInMbs, activeSps.bit_depth_chroma_minus8 + 8, nCoeff, mvs, mbTypes,
                    mbQps, shs, tr8x8Used, refsUsed);

            return result;
        }

        public void performIDRMarking(RefPicMarkingIDR refPicMarkingIDR, Frame picture) {
            clearAll();
            pictureBuffer.clear();

            Frame saved = saveRef(picture);
            if (refPicMarkingIDR.isUseForlongTerm()) {
                lRefs.put(0, saved);
                saved.setShortTerm(false);
            } else
                sRefs[firstSliceHeader.frame_num] = saved;
        }

        private Frame saveRef(Frame decoded) {
            Frame frame = pictureBuffer.size() > 0 ? pictureBuffer.remove(0) : Frame.createFrame(decoded);
            frame.copyFrom(decoded);
            return frame;
        }

        private void releaseRef(Frame picture) {
            if (picture != null) {
                pictureBuffer.add(picture);
            }
        }

        public void clearAll() {
            for (int i = 0; i < sRefs.length; i++) {
                releaseRef(sRefs[i]);
                sRefs[i] = null;
            }
            int[] keys = lRefs.keys();
            for (int key : keys) {
                releaseRef(lRefs.get(key));
            }
            lRefs.clear();
        }

        public void performMarking(RefPicMarking refPicMarking, Frame picture) {
            Frame saved = saveRef(picture);

            if (refPicMarking != null) {
                for (RefPicMarking.Instruction instr : refPicMarking.getInstructions()) {
                    switch (instr.getType()) {
                    case REMOVE_SHORT:
                        unrefShortTerm(instr.getArg1());
                        break;
                    case REMOVE_LONG:
                        unrefLongTerm(instr.getArg1());
                        break;
                    case CONVERT_INTO_LONG:
                        convert(instr.getArg1(), instr.getArg2());
                        break;
                    case TRUNK_LONG:
                        truncateLongTerm(instr.getArg1() - 1);
                        break;
                    case CLEAR:
                        clearAll();
                        break;
                    case MARK_LONG:
                        saveLong(saved, instr.getArg1());
                        saved = null;
                    }
                }
            }
            if (saved != null)
                saveShort(saved);

            int maxFrames = 1 << (activeSps.log2_max_frame_num_minus4 + 4);
            if (refPicMarking == null) {
                int maxShort = Math.max(1, activeSps.num_ref_frames - lRefs.size());
                int min = Integer.MAX_VALUE, num = 0, minFn = 0;
                for (Frame frame : sRefs) {
                    if (frame != null) {
                        int fnWrap = unwrap(firstSliceHeader.frame_num, frame.getFrameNo(), maxFrames);
                        if (fnWrap < min) {
                            min = fnWrap;
                            minFn = frame.getFrameNo();
                        }
                        num++;
                    }
                }
                if (num > maxShort) {
                    // System.out.println("Removing: " + minFn + ", POC: " +
                    // sRefs[minFn].getPOC());
                    releaseRef(sRefs[minFn]);
                    sRefs[minFn] = null;
                }
            }
        }

        private int unwrap(int thisFrameNo, int refFrameNo, int maxFrames) {
            return refFrameNo > thisFrameNo ? refFrameNo - maxFrames : refFrameNo;
        }

        private void saveShort(Frame saved) {
            sRefs[firstSliceHeader.frame_num] = saved;
        }

        private void saveLong(Frame saved, int longNo) {
            Frame prev = lRefs.get(longNo);
            if (prev != null)
                releaseRef(prev);
            saved.setShortTerm(false);

            lRefs.put(longNo, saved);
        }

        private void truncateLongTerm(int maxLongNo) {
            int[] keys = lRefs.keys();
            for (int key : keys) {
                if (key > maxLongNo) {
                    releaseRef(lRefs.get(key));
                    lRefs.remove(key);
                }
            }
        }

        private void convert(int shortNo, int longNo) {
            int ind = wrap(firstSliceHeader.frame_num - shortNo,
                    1 << (firstSliceHeader.sps.log2_max_frame_num_minus4 + 4));
            releaseRef(lRefs.get(longNo));
            lRefs.put(longNo, sRefs[ind]);
            sRefs[ind] = null;
            lRefs.get(longNo).setShortTerm(false);
        }

        private void unrefLongTerm(int longNo) {
            releaseRef(lRefs.get(longNo));
            lRefs.remove(longNo);
        }

        private void unrefShortTerm(int shortNo) {
            int ind = wrap(firstSliceHeader.frame_num - shortNo,
                    1 << (firstSliceHeader.sps.log2_max_frame_num_minus4 + 4));
            releaseRef(sRefs[ind]);
            sRefs[ind] = null;
        }
    }

    public static Frame createFrame(SeqParameterSet sps, int[][] buffer, int frame_num, int[][][][] mvs,
            Frame[][][] refsUsed, int POC) {
        int width = sps.pic_width_in_mbs_minus1 + 1 << 4;
        int height = getPicHeightInMbs(sps) << 4;

        Rect crop = null;
        if (sps.frame_cropping_flag) {
            int sX = sps.frame_crop_left_offset << 1;
            int sY = sps.frame_crop_top_offset << 1;
            int w = width - (sps.frame_crop_right_offset << 1) - sX;
            int h = height - (sps.frame_crop_bottom_offset << 1) - sY;
            crop = new Rect(sX, sY, w, h);
        }
        return new Frame(width, height, buffer, ColorSpace.YUV420, crop, frame_num, mvs, refsUsed, POC);
    }

    public void addSps(List<ByteBuffer> spsList) {
        for (ByteBuffer byteBuffer : spsList) {
            ByteBuffer dup = byteBuffer.duplicate();
            unescapeNAL(dup);
            SeqParameterSet s = SeqParameterSet.read(dup);
            sps.put(s.seq_parameter_set_id, s);
        }
    }

    public void addPps(List<ByteBuffer> ppsList) {
        for (ByteBuffer byteBuffer : ppsList) {
            ByteBuffer dup = byteBuffer.duplicate();
            unescapeNAL(dup);
            PictureParameterSet p = PictureParameterSet.read(dup);
            pps.put(p.pic_parameter_set_id, p);
        }
    }

    @Override
    public int probe(ByteBuffer data) {
        boolean validSps = false, validPps = false, validSh = false;
        for (ByteBuffer nalUnit : H264Utils.splitFrame(data.duplicate())) {
            NALUnit marker = NALUnit.read(nalUnit);
            if (marker.type == NALUnitType.IDR_SLICE || marker.type == NALUnitType.NON_IDR_SLICE) {
                BitReader reader = new BitReader(nalUnit);
                validSh = validSh(new SliceHeaderReader().readPart1(reader));
                break;
            } else if (marker.type == NALUnitType.SPS) {
                validSps = validSps(SeqParameterSet.read(nalUnit));
            } else if (marker.type == NALUnitType.PPS) {
                validPps = validPps(PictureParameterSet.read(nalUnit));
            }
        }

        return (validSh ? 60 : 0) + (validSps ? 20 : 0) + (validPps ? 20 : 0);
    }

    private boolean validSh(SliceHeader sh) {
        return sh.first_mb_in_slice == 0 && sh.slice_type != null && sh.pic_parameter_set_id < 2;
    }

    private boolean validSps(SeqParameterSet sps) {
        return sps.bit_depth_chroma_minus8 < 4 && sps.bit_depth_luma_minus8 < 4 && sps.chroma_format_idc != null
                && sps.seq_parameter_set_id < 2 && sps.pic_order_cnt_type <= 2;
    }

    private boolean validPps(PictureParameterSet pps) {
        return pps.pic_init_qp_minus26 <= 26 && pps.seq_parameter_set_id <= 2 && pps.pic_parameter_set_id <= 2;
    }

    public void setDebug(boolean b) {
        this.debug = b;
    }
}