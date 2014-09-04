package org.jcodec.containers.mp4.muxer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jcodec.common.Assert;
import org.jcodec.common.IntArrayList;
import org.jcodec.common.LongArrayList;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;
import org.jcodec.common.model.Unit;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.TrackType;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsets64Box;
import org.jcodec.containers.mp4.boxes.CompositionOffsetsBox;
import org.jcodec.containers.mp4.boxes.Edit;
import org.jcodec.containers.mp4.boxes.HandlerBox;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.MediaBox;
import org.jcodec.containers.mp4.boxes.MediaHeaderBox;
import org.jcodec.containers.mp4.boxes.MediaInfoBox;
import org.jcodec.containers.mp4.boxes.MovieHeaderBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.SampleDescriptionBox;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.SampleSizesBox;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox;
import org.jcodec.containers.mp4.boxes.SyncSamplesBox;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox;
import org.jcodec.containers.mp4.boxes.TrackHeaderBox;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.containers.mp4.boxes.CompositionOffsetsBox.Entry;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.SampleToChunkEntry;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class FramesMP4MuxerTrack extends AbstractMP4MuxerTrack {

    private List<TimeToSampleEntry> sampleDurations = new ArrayList<TimeToSampleEntry>();
    private long sameDurCount = 0;
    private long curDuration = -1;

    private LongArrayList chunkOffsets = new LongArrayList();
    private IntArrayList sampleSizes = new IntArrayList();
    private IntArrayList iframes = new IntArrayList();

    private List<Entry> compositionOffsets = new ArrayList<Entry>();
    private int lastCompositionOffset = 0;
    private int lastCompositionSamples = 0;
    private long ptsEstimate = 0;

    private int lastEntry = -1;

    private long trackTotalDuration;
    private int curFrame;
    private boolean allIframes = true;
    private TimecodeMP4MuxerTrack timecodeTrack;
    private SeekableByteChannel out;

    public FramesMP4MuxerTrack(SeekableByteChannel out, int trackId, TrackType type, int timescale) {
        super(trackId, type, timescale);
        
        this.out = out;

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

        chunkOffsets.add(out.position());

        for (ByteBuffer bs : curChunk) {
            sampleSizes.add(bs.remaining());
            out.write(bs);
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

        stbl.add(new SampleDescriptionBox(sampleEntries.toArray(new SampleEntry[sampleEntries.size()])));
        stbl.add(new SampleToChunkBox(samplesInChunks.toArray(new SampleToChunkEntry[samplesInChunks.size()])));
        stbl.add(new SampleSizesBox(sampleSizes.toArray()));
        stbl.add(new TimeToSampleBox(sampleDurations.toArray(new TimeToSampleEntry[sampleDurations.size()])));
        stbl.add(new ChunkOffsets64Box(chunkOffsets.toArray()));
        if (!allIframes && iframes.size() > 0)
            stbl.add(new SyncSamplesBox(iframes.toArray()));

        return trak;
    }

    private void putCompositionOffsets(NodeBox stbl) {
        if (compositionOffsets.size() > 0) {
            compositionOffsets.add(new Entry(lastCompositionSamples, lastCompositionOffset));

            int min = minOffset(compositionOffsets);
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

            stbl.add(new CompositionOffsetsBox(compositionOffsets.toArray(new Entry[compositionOffsets.size()])));
        }
    }

    public static int minOffset(List<Entry> offs) {
        int min = Integer.MAX_VALUE;
        for (Entry entry : offs) {
            if (entry.getOffset() < min)
                min = entry.getOffset();
        }
        return min;
    }

    public long getTrackTotalDuration() {
        return trackTotalDuration;
    }

    public void addSampleEntries(SampleEntry[] sampleEntries) {
        for (SampleEntry se : sampleEntries) {
            addSampleEntry(se);
        }
    }

    public TimecodeMP4MuxerTrack getTimecodeTrack() {
        return timecodeTrack;
    }

    public void setTimecode(TimecodeMP4MuxerTrack timecodeTrack) {
        this.timecodeTrack = timecodeTrack;
    }
}