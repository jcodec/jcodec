package org.jcodec.containers.mp4;

import static ch.lambdaj.Lambda.on;
import static org.jcodec.containers.mp4.TrackType.SOUND;
import static org.jcodec.containers.mp4.TrackType.TIMECODE;
import static org.jcodec.containers.mp4.TrackType.VIDEO;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.sound.sampled.AudioFormat;

import junit.framework.Assert;

import org.jcodec.common.io.Buffer;
import org.jcodec.common.io.RandomAccessOutputStream;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;
import org.jcodec.common.model.TapeTimecode;
import org.jcodec.common.model.Unit;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsets64Box;
import org.jcodec.containers.mp4.boxes.ClearApertureBox;
import org.jcodec.containers.mp4.boxes.CompositionOffsetsBox;
import org.jcodec.containers.mp4.boxes.CompositionOffsetsBox.Entry;
import org.jcodec.containers.mp4.boxes.DataInfoBox;
import org.jcodec.containers.mp4.boxes.DataRefBox;
import org.jcodec.containers.mp4.boxes.Edit;
import org.jcodec.containers.mp4.boxes.EditListBox;
import org.jcodec.containers.mp4.boxes.EncodedPixelBox;
import org.jcodec.containers.mp4.boxes.EndianBox;
import org.jcodec.containers.mp4.boxes.EndianBox.Endian;
import org.jcodec.containers.mp4.boxes.FileTypeBox;
import org.jcodec.containers.mp4.boxes.FormatBox;
import org.jcodec.containers.mp4.boxes.GenericMediaInfoBox;
import org.jcodec.containers.mp4.boxes.HandlerBox;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.LeafBox;
import org.jcodec.containers.mp4.boxes.MediaBox;
import org.jcodec.containers.mp4.boxes.MediaHeaderBox;
import org.jcodec.containers.mp4.boxes.MediaInfoBox;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.MovieHeaderBox;
import org.jcodec.containers.mp4.boxes.NameBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.PixelAspectExt;
import org.jcodec.containers.mp4.boxes.ProductionApertureBox;
import org.jcodec.containers.mp4.boxes.SampleDescriptionBox;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.SampleSizesBox;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.SampleToChunkEntry;
import org.jcodec.containers.mp4.boxes.SoundMediaHeaderBox;
import org.jcodec.containers.mp4.boxes.SyncSamplesBox;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry;
import org.jcodec.containers.mp4.boxes.TimecodeMediaInfoBox;
import org.jcodec.containers.mp4.boxes.TimecodeSampleEntry;
import org.jcodec.containers.mp4.boxes.TrackHeaderBox;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.containers.mp4.boxes.VideoMediaHeaderBox;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.movtool.Util;

import ch.lambdaj.Lambda;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Creates MP4 file out of a set of samples
 * 
 * @author The JCodec project
 * 
 */
public class MP4Muxer {
    private List<MuxerTrack> tracks = new ArrayList<MuxerTrack>();
    private long mdatOffset;

    private int nextTrackId = 1;
    private RandomAccessOutputStream out;

    public MP4Muxer(RandomAccessOutputStream output) throws IOException {
        this(output, Brand.MP4);
    }

    public MP4Muxer(RandomAccessOutputStream output, Brand brand) throws IOException {
        this(output, brand.getFileTypeBox());
    }

    public MP4Muxer(RandomAccessOutputStream output, FileTypeBox ftyp) throws IOException {
        this.out = output;

        ftyp.write(out);
        new Header("wide", 8).write(output);
        new Header("mdat", 1).write(output);
        mdatOffset = output.getPos();
        output.writeLong(0);
    }

    public CompressedTrack addVideoTrackWithTimecode(String fourcc, Size size, String encoderName, int timescale) {
        TimecodeTrack timecode = addTimecodeTrack(timescale);

        CompressedTrack track = addTrackForCompressed(VIDEO, timescale);

        track.addSampleEntry(videoSampleEntry(fourcc, size, encoderName));
        track.setTimecode(timecode);

        return track;
    }

