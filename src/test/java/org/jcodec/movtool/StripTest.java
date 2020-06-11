package org.jcodec.movtool;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jcodec.common.AudioFormat;
import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.IntArrayList;
import org.jcodec.common.Tuple._2;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Packet.FrameType;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.common.model.TapeTimecode;
import org.jcodec.common.model.Unit;
import org.jcodec.containers.mp4.BoxUtil;
import org.jcodec.containers.mp4.Chunk;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Movie;
import org.jcodec.containers.mp4.boxes.Box.LeafBox;
import org.jcodec.containers.mp4.boxes.Edit;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mp4.muxer.CodecMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.junit.Assert;
import org.junit.Test;

public class StripTest {
    public static final byte[] FAKE_MOOV_DATA = { 'F', 'A', 'K', 'E', '_', 'M', 'O', 'O', 'V', '_', 'D', 'A', 'T',
            'A' };
    public static final byte[] SAMPLE = { 'S', 'A', 'M', 'P', 'L', 'E' };

    @Test
    public void testCutChunksToGaps() {
        for (int chunkSize = 32; chunkSize >= 1; chunkSize--) {
            ArrayList<Chunk> chunks = new ArrayList<Chunk>();
            int sc = 0;
            int offset = 0;
            for (int j = 0; j < 64; j += chunkSize) {
                int startTv = sc * 1024;
                int nextOffset = offset;
                int realChSz = Math.min(chunkSize, 64 - sc);
                int[] samples = new int[realChSz];
                for (int s = 0; s < realChSz; s++, sc++) {
                    samples[s] = sc >= 32 ? sc + 142 - 32 : sc + 42;
                    offset += samples[s];
                }
                chunks.add(new Chunk(nextOffset, startTv, realChSz, -1, samples, 1024, null, 1));
            }

            ArrayList<_2<Long, Long>> gaps = new ArrayList<_2<Long, Long>>();
            gaps.add(new _2<Long, Long>(14400L, 28799L));
            gaps.add(new _2<Long, Long>(43200L, 57599L));

            int[] expSamples = new int[] { 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 70, 71, 72, 73,
                    142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 166, 167, 168, 169, 170, 171, 172, 173 };

            List<_2<Long, Long>> newEdits = new ArrayList<_2<Long, Long>>();
            List<Chunk> result = Strip.cutChunksToGaps(chunks, gaps, newEdits);

            int startSample = 0;
            for (int i = 0; i < result.size(); i++) {
                Chunk realChunk = result.get(i);
                int[] samples = Arrays.copyOfRange(expSamples, startSample, startSample + realChunk.getSampleCount());
                Assert.assertArrayEquals(samples, realChunk.getSampleSizes());
                Assert.assertEquals(startSample * 1024, realChunk.getStartTv());
                Assert.assertEquals(calcOff(samples[0]), realChunk.getOffset());
                startSample += realChunk.getSampleCount();
            }
            Assert.assertEquals(expSamples.length, startSample);

            long[] editStarts = new long[] { 0L, 15488L, 30976L };
            long[] editDurs = new long[] { 14400L, 14400L, 7936L };
            for (int i = 0; i < 2; i++) {
                _2<Long, Long> edit = newEdits.get(i);
                Assert.assertEquals(editStarts[i], (long) edit.v0);
                Assert.assertEquals(editDurs[i], (long) edit.v1);
            }
            Assert.assertEquals(3, newEdits.size());
        }
    }

    int calcOff(int sampleVal) {
        int[] expSamples = new int[] { 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61,
                62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152,
                153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172,
                173 };
        int sum = 0;
        for (int i = 0; i < expSamples.length; i++) {
            if (expSamples[i] == sampleVal)
                break;
            sum += expSamples[i];
        }
        return sum;
    }

    @Test
    public void testGapsSimple() {
        List<Edit> edits = new ArrayList<Edit>();
        edits.add(new Edit(3, 7, 1f));
        edits.add(new Edit(8, 4, 1f));
        edits.add(new Edit(6, 15, 1f));
        List<_2<Long, Long>> exp = new ArrayList<_2<Long, Long>>();
        exp.add(new _2<Long, Long>(0L, 3L));
        exp.add(new _2<Long, Long>(12L, 14L));
        List<_2<Long, Long>> gaps = Strip.findGaps(RationalLarge.ONE, edits);
        for (int i = 0; i < 2; i++) {
            Assert.assertEquals((long) exp.get(i).v0, (long) gaps.get(i).v0);
            Assert.assertEquals((long) exp.get(i).v1, (long) gaps.get(i).v1);
        }
    }

    @Test
    public void testIntervals() throws IOException {
        File tempFile = createTestMovie(SAMPLE);
        Movie fullMovie = MP4Util.createRefFullMovieFromFile(tempFile);
        MovieBox movie = fullMovie.getMoov();
        TrakBox track = movie.getAudioTracks().get(0);
        List<Edit> edits = new ArrayList<Edit>();
        edits.add(new Edit(movie.getTimescale() * 2, track.getTimescale() * 5, 1f));
        edits.add(new Edit(movie.getTimescale() * 5, track.getTimescale() * 10, 1f));
        track.setEdits(edits);

        new Strip().strip(movie);
        File tempFile2 = File.createTempFile("strip", "m4a");
        new Flatten().flatten(fullMovie, tempFile2);

        FileChannelWrapper ch = NIOUtils.readableChannel(tempFile2);
        MP4Demuxer demuxer = MP4Demuxer.createRawMP4Demuxer(ch);

        checkAudioTrack(demuxer);
        
        ch.close();

        tempFile.delete();
        tempFile2.delete();
    }
    
