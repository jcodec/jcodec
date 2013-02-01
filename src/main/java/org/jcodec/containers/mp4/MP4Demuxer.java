package org.jcodec.containers.mp4;

import static org.jcodec.containers.mp4.QTTimeUtil.mediaToEdited;
import static org.jcodec.containers.mp4.boxes.Box.findFirst;
import gnu.trove.list.array.TIntArrayList;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jcodec.common.io.Buffer;
import org.jcodec.common.io.RAInputStream;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.common.model.TapeTimecode;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsets64Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsetsBox;
import org.jcodec.containers.mp4.boxes.CompositionOffsetsBox;
import org.jcodec.containers.mp4.boxes.CompositionOffsetsBox.Entry;
import org.jcodec.containers.mp4.boxes.Edit;
import org.jcodec.containers.mp4.boxes.EditListBox;
import org.jcodec.containers.mp4.boxes.HandlerBox;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.NameBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.SampleSizesBox;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.SampleToChunkEntry;
import org.jcodec.containers.mp4.boxes.SyncSamplesBox;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry;
import org.jcodec.containers.mp4.boxes.TimecodeSampleEntry;
import org.jcodec.containers.mp4.boxes.TrakBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Demuxer frontend for MP4
 * 
 * 
 * @author The JCodec project
 * 
 */
public class MP4Demuxer {

    private List<DemuxerTrack> tracks;
    private TimecodeTrack timecodeTrack;
    private RAInputStream input;
    private MovieBox movie;

    public abstract class DemuxerTrack {
        protected TrakBox box;
        private TrackType type;
        private int no;
        protected SampleEntry[] sampleEntries;

        protected TimeToSampleEntry[] timeToSamples;
        protected SampleToChunkEntry[] sampleToChunks;
        protected long[] chunkOffsets;

        protected long duration;

        protected int sttsInd;
        protected int sttsSubInd;

        protected int stcoInd;

        protected int stscInd;

        protected long pts;
        protected long curFrame;
        protected int timescale;

        public DemuxerTrack(TrakBox trak) {
            no = trak.getTrackHeader().getNo();
            type = getTrackType(trak);
            sampleEntries = Box.findAll(trak, SampleEntry.class, "mdia", "minf", "stbl", "stsd", null);

            NodeBox stbl = trak.getMdia().getMinf().getStbl();

            TimeToSampleBox stts = findFirst(stbl, TimeToSampleBox.class, "stts");
            SampleToChunkBox stsc = findFirst(stbl, SampleToChunkBox.class, "stsc");
            ChunkOffsetsBox stco = findFirst(stbl, ChunkOffsetsBox.class, "stco");
            ChunkOffsets64Box co64 = findFirst(stbl, ChunkOffsets64Box.class, "co64");

            timeToSamples = stts.getEntries();
            sampleToChunks = stsc.getSampleToChunk();
            chunkOffsets = stco != null ? stco.getChunkOffsets() : co64.getChunkOffsets();

            for (TimeToSampleEntry ttse : timeToSamples) {
                duration += ttse.getSampleCount() * ttse.getSampleDuration();
            }
            box = trak;

            timescale = trak.getTimescale();
        }

        public int pts2Sample(long _tv, int _timescale) {
            long tv = _tv * timescale / _timescale;
            int ttsInd, sample = 0;
            for (ttsInd = 0; ttsInd < timeToSamples.length - 1; ttsInd++) {
                int a = timeToSamples[ttsInd].getSampleCount() * timeToSamples[ttsInd].getSampleDuration();
                if (tv < a)
                    break;
                tv -= a;
                sample += timeToSamples[ttsInd].getSampleCount();
            }
            return sample + (int) (tv / timeToSamples[ttsInd].getSampleDuration());
        }

        public TrackType getType() {
            return type;
        }

        public int getNo() {
            return no;
        }

        public SampleEntry[] getSampleEntries() {
            return sampleEntries;
        }

        public TrakBox getBox() {
            return box;
        }

        public long getTimescale() {
            return timescale;
        }

        public abstract MP4Packet getFrames(byte[] buffer, int n) throws IOException;

        public abstract MP4Packet getFrames(int n) throws IOException;

        protected abstract void seekPointer(long frameNo);

        public boolean canSeek(long pts) {
            return pts >= 0 && pts < duration;
        }

