package org.jcodec.codecs.wav;

import static org.jcodec.codecs.wav.ReaderLE.readShort;
import static org.jcodec.codecs.wav.ReaderLE.readInt;
import static org.jcodec.codecs.wav.StringReader.readString;
import static org.jcodec.codecs.wav.WriterLE.writeInt;
import static org.jcodec.codecs.wav.WriterLE.writeShort;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sound.sampled.AudioFormat;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.jcodec.common.model.ChannelLabel;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class WavHeader {

    public static class FmtChunkExtended extends FmtChunk {
        short cbSize;
        short bitsPerCodedSample;
        int channelLayout;
        int guid;

        public FmtChunkExtended(FmtChunk fmtChunk, short cbSize, short bitsPerCodedSample, int channelLayout, int guid) {
            super(fmtChunk);
            this.cbSize = cbSize;
            this.bitsPerCodedSample = bitsPerCodedSample;
            this.channelLayout = channelLayout;
            this.guid = guid;
        }

        public static FmtChunk read(InputStream input) throws IOException {
            FmtChunk fmtChunk = FmtChunk.read(input);
            return new FmtChunkExtended(fmtChunk, readShort(input), readShort(input), readInt(input), readInt(input));
        }

        public void write(OutputStream out) throws IOException {
            super.write(out);

            writeShort(out, cbSize);
            writeShort(out, bitsPerCodedSample);
            writeInt(out, channelLayout);
            writeInt(out, guid);
        }

        public int size() {
            return super.size() + 12;
        }

        public ChannelLabel[] getLabels() {
            List<ChannelLabel> labels = new ArrayList<ChannelLabel>();
            for (int i = 0; i < mapping.length; i++) {
                if ((channelLayout & (1 << i)) != 0)
                    labels.add(mapping[i]);
            }
            return labels.toArray(new ChannelLabel[0]);
        }
    }

    static ChannelLabel[] mapping = new ChannelLabel[] { ChannelLabel.FRONT_LEFT, ChannelLabel.FRONT_RIGHT,
            ChannelLabel.CENTER, ChannelLabel.LFE, ChannelLabel.REAR_LEFT, ChannelLabel.REAR_RIGHT,
            ChannelLabel.FRONT_CENTER_LEFT, ChannelLabel.FRONT_CENTER_RIGHT, ChannelLabel.REAR_CENTER,
            ChannelLabel.SIDE_LEFT, ChannelLabel.SIDE_RIGHT, ChannelLabel.CENTER, ChannelLabel.FRONT_LEFT,
            ChannelLabel.CENTER, ChannelLabel.FRONT_RIGHT, ChannelLabel.REAR_LEFT, ChannelLabel.REAR_CENTER,
            ChannelLabel.REAR_RIGHT, ChannelLabel.STEREO_LEFT, ChannelLabel.STEREO_RIGHT };

    public static class FmtChunk {
        public short audioFormat;
        public short numChannels;
        public int sampleRate;
        public int byteRate;
        public short blockAlign;
        public short bitsPerSample;

        public FmtChunk() {
            this.audioFormat = 1;
        }

        public FmtChunk(short audioFormat, short numChannels, int sampleRate, int byteRate, short blockAlign,
                short bitsPerSample) {
            this.audioFormat = audioFormat;
            this.numChannels = numChannels;
            this.sampleRate = sampleRate;
            this.byteRate = byteRate;
            this.blockAlign = blockAlign;
            this.bitsPerSample = bitsPerSample;
        }

        public FmtChunk(FmtChunk other) {
            this(other.audioFormat, other.numChannels, other.sampleRate, other.byteRate, other.blockAlign,
                    other.bitsPerSample);
        }

        public static FmtChunk read(InputStream input) throws IOException {
            return new FmtChunk(readShort(input), readShort(input), readInt(input), readInt(input), readShort(input),
                    readShort(input));
        }

        public void write(OutputStream out) throws IOException {
            writeShort(out, audioFormat);
            writeShort(out, numChannels);
            writeInt(out, sampleRate);
            writeInt(out, byteRate);
            writeShort(out, blockAlign);
            writeShort(out, bitsPerSample);
        }

        public int size() {
            return 16;
        }
    }

    public String chunkId;
    public int chunkSize;
    public String format;
    public FmtChunk fmt;
    public int dataOffset;
    public long dataSize;
    public static final int WAV_HEADER_SIZE = 44;

    public WavHeader(String chunkId, int chunkSize, String format, FmtChunk fmt, int dataOffset, long dataSize) {
        this.chunkId = chunkId;
        this.chunkSize = chunkSize;
        this.format = format;
        this.fmt = fmt;
        this.dataOffset = dataOffset;
        this.dataSize = dataSize;
    }

    public WavHeader(WavHeader header) {
        throw new UnsupportedOperationException();
    }

    public WavHeader(WavHeader header, int rate) {
        this(header);
        fmt.sampleRate = rate;
    }

    public static WavHeader stereo48k() {
        return stereo48k(0);
    }

    public static WavHeader stereo48k(long samples) {
        return new WavHeader("RIFF", 40, "WAVE", new FmtChunk((short) 1, (short) 2, 48000, 48000 * 2 * 16 / 8,
                (short) 4, (short) 16), 44, calcDataSize(1, 2, samples));
    }

    public static WavHeader mono48k(long samples) {
        return new WavHeader("RIFF", 40, "WAVE", new FmtChunk((short) 1, (short) 1, 48000, 48000 * 1 * 16 / 8,
                (short) 2, (short) 16), 44, calcDataSize(1, 2, samples));
    }

    public static WavHeader emptyWavHeader() {
        return new WavHeader("RIFF", 40, "WAVE", new FmtChunk(), 44, 0);
    }

    public static WavHeader read(File file) throws IOException {
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(file));
            return read(is);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public static WavHeader read(InputStream in) throws IOException {
        CountingInputStream cin = new CountingInputStream(in);
        String chunkId = readString(cin, 4);
        int chunkSize = readInt(cin);
        String format = readString(cin, 4);
        FmtChunk fmt = null;

        if (!"RIFF".equals(chunkId) || !"WAVE".equals(format)) {
            return null;
        }
        String fourcc;
        int size = 0;
        do {
            fourcc = readString(cin, 4);
            size = ReaderLE.readInt(cin);
            if ("fmt ".equals(fourcc) && size >= 14 && size <= 1024 * 1024) {
                switch (size) {
                case 16:
                case 18:
                    fmt = FmtChunk.read(cin);
                    StringReader.sureSkip(cin, 2);
                    break;
                case 40:
                    fmt = FmtChunkExtended.read(cin);
                    StringReader.sureSkip(cin, 12);
                    break;
                case 28:
                    fmt = FmtChunkExtended.read(cin);
                    break;
                default:
                    throw new IllegalStateException("Don't know how to handle fmt size: " + size);
                }
            } else if (!"data".equals(fourcc)) {
                StringReader.sureRead(cin, size);
            }
        } while (!"data".equals(fourcc));

        return new WavHeader(chunkId, chunkSize, format, fmt, cin.getCount(), size);
    }

    public static WavHeader multiChannelWav(List<File> wavs) throws IOException {
        return multiChannelWav(wavs.toArray(new File[0]));
    }

    public static WavHeader multiChannelWav(File... wavs) throws IOException {
        WavHeader headers[] = new WavHeader[wavs.length];
        for (int i = 0; i < wavs.length; i++) {
            headers[i] = read(wavs[i]);
        }
        return multiChannelWav(headers);
    }

    /** Takes single channel wavs as input produces multi channel wav */
    public static WavHeader multiChannelWav(WavHeader... wavs) {
        WavHeader w = emptyWavHeader();
        int totalSize = 0;
        for (WavHeader wavHeader : wavs) {
            totalSize += wavHeader.dataSize;
        }
        w.dataSize = totalSize;
        FmtChunk fmt = wavs[0].fmt;
        int bitsPerSample = fmt.bitsPerSample;
        int bytesPerSample = bitsPerSample / 8;
        int sampleRate = (int) fmt.sampleRate;
        w.fmt.bitsPerSample = (short) bitsPerSample;
        w.fmt.blockAlign = (short) (wavs.length * bytesPerSample);
        w.fmt.byteRate = wavs.length * bytesPerSample * sampleRate;
        w.fmt.numChannels = (short) wavs.length;
        w.fmt.sampleRate = sampleRate;
        return w;
    }

    public void write(OutputStream out) throws IOException {
        long chunkSize;
        if (dataSize <= 0xffffffffL) {
            chunkSize = dataSize + 36;
        } else {
            chunkSize = 40;
        }

        out.write("RIFF".getBytes());
        writeInt(out, (int) chunkSize);
        out.write("WAVE".getBytes());

        out.write("fmt ".getBytes());
        writeInt(out, fmt.size());
        fmt.write(out);
        out.write("data".getBytes());
        if (dataSize <= 0xffffffffL) {
            writeInt(out, (int) dataSize);
        } else {
            writeInt(out, 0);
        }
    }

    public void writeExtended(OutputStream out) throws IOException {
        long chunkSize;
        if (dataSize <= 0xffffffffL) {
            chunkSize = dataSize + 36;
        } else {
            chunkSize = 40;
        }

        out.write("RIFF".getBytes());
        writeInt(out, (int) chunkSize);
        out.write("WAVE".getBytes());

        out.write("fmt ".getBytes());
        writeInt(out, fmt.size());
        fmt.write(out);
        out.write("data".getBytes());

        if (dataSize <= 0xffffffffL) {
            writeInt(out, (int) dataSize);
        } else {
            writeInt(out, 0);
        }
    }

    public static long calcDataSize(int numChannels, int bytesPerSample, long samples) {
        return samples * numChannels * bytesPerSample;
    }

    public static WavHeader create(AudioFormat af, int size) {
        WavHeader w = emptyWavHeader();
        w.dataSize = size;
        FmtChunk fmt = new FmtChunk();
        int bitsPerSample = af.getSampleSizeInBits();
        int bytesPerSample = bitsPerSample / 8;
        int sampleRate = (int) af.getSampleRate();
        w.fmt.bitsPerSample = (short) bitsPerSample;
        w.fmt.blockAlign = (short) (af.getFrameSize());
        w.fmt.byteRate = (int) af.getFrameRate() * af.getFrameSize();
        w.fmt.numChannels = (short) af.getChannels();
        w.fmt.sampleRate = (int) af.getSampleRate();
        return w;
    }

    public ChannelLabel[] getChannelLabels() {
        if (fmt instanceof FmtChunkExtended) {
            return ((FmtChunkExtended) fmt).getLabels();
        } else {
            switch (fmt.numChannels) {
            case 1:
                return new ChannelLabel[] { ChannelLabel.MONO };
            case 2:
                return new ChannelLabel[] { ChannelLabel.STEREO_LEFT, ChannelLabel.STEREO_RIGHT };
            case 3:
                return new ChannelLabel[] { ChannelLabel.FRONT_LEFT, ChannelLabel.FRONT_RIGHT, ChannelLabel.REAR_CENTER };
            case 4:
                return new ChannelLabel[] { ChannelLabel.FRONT_LEFT, ChannelLabel.FRONT_RIGHT, ChannelLabel.REAR_LEFT,
                        ChannelLabel.REAR_RIGHT };
            case 5:
                return new ChannelLabel[] { ChannelLabel.FRONT_LEFT, ChannelLabel.FRONT_RIGHT, ChannelLabel.CENTER,
                        ChannelLabel.REAR_LEFT, ChannelLabel.REAR_RIGHT };
            case 6:
                return new ChannelLabel[] { ChannelLabel.FRONT_LEFT, ChannelLabel.FRONT_RIGHT, ChannelLabel.CENTER,
                        ChannelLabel.LFE, ChannelLabel.REAR_LEFT, ChannelLabel.REAR_RIGHT };
            case 7:
                return new ChannelLabel[] { ChannelLabel.FRONT_LEFT, ChannelLabel.FRONT_RIGHT, ChannelLabel.CENTER,
                        ChannelLabel.LFE, ChannelLabel.REAR_LEFT, ChannelLabel.REAR_RIGHT, ChannelLabel.REAR_CENTER };
            case 8:
                return new ChannelLabel[] { ChannelLabel.FRONT_LEFT, ChannelLabel.FRONT_RIGHT, ChannelLabel.CENTER,
                        ChannelLabel.LFE, ChannelLabel.REAR_LEFT, ChannelLabel.REAR_RIGHT, ChannelLabel.REAR_LEFT,
                        ChannelLabel.REAR_RIGHT };
            default:
                ChannelLabel[] labels = new ChannelLabel[fmt.numChannels];
                Arrays.fill(labels, ChannelLabel.MONO);
                return labels;
            }
        }
    }
}
