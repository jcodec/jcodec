package org.jcodec.containers.mp4.muxer;

import static org.jcodec.containers.mp4.MP4TrackType.SOUND;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
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
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4TrackType;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.Box.LeafBox;
import org.jcodec.containers.mp4.boxes.EndianBox;
import org.jcodec.containers.mp4.boxes.FileTypeBox;
import org.jcodec.containers.mp4.boxes.FormatBox;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.MovieHeaderBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.platform.Platform;

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
        buf.flip();
        output.write(buf);
    }

    public static VideoSampleEntry videoSampleEntry(String fourcc, Size size, String encoderName) {
        return VideoSampleEntry.createVideoSampleEntry(new Header(fourcc), (short) 0, (short) 0, "jcod", 0, 768,
                (short) size.getWidth(), (short) size.getHeight(), 72, 72, (short) 1,
                encoderName != null ? encoderName : "jcodec", (short) 24, (short) 1, (short) -1);
    }

    public static AudioSampleEntry audioSampleEntry(String fourcc, int drefId, int sampleSize, int channels,
            int sampleRate, ByteOrder endian) {
        AudioSampleEntry ase = AudioSampleEntry.createAudioSampleEntry(Header.createHeader(fourcc, 0), (short) drefId,
                (short) channels, (short) 16, sampleRate, (short) 0, 0, 65535, 0, 1, sampleSize, channels * sampleSize,
                sampleSize, (short) 1);

        NodeBox wave = new NodeBox(new Header("wave"));
        ase.add(wave);

        wave.add(FormatBox.createFormatBox(fourcc));
        wave.add(EndianBox.createEndianBox(endian));
        wave.add(terminatorAtom());
        // ase.add(new ChannelBox(atom));

        return ase;
    }

    public static LeafBox terminatorAtom() {
        return LeafBox.createLeafBox(new Header(Platform.stringFromBytes(new byte[4])), ByteBuffer.allocate(0));
    }

    public TimecodeMP4MuxerTrack addTimecodeTrack() {
        TimecodeMP4MuxerTrack track = new TimecodeMP4MuxerTrack(out, nextTrackId++);
        tracks.add(track);
        return track;
    }
    
    public FramesMP4MuxerTrack addTrackWithId(MP4TrackType type, Codec codec, int trackId) {
        for(AbstractMP4MuxerTrack t : tracks) {
            if (t.getTrackId() == trackId) {
                throw new IllegalArgumentException("track with id " + trackId + " already exists");
            }
        }
        FramesMP4MuxerTrack track = new FramesMP4MuxerTrack(out, trackId, type, codec);
        tracks.add(track);
        nextTrackId = Math.max(nextTrackId, trackId+1);
        return track;
    }

    private FramesMP4MuxerTrack addTrack(MP4TrackType type, Codec codec) {
        FramesMP4MuxerTrack track = new FramesMP4MuxerTrack(out, nextTrackId++, type, codec);
        tracks.add(track);
        return track;
    }

    public List<AbstractMP4MuxerTrack> getTracks() {
        return tracks;
    }

    @Override
    public void finish() throws IOException {
        if(tracks.size() == 0)
            throw new RuntimeException("Can not save header with 0 tracks.");
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
        MovieHeaderBox mvhd = movieHeader(movie);
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

    private MovieHeaderBox movieHeader(NodeBox movie) {
        int timescale = tracks.get(0).getTimescale();
        long duration = tracks.get(0).getTrackTotalDuration();
        AbstractMP4MuxerTrack videoTrack = getVideoTrack();
        if (videoTrack != null) {
            timescale = videoTrack.getTimescale();
            duration = videoTrack.getTrackTotalDuration();
        }

        return MovieHeaderBox.createMovieHeaderBox(timescale, duration, 1.0f, 1.0f, new Date().getTime(),
                new Date().getTime(), new int[] { 0x10000, 0, 0, 0, 0x10000, 0, 0, 0, 0x40000000 }, nextTrackId);
    }

    public PCMMP4MuxerTrack addPCMAudioTrack(AudioFormat format) {
        PCMMP4MuxerTrack track = new PCMMP4MuxerTrack(out, nextTrackId++, format);
        tracks.add(track);
        return track;
    }

    public FramesMP4MuxerTrack addCompressedAudioTrack(Codec codec, AudioFormat format) {
        FramesMP4MuxerTrack track = addTrack(SOUND, codec);
        track.addAudioSampleEntry(format);

        return track;
    }

    @Override
    public MuxerTrack addVideoTrack(Codec codec, VideoCodecMeta meta) {
        FramesMP4MuxerTrack track = addTrack(MP4TrackType.VIDEO, codec);
        if (meta == null && codec != Codec.H264) {
            throw new RuntimeException("VideoCodecMeta is required upfront for all codecs but H.264");
        }
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
}