    @Test
    public void testMultipleStrips() throws IOException {
        File tempFile = createTestMovie(SAMPLE);
        Movie fullMovie = MP4Util.createRefFullMovieFromFile(tempFile);
        MovieBox movie = fullMovie.getMoov();
        TrakBox track = movie.getAudioTracks().get(0);
        
        {
            List<Edit> edits = new ArrayList<Edit>();
            edits.add(new Edit(movie.getTimescale() * 4, track.getTimescale() * 3, 1f));
            edits.add(new Edit(movie.getTimescale() * 9, track.getTimescale() * 8, 1f));
            track.setEdits(edits);
            new Strip().strip(movie);
        }
        {
            List<Edit> edits = new ArrayList<Edit>();
            edits.add(new Edit(movie.getTimescale() * 3, track.getTimescale() * 1, 1f));
            edits.add(new Edit(movie.getTimescale() * 7, track.getTimescale() * 5, 1f));
            List<Edit> oldEdits = track.getEdits();
            if (oldEdits != null) {
              edits =
                  Util.editsOnEdits(
                      Rational.R(movie.getTimescale(), track.getTimescale()), oldEdits, edits);
            }
            track.setEdits(edits);
            new Strip().strip(movie);
        }
        {
            List<Edit> edits = new ArrayList<Edit>();
            edits.add(new Edit(movie.getTimescale() * 2, track.getTimescale() * 1, 1f));
            edits.add(new Edit(movie.getTimescale() * 5, track.getTimescale() * 4, 1f));
            List<Edit> oldEdits = track.getEdits();
            if (oldEdits != null) {
              edits =
                  Util.editsOnEdits(
                      Rational.R(movie.getTimescale(), track.getTimescale()), oldEdits, edits);
            }
            track.setEdits(edits);
            new Strip().strip(movie);
        }
        
        
        File tempFile2 = File.createTempFile("strip", "m4a");
        new Flatten().flatten(fullMovie, tempFile2);

        FileChannelWrapper ch = NIOUtils.readableChannel(tempFile2);
        MP4Demuxer demuxer = MP4Demuxer.createRawMP4Demuxer(ch);

        checkAudioTrack(demuxer);
        
        ch.close();

        tempFile.delete();
        tempFile2.delete();
    }

    private static void checkAudioTrack(MP4Demuxer demuxer) throws IOException {
        IntArrayList edited = IntArrayList.createIntArrayList();
        for (int i = 0; i < 1000; i++) {
            int ts = i * 1024;
            if ((ts + 1024 >= 5 * 48000 && ts < 7 * 48000) || (ts + 1024 >= 10 * 48000 && ts < 15 * 48000)) {
                edited.add(i);
            }
        }

        List<DemuxerTrack> audioTracks = demuxer.getAudioTracks();
        Assert.assertEquals(1, audioTracks.size());
        DemuxerTrack track = audioTracks.get(0);
        Packet nextFrame;
        while ((nextFrame = track.nextFrame()) != null) {
            ByteBuffer buf = nextFrame.getData();
            byte[] sample = NIOUtils.toArray(NIOUtils.read(buf, SAMPLE.length));
            Assert.assertArrayEquals(sample, SAMPLE);
            int seq = buf.getShort();
            int exp = edited.shift();
            Assert.assertEquals(exp, seq);
        }
    }

    public static File createTestMovie(byte[] sample) throws IOException {
        File tmp = File.createTempFile("test_", ".m4a");
        FileChannelWrapper ch = null;
        try {
            ch = NIOUtils.writableChannel(tmp);
            MP4Muxer muxer = MP4Muxer.createMP4MuxerToChannel(ch);

            CodecMP4MuxerTrack audioTrack = muxer.addCompressedAudioTrack(Codec.AAC, AudioFormat.MONO_44K_S16_BE);
            audioTrack.setTgtChunkDuration(Rational.R(32, 1), Unit.FRAME);
            for (int i = 0; i < 1000; i++) {
                ByteBuffer bb = ByteBuffer.allocate(sample.length + 2);
                bb.put(sample);
                bb.putShort((short) i);
                bb.flip();
                audioTrack.addFrame(MP4Packet.createPacket(bb, i * 1024, 48000, 1024, i, FrameType.KEY,
                        TapeTimecode.ZERO_TAPE_TIMECODE));
            }

            muxer.finish();

            ch.setPosition(ch.size());
            LeafBox fakeMoov = LeafBox.createLeafBox(Header.createHeader("free", 0), ByteBuffer.wrap(FAKE_MOOV_DATA));
            ch.write(BoxUtil.writeBox(fakeMoov));

            return tmp;
        } finally {
            if (ch != null) {
                NIOUtils.closeQuietly(ch);
            }
        }
    }
}
