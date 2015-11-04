package org.jcodec.containers.mp4.demuxer;

import static org.jcodec.containers.mp4.QTTimeUtil.mediaToEdited;
import static org.jcodec.containers.mp4.boxes.Box.findFirst;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.TrackType;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.CompositionOffsetsBox;
import org.jcodec.containers.mp4.boxes.CompositionOffsetsBox.Entry;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.SampleSizesBox;
import org.jcodec.containers.mp4.boxes.SyncSamplesBox;
import org.jcodec.containers.mp4.boxes.TrakBox;

import static org.jcodec.common.DemuxerTrackMeta.Type.*;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Regular MP4 track containing frames
 * 
 * @author The JCodec project
 * 
 */
public class FramesMP4DemuxerTrack extends AbstractMP4DemuxerTrack {

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

    public FramesMP4DemuxerTrack(MovieBox mov, TrakBox trak, SeekableByteChannel input) {
        super(trak);
        this.input = input;
        this.movie = mov;

        SampleSizesBox stsz = findFirst(trak, SampleSizesBox.class, "mdia", "minf", "stbl", "stsz");
        SyncSamplesBox stss = Box.findFirst(trak, SyncSamplesBox.class, "mdia", "minf", "stbl", "stss");
        SyncSamplesBox stps = Box.findFirst(trak, SyncSamplesBox.class, "mdia", "minf", "stbl", "stps");
        CompositionOffsetsBox ctts = Box.findFirst(trak, CompositionOffsetsBox.class, "mdia", "minf", "stbl", "ctts");
        compOffsets = ctts == null ? null : ctts.getEntries();
        if (stss != null) {
            syncSamples = stss.getSyncSamples();
        }
        if (stps != null) {
            partialSync = stps.getSyncSamples();
        }

        sizes = stsz.getSizes();
    }

    public synchronized MP4Packet nextFrame() throws IOException {
        if (curFrame >= sizes.length)
            return null;
        int size = sizes[(int) curFrame];

        return nextFrame(ByteBuffer.allocate(size));
    }

    public synchronized MP4Packet nextFrame(ByteBuffer storage) throws IOException {

        if (curFrame >= sizes.length)
            return null;
        int size = sizes[(int) curFrame];

        if (storage != null && storage.remaining() < size) {
            throw new IllegalArgumentException("Buffer size is not enough to fit a packet");
        }

        long pktPos = chunkOffsets[stcoInd] + offInChunk;

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

        MP4Packet pkt = new MP4Packet(result, mediaToEdited(box, realPts, movie.getTimescale()), timescale, duration,
                curFrame, sync, null, realPts, sampleToChunks[stscInd].getEntry() - 1, pktPos, size, psync);

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
        else {
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

        if (syncSamples != null)
            for (ssOff = 0; ssOff < syncSamples.length && syncSamples[ssOff] < curFrame + 1; ssOff++)
                ;

        if (partialSync != null)
            for (psOff = 0; psOff < partialSync.length && partialSync[psOff] < curFrame + 1; psOff++)
                ;

    }

    public long getFrameCount() {
        return sizes.length;
    }

    @Override
    public DemuxerTrackMeta getMeta() {
        int[] seekFrames;
        if (syncSamples == null) {
            //all frames are I-frames
            seekFrames  = new int[(int)getFrameCount()];
            for (int i = 0; i < seekFrames.length; i++) {
                seekFrames[i] = i;
            }
        } else {
            seekFrames = Arrays.copyOf(syncSamples, syncSamples.length);
            for (int i = 0; i < seekFrames.length; i++)
                seekFrames[i]--;
        }

        TrackType type = getType();
        return new DemuxerTrackMeta(type == TrackType.VIDEO ? VIDEO : (type == TrackType.SOUND ? AUDIO : OTHER),
                seekFrames, sizes.length, (double) duration / timescale, box.getCodedSize());
    }
}