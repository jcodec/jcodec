package org.jcodec.codecs.mpeg12;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jcodec.codecs.mpeg12.MPSMediaInfo.MPEGTrackMetadata;
import org.jcodec.codecs.mpeg12.MPSMediaInfo.MediaInfoDone;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mps.MTSUtils;
import org.jcodec.containers.mps.psi.PMTSection;
import org.jcodec.containers.mps.psi.PMTSection.PMTStream;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Gets media info from MPEG TS file
 * 
 * @author The JCodec project
 * 
 */
public class MTSMediaInfo {

    public List<MPEGTrackMetadata> getMediaInfo(File f) throws IOException {
        FileChannelWrapper ch = null;
        final List<PMTSection> pmtSections = new ArrayList<PMTSection>();
        final Map<Integer, MPSMediaInfo> pids = new HashMap<Integer, MPSMediaInfo>();
        final List<MPEGTrackMetadata> result = new ArrayList<MPEGTrackMetadata>();
        try {
            ch = NIOUtils.readableChannel(f);
            new MTSUtils.TSReader(false) {
                private ByteBuffer pmtBuffer;
                private int pmtPid = -1;
                private boolean pmtDone;

                @Override
                protected boolean onPkt(int guid, boolean payloadStart, ByteBuffer tsBuf, long filePos, boolean sectionSyntax,
                        ByteBuffer fullPkt) {
                    if (guid == 0) {
                        pmtPid = MTSUtils.parsePAT(tsBuf);
                    } else if (guid == pmtPid && !pmtDone) {
                        if (pmtBuffer == null) {
                            pmtBuffer = ByteBuffer.allocate(((tsBuf.duplicate().getInt() >> 8) & 0x3ff) + 3);
                        } else if (pmtBuffer.hasRemaining()) {
                            NIOUtils.writeL(pmtBuffer, tsBuf, Math.min(pmtBuffer.remaining(), tsBuf.remaining()));
                        }

                        if (!pmtBuffer.hasRemaining()) {
                            pmtBuffer.flip();
                            PMTSection pmt = MTSUtils.parsePMT(pmtBuffer);
                            pmtSections.add(pmt);
                            for (PMTStream stream : pmt.getStreams()) {
                                if (!pids.containsKey(stream.getPid()))
                                    pids.put(stream.getPid(), new MPSMediaInfo());
                            }
                            pmtDone = pmt.getSectionNumber() == pmt.getLastSectionNumber();
                            pmtBuffer = null;
                        }
                    } else if (pids.containsKey(guid)) {
                        try {
                            pids.get(guid).analyseBuffer(tsBuf, filePos);
                        } catch (MediaInfoDone e) {
                            result.addAll(pids.get(guid).getInfos());
                            pids.remove(guid);
                            if (pids.size() == 0)
                                return false;
                        }
                    }
                    return true;
                }
            }.readTsFile(ch);
        } finally {
            NIOUtils.closeQuietly(ch);
        }

        return result;
    }

    public static void main1(String[] args) throws IOException {
        List<MPEGTrackMetadata> info = new MTSMediaInfo().getMediaInfo(new File(args[0]));
        for (MPEGTrackMetadata stream : info) {
            System.out.println(stream.codec);
        }
    }

    public static MTSMediaInfo extract(SeekableByteChannel input) {
        // TODO Auto-generated method stub
        return null;
    }

    public MPEGTrackMetadata getVideoTrack() {
        // TODO Auto-generated method stub
        return null;
    }

    public List<MPEGTrackMetadata> getAudioTracks() {
        // TODO Auto-generated method stub
        return null;
    }
}
