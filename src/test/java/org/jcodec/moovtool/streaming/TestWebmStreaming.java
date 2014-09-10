package org.jcodec.moovtool.streaming;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.jcodec.common.IOUtils;
import org.jcodec.containers.mkv.MKVType;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.movtool.streaming.MovieRange;
import org.jcodec.movtool.streaming.VirtualMovie;
import org.jcodec.movtool.streaming.VirtualWebmMovie;
import org.jcodec.movtool.streaming.tracks.CachingTrack;
import org.jcodec.movtool.streaming.tracks.FilePool;
import org.jcodec.movtool.streaming.tracks.Prores2VP8Track;
import org.jcodec.movtool.streaming.tracks.RealTrack;
import org.junit.Assert;
import org.junit.Test;

public class TestWebmStreaming {

    private static final int CUES_START = 0x4D9;
    private static final int TWO_CUES_END = 0x4FB;

    public static void main(String[] args) throws IOException {

        File m1 = new File("src/test/resources/test.prores.mov");

        FilePool ch1 = new FilePool(m1, 10);
        MovieBox mov1 = MP4Util.parseMovie(m1);
        TrakBox v1 = mov1.getVideoTrack();

        RealTrack rt = new RealTrack(mov1, v1, ch1);

        long start = System.currentTimeMillis();
        ScheduledExecutorService cachePolicyExec = Executors.newSingleThreadScheduledExecutor();

        VirtualMovie vm = new VirtualWebmMovie(new CachingTrack(new Prores2VP8Track(rt, v1.getCodedSize()), 10, cachePolicyExec));
        System.out.println(System.currentTimeMillis() - start);

        File f = File.createTempFile("test", ".webm");
        System.out.println("Saving output to " + f.getAbsolutePath());
        BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(f));

        for (int off = 0; off < vm.size();) {
            int to = (int) (10000 * Math.random()) + off;

            MovieRange mr = new MovieRange(vm, off, to);
            // System.out.println("RANGE: " + off + ", " + to);

            IOUtils.copy(mr, os);
            mr.close();
            off = to + 1;
        }

        vm.close();

        os.close();
    }

    private static final byte[] b = new byte[] {
            // Cues
            (byte) 0x1C, (byte) 0x53, (byte) 0xBB, (byte) 0x6B, (byte) 0x42, (byte) 0x72,

            // CuePoint
            (byte) 0xBB, (byte) 0x8C,
            // CueTime
            (byte) 0xB3, (byte) 0x81, (byte) 0x00,
            // CueTrackPositions
            (byte) 0xB7, (byte) 0x87,
            // CueTrack
            (byte) 0xF7, (byte) 0x81, (byte) 0x01,
            // CueTrackPosition
            (byte) 0xF1, (byte) 0x82, (byte) 0x07, (byte) 0x25,

            // CuePoint
            (byte) 0xBB, (byte) 0x8D,
            // CueTime
            (byte) 0xB3, (byte) 0x81, (byte) 0x14,
            // CueTrackPositions
            (byte) 0xB7, (byte) 0x88,
            // CueTrack
            (byte) 0xF7, (byte) 0x81, (byte) 0x01,
            // CueTrackPosition
            (byte) 0xF1, (byte) 0x83, (byte) 0x04, (byte) 0x8B, (byte) 0x37 };

    @Test
    public void testMovieRange() throws Exception {
        File m1 = new File("src/test/resources/test.prores.mov");

        FilePool ch1 = new FilePool(m1, 10);
        MovieBox mov1 = MP4Util.parseMovie(m1);
        TrakBox v1 = mov1.getVideoTrack();

        RealTrack rt = new RealTrack(mov1, v1, ch1);

        long start = System.currentTimeMillis();
        ScheduledExecutorService cachePolicyExec = Executors.newSingleThreadScheduledExecutor();

        VirtualMovie vm = new VirtualWebmMovie(new CachingTrack(new Prores2VP8Track(rt, v1.getCodedSize()), 10, cachePolicyExec));
        System.out.println(System.currentTimeMillis() - start);

        MovieRange mr = null;
        try {
            int off = 0x18, to = 0x1B;
            try {
                mr = new MovieRange(vm, off, to);
                Assert.assertArrayEquals("webm".getBytes(), IOUtils.toByteArray(mr));
            } finally {
                mr.close();
            }

            off = 0x24;
            to = 0x27;
            try {
                mr = new MovieRange(vm, off, to);
                Assert.assertArrayEquals(MKVType.Segment.id, IOUtils.toByteArray(mr));
            } finally {
                mr.close();
            }

            off = 0x48B63;
            to = 0x48B66;
            try {
                mr = new MovieRange(vm, off, to);
                Assert.assertArrayEquals(MKVType.Cluster.id, IOUtils.toByteArray(mr));
            } finally {
                mr.close();
            }

            off = CUES_START;
            to = TWO_CUES_END;
            try {
                mr = new MovieRange(vm, off, to);
                Assert.assertArrayEquals(b, IOUtils.toByteArray(mr));
            } finally {
                mr.close();
            }
        } finally {
            vm.close();
        }

    }

    @Test
    public void testMultiRangeCopying() throws Exception {

        File m1 = new File("src/test/resources/test.prores.mov");

        FilePool ch1 = new FilePool(m1, 10);
        MovieBox mov1 = MP4Util.parseMovie(m1);
        TrakBox v1 = mov1.getVideoTrack();

        RealTrack rt = new RealTrack(mov1, v1, ch1);
        ScheduledExecutorService cachePolicyExec = Executors.newSingleThreadScheduledExecutor();

        VirtualMovie vm = new VirtualWebmMovie(new CachingTrack(new Prores2VP8Track(rt, v1.getCodedSize()), 10, cachePolicyExec));

        ByteArrayOutputStream baos = new ByteArrayOutputStream(b.length);

        for (int off = CUES_START; off < TWO_CUES_END;) {
            int remaining = TWO_CUES_END - off;
            int to = Math.min((int) (remaining * Math.random()) + off + 1, TWO_CUES_END);
            MovieRange mr = new MovieRange(vm, off, to);
            int copied = IOUtils.copy(mr, baos);
            int len = to - off + 1;
            System.out.println("off: " + off + " to: " + to + " len: " + len + " copied: " + copied);
            Assert.assertTrue("copied " + copied + " bytes, which is more then required " + len, len >= copied);
            mr.close();
            off = to + 1;
        }
        vm.close();

        Assert.assertArrayEquals(b, baos.toByteArray());

        baos.close();
    }
}
