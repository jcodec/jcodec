package org.jcodec.common;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class AudioFormat {

    private int sampleRate;
    private int sampleSizeInBits;
    private int channelCount;
    private boolean signed;
    private boolean bigEndian;

    public AudioFormat(int sampleRate, int sampleSizeInBits, int channelCount, boolean signed, boolean bigEndian) {
        this.sampleRate = sampleRate;
        this.sampleSizeInBits = sampleSizeInBits;
        this.channelCount = channelCount;
        this.signed = signed;
        this.bigEndian = bigEndian;
    }

    public int getChannels() {
        return channelCount;
    }

    public int getSampleSizeInBits() {
        return sampleSizeInBits;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public short getFrameSize() {
        return (short) ((sampleSizeInBits >> 3) * channelCount);
    }

    public int getFrameRate() {
        return sampleRate;
    }

    public boolean isBigEndian() {
        return bigEndian;
    }

    public boolean isSigned() {
        return signed;
    }
}
