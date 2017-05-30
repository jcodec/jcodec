package org.jcodec.containers.mp4.muxer;
import org.jcodec.api.NotSupportedException;
import org.jcodec.common.Assert;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.LongArrayList;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;
import org.jcodec.common.model.Unit;
import org.jcodec.containers.mp4.MP4TrackType;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsets64Box;
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
import org.jcodec.containers.mp4.boxes.TimeToSampleBox;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry;
import org.jcodec.containers.mp4.boxes.TrackHeaderBox;
import org.jcodec.containers.mp4.boxes.TrakBox;

import java.io.IOException;
import java.lang.IllegalStateException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class PCMMP4MuxerTrack extends AbstractMP4MuxerTrack {

    private int frameDuration;
    private int frameSize;
    private int framesInCurChunk;

    private LongArrayList chunkOffsets;
    private int totalFrames;
    private SeekableByteChannel out;

    public PCMMP4MuxerTrack(SeekableByteChannel out, int trackId, AudioFormat format) {
        super(trackId, MP4TrackType.SOUND);
        this.chunkOffsets = LongArrayList.createLongArrayList();
        this.out = out;
        this.frameDuration = 1;
        this.frameSize = (format.getSampleSizeInBits() >> 3) * format.getChannels();
        addSampleEntry(_audioSampleEntry(format));
        this._timescale = format.getSampleRate();

        setTgtChunkDuration(new Rational(1, 2), Unit.SEC);
    }
    
    public static AudioSampleEntry _audioSampleEntry(AudioFormat format) {
        return MP4Muxer.audioSampleEntry(lookupFourcc(format), 1, format.getSampleSizeInBits() >> 3,
                format.getChannels(), (int) format.getSampleRate(),
                format.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
    }
    
    public static String lookupFourcc(AudioFormat format) {
        if (format.getSampleSizeInBits() == 16 && !format.isBigEndian())
            return "sowt";
        else if (format.getSampleSizeInBits() == 24)
            return "in24";
        else
            throw new NotSupportedException("Audio format " + format + " is not supported.");
    }

    @Override
    public void addFrame(Packet outPacket) throws IOException {
        addSamples(outPacket.getData().duplicate());
    }

    public void addSamples(ByteBuffer buffer) throws IOException {
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
                && chunkDuration * tgtChunkDuration.getDen() >= tgtChunkDuration.getNum() * _timescale) {
            outChunk();
        }
    }

    private void outChunk() throws IOException {
        if (framesInCurChunk == 0)
            return;

        chunkOffsets.add(out.position());

        for (ByteBuffer b : curChunk) {
            out.write(b);
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

        TrakBox trak = TrakBox.createTrakBox();
        Size dd = getDisplayDimensions();
        TrackHeaderBox tkhd = TrackHeaderBox.createTrackHeaderBox(trackId,
                ((long) mvhd.getTimescale() * totalFrames * frameDuration) / _timescale, dd.getWidth(), dd.getHeight(),
                new Date().getTime(), new Date().getTime(), 1.0f, (short) 0, 0,
                new int[] { 0x10000, 0, 0, 0, 0x10000, 0, 0, 0, 0x40000000 });
        tkhd.setFlags(0xf);
        trak.add(tkhd);

        tapt(trak);

        MediaBox media = MediaBox.createMediaBox();
        trak.add(media);
        media.add(MediaHeaderBox.createMediaHeaderBox(_timescale, totalFrames * frameDuration, 0, new Date().getTime(),
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

        putEdits(trak);
        putName(trak);

        stbl.add(SampleDescriptionBox.createSampleDescriptionBox(sampleEntries.toArray(new SampleEntry[0])));
        stbl.add(SampleToChunkBox.createSampleToChunkBox(samplesInChunks.toArray(new SampleToChunkEntry[0])));
        stbl.add(SampleSizesBox.createSampleSizesBox(frameSize, totalFrames));
        stbl.add(TimeToSampleBox
                .createTimeToSampleBox(new TimeToSampleEntry[] { new TimeToSampleEntry(totalFrames, frameDuration) }));
        stbl.add(ChunkOffsets64Box.createChunkOffsets64Box(chunkOffsets.toArray()));

        return trak;
    }

    public long getTrackTotalDuration() {
        return totalFrames * frameDuration;
    }
}