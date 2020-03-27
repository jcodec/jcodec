package org.jcodec.samples.mp4;

import static org.jcodec.common.io.NIOUtils.readableChannel;
import static org.jcodec.common.io.NIOUtils.writableChannel;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.TrackType;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Rational;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.common.tools.MainUtils.Flag;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Movie;
import org.jcodec.containers.mp4.boxes.Edit;
import org.jcodec.containers.mp4.boxes.MetaValue;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.TextMetaDataSampleEntry;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mp4.demuxer.MP4DemuxerTrackMeta;
import org.jcodec.movtool.Flatten;
import org.jcodec.movtool.MetadataEditor;
import org.jcodec.movtool.Strip;
import org.jcodec.movtool.Util;
import org.jcodec.samples.mp4.Test1Proto.Words;
import org.jcodec.samples.mp4.Test1Proto.Words.Word;
import org.jcodec.samples.mp4.Test2Proto.Tag;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * This example trims a movie allowing to select the duration and offset of a
 * trim.
 * 
 * @author The JCodec project
 * 
 */
public class Trim {
    private static final Flag FLAG_FROM_SEC = Flag.flag("from", "f", "From second");
    private static final Flag FLAG_TO_SEC = Flag.flag("duration", "d", "Duration of the audio");
    private static final Flag FLAG_TITLE = Flag.flag("title", "t", "Metadata title to set");
    private static final Flag FLAG_AUTHOR = Flag.flag("author", "a", "Metadata author to set");
    private static final Flag FLAG_GPS = Flag.flag("gps", "g", "Metadata GPS to set");
    private static final Flag[] flags = { FLAG_FROM_SEC, FLAG_TO_SEC, FLAG_TITLE, FLAG_AUTHOR, FLAG_GPS };

    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();
        final Cmd cmd = MainUtils.parseArguments(args, flags);
        if (cmd.argsLength() < 2) {
            MainUtils.printHelpCmd("strip", flags, Arrays.asList(new String[] { "in movie", "?out movie" }));
            System.exit(-1);
            return;
        }
        SeekableByteChannel input = null;
        SeekableByteChannel out = null;

