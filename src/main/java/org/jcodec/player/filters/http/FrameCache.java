package org.jcodec.player.filters.http;

import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.io.Buffer;
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

    private RandomAccessFile fd;
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

        public IndexRecord(int frameNo, long pos, int dataLen, long pts, int duration, boolean key, TapeTimecode tapeTimecode) {
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
        fd = new RandomAccessFile(cacheWhere, "rwd");
        loadData(fd);
        addDataSegment();
    }

    public void close() throws IOException {
        fd.close();
    }

    public void loadData(RandomAccessFile f) throws IOException {
        index = new TIntObjectHashMap<IndexRecord>();
        pts2frame = new TLongIntHashMap(1, 0.5f, -1, -1);

        while (f.getFilePointer() + 5 <= f.length()) {
            int segmentType = f.read();
            int segmentSize = f.readInt();

            if (f.getFilePointer() + segmentSize > f.length()) {
                // invelid segment, file corruption starts here
                f.seek(f.getFilePointer() - 5);
                break;
            }

            if (segmentType == DATA_SEGMENT) {
                dataSegments.add(f.getFilePointer());
                f.seek(f.getFilePointer() + segmentSize);
            } else if (segmentType == INDEX_SEGMENT) {
                Buffer buffer = Buffer.fetchFrom(f, segmentSize);
                DataInput dinp = buffer.dinp();
                while (buffer.remaining() >= 29) {
                    int frameNo = dinp.readInt();
                    IndexRecord rec = new IndexRecord(frameNo, dinp.readLong(), dinp.readInt(), dinp.readLong(),
                            dinp.readInt(), dinp.readByte() == 1, null);
                    index.put(frameNo, rec);
                    pts2frame.put(rec.pts, frameNo);
                }
            } else {
                // invalid segment, file corruption starts here
                f.seek(f.getFilePointer() - 5);
                break;
            }
        }
    }

    public Packet getFrame(int frameNo, byte[] buffer) throws IOException {
        IndexRecord record = index.get(frameNo);
        if (record == null)
            return null;

        synchronized (fd) {

            fd.seek(record.pos);

            int remaining = record.dataLen;
            int dsOff = (int) (record.pos - getDataSegmentOff(record));
            while (remaining > 0) {
                int toRead = Math.min(remaining, DATASEG_SIZE - dsOff);
                fd.readFully(buffer, record.dataLen - remaining, toRead);
                remaining -= toRead;
                if (remaining > 0) {
                    skipToDataseg();
                    dsOff = 0;
                }
            }

            return new Packet(new Buffer(buffer, 0, record.dataLen), record.pts, 0, record.duration, frameNo,
                    record.key, record.tapeTimecode);
        }
    }

    private void skipToDataseg() throws IOException {
        while (fd.getFilePointer() + 5 <= fd.length()) {
            int segType = fd.read();
            int segSize = fd.readInt();
            if (segType != DATA_SEGMENT && segType != INDEX_SEGMENT)
                throw new IOException("Invalid segment: " + segType);
            if (segType == DATA_SEGMENT)
                return;
            fd.seek(fd.getFilePointer() + segSize);
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
        synchronized (fd) {
            fd.seek(dataSegments.get(dataSegments.size() - 1) + dsFill);
            long pos = fd.getFilePointer();
            Buffer data = packet.getData().fork();

            IndexRecord record = new IndexRecord((int) packet.getFrameNo(), pos, packet.getData().remaining(),
                    packet.getPts(), (int) packet.getDuration(), packet.isKeyFrame(), packet.getTapeTimecode());
            index.put((int) packet.getFrameNo(), record);

            while (data.remaining() > 0) {
                Buffer piece = data.read(Math.min(data.remaining(), DATASEG_SIZE - dsFill));
                piece.writeTo(fd);
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
    }

    private void addDataSegment() throws IOException {
        fd.write(DATA_SEGMENT);
        fd.writeInt(DATASEG_SIZE);
        dataSegments.add(fd.getFilePointer());
    }

    private void writeIndex(RandomAccessFile fd, List<IndexRecord> dsFrames) throws IOException {
        fd.write(INDEX_SEGMENT);
        fd.writeInt(dsFrames.size() * 29);
        Buffer buf = new Buffer(dsFrames.size() * 29), fork = buf.fork();
        DataOutput dout = buf.dout();
        for (IndexRecord indexRecord : dsFrames) {
            dout.writeInt(indexRecord.frameNo);
            dout.writeLong(indexRecord.pos);
            dout.writeInt(indexRecord.dataLen);
            dout.writeLong(indexRecord.pts);
            dout.writeInt(indexRecord.duration);
            dout.write(indexRecord.key ? 1 : 0);
        }
        fork.writeTo(fd);
    }

    public boolean hasFrame(int i) {
        return index.containsKey(i);
    }

    public MediaInfo getMediaInfo() {
        return mediaInfo;
    }

    public int[] getFrames() {
        return index.keys();
    }
}