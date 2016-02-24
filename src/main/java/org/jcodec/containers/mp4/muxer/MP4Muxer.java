package org.jcodec.containers.mp4.muxer;

import static org.jcodec.containers.mp4.TrackType.SOUND;
import static org.jcodec.containers.mp4.TrackType.VIDEO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jcodec.api.NotSupportedException;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.TrackType;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.EndianBox;
import org.jcodec.containers.mp4.boxes.EndianBox.Endian;
import org.jcodec.containers.mp4.boxes.FileTypeBox;
import org.jcodec.containers.mp4.boxes.FormatBox;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.LeafBox;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.MovieHeaderBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Creates MP4 file out of a set of samples
 * 
 * @author The JCodec project
 * 
 */
public class MP4Muxer {
    private List<AbstractMP4MuxerTrack> tracks;
    protected long mdatOffset;

    private int nextTrackId = 1;
    protected SeekableByteChannel out;

    public MP4Muxer(SeekableByteChannel output) throws IOException {
        this(output, Brand.MP4);
    }

    public MP4Muxer(SeekableByteChannel output, Brand brand) throws IOException {
        this(output, brand.getFileTypeBox());
    }

    public MP4Muxer(SeekableByteChannel output, FileTypeBox ftyp) throws IOException {
        this.tracks = new ArrayList<AbstractMP4MuxerTrack>();
        this.out = output;

        ByteBuffer buf = ByteBuffer.allocate(1024);
        ftyp.write(buf);
        new Header("wide", 8).write(buf);
        new Header("mdat", 1).write(buf);
        mdatOffset = buf.position();
        buf.putLong(0);
        buf.flip();
        output.write(buf);
    }

    public FramesMP4MuxerTrack addVideoTrackWithTimecode(String fourcc, Size size, String encoderName, int timescale) {
        TimecodeMP4MuxerTrack timecode = addTimecodeTrack(timescale);

        FramesMP4MuxerTrack track = addTrack(VIDEO, timescale);

        track.addSampleEntry(videoSampleEntry(fourcc, size, encoderName));
        track.setTimecode(timecode);

        return track;
    }

    public FramesMP4MuxerTrack addVideoTrack(String fourcc, Size size, String encoderName, int timescale) {
        FramesMP4MuxerTrack track = addTrack(VIDEO, timescale);

        track.addSampleEntry(videoSampleEntry(fourcc, size, encoderName));
        return track;
    }

    public static VideoSampleEntry videoSampleEntry(String fourcc, Size size, String encoderName) {
        return VideoSampleEntry
                .createVideoSampleEntry(new Header(fourcc), (short) 0, (short) 0, "jcod", 0, 768, (short) size.getWidth(), (short) size.getHeight(), 72, 72, (short) 1, encoderName != null ? encoderName : "jcodec", (short) 24, (short) 1, (short) -1);
    }

    public static AudioSampleEntry audioSampleEntry(String fourcc, int drefId, int sampleSize, int channels,
            int sampleRate, Endian endian) {
        AudioSampleEntry ase = AudioSampleEntry
                .createAudioSampleEntry(new Header(fourcc, 0), (short) drefId, (short) channels, (short) 16, sampleRate, (short) 0, 0, 65535, 0, 1, sampleSize, channels * sampleSize, sampleSize, (short) 1);

        NodeBox wave = new NodeBox(new Header("wave"));
        ase.add(wave);

        wave.add(FormatBox.createFormatBox(fourcc));
        wave.add(EndianBox.createEndianBox(endian));
        wave.add(terminatorAtom());
        // ase.add(new ChannelBox(atom));

        return ase;
    }

    public static AudioSampleEntry compressedAudioSampleEntry(String fourcc, int drefId, int sampleSize, int channels,
            int sampleRate, int samplesPerPacket, int bytesPerPacket, int bytesPerFrame) {
        AudioSampleEntry ase = AudioSampleEntry
                .createAudioSampleEntry(new Header(fourcc, 0), (short) drefId, (short) channels, (short) 16, sampleRate, (short) 0, 0, 65534, 0, samplesPerPacket, bytesPerPacket, bytesPerFrame, 16 / 8, (short) 1);
        return ase;
    }

    public static LeafBox terminatorAtom() {
        return LeafBox.createLeafBox(new Header(new String(new byte[4])), ByteBuffer.allocate(0));
    }

    public TimecodeMP4MuxerTrack addTimecodeTrack(int timescale) {
        TimecodeMP4MuxerTrack track = new TimecodeMP4MuxerTrack(out, nextTrackId++, timescale);
        tracks.add(track);
        return track;
    }

    public FramesMP4MuxerTrack addTrack(TrackType type, int timescale) {
        FramesMP4MuxerTrack track = new FramesMP4MuxerTrack(out, nextTrackId++, type, timescale);
        tracks.add(track);
        return track;
    }

    public PCMMP4MuxerTrack addPCMTrack(int timescale, int sampleDuration, int sampleSize,
            SampleEntry se) {
        PCMMP4MuxerTrack track = new PCMMP4MuxerTrack(out, nextTrackId++, TrackType.SOUND, timescale, sampleDuration, sampleSize, se);
        tracks.add(track);
        return track;
    }

    public List<AbstractMP4MuxerTrack> getTracks() {
        return tracks;
    }

    public void writeHeader() throws IOException {
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

        return MovieHeaderBox.createMovieHeaderBox(timescale, duration, 1.0f, 1.0f, new Date().getTime(), new Date().getTime(), new int[] { 0x10000, 0, 0, 0, 0x10000, 0, 0, 0, 0x40000000 }, nextTrackId);
    }

    public static String lookupFourcc(AudioFormat format) {
        if (format.getSampleSizeInBits() == 16 && !format.isBigEndian())
            return "sowt";
        else if (format.getSampleSizeInBits() == 24)
            return "in24";
        else
            throw new NotSupportedException("Audio format " + format + " is not supported.");
    }

    public PCMMP4MuxerTrack addPCMAudioTrack(AudioFormat format) {
        return addPCMTrack((int) format.getSampleRate(), 1, (format.getSampleSizeInBits() >> 3)
                * format.getChannels(), audioSampleEntry(format));
    }

    public static AudioSampleEntry audioSampleEntry(AudioFormat format) {
        return MP4Muxer.audioSampleEntry(lookupFourcc(format), 1,
        format.getSampleSizeInBits() >> 3, format.getChannels(), (int) format.getSampleRate(),
        format.isBigEndian() ? Endian.BIG_ENDIAN : Endian.LITTLE_ENDIAN);
    }

    public FramesMP4MuxerTrack addCompressedAudioTrack(String fourcc, int timescale, int channels, int sampleRate,
            int samplesPerPkt, Box... extra) {
        FramesMP4MuxerTrack track = addTrack(SOUND, timescale);

        AudioSampleEntry ase = AudioSampleEntry
                .createAudioSampleEntry(new Header(fourcc, 0), (short) 1, (short) channels, (short) 16, sampleRate, (short) 0, 0, 65534, 0, samplesPerPkt, 0, 0, 2, (short) 1);

        NodeBox wave = new NodeBox(new Header("wave"));
        ase.add(wave);

        wave.add(FormatBox.createFormatBox(fourcc));
        for (Box box : extra)
            wave.add(box);

        wave.add(terminatorAtom());

        track.addSampleEntry(ase);

        return track;
    }
}