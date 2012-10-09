package org.jcodec.player.filters;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;

import org.jcodec.common.io.Buffer;
import org.jcodec.common.model.AudioFrame;
import org.jcodec.common.model.ChannelLabel;
import org.jcodec.containers.mp4.boxes.channel.Label;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Generates audio tones
 * 
 * Audio format: PCM 16bit stereo signed little endian
 * 
 * @author The JCodec project
 * 
 */
public class ToneAudioSource implements AudioSource {

    private static final int FRAMES_PER_PACKET = 2048;
    private static final int SAMPLE_RATE = 48000;
    private long lastSample;
    private AudioFormat format;

    public ToneAudioSource() {
    }

    @Override
    public MediaInfo.AudioInfo getAudioInfo() {
        format = new AudioFormat(SAMPLE_RATE, 16, 2, true, false);
        return new MediaInfo.AudioInfo("pcm", SAMPLE_RATE, Long.MAX_VALUE, Long.MAX_VALUE, "Main Stereo", format,
                FRAMES_PER_PACKET, new ChannelLabel[] { ChannelLabel.STEREO_LEFT, ChannelLabel.STEREO_LEFT });
    }

    @Override
    public AudioFrame getFrame(byte[] result) throws IOException {
        if (result.length < (FRAMES_PER_PACKET << 2))
            throw new IllegalArgumentException("Data won't fit into the buffer");

        int off = 0;
        long ss = lastSample;
        for (int i = 0; i < FRAMES_PER_PACKET; i++) {
            int fr = (int) (((ss + i) / SAMPLE_RATE) % 50);
            short sl = (short) (Short.MAX_VALUE * Math.sin(((150 + fr * 10) * 2 * Math.PI * (ss + i)) / SAMPLE_RATE));
            result[off++] = (byte) (sl & 0xff);
            result[off++] = (byte) (sl >> 8);
            short sr = (short) (Short.MAX_VALUE * Math.sin(((150 + fr * 10) * 2 * Math.PI * (ss + i)) / SAMPLE_RATE));
            result[off++] = (byte) (sr & 0xff);
            result[off++] = (byte) (sr >> 8);
        }
        lastSample += FRAMES_PER_PACKET;

        return new AudioFrame(new Buffer(result, 0, 4 * FRAMES_PER_PACKET), format, FRAMES_PER_PACKET, lastSample,
                FRAMES_PER_PACKET, SAMPLE_RATE, (int)(lastSample / FRAMES_PER_PACKET));
    }

    
    @Override
    public boolean drySeek(long clock, long timescale) throws IOException {
        return true;
    }

    @Override
    public void seek(long clock, long timescale) {
        lastSample = (clock * SAMPLE_RATE) / timescale;
    }

    @Override
    public void close() throws IOException {
    }
}