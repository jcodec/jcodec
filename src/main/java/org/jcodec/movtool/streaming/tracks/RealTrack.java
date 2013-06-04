package org.jcodec.movtool.streaming.tracks;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.SampleSizesBox;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.FramesMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.PCMMP4DemuxerTrack;
import org.jcodec.movtool.streaming.VirtualPacket;
import org.jcodec.movtool.streaming.VirtualTrack;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Track taken from a real movie
 * 
 * @author The JCodec project
 * 
 */
public class RealTrack implements VirtualTrack {

    private TrakBox trak;
    private SeekableByteChannel ch;
    private AbstractMP4DemuxerTrack demuxer;
    private ByteBuffer dummy;

    public RealTrack(MovieBox movie, TrakBox trak, SeekableByteChannel ch) {
        dummy = ByteBuffer.allocate(1024 * 1024 * 2);
        SampleSizesBox stsz = Box.findFirst(trak, SampleSizesBox.class, "mdia", "minf", "stbl", "stsz");
        if (stsz.getDefaultSize() == 0) {
            this.demuxer = new FramesMP4DemuxerTrack(movie, trak, ch) {
                @Override
                protected ByteBuffer readPacketData(SeekableByteChannel ch, ByteBuffer buffer, long position, int size)
                        throws IOException {
                    return buffer;
                }
            };
        } else {
            this.demuxer = new PCMMP4DemuxerTrack(movie, trak, ch) {
                @Override
                protected ByteBuffer readPacketData(SeekableByteChannel ch, ByteBuffer buffer, long position, int size)
                        throws IOException {
                    return buffer;
                }
            };
        }
        this.trak = trak;
        this.ch = ch;
    }

    @Override
    public VirtualPacket nextPacket() throws IOException {
        MP4Packet pkt = demuxer.nextFrame(dummy);
        if (pkt == null)
            return null;
        return new RealPacket(pkt);
    }

    @Override
    public SampleEntry getSampleEntry() {
        return trak.getSampleEntries()[0];
    }

    @Override
    public void close() {
        NIOUtils.closeQuietly(ch);
    }

    public class RealPacket implements VirtualPacket {

        private MP4Packet packet;

        public RealPacket(MP4Packet nextFrame) {
            this.packet = nextFrame;
        }

        @Override
        public ByteBuffer getData() throws IOException {
            ByteBuffer bb = ByteBuffer.allocate(packet.getSize());
            
            synchronized (ch) {
                ch.position(packet.getFileOff());
                ch.read(bb);
                bb.flip();
                return bb;
            }
        }

        @Override
        public int getDataLen() {
            return packet.getSize();
        }

        @Override
        public double getPts() {
            return packet.getPtsD();
        }

        @Override
        public double getDuration() {
            return (double) packet.getDuration() / packet.getTimescale();
        }

        @Override
        public boolean isKeyframe() {
            return packet.isKeyFrame();
        }

        @Override
        public int getFrameNo() {
            return (int) packet.getFrameNo();
        }
    }
}