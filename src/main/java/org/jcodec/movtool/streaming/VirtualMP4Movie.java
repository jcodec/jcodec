package org.jcodec.movtool.streaming;

import static org.jcodec.movtool.streaming.MovieHelper.produceHeader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * ISO BMF ( MP4 ) specific muxing
 * 
 * @author The JCodec project
 * 
 */
public class VirtualMP4Movie extends VirtualMovie {

    public VirtualMP4Movie(VirtualTrack... tracks) throws IOException {
        super(tracks);
    }

    protected MovieSegment headerChunk(List<MovieSegment> ch, VirtualTrack[] tracks, long dataSize) throws IOException {
        PacketChunk[] chunks = ch.toArray(new PacketChunk[0]);
        int headerSize = produceHeader(chunks, tracks, dataSize).remaining();
        for (PacketChunk chunk : chunks) {
            chunk.offset(headerSize);
        }

        final ByteBuffer header = produceHeader(chunks, tracks, dataSize);
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

    protected MovieSegment packetChunk(VirtualPacket pkt, int chunkNo, int track, long pos) {
        return new PacketChunk(pkt, track, chunkNo, pos);
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
            return packet.getData().duplicate();
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
}