        public synchronized boolean seek(long pts) {
            if (pts < 0)
                throw new IllegalArgumentException("Seeking to negative pts");
            if (pts >= duration)
                return false;

            sttsInd = 0;
            long prevDur = 0;
            int frameNo = 0;
            while (pts > prevDur + timeToSamples[sttsInd].getSampleCount() * timeToSamples[sttsInd].getSampleDuration()) {
                prevDur += timeToSamples[sttsInd].getSampleCount() * timeToSamples[sttsInd].getSampleDuration();
                frameNo += timeToSamples[sttsInd].getSampleCount();
            }
            sttsSubInd = (int) ((pts - prevDur) / timeToSamples[sttsInd].getSampleDuration());
            frameNo += sttsSubInd;
            this.pts = prevDur + timeToSamples[sttsInd].getSampleDuration() * sttsSubInd;

            seekPointer(frameNo);

            return true;
        }

        protected long shiftPts(long frames) {
            long result = 0;
            int rem;
            while (frames > (rem = timeToSamples[sttsInd].getSampleCount() - sttsSubInd)) {
                frames -= rem;
                result += rem * timeToSamples[sttsInd].getSampleDuration();
                sttsInd++;
                sttsSubInd = 0;
                if (sttsInd >= timeToSamples.length)
                    return result;
            }
            result += frames * timeToSamples[sttsInd].getSampleDuration();

            pts += result;

            return result;
        }

        protected void nextChunk() {
            if (stcoInd >= chunkOffsets.length)
                return;
            stcoInd++;

            if ((stscInd + 1 < sampleToChunks.length) && stcoInd + 1 == sampleToChunks[stscInd + 1].getFirst()) {
                stscInd++;
            }
        }

        public synchronized boolean gotoFrame(long frameNo) {
            if (frameNo < 0)
                throw new IllegalArgumentException("negative frame number");
            if (frameNo >= getFrameCount())
                return false;
            if (frameNo == curFrame)
                return true;

            seekPointer(frameNo);
            seekPts(frameNo);

            return true;
        }

        private void seekPts(long frameNo) {
            pts = sttsInd = sttsSubInd = 0;
            shiftPts(frameNo);
        }

        public int getStartTimecode() throws IOException {
            if (!box.isTimecode())
                throw new IllegalStateException("Not a timecode track");
            MP4Packet nextFrame = getFrames(new byte[4], 1);
            gotoFrame(0);
            return nextFrame.getData().fork().dinp().readInt();
        }

        public int parseTimecode(String tc) {
            String[] split = tc.split(":");

            TimecodeSampleEntry tmcd = Box.findFirst(box, TimecodeSampleEntry.class, "mdia", "minf", "stbl", "stsd",
                    "tmcd");
            byte nf = tmcd.getNumFrames();

            return Integer.parseInt(split[3]) + Integer.parseInt(split[2]) * nf + Integer.parseInt(split[1]) * 60 * nf
                    + Integer.parseInt(split[0]) * 3600 * nf;
        }

        public RationalLarge getDuration() {
            return new RationalLarge(box.getMediaDuration(), box.getTimescale());
        }

        public abstract long getFrameCount();

        public long getCurFrame() {
            return curFrame;
        }

        public List<Edit> getEdits() {
            EditListBox editListBox = Box.findFirst(box, EditListBox.class, "edts", "elst");
            if (editListBox != null)
                return editListBox.getEdits();
            return null;
        }

        public String getName() {
            NameBox nameBox = Box.findFirst(box, NameBox.class, "udta", "name");
            return nameBox != null ? nameBox.getName() : null;
        }

        public String getFourcc() {
            return getSampleEntries()[0].getFourcc();
        }
    }

    public DemuxerTrack create(TrakBox trak) {
        SampleSizesBox stsz = findFirst(trak, SampleSizesBox.class, "mdia", "minf", "stbl", "stsz");
        if (stsz.getDefaultSize() == 0)
            return new FramesTrack(trak);
        else
            return new SamplesTrack(trak);
    }

    /**
     * Track of audio samples
     * 
     */
    private class SamplesTrack extends DemuxerTrack {

        private int defaultSampleSize;

        private long posShift;

        protected int totalFrames;

        public SamplesTrack(TrakBox trak) {
            super(trak);

            SampleSizesBox stsz = findFirst(trak, SampleSizesBox.class, "mdia", "minf", "stbl", "stsz");
            defaultSampleSize = stsz.getDefaultSize();

            int chunks = 0;
            for (int i = 1; i < sampleToChunks.length; i++) {
                int ch = (int) (sampleToChunks[i].getFirst() - sampleToChunks[i - 1].getFirst());
                totalFrames += ch * sampleToChunks[i - 1].getCount();
                chunks += ch;
            }
            totalFrames += sampleToChunks[sampleToChunks.length - 1].getCount() * (chunkOffsets.length - chunks);
        }

