package org.jcodec.samples.streaming;

import static org.jcodec.codecs.mpeg12.bitstream.PictureHeader.IntraCoded;
import static org.jcodec.containers.mps.MPSDemuxer.mediaStream;
import static org.jcodec.containers.mps.MPSDemuxer.videoStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jcodec.codecs.mpeg12.MPEGDecoder;
import org.jcodec.codecs.s302.S302MDecoder;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.model.AudioBuffer;
import org.jcodec.common.model.ChannelLabel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mps.MPSDemuxer;
import org.jcodec.containers.mps.MPSDemuxer.PESPacket;
import org.jcodec.containers.mps.MTSDemuxer;
import org.jcodec.containers.mps.MTSDemuxer.MTSPacket;
import org.jcodec.player.filters.MediaInfo;
import org.jcodec.samples.streaming.MTSIndex.FrameEntry;
import org.jcodec.samples.streaming.MTSIndex.VideoFrameEntry;
import org.junit.Assert;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Streaming adaptor for MPEG transport stream container
 * 
 * @author The JCodec project
 * 
 */
public class MTSAdapter implements Adapter {

    private MTSIndex index;
    private List<AdapterTrack> tracks;
    private File mtsFile;

    public MTSAdapter(File mtsFile, MTSIndex index) throws IOException {
        this.mtsFile = mtsFile;

        this.index = index;

        tracks = new ArrayList<AdapterTrack>();
        for (Integer sid : index.getStreamIds()) {
            if (videoStream(sid))
                tracks.add(new VideoAdapterTrack(sid));
            else if (MPSDemuxer.audioStream(sid))
                tracks.add(new AudioAdapterTrack(sid));
        }
    }

    @Override
    public AdapterTrack getTrack(int trackNo) {
        return tracks.get(trackNo);
    }

    @Override
    public List<AdapterTrack> getTracks() {
        return tracks;
    }

    private static final boolean markerStart(ByteBuffer buf) {
        return buf.get(0) == 0 && buf.get(1) == 0 && buf.get(2) == 1;
    }

    public class AudioAdapterTrack implements Adapter.AudioAdapterTrack {
        protected int sid;
        protected FileChannel channel;

        public AudioAdapterTrack(int sid) throws IOException {
            this.sid = sid;
            channel = new FileInputStream(mtsFile).getChannel();
            channel.position(index.frame(sid, 0).dataOffset);
        }

        protected synchronized Packet frame(FrameEntry e) throws IOException {
            channel.position(e.dataOffset);

            MTSPacket ts = MTSDemuxer.readPacket(channel);
            if (ts == null)
                return null;
            int guid = ts.pid;
            Assert.assertEquals(sid, ts.payload.get(3));

            List<ByteBuffer> packets = new LinkedList<ByteBuffer>();
            PESPacket pes = MPSDemuxer.readPES(ts.payload, 0);
            int remaining = pes.length <= 0 ? Integer.MAX_VALUE : pes.length;

            while (remaining > 0) {

                if (ts.pid == guid && ts.payload != null) {
                    ByteBuffer part = NIOUtils.read(ts.payload, Math.min(remaining, ts.payload.remaining()));
                    packets.add(part);
                    remaining -= part.remaining();
                }

                ts = MTSDemuxer.readPacket(channel);

                if (ts == null)
                    return null;

                if (ts.pid == guid && ts.payloadStart)
                    break;
            }

            ByteBuffer data = NIOUtils.combine(packets);

            return new Packet(data, e.pts, 90000, e.duration, e.frameNo, true, null);
        }

        @Override
        public MediaInfo getMediaInfo() throws IOException {
            Packet frame = getFrame(0);
            AudioBuffer decoded = new S302MDecoder().decodeFrame(frame.getData(),
                    ByteBuffer.allocate(frame.getData().remaining()));
            int frames = index.getNumFrames(sid);
            FrameEntry e = index.frame(sid, frames - 1);
            long duration = e.pts;
            return new MediaInfo.AudioInfo("s302", 90000, duration, frames, name(decoded.getFormat().getChannels()),
                    null, decoded.getFormat(), decoded.getNFrames(), labels(decoded.getFormat().getChannels()));
        }

        private String name(int channels) {
            switch (channels) {
            case 1:
                return "Mono";
            case 2:
                return "Stereo 2.0";
            case 4:
                return "Surround 4.0";
            case 8:
                return "Stereo 2.0 + Surround 5.1";
            }
            return null;
        }

        private ChannelLabel[] labels(int channels) {
            switch (channels) {
            case 1:
                return new ChannelLabel[] { ChannelLabel.MONO };
            case 2:
                return new ChannelLabel[] { ChannelLabel.STEREO_LEFT, ChannelLabel.STEREO_RIGHT };
            case 4:
                return new ChannelLabel[] { ChannelLabel.FRONT_LEFT, ChannelLabel.FRONT_RIGHT, ChannelLabel.REAR_LEFT,
                        ChannelLabel.REAR_RIGHT };
            case 8:
                return new ChannelLabel[] { ChannelLabel.STEREO_LEFT, ChannelLabel.STEREO_RIGHT,
                        ChannelLabel.FRONT_LEFT, ChannelLabel.FRONT_RIGHT, ChannelLabel.REAR_LEFT,
                        ChannelLabel.REAR_RIGHT, ChannelLabel.CENTER, ChannelLabel.LFE };
            }
            return null;
        }

