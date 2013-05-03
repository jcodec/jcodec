package org.jcodec.player.filters.audio;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.sound.sampled.AudioFormat;

import org.jcodec.common.model.AudioFrame;
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
public class Resampler24To16 implements AudioSource {

    private AudioSource src;
    private ByteBuffer buffer;
    private AudioFormat srcFormat;
    private AudioFormat newFormat;

    public Resampler24To16(AudioSource src) throws IOException {
        this.src = src;
        AudioInfo audioInfo = src.getAudioInfo();
        this.srcFormat = audioInfo.getFormat();
        buffer = ByteBuffer.allocate(audioInfo.getFramesPerPacket() * 2 * srcFormat.getFrameSize());
        newFormat = new AudioFormat(srcFormat.getSampleRate(), 16, srcFormat.getChannels(), true,
                srcFormat.isBigEndian());
    }

    public MediaInfo.AudioInfo getAudioInfo() throws IOException {
        AudioInfo audioInfo = src.getAudioInfo();
        return new AudioInfo(audioInfo.getFourcc(), audioInfo.getTimescale(), audioInfo.getDuration(),
                audioInfo.getNFrames(), audioInfo.getName(), null, newFormat, audioInfo.getFramesPerPacket(),
                audioInfo.getLabels());
    }

    public AudioFrame getFrame(ByteBuffer result) throws IOException {
        buffer.rewind();
        AudioFrame from = src.getFrame(buffer);
        if (from == null)
            return null;

        ByteBuffer dup = result.duplicate();
        ByteBuffer data = from.getData();
        if (!srcFormat.isBigEndian()) {
            while(data.hasRemaining()) {
                buffer.get();
                dup.put(buffer.get());
                dup.put(buffer.get());
            }
        } else {
            while(data.hasRemaining()) {
                dup.put(buffer.get());
                dup.put(buffer.get());
                buffer.get();
            }
        }
        dup.flip();
        return new AudioFrame(dup, newFormat, from.getNFrames(), from.getPts(),
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