        public MP4Packet getFrames(int n) throws IOException {
            return getFrames(new byte[n * getFrameSize()], n);
        }

        public synchronized MP4Packet getFrames(byte[] result, int n) throws IOException {
            if (n < 0)
                throw new IllegalArgumentException("Negative number of frames");

            int frameSize = getFrameSize();
            n = (int) (curFrame + n > totalFrames ? totalFrames - curFrame : n);
            if (n == 0 || stcoInd >= chunkOffsets.length) {
                return null;
            }

            int tgtLen = frameSize * n;
            if (tgtLen > result.length) {
                throw new IllegalArgumentException("Insufficient room to fit " + n + " samples");
            }

            int se = sampleToChunks[stscInd].getEntry();
            int rOff = 0;
            do {
                long chSize = sampleToChunks[stscInd].getCount() * frameSize;
                int toRead = (int) Math.min(tgtLen - rOff, chSize - posShift);
                int read;
                synchronized (input) {
                    input.seek(chunkOffsets[stcoInd] + posShift);
                    read = input.read(result, rOff, toRead);
                }
                if (read == -1)
                    break;
                rOff += read;
                posShift += read;

                if (posShift == chSize) {
                    nextChunk();
                    posShift = 0;
                }
            } while (rOff < tgtLen && stcoInd < chunkOffsets.length && sampleToChunks[stscInd].getEntry() == se);

            long _pts = pts;
            int doneFrames = rOff / frameSize;
            shiftPts(doneFrames);

            MP4Packet pkt = new MP4Packet(new Buffer(result),
                    QTTimeUtil.mediaToEdited(box, _pts, movie.getTimescale()), timescale, (int) (pts - _pts), curFrame,
                    true, null, _pts, se - 1);

            curFrame += doneFrames;

            return pkt;
        }

        public int getFrameSize() {
            SampleEntry entry = sampleEntries[sampleToChunks[stscInd].getEntry() - 1];
            if (entry instanceof AudioSampleEntry) {
                return ((AudioSampleEntry) entry).calcFrameSize();
            } else {
                return defaultSampleSize;
            }
        }

        protected void seekPointer(long frameNo) {
            for (stcoInd = 0, stscInd = 0, curFrame = 0;;) {
                long nextFrame = curFrame + sampleToChunks[stscInd].getCount();
                if (nextFrame > frameNo)
                    break;
                curFrame = nextFrame;
                nextChunk();
            }
            posShift = (int) ((frameNo - curFrame) * getFrameSize());
            curFrame = frameNo;
        }

        public long getFrameCount() {
            return totalFrames;
        }
    }

    /**
     * Timecode track, provides timecode information for video track
     * 
     */
    public class TimecodeTrack {

        private TrakBox box;
        private TimeToSampleEntry[] timeToSamples;
        private int[] samples;
        private long[] samplesTv;
        private int[] samplesDur;
        private int[] samplesStartFrame;
        private TimecodeSampleEntry tse;

        public TimecodeTrack(TrakBox trak) throws IOException {
            this.box = trak;

            NodeBox stbl = trak.getMdia().getMinf().getStbl();

            TimeToSampleBox stts = findFirst(stbl, TimeToSampleBox.class, "stts");
            SampleToChunkBox stsc = findFirst(stbl, SampleToChunkBox.class, "stsc");
            ChunkOffsetsBox stco = findFirst(stbl, ChunkOffsetsBox.class, "stco");
            ChunkOffsets64Box co64 = findFirst(stbl, ChunkOffsets64Box.class, "co64");

            timeToSamples = stts.getEntries();
            readSamples(stsc.getSampleToChunk(), stco != null ? stco.getChunkOffsets() : co64.getChunkOffsets());
            
            tse = (TimecodeSampleEntry) box.getSampleEntries()[0];

            cacheEntryPoints();
        }

        private void cacheEntryPoints() {
            samplesTv = new long[samples.length];
            samplesDur = new int[samples.length];
            samplesStartFrame = new int[samples.length];
            int ttsInd = 0, ttsSubInd = 0;

            long tv = 0;
            for (int i = 0; i < samples.length; i++) {
                samplesTv[i] = tv;
                samplesStartFrame[i] = (int) ((tv * tse.getNumFrames()) / box.getTimescale());
                int dur = timeToSamples[ttsInd].getSampleDuration();
                samplesDur[i] = dur;
                tv += dur;
                ttsSubInd++;
                if (ttsInd < timeToSamples.length - 1 && ttsSubInd >= timeToSamples[ttsInd].getSampleCount())
                    ttsInd++;
            }
        }

