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

    private static int field(int off, int size) {
	return (((1 << size) - 1) << 16) | off;
    }

    private static int getField(int header, int field) {
	return (header >> (field & 0xffff)) & (field >> 16);
    }

    private SeekableByteChannel ch;
    private List<DemuxerTrack> tracks;
    private int frameNo;
    private ByteBuffer readBuffer;
    private int runningFour;
    private int channelCount;
    private int sampleRate;
    private boolean eof;

    public MPEGAudioDemuxer(SeekableByteChannel ch) throws IOException {
	this.ch = ch;
	readBuffer = ByteBuffer.allocate(1 << 18); // 256K
	readMoreData();
	if (readBuffer.remaining() < 4) {
	    eof = true;
	} else {
	    runningFour = readBuffer.getInt();
	}
	tracks = new ArrayList<DemuxerTrack>();
	tracks.add(this);
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
	if (!validBoundary(runningFour)) {
	    eof = skipJunk();
	}
	int frameSize = calcFrameSize(runningFour);
	ByteBuffer frame = ByteBuffer.allocate(frameSize);
	eof = readFrame(frame);
	frame.flip();

	int header = frame.getInt(frame.position());

	sampleRate = getSampleRate(header);
	channelCount = getChannelCount(header);
	Packet pkt = new Packet(frame, frameNo * 1152, sampleRate, 1152, frameNo, FrameType.KEY, null, 0);

	++frameNo;

	return pkt;
    }

    private int getChannelCount(int header) {
	int id = (header >> 6) & 0x3;
	return id == 3 ? 1 : 2;
    }

    private int getSampleRate(int header) {
	int id = (header >> 10) & 0x3;
	return id == 0 ? 44100 : (id == 1 ? 48000 : 32000);
    }

    private boolean validBoundary(int four) {
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

    private int calcFrameSize(int header) {
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
	while (!validBoundary(runningFour)) {
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
	Logger.warn(String.format("[mp3demuxer] Skipped %d byts of junk", total));
	return eof;
    }

    @Override
    public DemuxerTrackMeta getMeta() {
	return new DemuxerTrackMeta(TrackType.AUDIO, Codec.MP3, 0, null, 0, null, null,
		AudioCodecMeta.createAudioCodecMeta(".mp3", 16, channelCount, sampleRate, ByteOrder.LITTLE_ENDIAN,
			false, null, null));
    }
}
