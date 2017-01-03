package org.jcodec.containers.mp4.muxer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jcodec.codecs.aac.ADTSParser;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.mpeg4.mp4.EsdsBox;
import org.jcodec.common.ArrayUtil;
import org.jcodec.common.Assert;
import org.jcodec.common.Codec;
import org.jcodec.common.IntArrayList;
import org.jcodec.common.LongArrayList;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;
import org.jcodec.common.model.Unit;
import org.jcodec.containers.mp4.MP4TrackType;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsets64Box;
import org.jcodec.containers.mp4.boxes.CompositionOffsetsBox;
import org.jcodec.containers.mp4.boxes.CompositionOffsetsBox.Entry;
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
public class FramesMP4MuxerTrack extends AbstractMP4MuxerTrack {

    private List<TimeToSampleEntry> sampleDurations;
    private long sameDurCount = 0;
    private long curDuration = -1;

    private LongArrayList chunkOffsets;
    private IntArrayList sampleSizes;
    private IntArrayList iframes;

    private List<Entry> compositionOffsets;
    private int lastCompositionOffset = 0;
    private int lastCompositionSamples = 0;
    private long ptsEstimate = 0;

    private int lastEntry = -1;

    private long trackTotalDuration;
    private int curFrame;
    private boolean allIframes = true;
    private TimecodeMP4MuxerTrack timecodeTrack;
    private SeekableByteChannel out;
    private Codec codec;

    // SPS/PPS lists when h.264 is stored, otherwise these lists are not used.
    private List<ByteBuffer> spsList = new ArrayList<ByteBuffer>();
    private List<ByteBuffer> ppsList = new ArrayList<ByteBuffer>();

    // ADTS header used to construct audio sample entry for AAC
    private ADTSParser.Header adtsHeader;

    public FramesMP4MuxerTrack(SeekableByteChannel out, int trackId, MP4TrackType type, Codec codec) {
        super(trackId, type);
        this.sampleDurations = new ArrayList<TimeToSampleEntry>();
        this.chunkOffsets = LongArrayList.createLongArrayList();
        this.sampleSizes = IntArrayList.createIntArrayList();
        this.iframes = IntArrayList.createIntArrayList();
        this.compositionOffsets = new ArrayList<Entry>();

        this.out = out;

        this.codec = codec;

        setTgtChunkDuration(new Rational(1, 1), Unit.FRAME);
    }

    public void addFrame(Packet pkt) throws IOException {
        if (codec == Codec.H264) {
            ByteBuffer result = pkt.getData();
            H264Utils.wipePSinplace(result, spsList, ppsList);
            result = H264Utils.encodeMOVPacket(result);
            pkt = Packet.createPacketWithData(pkt, result);
        } else if (codec == Codec.AAC && adtsHeader == null) {
            adtsHeader = ADTSParser.read(pkt.getData());
        }
        addFrameInternal(pkt, 1);
        processTimecode(pkt);
    }

    public void addFrameInternal(Packet pkt, int entryNo) throws IOException {
        if (finished)
            throw new IllegalStateException("The muxer track has finished muxing");

        if (_timescale != NO_TIMESCALE_SET && pkt.getTimescale() != _timescale)
            throw new RuntimeException(
                    "Mix and match of timescale is not supported, was: " + _timescale + ", now: " + pkt.getTimescale());

        if (_timescale == NO_TIMESCALE_SET)
            _timescale = pkt.getTimescale();

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

        lastEntry = entryNo;
    }

    private void processTimecode(Packet pkt) throws IOException {
        if (timecodeTrack != null)
            timecodeTrack.addTimecode(pkt);
    }

    private void outChunkIfNeeded(int entryNo) throws IOException {
        Assert.assertTrue(tgtChunkDurationUnit == Unit.FRAME || tgtChunkDurationUnit == Unit.SEC);

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

    protected Box finish(MovieHeaderBox mvhd) throws IOException {
        if (finished)
            throw new IllegalStateException("The muxer track has finished muxing");
        setCodecPrivateIfNeeded();

        outChunk(lastEntry);

        if (sameDurCount > 0) {
            sampleDurations.add(new TimeToSampleEntry((int) sameDurCount, (int) curDuration));
        }
        finished = true;

        TrakBox trak = TrakBox.createTrakBox();
        Size dd = getDisplayDimensions();
        TrackHeaderBox tkhd = TrackHeaderBox.createTrackHeaderBox(trackId,
                ((long) mvhd.getTimescale() * trackTotalDuration) / _timescale, dd.getWidth(), dd.getHeight(),
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

        putCompositionOffsets(stbl);
        putEdits(trak);
        putName(trak);

        stbl.add(SampleDescriptionBox.createSampleDescriptionBox(sampleEntries.toArray(new SampleEntry[0])));
        stbl.add(SampleToChunkBox.createSampleToChunkBox(samplesInChunks.toArray(new SampleToChunkEntry[0])));
        stbl.add(SampleSizesBox.createSampleSizesBox2(sampleSizes.toArray()));
        stbl.add(TimeToSampleBox.createTimeToSampleBox(sampleDurations.toArray(new TimeToSampleEntry[] {})));
        stbl.add(ChunkOffsets64Box.createChunkOffsets64Box(chunkOffsets.toArray()));
        if (!allIframes && iframes.size() > 0)
            stbl.add(SyncSamplesBox.createSyncSamplesBox(iframes.toArray()));

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

            stbl.add(CompositionOffsetsBox.createCompositionOffsetsBox(compositionOffsets.toArray(new Entry[0])));
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

    public void setCodecPrivateIfNeeded() {
        if (codec == Codec.H264) {
            getEntries().get(0).add(H264Utils.createAvcCFromPS(selectUnique(spsList), selectUnique(ppsList), 4));
        } else if (codec == Codec.AAC) {
            getEntries().get(0).add(EsdsBox.fromADTS(adtsHeader));
        }
    }

    private static class ByteArrayWrapper {
        private byte[] bytes;

        public ByteArrayWrapper(ByteBuffer bytes) {
            this.bytes = NIOUtils.toArray(bytes);
        }

        public ByteBuffer get() {
            return ByteBuffer.wrap(bytes);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ByteArrayWrapper))
                return false;
            return Arrays.equals(bytes, ((ByteArrayWrapper) obj).bytes);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(bytes);
        }
    }

    private List<ByteBuffer> selectUnique(List<ByteBuffer> bblist) {
        Set<ByteArrayWrapper> all = new HashSet<ByteArrayWrapper>();
        for (ByteBuffer byteBuffer : bblist) {
            all.add(new ByteArrayWrapper(byteBuffer));
        }
        List<ByteBuffer> result = new ArrayList<ByteBuffer>();
        for (ByteArrayWrapper bs : all) {
            result.add(bs.get());
        }
        return result;
    }
}