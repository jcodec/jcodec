package org.jcodec.codecs.wav;

import java.io.IOException;

import org.jcodec.common.AudioCodecMeta;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.Codec;
import org.jcodec.common.Muxer;
import org.jcodec.common.MuxerTrack;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Outputs integer samples into wav file
 * 
 * @author The JCodec project
 */
public class WavMuxer implements Muxer, MuxerTrack {

    protected SeekableByteChannel out;
    protected WavHeader header;
    protected int written;
    private AudioFormat format;

    public WavMuxer(SeekableByteChannel out) throws IOException {
        this.out = out;
    }
    
    @Override
    public void addFrame(Packet outPacket) throws IOException {
        written += out.write(outPacket.getData());
    }

    public void close() throws IOException {
        out.setPosition(0);
        WavHeader.createWavHeader(format, format.bytesToFrames(written)).write(out);
        NIOUtils.closeQuietly(out);
    }

    @Override
    public MuxerTrack addVideoTrack(Codec codec, VideoCodecMeta meta) {
        return null;
    }

    @Override
    public MuxerTrack addAudioTrack(Codec codec, AudioCodecMeta meta) {
        header = WavHeader.createWavHeader(meta.getFormat(), 0);
        this.format = meta.getFormat();
        try {
            header.write(out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public void finish() throws IOException {
        // NOP
    }
}