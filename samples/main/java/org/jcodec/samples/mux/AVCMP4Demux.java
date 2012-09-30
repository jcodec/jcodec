package org.jcodec.samples.mux;

import java.io.File;

public class AVCMP4Demux {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Syntax: <in.264> <out.mp4>\n" + "\tWhere:\n"
                    + "\t-q\tLook for stream parameters only in the beginning of stream");
            return;
        }

        File in = new File(args[0]);
        File out = new File(args[1]);
    }

}
