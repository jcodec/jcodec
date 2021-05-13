package org.jcodec.containers.mp4.muxer;

import static org.jcodec.common.Ints.checkedCast;
import static org.jcodec.common.Preconditions.checkState;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.jcodec.common.IntArrayList;
import org.jcodec.common.LongArrayList;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;
import org.jcodec.common.model.Unit;
import org.jcodec.containers.mp4.MP4TrackType;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsets64Box;
import org.jcodec.containers.mp4.boxes.CompositionOffsetsBox;
import org.jcodec.containers.mp4.boxes.CompositionOffsetsBox.Entry;
import org.jcodec.containers.mp4.boxes.CompositionOffsetsBox.LongEntry;
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
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.SampleToChunkEntry;
import org.jcodec.containers.mp4.boxes.SyncSamplesBox;
import org.jcodec.containers.mp4.boxes.TextMetaDataSampleEntry;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry;
import org.jcodec.containers.mp4.boxes.TrackHeaderBox;
import org.jcodec.containers.mp4.boxes.TrakBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MP4MuxerTrack extends AbstractMP4MuxerTrack {

    private List<TimeToSampleEntry> sampleDurations;
    private long sameDurCount = 0;
    private long curDuration = -1;

    private LongArrayList chunkOffsets;
    private IntArrayList sampleSizes;
    private IntArrayList iframes;

    private List<LongEntry> compositionOffsets;
    private long lastCompositionOffset = 0;
    private long lastCompositionSamples = 0;
    private long ptsEstimate = 0;

    private int lastEntry = -1;

    private long trackTotalDuration;
    private int curFrame;
    private boolean allIframes = true;
    private TimecodeMP4MuxerTrack timecodeTrack;
    public MP4MuxerTrack(int trackId, MP4TrackType type) {
        super(trackId, type);
        this.sampleDurations = new ArrayList<TimeToSampleEntry>();
        this.chunkOffsets = LongArrayList.createLongArrayList();
        this.sampleSizes = IntArrayList.createIntArrayList();
        this.iframes = IntArrayList.createIntArrayList();
        this.compositionOffsets = new ArrayList<LongEntry>();

        setTgtChunkDuration(new Rational(1, 1), Unit.FRAME);
    }

    @Override
    public void addFrame(Packet pkt) throws IOException {
        addFrameInternal(pkt, 1);
        processTimecode(pkt);
    }

    public void addFrameInternal(Packet pkt, int entryNo) throws IOException {
        if (finished)
            throw new IllegalStateException("The muxer track has finished muxing");

        if (_timescale == NO_TIMESCALE_SET) {
            _timescale = pkt.getTimescale();
        }
        
        if (_timescale != pkt.getTimescale()) {
            pkt.setPts((pkt.getPts() * _timescale) / pkt.getTimescale());
            pkt.setDuration((pkt.getDuration() * _timescale) / pkt.getTimescale());
            pkt.setTimescale(_timescale);
        }
        
        if(type == MP4TrackType.VIDEO) {
            long compositionOffset = pkt.getPts() - ptsEstimate;
            if (compositionOffset != lastCompositionOffset) {
                if (lastCompositionSamples > 0)
                    compositionOffsets.add(new LongEntry(lastCompositionSamples, lastCompositionOffset));
                lastCompositionOffset = compositionOffset;
                lastCompositionSamples = 0;
            }
            lastCompositionSamples++;
            ptsEstimate += pkt.getDuration();
        }

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

        lastEntry = entryNo;
    }

    private void processTimecode(Packet pkt) throws IOException {
        if (timecodeTrack != null)
            timecodeTrack.addTimecode(pkt);
    }

    private void outChunkIfNeeded(int entryNo) throws IOException {
        checkState(tgtChunkDurationUnit == Unit.FRAME || tgtChunkDurationUnit == Unit.SEC);

        if (tgtChunkDurationUnit == Unit.FRAME
                && curChunk.size() * tgtChunkDuration.getDen() == tgtChunkDuration.getNum()) {
            outChunk(entryNo);
        } else if (tgtChunkDurationUnit == Unit.SEC && chunkDuration > 0
                && chunkDuration * tgtChunkDuration.getDen() >= tgtChunkDuration.getNum() * _timescale) {
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

    @Override
    protected Box finish(MovieHeaderBox mvhd) throws IOException {
        checkState(!finished, "The muxer track has finished muxing");

        outChunk(lastEntry);

        if (sameDurCount > 0) {
            sampleDurations.add(new TimeToSampleEntry((int) sameDurCount, (int) curDuration));
        }
        finished = true;

        TrakBox trak = TrakBox.createTrakBox();
        Size dd = getDisplayDimensions();
        TrackHeaderBox tkhd = TrackHeaderBox.createTrackHeaderBox(trackId,
                (mvhd.getTimescale() * trackTotalDuration) / _timescale, dd.getWidth(), dd.getHeight(),
                new Date().getTime(), new Date().getTime(), 1.0f, (short) 0, 0,
                new int[] { 0x10000, 0, 0, 0, 0x10000, 0, 0, 0, 0x40000000 });
        tkhd.setFlags(0xf);
        trak.add(tkhd);

        tapt(trak);

        MediaBox media = MediaBox.createMediaBox();
        trak.add(media);
        media.add(MediaHeaderBox.createMediaHeaderBox(_timescale, trackTotalDuration, 0, new Date().getTime(),
                new Date().getTime(), 0));

        HandlerBox hdlr = HandlerBox.createHandlerBox("mhlr", type.getHandler(), "appl", 0, 0);
        media.add(hdlr);

        MediaInfoBox minf = MediaInfoBox.createMediaInfoBox();
        media.add(minf);
        mediaHeader(minf, type);
        minf.add(HandlerBox.createHandlerBox("dhlr", "url ", "appl", 0, 0));
        addDref(minf);
        NodeBox stbl = new NodeBox(new Header("stbl"));
        minf.add(stbl);

        putCompositionOffsets(mvhd, stbl);
        putEdits(trak);
        putName(trak);

        stbl.add(SampleDescriptionBox.createSampleDescriptionBox(sampleEntries.toArray(new SampleEntry[0])));
        stbl.add(SampleToChunkBox.createSampleToChunkBox(samplesInChunks.toArray(new SampleToChunkEntry[0])));
        stbl.add(createStsz());
        stbl.add(TimeToSampleBox.createTimeToSampleBox(sampleDurations.toArray(new TimeToSampleEntry[] {})));
        stbl.add(ChunkOffsets64Box.createChunkOffsets64Box(chunkOffsets.toArray()));
        if (!allIframes && iframes.size() > 0)
            stbl.add(SyncSamplesBox.createSyncSamplesBox(iframes.toArray()));

        return trak;
    }

    private SampleSizesBox createStsz() {
        if (sampleSizes.size() == 0) {
            return SampleSizesBox.createSampleSizesBox2(sampleSizes.toArray());
        }
        boolean allSame = true;
        int defaultSize = sampleSizes.get(0);
        for (int i = 0; i < sampleSizes.size(); i++) {
            if (sampleSizes.get(i) != defaultSize) {
                allSame = false;
                break;
            }
        }
        return allSame ? SampleSizesBox.createSampleSizesBox(defaultSize, sampleSizes.size())
                : SampleSizesBox.createSampleSizesBox2(sampleSizes.toArray());
    }

    private void putCompositionOffsets(MovieHeaderBox mvhd, NodeBox stbl) {
        if (compositionOffsets.size() > 0) {
            compositionOffsets.add(new LongEntry(lastCompositionSamples, lastCompositionOffset));

            long min = minLongOffset(compositionOffsets);
            if (min > 0) {
                for (LongEntry entry : compositionOffsets) {
                    entry.offset -= min;
                }
            }

            LongEntry first = compositionOffsets.get(0);
            if (first.getOffset() > 0) {
                if (edits == null) {
                    edits = new ArrayList<Edit>();
                    long totalDuration = (mvhd.getTimescale() * trackTotalDuration) / _timescale;
                    edits.add(new Edit(totalDuration, first.getOffset(), 1.0f));
                } else {
                    for (Edit edit : edits) {
                        edit.setMediaTime(edit.getMediaTime() + first.getOffset());
                    }
                }
            }
            
            Entry[] intEntries = new Entry[compositionOffsets.size()];
            for (int i = 0; i < compositionOffsets.size(); i++) {
                LongEntry longEntry = compositionOffsets.get(i);
                intEntries[i] = new Entry(checkedCast(longEntry.count), checkedCast(longEntry.offset));
            }

            stbl.add(CompositionOffsetsBox.createCompositionOffsetsBox(intEntries));
        }
    }

    public static long minLongOffset(List<LongEntry> offs) {
        long min = Long.MAX_VALUE;
        for (LongEntry entry : offs) {
            min = Math.min(min, entry.getOffset());
        }
        return min;
    }
    
    public static int minOffset(List<Entry> offs) {
        int min = Integer.MAX_VALUE;
        for (Entry entry : offs) {
            min = Math.min(min, entry.getOffset());
        }
        return min;
    }

    @Override
    public long getTrackTotalDuration() {
        return trackTotalDuration;
    }

    public TimecodeMP4MuxerTrack getTimecodeTrack() {
        return timecodeTrack;
    }

    public void setTimecode(TimecodeMP4MuxerTrack timecodeTrack) {
        this.timecodeTrack = timecodeTrack;
    }

    public void addMetaSampleEntry(String contentEncoding, String contentType) {
        TextMetaDataSampleEntry se = new TextMetaDataSampleEntry(contentEncoding, contentType);
        se.setDrefInd((short)1);
        addSampleEntry(se);
    }
}
