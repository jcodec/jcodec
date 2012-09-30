package org.jcodec.samples.streaming;

import static org.jcodec.common.io.ReaderBE.readInt16;
import static org.jcodec.common.io.ReaderBE.readInt32;
import static org.jcodec.common.io.ReaderBE.readInt64;
import static org.jcodec.containers.mps.MPSDemuxer.videoStream;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.jcodec.common.io.Buffer;
import org.jcodec.common.io.RandomAccessFileInputStream;
import org.jcodec.common.io.RandomAccessFileOutputStream;
import org.jcodec.common.io.ReaderBE;
import org.jcodec.common.model.TapeTimecode;
import org.jcodec.containers.mps.MPSDemuxer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * MPEG TS file index
 * 
 * @author The JCodec project
 * 
 */
public class MTSIndex {

    public static class FrameEntry {
        public long dataOffset;
        public long pts;
        public int duration;
        public int frameNo;

        public FrameEntry(long dataOffset, long pts, int duration, int frameNo) {
            this.dataOffset = dataOffset;
            this.pts = pts;
            this.duration = duration;
            this.frameNo = frameNo;
        }
    }

    public static class VideoFrameEntry extends FrameEntry {
        public int edInd;
        public int gopId;
        public int timeCode;
        public short displayOrder;
        public byte frameType;

        public VideoFrameEntry(long dataOffset, long pts, int duration, int frameNo, int edInd, int gopId,
                int timeCode, short displayOrder, byte frameType) {
            super(dataOffset, pts, duration, frameNo);
            this.edInd = edInd;
            this.gopId = gopId;
            this.timeCode = timeCode;
            this.displayOrder = displayOrder;
            this.frameType = frameType;
        }

        public int getDisplayOrder() {
            return displayOrder;
        }

        public TapeTimecode getTapeTimecode() {
            return new TapeTimecode((short) ((timeCode >> 19) & 0x3f), (byte) ((timeCode >> 13) & 0x3f),
                    (byte) ((timeCode >> 7) & 0x3f), (byte) ((timeCode >> 1) & 0x3f), (timeCode & 0x1) == 1 ? true
                            : false);
        }

        public void setTapeTimecode(int hours, int minutes, int seconds, int frames, boolean dropFrame) {
            this.timeCode = (dropFrame ? 1 : 0) | (frames << 1) | (seconds << 7) | (minutes << 13) | (hours << 19);
        }
    }

    public static class StreamEntry {
        public int sid;
        public List<Buffer> extraData;
        public List<FrameEntry> frames;

        private StreamEntry(int sid) {
            this.sid = sid;
            this.extraData = new ArrayList<Buffer>(1);
            this.frames = Collections.synchronizedList(new ArrayList<FrameEntry>(5000));
        }

        public StreamEntry(int sid, List<Buffer> extraData, List<FrameEntry> frames) {
            this.sid = sid;
            this.extraData = extraData;
            this.frames = frames;
        }

        public FrameEntry addAudio(long offset, long pts, int duration) {
            FrameEntry e = new FrameEntry(offset, pts, duration, frames.size());

            frames.add(e);

            return e;
        }

        public VideoFrameEntry addVideo(long offset, long pts, int duration, Buffer ed, int gopId, int timecode,
                short displayOrder, byte frameType) {
            if (ed != null && (extraData.size() == 0 || !ed.equals(extraData.get(extraData.size() - 1)))) {
                extraData.add(ed);
            }

            VideoFrameEntry e = new VideoFrameEntry(offset, pts, duration, frames.size(), extraData.size() - 1, gopId,
                    timecode, displayOrder, frameType);

            frames.add(e);

            return e;
        }

        public FrameEntry last() {
            return frames.isEmpty() ? null : frames.get(frames.size() - 1);
        }
    }

    private Map<Integer, StreamEntry> streams;

    public MTSIndex() {
        this.streams = Collections.synchronizedMap(new HashMap<Integer, StreamEntry>());
    }

    public MTSIndex(List<StreamEntry> streams) {
        this.streams = new HashMap<Integer, StreamEntry>();
        for (StreamEntry se : streams) {
            this.streams.put(se.sid, se);
        }
    }

    public Set<Integer> getStreamIds() {
        return streams.keySet();
    }

    public FrameEntry search(int sid, long pts) {
        FrameEntry prev = null;
        List<FrameEntry> frames = streams.get(sid).frames;
        synchronized (frames) {
            for (FrameEntry indexEntry : frames) {
                if (indexEntry.pts <= pts)
                    prev = indexEntry;
                else if (indexEntry.pts >= pts) {
                    break;
                }
            }
            FrameEntry lastFrame = frames.size() > 0 ? frames.get(frames.size() - 1) : null;
            return prev != lastFrame ? prev : (prev != null && pts == prev.pts ? prev : null);
        }
    }

