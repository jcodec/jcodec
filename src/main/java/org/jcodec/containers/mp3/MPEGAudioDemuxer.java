package org.jcodec.containers.mp3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.AudioCodecMeta;
import org.jcodec.common.Codec;
import org.jcodec.common.Demuxer;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.TrackType;
import org.jcodec.common.UsedViaReflection;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Packet.FrameType;

/**
 * Demuxer for MPEG 1/2 audio layer 1,2,3 (MP3).
 * 
 * Extracts raw MPEG audio frames from the ES.
 * 
 * See http://mpgedit.org/mpgedit/mpeg_format/mpeghdr.htm for more detail.
 * 
 * @author Stanislav Vitvitskiy
 */
public class MPEGAudioDemuxer implements Demuxer, DemuxerTrack {
    private static int field(int off, int size) {
        return (((1 << size) - 1) << 16) | off;
    }

    private static final int MAX_FRAME_SIZE = 1728;
    private static final int MIN_FRAME_SIZE = 52;
    private static final int CHANNELS = field(6, 2);
    private static final int PADDING = field(9, 1);
    private static final int SAMPLE_RATE = field(10, 2);
    private static final int BITRATE = field(12, 4);
    private static final int VERSION = field(19, 2);
    private static final int LAYER = field(17, 2);
    private static final int SYNC = field(21, 11);

    private static final int MPEG1 = 0x3;
    private static final int MPEG2 = 0x2;
    private static final int MPEG25 = 0x0;

    private static int bitrateTable[][][] = {
            { { 0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448 },
                    { 0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384 },
                    { 0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320 } },
            { { 0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256 },
                    { 0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160 },
                    { 0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160 } } };
    private static int freqTab[] = { 44100, 48000, 32000 };
    private static int rateReductTab[] = { 2, 0, 1, 0 };

    private static int getField(int header, int field) {
        return (header >> (field & 0xffff)) & (field >> 16);
    }

    private SeekableByteChannel ch;
    private List<DemuxerTrack> tracks;
    private int frameNo;
    private ByteBuffer readBuffer;
    private int runningFour;
    private boolean eof;
    private DemuxerTrackMeta meta;
    private int sampleRate;

    public MPEGAudioDemuxer(SeekableByteChannel ch) throws IOException {
        this.ch = ch;
        readBuffer = ByteBuffer.allocate(1 << 18); // 256K
        readMoreData();
        if (readBuffer.remaining() < 4) {
            eof = true;
        } else {
            runningFour = readBuffer.getInt();
            if (!validHeader(runningFour)) {
                eof = skipJunk();
            }
            extractMeta();
        }

        tracks = new ArrayList<DemuxerTrack>();
        tracks.add(this);
    }

    private void extractMeta() {
        if (!validHeader(runningFour))
            return;
        int layer = 3 - getField(runningFour, LAYER);
        int channelCount = getField(runningFour, CHANNELS) == 3 ? 1 : 2;

        int version = getField(runningFour, VERSION);
        sampleRate = freqTab[getField(runningFour, SAMPLE_RATE)] >> rateReductTab[version];
        AudioCodecMeta codecMeta = AudioCodecMeta.createAudioCodecMeta(".mp3", 16, channelCount, sampleRate,
                ByteOrder.LITTLE_ENDIAN, false, null, null);
        Codec codec = layer == 2 ? Codec.MP3 : (layer == 1 ? Codec.MP2 : Codec.MP1);
        meta = new DemuxerTrackMeta(TrackType.AUDIO, codec, 0, null, 0, null, null, codecMeta);
    }

    @Override
    public void close() throws IOException {
        ch.close();
    }

    @Override
    public List<? extends DemuxerTrack> getTracks() {
        return tracks;
    }

    @Override
    public List<? extends DemuxerTrack> getVideoTracks() {
        return null;
    }

    @Override
    public List<? extends DemuxerTrack> getAudioTracks() {
        return tracks;
    }

