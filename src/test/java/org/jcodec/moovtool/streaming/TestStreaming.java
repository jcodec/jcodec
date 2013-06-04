package org.jcodec.moovtool.streaming;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.IOUtils;
import org.jcodec.common.NIOUtils;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.movtool.streaming.MovieRange;
import org.jcodec.movtool.streaming.VirtualMovie;
import org.jcodec.movtool.streaming.VirtualTrack;
import org.jcodec.movtool.streaming.tracks.CachingTrack;
import org.jcodec.movtool.streaming.tracks.Prores2AVCTrack;
import org.jcodec.movtool.streaming.tracks.RealTrack;
import org.jcodec.movtool.streaming.tracks.StereoDownmixTrack;

public class TestStreaming {

    public static void main(String[] args) throws IOException {

        File m1 = new File(System.getProperty("user.home") + "/Desktop/supercool.mov");

        FileChannelWrapper ch1 = NIOUtils.readableFileChannel(m1);

        MovieBox mov1 = MP4Util.parseMovie(m1);
        TrakBox v1 = mov1.getVideoTrack();

        RealTrack rt = new RealTrack(mov1, v1, ch1);
        VirtualTrack rt1 = new StereoDownmixTrack(new RealTrack(mov1, mov1.getAudioTracks().get(0), ch1),
                new RealTrack(mov1, mov1.getAudioTracks().get(1), ch1), new RealTrack(mov1, mov1.getAudioTracks()
                        .get(2), ch1), new RealTrack(mov1, mov1.getAudioTracks().get(3), ch1));

        long start = System.currentTimeMillis();
        ScheduledExecutorService cachePolicyExec = Executors.newSingleThreadScheduledExecutor();

        VirtualMovie vm = new VirtualMovie(new CachingTrack(new Prores2AVCTrack(rt, v1.getCodedSize()), 10,
                cachePolicyExec), new CachingTrack(rt1, 10, cachePolicyExec));
        System.out.println(System.currentTimeMillis() - start);

        BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(System.getProperty("user.home")
                + "/Desktop/megashit.mov"));

        for (int off = 0; off < vm.size();) {
            int to = (int) (10000 * Math.random()) + off;

            MovieRange mr;
            if (off > 20) {
                mr = new MovieRange(vm, off - 20, to);
                for (int i = 0; i < 20; i++)
                    mr.read();
                // System.out.println("RANGE: " + (off - 20) + ", " + to);
            } else {
                mr = new MovieRange(vm, off, to);
                // System.out.println("RANGE: " + off + ", " + to);
            }

            IOUtils.copy(mr, os);
            mr.close();
            off = to;
        }

        vm.release();

        os.close();
    }
}
