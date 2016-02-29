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

    public static AudioCodecMeta createAudioCodecMeta(String fourcc, int sampleSize, int channelCount, int sampleRate,
            Endian endian, boolean pcm, Label[] labels, ByteBuffer codecPrivate) {
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
            Endian endian, boolean pcm, Label[] labels, int samplesPerPacket, int bytesPerPacket, int bytesPerFrame,
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
        self.endian = format.isBigEndian() ? Endian.BIG_ENDIAN : Endian.LITTLE_ENDIAN;
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
    private Endian endian;
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