    @Override
    public Packet nextFrame() throws IOException {
        if (eof)
            return null;
        if (!validHeader(runningFour)) {
            eof = skipJunk();
        }
        int frameSize = calcFrameSize(runningFour);
        ByteBuffer frame = ByteBuffer.allocate(frameSize);
        eof = readFrame(frame);
        frame.flip();

        Packet pkt = new Packet(frame, frameNo * 1152, sampleRate, 1152, frameNo, FrameType.KEY, null, 0);

        ++frameNo;

        return pkt;
    }

    private static boolean validHeader(int four) {
        if (getField(four, SYNC) != 0x7ff)
            return false;
        if (getField(four, LAYER) == 0)
            return false;
        if (getField(four, SAMPLE_RATE) == 3)
            return false;
        if (getField(four, BITRATE) == 0xf)
            return false;
        return true;
    }

    private void readMoreData() throws IOException {
        readBuffer.clear();
        ch.read(readBuffer);
        readBuffer.flip();
    }

    private static int calcFrameSize(int header) {
        int bitrateIdx = getField(header, BITRATE);
        int layer = 3 - getField(header, LAYER);
        int version = getField(header, VERSION);
        int mpeg2 = version != 3 ? 1 : 0;
        int bitRate = bitrateTable[mpeg2][layer][bitrateIdx] * 1000;
        int sampleRate = freqTab[getField(header, SAMPLE_RATE)] >> rateReductTab[version];
        int padding = getField(header, PADDING);
        int lsf = version == MPEG25 || version == MPEG2 ? 1 : 0;
        switch (layer) {
        case 0:
            return ((bitRate * 12) / sampleRate + padding) * 4;
        case 1:
            return (bitRate * 144) / sampleRate + padding;
        default:
        case 2:
            return (bitRate * 144) / (sampleRate << lsf) + padding;
        }
    }

    private boolean readFrame(ByteBuffer frame) throws IOException {
        boolean eof = false;
        while (frame.hasRemaining()) {
            frame.put((byte) (runningFour >> 24));
            runningFour <<= 8;
            if (!readBuffer.hasRemaining())
                readMoreData();
            if (readBuffer.hasRemaining())
                runningFour |= readBuffer.get() & 0xff;
            else
                eof = true;
        }
        return eof;
    }

    private boolean skipJunk() throws IOException {
        boolean eof = false;
        int total = 0;
        while (!validHeader(runningFour)) {
            if (!readBuffer.hasRemaining())
                readMoreData();
            if (!readBuffer.hasRemaining()) {
                eof = true;
                break;
            }
            runningFour <<= 8;
            runningFour |= readBuffer.get() & 0xff;
            ++total;
        }
        Logger.warn(String.format("[mp3demuxer] Skipped %d bytes of junk", total));
        return eof;
    }

    @Override
    public DemuxerTrackMeta getMeta() {
        return meta;
    }

    /**
     * Used to auto-detect MPEG Audio (MP3) files
     * 
     * @param b
     *            Buffer containing a snippet of data
     * @return Score from 0 to 100
     */
    @UsedViaReflection
    public static int probe(final ByteBuffer b) {
        ByteBuffer fork = b.duplicate();

        int valid = 0, total = 0;
        int header = fork.getInt();
        do {
            if (!validHeader(header))
                header = skipJunkBB(header, fork);
            int size = calcFrameSize(header);
            if (fork.remaining() < size)
                break;
            ++total;
            if (size > 0)
                NIOUtils.skip(fork, size - 4);
            else
                header = skipJunkBB(header, fork);
            if (fork.remaining() >= 4) {
                header = fork.getInt();
                if (size >= MIN_FRAME_SIZE && size <= MAX_FRAME_SIZE && validHeader(header))
                    valid++;
            }
        } while (fork.remaining() >= 4);

        return (100 * valid) / total;
    }

    private static int skipJunkBB(int header, ByteBuffer fork) {
        while (!validHeader(header) && fork.hasRemaining()) {
            header <<= 8;
            header |= fork.get() & 0xff;
        }
        return header;
    }
}
