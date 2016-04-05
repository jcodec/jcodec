package org.jcodec.codecs.wav;
import org.jcodec.api.UnhandledStateException;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.JCodecUtil2;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ChannelLabel;

import js.io.File;
import js.io.IOException;
import js.nio.ByteBuffer;
import js.nio.ByteOrder;
import js.nio.channels.ReadableByteChannel;
import js.nio.channels.WritableByteChannel;
import js.util.ArrayList;
import js.util.Arrays;
import js.util.List;

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

        public FmtChunkExtended(FmtChunk other, short cbSize, short bitsPerCodedSample, int channelLayout, int guid) {
            super(other.audioFormat, other.numChannels, other.sampleRate, other.byteRate, other.blockAlign,
                    other.bitsPerSample);
            this.cbSize = cbSize;
            this.bitsPerCodedSample = bitsPerCodedSample;
            this.channelLayout = channelLayout;
            this.guid = guid;
        }

        public static FmtChunk read(ByteBuffer bb) throws IOException {
            FmtChunk fmtChunk = FmtChunk.get(bb);
            ByteOrder old = (ByteOrder) bb.getOrder();
            try {
                bb.order(ByteOrder.LITTLE_ENDIAN);
                return new FmtChunkExtended(fmtChunk, bb.getShort(), bb.getShort(), bb.getInt(), bb.getInt());
            } finally {
                bb.order(old);
            }
        }

        public void put(ByteBuffer bb) throws IOException {
            super.put(bb);
            ByteOrder old = (ByteOrder) bb.getOrder();
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.putShort(cbSize);
            bb.putShort(bitsPerCodedSample);
            bb.putInt(channelLayout);
            bb.putInt(guid);
            bb.order(old);
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

        public FmtChunk(short audioFormat, short numChannels, int sampleRate, int byteRate, short blockAlign,
                short bitsPerSample) {
            this.audioFormat = audioFormat;
            this.numChannels = numChannels;
            this.sampleRate = sampleRate;
            this.byteRate = byteRate;
            this.blockAlign = blockAlign;
            this.bitsPerSample = bitsPerSample;
        }

        public static FmtChunk get(ByteBuffer bb) throws IOException {
            ByteOrder old = (ByteOrder) bb.getOrder();
            try {
                bb.order(ByteOrder.LITTLE_ENDIAN);
                return new FmtChunk(bb.getShort(), bb.getShort(), bb.getInt(), bb.getInt(), bb.getShort(),
                        bb.getShort());
            } finally {
                bb.order(old);
            }
        }

        public void put(ByteBuffer bb) throws IOException {
            ByteOrder old = (ByteOrder) bb.getOrder();
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.putShort(audioFormat);
            bb.putShort(numChannels);
            bb.putInt(sampleRate);
            bb.putInt(byteRate);
            bb.putShort(blockAlign);
            bb.putShort(bitsPerSample);
            bb.order(old);
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

    public static WavHeader copyWithRate(WavHeader header, int rate) {
        WavHeader result = new WavHeader(header.chunkId, header.chunkSize, header.format,
                copyFmt(header.fmt), header.dataOffset, header.dataSize);
        result.fmt.sampleRate = rate;
        return result;
    }

    public static WavHeader copyWithChannels(WavHeader header, int channels) {
        WavHeader result = new WavHeader(header.chunkId, header.chunkSize, header.format,
                copyFmt(header.fmt), header.dataOffset, header.dataSize);
        result.fmt.numChannels = (short) channels;
        return result;
    }

    private static FmtChunk copyFmt(FmtChunk fmt) {
        if (fmt instanceof FmtChunkExtended) {
            FmtChunkExtended fmtext = (FmtChunkExtended) fmt;
            fmt = new FmtChunkExtended(fmtext, fmtext.cbSize, fmtext.bitsPerCodedSample, fmtext.channelLayout, fmtext.guid);
        }        else {
            fmt = new FmtChunk(fmt.audioFormat, fmt.numChannels, fmt.sampleRate, fmt.byteRate, fmt.blockAlign,
                    fmt.bitsPerSample);
        }
        return fmt;
    }

    /**
     * Creates wav header for the specified audio format
     * 
     * @param format
     * @param samples
     */
    public static WavHeader createWavHeader(AudioFormat format, int samples) {
        WavHeader w = new WavHeader("RIFF", 40, "WAVE", new FmtChunk((short) 1, (short) format.getChannels(), format.getSampleRate(),
                format.getSampleRate() * format.getChannels() * (format.getSampleSizeInBits() >> 3),
                (short) (format.getChannels() * (format.getSampleSizeInBits() >> 3)),
                (short) format.getSampleSizeInBits()), 44, calcDataSize(format.getChannels(),
                format.getSampleSizeInBits() >> 3, samples));
        return w;
    }

    public static WavHeader stereo48k() {
        return stereo48kWithSamples(0);
    }

    public static WavHeader stereo48kWithSamples(long samples) {
        return new WavHeader("RIFF", 40, "WAVE", new FmtChunk((short) 1, (short) 2, 48000, 48000 * 2 * 16 / 8,
                (short) 4, (short) 16), 44, calcDataSize(2, 2, samples));
    }

    public static WavHeader mono48k(long samples) {
        return new WavHeader("RIFF", 40, "WAVE", new FmtChunk((short) 1, (short) 1, 48000, 48000 * 1 * 16 / 8,
                (short) 2, (short) 16), 44, calcDataSize(1, 2, samples));
    }

    public static WavHeader emptyWavHeader() {
        return new WavHeader("RIFF", 40, "WAVE", newFmtChunk(), 44, 0);
    }

    private static FmtChunk newFmtChunk() {
        return new FmtChunk((short) 1, (short) 0, 0, 0, (short) 0, (short) 0);
    }

    public static WavHeader read(File file) throws IOException {
        ReadableByteChannel is = null;
        try {
            is = NIOUtils.readableChannel(file);
            return readChannel(is);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public static WavHeader readChannel(ReadableByteChannel _in) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(128);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        _in.read(buf);
        if (buf.remaining() > 0)
            throw new IOException("Incomplete wav header found");
        buf.flip();

        String chunkId = NIOUtils.readString(buf, 4);
        int chunkSize = buf.getInt();
        String format = NIOUtils.readString(buf, 4);
        FmtChunk fmt = null;

        if (!"RIFF".equals(chunkId) || !"WAVE".equals(format)) {
            return null;
        }
        String fourcc;
        int size = 0;
        do {
            fourcc = NIOUtils.readString(buf, 4);
            size = buf.getInt();
            if ("fmt ".equals(fourcc) && size >= 14 && size <= 1024 * 1024) {
                switch (size) {
                case 16:
                    fmt = FmtChunk.get(buf);
                    break;
                case 18:
                    fmt = FmtChunk.get(buf);
                    NIOUtils.skip(buf, 2);
                    break;
                case 40:
                    fmt = FmtChunkExtended.get(buf);
                    NIOUtils.skip(buf, 12);
                    break;
                case 28:
                    fmt = FmtChunkExtended.get(buf);
                    break;
                default:
                    throw new UnhandledStateException("Don't know how to handle fmt size: " + size);
                }
            } else if (!"data".equals(fourcc)) {
                NIOUtils.skip(buf, size);
            }
        } while (!"data".equals(fourcc));

        return new WavHeader(chunkId, chunkSize, format, fmt, buf.position(), size);
    }

    public static WavHeader multiChannelWavFromFiles(File... arguments) throws IOException {
        WavHeader headers[] = new WavHeader[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            headers[i] = read(arguments[i]);
        }
        return multiChannelWav(headers);
    }

    /** Takes single channel wavs as input produces multi channel wav */
    public static WavHeader multiChannelWav(WavHeader... arguments) {
        WavHeader w = emptyWavHeader();
        int totalSize = 0;
        for (int i = 0; i < arguments.length; i++) {
            WavHeader wavHeader = arguments[i];
            totalSize += wavHeader.dataSize;
        }
        w.dataSize = totalSize;
        FmtChunk fmt = arguments[0].fmt;
        int bitsPerSample = fmt.bitsPerSample;
        int bytesPerSample = bitsPerSample / 8;
        int sampleRate = (int) fmt.sampleRate;
        w.fmt.bitsPerSample = (short) bitsPerSample;
        w.fmt.blockAlign = (short) (arguments.length * bytesPerSample);
        w.fmt.byteRate = arguments.length * bytesPerSample * sampleRate;
        w.fmt.numChannels = (short) arguments.length;
        w.fmt.sampleRate = sampleRate;
        return w;
    }

    public void write(WritableByteChannel out) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(44);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        long chunkSize;
        if (dataSize <= 0xffffffffL) {
            chunkSize = dataSize + 36;
        } else {
            chunkSize = 40;
        }

        bb.putArr(JCodecUtil2.asciiString("RIFF"));
        bb.putInt((int) chunkSize);
        bb.putArr(JCodecUtil2.asciiString("WAVE"));

        bb.putArr(JCodecUtil2.asciiString("fmt "));
        bb.putInt(fmt.size());
        fmt.put(bb);
        bb.putArr(JCodecUtil2.asciiString("data"));
        if (dataSize <= 0xffffffffL) {
            bb.putInt((int) dataSize);
        } else {
            bb.putInt(0);
        }
        bb.flip();
        out.write(bb);
    }

    public static long calcDataSize(int numChannels, int bytesPerSample, long samples) {
        return samples * numChannels * bytesPerSample;
    }

    public static WavHeader create(AudioFormat af, int size) {
        WavHeader w = emptyWavHeader();
        w.dataSize = size;
        FmtChunk fmt = newFmtChunk();
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

    public AudioFormat getFormat() {
        return new AudioFormat(fmt.sampleRate, fmt.bitsPerSample, fmt.numChannels, true, false);
    }
}
