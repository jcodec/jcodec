package org.jcodec.samples.streaming;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.TrackType;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.PixelAspectExt;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mp4.demuxer.MP4DemuxerTrackMeta;
import org.jcodec.containers.mp4.demuxer.PCMMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.TimecodeMP4DemuxerTrack;
import org.jcodec.player.filters.MediaInfo;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Streaming adaptor for Quicktime movies
 * 
 * @author The JCodec project
 * 
 */
public class QTAdapter implements Adapter {
    private MP4Demuxer demuxer;
    private ArrayList<AdapterTrack> tracks;
    private SeekableByteChannel is;

    public QTAdapter(File file) throws IOException {
        is = NIOUtils.readableChannel(file);

        demuxer = MP4Demuxer.createMP4Demuxer(is);
        tracks = new ArrayList<AdapterTrack>();
        for (SeekableDemuxerTrack demuxerTrack : demuxer.getTracks()) {
            DemuxerTrackMeta meta = demuxerTrack.getMeta();
            if (meta.getType() == TrackType.AUDIO) {
                tracks.add(new QTAudioAdaptorTrack(demuxerTrack));
            } else if (meta.getType() == TrackType.VIDEO) {
                tracks.add(new QTVideoAdaptorTrack(demuxerTrack, demuxer.getTimecodeTrack()));
            }
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

    public static class QTVideoAdaptorTrack implements VideoAdapterTrack {
        protected SeekableDemuxerTrack demuxerTrack;
        private TimecodeMP4DemuxerTrack timecodeTrack;

        public QTVideoAdaptorTrack(SeekableDemuxerTrack demuxerTrack, TimecodeMP4DemuxerTrack timecodeTrack) {
            this.demuxerTrack = demuxerTrack;
            this.timecodeTrack = timecodeTrack;
        }

        public synchronized int search(double sec) throws IOException {
            demuxerTrack.seek(sec);
            return (int) demuxerTrack.getCurFrame();
        }

        @Override
        public MediaInfo getMediaInfo() {
            MP4DemuxerTrackMeta meta = (MP4DemuxerTrackMeta) demuxerTrack.getMeta();
            VideoSampleEntry vse = (VideoSampleEntry) meta.getSampleEntries()[0];
            PixelAspectExt pasp = NodeBox.findFirst(vse, PixelAspectExt.class, "pasp");

            Rational r = new Rational(1, 1);
            if (pasp != null) {
                r = pasp.getRational();
            }
            return new MediaInfo.VideoInfo(vse.getFourcc(), 1000, (long) (meta.getTotalDuration() * 1000),
                    meta.getTotalFrames(), meta.getVideoCodecMeta().getFourcc(), null, r,
                    new Size(vse.getWidth(), vse.getHeight()));

        }

        @Override
        public Packet[] getGOP(int gopId) throws IOException {
            if (!demuxerTrack.gotoFrame(gopId))
                return null;
            MP4Packet frames = (MP4Packet) demuxerTrack.nextFrame();

            return frames == null ? null
                    : new Packet[] { timecodeTrack == null ? frames : timecodeTrack.getTimecode(frames) };
        }

        @Override
        public int gopId(int frameNo) {
            return frameNo;
        }
    }

    public static class QTAudioAdaptorTrack implements AudioAdapterTrack {
        protected SeekableDemuxerTrack demuxerTrack;

        public QTAudioAdaptorTrack(SeekableDemuxerTrack demuxerTrack) {
            this.demuxerTrack = demuxerTrack;
        }

        private Packet getFrameNonPCM(int frameNo) throws IOException {
            if (demuxerTrack.getCurFrame() != frameNo)
                demuxerTrack.gotoFrame(frameNo);
            return demuxerTrack.nextFrame();
        }

        private int searchFramePCM(double sec) throws IOException {
            demuxerTrack.seek(sec);
            return (int) (demuxerTrack.getCurFrame() >> 11);
        }

        private Packet getFramePCM(int frameNo) throws IOException {
            frameNo <<= 11;
            if (demuxerTrack.getCurFrame() != frameNo)
                demuxerTrack.gotoFrame(frameNo);
            Packet packet = demuxerTrack.nextFrame();
            if (packet == null)
                return null;
            return new Packet(packet.getData(), packet.getPts(), packet.getTimescale(), packet.getDuration(),
                    packet.getFrameNo() >> 11, packet.isKeyFrame() ? Packet.FrameType.KEY : Packet.FrameType.UNKNOWN,
                    packet.getTapeTimecode(), 0);
        }

        @Override
        public MediaInfo getMediaInfo() {
            MP4DemuxerTrackMeta meta = (MP4DemuxerTrackMeta) demuxerTrack.getMeta();

            AudioSampleEntry se = (AudioSampleEntry) meta.getSampleEntries()[0];
            boolean pcm = demuxerTrack instanceof PCMMP4DemuxerTrack;
            return new MediaInfo.AudioInfo(se.getFourcc(), 48000, (long) (meta.getTotalDuration() * 48000),
                    (meta.getTotalFrames() >> (pcm ? 11 : 0)), meta.getAudioCodecMeta().getFourcc(), null,
                    se.getFormat(), se.getLabels());
        }

        private boolean isPCM(SeekableDemuxerTrack track) {
            SampleEntry se = ((MP4DemuxerTrackMeta)track.getMeta()).getSampleEntries()[0];
            return (se instanceof AudioSampleEntry) && ((AudioSampleEntry) se).isPCM();
        }

        private boolean isPCM(TrakBox track) {
            SampleEntry se = track.getSampleEntries()[0];
            return (se instanceof AudioSampleEntry) && ((AudioSampleEntry) se).isPCM();
        }

        public synchronized int search(double sec) throws IOException {
            if (isPCM(demuxerTrack)) {
                return searchFramePCM(sec);
            } else {
                demuxerTrack.seek(sec);
                return (int) demuxerTrack.getCurFrame();
            }
        }

        @Override
        public synchronized Packet getFrame(int frameId) throws IOException {
            if (isPCM(demuxerTrack)) {
                return getFramePCM(frameId);
            } else {
                return getFrameNonPCM(frameId);
            }
        }
    }

    @Override
    public void close() {
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}