package org.jcodec.player.filters.audio;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;

import org.jcodec.codecs.wav.StringReader;
import org.jcodec.codecs.wav.WavHeader;
import org.jcodec.common.io.Buffer;
import org.jcodec.common.io.RandomAccessFileInputStream;
import org.jcodec.common.io.RandomAccessInputStream;
import org.jcodec.common.model.AudioFrame;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.player.filters.MediaInfo;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class WavAudioSource implements AudioSource {

    private static final int FRAMES_PER_PACKET = 2048;
    private WavHeader header;
    private RandomAccessInputStream src;
    private int frameSize;
    private AudioFormat format;
    private long headerSize;

    public WavAudioSource(File src) throws IOException {
        header = WavHeader.read(src);
        headerSize = src.length() - header.dataSize;
        this.src = new RandomAccessFileInputStream(src);
        this.src.seek(header.dataOffset);
        frameSize = header.fmt.numChannels * (header.fmt.bitsPerSample >> 3);
    }

    public MediaInfo.AudioInfo getAudioInfo() {
        format = new AudioFormat(header.fmt.sampleRate, header.fmt.bitsPerSample, header.fmt.numChannels, true, false);

        return new MediaInfo.AudioInfo("pcm", header.fmt.sampleRate, header.dataSize / frameSize, header.dataSize
                / frameSize, "", null, format, FRAMES_PER_PACKET, header.getChannelLabels());
    }

    public AudioFrame getFrame(byte[] data) throws IOException {
        int toRead = frameSize * FRAMES_PER_PACKET;
        if (data.length < toRead)
            throw new IllegalArgumentException("Data won't fit");
        int read;
        if ((read = StringReader.sureRead(src, data, toRead)) != toRead) {
            Arrays.fill(data, read, toRead, (byte) 0);
        }
        long pts = (src.getPos() - headerSize) / header.fmt.blockAlign;
        return new AudioFrame(new Buffer(data, 0, toRead), format, FRAMES_PER_PACKET, pts, FRAMES_PER_PACKET,
                header.fmt.sampleRate, (int) (pts / FRAMES_PER_PACKET));
    }

    public boolean drySeek(RationalLarge second) throws IOException {
        int frameSize = header.fmt.numChannels * (header.fmt.bitsPerSample >> 3);
        long off = second.multiplyS((long) header.fmt.sampleRate) * frameSize;
        long where = header.dataOffset + off - (off % frameSize);
        return where < src.length();
    }

    public void seek(RationalLarge second) throws IOException {
        int frameSize = header.fmt.numChannels * (header.fmt.bitsPerSample >> 3);
        long off = second.multiplyS((long) header.fmt.sampleRate) * frameSize;
        long where = header.dataOffset + off - (off % frameSize);
        src.seek(where);
    }

    public RationalLarge getPos() {
        try {
            int frameSize = header.fmt.numChannels * (header.fmt.bitsPerSample >> 3);
            return new RationalLarge((src.getPos() - header.dataOffset) / frameSize, header.fmt.sampleRate);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        this.src.close();
    }
}