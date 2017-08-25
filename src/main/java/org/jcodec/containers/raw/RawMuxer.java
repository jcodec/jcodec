package org.jcodec.containers.raw;

import java.io.IOException;

import org.jcodec.common.AudioCodecMeta;
import org.jcodec.common.Muxer;
import org.jcodec.common.MuxerTrack;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;

public class RawMuxer implements Muxer, MuxerTrack {

    private SeekableByteChannel ch;
    private boolean hasVideo;
    private boolean hasAudio;

    public RawMuxer(SeekableByteChannel destStream) {
        this.ch = destStream;
    }

    @Override
    public MuxerTrack addVideoTrack(VideoCodecMeta meta) {
        if (hasAudio)
            throw new RuntimeException("Raw muxer supports either video or audio track but not both.");
        hasVideo = true;
        return this;
    }

    @Override
    public MuxerTrack addAudioTrack(AudioCodecMeta meta) {
        if (hasVideo)
            throw new RuntimeException("Raw muxer supports either video or audio track but not both.");
        hasAudio = true;
        return this;
    }

    @Override
    public void finish() throws IOException {
    }

    @Override
    public void addFrame(Packet outPacket) throws IOException {
        ch.write(outPacket.getData().duplicate());
    }
}