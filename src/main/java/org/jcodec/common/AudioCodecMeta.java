package org.jcodec.common;

import org.jcodec.common.model.Label;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class AudioCodecMeta extends CodecMeta {

    public static AudioCodecMeta createAudioCodecMeta(String fourcc, int sampleSize, int channelCount, int sampleRate,
            ByteOrder endian, boolean pcm, Label[] labels, ByteBuffer codecPrivate) {
        AudioCodecMeta self = new AudioCodecMeta(fourcc, codecPrivate);
        self.sampleSize = sampleSize;
        self.channelCount = channelCount;
        self.sampleRate = sampleRate;
        self.endian = endian;
        self.pcm = pcm;
        self.labels = labels;
        return self;
    }

    public static AudioCodecMeta createAudioCodecMeta2(String fourcc, int sampleSize, int channelCount, int sampleRate,
            ByteOrder endian, boolean pcm, Label[] labels, int samplesPerPacket, int bytesPerPacket, int bytesPerFrame,
            ByteBuffer codecPrivate) {
        AudioCodecMeta self = new AudioCodecMeta(fourcc, codecPrivate);
        self.sampleSize = sampleSize;
        self.channelCount = channelCount;
        self.sampleRate = sampleRate;
        self.endian = endian;
        self.samplesPerPacket = samplesPerPacket;
        self.bytesPerPacket = bytesPerPacket;
        self.bytesPerFrame = bytesPerFrame;
        self.pcm = pcm;
        self.labels = labels;
        return self;
    }

    public static AudioCodecMeta createAudioCodecMeta3(String fourcc, ByteBuffer codecPrivate, AudioFormat format,
            boolean pcm, Label[] labels) {
        AudioCodecMeta self = new AudioCodecMeta(fourcc, codecPrivate);
        self.sampleSize = format.getSampleSizeInBits() >> 3;
        self.channelCount = format.getChannels();
        self.sampleRate = format.getSampleRate();
        self.endian = format.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
        self.pcm = pcm;
        self.labels = labels;
        return self;
    }

    public AudioCodecMeta(String fourcc, ByteBuffer codecPrivate) {
        super(fourcc, codecPrivate);
    }

    private int sampleSize;
    private int channelCount;
    private int sampleRate;
    private ByteOrder endian;
    private int samplesPerPacket;
    private int bytesPerPacket;
    private int bytesPerFrame;
    private boolean pcm;
    private Label[] labels;

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

    public int getSamplesPerPacket() {
        return samplesPerPacket;
    }

    public int getBytesPerPacket() {
        return bytesPerPacket;
    }

    public int getBytesPerFrame() {
        return bytesPerFrame;
    }

    public ByteOrder getEndian() {
        return endian;
    }

    public boolean isPCM() {
        return pcm;
    }

    public AudioFormat getFormat() {
        return new AudioFormat(sampleRate, sampleSize << 3, channelCount, true, endian == ByteOrder.BIG_ENDIAN);
    }

    public Label[] getChannelLabels() {
        return labels;
    }
    
    public static AudioCodecMeta fromAudioFormat(AudioFormat format) {
        AudioCodecMeta self = new AudioCodecMeta(null, null);
        self.sampleSize = format.getSampleSizeInBits() >> 3;
        self.channelCount = format.getChannels();
        self.sampleRate = format.getSampleRate();
        self.endian = format.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
        self.pcm = false;
        return self;
    }
}