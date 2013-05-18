package org.jcodec.samples.streaming;

import static org.jcodec.codecs.mpeg12.bitstream.PictureHeader.IntraCoded;
import static org.jcodec.codecs.s302.S302MUtils.labels;
import static org.jcodec.codecs.s302.S302MUtils.name;
import static org.jcodec.common.NIOUtils.readableFileChannel;
import static org.jcodec.containers.mps.MPSDemuxer.videoStream;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.codecs.mpeg12.MPEGDecoder;
import org.jcodec.codecs.s302.S302MDecoder;
import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.model.AudioBuffer;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mps.MPSDemuxer;
import org.jcodec.containers.mps.MPSDemuxer.Track;
import org.jcodec.player.filters.MediaInfo;
import org.jcodec.samples.streaming.MTSIndex.FrameEntry;
import org.jcodec.samples.streaming.MTSIndex.VideoFrameEntry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Streaming adaptor for MPEG program stream container
 * 
 * @author The JCodec project
 * 
 */
public class MPSAdapter implements Adapter {

    private MTSIndex index;
    private List<AdapterTrack> tracks = new ArrayList<AdapterTrack>();
    private MPSDemuxer demuxer;
    private FileChannelWrapper channel;

    public MPSAdapter(File mtsFile, MTSIndex index) throws IOException {
        channel = readableFileChannel(mtsFile);
        demuxer = new MPSDemuxer(channel);
        List<Track> tracks2 = demuxer.getTracks();
        for (Track track : tracks2) {
            if (videoStream(track.getSid()))
                tracks.add(new MPSVideoAdapterTrack(track));
            else
                tracks.add(new MPSAudioAdapterTrack(track));
        }

        this.index = index;
    }

    abstract class MPSAdapterTrack implements AdapterTrack {
        protected Track track;

        public MPSAdapterTrack(Track track) {
            this.track = track;
        }

        @Override
        public int search(long pts) throws IOException {
            FrameEntry e = index.search(track.getSid(), pts);
            return e == null ? -1 : e.frameNo;
        }
    }

    class MPSAudioAdapterTrack extends MPSAdapterTrack implements Adapter.AudioAdapterTrack {
        private int curFrame = -1;

        public MPSAudioAdapterTrack(Track track) {
            super(track);
        }

        public MediaInfo getMediaInfo() throws IOException {
            Packet frame = getFrame(0);
            AudioBuffer decoded = new S302MDecoder().decodeFrame(frame.getData(),
                    ByteBuffer.allocate(frame.getData().remaining()));
            int frames = index.getNumFrames(track.getSid());
            FrameEntry e = index.frame(track.getSid(), frames - 1);
            long duration = e.pts;
            return new MediaInfo.AudioInfo("s302", 90000, duration, frames, name(decoded.getFormat().getChannels()),
                    null, decoded.getFormat(), decoded.getNFrames(), labels(decoded.getFormat().getChannels()));
        }

        @Override
        public Packet getFrame(int frameId) throws IOException {
            FrameEntry e = index.frame(track.getSid(), frameId);
            if (e == null)
                return null;
            synchronized (demuxer) {
                if (curFrame != frameId) {
                    demuxer.seekByte(curFrame);
                    curFrame = frameId;
                }
                return frame(e);
            }
        }

        private Packet frame(FrameEntry e) throws IOException {
            Packet frame = track.getFrame(ByteBuffer.allocate(0x20000));
            return new Packet(frame.getData(), e.pts, 90000, e.duration, e.frameNo, true, null);
        }
    }

    class MPSVideoAdapterTrack extends MPSAdapterTrack implements Adapter.VideoAdapterTrack {
        private int curFrame = -1;

        public MPSVideoAdapterTrack(Track track) {
            super(track);
        }

        public MediaInfo getMediaInfo() throws IOException {
            Packet frame = getFrame(0);
            Size sz = MPEGDecoder.getSize(frame.getData());

            int frames = index.getNumFrames(track.getSid());
            FrameEntry e = index.frame(track.getSid(), frames - 1);
            long duration = e.pts;

            return new MediaInfo.VideoInfo("m2v1", 90000, duration, frames, "", null, new Rational(1, 1), sz);
        }

        @Override
        public Packet[] getGOP(int frameNo) throws IOException {
            FrameEntry e = index.frame(track.getSid(), frameNo);

            return e == null ? null : frames(gop((VideoFrameEntry) e));
        }

        @Override
        public int gopId(int frameNo) {
            VideoFrameEntry vfe = (VideoFrameEntry) index.frame(track.getSid(), frameNo);
            return vfe == null ? -1 : vfe.gopId;
        }

        private List<VideoFrameEntry> gop(VideoFrameEntry cur) throws IOException {
            List<VideoFrameEntry> result = new ArrayList<VideoFrameEntry>();

            int nextGop = Integer.MAX_VALUE;
            boolean ngAdded = false;
            for (int i = cur.gopId;; i++) {
                VideoFrameEntry fe = (VideoFrameEntry) index.frame(track.getSid(), i);
                if (fe == null)
                    break;
                if (fe.gopId == cur.gopId) {
                    if (i > nextGop && !ngAdded) {
                        result.add((VideoFrameEntry) index.frame(track.getSid(), nextGop));
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

        protected Packet frame(VideoFrameEntry e) throws IOException {
            Packet frame;

            synchronized (demuxer) {
                if (e.frameNo != curFrame) {
                    demuxer.seekByte(e.dataOffset);
                    curFrame = e.frameNo;
                }
                frame = track.getFrame(ByteBuffer.allocate(0x40000));
            }
            if (frame == null)
                return null;

            // packets.add(0, index.getExtraData(sid, e.edInd));
            // NIOUtils.combine(packets)
            Packet pkt = new Packet(frame.getData(), e.pts, 90000, e.duration, e.frameNo, e.frameType == IntraCoded,
                    e.getTapeTimecode());
            pkt.setDisplayOrder(e.displayOrder);

            return pkt;
        }

        private Packet getFrame(int frameId) throws IOException {
            VideoFrameEntry e = (VideoFrameEntry) index.frame(track.getSid(), frameId);
            return e == null ? null : frame(e);
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

    @Override
    public void close() throws IOException {
        channel.close();
    }
}