package org.jcodec.movtool.streaming.tracks;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;

import org.jcodec.codecs.wav.WavHeader;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.ChannelBox;
import org.jcodec.containers.mp4.boxes.EndianBox.Endian;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.channel.ChannelUtils;
import org.jcodec.containers.mp4.boxes.channel.Label;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.movtool.streaming.VirtualPacket;
import org.jcodec.movtool.streaming.VirtualTrack;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Reads data from a wave file
 * 
 * @author The JCodec project
 * 
 */
public class WavTrack implements VirtualTrack {

    public static final int FRAMES_PER_PKT = 1024;

    private FilePool pool;
    private WavHeader header;
    private AudioSampleEntry se;
    private int pktDataLen;
    private double pktDuration;

    private long offset;

    private double pts;

    private int frameNo;

    private long size;

    public WavTrack(FilePool pool, Label... labels) throws IOException {
        this.pool = pool;

        SeekableByteChannel ch = null;
        try {
            ch = pool.getChannel();
            header = WavHeader.read(Channels.newInputStream(ch));
            size = ch.size();
        } finally {
            ch.close();
        }

        se = MP4Muxer.audioSampleEntry("sowt", 1, header.fmt.bitsPerSample >> 3, header.fmt.numChannels,
                header.fmt.sampleRate, Endian.LITTLE_ENDIAN);
        ChannelBox chan = new ChannelBox();
        ChannelUtils.setLabels(labels, chan);
        se.add(chan);

        pktDataLen = FRAMES_PER_PKT * header.fmt.numChannels * (header.fmt.bitsPerSample >> 3);
        pktDuration = (double) FRAMES_PER_PKT / header.fmt.sampleRate;

        offset = header.dataOffset;
        pts = 0;
        frameNo = 0;
    }

    @Override
    public VirtualPacket nextPacket() throws IOException {
        if (offset >= size)
            return null;

        WavPacket pkt = new WavPacket(frameNo, pts, offset, (int) Math.min(size - offset, pktDataLen));

        offset += pktDataLen;
        frameNo += FRAMES_PER_PKT;
        pts = (double) frameNo / header.fmt.sampleRate;

        return pkt;
    }

    @Override
    public SampleEntry getSampleEntry() {
        return se;
    }

    @Override
    public VirtualEdit[] getEdits() {
        return null;
    }

    @Override
    public int getPreferredTimescale() {
        return header.fmt.sampleRate;
    }

    @Override
    public void close() throws IOException {
        pool.close();
    }

    public class WavPacket implements VirtualPacket {
        private int frameNo;
        private double pts;
        private long offset;
        private int dataLen;

        public WavPacket(int frameNo, double pts, long offset, int dataLen) {
            this.frameNo = frameNo;
            this.pts = pts;
            this.offset = offset;
            this.dataLen = dataLen;
        }

        @Override
        public ByteBuffer getData() throws IOException {
            SeekableByteChannel ch = null;
            try {
                ch = pool.getChannel();
                ch.position(offset);
                ByteBuffer buffer = ByteBuffer.allocate(dataLen);
                NIOUtils.read(ch, buffer);
                buffer.flip();
                return buffer;
            } finally {
                ch.close();
            }
        }

        @Override
        public int getDataLen() throws IOException {
            return dataLen;
        }

        @Override
        public double getPts() {
            return pts;
        }

        @Override
        public double getDuration() {
            return pktDuration;
        }

        @Override
        public boolean isKeyframe() {
            return true;
        }

        @Override
        public int getFrameNo() {
            return frameNo;
        }
    }
}