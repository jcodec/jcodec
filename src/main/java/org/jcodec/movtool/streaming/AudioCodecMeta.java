package org.jcodec.movtool.streaming;

import java.nio.ByteBuffer;

import org.jcodec.common.AudioFormat;
import org.jcodec.containers.mp4.boxes.EndianBox.Endian;
import org.jcodec.containers.mp4.boxes.channel.Label;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class AudioCodecMeta extends CodecMeta {

    private int sampleSize;
    private int channelCount;
    private int sampleRate;
    private Endian endian;
    private boolean pcm;
    private Label[] labels;

    public AudioCodecMeta(String fourcc, int sampleSize, int channelCount, int sampleRate, Endian endian, boolean pcm,
            Label[] labels, ByteBuffer codecPrivate) {
        super(fourcc, codecPrivate);
        this.sampleSize = sampleSize;
        this.channelCount = channelCount;
        this.sampleRate = sampleRate;
        this.endian = endian;
        this.pcm = pcm;
        this.labels = labels;
    }

    public AudioCodecMeta(String fourcc, ByteBuffer codecPrivate, org.jcodec.common.AudioFormat format, boolean pcm, Label[] labels) {
        super(fourcc, codecPrivate);
        this.sampleSize = format.getSampleSizeInBits() >> 3;
        this.channelCount = format.getChannels();
        this.sampleRate = format.getSampleRate();
        this.endian = format.isBigEndian() ? Endian.BIG_ENDIAN : Endian.LITTLE_ENDIAN;
        this.pcm = pcm;
        this.labels = labels;
    }

    public int getFrameSize() {
        return sampleSize * channelCount;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public Endian getEndian() {
        return endian;
    }

    public boolean isPCM() {
        return pcm;
    }

    public AudioFormat getFormat() {
        return new AudioFormat(sampleRate, sampleSize << 3, channelCount, true, endian == Endian.BIG_ENDIAN);
    }

    public Label[] getChannelLabels() {
        return labels;
    }
}