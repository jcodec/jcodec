package org.jcodec.containers.mp4.demuxer;

import static org.jcodec.containers.mp4.QTTimeUtil.mediaToEdited;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet.FrameType;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.CompositionOffsetsBox;
import org.jcodec.containers.mp4.boxes.CompositionOffsetsBox.Entry;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.SampleSizesBox;
import org.jcodec.containers.mp4.boxes.SyncSamplesBox;
import org.jcodec.containers.mp4.boxes.TrakBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Regular MP4 track containing frames
 * 
 * @author The JCodec project
 * 
 */
public class MP4DemuxerTrack extends AbstractMP4DemuxerTrack {

    private int[] sizes;

    private long offInChunk;

    private int noInChunk;

    private int[] syncSamples;
    private int[] partialSync;
    private int ssOff;
    private int psOff;

    private Entry[] compOffsets;
    private int cttsInd;
    private int cttsSubInd;

    private SeekableByteChannel input;

    private MovieBox movie;

    public MP4DemuxerTrack(MovieBox mov, TrakBox trak, SeekableByteChannel input) {
        super(trak);
        this.input = input;
        this.movie = mov;
        SampleSizesBox stsz = NodeBox.findFirstPath(trak, SampleSizesBox.class, Box.path("mdia.minf.stbl.stsz"));
        SyncSamplesBox stss = NodeBox.findFirstPath(trak, SyncSamplesBox.class, Box.path("mdia.minf.stbl.stss"));
        SyncSamplesBox stps = NodeBox.findFirstPath(trak, SyncSamplesBox.class, Box.path("mdia.minf.stbl.stps"));
        CompositionOffsetsBox ctts = NodeBox.findFirstPath(trak, CompositionOffsetsBox.class, Box.path("mdia.minf.stbl.ctts"));
        compOffsets = ctts == null ? null : ctts.getEntries();
        if (stss != null) {
            syncSamples = stss.getSyncSamples();
        }
        if (stps != null) {
            partialSync = stps.getSyncSamples();
        }

        if (stsz.getDefaultSize() != 0) {
            sizes = new int[stsz.getCount()];
            Arrays.fill(sizes, stsz.getDefaultSize());
        } else
            sizes = stsz.getSizes();
    }

    @Override
    public synchronized MP4Packet nextFrame() throws IOException {
        if (curFrame >= sizes.length)
            return null;
        int size = sizes[(int) curFrame];

        return getNextFrame(ByteBuffer.allocate(size));
    }

    @Override
    public synchronized MP4Packet getNextFrame(ByteBuffer storage) throws IOException {

        if (curFrame >= sizes.length)
            return null;
        int size = sizes[(int) curFrame];

        if (storage != null && storage.remaining() < size) {
            throw new IllegalArgumentException("Buffer size is not enough to fit a packet");
        }

        long pktPos = chunkOffsets[Math.min(chunkOffsets.length - 1, stcoInd)] + offInChunk;

        ByteBuffer result = readPacketData(input, storage, pktPos, size);

        if (result != null && result.remaining() < size)
            return null;

        int duration = timeToSamples[sttsInd].getSampleDuration();

        boolean sync = syncSamples == null;

        if (syncSamples != null && ssOff < syncSamples.length && (curFrame + 1) == syncSamples[ssOff]) {
            sync = true;
            ssOff++;
        }

        boolean psync = false;
        if (partialSync != null && psOff < partialSync.length && (curFrame + 1) == partialSync[psOff]) {
            psync = true;
            psOff++;
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

        ByteBuffer data = result == null ? null : convertPacket(result);
        long _pts = mediaToEdited(box, realPts, movie.getTimescale());
        FrameType ftype = sync ? FrameType.KEY : FrameType.INTER;
        int entryNo = sampleToChunks[stscInd].getEntry() - 1;
        MP4Packet pkt = new MP4Packet(data, _pts, timescale, duration, curFrame, ftype, null, 0, realPts, entryNo,
                pktPos, size, psync);

        offInChunk += size;

        curFrame++;
        noInChunk++;
        if (noInChunk >= sampleToChunks[stscInd].getCount()) {
            noInChunk = 0;
            offInChunk = 0;

            nextChunk();
        }
        shiftPts(1);

        return pkt;
    }

    @Override
    public boolean gotoSyncFrame(long frameNo) {
        if (syncSamples == null)
            return gotoFrame(frameNo);
        if (frameNo < 0)
            throw new IllegalArgumentException("negative frame number");
        if (frameNo >= getFrameCount())
            return false;
        if (frameNo == curFrame)
            return true;
        for (int i = 0; i < syncSamples.length; i++) {
            if (syncSamples[i] - 1 > frameNo)
                return gotoFrame(syncSamples[i - 1] - 1);
        }
        return gotoFrame(syncSamples[syncSamples.length - 1] - 1);
    }

    @Override
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

        if (syncSamples != null)
            for (ssOff = 0; ssOff < syncSamples.length && syncSamples[ssOff] < curFrame + 1; ssOff++)
                ;

        if (partialSync != null)
            for (psOff = 0; psOff < partialSync.length && partialSync[psOff] < curFrame + 1; psOff++)
                ;

    }

    @Override
    public long getFrameCount() {
        return sizes.length;
    }
}