    public CompressedTrack addVideoTrack(String fourcc, Size size, String encoderName, int timescale) {
        CompressedTrack track = addTrackForCompressed(VIDEO, timescale);

        track.addSampleEntry(videoSampleEntry(fourcc, size, encoderName));
        return track;
    }

    public static VideoSampleEntry videoSampleEntry(String fourcc, Size size, String encoderName) {
        return new VideoSampleEntry(new Header(fourcc), (short) 0, (short) 0, "jcod", 0, 768, (short) size.getWidth(),
                (short) size.getHeight(), 72, 72, (short) 1, encoderName != null ? encoderName : "jcodec", (short) 24,
                (short) 1, (short) -1);
    }

    public static AudioSampleEntry audioSampleEntry(String fourcc, int drefId, int sampleSize, int channels,
            int sampleRate, Endian endian) {
        AudioSampleEntry ase = new AudioSampleEntry(new Header(fourcc, 0), (short) drefId, (short) channels,
                (short) 16, sampleRate, (short) 0, 0, 65535, 0, 1, sampleSize, channels * sampleSize, sampleSize,
                (short) 1);

        NodeBox wave = new NodeBox(new Header("wave"));
        ase.add(wave);

        wave.add(new FormatBox(fourcc));
        wave.add(new EndianBox(endian));
        wave.add(terminatorAtom());
        // ase.add(new ChannelBox(atom));

        return ase;
    }

    public static LeafBox terminatorAtom() {
        return new LeafBox(new Header(new String(new byte[4])), new byte[0]);
    }

    public TimecodeTrack addTimecodeTrack(int timescale) {
        TimecodeTrack track = new TimecodeTrack(nextTrackId++, timescale);
        tracks.add(track);
        return track;
    }

    public CompressedTrack addTrackForCompressed(TrackType type, int timescale) {
        CompressedTrack track = new CompressedTrack(nextTrackId++, type, timescale);
        tracks.add(track);
        return track;
    }

    public UncompressedTrack addTrackForUncompressed(TrackType type, int timescale, int sampleDuration, int sampleSize,
            SampleEntry se) {
        UncompressedTrack track = new UncompressedTrack(nextTrackId++, type, timescale, sampleDuration, sampleSize, se);
        tracks.add(track);
        return track;
    }

    public List<MuxerTrack> getTracks() {
        return tracks;
    }

    public abstract class MuxerTrack {
        protected int trackId;
        protected TrackType type;
        protected int timescale;

        protected Rational tgtChunkDuration;
        protected Unit tgtChunkDurationUnit;

        protected long chunkDuration;
        protected List<Buffer> curChunk = new ArrayList<Buffer>();

        protected List<SampleToChunkEntry> samplesInChunks = new ArrayList<SampleToChunkEntry>();
        protected int samplesInLastChunk = -1;
        protected int chunkNo = 0;

        protected boolean finished;

        protected List<SampleEntry> sampleEntries = new ArrayList<SampleEntry>();
        protected List<Edit> edits;
        private String name;

        public MuxerTrack(int trackId, TrackType type, int timescale) {
            this.trackId = trackId;
            this.type = type;
            this.timescale = timescale;
        }

        public void setTgtChunkDuration(Rational duration, Unit unit) {
            this.tgtChunkDuration = duration;
            this.tgtChunkDurationUnit = unit;
        }

        public abstract long getTrackTotalDuration();

        public int getTimescale() {
            return timescale;
        }

        protected abstract Box finish(MovieHeaderBox mvhd) throws IOException;

        public boolean isVideo() {
            return type == VIDEO;
        }

        public boolean isTimecode() {
            return type == TIMECODE;
        }

        public boolean isAudio() {
            return type == SOUND;
        }

        public Size getDisplayDimensions() {
            int width = 0, height = 0;
            if (sampleEntries.get(0) instanceof VideoSampleEntry) {
                VideoSampleEntry vse = (VideoSampleEntry) sampleEntries.get(0);
                PixelAspectExt paspBox = Box.findFirst(vse, PixelAspectExt.class, PixelAspectExt.fourcc());
                Rational pasp = paspBox != null ? paspBox.getRational() : new Rational(1, 1);
                width = (int) (pasp.getNum() * vse.getWidth()) / pasp.getDen();
                height = (int) vse.getHeight();
            }
            return new Size(width, height);
        }

