package org.jcodec.movtool;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mp4.BoxFactory;
import org.jcodec.containers.mp4.BoxUtil;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.NodeBox;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.jcodec.common.Ints.checkedCast;
import static org.jcodec.common.io.IOUtils.closeQuietly;
import static org.jcodec.common.io.NIOUtils.readableChannel;
import static org.jcodec.containers.mp4.boxes.Header.FOURCC_FREE;

public class MoovVersions {
    private static final byte[] MOOV_FOURCC = {'m', 'o', 'o', 'v'};

    public static List<MP4Util.Atom> listMoovVersionAtoms(File file) throws IOException {
        ArrayList<MP4Util.Atom> result = new ArrayList<MP4Util.Atom>();
        SeekableByteChannel is = null;
        try {
            is = readableChannel(file);
            for (MP4Util.Atom atom : MP4Util.getRootAtoms(is)) {
                if ("free".equals(atom.getHeader().getFourcc()) && isMoov(is, atom)) {
                    result.add(atom);
                }
                if ("moov".equals(atom.getHeader().getFourcc())) {
                    result.add(atom);
                    break;
                }
            }
        } finally {
            closeQuietly(is);
        }
        return result;
    }

    /**
     * Appends moov to file and sets previous moov to 'free'
     */
    public static void addVersion(File file, MovieBox moov) throws IOException {
        long hdrsize = moov.getHeader().getSize();
        long estimate = moov.estimateSize();
        long size = Math.max(hdrsize, estimate) * 2;

        ByteBuffer allocate = ByteBuffer.allocate(checkedCast(size));
        moov.write(allocate);
        allocate.flip();

        MP4Util.Atom oldmoov = MP4Util.findFirstAtomInFile("moov", file);

        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "rw");
            raf.seek(oldmoov.getOffset() + 4);
            raf.write(FOURCC_FREE);
            raf.seek(raf.length());
            raf.write(NIOUtils.toArray(allocate));
        } finally {
            closeQuietly(raf);
        }
    }

    /**
     * Reverts to previous moov version
     *
     * @throws NoSuchElementException if undo is not possible, e.g. only one version is available
     */
    public static void undo(File file) throws IOException {
        List<MP4Util.Atom> versions = listMoovVersionAtoms(file);
        if (versions.size() < 2) {
            throw new NoSuchElementException("Nowhere to rollback");
        }
        rollback(file, versions.get(versions.size() - 2));
    }

    /**
     * Reverts to specific moov version. Use listMoovVersionAtoms for versions list.
     */
    public static void rollback(File file, MP4Util.Atom version) throws IOException {
        MP4Util.Atom oldmoov = MP4Util.findFirstAtomInFile("moov", file);
        if (oldmoov.getOffset() == version.getOffset()) {
            throw new IllegalArgumentException("Already at version you are trying to rollback to");
        }
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "rw");
            raf.seek(version.getOffset() + 4);
            raf.write(MOOV_FOURCC);
            raf.seek(oldmoov.getOffset() + 4);
            raf.write(FOURCC_FREE);
        } finally {
            closeQuietly(raf);
        }
    }


    private static boolean isMoov(SeekableByteChannel is, MP4Util.Atom atom) throws IOException {
        Header header = atom.getHeader();
        is.setPosition(atom.getOffset() + header.headerSize());
        try {
            Box mov = BoxUtil.parseBox(NIOUtils.fetchFromChannel(is, (int) header.getSize()), Header.createHeader("moov", header.getSize()), BoxFactory.getDefault());
            return (mov instanceof MovieBox) && BoxUtil.containsBox((NodeBox) mov, "mvhd");
        } catch (IOException t) {
            return false;
        }
    }


}
