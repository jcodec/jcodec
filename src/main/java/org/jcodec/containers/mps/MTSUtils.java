package org.jcodec.containers.mps;
import static org.jcodec.common.Preconditions.checkState;
import static org.jcodec.common.io.NIOUtils.getRel;

import org.jcodec.common.IntArrayList;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mps.psi.PATSection;
import org.jcodec.containers.mps.psi.PMTSection;
import org.jcodec.containers.mps.psi.PMTSection.PMTStream;
import org.jcodec.containers.mps.psi.PSISection;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MTSUtils {
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
        PATSection pat = PATSection.parsePAT(data);
        if (pat.getPrograms().size() > 0)
            return pat.getPrograms().values()[0];
        else
            return -1;
    }

    @Deprecated
    public static PMTSection parsePMT(ByteBuffer data) {
        return PMTSection.parsePMT(data);
    }

    @Deprecated
    public static PSISection parseSection(ByteBuffer data) {
        return PSISection.parsePSI(data);
    }

    private static void parseEsInfo(ByteBuffer read) {

    }

    public static PMTStream[] getProgramGuids(File src) throws IOException {
        SeekableByteChannel ch = null;
        try {
            ch = NIOUtils.readableChannel(src);
            return getProgramGuidsFromChannel(ch);
        } finally {
            NIOUtils.closeQuietly(ch);
        }
    }

    public static PMTStream[] getProgramGuidsFromChannel(SeekableByteChannel _in) throws IOException {
        PMTExtractor ex = new PMTExtractor();
        ex.readTsFile(_in);
        PMTSection pmt = ex.getPmt();
        return pmt.getStreams();
    }

    private static class PMTExtractor extends TSReader {
        public PMTExtractor() {
            super(false);
        }

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

        public TSReader(boolean flush) {
            this.flush = flush;
        }

        public void readTsFile(SeekableByteChannel ch) throws IOException {
            ch.setPosition(0);
            ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);

            for (long pos = ch.position(); ch.read(buf) >= TS_PKT_SIZE; pos = ch.position()) {
                long posRem = pos;
                buf.flip();
                while (buf.remaining() >= TS_PKT_SIZE) {
                    ByteBuffer tsBuf = NIOUtils.read(buf, TS_PKT_SIZE);
                    ByteBuffer fullPkt = tsBuf.duplicate();
                    pos += TS_PKT_SIZE;
                    checkState(TS_SYNC_MARKER == (tsBuf.get() & 0xff));
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
                    ch.setPosition(posRem);
                    ch.write(buf);
                }
                buf.clear();
            }
        }

        protected boolean onPkt(int guid, boolean payloadStart, ByteBuffer tsBuf, long filePos, boolean sectionSyntax,
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

    public static int[] getMediaPidsFromChannel(SeekableByteChannel src) throws IOException {
        return filterMediaPids(MTSUtils.getProgramGuidsFromChannel(src));
    }

    public static int[] getMediaPids(File src) throws IOException {
        return filterMediaPids(MTSUtils.getProgramGuids(src));
    }

    private static int[] filterMediaPids(PMTStream[] programs) {
        IntArrayList result = IntArrayList.createIntArrayList();
        for (PMTStream stream : programs) {
            if (stream.getStreamType().isVideo() || stream.getStreamType().isAudio())
                result.add(stream.getPid());
        }

        return result.toArray();
    }
}