        public void tapt(TrakBox trak) {
            Size dd = getDisplayDimensions();
            if (type == VIDEO) {
                NodeBox tapt = new NodeBox(new Header("tapt"));
                tapt.add(new ClearApertureBox(dd.getWidth(), dd.getHeight()));
                tapt.add(new ProductionApertureBox(dd.getWidth(), dd.getHeight()));
                tapt.add(new EncodedPixelBox(dd.getWidth(), dd.getHeight()));
                trak.add(tapt);
            }
        }

        public void addSampleEntry(SampleEntry se) {
            if (finished)
                throw new IllegalStateException("The muxer track has finished muxing");
            sampleEntries.add(se);
        }

        public List<SampleEntry> getEntries() {
            return sampleEntries;
        }

        public void setEdits(List<Edit> edits) {
            this.edits = edits;
        }

        protected void putEdits(TrakBox trak) {
            if (edits != null) {
                NodeBox edts = new NodeBox(new Header("edts"));
                edts.add(new EditListBox(edits));
                trak.add(edts);
            }
        }

        public void setName(String name) {
            this.name = name;
        }

        protected void putName(TrakBox trak) {
            if (name != null) {
                NodeBox udta = new NodeBox(new Header("udta"));
                udta.add(new NameBox(name));
                trak.add(udta);
            }
        }
    }

    public class UncompressedTrack extends MuxerTrack {

        private int frameDuration;
        private int frameSize;
        private int framesInCurChunk;

        private TLongArrayList chunkOffsets = new TLongArrayList();
        private int totalFrames;

        public UncompressedTrack(int trackId, TrackType type, int timescale, int frameDuration, int frameSize,
                SampleEntry se) {
            super(trackId, type, timescale);
            this.frameDuration = frameDuration;
            this.frameSize = frameSize;
            addSampleEntry(se);

            setTgtChunkDuration(new Rational(1, 2), Unit.SEC);
        }

        public void addSamples(Buffer buffer) throws IOException {
            curChunk.add(buffer);

            int frames = buffer.remaining() / frameSize;
            totalFrames += frames;

            framesInCurChunk += frames;
            chunkDuration += frames * frameDuration;

            outChunkIfNeeded();
        }

        private void outChunkIfNeeded() throws IOException {
            Assert.assertTrue(tgtChunkDurationUnit == Unit.FRAME || tgtChunkDurationUnit == Unit.SEC);

            if (tgtChunkDurationUnit == Unit.FRAME
                    && framesInCurChunk * tgtChunkDuration.getDen() == tgtChunkDuration.getNum()) {
                outChunk();
            } else if (tgtChunkDurationUnit == Unit.SEC && chunkDuration > 0
                    && chunkDuration * tgtChunkDuration.getDen() >= tgtChunkDuration.getNum() * timescale) {
                outChunk();
            }
        }

        private void outChunk() throws IOException {
            if (framesInCurChunk == 0)
                return;

            chunkOffsets.add(out.getPos());

            for (Buffer b : curChunk) {
                b.writeTo(out);
            }
            curChunk.clear();

            if (samplesInLastChunk == -1 || framesInCurChunk != samplesInLastChunk) {
                samplesInChunks.add(new SampleToChunkEntry(chunkNo + 1, framesInCurChunk, 1));
            }
            samplesInLastChunk = framesInCurChunk;

            chunkNo++;

            framesInCurChunk = 0;
            chunkDuration = 0;
        }

