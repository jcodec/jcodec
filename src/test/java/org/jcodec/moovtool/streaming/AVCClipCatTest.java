package org.jcodec.moovtool.streaming;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.jcodec.common.io.IOUtils;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.movtool.streaming.MovieRange;
import org.jcodec.movtool.streaming.VirtualMP4Movie;
import org.jcodec.movtool.streaming.VirtualMovie;
import org.jcodec.movtool.streaming.VirtualTrack;
import org.jcodec.movtool.streaming.tracks.ClipTrack;
import org.jcodec.movtool.streaming.tracks.FilePool;
import org.jcodec.movtool.streaming.tracks.RealTrack;
import org.jcodec.movtool.streaming.tracks.avc.AVCClipTrack;
import org.jcodec.movtool.streaming.tracks.avc.AVCConcatTrack;
import org.junit.Test;

public class AVCClipCatTest {

    @Test
    public void testClipCat() throws IOException {
        File f1 = new File("src/test/resources/AVCClipCatTest/seq_1.mp4");
        File f2 = new File("src/test/resources/AVCClipCatTest/seq_2.mp4");
        File f3 = new File("src/test/resources/AVCClipCatTest/seq_3.mp4");

        MovieBox m1 = MP4Util.parseMovie(f1);
        MovieBox m2 = MP4Util.parseMovie(f2);
        MovieBox m3 = MP4Util.parseMovie(f3);

        VirtualTrack t1 = new ClipTrack(new RealTrack(m1, m1.getVideoTrack(), new FilePool(f1, 10)), 60, 120);
        VirtualTrack t2 = new ClipTrack(new RealTrack(m2, m2.getVideoTrack(), new FilePool(f2, 10)), 60, 120);
        VirtualTrack t3 = new ClipTrack(new RealTrack(m3, m3.getVideoTrack(), new FilePool(f3, 10)), 60, 120);

        AVCConcatTrack ct = new AVCConcatTrack( t1, t2, t3 );
        VirtualMovie vm = new VirtualMP4Movie(ct);

        MovieRange range = new MovieRange(vm, 0, vm.size());
        
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(
                System.getProperty("user.home"), "Desktop/cat_key_clip.mp4")));
        IOUtils.copy(range, out);
        out.flush();
        out.close();
    }
    
    @Test
    public void testAVCClip() throws IOException {
        File f1 = new File("src/test/resources/AVCClipCatTest/seq_1.mp4");

        MovieBox m1 = MP4Util.parseMovie(f1);

        VirtualTrack t1 = new AVCClipTrack(new RealTrack(m1, m1.getVideoTrack(), new FilePool(f1, 10)), 60, 120);

        VirtualMovie vm = new VirtualMP4Movie(t1);

        MovieRange range = new MovieRange(vm, 0, vm.size());

        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(
                System.getProperty("user.home"), "Desktop/precise_clip.mp4")));
        IOUtils.copy(range, out);
        out.flush();
        out.close();
    }
    
    @Test
    public void testAVCClipCat() throws IOException {
        File f1 = new File("src/test/resources/AVCClipCatTest/seq_1.mp4");
        File f2 = new File("src/test/resources/AVCClipCatTest/seq_2.mp4");
        File f3 = new File("src/test/resources/AVCClipCatTest/seq_3.mp4");

        MovieBox m1 = MP4Util.parseMovie(f1);
        MovieBox m2 = MP4Util.parseMovie(f2);
        MovieBox m3 = MP4Util.parseMovie(f3);

        VirtualTrack t1 = new AVCClipTrack(new RealTrack(m1, m1.getVideoTrack(), new FilePool(f1, 10)), 60, 120);
        VirtualTrack t2 = new AVCClipTrack(new RealTrack(m2, m2.getVideoTrack(), new FilePool(f2, 10)), 60, 120);
        VirtualTrack t3 = new AVCClipTrack(new RealTrack(m3, m3.getVideoTrack(), new FilePool(f3, 10)), 60, 120);

        AVCConcatTrack ct = new AVCConcatTrack( t1, t2, t3 );
        VirtualMovie vm = new VirtualMP4Movie(ct);

        MovieRange range = new MovieRange(vm, 0, vm.size());

        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(
                System.getProperty("user.home"), "Desktop/cat_avc_clip.mp4")));
        IOUtils.copy(range, out);
        out.flush();
        out.close();
    }
}
