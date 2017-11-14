package org.jcodec.samples.streaming;

import static org.jcodec.containers.mps.MPSUtils.videoStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.TapeTimecode;

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
                            : false, 30);
        }

        public void setTapeTimecode(int hours, int minutes, int seconds, int frames, boolean dropFrame) {
            this.timeCode = (dropFrame ? 1 : 0) | (frames << 1) | (seconds << 7) | (minutes << 13) | (hours << 19);
        }
    }

    public static class StreamEntry {
        public int sid;
        public List<ByteBuffer> extraData;
        public List<FrameEntry> frames;

        private StreamEntry(int sid) {
            this.sid = sid;
            this.extraData = new ArrayList<ByteBuffer>(1);
            this.frames = Collections.synchronizedList(new ArrayList<FrameEntry>(5000));
        }

        public StreamEntry(int sid, List<ByteBuffer> extraData, List<FrameEntry> frames) {
            this.sid = sid;
            this.extraData = extraData;
            this.frames = frames;
        }

        public FrameEntry addAudio(long offset, long pts, int duration) {
            FrameEntry e = new FrameEntry(offset, pts, duration, frames.size());

            frames.add(e);

            return e;
        }

        public VideoFrameEntry addVideo(long offset, long pts, int duration, ByteBuffer ed, int gopId, int timecode,
                short displayOrder, byte frameType) {
            if (ed != null && (extraData.size() == 0 || !ed.equals(extraData.get(extraData.size() - 1)))) {
                extraData.add(ed);
            }

            VideoFrameEntry e = new VideoFrameEntry(offset, pts, duration, frames.size(), extraData.size() - 1,
                    gopId == -1 ? frames.size() : gopId, timecode, displayOrder, frameType);

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

    public VideoFrameEntry addVideo(int streamId, long offset, long pts, int duration, ByteBuffer seqHeader, int gopId,
            int timecode, short displayOrder, byte frameType) {
        StreamEntry stream = streams.get(streamId);
        if (stream == null) {
            stream = new StreamEntry(streamId);
            streams.put(streamId, stream);
        }

        return stream.addVideo(offset, pts, duration, seqHeader, gopId, timecode, displayOrder, frameType);
    }

    public static MTSIndex read(File indexFile) throws IOException {
        FileChannel is = new FileInputStream(indexFile).getChannel();
        try {
            List<StreamEntry> streams = new ArrayList<StreamEntry>();
            int nStreams = NIOUtils.readByte(is) & 0xff;
            for (int i = 0; i < nStreams; i++) {
                ArrayList<ByteBuffer> extraData = new ArrayList<ByteBuffer>();
                int size = NIOUtils.readInt(is);
                long pos = is.position();
                ByteBuffer buf = NIOUtils.fetchFromChannel(is, size);

                int sid = buf.get() & 0xff;
                while (buf.get() == 0) {
                    extraData.add(NIOUtils.read(buf, buf.getShort() & 0xffff));
                }
                ArrayList<FrameEntry> frames = new ArrayList<FrameEntry>();
                for (int j = 0; j < buf.getInt(); j++) {
                    if (videoStream(sid)) {
                        VideoFrameEntry e = new VideoFrameEntry(buf.getLong(), buf.getLong(), buf.getInt(), j,
                                buf.getInt(), buf.getInt(), buf.getInt(), (short) 0, (byte) 0);
                        int doft = buf.getShort() & 0xffff;
                        e.displayOrder = (short) (doft & 0x3ff);
                        e.frameType = (byte) (doft >> 10);
                        frames.add(e);
                    } else {
                        frames.add(new FrameEntry(buf.getLong(), buf.getLong(), buf.getInt(), j));
                    }
                }
                Assert.assertEquals(is.position() - pos, size);
                streams.add(new StreamEntry(sid, extraData, frames));
            }
            return new MTSIndex(streams);
        } finally {
            NIOUtils.closeQuietly(is);
        }
    }

    public ByteBuffer getExtraData(int sid, int ind) {
        return streams.get(sid).extraData.get(ind);
    }

    public void write(File indexFile) throws IOException {

        int size = 1024;
        for (StreamEntry streamEntry : streams.values()) {
            size += streamEntry.frames.size() * 36;
        }
        ByteBuffer buf = ByteBuffer.allocate(size);

        buf.put((byte) streams.size());
        for (StreamEntry streamEntry : streams.values()) {
            ByteBuffer fork = buf.duplicate();
            buf.putInt(0);
            writeStream(streamEntry, buf);
            fork.putInt(buf.position() - fork.position() - 8);
        }
        buf.flip();

        NIOUtils.writeTo(buf, indexFile);
    }

    private void writeStream(StreamEntry streamEntry, ByteBuffer out) {
        out.put((byte) streamEntry.sid);
        for (ByteBuffer buffer : streamEntry.extraData) {
            out.put((byte) 0);
            out.putShort((short) buffer.remaining());
            NIOUtils.write(out, buffer);
        }
        out.put((byte) 1);
        out.putInt(streamEntry.frames.size());
        for (FrameEntry frameEntry : streamEntry.frames) {
            writeFrame(frameEntry, out);
        }
    }

    private void writeFrame(FrameEntry frameEntry, ByteBuffer out) {
        out.putLong(frameEntry.dataOffset);
        out.putLong(frameEntry.pts);
        out.putInt(frameEntry.duration);
        if (frameEntry instanceof VideoFrameEntry) {
            VideoFrameEntry vfe = (VideoFrameEntry) frameEntry;
            out.putInt(vfe.edInd);
            out.putInt(vfe.gopId);
            out.putInt(vfe.timeCode);
            out.putShort((short) (vfe.displayOrder | (vfe.frameType << 10)));
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