        try {
            input = readableChannel(new File(cmd.getArg(0)));
            File file = new File(cmd.getArg(1));
            out = writableChannel(file);
            Movie movie = MP4Util.createRefFullMovie(input, "file://" + new File(cmd.getArg(0)).getAbsolutePath());
            addMetadata(movie, cmd.getStringFlagD(FLAG_TITLE, "A title"), cmd.getStringFlagD(FLAG_AUTHOR, "An author"),
                    cmd.getStringFlagD(FLAG_GPS, "+81.1000-015.5999/"));
            final double inS = cmd.getDoubleFlagD(FLAG_FROM_SEC, 0d);
            final double durS = cmd.getDoubleFlagD(FLAG_TO_SEC,
                    ((double) movie.getMoov().getDuration() / movie.getMoov().getTimescale()) - inS);
            modifyMovie(inS, durS, movie.getMoov());
            Flatten flatten = new Flatten();
            TrakBox[] tracks = movie.getMoov().getTracks();
            for (TrakBox trak : tracks) {
                if (trak.getTrackHeader().getNo() == 1) {
                    flatten.setSampleProcessor(trak, new WordProcessor(inS, durS));
                } else if (trak.getTrackHeader().getNo() == 2) {
                    flatten.setSampleProcessor(trak, new TagProcessor(inS, durS));
                }
            }
            long finishTime = System.currentTimeMillis();
            System.out.println("Checkpoint: " + (finishTime - startTime) + "ms");
            flatten.flattenChannel(movie, out);
        } finally {
            if (input != null)
                input.close();
            if (out != null)
                out.close();
        }
        long finishTime = System.currentTimeMillis();
        System.out.println("Processing time: " + (finishTime - startTime) + "ms");
    }

    private static void addMetadata(Movie movie, String title, String author, String gps) {
        MetadataEditor.MovieEditor mediaMeta = MetadataEditor.MovieEditor.createFromMovie(movie.getMoov());
        Map<Integer, MetaValue> meta = mediaMeta.getItunesMeta();
        meta.put(0xa9616c62, MetaValue.createString(title));
        meta.put(0xa96e616d, MetaValue.createString(author));
        Map<Integer, MetaValue> udata = mediaMeta.getUdata();
        udata.put(0xa978797a, MetaValue.createStringWithLocale(gps, 0x15c7));
        mediaMeta.apply(movie.getMoov());
    }

    private static class WordProcessor implements Flatten.SampleProcessor {
        private double inS;
        private double durS;

        public WordProcessor(double inS, double durS) {
            this.inS = inS;
            this.durS = durS;
        }

        @Override
        public ByteBuffer processSample(ByteBuffer src) throws IOException {
            int size = src.getInt();
            if (size != src.remaining())
                throw new IOException("Error");
            long masterOnset = (long) (inS * 1000);
            long masterOffset = masterOnset + (long) (durS * 1000);
            Words words = Words.parseFrom(src);

            Words.Builder builder = words.toBuilder();
            int w = 0;
            while (builder.getWordsCount() > w) {
                Word word = builder.getWords(w);
                long onset = word.getOnsetMilli();
                long offset = word.getOffsetMilli();
                if (onset > masterOnset && offset < masterOffset) {
                    word = word.toBuilder().setOnsetMilli(onset - masterOnset).setOffsetMilli(offset - masterOnset)
                            .build();
                    builder.setWords(w++, word);
                } else {
                    builder.removeWords(w);
                }
            }
            words = builder.build();
            // System.out.println(words);
            return withLen(words.toByteArray());
        }
    }

    private static ByteBuffer withLen(byte[] byteArray) {
        ByteBuffer out = ByteBuffer.allocate(byteArray.length + 4);
        out.putInt(byteArray.length);
        out.put(byteArray);
        out.flip();
        return out;
    }

    private static class TagProcessor implements Flatten.SampleProcessor {
        private double inS;
        private double durS;

        public TagProcessor(double inS, double durS) {
            this.inS = inS;
            this.durS = durS;
        }

        @Override
        public ByteBuffer processSample(ByteBuffer src) throws IOException {
            int size = src.getInt();
            if (size != src.remaining())
                throw new IOException("Error");
            long masterOnset = (long) (inS * 1000);
            Tag audioTag = Tag.parseFrom(src);
            long newOnset = audioTag.getOnsetMilli() - masterOnset;
            if (newOnset < 0)
                newOnset = 0;
            audioTag = audioTag.toBuilder().setOnsetMilli(newOnset).build();
//            System.out.println(audioTag);
            return withLen(audioTag.toByteArray());
        }
    }

    private static void modifyMovie(double inS, double durS, MovieBox movie) throws IOException {
        for (TrakBox track : movie.getTracks()) {
            List<Edit> edits = new ArrayList<Edit>();
            Edit edit = new Edit((long) (movie.getTimescale() * durS), (long) (track.getTimescale() * inS), 1f);
            edits.add(edit);
            List<Edit> oldEdits = track.getEdits();
            if (oldEdits != null) {
                edits = Util.editsOnEdits(Rational.R(movie.getTimescale(), track.getTimescale()), oldEdits, edits);
            }
            track.setEdits(edits);
        }

        Strip strip = new Strip();
        strip.strip(movie);
        strip.trim(movie, null);
    }

    private static ByteBuffer nextFrame(SeekableDemuxerTrack track) throws IOException {
        Packet pkt = track.nextFrame();
        ByteBuffer buffer = pkt.getData();
        int size = buffer.getInt();
        buffer = NIOUtils.read(buffer, size);
        return buffer;
    }

    public static long getFirstTagTs(SeekableByteChannel ch) throws IOException {
        MP4Demuxer demuxer = MP4Demuxer.createMP4Demuxer(ch);
        List<SeekableDemuxerTrack> tracks = demuxer.getTracks();
        long firstTag = 0;
        for (SeekableDemuxerTrack track : tracks) {
            DemuxerTrackMeta meta = track.getMeta();
            if (meta.getType() != TrackType.META)
                continue;

            TextMetaDataSampleEntry sampleEntry = (TextMetaDataSampleEntry) ((MP4DemuxerTrackMeta) meta)
                    .getSampleEntries()[0];
            if ("application/audio_tags_1".equals(sampleEntry.getContentEncoding())) {
                Tag audioTag = Tag.parseFrom(nextFrame(track));
                firstTag = audioTag.getOnsetMilli();
            }
        }
        ch.setPosition(0);
        return firstTag;
    }
}
