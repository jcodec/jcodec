package org.jcodec.containers.mp4.demuxer;

import static org.jcodec.containers.mp4.boxes.Box.findFirst;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.QTTimeUtil;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.SampleSizesBox;
import org.jcodec.containers.mp4.boxes.TrakBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Specialized demuxer track for PCM audio samples
 * 
 * Always reads one chunk of frames at a time, except for after seek. After seek
 * the beginning of chunk before the seek point is not read effectivaly reading
 * PCM frame from exactly the frame seek was performed to.
 * 
 * Packet size depends on underlying container PCM chunk sizes.
 * 
 * @author The JCodec project
 * 
 */
public class PCMMP4DemuxerTrack extends AbstractMP4DemuxerTrack {

    private int defaultSampleSize;

    private int posShift;

    protected int totalFrames;

    private SeekableByteChannel input;

    private MovieBox movie;

    public PCMMP4DemuxerTrack(MovieBox movie, TrakBox trak, SeekableByteChannel input) {
        super(trak);

        this.movie = movie;
        this.input = input;

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

    public Packet nextFrame() throws IOException {
        int frameSize = getFrameSize();
        int chSize = sampleToChunks[stscInd].getCount() * frameSize - posShift;

        return nextFrame(ByteBuffer.allocate(chSize));
    }

    @Override
    public synchronized MP4Packet nextFrame(ByteBuffer buffer) throws IOException {
        if (stcoInd >= chunkOffsets.length)
            return null;
        int frameSize = getFrameSize();

        int se = sampleToChunks[stscInd].getEntry();
        int chSize = sampleToChunks[stscInd].getCount() * frameSize;

        long pktOff = chunkOffsets[stcoInd] + posShift;
        int pktSize = chSize - posShift;
        ByteBuffer result = readPacketData(input, buffer, pktOff, pktSize);

        long ptsRem = pts;
        int doneFrames = pktSize / frameSize;
        shiftPts(doneFrames);

        MP4Packet pkt = new MP4Packet(result, QTTimeUtil.mediaToEdited(box, ptsRem, movie.getTimescale()), timescale,
                (int) (pts - ptsRem), curFrame, true, null, ptsRem, se - 1, pktOff, pktSize, true);

        curFrame += doneFrames;

        posShift = 0;

        ++stcoInd;
        if (stscInd < sampleToChunks.length - 1 && (stcoInd + 1) == sampleToChunks[stscInd + 1].getFirst())
            stscInd++;

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

    @Override
    public DemuxerTrackMeta getMeta() {
        return new DemuxerTrackMeta(DemuxerTrackMeta.Type.AUDIO, null, totalFrames, (double) duration / timescale, null);
    }
}