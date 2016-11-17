package org.jcodec.containers.mp4.muxer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jcodec.common.Codec;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.TapeTimecode;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.MP4TrackType;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.Edit;
import org.jcodec.containers.mp4.boxes.MovieHeaderBox;
import org.jcodec.containers.mp4.boxes.TimecodeSampleEntry;
import org.jcodec.movtool.Util;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Timecode MP4 muxer track
 * 
 * @author The JCodec project
 * 
 */
public class TimecodeMP4MuxerTrack extends FramesMP4MuxerTrack {

    private TapeTimecode prevTimecode;
    private TapeTimecode firstTimecode;

    private int fpsEstimate;
    private long sampleDuration;
    private long samplePts;
    private int tcFrames;
    private List<Edit> lower;
    private List<Packet> gop;

    public TimecodeMP4MuxerTrack(SeekableByteChannel out, int trackId) {
        super(out, trackId, MP4TrackType.TIMECODE, Codec.TIMECODE);
        this.lower = new ArrayList<Edit>();
        this.gop = new ArrayList<Packet>();
    }

    public void addTimecode(Packet packet) throws IOException {
        if(_timescale == NO_TIMESCALE_SET)
            _timescale = packet.getTimescale();
        if(_timescale != NO_TIMESCALE_SET && _timescale != packet.getTimescale())
            throw new RuntimeException("MP4 timecode track doesn't support timescale switching.");
        if (packet.isKeyFrame())
            processGop();
        gop.add(Packet.createPacketWithData(packet, (ByteBuffer) null));
    }

    private void processGop() throws IOException {
        if (gop.size() > 0) {
            for (Packet pkt : sortByDisplay(gop)) {
                addTimecodeInt(pkt);
            }
            gop.clear();
        }
    }

    private List<Packet> sortByDisplay(List<Packet> gop) {
        ArrayList<Packet> result = new ArrayList<Packet>(gop);
        Collections.sort(result, new Comparator<Packet>() {
            public int compare(Packet o1, Packet o2) {
                if (o1 == null && o2 == null)
                    return 0;
                else if (o1 == null)
                    return -1;
                else if (o2 == null)
                    return 1;
                else
                    return o1.getDisplayOrder() > o2.getDisplayOrder() ? 1 : (o1.getDisplayOrder() == o2
                            .getDisplayOrder() ? 0 : -1);
            }
        });
        return result;
    }

    protected Box finish(MovieHeaderBox mvhd) throws IOException {
        processGop();
        outTimecodeSample();

        if (sampleEntries.size() == 0)
            return null;

        if (edits != null) {
            edits = Util.editsOnEdits(new Rational(1, 1), lower, edits);
        } else
            edits = lower;

        return super.finish(mvhd);
    }

    private void addTimecodeInt(Packet packet) throws IOException {
        TapeTimecode tapeTimecode = packet.getTapeTimecode();
        boolean gap = isGap(prevTimecode, tapeTimecode);
        prevTimecode = tapeTimecode;

        if (gap) {
            outTimecodeSample();
            firstTimecode = tapeTimecode;
            fpsEstimate = tapeTimecode.isDropFrame() ? 30 : -1;
            samplePts += sampleDuration;
            sampleDuration = 0;
            tcFrames = 0;
        }
        sampleDuration += packet.getDuration();
        tcFrames++;
    }

    private boolean isGap(TapeTimecode prevTimecode, TapeTimecode tapeTimecode) {
        boolean gap = false;

        if (prevTimecode == null && tapeTimecode != null) {
            gap = true;
        } else if (prevTimecode != null) {
            if (tapeTimecode == null)
                gap = true;
            else {
                if (prevTimecode.isDropFrame() != tapeTimecode.isDropFrame()) {
                    gap = true;
                } else {
                    gap = isTimeGap(prevTimecode, tapeTimecode);
                }
            }
        }
        return gap;
    }

    private boolean isTimeGap(TapeTimecode prevTimecode, TapeTimecode tapeTimecode) {
        boolean gap = false;
        int sec = toSec(tapeTimecode);
        int secDiff = sec - toSec(prevTimecode);
        if (secDiff == 0) {
            int frameDiff = tapeTimecode.getFrame() - prevTimecode.getFrame();
            if (fpsEstimate != -1)
                frameDiff = (frameDiff + fpsEstimate) % fpsEstimate;
            gap = frameDiff != 1;
        } else if (secDiff == 1) {
            if (fpsEstimate == -1) {
                if (tapeTimecode.getFrame() == 0)
                    fpsEstimate = prevTimecode.getFrame() + 1;
                else
                    gap = true;
            } else {
                int firstFrame = tapeTimecode.isDropFrame() && (sec % 60) == 0 && (sec % 600) != 0 ? 2 : 0;

                if (tapeTimecode.getFrame() != firstFrame || prevTimecode.getFrame() != fpsEstimate - 1)
                    gap = true;
            }
        } else {
            gap = true;
        }
        return gap;
    }

    private void outTimecodeSample() throws IOException {
        if (sampleDuration > 0) {
            if (firstTimecode != null) {
                if (fpsEstimate == -1)
                    fpsEstimate = prevTimecode.getFrame() + 1;
                TimecodeSampleEntry tmcd = TimecodeSampleEntry.createTimecodeSampleEntry((firstTimecode.isDropFrame() ? 1 : 0), _timescale, (int) (sampleDuration / tcFrames), fpsEstimate);
                sampleEntries.add(tmcd);
                ByteBuffer sample = ByteBuffer.allocate(4);
                sample.putInt(toCounter(firstTimecode, fpsEstimate));
                sample.flip();
                addFrame(MP4Packet.createMP4Packet(sample, samplePts, _timescale, sampleDuration, 0, true, null, 0, samplePts, sampleEntries.size() - 1));

                lower.add(new Edit(sampleDuration, samplePts, 1.0f));
            } else {
                lower.add(new Edit(sampleDuration, -1, 1.0f));
            }
        }
    }

    private int toCounter(TapeTimecode tc, int fps) {
        int frames = toSec(tc) * fps + tc.getFrame();
        if (tc.isDropFrame()) {
            long D = frames / 18000;
            long M = frames % 18000;
            frames -= 18 * D + 2 * ((M - 2) / 1800);
        }

        return frames;
    }

    private int toSec(TapeTimecode tc) {
        return tc.getHour() * 3600 + tc.getMinute() * 60 + tc.getSecond();
    }
}