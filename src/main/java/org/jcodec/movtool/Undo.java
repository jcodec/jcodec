package org.jcodec.movtool;

import java.io.File;

import org.jcodec.containers.mp4.MP4Util.Atom;

import java.io.IOException;
import java.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Uses QuickTime feature to undo the recent changes
 *
 * @author The JCodec project
 */
public class Undo {
    public static void main1(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Syntax: qt-undo [-l] <movie>");
            System.err.println("\t-l\t\tList all the previous versions of this movie.");
            System.exit(-1);
        }
        if ("-l".equals(args[0])) {
            List<Atom> list = MoovVersions.listMoovVersionAtoms(new File(args[1]));
            System.out.println((list.size() - 1) + " versions.");
        } else {
            MoovVersions.undo(new File(args[0]));
        }
    }

}
