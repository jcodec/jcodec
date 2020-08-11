package org.jcodec.containers.mp4.muxer;

import static org.jcodec.common.Preconditions.checkArgument;
import static org.jcodec.common.Preconditions.checkNotNull;
import static org.jcodec.common.Preconditions.checkState;
import static org.jcodec.containers.mp4.MP4TrackType.SOUND;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.jcodec.common.AudioCodecMeta;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.Codec;
import org.jcodec.common.Muxer;
import org.jcodec.common.MuxerTrack;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4TrackType;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.FileTypeBox;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.MovieHeaderBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Creates MP4 file out of a set of samples
 * 
 * @author The JCodec project
 * 
 */
public class MP4Muxer implements Muxer {
    private List<AbstractMP4MuxerTrack> tracks;
    protected long mdatOffset;

    private int nextTrackId = 1;
    protected SeekableByteChannel out;

    public static MP4Muxer createMP4MuxerToChannel(SeekableByteChannel output) throws IOException {
        return new MP4Muxer(output, Brand.MP4.getFileTypeBox());
    }

    public static MP4Muxer createMP4Muxer(SeekableByteChannel output, Brand brand) throws IOException {
        return new MP4Muxer(output, brand.getFileTypeBox());
    }

    public MP4Muxer(SeekableByteChannel output, FileTypeBox ftyp) throws IOException {
        this.tracks = new ArrayList<AbstractMP4MuxerTrack>();
        this.out = output;

        ByteBuffer buf = ByteBuffer.allocate(1024);
        ftyp.write(buf);
        Header.createHeader("wide", 8).write(buf);
        Header.createHeader("mdat", 1).write(buf);
        mdatOffset = buf.position();
        buf.putLong(0);
        ((java.nio.Buffer)buf).flip();
        output.write(buf);
    }

    public TimecodeMP4MuxerTrack addTimecodeTrack() {
        return addTrack(new TimecodeMP4MuxerTrack(nextTrackId++));
    }
    
    public CodecMP4MuxerTrack addTrackWithId(MP4TrackType type, Codec codec, int trackId) {
        checkArgument(!hasTrackId(trackId), "track with id %s already exists", trackId);
        CodecMP4MuxerTrack track = new CodecMP4MuxerTrack(trackId, type, codec);
        tracks.add(track);
        nextTrackId = Math.max(nextTrackId, trackId + 1);
        return track;
    }
    
    public int getNextTrackId() {
        return nextTrackId;
    }

    private CodecMP4MuxerTrack doAddTrack(MP4TrackType type, Codec codec) {
        return addTrack(new CodecMP4MuxerTrack(nextTrackId++, type, codec));
    }
    
    public <T extends AbstractMP4MuxerTrack> T addTrack(T track) {
        checkNotNull(track, "track can not be null");
        int trackId = track.getTrackId();
        checkArgument(trackId <= nextTrackId);
        checkArgument(!hasTrackId(trackId), "track with id %s already exists", trackId);
        tracks.add(track.setOut(out));
        nextTrackId = Math.max(trackId + 1, nextTrackId);
        return track;
    }

    public boolean hasTrackId(int trackId) {
        for (AbstractMP4MuxerTrack t : tracks) {
            if (t.getTrackId() == trackId) {
                return true;
            }
        }
        return false;
    }

    public List<AbstractMP4MuxerTrack> getTracks() {
        return Collections.unmodifiableList(tracks);
    }

    @Override
    public void finish() throws IOException {
        checkState(tracks.size() != 0, "Can not save header with 0 tracks.");
        MovieBox movie = finalizeHeader();

        storeHeader(movie);
    }

    public void storeHeader(MovieBox movie) throws IOException {
        long mdatSize = out.position() - mdatOffset + 8;
        MP4Util.writeMovie(out, movie);

        out.setPosition(mdatOffset);
        NIOUtils.writeLong(out, mdatSize);
    }

    public MovieBox finalizeHeader() throws IOException {
        MovieBox movie = MovieBox.createMovieBox();
        MovieHeaderBox mvhd = movieHeader();
        movie.addFirst(mvhd);

        for (AbstractMP4MuxerTrack track : tracks) {
            Box trak = track.finish(mvhd);
            if (trak != null)
                movie.add(trak);
        }
        return movie;
    }

    public AbstractMP4MuxerTrack getVideoTrack() {
        for (AbstractMP4MuxerTrack frameMuxer : tracks) {
            if (frameMuxer.isVideo()) {
                return frameMuxer;
            }
        }
        return null;
    }

    public AbstractMP4MuxerTrack getTimecodeTrack() {
        for (AbstractMP4MuxerTrack frameMuxer : tracks) {
            if (frameMuxer.isTimecode()) {
                return frameMuxer;
            }
        }
        return null;
    }

    public List<AbstractMP4MuxerTrack> getAudioTracks() {
        ArrayList<AbstractMP4MuxerTrack> result = new ArrayList<AbstractMP4MuxerTrack>();
        for (AbstractMP4MuxerTrack frameMuxer : tracks) {
            if (frameMuxer.isAudio()) {
                result.add(frameMuxer);
            }
        }
        return result;
    }

    private MovieHeaderBox movieHeader() {
        AbstractMP4MuxerTrack videoTrack = getVideoTrack();
        int timescale = tracks.get(0).getTimescale();
        if (videoTrack != null) {
            timescale = videoTrack.getTimescale();
        }
        long duration = 0;
        for (AbstractMP4MuxerTrack track : tracks) {
            long trackDuration = track.getTrackTotalDuration() * timescale / track.getTimescale();
            if (trackDuration > duration) {
                duration = trackDuration;
            }
        }

        return MovieHeaderBox.createMovieHeaderBox(timescale, duration, 1.0f, 1.0f, new Date().getTime(),
                new Date().getTime(), new int[] { 0x10000, 0, 0, 0, 0x10000, 0, 0, 0, 0x40000000 }, nextTrackId);
    }

    public PCMMP4MuxerTrack addPCMAudioTrack(AudioFormat format) {
        return addTrack(new PCMMP4MuxerTrack(nextTrackId++, format));
    }

    public CodecMP4MuxerTrack addCompressedAudioTrack(Codec codec, AudioFormat format) {
        CodecMP4MuxerTrack track = doAddTrack(SOUND, codec);
        track.addAudioSampleEntry(format);

        return track;
    }

    @Override
    public MuxerTrack addVideoTrack(Codec codec, VideoCodecMeta meta) {
        CodecMP4MuxerTrack track = doAddTrack(MP4TrackType.VIDEO, codec);
        checkArgument(meta != null || codec == Codec.H264,
                "VideoCodecMeta is required upfront for all codecs but H.264");
        track.addVideoSampleEntry(meta);
        return track;
    }

    @Override
    public MuxerTrack addAudioTrack(Codec codec, AudioCodecMeta meta) {
        AudioFormat format = meta.getFormat();
        if (codec == Codec.PCM) {
            return addPCMAudioTrack(format);
        } else {
            return addCompressedAudioTrack(codec, format);
        }
    }

    public MP4MuxerTrack addMetaTrack(String contentEncoding, String contentType) {
        MP4MuxerTrack track = addTrack(new MP4MuxerTrack(nextTrackId++, MP4TrackType.META));
        track.addMetaSampleEntry(contentEncoding, contentType);
        return track;
    }
}
