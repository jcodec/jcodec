package org.jcodec.containers.mps;

import static org.jcodec.common.NIOUtils.getRel;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.EnumSet;

import org.jcodec.common.Assert;
import org.jcodec.common.IntArrayList;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.containers.mps.psi.PATSection;
import org.jcodec.containers.mps.psi.PMTSection;
import org.jcodec.containers.mps.psi.PMTSection.PMTStream;
import org.jcodec.containers.mps.psi.PSISection;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MTSUtils {
    public static enum StreamType {
        RESERVED(0x0, false, false),

        VIDEO_MPEG1(0x01, true, false),

        VIDEO_MPEG2(0x02, true, false),

        AUDIO_MPEG1(0x03, false, true),

        AUDIO_MPEG2(0x04, false, true),

        PRIVATE_SECTION(0x05, false, false),

        PRIVATE_DATA(0x06, false, false),

        MHEG(0x7, false, false),

        DSM_CC(0x8, false, false),

        ATM_SYNC(0x9, false, false),

        DSM_CC_A(0xa, false, false),

        DSM_CC_B(0xb, false, false),

        DSM_CC_C(0xc, false, false),

        DSM_CC_D(0xd, false, false),

        MPEG_AUX(0xe, false, false),

        AUDIO_AAC_ADTS(0x0f, false, true),

        VIDEO_MPEG4(0x10, true, false),

        AUDIO_AAC_LATM(0x11, false, true),

        FLEXMUX_PES(0x12, false, false),

        FLEXMUX_SEC(0x13, false, false),

        DSM_CC_SDP(0x14, false, false),

        META_PES(0x15, false, false),

        META_SEC(0x16, false, false),

        DSM_CC_DATA_CAROUSEL(0x17, false, false),

        DSM_CC_OBJ_CAROUSEL(0x18, false, false),

        DSM_CC_SDP1(0x19, false, false),

        IPMP(0x1a, false, false),

        VIDEO_H264(0x1b, true, false),

        AUDIO_AAC_RAW(0x1c, false, true),

        SUBS(0x1d, false, false),

        AUX_3D(0x1e, false, false),

        VIDEO_AVC_SVC(0x1f, true, false),

        VIDEO_AVC_MVC(0x20, true, false),

        VIDEO_J2K(0x21, true, false),

        VIDEO_MPEG2_3D(0x22, true, false),

        VIDEO_H264_3D(0x23, true, false),

        VIDEO_CAVS(0x42, false, true),

        IPMP_STREAM(0x7f, false, false),

        AUDIO_AC3(0x81, false, true),

        AUDIO_DTS(0x8a, false, true);

        private int tag;
        private boolean video;
        private boolean audio;
        private static EnumSet<StreamType> typeEnum = EnumSet.allOf(StreamType.class);

        private StreamType(int tag, boolean video, boolean audio) {
            this.tag = tag;
            this.video = video;
            this.audio = audio;
        }

        public static StreamType fromTag(int streamTypeTag) {
            for (StreamType streamType : typeEnum) {
                if (streamType.tag == streamTypeTag)
                    return streamType;
            }
            return null;
        }

        public int getTag() {
            return tag;
        }

        public boolean isVideo() {
            return video;
        }

        public boolean isAudio() {
            return audio;
        }
    };

    /**
     * Parses PAT ( Program Association Table )
     * 
     * @param data
     * @deprecated Use org.jcodec.containers.mps.psi.PAT.parse method instead,
     *             this method will not work correctly for streams with multiple
     *             programs
     * @return Pid of the first PMT found in the PAT
     */
    @Deprecated
    public static int parsePAT(ByteBuffer data) {
        PATSection pat = PATSection.parse(data);
        if (pat.getPrograms().size() > 0)
            return pat.getPrograms().values()[0];
        else
            return -1;
    }

    @Deprecated
    public static PMTSection parsePMT(ByteBuffer data) {
        return PMTSection.parse(data);
    }

    @Deprecated
    public static PSISection parseSection(ByteBuffer data) {
        return PSISection.parse(data);
    }

    private static void parseEsInfo(ByteBuffer read) {

    }

    public static PMTStream[] getProgramGuids(File src) throws IOException {
        SeekableByteChannel ch = null;
        try {
            ch = NIOUtils.readableFileChannel(src);
            return getProgramGuids(ch);
        } finally {
            NIOUtils.closeQuietly(ch);
        }
    }

    public static PMTStream[] getProgramGuids(SeekableByteChannel in) throws IOException {
        PMTExtractor ex = new PMTExtractor();
        ex.readTsFile(in);
        PMTSection pmt = ex.getPmt();
        return pmt.getStreams();
    }

    private static class PMTExtractor extends TSReader {
        private int pmtGuid = -1;
        private PMTSection pmt;

        @Override
        public boolean onPkt(int guid, boolean payloadStart, ByteBuffer tsBuf, long filePos, boolean sectionSyntax,
                ByteBuffer fullPkt) {
            if (guid == 0) {
                pmtGuid = parsePAT(tsBuf);
            } else if (pmtGuid != -1 && guid == pmtGuid) {
                pmt = parsePMT(tsBuf);
                return false;
            }
            return true;
        }

        public PMTSection getPmt() {
            return pmt;
        }
    };

    public static abstract class TSReader {
        private static final int TS_SYNC_MARKER = 0x47;
        private static final int TS_PKT_SIZE = 188;
        // Buffer must have an integral number of MPEG TS packets
        public static final int BUFFER_SIZE = TS_PKT_SIZE << 9;
        private boolean flush;

        public TSReader() {
            this(false);
        }

        public TSReader(boolean flush) {
            this.flush = flush;
        }

        public void readTsFile(SeekableByteChannel ch) throws IOException {
            ch.position(0);
            ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);

            for (long pos = ch.position(); ch.read(buf) >= TS_PKT_SIZE; pos = ch.position()) {
                long posRem = pos;
                buf.flip();
                while (buf.remaining() >= TS_PKT_SIZE) {
                    ByteBuffer tsBuf = NIOUtils.read(buf, TS_PKT_SIZE);
                    ByteBuffer fullPkt = tsBuf.duplicate();
                    pos += TS_PKT_SIZE;
                    Assert.assertEquals(TS_SYNC_MARKER, tsBuf.get() & 0xff);
                    int guidFlags = ((tsBuf.get() & 0xff) << 8) | (tsBuf.get() & 0xff);
                    int guid = (int) guidFlags & 0x1fff;

                    int payloadStart = (guidFlags >> 14) & 0x1;
                    int b0 = tsBuf.get() & 0xff;
                    int counter = b0 & 0xf;
                    if ((b0 & 0x20) != 0) {
                        NIOUtils.skip(tsBuf, tsBuf.get() & 0xff);
                    }
                    boolean sectionSyntax = payloadStart == 1 && (getRel(tsBuf, getRel(tsBuf, 0) + 2) & 0x80) == 0x80;
                    if (sectionSyntax) {
                        // Adaptation field
                        NIOUtils.skip(tsBuf, tsBuf.get() & 0xff);
                    }
                    if (!onPkt(guid, payloadStart == 1, tsBuf, pos - tsBuf.remaining(), sectionSyntax, fullPkt))
                        return;
                }
                if (flush) {
                    buf.flip();
                    ch.position(posRem);
                    ch.write(buf);
                }
                buf.clear();
            }
        }

        public boolean onPkt(int guid, boolean payloadStart, ByteBuffer tsBuf, long filePos, boolean sectionSyntax,
                ByteBuffer fullPkt) {
            // DO NOTHING
            return true;
        }
    }

    public static int getVideoPid(File src) throws IOException {
        for (PMTStream stream : MTSUtils.getProgramGuids(src)) {
            if (stream.getStreamType().isVideo())
                return stream.getPid();
        }

        throw new RuntimeException("No video stream");
    }

    public static int getAudioPid(File src) throws IOException {
        for (PMTStream stream : MTSUtils.getProgramGuids(src)) {
            if (stream.getStreamType().isAudio())
                return stream.getPid();
        }

        throw new RuntimeException("No audio stream");
    }

    public static int[] getMediaPids(SeekableByteChannel src) throws IOException {
        return filterMediaPids(MTSUtils.getProgramGuids(src));
    }

    public static int[] getMediaPids(File src) throws IOException {
        return filterMediaPids(MTSUtils.getProgramGuids(src));
    }

    private static int[] filterMediaPids(PMTStream[] programs) {
        IntArrayList result = new IntArrayList();
        for (PMTStream stream : programs) {
            if (stream.getStreamType().isVideo() || stream.getStreamType().isAudio())
                result.add(stream.getPid());
        }

        return result.toArray();
    }
}