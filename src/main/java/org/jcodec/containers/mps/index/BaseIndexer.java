package org.jcodec.containers.mps.index;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jcodec.codecs.mpeg12.bitstream.PictureHeader;
import org.jcodec.common.ArrayUtil;
import org.jcodec.common.IntArrayList;
import org.jcodec.common.LongArrayList;
import org.jcodec.common.RunLength;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.tools.MathUtil;
import org.jcodec.containers.mps.MPSDemuxer.PESPacket;
import org.jcodec.containers.mps.MPSUtils;
import org.jcodec.containers.mps.index.MPSIndex.MPSStreamIndex;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Indexes MPEG PS/TS file for the purpose of quick random access in the future
 * 
 * @author The JCodec project
 * 
 */
public abstract class BaseIndexer extends MPSUtils.PESReader {

    private Map<Integer, BaseAnalyser> analyzers;
    private LongArrayList tokens;
    private RunLength.Integer streams;

    public BaseIndexer() {
        this.analyzers = new HashMap<Integer, BaseAnalyser>();
        this.tokens = LongArrayList.createLongArrayList();
        this.streams = new RunLength.Integer();
    }

    public int estimateSize() {
        int sizeEstimate = (tokens.size() << 3) + streams.estimateSize() + 128;
        for (Integer stream : analyzers.keySet()) {
            sizeEstimate += analyzers.get(stream).estimateSize();
        }
        return sizeEstimate;
    }

    protected static abstract class BaseAnalyser {
        protected IntArrayList pts;
        protected IntArrayList dur;

        public BaseAnalyser() {
            this.pts = new IntArrayList(250000);
            this.dur = new IntArrayList(250000);
        }

        public abstract void pkt(ByteBuffer pkt, PESPacket pesHeader);

        public abstract void finishAnalyse();

        public int estimateSize() {
            return (pts.size() << 2) + 4;
        }

        public abstract MPSStreamIndex serialize(int streamId);
    }

    // TODO: check how ES are packetized in the following audio formats:
    // mp1, mp2, s302m, aac, pcm_s16le, pcm_s16be, pcm_dvd, mp3, ac3, dts, 
    private static class GenericAnalyser extends BaseAnalyser {
        private IntArrayList sizes;
        private int knownDuration;
        private long lastPts;

        public GenericAnalyser() {
            super();
            this.sizes = new IntArrayList(250000);
        }

        public void pkt(ByteBuffer pkt, PESPacket pesHeader) {
            sizes.add(pkt.remaining());

            if (pesHeader.pts == -1) {
                pesHeader.pts = lastPts + knownDuration;
            } else {
                knownDuration = (int) (pesHeader.pts - lastPts);
                lastPts = pesHeader.pts;
            }
            pts.add((int) pesHeader.pts);
            dur.add(knownDuration);
        }

        public MPSStreamIndex serialize(int streamId) {
            return new MPSStreamIndex(streamId, sizes.toArray(), pts.toArray(), dur.toArray(), new int[0]);
        }

        @Override
        public int estimateSize() {
            return super.estimateSize() + (sizes.size() << 2) + 32;
        }

        @Override
        public void finishAnalyse() {
        }
    }

    private static class MPEGVideoAnalyser extends BaseAnalyser {
        private int marker = -1;
        private long position;
        private IntArrayList sizes;
        private IntArrayList keyFrames;
        private int frameNo;
        private boolean inFrameData;

        private Frame lastFrame;
        private List<Frame> curGop;
        private long phPos = -1;
        private Frame lastFrameOfLastGop;

        public MPEGVideoAnalyser() {
            super();
            this.sizes = new IntArrayList(250000);
            this.keyFrames = new IntArrayList(20000);
            this.curGop = new ArrayList<Frame>();
        }

        private static class Frame {
            long offset;
            int size;
            int pts;
            int tempRef;
        }

