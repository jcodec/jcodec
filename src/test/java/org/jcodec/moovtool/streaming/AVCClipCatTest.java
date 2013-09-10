package org.jcodec.moovtool.streaming;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.jcodec.common.IOUtils;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.movtool.streaming.MovieRange;
import org.jcodec.movtool.streaming.VirtualMovie;
import org.jcodec.movtool.streaming.VirtualTrack;
import org.jcodec.movtool.streaming.tracks.ClipTrack;
import org.jcodec.movtool.streaming.tracks.FilePool;
import org.jcodec.movtool.streaming.tracks.RealTrack;
import org.jcodec.movtool.streaming.tracks.avc.AVCClipTrack;
import org.jcodec.movtool.streaming.tracks.avc.AVCConcatTrack;

public class AVCClipCatTest {

    public void testClipCat() throws IOException {
        File f1 = new File(System.getProperty("user.home"), "Desktop/h264_concat/test1/seq_1.mov");
        File f2 = new File(System.getProperty("user.home"), "Desktop/h264_concat/test1/seq_2.mov");
        File f3 = new File(System.getProperty("user.home"), "Desktop/h264_concat/test1/seq_3.mov");

        MovieBox m1 = MP4Util.parseMovie(f1);
        MovieBox m2 = MP4Util.parseMovie(f2);
        MovieBox m3 = MP4Util.parseMovie(f3);

        VirtualTrack t1 = new ClipTrack(new RealTrack(m1, m1.getVideoTrack(), new FilePool(f1, 10)), 30, 90);
        VirtualTrack t2 = new ClipTrack(new RealTrack(m2, m2.getVideoTrack(), new FilePool(f2, 10)), 30, 90);
        VirtualTrack t3 = new ClipTrack(new RealTrack(m3, m3.getVideoTrack(), new FilePool(f3, 10)), 30, 90);

        AVCConcatTrack ct = new AVCConcatTrack( t1, t2, t3 );
        VirtualMovie vm = new VirtualMovie(ct);

        MovieRange range = new MovieRange(vm, 0, vm.size());

        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(
                System.getProperty("user.home"), "Desktop/cat_key_clip.mov")));
        IOUtils.copy(range, out);
        out.flush();
        out.close();
    }
    
    public void testAVCClip() throws IOException {
        File f1 = new File(System.getProperty("user.home"), "Desktop/h264_concat/test2/seq_1.mov");

        MovieBox m1 = MP4Util.parseMovie(f1);

        VirtualTrack t1 = new AVCClipTrack(new RealTrack(m1, m1.getVideoTrack(), new FilePool(f1, 10)), 30, 190);

        VirtualMovie vm = new VirtualMovie(t1);

        MovieRange range = new MovieRange(vm, 0, vm.size());

        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(
                System.getProperty("user.home"), "Desktop/precise_clip.mov")));
        IOUtils.copy(range, out);
        out.flush();
        out.close();
    }
    
    public void testAVCClipCat() throws IOException {
        File f1 = new File(System.getProperty("user.home"), "Desktop/h264_concat/test1/seq_1.mov");
        File f2 = new File(System.getProperty("user.home"), "Desktop/h264_concat/test1/seq_2.mov");
        File f3 = new File(System.getProperty("user.home"), "Desktop/h264_concat/test1/seq_3.mov");

        MovieBox m1 = MP4Util.parseMovie(f1);
        MovieBox m2 = MP4Util.parseMovie(f2);
        MovieBox m3 = MP4Util.parseMovie(f3);

        VirtualTrack t1 = new AVCClipTrack(new RealTrack(m1, m1.getVideoTrack(), new FilePool(f1, 10)), 30, 90);
        VirtualTrack t2 = new AVCClipTrack(new RealTrack(m2, m2.getVideoTrack(), new FilePool(f2, 10)), 30, 90);
        VirtualTrack t3 = new AVCClipTrack(new RealTrack(m3, m3.getVideoTrack(), new FilePool(f3, 10)), 30, 90);

        AVCConcatTrack ct = new AVCConcatTrack( t1, t2, t3 );
        VirtualMovie vm = new VirtualMovie(ct);

        MovieRange range = new MovieRange(vm, 0, vm.size());

        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(
                System.getProperty("user.home"), "Desktop/cat_avc_clip.mov")));
        IOUtils.copy(range, out);
        out.flush();
        out.close();
    }
}
