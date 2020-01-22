package org.jcodec.containers.mp4.demuxer;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jcodec.common.Demuxer;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.TrackType;
import org.jcodec.common.model.Packet;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Demuxer frontend for Dash MP4
 * 
 * @author The JCodec project
 * 
 */
public class DashMP4Demuxer implements Demuxer {
    private List<SeekableDemuxerTrack> tracks;

    // f08e80da-bf1d-4e3d-8899-f0f6155f6efa-f08e80da-bf1d-4e3d-8899-f0f6155f6efa.f1080_4800000.mp4.part-Frag0
    public static class Builder {
        List<TrackBuilder> tracks = new ArrayList<TrackBuilder>();

        public TrackBuilder addTrack() {
            TrackBuilder ret = new TrackBuilder(this);
            tracks.add(ret);
            return ret;
        }

        public DashMP4Demuxer build() throws IOException {
            List<List<File>> list = new LinkedList<List<File>>();
            for (TrackBuilder trackBuilder : tracks) {
                list.add(trackBuilder.list);
            }
            return new DashMP4Demuxer(list);
        }
    }

    public static class TrackBuilder {
        private Builder builder;
        private List<File> list = new LinkedList<File>();

        public TrackBuilder(Builder builder) {
            this.builder = builder;
        }

        public TrackBuilder addPattern(String pattern) {
            for (int i = 0;; i++) {
                String name = String.format(pattern, i);
                File file = new File(name);
                if (!file.exists())
                    break;
                list.add(file);
            }
            return this;
        }

        public TrackBuilder addFile(File file) {
            list.add(file);
            return this;
        }

        public Builder done() {
            return builder;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private DashMP4Demuxer(List<List<File>> files) throws IOException {
        tracks = new LinkedList<SeekableDemuxerTrack>();
        for (List<File> file : files) {
            tracks.add(new CodecMP4DemuxerTrack(DashMP4DemuxerTrack.createFromFiles(file)));
        }
    }

    public DemuxerTrack getVideoTrack() {
        for (SeekableDemuxerTrack demuxerTrack : tracks) {
            DemuxerTrackMeta meta = demuxerTrack.getMeta();
            if (meta.getType() == TrackType.VIDEO)
                return demuxerTrack;
        }
        return null;
    }

    @Override
    public List<DemuxerTrack> getTracks() {
        return new ArrayList<DemuxerTrack>(tracks);
    }

    @Override
    public List<DemuxerTrack> getVideoTracks() {
        ArrayList<DemuxerTrack> result = new ArrayList<DemuxerTrack>();
        for (SeekableDemuxerTrack demuxerTrack : tracks) {
            DemuxerTrackMeta meta = demuxerTrack.getMeta();
            if (meta.getType() == TrackType.VIDEO)
                result.add(demuxerTrack);
        }
        return result;
    }

    @Override
    public List<DemuxerTrack> getAudioTracks() {
        ArrayList<DemuxerTrack> result = new ArrayList<DemuxerTrack>();
        for (SeekableDemuxerTrack demuxerTrack : tracks) {
            DemuxerTrackMeta meta = demuxerTrack.getMeta();
            if (meta.getType() == TrackType.AUDIO)
                result.add(demuxerTrack);
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        for (SeekableDemuxerTrack track : tracks) {
            ((Closeable) ((CodecMP4DemuxerTrack) track).getOther()).close();
        }
    }

    public static void main(String[] args) throws IOException {
        DashMP4Demuxer demuxer = DashMP4Demuxer.builder().addTrack().addPattern(args[0]).done().addTrack()
                .addPattern(args[1]).done().build();
        DemuxerTrack videoTrack = demuxer.getVideoTrack();
        DemuxerTrackMeta meta = videoTrack.getMeta();
        System.out.println("Total frames: " + meta.getTotalFrames());
        Packet nextFrame;
        while ((nextFrame = videoTrack.nextFrame()) != null) {
            System.out.println(nextFrame.getPts());
        }
    }
}