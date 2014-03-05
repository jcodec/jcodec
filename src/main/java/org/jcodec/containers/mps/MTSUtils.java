package org.jcodec.containers.mps;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.jcodec.common.Assert;
import org.jcodec.common.IntArrayList;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;

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

    public static class PMT {

        private int pcrPid;
        private List<Tag> tags;
        private List<Stream> streams;

        public PMT(int pcrPid, List<Tag> tags, List<Stream> streams) {
            this.pcrPid = pcrPid;
            this.tags = tags;
            this.streams = streams;
        }

        public int getPcrPid() {
            return pcrPid;
        }

        public List<Tag> getTags() {
            return tags;
        }

        public List<Stream> getStreams() {
            return streams;
        }
    }

    public static class Tag {
        private int tag;
        private ByteBuffer content;

        public Tag(int tag, ByteBuffer content) {
            this.tag = tag;
            this.content = content;
        }

        public int getTag() {
            return tag;
        }

        public ByteBuffer getContent() {
            return content;
        }
    }

    public static class Stream {
        private int streamTypeTag;
        private int pid;
        private ByteBuffer info;
        private StreamType streamType;

        public Stream(int streamTypeTag, int pid, ByteBuffer info) {
            this.streamTypeTag = streamTypeTag;
            this.pid = pid;
            this.info = info.duplicate();
            this.streamType = StreamType.fromTag(streamTypeTag);
        }

        public int getStreamTypeTag() {
            return streamTypeTag;
        }

        public StreamType getStreamType() {
            return streamType;
        }

        public int getPid() {
            return pid;
        }

        public ByteBuffer getInfo() {
            return info;
        }
    }

    public static int parsePAT(ByteBuffer data) {
        parseSection(data);
        int pmtPid = -1;
        while (data.remaining() > 4) {
            int programNum = data.getShort() & 0xffff;
            int w = data.getShort();
            if (programNum != 0)
                pmtPid = w & 0x1fff;
        }

        return pmtPid;
    }

    public static PMT parsePMT(ByteBuffer data) {
        parseSection(data);

        // PMT itself
        int w1 = data.getShort() & 0xffff;
        int pcrPid = w1 & 0x1fff;

        int w2 = data.getShort() & 0xffff;
        int programInfoLength = w2 & 0xfff;

        List<Tag> tags = parseTags(NIOUtils.read(data, programInfoLength));
        List<Stream> streams = new ArrayList<Stream>();
        while (data.remaining() > 4) {
            int streamType = data.get() & 0xff;
            int wn = data.getShort() & 0xffff;
            int elementaryPid = wn & 0x1fff;

            System.out.println(String.format("Elementary stream: [%d,%d]", streamType, elementaryPid));

            int wn1 = data.getShort() & 0xffff;
            int esInfoLength = wn1 & 0xfff;
            streams.add(new Stream(streamType, elementaryPid, NIOUtils.read(data, esInfoLength)));
        }

        return new PMT(pcrPid, tags, streams);
    }

    public static void parseSection(ByteBuffer data) {
        int tableId = data.get() & 0xff;
        int w0 = data.getShort() & 0xffff;
        int sectionSyntaxIndicator = w0 >> 15;
        if (((w0 >> 14) & 1) != 0)
            throw new RuntimeException("Invalid PMT");
        int sectionLength = w0 & 0xfff;

        data.limit(data.position() + sectionLength);

        int programNumber = data.getShort() & 0xffff;
        int b0 = data.get() & 0xff;
        int versionNumber = (b0 >> 1) & 0x1f;
        int currentNextIndicator = b0 & 1;

        int sectionNumber = data.get() & 0xff;
        int lastSectionNumber = data.get() & 0xff;
    }

    private static void parseEsInfo(ByteBuffer read) {

    }

    private static List<Tag> parseTags(ByteBuffer bb) {
        List<Tag> tags = new ArrayList<Tag>();
        while (bb.hasRemaining()) {
            int tag = bb.get();
            int tagLen = bb.get();
            System.out.println(String.format("TAG: [0x%x, 0x%x]", tag, tagLen));
            tags.add(new Tag(tag, NIOUtils.read(bb, tagLen)));
        }
        return tags;
    }

    public static List<Stream> getPrograms(File src) throws IOException {
        SeekableByteChannel ch = null;
        try {
            ch = NIOUtils.readableFileChannel(src);
            return getProgramGuids(ch);
        } finally {
            NIOUtils.closeQuietly(ch);
        }
    }

    public static List<Stream> getProgramGuids(SeekableByteChannel in) throws IOException {
        PMTExtractor ex = new PMTExtractor();
        ex.readTsFile(in);
        PMT pmt = ex.getPmt();
        return pmt.getStreams();
    }

    private static class PMTExtractor extends TSReader {
        private int pmtGuid = -1;
        private PMT pmt;

        @Override
        protected boolean onPkt(int guid, boolean payloadStart, ByteBuffer tsBuf, long filePos) {
            if (guid == 0) {
                pmtGuid = parsePAT(tsBuf);
            } else if (pmtGuid != -1 && guid == pmtGuid) {
                pmt = parsePMT(tsBuf);
                return false;
            }
            return true;
        }

        public PMT getPmt() {
            return pmt;
        }
    };

    public abstract static class TSReader {
        // Buffer must have an integral number of MPEG TS packets
        public static final int BUFFER_SIZE = 188 << 9;

        public void readTsFile(SeekableByteChannel ch) throws IOException {
            ch.position(0);
            ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);

            for (long pos = ch.position(); ch.read(buf) != -1; pos = ch.position()) {
                buf.flip();
                while (buf.hasRemaining()) {
                    ByteBuffer tsBuf = NIOUtils.read(buf, 188);
                    pos += 188;
                    Assert.assertEquals(0x47, tsBuf.get() & 0xff);
                    int guidFlags = ((tsBuf.get() & 0xff) << 8) | (tsBuf.get() & 0xff);
                    int guid = (int) guidFlags & 0x1fff;

                    int payloadStart = (guidFlags >> 14) & 0x1;
                    int b0 = tsBuf.get() & 0xff;
                    int counter = b0 & 0xf;
                    if ((b0 & 0x20) != 0) {
                        NIOUtils.skip(tsBuf, tsBuf.get() & 0xff);
                    }
                    if (payloadStart == 1) {
                        NIOUtils.skip(tsBuf, tsBuf.get() & 0xff);
                    }
                    if (!onPkt(guid, payloadStart == 1, tsBuf, pos - tsBuf.remaining()))
                        return;
                }
                buf.flip();
            }
        }

        protected abstract boolean onPkt(int guid, boolean payloadStart, ByteBuffer tsBuf, long filePos);
    }

    public static int getVideoPid(File src) throws IOException {
        List<Stream> streams = MTSUtils.getPrograms(src);
        for (Stream stream : streams) {
            if (stream.getStreamType().isVideo())
                return stream.getPid();
        }

        throw new RuntimeException("No video stream");
    }

    public static int getAudioPid(File src) throws IOException {
        List<Stream> streams = MTSUtils.getPrograms(src);
        for (Stream stream : streams) {
            if (stream.getStreamType().isVideo())
                return stream.getPid();
        }

        throw new RuntimeException("No video stream");
    }

    public static int[] getMediaPids(File src) throws IOException {
        IntArrayList result = new IntArrayList();
        List<Stream> streams = MTSUtils.getPrograms(src);
        for (Stream stream : streams) {
            if (stream.getStreamType().isVideo() || stream.getStreamType().isAudio())
                result.add(stream.getPid());
        }

        return result.toArray();
    }
}