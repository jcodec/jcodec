package org.jcodec.movtool;

import java.io.File;
import java.io.IOException;

import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.boxes.MovieBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class WebOptimize {
    public static void main1(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Syntax: optimize <movie>");
            System.exit(-1);
        }
        File tgt = new File(args[0]);
        File src = hidFile(tgt);
        tgt.renameTo(src);

        try {
            MovieBox movie = MP4Util.createRefMovieFromFile(src);

            new Flattern().flattern(movie, tgt);
        } catch (Throwable t) {
            t.printStackTrace();
            tgt.renameTo(new File(tgt.getParentFile(), tgt.getName() + ".error"));
            src.renameTo(tgt);
        }
    }
    public static File hidFile(File tgt) {
        File src = new File(tgt.getParentFile(), "." + tgt.getName());
        if (src.exists()) {
            int i = 1;
            do {
                src = new File(tgt.getParentFile(), "." + tgt.getName() + "." + (i++));
            } while (src.exists());
        }
        return src;
    }
}
