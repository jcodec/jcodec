package org.jcodec.player.filters;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;

import org.jcodec.common.io.Buffer;
import org.jcodec.common.model.AudioFrame;
import org.jcodec.common.model.ChannelLabel;
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
    private int pattern;
    private int channels;

    public ChannelSelector(AudioSource src, int pattern) throws IOException {
        this.src = src;
        this.pattern = pattern;
        AudioInfo audioInfo = src.getAudioInfo();
        this.srcFormat = audioInfo.getFormat();
        buffer = new byte[audioInfo.getFramesPerPacket() * 2 * srcFormat.getFrameSize()];
        channels = 0;
        for (int i = 0; i < 32; i++) {
            channels += pattern & 0x1;
            pattern >>= 1;
        }
        newFormat = new AudioFormat(srcFormat.getSampleRate(), srcFormat.getSampleSizeInBits(), channels, true,
                srcFormat.isBigEndian());
    }

    public MediaInfo.AudioInfo getAudioInfo() throws IOException {
        AudioInfo audioInfo = src.getAudioInfo();
        ChannelLabel[] srcLabels = audioInfo.getLabels();
        ChannelLabel[] resultLabels = new ChannelLabel[channels];
        for (int i = 0, lbl = 0; i < 32; i++) {
            if ((pattern & 0x1) == 1) {
                resultLabels[lbl++] = srcLabels[i];
            }
            pattern >>= 1;
        }

        return new AudioInfo(audioInfo.getFourcc(), audioInfo.getTimescale(), audioInfo.getDuration(),
                audioInfo.getNFrames(), audioInfo.getName(), newFormat, audioInfo.getFramesPerPacket(), resultLabels);
    }

    public AudioFrame getFrame(byte[] result) throws IOException {

        AudioFrame from = src.getFrame(buffer);
        if (from == null)
            return null;

        Buffer data = from.getData();
        int j = 0, channels = srcFormat.getChannels(), sampleSize = srcFormat.getSampleSizeInBits() >> 3;
        for (int k = data.pos; k < data.limit;) {
            for (int z = 0; z < channels; z++) {
                if (((pattern >> z) & 0x1) == 1) {
                    result[j++] = buffer[k++];
                    if (sampleSize > 1)
                        result[j++] = buffer[k++];
                    if (sampleSize > 2)
                        result[j++] = buffer[k++];
                } else {
                    k += sampleSize;
                }
            }
        }
        return new AudioFrame(new Buffer(result, 0, j), newFormat, from.getNFrames(), from.getPts(),
                from.getDuration(), from.getTimescale(), from.getFrameNo());
    }
    
    public boolean drySeek(long clock, long timescale) throws IOException {
        return src.drySeek(clock, timescale);
    }

    public void seek(long clock, long timescale) throws IOException {
        src.seek(clock, timescale);
    }

    @Override
    public void close() throws IOException {
        src.close();
    }
}
