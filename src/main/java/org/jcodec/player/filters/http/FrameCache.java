package org.jcodec.player.filters.http;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.NIOUtils;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.TapeTimecode;
import org.jcodec.player.filters.MediaInfo;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Frame cache, improved version
 * 
 * @author The JCodec project
 * 
 */
public class FrameCache {

    private static int DATA_SEGMENT = 87;
    private static int INDEX_SEGMENT = 113;

    private static int DATASEG_SIZE = 1024 * 1024 * 10;

    private FileChannel fd;
    private TIntObjectHashMap<IndexRecord> index;
    private List<Long> dataSegments = new ArrayList<Long>();
    private MediaInfo mediaInfo;
    private TLongIntMap pts2frame;

    private int dsFill;
    private List<IndexRecord> dsFrames = new ArrayList<IndexRecord>();

    private static class IndexRecord {
        public int frameNo;
        public long pos;
        public int dataLen;
        public long pts;
        public int duration;
        public boolean key;
        public TapeTimecode tapeTimecode;

        public IndexRecord(int frameNo, long pos, int dataLen, long pts, int duration, boolean key,
                TapeTimecode tapeTimecode) {
            this.frameNo = frameNo;
            this.pos = pos;
            this.dataLen = dataLen;
            this.pts = pts;
            this.duration = duration;
            this.key = key;
            this.tapeTimecode = tapeTimecode;
        }
    }

    public FrameCache(File cacheWhere) throws IOException {
        fd = new FileInputStream(cacheWhere).getChannel();
        loadData(fd);
        addDataSegment();
    }

    public void close() throws IOException {
        synchronized (fd) {
            fd.close();
            fd = null;
        }
    }

    public void loadData(FileChannel f) throws IOException {
        index = new TIntObjectHashMap<IndexRecord>();
        pts2frame = new TLongIntHashMap(1, 0.5f, -1, -1);

        while (f.position() + 5 <= f.size()) {
            int segmentType = NIOUtils.readByte(f);
            int segmentSize = NIOUtils.readInt(f);

            if (f.position() + segmentSize > f.size()) {
                // invelid segment, file corruption starts here
                f.position(f.position() - 5);
                break;
            }

            if (segmentType == DATA_SEGMENT) {
                dataSegments.add(f.position());
                f.position(f.position() + segmentSize);
            } else if (segmentType == INDEX_SEGMENT) {
                ByteBuffer buffer = NIOUtils.fetchFrom(f, segmentSize);
                while (buffer.remaining() >= 29) {
                    int frameNo = buffer.getInt();
                    IndexRecord rec = new IndexRecord(frameNo, buffer.getLong(), buffer.getInt(), buffer.getLong(),
                            buffer.getInt(), buffer.get() == 1, null);
                    index.put(frameNo, rec);
                    pts2frame.put(rec.pts, frameNo);
                }
            } else {
                // invalid segment, file corruption starts here
                f.position(f.position() - 5);
                break;
            }
        }
    }

    public Packet getFrame(int frameNo, ByteBuffer buffer) throws IOException {

        IndexRecord record = index.get(frameNo);
        if (record == null)
            return null;

        ByteBuffer out = buffer.duplicate();
        out.limit(out.position() + record.dataLen);

        if (fd == null)
            return null;
        synchronized (fd) {
            if (fd == null)
                return null;

            fd.position(record.pos);

            int dsOff = (int) (record.pos - getDataSegmentOff(record));
            while (out.remaining() > 0) {
                int toRead = Math.min(out.remaining(), DATASEG_SIZE - dsOff);
                NIOUtils.read(fd, out, toRead);
                if (out.remaining() > 0) {
                    skipToDataseg();
                    dsOff = 0;
                }
            }

            out.flip();

            return new Packet(out, record.pts, 0, record.duration, frameNo, record.key, record.tapeTimecode);
        }
    }

    private void skipToDataseg() throws IOException {
        while (fd.position() + 5 <= fd.size()) {
            int segType = NIOUtils.readByte(fd);
            int segSize = NIOUtils.readInt(fd);
            if (segType != DATA_SEGMENT && segType != INDEX_SEGMENT)
                throw new IOException("Invalid segment: " + segType);
            if (segType == DATA_SEGMENT)
                return;
            fd.position(fd.position() + segSize);
        }
    }

