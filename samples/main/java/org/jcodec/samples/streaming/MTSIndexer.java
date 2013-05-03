package org.jcodec.samples.streaming;

import static ch.lambdaj.Lambda.extract;
import static ch.lambdaj.Lambda.on;
import static org.jcodec.codecs.mpeg12.bitstream.PictureHeader.BiPredictiveCoded;
import static org.jcodec.codecs.mpeg12.bitstream.PictureHeader.IntraCoded;
import static org.jcodec.common.NIOUtils.from;
import static org.jcodec.containers.mps.MPSDemuxer.videoStream;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jcodec.codecs.mpeg12.bitstream.GOPHeader;
import org.jcodec.codecs.mpeg12.bitstream.PictureHeader;
import org.jcodec.common.model.TapeTimecode;
import org.jcodec.containers.mps.MPSDemuxer;
import org.jcodec.containers.mps.MPSDemuxer.PESPacket;
import org.jcodec.containers.mps.MTSDemuxer;
import org.jcodec.containers.mps.MTSDemuxer.MTSPacket;
import org.jcodec.samples.streaming.MTSIndex.FrameEntry;
import org.jcodec.samples.streaming.MTSIndex.VideoFrameEntry;

import ch.lambdaj.Lambda;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Creates an index of an MPEG TS file
 * 
 * @author The JCodec project
 * 
 */
public class MTSIndexer {

    private File mtsFile;
    private MTSIndex index;
    private volatile boolean done;

    public MTSIndexer(File mtsFile, MTSIndex index) {
        this.mtsFile = mtsFile;
        this.index = index;
    }

    public void index() throws IOException {
        FileChannel channel = null;
        try {
            channel = new FileInputStream(mtsFile).getChannel();

            TIntObjectHashMap<PESProgram> programs = new TIntObjectHashMap<PESProgram>();
            while (true) {
                long offset = channel.position();
                if (channel.size() - offset < 188)
                    break;
                MTSPacket pkt = MTSDemuxer.readPacket(channel);
                ByteBuffer data = pkt.payload;

                if (data == null)
                    continue;

                PESProgram program = programs.get(pkt.pid);
                if (program == null && pkt.payloadStart && markerStart(data)) {
                    program = new PESProgram(index);
                    programs.put(pkt.pid, program);
                }
                if (program != null)
                    program.packet(pkt, offset);
            }
        } finally {
            channel.close();
            done = true;
        }
    }

    private class PESProgram {
        private MTSIndex index;
        private PESPacket pes = null;
        private ByteBuffer buffer = null, seqHeader = null, gopHeader = null, pictureHeader = null;
        private int marker = 0xffffffff, curMarker = marker;
        private boolean skipPES = false;
        private long pesOffset = 0;

        private List<VideoFrameEntry> gop;
        private List<VideoFrameEntry> prevGop;
        private GOPHeader prevGopHeader;
        private FrameEntry lastVideoFrame;

        public PESProgram(MTSIndex index) {
            this.index = index;
        }

        public void packet(MTSPacket pkt, long offset) throws IOException {
            ByteBuffer data = pkt.payload;

            if (pkt.payloadStart && markerStart(data)) {
                int streamId = data.get(3);
                pes = MPSDemuxer.readPES(data, 0);

                if (!videoStream(streamId)) {
                    FrameEntry last = index.last(pes.streamId);
                    if (last != null)
                        last.duration = (int) (pes.pts - last.pts);
                    index.addAudio(pes.streamId, offset, pes.pts, 0);
                } else {
                    pesOffset = offset;
                }
                skipPES = !videoStream(streamId);
            }

            if (!skipPES) {
                while (data.hasRemaining()) {
                    byte b = data.get();
                    marker = (marker << 8) | (b & 0xff);

                    if (buffer != null)
                        buffer.put(b);

                    if (marker < 0x100 || marker >= 0x1b9 || marker == 0x1b5 || marker == 0x1b2)
                        continue;

                    if (curMarker == 0x1b3) {
                        seqHeader = buffer;
                        buffer.flip().limit(buffer.limit() - 4);
                        buffer = null;
                    } else if (curMarker == 0x1b8) {
                        gopHeader = buffer;
                        buffer.flip().limit(buffer.limit() - 4);
                        buffer = null;
                    } else if (curMarker == 0x100) {
                        pictureHeader = buffer;
                        buffer.flip().limit(buffer.limit() - 4);
                        buffer = null;

                        videoFrame(index, pesOffset, pes, seqHeader, gopHeader, pictureHeader);
                    }

                    if (marker == 0x1b3 || marker == 0x1b8 || marker == 0x100) {
                        buffer = ByteBuffer.allocate(1024);
                        buffer.putInt(marker);
                    }

                    curMarker = marker;
                }
            }
        }

