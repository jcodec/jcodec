package org.jcodec.movtool;

import java.io.File;

import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.MediaHeaderBox;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.MovieFragmentBox;
import org.jcodec.containers.mp4.boxes.TrakBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class ChangeTimescale {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Syntax: chts <movie> <timescale>");
            System.exit(-1);
        }
        final int ts = Integer.parseInt(args[1]);
        if (ts < 600) {
            System.out.println("Could not set timescale < 600");
            System.exit(-1);
        }
        new InplaceMP4Editor().modify(new File(args[0]), new MP4Edit() {
            public void apply(MovieBox mov) {
                TrakBox vt = mov.getVideoTrack();
                MediaHeaderBox mdhd = Box.findFirst(vt, MediaHeaderBox.class, "mdia", "mdhd");
                int oldTs = mdhd.getTimescale();

                if (oldTs > ts) {
                    throw new RuntimeException("Old timescale (" + oldTs + ") is greater then new timescale (" + ts
                            + "), not touching.");
                }

                vt.fixMediaTimescale(ts);

                mov.fixTimescale(ts);
            }

            @Override
            public void apply(MovieBox mov, MovieFragmentBox[] fragmentBox) {
                throw new RuntimeException("Unsupported");
            }
        });
    }
}
