package org.jcodec.containers.mp4.demuxer;

import static org.jcodec.containers.mp4.boxes.Box.findFirst;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.TrackType;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.HandlerBox;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.SampleSizesBox;
import org.jcodec.containers.mp4.boxes.TrakBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Demuxer frontend for MP4
 * 
 * @author The JCodec project
 * 
 */
public class MP4Demuxer {

    private List<AbstractMP4DemuxerTrack> tracks;
    private TimecodeMP4DemuxerTrack timecodeTrack;
    MovieBox movie;
    private SeekableByteChannel input;

    public AbstractMP4DemuxerTrack create(TrakBox trak) {
        SampleSizesBox stsz = findFirst(trak, SampleSizesBox.class, "mdia", "minf", "stbl", "stsz");
        if (stsz.getDefaultSize() == 0)
            return new FramesMP4DemuxerTrack(movie, trak, input);
        else
            return new PCMMP4DemuxerTrack(movie, trak, input);
    }

    public MP4Demuxer(SeekableByteChannel input) throws IOException {
        this.input = input;
        tracks = new LinkedList<AbstractMP4DemuxerTrack>();
        findMovieBox(input);
    }

    public AbstractMP4DemuxerTrack[] getTracks() {
        return tracks.toArray(new AbstractMP4DemuxerTrack[] {});
    }

    private void findMovieBox(SeekableByteChannel input) throws IOException {
        movie = MP4Util.parseMovie(input);
        if (movie == null)
            throw new IOException("Could not find movie meta information box");

        processHeader(movie);
    }

    private void processHeader(NodeBox moov) throws IOException {
        TrakBox tt = null;
        for (TrakBox trak : Box.findAll(moov, TrakBox.class, "trak")) {
            SampleEntry se = Box.findFirst(trak, SampleEntry.class, "mdia", "minf", "stbl", "stsd", null);
            if ("tmcd".equals(se.getFourcc())) {
                tt = trak;
            } else {
                tracks.add(create(trak));
            }
        }
        if (tt != null) {
            AbstractMP4DemuxerTrack video = getVideoTrack();
            if (video != null)
                timecodeTrack = new TimecodeMP4DemuxerTrack(movie, tt, input);
        }
    }

    public static TrackType getTrackType(TrakBox trak) {
        HandlerBox handler = findFirst(trak, HandlerBox.class, "mdia", "hdlr");
        return TrackType.fromHandler(handler.getComponentSubType());
    }

    public AbstractMP4DemuxerTrack getVideoTrack() {
        for (AbstractMP4DemuxerTrack demuxerTrack : tracks) {
            if (demuxerTrack.box.isVideo())
                return demuxerTrack;
        }
        return null;
    }

    public MovieBox getMovie() {
        return movie;
    }

    public AbstractMP4DemuxerTrack getTrack(int no) {
        for (AbstractMP4DemuxerTrack track : tracks) {
            if (track.getNo() == no)
                return track;
        }
        return null;
    }

    public List<AbstractMP4DemuxerTrack> getAudioTracks() {
        ArrayList<AbstractMP4DemuxerTrack> result = new ArrayList<AbstractMP4DemuxerTrack>();
        for (AbstractMP4DemuxerTrack demuxerTrack : tracks) {
            if (demuxerTrack.box.isAudio())
                result.add(demuxerTrack);
        }
        return result;
    }

    public TimecodeMP4DemuxerTrack getTimecodeTrack() {
        return timecodeTrack;
    }

    private static int ftyp = ('f' << 24) | ('t' << 16) | ('y' << 8) | 'p';
    private static int free = ('f' << 24) | ('r' << 16) | ('e' << 8) | 'e';
    private static int moov = ('m' << 24) | ('o' << 16) | ('o' << 8) | 'v';
    private static int mdat = ('m' << 24) | ('d' << 16) | ('a' << 8) | 't';

    public static int probe(final ByteBuffer b) {
        ByteBuffer fork = b.duplicate();
        int success = 0;
        int total = 0;
        while (fork.remaining() >= 8) {
            long len = fork.getInt();
            int fcc = fork.getInt();
            int hdrLen = 8;
            if (len == 1) {
                len = fork.getLong();
                hdrLen = 16;
            } else if (len < 8)
                break;
            if (fcc == ftyp && len < 64 || fcc == moov && len < 100 * 1024 * 1024 || fcc == free || fcc == mdat)
                success++;
            total++;
            if (len >= Integer.MAX_VALUE)
                break;
            NIOUtils.skip(fork, (int) (len - hdrLen));
        }

        return success * 100 / total;
    }
}