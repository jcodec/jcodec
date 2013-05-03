package org.jcodec.player.filters.audio;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import javax.sound.sampled.AudioFormat;

import org.jcodec.codecs.wav.WavHeader;
import org.jcodec.common.NIOUtils;
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
    private FileChannel src;
    private int frameSize;
    private AudioFormat format;
    private long headerSize;

    public WavAudioSource(File src) throws IOException {
        header = WavHeader.read(src);
        headerSize = src.length() - header.dataSize;
        this.src = new FileInputStream(src).getChannel();
        this.src.position(header.dataOffset);
        frameSize = header.fmt.numChannels * (header.fmt.bitsPerSample >> 3);
    }

    public MediaInfo.AudioInfo getAudioInfo() {
        format = new AudioFormat(header.fmt.sampleRate, header.fmt.bitsPerSample, header.fmt.numChannels, true, false);

        return new MediaInfo.AudioInfo("pcm", header.fmt.sampleRate, header.dataSize / frameSize, header.dataSize
                / frameSize, "", null, format, FRAMES_PER_PACKET, header.getChannelLabels());
    }

    public AudioFrame getFrame(ByteBuffer data) throws IOException {
        int toRead = frameSize * FRAMES_PER_PACKET;
        if (data.remaining() < toRead)
            throw new IllegalArgumentException("Data won't fit");
        ByteBuffer dd = data.duplicate();
        int read;
        if ((read = NIOUtils.read(src, dd, toRead)) != toRead) {
            NIOUtils.fill(dd, (byte) 0);
        }
        long pts = (src.position() - headerSize) / header.fmt.blockAlign;
        dd.flip();
        return new AudioFrame(dd, format, FRAMES_PER_PACKET, pts, FRAMES_PER_PACKET, header.fmt.sampleRate,
                (int) (pts / FRAMES_PER_PACKET));
    }

    public boolean drySeek(RationalLarge second) throws IOException {
        int frameSize = header.fmt.numChannels * (header.fmt.bitsPerSample >> 3);
        long off = second.multiplyS((long) header.fmt.sampleRate) * frameSize;
        long where = header.dataOffset + off - (off % frameSize);
        return where < src.size();
    }

    public void seek(RationalLarge second) throws IOException {
        int frameSize = header.fmt.numChannels * (header.fmt.bitsPerSample >> 3);
        long off = second.multiplyS((long) header.fmt.sampleRate) * frameSize;
        long where = header.dataOffset + off - (off % frameSize);
        src.position(where);
    }

    public RationalLarge getPos() {
        try {
            int frameSize = header.fmt.numChannels * (header.fmt.bitsPerSample >> 3);
            return new RationalLarge((src.position() - header.dataOffset) / frameSize, header.fmt.sampleRate);
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