        private void videoFrame(MTSIndex index, long pesOffset, PESPacket pes, ByteBuffer seqHeaderBuf,
                ByteBuffer gopHeaderBuf, ByteBuffer pictureHeader) throws IOException {

            PictureHeader ph = PictureHeader.read(from(pictureHeader, 4));

            GOPHeader gopHeader = gopHeaderBuf == null ? null : GOPHeader.read(from(gopHeaderBuf, 4));

            if (ph.picture_coding_type == IntraCoded) {
                assignTimecodes(gop, gopHeader, prevGopHeader);
                prevGopHeader = gopHeader;
                prevGop = gop;
                gop = new ArrayList<VideoFrameEntry>();
            }

            if (gop != null) {
                if (ph.picture_coding_type == BiPredictiveCoded && gop.size() == 1 && prevGop != null) {
                    prevGop.add(index.addVideo(pes.streamId, pesOffset, pes.pts, 0, seqHeaderBuf,
                            prevGop.get(0).frameNo, 0, (short) ph.temporal_reference, (byte) ph.picture_coding_type));
                } else {
                    gop.add(index.addVideo(pes.streamId, pesOffset, pes.pts, 0, seqHeaderBuf,
                            gop.size() > 0 ? gop.get(0).frameNo : -1, 0, (short) ph.temporal_reference,
                            (byte) ph.picture_coding_type));
                }
            }
        }

        private void assignTimecodes(List<VideoFrameEntry> gop, GOPHeader nextGop, GOPHeader prevGop) {
            if (gop == null)
                return;

            for (VideoFrameEntry frameEntry : Lambda.<VideoFrameEntry> sort(gop, on(VideoFrameEntry.class)
                    .getDisplayOrder())) {
                if (lastVideoFrame != null)
                    lastVideoFrame.duration = (int) (frameEntry.pts - lastVideoFrame.pts);
                lastVideoFrame = frameEntry;
            }
            if (nextGop == null || prevGop == null)
                return;
            TapeTimecode tt2 = nextGop.getTimeCode();
            TapeTimecode tt1 = prevGop.getTimeCode();
            int secDiff = (tt2.getHour() - tt1.getHour()) * 3600 + (tt2.getMinute() - tt1.getMinute()) * 60
                    + (tt2.getSecond() - tt1.getSecond());

            if (secDiff > 0) {
                Set<Integer> unique = new HashSet<Integer>(extract(gop, on(VideoFrameEntry.class).getDisplayOrder()));
                int frameDiff = unique.size() - (tt2.getFrame() - tt1.getFrame());
                int fps = frameDiff / secDiff;
                int baseCounter = tt1.getHour() * 3600 * fps + tt1.getMinute() * 60 * fps + tt1.getSecond() * fps
                        + tt1.getFrame();
                for (VideoFrameEntry packet : gop) {
                    int counter = baseCounter + packet.getDisplayOrder();
                    packet.setTapeTimecode(counter / (3600 * fps), (counter / (60 * fps)) % 60, (counter / fps) % 60,
                            counter % fps, tt1.isDropFrame());

                }
            } else {
                for (VideoFrameEntry packet : gop) {
                    packet.setTapeTimecode(tt1.getHour(), tt1.getMinute(), tt1.getSecond(),
                            tt1.getFrame() + packet.getDisplayOrder(), tt1.isDropFrame());
                }
            }
        }
    }

    private static final boolean markerStart(ByteBuffer buf) {
        return buf.get(0) == 0 && buf.get(1) == 0 && buf.get(2) == 1;
    }

    public MTSIndex getIndex() {
        return index;
    }

    public boolean isDone() {
        return done;
    }
}