        protected Box finish(MovieHeaderBox mvhd) throws IOException {
            if (finished)
                throw new IllegalStateException("The muxer track has finished muxing");

            outChunk();

            finished = true;

            TrakBox trak = new TrakBox();
            Size dd = getDisplayDimensions();
            TrackHeaderBox tkhd = new TrackHeaderBox(trackId,
                    ((long) mvhd.getTimescale() * totalFrames * frameDuration) / timescale, dd.getWidth(),
                    dd.getHeight(), new Date().getTime(), new Date().getTime(), 1.0f, (short) 0, 0, new int[] {
                            0x10000, 0, 0, 0, 0x10000, 0, 0, 0, 0x40000000 });
            tkhd.setFlags(0xf);
            trak.add(tkhd);

            tapt(trak);

            MediaBox media = new MediaBox();
            trak.add(media);
            media.add(new MediaHeaderBox(timescale, totalFrames * frameDuration, 0, new Date().getTime(), new Date()
                    .getTime(), 0));

            HandlerBox hdlr = new HandlerBox("mhlr", type.getHandler(), "appl", 0, 0);
            media.add(hdlr);

            MediaInfoBox minf = new MediaInfoBox();
            media.add(minf);
            mediaHeader(minf, type);
            minf.add(new HandlerBox("dhlr", "url ", "appl", 0, 0));
            addDref(minf);
            NodeBox stbl = new NodeBox(new Header("stbl"));
            minf.add(stbl);

            putEdits(trak);
            putName(trak);

            stbl.add(new SampleDescriptionBox(sampleEntries.toArray(new SampleEntry[0])));
            stbl.add(new SampleToChunkBox(samplesInChunks.toArray(new SampleToChunkEntry[0])));
            stbl.add(new SampleSizesBox(frameSize, totalFrames));
            stbl.add(new TimeToSampleBox(new TimeToSampleEntry[] { new TimeToSampleEntry(totalFrames, frameDuration) }));
            stbl.add(new ChunkOffsets64Box(chunkOffsets.toArray()));

            return trak;
        }

        public long getTrackTotalDuration() {
            return totalFrames * frameDuration;
        }
    }

    public class TimecodeTrack extends CompressedTrack {

        private TapeTimecode prevTimecode;
        private TapeTimecode firstTimecode;

        private int fpsEstimate;
        private long sampleDuration;
        private long samplePts;
        private int tcFrames;
        private List<Edit> lower = new ArrayList<Edit>();

        private List<Packet> gop = new ArrayList<Packet>();

        public TimecodeTrack(int trackId, int timescale) {
            super(trackId, TrackType.TIMECODE, timescale);
        }

        public void addTimecode(Packet packet) throws IOException {
            if (packet.isKeyFrame())
                processGop();
            gop.add(new Packet(packet, (Buffer) null));
        }

        private void processGop() throws IOException {
            if (gop.size() > 0) {
                for (Packet pkt : Lambda.<Packet> sort(gop, on(Packet.class).getDisplayOrder())) {
                    addTimecodeInt(pkt);
                }
                gop.clear();
            }
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

        // TODO: support drop frame timecode
        private boolean isTimeGap(TapeTimecode prevTimecode, TapeTimecode tapeTimecode) {
            boolean gap = false;
            int secDiff = toSec(tapeTimecode) - toSec(prevTimecode);
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
                    if (tapeTimecode.getFrame() != 0 || prevTimecode.getFrame() != fpsEstimate - 1)
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
                    TimecodeSampleEntry tmcd = new TimecodeSampleEntry((firstTimecode.isDropFrame() ? 1 : 0),
                            timescale, (int) (sampleDuration / tcFrames), fpsEstimate);
                    sampleEntries.add(tmcd);
                    Buffer sample = new Buffer(4);
                    sample.dout().writeInt(toCounter(firstTimecode, fpsEstimate));
                    addFrame(new MP4Packet(new Buffer(sample.buffer, 0, 4), samplePts, timescale, sampleDuration, 0,
                            true, null, samplePts, sampleEntries.size() - 1));

                    lower.add(new Edit(sampleDuration, samplePts, 1.0f));
                } else {
                    lower.add(new Edit(sampleDuration, -1, 1.0f));
                }
            }
        }

        private int toCounter(TapeTimecode tc, int fps) {
            return toSec(tc) * fps + tc.getFrame();
        }