        public void pkt(ByteBuffer pkt, PESPacket pesHeader) {

            while (pkt.hasRemaining()) {
                int b = pkt.get() & 0xff;
                ++position;
                marker = (marker << 8) | b;

                if (phPos != -1) {
                    long phOffset = position - phPos;
                    if (phOffset == 5)
                        lastFrame.tempRef = b << 2;
                    else if (phOffset == 6) {
                        int picCodingType = (b >> 3) & 0x7;
                        lastFrame.tempRef |= b >> 6;
                        if (picCodingType == PictureHeader.IntraCoded) {
                            keyFrames.add(frameNo - 1);
                            if (curGop.size() > 0)
                                outGop();
                        }
                    }
                }

                if ((marker & 0xffffff00) != 0x100)
                    continue;

                if (inFrameData && (marker == 0x100 || marker > 0x1af)) {
                    // End of frame
                    lastFrame.size = (int) (position - 4 - lastFrame.offset);
                    curGop.add(lastFrame);
                    lastFrame = null;
                    inFrameData = false;
                } else if (!inFrameData && (marker > 0x100 && marker <= 0x1af)) {
                    inFrameData = true;
                }

                if (lastFrame == null && (marker == 0x1b3 || marker == 0x1b8 || marker == 0x100)) {
                    Frame frame = new Frame();
                    frame.pts = (int) pesHeader.pts;
                    frame.offset = position - 4;
                    Logger.info(String.format("FRAME[%d]: %012x, %d", frameNo, (pesHeader.pos + pkt.position()
                            - 4), pesHeader.pts));
                    frameNo++;
                    lastFrame = frame;
                }
                if (lastFrame != null && lastFrame.pts == -1 && marker == 0x100) {
                    lastFrame.pts = (int) pesHeader.pts;
                }

                phPos = marker == 0x100 ? position - 4 : -1;
            }
        }

        private void outGop() {
            fixPts(curGop);
            for (Frame frame : curGop) {
                sizes.add(frame.size);
                pts.add(frame.pts);
            }
            curGop.clear();
        }

        private void fixPts(List<Frame> curGop) {
            Frame[] frames = curGop.toArray(new Frame[0]);
            Arrays.sort(frames, new Comparator<Frame>() {
                public int compare(Frame o1, Frame o2) {
                    return o1.tempRef > o2.tempRef ? 1 : (o1.tempRef == o2.tempRef ? 0 : -1);
                }
            });
            for (int dir = 0; dir < 3; dir++) {
                for (int i = 0, lastPts = -1, secondLastPts = -1, lastTref = -1, secondLastTref = -1; i < frames.length; i++) {
                    if (frames[i].pts == -1 && lastPts != -1 && secondLastPts != -1)
                        frames[i].pts = lastPts + (lastPts - secondLastPts) / MathUtil.abs(lastTref - secondLastTref);
                    if (frames[i].pts != -1) {
                        secondLastPts = lastPts;
                        secondLastTref = lastTref;
                        lastPts = frames[i].pts;
                        lastTref = frames[i].tempRef;
                    }
                }
                ArrayUtil.reverse(frames);
            }
            if (lastFrameOfLastGop != null) {
                dur.add(frames[0].pts - lastFrameOfLastGop.pts);
            }
            for (int i = 1; i < frames.length; i++) {
                dur.add(frames[i].pts - frames[i - 1].pts);
            }
            lastFrameOfLastGop = frames[frames.length - 1];
        }

        @Override
        public void finishAnalyse() {
            if (lastFrame == null)
                return;
            lastFrame.size = (int) (position - lastFrame.offset);
            curGop.add(lastFrame);
            outGop();
        }

        public MPSStreamIndex serialize(int streamId) {
            return new MPSStreamIndex(streamId, sizes.toArray(), pts.toArray(), dur.toArray(), keyFrames.toArray());
        }
    }

    protected BaseAnalyser getAnalyser(int stream) {
        BaseAnalyser analizer = analyzers.get(stream);
        if (analizer == null) {
            analizer = stream >= 0xe0 && stream <= 0xef ? new MPEGVideoAnalyser() : new GenericAnalyser();
            analyzers.put(stream, analizer);
        }
        return analyzers.get(stream);
    }

    public MPSIndex serialize() {
        List<MPSStreamIndex> streamsIndices = new ArrayList<MPSStreamIndex>();
        Set<Entry<Integer, BaseAnalyser>> entrySet = analyzers.entrySet();
        for (Entry<Integer, BaseAnalyser> entry : entrySet) {
            streamsIndices.add(entry.getValue().serialize(entry.getKey()));
        }
        return new MPSIndex(tokens.toArray(), streams, streamsIndices.toArray(new MPSStreamIndex[0]));
    }

    protected void savePESMeta(int stream, long token) {
        tokens.add(token);
        streams.add(stream);
    }

    void finishAnalyse() {
        super.finishRead();
        for (BaseAnalyser baseAnalyser : analyzers.values()) {
            baseAnalyser.finishAnalyse();
        }
    }
}