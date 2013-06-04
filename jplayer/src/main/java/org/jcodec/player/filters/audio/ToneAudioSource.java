package org.jcodec.player.filters.audio;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.sound.sampled.AudioFormat;

import org.jcodec.common.model.AudioFrame;
import org.jcodec.common.model.ChannelLabel;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.player.filters.MediaInfo;

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
        return new MediaInfo.AudioInfo("pcm", SAMPLE_RATE, Long.MAX_VALUE, Long.MAX_VALUE, "Main Stereo", null, format,
                new ChannelLabel[] { ChannelLabel.STEREO_LEFT, ChannelLabel.STEREO_LEFT });
    }

    @Override
    public AudioFrame getFrame(ByteBuffer result) throws IOException {
        if (result.remaining() < (FRAMES_PER_PACKET << 2))
            throw new IllegalArgumentException("Data won't fit into the buffer");
        ByteBuffer dd = result.duplicate();

        long ss = lastSample;
        for (int i = 0; i < FRAMES_PER_PACKET; i++) {
            int fr = (int) (((ss + i) / SAMPLE_RATE) % 50);
            short sl = (short) (Short.MAX_VALUE * Math.sin(((150 + fr * 10) * 2 * Math.PI * (ss + i)) / SAMPLE_RATE));
            dd.put((byte) (sl & 0xff));
            dd.put((byte) (sl >> 8));
            short sr = (short) (Short.MAX_VALUE * Math.sin(((150 + fr * 10) * 2 * Math.PI * (ss + i)) / SAMPLE_RATE));
            dd.put((byte) (sr & 0xff));
            dd.put((byte) (sr >> 8));
        }
        lastSample += FRAMES_PER_PACKET;
        dd.flip();

        return new AudioFrame(dd, format, FRAMES_PER_PACKET, lastSample, FRAMES_PER_PACKET, SAMPLE_RATE,
                (int) (lastSample / FRAMES_PER_PACKET));
    }

    @Override
    public boolean drySeek(RationalLarge second) throws IOException {
        return true;
    }

    @Override
    public void seek(RationalLarge second) {
        lastSample = second.multiplyS(SAMPLE_RATE);
    }

    @Override
    public void close() throws IOException {
    }
}