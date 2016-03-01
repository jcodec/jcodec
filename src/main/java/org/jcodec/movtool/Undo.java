package org.jcodec.movtool;

import static org.jcodec.common.io.NIOUtils.readableChannel;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mp4.BoxFactory;
import org.jcodec.containers.mp4.BoxUtil;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Atom;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.NodeBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Uses QuickTime feature to undo the recent changes
 * 
 * @author The JCodec project
 * 
 */
public class Undo {
    public static void main1(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Syntax: qt-undo [-l] <movie>");
            System.err.println("\t-l\t\tList all the previous versions of this movie.");
            System.exit(-1);
        }
        Undo undo = new Undo();
        if ("-l".equals(args[0])) {
            List<Atom> list = undo.list(args[1]);
            System.out.println((list.size() - 1) + " versions.");
        } else {
            undo.undo(args[0]);
        }
    }

    private void undo(String fineName) throws IOException {
        List<Atom> versions = list(fineName);
        if (versions.size() < 2) {
            System.err.println("Nowhere to rollback.");
            return;
        }
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(new File(fineName), "rw");
            raf.seek(versions.get(versions.size() - 2).getOffset() + 4);
            raf.write(new byte[] { 'm', 'o', 'o', 'v' });
            raf.seek(versions.get(versions.size() - 1).getOffset() + 4);
            raf.write(new byte[] { 'f', 'r', 'e', 'e' });
        } finally {
            raf.close();
        }
    }

    private List<Atom> list(String fileName) throws IOException {
        ArrayList<Atom> result = new ArrayList<Atom>();
        SeekableByteChannel is = null;
        try {
            is = readableChannel(new File(fileName));
            int version = 0;
            for (Atom atom : MP4Util.getRootAtoms(is)) {
                if ("free".equals(atom.getHeader().getFourcc()) && isMoov(is, atom)) {
                    result.add(atom);
                }
                if ("moov".equals(atom.getHeader().getFourcc())) {
                    result.add(atom);
                    break;
                }
            }
        } finally {
            is.close();
        }
        return result;
    }

    private boolean isMoov(SeekableByteChannel is, Atom atom) throws IOException {
        is.setPosition(atom.getOffset() + atom.getHeader().headerSize());
        try {
            Box mov = BoxUtil.parseBox(NIOUtils.fetchFromChannel(is, (int) atom.getHeader().getSize()), Header.createHeader("moov", atom
                    .getHeader().getSize()), BoxFactory.getDefault());
            return (mov instanceof MovieBox) && BoxUtil.containsBox((NodeBox) mov, "mvhd");
        } catch (Throwable t) {
            return false;
        }
    }
}
