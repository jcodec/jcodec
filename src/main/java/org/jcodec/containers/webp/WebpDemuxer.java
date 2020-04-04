package org.jcodec.containers.webp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.Demuxer;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.UsedViaReflection;
import org.jcodec.common.io.DataReader;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Packet.FrameType;
import org.jcodec.containers.mp4.demuxer.DemuxerProbe;
import org.jcodec.platform.Platform;
import org.jetbrains.annotations.NotNull;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Reads integer samples from the wav file
 *
 * @author Stanislav Vitvitskiy
 */
public class WebpDemuxer implements Demuxer, DemuxerTrack {
    public final static int FOURCC_RIFF = 0x46464952; // 'RIFF'
    public final static int FOURCC_WEBP = 0x50424557; // 'WEBP'
    public final static int FOURCC_VP8 = 0x20385056; // 'VP8 '
    public final static int FOURCC_ICCP = 0x50434349;
    public final static int FOURCC_ANIM = 0x4d494e41;
    public final static int FOURCC_ANMF = 0x464d4e41;
    public final static int FOURCC_XMP = 0x20504d58;
    public final static int FOURCC_EXIF = 0x46495845;
    public final static int FOURCC_ALPH = 0x48504c41;
    public final static int FOURCC_VP8L = 0x4c385056;
    public final static int FOURCC_VP8X = 0x58385056;

    private ArrayList<DemuxerTrack> vt;
    private boolean headerRead;
    private DataReader raf;
    private boolean done;

    public WebpDemuxer(SeekableByteChannel channel) {
        this.raf = DataReader.createDataReader(channel, ByteOrder.LITTLE_ENDIAN);
        vt = new ArrayList<DemuxerTrack>();
        vt.add(this);
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }

    @Override
    public Packet nextFrame() throws IOException {
        if (done)
            return null;

        if (!headerRead) {
            readHeader();
            headerRead = true;
        }
        int fourCC = raf.readInt();
        int size = raf.readInt();
        done = true;
        switch (fourCC) {
            case FOURCC_VP8:
                byte[] b = new byte[size];
                raf.readFully(b);
                return new Packet(ByteBuffer.wrap(b), 0, 25, 1, 0, FrameType.KEY, null, 0);
            case FOURCC_ICCP:
            case FOURCC_ANIM:
            case FOURCC_ANMF:
            case FOURCC_XMP:
            case FOURCC_EXIF:
            case FOURCC_ALPH:
            case FOURCC_VP8L:
            case FOURCC_VP8X:
            default:
                Logger.warn("Skipping unsupported chunk: " + dwToFourCC(fourCC) + ".");
                byte[] b1 = new byte[size];
                raf.readFully(b1);
        }
        return null;
    }

    private void readHeader() throws IOException {
        if (raf.readInt() != FOURCC_RIFF)
            throw new IOException("Invalid RIFF file.");
        int size = raf.readInt(); // Size must be sane
        if (raf.readInt() != FOURCC_WEBP)
            throw new IOException("Not a WEBP file.");
    }

    @Override
    public DemuxerTrackMeta getMeta() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<? extends DemuxerTrack> getTracks() {
        return vt;
    }

    @Override
    public List<? extends DemuxerTrack> getVideoTracks() {
        return vt;
    }

    @Override
    public List<? extends DemuxerTrack> getAudioTracks() {
        return new ArrayList<DemuxerTrack>();
    }

    public final static DemuxerProbe PROBE = b -> {
        ByteBuffer _b = b.duplicate();
        if (_b.remaining() < 12)
            return 0;
        _b.order(ByteOrder.LITTLE_ENDIAN);
        if (_b.getInt() != FOURCC_RIFF)
            return 0;
        int size = _b.getInt(); // Size must be sane
        if (_b.getInt() != FOURCC_WEBP)
            return 0;
        return 100;
    };

    public static String dwToFourCC(int fourCC) {
        char[] ch = new char[4];
        ch[0] = (char) ((fourCC >> 24) & 0xff);
        ch[1] = (char) ((fourCC >> 16) & 0xff);
        ch[2] = (char) ((fourCC >> 8) & 0xff);
        ch[3] = (char) ((fourCC >> 0) & 0xff);
        return Platform.stringFromChars(ch);
    }
}
