package org.jcodec.movtool.streaming;

import static org.jcodec.movtool.streaming.MovieHelper.produceHeader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.containers.mp4.Brand;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Virtual movie. A movie constructed on-the-fly from virtual track data.
 * 
 * @author The JCodec project
 * 
 */
public class VirtualMovie {
    private PacketChunk[] chunks;
    private MovieSegment headerChunk;
    private long size;
    private VirtualTrack[] tracks;
    private Brand brand;

    public VirtualMovie(VirtualTrack... tracks) throws IOException {
        this(Brand.MP4, tracks);
    }

    public VirtualMovie(Brand brand, VirtualTrack... tracks) throws IOException {
        this.tracks = tracks;
        this.brand = brand;

        muxTracks();
    }

    private void muxTracks() throws IOException {
        List<PacketChunk> chch = new ArrayList<PacketChunk>();
        VirtualPacket[] heads = new VirtualPacket[tracks.length], tails = new VirtualPacket[tracks.length];

        for (int curChunk = 1;; curChunk++) {
            int min = -1;

            for (int i = 0; i < heads.length; i++) {
                if (heads[i] == null) {
                    heads[i] = tracks[i].nextPacket();
                    if (heads[i] == null)
                        continue;
                }

                min = min == -1 || heads[i].getPts() < heads[min].getPts() ? i : min;
            }
            if (min == -1)
                break;
            chch.add(new PacketChunk(heads[min], min, curChunk, size));
            if (heads[min].getDataLen() >= 0)
                size += heads[min].getDataLen();
            else
                System.err.println("WARN: Negative frame data len!!!");
            tails[min] = heads[min];
            heads[min] = tracks[min].nextPacket();
        }
        chunks = chch.toArray(new PacketChunk[chch.size()]);
        long dataSize = size;
        int headerSize = produceHeader(chunks, tracks, dataSize, brand).remaining();
        size += headerSize;
        for (PacketChunk ch : chch) {
            ch.offset(headerSize);
        }
        headerChunk = headerChunk(produceHeader(chunks, tracks, dataSize, brand));
        chunks = chch.toArray(new PacketChunk[chch.size()]);
    }

    private MovieSegment headerChunk(final ByteBuffer header) {
        return new MovieSegment() {
            public ByteBuffer getData() {
                return header.duplicate();
            }

            public int getNo() {
                return 0;
            }

            public long getPos() {
                return 0;
            }

            public int getDataLen() {
                return header.remaining();
            }
        };
    }

    public class PacketChunk implements MovieSegment {
        private VirtualPacket packet;
        private int track;
        private int no;
        private long pos;

        public PacketChunk(VirtualPacket packet, int track, int no, long pos) {
            this.packet = packet;
            this.track = track;
            this.no = no;
            this.pos = pos;
        }

        public ByteBuffer getData() throws IOException {
            return packet.getData() == null ? null : packet.getData().duplicate();
        }

        public int getNo() {
            return no;
        }

        public long getPos() {
            return pos;
        }

        public void offset(int off) {
            pos += off;
        }

        public int getDataLen() throws IOException {
            return packet.getDataLen();
        }

        public VirtualPacket getPacket() {
            return packet;
        }

        public int getTrack() {
            return track;
        }
    }

    public void close() throws IOException {
        for (VirtualTrack virtualTrack : tracks) {
            virtualTrack.close();
        }
    }

    public MovieSegment getPacketAt(long position) throws IOException {
        if (position >= 0 && position < headerChunk.getDataLen())
            return headerChunk;
        for (int i = 0; i < chunks.length - 1; i++) {
            if (chunks[i + 1].getPos() > position)
                return chunks[i];
        }
        if (position < size)
            return chunks[chunks.length - 1];
        return null;
    }

    public MovieSegment getPacketByNo(int no) {
        if (no > chunks.length)
            return null;
        if (no == 0)
            return headerChunk;
        return chunks[no - 1];
    }

    public long size() {
        return size;
    }
}