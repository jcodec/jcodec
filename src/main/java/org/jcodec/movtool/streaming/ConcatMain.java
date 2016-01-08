package org.jcodec.movtool.streaming;

import static java.lang.Integer.parseInt;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;

import org.jcodec.common.io.IOUtils;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.movtool.streaming.tracks.ConcatTrack;
import org.jcodec.movtool.streaming.tracks.FilePool;
import org.jcodec.movtool.streaming.tracks.RealTrack;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class ConcatMain {
    
    public static void main(String[] args) throws Exception {
        File folder = new File(System.getProperty("user.home"), "upload");
        File[] listFiles = folder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("chunk") && name.endsWith(".mov");
            }
        });
        Arrays.sort(listFiles, new Comparator<File>() {
            public int compare(File o1, File o2) {
                return parseInt(o1.getName().replaceAll("[^0-9]", ""))
                        - parseInt(o2.getName().replaceAll("[^0-9]", ""));
            }
        });
        RealTrack[] tracks = new RealTrack[listFiles.length];
        for (int i = 0; i < tracks.length; i++) {
            File m1 = listFiles[i];
            FilePool ch1 = new FilePool(m1, 1);
            MovieBox mov1 = MP4Util.parseMovie(m1);
            TrakBox v1 = mov1.getVideoTrack();

            RealTrack rt = new RealTrack(mov1, v1, ch1);
            tracks[i] = rt;
        }
        ConcatTrack concat = new ConcatTrack(tracks);
        VirtualMovie vm = new VirtualMP4Movie(concat);

        BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(new File(
                System.getProperty("user.home"), "concat.mov")));
        MovieRange movieRange = new MovieRange(vm, 0, vm.size() - 1);
        IOUtils.copy(movieRange, os);
        movieRange.close();
        os.close();
        vm.close();
    }
}
