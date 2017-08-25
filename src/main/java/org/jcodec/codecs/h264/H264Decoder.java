package org.jcodec.codecs.h264;
import static org.jcodec.codecs.h264.H264Const.PROFILE_BASELINE;
import static org.jcodec.codecs.h264.H264Const.PROFILE_HIGH;
import static org.jcodec.codecs.h264.H264Const.PROFILE_MAIN;
import static org.jcodec.common.tools.MathUtil.wrap;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.jcodec.codecs.h264.H264Utils.MvList2D;
import org.jcodec.codecs.h264.decode.DeblockerInput;
import org.jcodec.codecs.h264.decode.FrameReader;
import org.jcodec.codecs.h264.decode.SliceDecoder;
import org.jcodec.codecs.h264.decode.SliceHeaderReader;
import org.jcodec.codecs.h264.decode.SliceReader;
import org.jcodec.codecs.h264.decode.deblock.DeblockingFilter;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.RefPicMarking;
import org.jcodec.codecs.h264.io.model.RefPicMarkingIDR;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.common.Codec;
import org.jcodec.common.IntObjectMap;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Rect;
import org.jcodec.common.model.Size;

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
public class H264Decoder extends VideoDecoder {

    private Frame[] sRefs;
    private IntObjectMap<Frame> lRefs;
    private List<Frame> pictureBuffer;
    private POCManager poc;
    private FrameReader reader;
    private ExecutorService tp;
    private boolean threaded;