        public MP4Packet getTimecode(MP4Packet pkt) throws IOException {

            long tv = QTTimeUtil
                    .editedToMedia(box, box.rescale(pkt.getPts(), pkt.getTimescale()), movie.getTimescale());
            int sample;
            for (sample = 0; sample < samples.length - 1; sample++) {
                if (samplesTv[sample] <= tv && samplesTv[sample] + samplesDur[sample] > tv)
                    break;
            }

            int frameNo = (int) ((tv * tse.getNumFrames()) / box.getTimescale());

            return new MP4Packet(pkt, getTimecode(samples[sample], frameNo - samplesStartFrame[sample], tse));
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

        private void readSamples(SampleToChunkEntry[] sampleToChunks, long[] chunkOffsets) throws IOException {
            synchronized (input) {
                int stscInd = 0;
                TIntArrayList ss = new TIntArrayList();
                for (int chunkNo = 0; chunkNo < chunkOffsets.length; chunkNo++) {
                    int nSamples = sampleToChunks[stscInd].getCount();
                    if (stscInd < sampleToChunks.length - 1 && chunkNo + 1 >= sampleToChunks[stscInd + 1].getFirst())
                        stscInd++;
                    long offset = chunkOffsets[chunkNo];
                    input.seek(offset);
                    for (int i = 0; i < nSamples; i++) {
                        ss.add((input.read() << 24) | (input.read() << 16) | (input.read() << 8) | input.read());
                    }
                }
                samples = ss.toArray();
            }
        }

        /**
         * 
         * @return
         * @deprecated Use getTimecode to automatically populate tape timecode
         *             for each frame
         */
        public int getStartTimecode() {
            return samples[0];
        }
    }

    /**
     * Track of video frames
     * 
     */
    public class FramesTrack extends DemuxerTrack {

        private int[] sizes;

        private long offInChunk;

        private int noInChunk;

        private int[] syncSamples;
        private int ssOff;

        private Entry[] compOffsets;
        private int cttsInd;
        private int cttsSubInd;

        public FramesTrack(TrakBox trak) {
            super(trak);

            SampleSizesBox stsz = findFirst(trak, SampleSizesBox.class, "mdia", "minf", "stbl", "stsz");
            SyncSamplesBox stss = Box.findFirst(trak, SyncSamplesBox.class, "mdia", "minf", "stbl", "stss");
            CompositionOffsetsBox ctts = Box.findFirst(trak, CompositionOffsetsBox.class, "mdia", "minf", "stbl",
                    "ctts");
            compOffsets = ctts == null ? null : ctts.getEntries();
            if (stss != null) {
                syncSamples = stss.getSyncSamples();
            }
            sizes = stsz.getSizes();
        }

        public synchronized MP4Packet getFrames(int n) throws IOException {
            if (n != 1)
                throw new IllegalArgumentException("Frames should be = 1 for this track");
            if (curFrame >= sizes.length)
                return null;
            int size = sizes[(int) curFrame];

            return getFrames(new byte[size], 1);
        }

        public synchronized MP4Packet getFrames(byte[] result, int n) throws IOException {
            if (n != 1)
                throw new IllegalArgumentException("Frames should be = 1 for this track");

            if (curFrame >= sizes.length)
                return null;
            int size = sizes[(int) curFrame];

            if (result.length < size) {
                throw new IllegalArgumentException("Buffer size is not enough to fit a packet");
            }

            synchronized (input) {
                input.seek(chunkOffsets[stcoInd] + offInChunk);
                if (input.read(result) < size)
                    return null;
            }

            int duration = timeToSamples[sttsInd].getSampleDuration();

            boolean sync = syncSamples == null;

            if (syncSamples != null && ssOff < syncSamples.length && (curFrame + 1) == syncSamples[ssOff]) {
                sync = true;
                ssOff++;
            }

            long realPts = pts;
            if (compOffsets != null) {
                realPts = pts + compOffsets[cttsInd].getOffset();
                cttsSubInd++;
                if (cttsInd < compOffsets.length - 1 && cttsSubInd == compOffsets[cttsInd].getCount()) {
                    cttsInd++;
                    cttsSubInd = 0;
                }
            }

            MP4Packet pkt = new MP4Packet(new Buffer(result), mediaToEdited(box, realPts, movie.getTimescale()),
                    timescale, duration, curFrame, sync, null, realPts, sampleToChunks[stscInd].getEntry() - 1);

            offInChunk += size;

            curFrame++;
            noInChunk++;
            sttsSubInd++;
            if (noInChunk >= sampleToChunks[stscInd].getCount()) {
                noInChunk = 0;
                offInChunk = 0;

                nextChunk();
            }
            shiftPts(1);

            return pkt;
        }

