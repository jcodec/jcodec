package org.jcodec.player.filters.audio;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.sound.sampled.AudioFormat;

import org.jcodec.common.model.AudioFrame;
import org.jcodec.common.model.ChannelLabel;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.player.filters.MediaInfo;
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
    private ByteBuffer buffer;
    private AudioFormat newFormat;
    private int pattern;
    private int channels;

    public ChannelSelector(AudioSource src, int pattern) throws IOException {
        this.src = src;
        this.pattern = pattern;
        AudioInfo audioInfo = src.getAudioInfo();
        this.srcFormat = audioInfo.getFormat();
        buffer = ByteBuffer.allocate(96000 * 2 * srcFormat.getFrameSize());
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
                audioInfo.getNFrames(), audioInfo.getName(), null, newFormat, resultLabels);
    }

    public AudioFrame getFrame(ByteBuffer result) throws IOException {

        AudioFrame from = src.getFrame(buffer);
        if (from == null)
            return null;

        ByteBuffer dd = result.duplicate();
        ByteBuffer data = from.getData();
        int channels = srcFormat.getChannels(), sampleSize = srcFormat.getSampleSizeInBits() >> 3;
        while(data.hasRemaining()) {
            for (int z = 0; z < channels; z++) {
                if (((pattern >> z) & 0x1) == 1) {
                    dd.put(buffer.get());
                    if (sampleSize > 1)
                        dd.put(buffer.get());
                    if (sampleSize > 2)
                        dd.put(buffer.get());
                } else {
                    buffer.position(buffer.position() + sampleSize);
                }
            }
        }
        dd.flip();
        return new AudioFrame(dd, newFormat, from.getNFrames(), from.getPts(),
                from.getDuration(), from.getTimescale(), from.getFrameNo());
    }
    
    public boolean drySeek(RationalLarge second) throws IOException {
        return src.drySeek(second);
    }

    public void seek(RationalLarge second) throws IOException {
        src.seek(second);
    }

    @Override
    public void close() throws IOException {
        src.close();
    }
}