    public H264Decoder() {
        pictureBuffer = new ArrayList<Frame>();
        poc = new POCManager();
        this.threaded = Runtime.getRuntime().availableProcessors() > 1;
        if (threaded) {
            tp = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                }
            });
        }
        reader = new FrameReader();
    }

    /**
     * Constructs this decoder from a portion of a stream that contains AnnexB
     * delimited (00 00 00 01) SPS/PPS NAL units. SPS/PPS NAL units are 0x67 and
     * 0x68 respectfully.
     * 
     * @param codecPrivate
     */
    public static H264Decoder createH264DecoderFromCodecPrivate(ByteBuffer codecPrivate) {
        H264Decoder d = new H264Decoder();
        for (ByteBuffer bb : H264Utils.splitFrame(codecPrivate.duplicate())) {
            NALUnit nu = NALUnit.read(bb);
            if (nu.type == NALUnitType.SPS) {
                d.reader.addSps(bb);
            } else if (nu.type == NALUnitType.PPS) {
                d.reader.addPps(bb);
            }
        }
        return d;
    }

    @Override
    public Frame decodeFrame(ByteBuffer data, byte[][] buffer) {
        return decodeFrameFromNals(H264Utils.splitFrame(data), buffer);
    }

    public Frame decodeFrameFromNals(List<ByteBuffer> nalUnits, byte[][] buffer) {
        return new FrameDecoder(this).decodeFrame(nalUnits, buffer);
    }

    private static final class SliceDecoderRunnable implements Runnable {
        private final SliceReader sliceReader;
        private final Frame result;
        private FrameDecoder fdec;

        private SliceDecoderRunnable(FrameDecoder fdec, SliceReader sliceReader, Frame result) {
            this.fdec = fdec;
            this.sliceReader = sliceReader;
            this.result = result;
        }

        public void run() {
            new SliceDecoder(fdec.activeSps, fdec.dec.sRefs, fdec.dec.lRefs, fdec.di, result)
                    .decodeFromReader(sliceReader);
        }
    }

    static class FrameDecoder {
        private SeqParameterSet activeSps;
        private DeblockingFilter filter;
        private SliceHeader firstSliceHeader;
        private NALUnit firstNu;
        private H264Decoder dec;
        private DeblockerInput di;

        public FrameDecoder(H264Decoder decoder) {
            this.dec = decoder;
        }

        public Frame decodeFrame(List<ByteBuffer> nalUnits, byte[][] buffer) {
            List<SliceReader> sliceReaders = dec.reader.readFrame(nalUnits);
            if (sliceReaders == null || sliceReaders.size() == 0)
                return null;
            final Frame result = init(sliceReaders.get(0), buffer);
            if (dec.threaded && sliceReaders.size() > 1) {
                List<Future<?>> futures = new ArrayList<Future<?>>();
                for (SliceReader sliceReader : sliceReaders) {
                    futures.add(dec.tp.submit(new SliceDecoderRunnable(this, sliceReader, result)));
                }

                for (Future<?> future : futures) {
                    waitForSure(future);
                }

            } else {
                for (SliceReader sliceReader : sliceReaders) {
                    new SliceDecoder(activeSps, dec.sRefs, dec.lRefs, di, result).decodeFromReader(sliceReader);
                }
            }

            filter.deblockFrame(result);

            updateReferences(result);

            return result;
        }

        private void waitForSure(Future<?> future) {
            while (true) {
                try {
                    future.get();
                    break;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
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

        private Frame init(SliceReader sliceReader, byte[][] buffer) {
            firstNu = sliceReader.getNALUnit();

            firstSliceHeader = sliceReader.getSliceHeader();
            activeSps = firstSliceHeader.sps;

            validateSupportedFeatures(firstSliceHeader.sps, firstSliceHeader.pps);

            int picWidthInMbs = activeSps.picWidthInMbsMinus1 + 1;
            int picHeightInMbs = SeqParameterSet.getPicHeightInMbs(activeSps);

            if (dec.sRefs == null) {
                dec.sRefs = new Frame[1 << (firstSliceHeader.sps.log2MaxFrameNumMinus4 + 4)];
                dec.lRefs = new IntObjectMap<Frame>();
            }

            di = new DeblockerInput(activeSps);

            Frame result = createFrame(activeSps, buffer, firstSliceHeader.frameNum, firstSliceHeader.sliceType,
                    di.mvs, di.refsUsed, dec.poc.calcPOC(firstSliceHeader, firstNu));

            filter = new DeblockingFilter(picWidthInMbs, activeSps.bitDepthChromaMinus8 + 8, di);

            return result;
        }

        private void validateSupportedFeatures(SeqParameterSet sps, PictureParameterSet pps) {
            if (sps.mbAdaptiveFrameFieldFlag)
                throw new RuntimeException("Unsupported h264 feature: MBAFF.");
            if (sps.bitDepthLumaMinus8 != 0 || sps.bitDepthChromaMinus8 != 0)
                throw new RuntimeException("Unsupported h264 feature: High bit depth.");
            if (sps.chromaFormatIdc != ColorSpace.YUV420J)
                throw new RuntimeException("Unsupported h264 feature: " + sps.chromaFormatIdc + " color.");
            if (!sps.frameMbsOnlyFlag || sps.fieldPicFlag)
                throw new RuntimeException("Unsupported h264 feature: interlace.");
            if (pps.constrainedIntraPredFlag)
                throw new RuntimeException("Unsupported h264 feature: constrained intra prediction.");
            if (sps.getScalingMatrix() != null || pps.extended != null && pps.extended.getScalingMatrix() != null)
                throw new RuntimeException("Unsupported h264 feature: scaling list.");
            if (sps.qpprimeYZeroTransformBypassFlag)
                throw new RuntimeException("Unsupported h264 feature: qprime zero transform bypass.");
            if (sps.profileIdc != PROFILE_BASELINE && sps.profileIdc != PROFILE_MAIN && sps.profileIdc != PROFILE_HIGH)
                throw new RuntimeException("Unsupported h264 feature: " + sps.profileIdc + " profile.");
        }

        public void performIDRMarking(RefPicMarkingIDR refPicMarkingIDR, Frame picture) {
            clearAll();
            dec.pictureBuffer.clear();

            Frame saved = saveRef(picture);
            if (refPicMarkingIDR.isUseForlongTerm()) {
                dec.lRefs.put(0, saved);
                saved.setShortTerm(false);
            } else
                dec.sRefs[firstSliceHeader.frameNum] = saved;
        }

        private Frame saveRef(Frame decoded) {
            Frame frame = dec.pictureBuffer.size() > 0 ? dec.pictureBuffer.remove(0) : Frame.createFrame(decoded);
            frame.copyFromFrame(decoded);
            return frame;
        }

        private void releaseRef(Frame picture) {
            if (picture != null) {
                dec.pictureBuffer.add(picture);
            }
        }

        public void clearAll() {
            for (int i = 0; i < dec.sRefs.length; i++) {
                releaseRef(dec.sRefs[i]);
                dec.sRefs[i] = null;
            }
            int[] keys = dec.lRefs.keys();
            for (int i = 0; i < keys.length; i++) {
                releaseRef(dec.lRefs.get(keys[i]));
            }
            dec.lRefs.clear();
        }

        public void performMarking(RefPicMarking refPicMarking, Frame picture) {
            Frame saved = saveRef(picture);

            if (refPicMarking != null) {
                RefPicMarking.Instruction[] instructions = refPicMarking.getInstructions();
                for (int i = 0; i < instructions.length; i++) {
                    RefPicMarking.Instruction instr = instructions[i];
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

            int maxFrames = 1 << (activeSps.log2MaxFrameNumMinus4 + 4);
            if (refPicMarking == null) {
                int maxShort = Math.max(1, activeSps.numRefFrames - dec.lRefs.size());
                int min = Integer.MAX_VALUE, num = 0, minFn = 0;
                for (int i = 0; i < dec.sRefs.length; i++) {
                    if (dec.sRefs[i] != null) {
                        int fnWrap = unwrap(firstSliceHeader.frameNum, dec.sRefs[i].getFrameNo(), maxFrames);
                        if (fnWrap < min) {
                            min = fnWrap;
                            minFn = dec.sRefs[i].getFrameNo();
                        }
                        num++;
                    }
                }
                if (num > maxShort) {
                    releaseRef(dec.sRefs[minFn]);
                    dec.sRefs[minFn] = null;
                }
            }
        }

        private int unwrap(int thisFrameNo, int refFrameNo, int maxFrames) {
            return refFrameNo > thisFrameNo ? refFrameNo - maxFrames : refFrameNo;
        }

        private void saveShort(Frame saved) {
            dec.sRefs[firstSliceHeader.frameNum] = saved;
        }

        private void saveLong(Frame saved, int longNo) {
            Frame prev = dec.lRefs.get(longNo);
            if (prev != null)
                releaseRef(prev);
            saved.setShortTerm(false);

            dec.lRefs.put(longNo, saved);
        }

        private void truncateLongTerm(int maxLongNo) {
            int[] keys = dec.lRefs.keys();
            for (int i = 0; i < keys.length; i++) {
                if (keys[i] > maxLongNo) {
                    releaseRef(dec.lRefs.get(keys[i]));
                    dec.lRefs.remove(keys[i]);
                }
            }
        }

        private void convert(int shortNo, int longNo) {
            int ind = wrap(firstSliceHeader.frameNum - shortNo,
                    1 << (firstSliceHeader.sps.log2MaxFrameNumMinus4 + 4));
            releaseRef(dec.lRefs.get(longNo));
            dec.lRefs.put(longNo, dec.sRefs[ind]);
            dec.sRefs[ind] = null;
            dec.lRefs.get(longNo).setShortTerm(false);
        }

        private void unrefLongTerm(int longNo) {
            releaseRef(dec.lRefs.get(longNo));
            dec.lRefs.remove(longNo);
        }

        private void unrefShortTerm(int shortNo) {
            int ind = wrap(firstSliceHeader.frameNum - shortNo,
                    1 << (firstSliceHeader.sps.log2MaxFrameNumMinus4 + 4));
            releaseRef(dec.sRefs[ind]);
            dec.sRefs[ind] = null;
        }
    }

    public static Frame createFrame(SeqParameterSet sps, byte[][] buffer, int frameNum, SliceType frameType,
            MvList2D mvs, Frame[][][] refsUsed, int POC) {
        int width = sps.picWidthInMbsMinus1 + 1 << 4;
        int height = SeqParameterSet.getPicHeightInMbs(sps) << 4;

        Rect crop = null;
        if (sps.frameCroppingFlag) {
            int sX = sps.frameCropLeftOffset << 1;
            int sY = sps.frameCropTopOffset << 1;
            int w = width - (sps.frameCropRightOffset << 1) - sX;
            int h = height - (sps.frameCropBottomOffset << 1) - sY;
            crop = new Rect(sX, sY, w, h);
        }
        return new Frame(width, height, buffer, ColorSpace.YUV420, crop, frameNum, frameType, mvs, refsUsed, POC);
    }

    public void addSps(List<ByteBuffer> spsList) {
        reader.addSpsList(spsList);
    }

    public void addPps(List<ByteBuffer> ppsList) {
        reader.addPpsList(ppsList);
    }

    public static int probe(ByteBuffer data) {
        boolean validSps = false, validPps = false, validSh = false;
        for (ByteBuffer nalUnit : H264Utils.splitFrame(data.duplicate())) {
            NALUnit marker = NALUnit.read(nalUnit);
            if (marker.type == NALUnitType.IDR_SLICE || marker.type == NALUnitType.NON_IDR_SLICE) {
                BitReader reader = BitReader.createBitReader(nalUnit);
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

    private static boolean validSh(SliceHeader sh) {
        return sh.firstMbInSlice == 0 && sh.sliceType != null && sh.picParameterSetId < 2;
    }

    private static boolean validSps(SeqParameterSet sps) {
        return sps.bitDepthChromaMinus8 < 4 && sps.bitDepthLumaMinus8 < 4 && sps.chromaFormatIdc != null
                && sps.seqParameterSetId < 2 && sps.picOrderCntType <= 2;
    }

    private static boolean validPps(PictureParameterSet pps) {
        return pps.picInitQpMinus26 <= 26 && pps.seqParameterSetId <= 2 && pps.picParameterSetId <= 2;
    }

    @Override
    public VideoCodecMeta getCodecMeta(ByteBuffer data) {
        List<ByteBuffer> rawSPS = H264Utils.getRawSPS(data.duplicate());
        List<ByteBuffer> rawPPS = H264Utils.getRawPPS(data.duplicate());
        if (rawSPS.size() == 0) {
            Logger.warn("Can not extract metadata from the packet not containing an SPS.");
            return null;
        }
        SeqParameterSet sps = SeqParameterSet.read(rawSPS.get(0));
        Size size = H264Utils.getPicSize(sps);
//, H264Utils.saveCodecPrivate(rawSPS, rawPPS)
        return org.jcodec.common.VideoCodecMeta.createSimpleVideoCodecMeta(Codec.H264, null, size, ColorSpace.YUV420);
    }
}
