package org.jcodec.samples.streaming;

import static ch.lambdaj.Lambda.extract;
import static ch.lambdaj.Lambda.on;
import static org.jcodec.containers.mps.MPSDemuxer.videoStream;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.jcodec.codecs.mpeg12.bitstream.GOPHeader;
import org.jcodec.codecs.mpeg12.bitstream.PictureHeader;
import org.jcodec.common.io.Buffer;
import org.jcodec.common.io.RandomAccessFileInputStream;
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
        RandomAccessFileInputStream is = null;
        try {
            is = new RandomAccessFileInputStream(mtsFile);

            TIntObjectHashMap<PESProgram> programs = new TIntObjectHashMap<PESProgram>();
            while (true) {
                long offset = is.getPos();
                if (is.length() - offset < 188)
                    break;
                MTSPacket pkt = MTSDemuxer.readPacket(is);
                Buffer data = pkt.payload;

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
            IOUtils.closeQuietly(is);
            done = true;
        }
    }

    private class PESProgram {
        private MTSIndex index;
        private PESPacket pes = null;
        private Buffer buffer = null, seqHeader = null, gopHeader = null, pictureHeader = null;
        private int marker = 0xffffffff, curMarker = marker;
        private boolean skipPES = false;
        private long pesOffset = 0;

        private List<VideoFrameEntry> gop;
        private GOPHeader prevGop;
        private FrameEntry lastVideoFrame;

        public PESProgram(MTSIndex index) {
            this.index = index;
        }

        public void packet(MTSPacket pkt, long offset) throws IOException {
            Buffer data = pkt.payload;

            if (pkt.payloadStart && markerStart(data)) {
                int streamId = data.get(3);
                data.read(4);
                pes = MPSDemuxer.readPES(streamId, data.is());

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
                for (int i = data.pos; i < data.limit; i++) {
                    marker = (marker << 8) | (data.buffer[i] & 0xff);

                    if (buffer != null)
                        buffer.write(data.buffer[i]);

                    if (marker < 0x100 || marker >= 0x1b9 || marker == 0x1b5 || marker == 0x1b2)
                        continue;

                    if (curMarker == 0x1b3) {
                        seqHeader = new Buffer(buffer.buffer, 0, buffer.pos - 4);
                        buffer = null;
                    } else if (curMarker == 0x1b8) {
                        gopHeader = new Buffer(buffer.buffer, 0, buffer.pos - 4);
                        buffer = null;
                    } else if (curMarker == 0x100) {
                        pictureHeader = new Buffer(buffer.buffer, 0, buffer.pos - 4);
                        buffer = null;

                        videoFrame(index, pesOffset, pes, seqHeader, gopHeader, pictureHeader);
                    }

                    if (marker == 0x1b3 || marker == 0x1b8 || marker == 0x100) {
                        buffer = new Buffer(1024);
                        buffer.dout().writeInt(marker);
                    }

                    curMarker = marker;
                }
            }
        }

        private void videoFrame(MTSIndex index, long pesOffset, PESPacket pes, Buffer seqHeader, Buffer gopHeader,
                Buffer pictureHeader) throws IOException {

            PictureHeader ph = PictureHeader.read(pictureHeader.from(4));

            GOPHeader curGop = gopHeader == null ? null : GOPHeader.read(gopHeader.from(4));

            if (ph.picture_coding_type == PictureHeader.IntraCoded) {
                processGOP(gop, curGop, prevGop);
                prevGop = curGop;
                gop = new ArrayList<VideoFrameEntry>();
            }

            if (gop != null) {
                VideoFrameEntry entry = index.addVideo(pes.streamId, pesOffset, pes.pts, 0, seqHeader, 0, 0,
                        (short) ph.temporal_reference, (byte) ph.picture_coding_type);
                gop.add(entry);
                entry.gopId = gop.get(0).frameNo;
            }
        }

        private void processGOP(List<VideoFrameEntry> gop, GOPHeader nextGop, GOPHeader prevGop) {
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

    private static final boolean markerStart(Buffer buf) {
        return buf.get(0) == 0 && buf.get(1) == 0 && buf.get(2) == 1;
    }

    public MTSIndex getIndex() {
        return index;
    }
    
    public boolean isDone() {
        return done;
    }
}