    private long getDataSegmentOff(IndexRecord record) {
        long found = dataSegments.get(0);
        for (long offset : dataSegments) {
            if (offset > record.pos)
                break;
            found = offset;
        }
        return found;
    }

    public int pts2frame(long pts) {
        synchronized (pts2frame) {

            long[] keys = pts2frame.keys();

            int nearestBefore = -1, nearestAfter = -1;
            for (int i = 0; i < keys.length; i++) {
                if (keys[i] <= pts && (nearestBefore == -1 || keys[i] > keys[nearestBefore]))
                    nearestBefore = i;
                if (keys[i] >= pts && (nearestAfter == -1 || keys[i] < keys[nearestAfter]))
                    nearestAfter = i;
            }

            int[] values = pts2frame.values();
            if (nearestBefore != -1 && nearestAfter != -1 && values[nearestAfter] - values[nearestBefore] <= 1)
                return values[nearestBefore];
            return -1;
        }
    }

    public void addFrame(Packet packet) throws IOException {
        if (fd == null)
            return;
        synchronized (fd) {
            if (fd == null)
                return;
            fd.position(dataSegments.get(dataSegments.size() - 1) + dsFill);
            long pos = fd.position();
            ByteBuffer data = packet.getData().duplicate();

            IndexRecord record = new IndexRecord((int) packet.getFrameNo(), pos, packet.getData().remaining(),
                    packet.getPts(), (int) packet.getDuration(), packet.isKeyFrame(), packet.getTapeTimecode());
            index.put((int) packet.getFrameNo(), record);

            while (data.remaining() > 0) {
                ByteBuffer piece = NIOUtils.read(data, Math.min(data.remaining(), DATASEG_SIZE - dsFill));
                fd.write(piece);
                dsFill += piece.remaining();

                if (dsFill == DATASEG_SIZE) {
                    if (dsFrames.size() > 0) {
                        writeIndex(fd, dsFrames);
                        dsFrames.clear();
                    }
                    dsFill = 0;
                    addDataSegment();
                }
            }

            dsFrames.add(record);
        }
        synchronized (pts2frame) {
            pts2frame.put(packet.getPts(), (int) packet.getFrameNo());
        }
        synchronized (starts) {
            updateCached((int) packet.getFrameNo());
        }
    }

    private void addDataSegment() throws IOException {
        NIOUtils.writeByte(fd, (byte) DATA_SEGMENT);
        NIOUtils.writeInt(fd, DATASEG_SIZE);
        dataSegments.add(fd.position());
    }

    private void writeIndex(FileChannel fd, List<IndexRecord> dsFrames) throws IOException {
        NIOUtils.writeByte(fd, (byte) INDEX_SEGMENT);
        NIOUtils.writeInt(fd, dsFrames.size() * 29);
        ByteBuffer buf = ByteBuffer.allocate(dsFrames.size() * 29);
        for (IndexRecord indexRecord : dsFrames) {
            buf.putInt(indexRecord.frameNo);
            buf.putLong(indexRecord.pos);
            buf.putInt(indexRecord.dataLen);
            buf.putLong(indexRecord.pts);
            buf.putInt(indexRecord.duration);
            buf.put((byte) (indexRecord.key ? 1 : 0));
        }
        buf.flip();
        fd.write(buf);
    }

    public boolean hasFrame(int i) {
        return index.containsKey(i);
    }

    public MediaInfo getMediaInfo() {
        return mediaInfo;
    }

    TIntArrayList starts = new TIntArrayList();;
    TIntArrayList ends = new TIntArrayList();

    private void updateCached(int frame) {
        for (int i = 0; i < starts.size(); i++) {
            if (frame >= starts.get(i) && frame <= ends.get(i))
                return;
            if (frame == starts.get(i) - 1) {
                starts.set(i, frame);
                return;
            }
            if (frame == ends.get(i) + 1) {
                ends.set(i, frame);
                return;
            }
        }
        starts.add(frame);
        ends.add(frame);
    }

    public int[][] getCached() {
        return new int[][] { starts.toArray(), ends.toArray() };
    }
}