        @Override
        public int search(long pts) throws IOException {
            FrameEntry e = index.search(sid, pts);
            return e == null ? -1 : e.frameNo;
        }

        @Override
        public Packet getFrame(int frameId) throws IOException {
            FrameEntry e = index.frame(sid, frameId);
            return e == null ? null : frame(e);
        }

        void close() throws IOException {
            channel.close();
        }
    }

    public class VideoAdapterTrack implements Adapter.VideoAdapterTrack {
        protected int sid;
        protected FileChannel channel;

        public VideoAdapterTrack(int sid) throws IOException {
            this.sid = sid;
            channel = new FileInputStream(mtsFile).getChannel();
            channel.position(index.frame(sid, 0).dataOffset);
        }

        @Override
        public int search(long pts) throws IOException {
            FrameEntry e = index.search(sid, pts);

            return e == null ? -1 : e.frameNo;
        }

        @Override
        public Packet[] getGOP(int frameNo) throws IOException {
            FrameEntry e = index.frame(sid, frameNo);

            return e == null ? null : frames(gop((VideoFrameEntry) e));
        }

        @Override
        public int gopId(int frameNo) {
            VideoFrameEntry vfe = (VideoFrameEntry) index.frame(sid, frameNo);
            return vfe == null ? -1 : vfe.gopId;
        }

        private List<VideoFrameEntry> gop(VideoFrameEntry cur) throws IOException {
            List<VideoFrameEntry> result = new ArrayList<VideoFrameEntry>();

            int nextGop = Integer.MAX_VALUE;
            boolean ngAdded = false;
            for (int i = cur.gopId;; i++) {
                VideoFrameEntry fe = (VideoFrameEntry) index.frame(sid, i);
                if (fe == null)
                    break;
                if (fe.gopId == cur.gopId) {
                    if (i > nextGop && !ngAdded) {
                        result.add((VideoFrameEntry) index.frame(sid, nextGop));
                        ngAdded = true;
                    }
                    result.add(fe);
                }
                if (fe.gopId > nextGop)
                    break;
                if (fe.gopId > cur.gopId && nextGop != fe.gopId) {
                    nextGop = fe.gopId;
                }
            }

            return result;
        }

        private Packet[] frames(List<VideoFrameEntry> gop) throws IOException {
            Packet[] result = new Packet[gop.size()];
            for (int i = 0; i < gop.size(); i++) {
                result[i] = frame(gop.get(i));
            }
            return result;
        }

        protected synchronized Packet frame(VideoFrameEntry e) throws IOException {
            channel.position(e.dataOffset);

            MTSPacket ts = MTSDemuxer.readPacket(channel);
            if (ts == null)
                return null;
            int guid = ts.pid;
            Assert.assertEquals(sid, ts.payload.get(3));

            List<ByteBuffer> packets = null;
            PESPacket pes = null;
            boolean skip = false;
            int marker = 0xffffffff;
            while (ts != null) {
                int streamId = ts.payload.get(3);
                if (ts.payloadStart || (pes.length <= 0 && markerStart(ts.payload) && mediaStream(streamId))) {
                    skip = streamId != sid;
                    pes = MPSDemuxer.readPES(ts.payload, 0);
                }

                ByteBuffer data = ts.payload;
                ByteBuffer leading = data.duplicate();
                while (data.hasRemaining()) {
                    marker = (marker << 8) | (data.get() & 0xff);

                    if (marker < 0x100 || marker > 0x1b9)
                        continue;

                    if (packets == null && marker == 0x100) {
                        packets = new ArrayList<ByteBuffer>();
                    } else if (packets != null
                            && (marker == 0x100 || (marker >= 0x1b0 && marker != 0x1b2 && marker != 0x1b5))) {
                        leading.limit(data.position() - 3);
                        packets.add(leading);
                        packets.add(0, index.getExtraData(sid, e.edInd));
                        Packet pkt = new Packet(NIOUtils.combine(packets), e.pts, 90000, e.duration, e.frameNo,
                                e.frameType == IntraCoded, e.getTapeTimecode());
                        pkt.setDisplayOrder(e.displayOrder);
                        return pkt;
                    }
                }

                if (!skip && packets != null)
                    packets.add(data);

                do {
                    ts = MTSDemuxer.readPacket(channel);
                } while (ts != null && (ts.pid != guid || ts.payload == null));
            }

            return null;
        }

        private Packet getFrame(int frameId) throws IOException {
            VideoFrameEntry e = (VideoFrameEntry) index.frame(sid, frameId);
            return e == null ? null : frame(e);
        }

        @Override
        public MediaInfo getMediaInfo() throws IOException {
            Packet frame = getFrame(0);
            Size sz = MPEGDecoder.getSize(frame.getData());

            int frames = index.getNumFrames(sid);
            FrameEntry e = index.frame(sid, frames - 1);
            long duration = e.pts;

            return new MediaInfo.VideoInfo("m2v1", 90000, duration, frames, "", null, new Rational(1, 1), sz);
        }

        void close() throws IOException {
            channel.close();
        }
    }

    @Override
    public void close() {
        List<AdapterTrack> tracks2 = tracks;
        for (AdapterTrack adapterTrack : tracks2) {
            try {
                if (adapterTrack instanceof VideoAdapterTrack)
                    ((VideoAdapterTrack) adapterTrack).close();
                else
                    ((AudioAdapterTrack) adapterTrack).close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}