        protected void seekPointer(long frameNo) {
            if (compOffsets != null) {
                cttsSubInd = (int) frameNo;
                cttsInd = 0;
                while (cttsSubInd >= compOffsets[cttsInd].getCount()) {
                    cttsSubInd -= compOffsets[cttsInd].getCount();
                    cttsInd++;
                }
            }

            curFrame = (int) frameNo;
            stcoInd = 0;
            stscInd = 0;
            noInChunk = (int) frameNo;
            offInChunk = 0;

            while (noInChunk >= sampleToChunks[stscInd].getCount()) {
                noInChunk -= sampleToChunks[stscInd].getCount();

                nextChunk();
            }

            for (int i = 0; i < noInChunk; i++) {
                offInChunk += sizes[(int) frameNo - noInChunk + i];
            }
        }

        public long getFrameCount() {
            return sizes.length;
        }
    };

    public MP4Demuxer(RAInputStream input) throws IOException {
        this.input = input;
        tracks = new LinkedList<DemuxerTrack>();
        findMovieBox(input);
    }
    
    public DemuxerTrack[] getTracks() {
        return tracks.toArray(new DemuxerTrack[] {});
    }

    private void findMovieBox(RAInputStream input) throws IOException {
        movie = MP4Util.parseMovie(input);
        if (movie == null)
            throw new IOException("Could not find movie meta information box");

        processHeader(movie);
    }

    private void processHeader(NodeBox moov) throws IOException {
        TrakBox tt = null;
        for (TrakBox trak : Box.findAll(moov, TrakBox.class, "trak")) {
            SampleEntry se = Box.findFirst(trak, SampleEntry.class, "mdia", "minf", "stbl", "stsd", null);
            if ("tmcd".equals(se.getFourcc())) {
                tt = trak;
            } else {
                tracks.add(create(trak));
            }
        }
        if (tt != null) {
            DemuxerTrack video = getVideoTrack();
            if (video != null)
                timecodeTrack = new TimecodeTrack(tt);
        }
    }

    public static TrackType getTrackType(TrakBox trak) {
        HandlerBox handler = findFirst(trak, HandlerBox.class, "mdia", "hdlr");
        return TrackType.fromHandler(handler.getComponentSubType());
    }

    public DemuxerTrack getVideoTrack() {
        for (DemuxerTrack demuxerTrack : tracks) {
            if (demuxerTrack.box.isVideo())
                return demuxerTrack;
        }
        return null;
    }

    public MovieBox getMovie() {
        return movie;
    }

    public DemuxerTrack getTrack(int no) {
        for (DemuxerTrack track : tracks) {
            if (track.getNo() == no)
                return track;
        }
        return null;
    }

    public List<DemuxerTrack> getAudioTracks() {
        ArrayList<DemuxerTrack> result = new ArrayList<DemuxerTrack>();
        for (DemuxerTrack demuxerTrack : tracks) {
            if (demuxerTrack.box.isAudio())
                result.add(demuxerTrack);
        }
        return result;
    }

    public TimecodeTrack getTimecodeTrack() {
        return timecodeTrack;
    }

    private static int ftyp = ('f' << 24) | ('t' << 16) | ('y' << 8) | 'p';
    private static int free = ('f' << 24) | ('r' << 16) | ('e' << 8) | 'e';
    private static int moov = ('m' << 24) | ('o' << 16) | ('o' << 8) | 'v';
    private static int mdat = ('m' << 24) | ('d' << 16) | ('a' << 8) | 't';

    public static int probe(final Buffer b) {
        Buffer fork = b.fork();
        DataInput dinp = fork.dinp();
        int success = 0;
        int total = 0;
        try {
            while (fork.remaining() >= 8) {
                long len = dinp.readInt();
                int fcc = dinp.readInt();
                int hdrLen = 8;
                if (len == 1) {
                    len = dinp.readLong();
                    hdrLen = 16;
                } else if (len < 8)
                    break;
                if (fcc == ftyp && len < 64 || fcc == moov && len < 100 * 1024 * 1024 || fcc == free || fcc == mdat)
                    success++;
                total++;
                if (len >= Integer.MAX_VALUE)
                    break;
                fork.read((int) len - hdrLen);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return success * 100 / total;
    }
}