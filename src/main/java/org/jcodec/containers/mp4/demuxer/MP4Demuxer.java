package org.jcodec.containers.mp4.demuxer;

import static org.jcodec.common.Fourcc.free;
import static org.jcodec.common.Fourcc.ftyp;
import static org.jcodec.common.Fourcc.mdat;
import static org.jcodec.common.Fourcc.moov;
import static org.jcodec.common.Fourcc.wide;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jcodec.common.Demuxer;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.UsedViaReflection;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mp4.MP4TrackType;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Movie;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.HandlerBox;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.SampleSizesBox;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.platform.Platform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Demuxer frontend for MP4
 * 
 * @author The JCodec project
 * 
 */
public class MP4Demuxer implements Demuxer {

    private List<AbstractMP4DemuxerTrack> tracks;
    private TimecodeMP4DemuxerTrack timecodeTrack;
    MovieBox movie;
    protected SeekableByteChannel input;
    
    //modifies h264 to conform to annexb and aac to contain adts header
    public static MP4Demuxer createMP4Demuxer(SeekableByteChannel input) throws IOException {
        return new MP4Demuxer(input);
    }

    //does not modify packets
    public static MP4Demuxer createRawMP4Demuxer(SeekableByteChannel input) throws IOException {
        return new MP4Demuxer(input) {
            @Override
            protected AbstractMP4DemuxerTrack newTrack(TrakBox trak) {
                return new MP4DemuxerTrack(movie, trak, this.input);
            }
        };
    }

    private AbstractMP4DemuxerTrack fromTrakBox(TrakBox trak) {
        SampleSizesBox stsz = NodeBox.findFirstPath(trak, SampleSizesBox.class, Box.path("mdia.minf.stbl.stsz"));
        if (stsz.getDefaultSize() == 0)
            return newTrack(trak);
        return new PCMMP4DemuxerTrack(movie, trak, input);
    }

    protected AbstractMP4DemuxerTrack newTrack(TrakBox trak) {
        return new CodecMP4DemuxerTrack(movie, trak, input);
    }

    MP4Demuxer(SeekableByteChannel input) throws IOException {
        this.input = input;
        tracks = new LinkedList<AbstractMP4DemuxerTrack>();
        findMovieBox(input);
    }

    private void findMovieBox(SeekableByteChannel input) throws IOException {
        Movie mv = MP4Util.parseFullMovieChannel(input);
        if (mv == null || mv.getMoov() == null)
            throw new IOException("Could not find movie meta information box");
        movie = mv.getMoov();

        processHeader(movie);
    }

    private void processHeader(NodeBox moov) throws IOException {
        TrakBox tt = null;
        TrakBox[] trakBoxs = NodeBox.findAll(moov, TrakBox.class, "trak");
        for (int i = 0; i < trakBoxs.length; i++) {
            TrakBox trak = trakBoxs[i];
            SampleEntry se = NodeBox.findFirstPath(trak, SampleEntry.class, new String[] { "mdia", "minf", "stbl", "stsd", null });
            if (se != null && "tmcd".equals(se.getFourcc())) {
                tt = trak;
            } else {
                tracks.add(fromTrakBox(trak));
            }
        }
        if (tt != null) {
            DemuxerTrack video = getVideoTrack();
            if (video != null)
                timecodeTrack = new TimecodeMP4DemuxerTrack(movie, tt, input);
        }
    }

    public static MP4TrackType getTrackType(TrakBox trak) {
        HandlerBox handler = NodeBox.findFirstPath(trak, HandlerBox.class, Box.path("mdia.hdlr"));
        return MP4TrackType.fromHandler(handler.getComponentSubType());
    }

    public DemuxerTrack getVideoTrack() {
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

    @Override
    public List<AbstractMP4DemuxerTrack> getTracks() {
        return new ArrayList<AbstractMP4DemuxerTrack>(tracks);
    }

    @Override
    public List<DemuxerTrack> getVideoTracks() {
        ArrayList<DemuxerTrack> result = new ArrayList<DemuxerTrack>();
        for (AbstractMP4DemuxerTrack demuxerTrack : tracks) {
            if (demuxerTrack.box.isVideo())
                result.add(demuxerTrack);
        }
        return result;
    }

    @Override
    public List<DemuxerTrack> getAudioTracks() {
        ArrayList<DemuxerTrack> result = new ArrayList<DemuxerTrack>();
        for (AbstractMP4DemuxerTrack demuxerTrack : tracks) {
            if (demuxerTrack.box.isAudio())
                result.add(demuxerTrack);
        }
        return result;
    }

    public TimecodeMP4DemuxerTrack getTimecodeTrack() {
        return timecodeTrack;
    }

    @UsedViaReflection
    public static int probe(final ByteBuffer b) {
        ByteBuffer fork = b.duplicate();
        int success = 0;
        int total = 0;
        while (fork.remaining() >= 8) {
            long len = Platform.unsignedInt(fork.getInt());
            int fcc = fork.getInt();
            int hdrLen = 8;
            if (len == 1) {
                len = fork.getLong();
                hdrLen = 16;
            } else if (len < 8)
                break;
            if (fcc == ftyp && len < 64 || fcc == moov && len < 100 * 1024 * 1024 || fcc == free || fcc == mdat
                    || fcc == wide)
                success++;
            total++;
            if (len >= Integer.MAX_VALUE)
                break;
            NIOUtils.skip(fork, (int) (len - hdrLen));
        }

        return total == 0 ? 0 : success * 100 / total;
    }

    @Override
    public void close() throws IOException {
        input.close();
    }
}