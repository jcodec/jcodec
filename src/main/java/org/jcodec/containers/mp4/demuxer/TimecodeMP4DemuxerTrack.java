package org.jcodec.containers.mp4.demuxer;

import static org.jcodec.containers.mp4.boxes.Box.findFirst;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jcodec.common.IntArrayList;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.model.TapeTimecode;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.QTTimeUtil;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsets64Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsetsBox;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox;
import org.jcodec.containers.mp4.boxes.TimecodeSampleEntry;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.SampleToChunkEntry;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry;

/**
 * Timecode track, provides timecode information for video track
 * 
 */
public class TimecodeMP4DemuxerTrack {

    private TrakBox box;
    private TimeToSampleEntry[] timeToSamples;
    private int[] sampleCache;
    private TimecodeSampleEntry tse;
    private SeekableByteChannel input;
    private MovieBox movie;
    private long[] chunkOffsets;
    private SampleToChunkEntry[] sampleToChunks;

    public TimecodeMP4DemuxerTrack(MovieBox movie, TrakBox trak, SeekableByteChannel input) throws IOException {
        this.box = trak;
        this.input = input;
        this.movie = movie;

        NodeBox stbl = trak.getMdia().getMinf().getStbl();

        TimeToSampleBox stts = findFirst(stbl, TimeToSampleBox.class, "stts");
        SampleToChunkBox stsc = findFirst(stbl, SampleToChunkBox.class, "stsc");
        ChunkOffsetsBox stco = findFirst(stbl, ChunkOffsetsBox.class, "stco");
        ChunkOffsets64Box co64 = findFirst(stbl, ChunkOffsets64Box.class, "co64");

        timeToSamples = stts.getEntries();
        chunkOffsets = stco != null ? stco.getChunkOffsets() : co64.getChunkOffsets();
        sampleToChunks = stsc.getSampleToChunk();
        if (chunkOffsets.length == 1) {
            cacheSamples(sampleToChunks, chunkOffsets);
        }

        tse = (TimecodeSampleEntry) box.getSampleEntries()[0];
    }

    public MP4Packet getTimecode(MP4Packet pkt) throws IOException {

        long tv = QTTimeUtil.editedToMedia(box, box.rescale(pkt.getPts(), pkt.getTimescale()), movie.getTimescale());
        int sample;
        int ttsInd = 0, ttsSubInd = 0;
        for (sample = 0; sample < sampleCache.length - 1; sample++) {
            int dur = timeToSamples[ttsInd].getSampleDuration();
            if (tv < dur)
                break;
            tv -= dur;
            ttsSubInd++;
            if (ttsInd < timeToSamples.length - 1 && ttsSubInd >= timeToSamples[ttsInd].getSampleCount())
                ttsInd++;
        }

        int frameNo = (int) ((((2 * tv * tse.getTimescale()) / box.getTimescale()) / tse.getFrameDuration()) + 1) / 2;

        return new MP4Packet(pkt, getTimecode(getTimecodeSample(sample), frameNo, tse));
    }

    private int getTimecodeSample(int sample) throws IOException {
        if (sampleCache != null)
            return sampleCache[sample];
        else {
            synchronized (input) {
                int stscInd, stscSubInd;
                for (stscInd = 0, stscSubInd = sample; stscInd < sampleToChunks.length
                        && stscSubInd >= sampleToChunks[stscInd].getCount(); stscSubInd -= sampleToChunks[stscInd]
                        .getCount(), stscInd++)
                    ;
                long offset = chunkOffsets[stscInd]
                        + (Math.min(stscSubInd, sampleToChunks[stscInd].getCount() - 1) << 2);
                if (input.position() != offset)
                    input.position(offset);
                ByteBuffer buf = NIOUtils.fetchFrom(input, 4);
                return buf.getInt();
            }
        }
    }

    private TapeTimecode getTimecode(int startCounter, int frameNo, TimecodeSampleEntry entry) {
        int frame = dropFrameAdjust(frameNo + startCounter, entry);
        int sec = frame / entry.getNumFrames();
        return new TapeTimecode((short) (sec / 3600), (byte) ((sec / 60) % 60), (byte) (sec % 60),
                (byte) (frame % entry.getNumFrames()), entry.isDropFrame());
    }

    private int dropFrameAdjust(int frame, TimecodeSampleEntry entry) {
        if (entry.isDropFrame()) {
            long D = frame / 17982;
            long M = frame % 17982;
            frame += 18 * D + 2 * ((M - 2) / 1798);
        }
        return frame;
    }

    private void cacheSamples(SampleToChunkEntry[] sampleToChunks, long[] chunkOffsets) throws IOException {
        synchronized (input) {
            int stscInd = 0;
            IntArrayList ss = new IntArrayList();
            for (int chunkNo = 0; chunkNo < chunkOffsets.length; chunkNo++) {
                int nSamples = sampleToChunks[stscInd].getCount();
                if (stscInd < sampleToChunks.length - 1 && chunkNo + 1 >= sampleToChunks[stscInd + 1].getFirst())
                    stscInd++;
                long offset = chunkOffsets[chunkNo];
                input.position(offset);
                ByteBuffer buf = NIOUtils.fetchFrom(input, nSamples * 4);
                for (int i = 0; i < nSamples; i++) {
                    ss.add(buf.getInt());
                }
            }
            sampleCache = ss.toArray();
        }
    }

    /**
     * 
     * @return
     * @throws IOException 
     * @deprecated Use getTimecode to automatically populate tape timecode for
     *             each frame
     */
    public int getStartTimecode() throws IOException {
        return getTimecodeSample(0);
    }

    public TrakBox getBox() {
        return box;
    }

    public int parseTimecode(String tc) {
        String[] split = tc.split(":");

        TimecodeSampleEntry tmcd = Box
                .findFirst(box, TimecodeSampleEntry.class, "mdia", "minf", "stbl", "stsd", "tmcd");
        byte nf = tmcd.getNumFrames();

        return Integer.parseInt(split[3]) + Integer.parseInt(split[2]) * nf + Integer.parseInt(split[1]) * 60 * nf
                + Integer.parseInt(split[0]) * 3600 * nf;
    }

    public int timeCodeToFrameNo(String timeCode) throws Exception {
        if (isValidTimeCode(timeCode)) {
            int movieFrame = parseTimecode(timeCode.trim()) - sampleCache[0];
            int frameRate = tse.getNumFrames();
            long framesInTimescale = movieFrame * tse.getTimescale();
            long mediaToEdited = QTTimeUtil.mediaToEdited(box, framesInTimescale / frameRate, movie.getTimescale())
                    * frameRate;
            return (int) (mediaToEdited / box.getTimescale());
        }
        return -1;
    }

    private static boolean isValidTimeCode(String input) {
        Pattern p = Pattern.compile("[0-9][0-9]:[0-5][0-9]:[0-5][0-9]:[0-2][0-9]");
        Matcher m = p.matcher(input);
        if (input != null && !input.trim().equals("") && m.matches()) {
            return true;
        }
        return false;
    }
}