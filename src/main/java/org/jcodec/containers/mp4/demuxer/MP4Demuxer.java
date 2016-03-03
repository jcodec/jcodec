package org.jcodec.containers.mp4.demuxer;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.SampleSizesBox;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.platform.Platform;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
        SampleSizesBox stsz = NodeBox.findFirstPath(trak, SampleSizesBox.class, Box.path("mdia.minf.stbl.stsz"));
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
        movie = MP4Util.parseMovieChannel(input);
        if (movie == null)
            throw new IOException("Could not find movie meta information box");

        processHeader(movie);
    }

    private void processHeader(NodeBox moov) throws IOException {
        TrakBox tt = null;
        TrakBox[] trakBoxs = NodeBox.findAll(moov, TrakBox.class, "trak");
        for (int i = 0; i < trakBoxs.length; i++) {
            TrakBox trak = trakBoxs[i];
            SampleEntry se = NodeBox.findFirstPath(trak, SampleEntry.class, new String[] { "mdia", "minf", "stbl", "stsd", null });
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
    
    static private int makeInt(byte b3, byte b2, byte b1, byte b0) {
        return (((b3       ) << 24) |
                ((b2 & 0xff) << 16) |
                ((b1 & 0xff) <<  8) |
                ((b0 & 0xff)      ));
    }

    private static int intFourcc(String string) {
        byte[] b = Platform.getBytes(string);
        return makeInt(b[0], b[1], b[2], b[3]);
    }

    private static int ftyp = intFourcc("ftyp");
    private static int free = intFourcc("free"); 
    private static int moov = intFourcc("moov");
    private static int mdat = intFourcc("mdat");
    private static int wide = intFourcc("wide");
    
    public static int probe(final ByteBuffer b) {
        ByteBuffer fork = b.duplicate();
        int success = 0;
        int total = 0;
        while (fork.remaining() >= 8) {
            long len = fork.getInt() & 0xffffffffL;
            int fcc = fork.getInt();
            int hdrLen = 8;
            if (len == 1) {
                len = fork.getLong();
                hdrLen = 16;
            } else if (len < 8)
                break;
            if (fcc == ftyp && len < 64 || fcc == moov && len < 100 * 1024 * 1024 || fcc == free || fcc == mdat || fcc == wide)
                success++;
            total++;
            if (len >= Integer.MAX_VALUE)
                break;
            NIOUtils.skip(fork, (int) (len - hdrLen));
        }

        return total == 0 ? 0 : success * 100 / total;
    }

}