        private int toSec(TapeTimecode tc) {
            return tc.getHour() * 3600 + tc.getMinute() * 60 + tc.getSecond();
        }
    }

    public class CompressedTrack extends MuxerTrack {

        private List<TimeToSampleEntry> sampleDurations = new ArrayList<TimeToSampleEntry>();
        private long sameDurCount = 0;
        private long curDuration = -1;

        private TLongArrayList chunkOffsets = new TLongArrayList();
        private TIntArrayList sampleSizes = new TIntArrayList();
        private TIntArrayList iframes = new TIntArrayList();

        private List<Entry> compositionOffsets = new ArrayList<Entry>();
        private int lastCompositionOffset = 0;
        private int lastCompositionSamples = 0;
        private long ptsEstimate = 0;

        private int lastEntry = -1;

        private long trackTotalDuration;
        private int curFrame;
        private boolean allIframes = true;
        private TimecodeTrack timecodeTrack;

        public CompressedTrack(int trackId, TrackType type, int timescale) {
            super(trackId, type, timescale);

            setTgtChunkDuration(new Rational(1, 1), Unit.FRAME);
        }

        public void addFrame(MP4Packet pkt) throws IOException {
            if (finished)
                throw new IllegalStateException("The muxer track has finished muxing");
            int entryNo = pkt.getEntryNo() + 1;

            int compositionOffset = (int) (pkt.getPts() - ptsEstimate);
            if (compositionOffset != lastCompositionOffset) {
                if (lastCompositionSamples > 0)
                    compositionOffsets.add(new Entry(lastCompositionSamples, lastCompositionOffset));
                lastCompositionOffset = compositionOffset;
                lastCompositionSamples = 0;
            }
            lastCompositionSamples++;
            ptsEstimate += pkt.getDuration();

            if (lastEntry != -1 && lastEntry != entryNo) {
                outChunk(lastEntry);
                samplesInLastChunk = -1;
            }

            curChunk.add(pkt.getData());

            if (pkt.isKeyFrame())
                iframes.add(curFrame + 1);
            else
                allIframes = false;

            curFrame++;

            chunkDuration += pkt.getDuration();
            if (curDuration != -1 && pkt.getDuration() != curDuration) {
                sampleDurations.add(new TimeToSampleEntry((int) sameDurCount, (int) curDuration));
                sameDurCount = 0;
            }
            curDuration = pkt.getDuration();
            sameDurCount++;
            trackTotalDuration += pkt.getDuration();

            outChunkIfNeeded(entryNo);

            processTimecode(pkt);

            lastEntry = entryNo;
        }

        private void processTimecode(MP4Packet pkt) throws IOException {
            if (timecodeTrack != null)
                timecodeTrack.addTimecode(pkt);
        }

        private void outChunkIfNeeded(int entryNo) throws IOException {
            Assert.assertTrue(tgtChunkDurationUnit == Unit.FRAME || tgtChunkDurationUnit == Unit.SEC);

            if (tgtChunkDurationUnit == Unit.FRAME
                    && curChunk.size() * tgtChunkDuration.getDen() == tgtChunkDuration.getNum()) {
                outChunk(entryNo);
            } else if (tgtChunkDurationUnit == Unit.SEC && chunkDuration > 0
                    && chunkDuration * tgtChunkDuration.getDen() >= tgtChunkDuration.getNum() * timescale) {
                outChunk(entryNo);
            }
        }

        void outChunk(int entryNo) throws IOException {
            if (curChunk.size() == 0)
                return;

            chunkOffsets.add(out.getPos());

            for (Buffer bs : curChunk) {
                sampleSizes.add(bs.remaining());
                bs.writeTo(out);
            }

            if (samplesInLastChunk == -1 || samplesInLastChunk != curChunk.size()) {
                samplesInChunks.add(new SampleToChunkEntry(chunkNo + 1, curChunk.size(), entryNo));
            }
            samplesInLastChunk = curChunk.size();
            chunkNo++;

            chunkDuration = 0;
            curChunk.clear();
        }

        protected Box finish(MovieHeaderBox mvhd) throws IOException {
            if (finished)
                throw new IllegalStateException("The muxer track has finished muxing");

            outChunk(lastEntry);

            if (sameDurCount > 0) {
                sampleDurations.add(new TimeToSampleEntry((int) sameDurCount, (int) curDuration));
            }
            finished = true;

            TrakBox trak = new TrakBox();
            Size dd = getDisplayDimensions();
            TrackHeaderBox tkhd = new TrackHeaderBox(trackId, ((long) mvhd.getTimescale() * trackTotalDuration)
                    / timescale, dd.getWidth(), dd.getHeight(), new Date().getTime(), new Date().getTime(), 1.0f,
                    (short) 0, 0, new int[] { 0x10000, 0, 0, 0, 0x10000, 0, 0, 0, 0x40000000 });
            tkhd.setFlags(0xf);
            trak.add(tkhd);

            tapt(trak);

            MediaBox media = new MediaBox();
            trak.add(media);
            media.add(new MediaHeaderBox(timescale, trackTotalDuration, 0, new Date().getTime(), new Date().getTime(),
                    0));

            HandlerBox hdlr = new HandlerBox("mhlr", type.getHandler(), "appl", 0, 0);
            media.add(hdlr);

            MediaInfoBox minf = new MediaInfoBox();
            media.add(minf);
            mediaHeader(minf, type);
            minf.add(new HandlerBox("dhlr", "url ", "appl", 0, 0));
            addDref(minf);
            NodeBox stbl = new NodeBox(new Header("stbl"));
            minf.add(stbl);

            putCompositionOffsets(stbl);
            putEdits(trak);
            putName(trak);

            stbl.add(new SampleDescriptionBox(sampleEntries.toArray(new SampleEntry[0])));
            stbl.add(new SampleToChunkBox(samplesInChunks.toArray(new SampleToChunkEntry[0])));
            stbl.add(new SampleSizesBox(sampleSizes.toArray()));
            stbl.add(new TimeToSampleBox(sampleDurations.toArray(new TimeToSampleEntry[] {})));
            stbl.add(new ChunkOffsets64Box(chunkOffsets.toArray()));
            if (!allIframes && iframes.size() > 0)
                stbl.add(new SyncSamplesBox(iframes.toArray()));

            return trak;
        }

        private void putCompositionOffsets(NodeBox stbl) {
            if (compositionOffsets.size() > 0) {
                compositionOffsets.add(new Entry(lastCompositionSamples, lastCompositionOffset));

                Integer min = Lambda.min(compositionOffsets, Lambda.on(Entry.class).getOffset());
                if (min > 0) {
                    for (Entry entry : compositionOffsets) {
                        entry.offset -= min;
                    }
                }

                Entry first = compositionOffsets.get(0);
                if (first.getOffset() > 0) {
                    if (edits == null) {
                        edits = new ArrayList<Edit>();
                        edits.add(new Edit(trackTotalDuration, first.getOffset(), 1.0f));
                    } else {
                        for (Edit edit : edits) {
                            edit.setMediaTime(edit.getMediaTime() + first.getOffset());
                        }
                    }
                }

                stbl.add(new CompositionOffsetsBox(compositionOffsets.toArray(new Entry[0])));
            }
        }

        public long getTrackTotalDuration() {
            return trackTotalDuration;
        }

        public void addSampleEntries(SampleEntry[] sampleEntries) {
            for (SampleEntry se : sampleEntries) {
                addSampleEntry(se);
            }
        }

        public TimecodeTrack getTimecodeTrack() {
            return timecodeTrack;
        }

        public void setTimecode(TimecodeTrack timecodeTrack) {
            this.timecodeTrack = timecodeTrack;
        }
    }

    public void writeHeader() throws IOException {
        NodeBox movie = new MovieBox();
        MovieHeaderBox mvhd = movieHeader(movie);
        movie.addFirst(mvhd);

        for (MuxerTrack track : tracks) {
            Box trak = track.finish(mvhd);
            if (trak != null)
                movie.add(trak);
        }

        long mdatSize = out.getPos() - mdatOffset + 8;
        movie.write(out);

        out.seek(mdatOffset);
        out.writeLong(mdatSize);
    }

    private void mediaHeader(MediaInfoBox minf, TrackType type) {
        switch (type) {
        case VIDEO:
            VideoMediaHeaderBox vmhd = new VideoMediaHeaderBox(0, 0, 0, 0);
            vmhd.setFlags(1);
            minf.add(vmhd);
            break;
        case SOUND:
            SoundMediaHeaderBox smhd = new SoundMediaHeaderBox();
            smhd.setFlags(1);
            minf.add(smhd);
            break;
        case TIMECODE:
            NodeBox gmhd = new NodeBox(new Header("gmhd"));
            gmhd.add(new GenericMediaInfoBox());
            NodeBox tmcd = new NodeBox(new Header("tmcd"));
            gmhd.add(tmcd);
            tmcd.add(new TimecodeMediaInfoBox((short) 0, (short) 0, (short) 12, new short[] { 0, 0, 0 }, new short[] {
                    0xff, 0xff, 0xff }, "Lucida Grande"));
            minf.add(gmhd);
            break;
        default:
            throw new IllegalStateException("Handler " + type.getHandler() + " not supported");
        }
    }

    private void addDref(NodeBox minf) {
        DataInfoBox dinf = new DataInfoBox();
        minf.add(dinf);
        DataRefBox dref = new DataRefBox();
        dinf.add(dref);
        dref.add(new LeafBox(new Header("alis", 0), new byte[] { 0, 0, 0, 1 }));
    }

    public MuxerTrack getVideoTrack() {
        for (MuxerTrack frameMuxer : tracks) {
            if (frameMuxer.isVideo()) {
                return frameMuxer;
            }
        }
        return null;
    }

    public MuxerTrack getTimecodeTrack() {
        for (MuxerTrack frameMuxer : tracks) {
            if (frameMuxer.isTimecode()) {
                return frameMuxer;
            }
        }
        return null;
    }

    public List<MuxerTrack> getAudioTracks() {
        ArrayList<MuxerTrack> result = new ArrayList<MuxerTrack>();
        for (MuxerTrack frameMuxer : tracks) {
            if (frameMuxer.isAudio()) {
                result.add(frameMuxer);
            }
        }
        return result;
    }

    private MovieHeaderBox movieHeader(NodeBox movie) {
        int timescale = tracks.get(0).getTimescale();
        long duration = tracks.get(0).getTrackTotalDuration();
        MuxerTrack videoTrack = getVideoTrack();
        if (videoTrack != null) {
            timescale = videoTrack.getTimescale();
            duration = videoTrack.getTrackTotalDuration();
        }

        return new MovieHeaderBox(timescale, duration, 1.0f, 1.0f, new Date().getTime(), new Date().getTime(),
                new int[] { 0x10000, 0, 0, 0, 0x10000, 0, 0, 0, 0x40000000 }, nextTrackId);
    }

    public static String lookupFourcc(AudioFormat format) {
        if (format.getSampleSizeInBits() == 16 && !format.isBigEndian())
            return "sowt";
        else if (format.getSampleSizeInBits() == 24)
            return "in24";
        else
            throw new IllegalArgumentException("Audio format " + format + " is not supported.");
    }

    public UncompressedTrack addUncompressedAudioTrack(AudioFormat format) {
        return addTrackForUncompressed(SOUND, (int) format.getSampleRate(), 1, (format.getSampleSizeInBits() >> 3)
                * format.getChannels(), MP4Muxer.audioSampleEntry(lookupFourcc(format), 1,
                format.getSampleSizeInBits() >> 3, format.getChannels(), (int) format.getSampleRate(),
                format.isBigEndian() ? Endian.BIG_ENDIAN : Endian.LITTLE_ENDIAN));
    }

    public CompressedTrack addCompressedAudioTrack(String fourcc, int timescale, int channels, int sampleRate,
            int samplesPerPkt, Box... extra) {
        CompressedTrack track = addTrackForCompressed(SOUND, timescale);

        AudioSampleEntry ase = new AudioSampleEntry(new Header(fourcc, 0), (short) 1, (short) channels, (short) 16,
                sampleRate, (short) 0, 0, 65534, 0, samplesPerPkt, 0, 0, 2, (short) 1);

        NodeBox wave = new NodeBox(new Header("wave"));
        ase.add(wave);

        wave.add(new FormatBox(fourcc));
        for (Box box : extra)
            wave.add(box);

        wave.add(terminatorAtom());

        track.addSampleEntry(ase);

        return track;
    }
}