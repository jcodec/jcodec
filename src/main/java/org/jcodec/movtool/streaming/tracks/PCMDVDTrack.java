package org.jcodec.movtool.streaming.tracks;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.codecs.pcmdvd.PCMDVDDecoder;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.model.AudioBuffer;
import org.jcodec.containers.mp4.boxes.channel.Label;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.movtool.streaming.AudioCodecMeta;
import org.jcodec.movtool.streaming.CodecMeta;
import org.jcodec.movtool.streaming.VirtualPacket;
import org.jcodec.movtool.streaming.VirtualTrack;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A decoding track for PCM DVD stream
 * 
 * @author The JCodec project
 * 
 */
public class PCMDVDTrack implements VirtualTrack {

    private VirtualTrack src;
    private AudioFormat format;
    private VirtualPacket prevPkt;
    private PCMDVDDecoder decoder;
    private int nFrames;

    public PCMDVDTrack(VirtualTrack src) throws IOException {
        this.src = src;
        prevPkt = src.nextPacket();
        decoder = new PCMDVDDecoder();
        if (prevPkt != null) {
            AudioBuffer decodeFrame = decoder.decodeFrame(prevPkt.getData(),
                    ByteBuffer.allocate(prevPkt.getData().remaining()));
            format = decodeFrame.getFormat();
            nFrames = decodeFrame.getNFrames();
        }
    }

    @Override
    public VirtualPacket nextPacket() throws IOException {
        if (prevPkt == null)
            return null;
        VirtualPacket ret = prevPkt;

        prevPkt = src.nextPacket();

        return new PCMDVDPkt(ret);
    }

    private class PCMDVDPkt extends VirtualPacketWrapper {

        public PCMDVDPkt(VirtualPacket src) {
            super(src);
        }

        @Override
        public ByteBuffer getData() throws IOException {
            ByteBuffer data = super.getData();
            AudioBuffer decodeFrame = decoder.decodeFrame(data, data);
            return decodeFrame.getData();
        }

        @Override
        public int getDataLen() throws IOException {
            return (nFrames * format.getChannels()) << 1;
        }
    }

    @Override
    public CodecMeta getCodecMeta() {
        return new AudioCodecMeta(MP4Muxer.lookupFourcc(format), ByteBuffer.allocate(0), format, true, new Label[] {
                Label.Left, Label.Right });
    }

    @Override
    public VirtualEdit[] getEdits() {
        return null;
    }

    @Override
    public int getPreferredTimescale() {
        return format.getSampleRate();
    }

    @Override
    public void close() throws IOException {
        src.close();
    }
}
