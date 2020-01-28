package org.jcodec.player.filters;

import static org.jcodec.common.io.NIOUtils.readableFileChannel;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.TrackType;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mp4.demuxer.MP4DemuxerTrackMeta;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class JCodecPacketSource {

    private MP4Demuxer demuxer;
    private List<Track> tracks;
    private SeekableByteChannel is;

    public JCodecPacketSource(File file) throws IOException {
        is = NIOUtils.readableChannel(file);
        demuxer = MP4Demuxer.createMP4Demuxer(is);

        tracks = new ArrayList<Track>();
        for (SeekableDemuxerTrack demuxerTrack : demuxer.getTracks()) {
            DemuxerTrackMeta meta = demuxerTrack.getMeta();
            if (meta.getType() == TrackType.VIDEO || meta.getType() == TrackType.AUDIO)
                tracks.add(new Track(demuxerTrack));
        }
    }

    public List<? extends PacketSource> getTracks() {
        return tracks;
    }

    public PacketSource getVideo() {
        for (Track track : tracks) {
            DemuxerTrackMeta meta = track.track.getMeta();
            if (meta.getType() == TrackType.VIDEO)
                return track;
        }
        return null;
    }

    public List<? extends PacketSource> getAudio() {
        List<Track> result = new ArrayList<Track>();
        for (Track track : tracks) {
            DemuxerTrackMeta meta = track.track.getMeta();
            if (meta.getType() == TrackType.AUDIO)
                result.add(track);
        }
        return result;
    }

    public class Track implements PacketSource {
        private SeekableDemuxerTrack track;
        private boolean closed;

        public Track(SeekableDemuxerTrack track) {
            this.track = track;
        }

        public Packet getPacket(ByteBuffer buffer) {
            try {
                return track.nextFrame();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public MediaInfo getMediaInfo() {
            MP4DemuxerTrackMeta meta = (MP4DemuxerTrackMeta) track.getMeta();
            RationalLarge duration = RationalLarge.R((long) (meta.getTotalDuration() * 1000), 1000);
            if (meta.getType() == TrackType.VIDEO) {
                VideoSampleEntry se = (VideoSampleEntry) meta.getSampleEntries()[0];
                return new MediaInfo.VideoInfo(se.getFourcc(), (int) duration.getDen(), duration.getNum(),
                        meta.getTotalFrames(), meta.getVideoCodecMeta().getFourcc(), null,
                        meta.getVideoCodecMeta().getPasp(), new Size((int) se.getWidth(), (int) se.getHeight()));
            } else if (meta.getType() == TrackType.AUDIO) {
                AudioSampleEntry se = (AudioSampleEntry) meta.getSampleEntries()[0];
                return new MediaInfo.AudioInfo(se.getFourcc(), (int) duration.getDen(), duration.getNum(),
                        meta.getTotalFrames(), meta.getAudioCodecMeta().getFourcc(), null, se.getFormat(),
                        se.getLabels());
            }
            throw new RuntimeException("This shouldn't happen");
        }

        public boolean drySeek(RationalLarge second) throws IOException {
            track.seek(second.scalar());
            return true;
        }

        public void seek(RationalLarge second) throws IOException {
            track.seek(second.scalar());
        }

        public void close() throws IOException {
            this.closed = true;
            checkClose();
        }

        @Override
        public void gotoFrame(int frameNo) throws IOException {
            track.gotoFrame(frameNo);
        }
    }

    private void checkClose() throws IOException {
        boolean closed = true;
        for (Track track : tracks) {
            closed &= track.closed;
        }
        if (closed)
            is.close();
    }
}