    public FrameEntry frame(int sid, int frame) {
        List<FrameEntry> list = streams.get(sid).frames;
        synchronized (list) {
            return frame < list.size() ? (frame >= 0 ? list.get(frame) : null) : null;
        }
    }

    public FrameEntry addAudio(int streamId, long offset, long pts, int duration) {
        StreamEntry stream = streams.get(streamId);
        if (stream == null) {
            stream = new StreamEntry(streamId);
            streams.put(streamId, stream);
        }

        return stream.addAudio(offset, pts, duration);
    }

    public VideoFrameEntry addVideo(int streamId, long offset, long pts, int duration, Buffer seqHeader, int gopId,
            int timecode, short displayOrder, byte frameType) {
        StreamEntry stream = streams.get(streamId);
        if (stream == null) {
            stream = new StreamEntry(streamId);
            streams.put(streamId, stream);
        }

        return stream.addVideo(offset, pts, duration, seqHeader, gopId, timecode, displayOrder, frameType);
    }

    public static MTSIndex read(File indexFile) throws IOException {
        RandomAccessFileInputStream is = new RandomAccessFileInputStream(indexFile);
        try {
            List<StreamEntry> streams = new ArrayList<StreamEntry>();
            int nStreams = is.read();
            for (int i = 0; i < nStreams; i++) {
                ArrayList<Buffer> extraData = new ArrayList<Buffer>();
                long size = readInt64(is);
                long pos = is.getPos();
                int sid = is.read();
                while (is.read() == 0) {
                    Buffer buffer = Buffer.fetchFrom(is, (int) readInt16(is));
                    extraData.add(buffer);
                }
                ArrayList<FrameEntry> frames = new ArrayList<FrameEntry>();
                int nFrames = (int) readInt32(is);
                for (int j = 0; j < nFrames; j++) {
                    if (videoStream(sid)) {
                        VideoFrameEntry e = new VideoFrameEntry(readInt64(is), ReaderBE.readInt64(is),
                                (int) readInt32(is), j, (int) readInt32(is), (int) readInt32(is), (int) readInt32(is),
                                (short) 0, (byte) 0);
                        short doft = (short) readInt16(is);
                        e.displayOrder = (short) (doft & 0x3ff);
                        e.frameType = (byte) (doft >> 10);
                        frames.add(e);
                    } else {
                        frames.add(new FrameEntry(readInt64(is), ReaderBE.readInt64(is), (int) readInt32(is), j));
                    }
                }
                Assert.assertEquals(is.getPos() - pos, size);
                streams.add(new StreamEntry(sid, extraData, frames));
            }
            return new MTSIndex(streams);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public Buffer getExtraData(int sid, int ind) {
        return streams.get(sid).extraData.get(ind);
    }

    public void write(File indexFile) throws IOException {
        RandomAccessFileOutputStream out = null;
        try {
            out = new RandomAccessFileOutputStream(indexFile);
            out.writeByte(streams.size());
            for (StreamEntry streamEntry : streams.values()) {
                long before = out.getPos();
                out.writeLong(0);
                writeStream(streamEntry, out);
                long after = out.getPos();
                out.seek(before);
                out.writeLong(after - before - 8);
                out.seek(after);
            }
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    private void writeStream(StreamEntry streamEntry, RandomAccessFileOutputStream out) throws IOException {
        out.writeByte(streamEntry.sid);
        for (Buffer buffer : streamEntry.extraData) {
            out.writeByte(0);
            out.writeShort(buffer.remaining());
            buffer.writeTo(out);
        }
        out.writeByte(1);
        out.writeInt(streamEntry.frames.size());
        for (FrameEntry frameEntry : streamEntry.frames) {
            writeFrame(frameEntry, out);
        }
    }

    private void writeFrame(FrameEntry frameEntry, RandomAccessFileOutputStream out) throws IOException {
        out.writeLong(frameEntry.dataOffset);
        out.writeLong(frameEntry.pts);
        out.writeInt(frameEntry.duration);
        if (frameEntry instanceof VideoFrameEntry) {
            VideoFrameEntry vfe = (VideoFrameEntry) frameEntry;
            out.writeInt(vfe.edInd);
            out.writeInt(vfe.gopId);
            out.writeInt(vfe.timeCode);
            out.writeShort(vfe.displayOrder | (vfe.frameType << 10));
        }
    }

    public int getNumFrames(int sid) {
        return streams.get(sid) != null ? streams.get(sid).frames.size() : 0;
    }

    public FrameEntry last(int sid) {
        StreamEntry stream = streams.get(sid);
        return stream == null ? null : stream.last();
    }
}