package org.jcodec.player.filters;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;

import org.jcodec.common.io.Buffer;
import org.jcodec.common.model.AudioFrame;
import org.jcodec.player.filters.MediaInfo.AudioInfo;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class ChannelSelector implements AudioSource {

    private AudioSource src;
    private AudioFormat srcFormat;
    private byte[] buffer;
    private AudioFormat newFormat;

    public ChannelSelector(AudioSource src) {
        this.src = src;
        AudioInfo audioInfo = src.getAudioInfo();
        this.srcFormat = audioInfo.getFormat();
        buffer = new byte[audioInfo.getFramesPerPacket() * 2 * srcFormat.getFrameSize()];
        newFormat = new AudioFormat(srcFormat.getSampleRate(), srcFormat.getSampleSizeInBits(), 2, true,
                srcFormat.isBigEndian());
    }

    public MediaInfo.AudioInfo getAudioInfo() {
        AudioInfo audioInfo = src.getAudioInfo();
        return new AudioInfo(audioInfo.getFourcc(), audioInfo.getTimescale(), audioInfo.getDuration(),
                audioInfo.getNFrames(), newFormat, audioInfo.getFramesPerPacket());
    }

    public AudioFrame getFrame(byte[] result) throws IOException {

        AudioFrame from = src.getFrame(buffer);
        if (from == null)
            return null;

        Buffer data = from.getData();
        int skip = srcFormat.getFrameSize() - 4;
        int j = 0;
        for (int k = data.pos; k < data.limit;) {
            result[j++] = buffer[k++];
            result[j++] = buffer[k++];
            result[j++] = buffer[k++];
            result[j++] = buffer[k++];

            k += skip;
        }
        return new AudioFrame(new Buffer(result, 0, j), newFormat, from.getNFrames(), from.getPts(),
                from.getDuration(), from.getTimescale());
    }

    public boolean seek(long clock, long timescale) throws IOException {
        return src.seek(clock, timescale);
    }

    @Override
    public void close() throws IOException {
        src.close();
    }
}
