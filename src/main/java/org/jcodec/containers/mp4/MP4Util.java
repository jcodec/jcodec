package org.jcodec.containers.mp4;

import static org.jcodec.common.io.IOUtils.closeQuietly;
import static org.jcodec.common.io.NIOUtils.readableChannel;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jcodec.common.AutoFileChannelWrapper;
import org.jcodec.common.Codec;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.logging.Logger;
import org.jcodec.containers.mp4.MP4Util.Atom;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.FileTypeBox;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.MovieFragmentBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.TrakBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MP4Util {

    private static Map<Codec, String> codecMapping = new HashMap<Codec, String>();

    static {
        codecMapping.put(Codec.MPEG2, "m2v1");
        codecMapping.put(Codec.H264, "avc1");
        codecMapping.put(Codec.H265, "hev1");
        codecMapping.put(Codec.J2K, "mjp2");
    }

    public static class Movie {
        private FileTypeBox ftyp;
        private MovieBox moov;

        public Movie(FileTypeBox ftyp, MovieBox moov) {
            this.ftyp = ftyp;
            this.moov = moov;
        }

        public FileTypeBox getFtyp() {
            return ftyp;
        }

        public MovieBox getMoov() {
            return moov;
        }
    }

    public static MovieBox createRefMovie(SeekableByteChannel input, String url) throws IOException {
        MovieBox movie = parseMovieChannel(input);

        TrakBox[] tracks = movie.getTracks();
        for (int i = 0; i < tracks.length; i++) {
            TrakBox trakBox = tracks[i];
            trakBox.setDataRef(url);
        }
        return movie;
    }

    public static MovieBox parseMovieChannel(SeekableByteChannel input) throws IOException {
        for (Atom atom : getRootAtoms(input)) {
            if ("moov".equals(atom.getHeader().getFourcc())) {
                return (MovieBox) atom.parseBox(input);
            }
        }
        return null;
    }

    public static Movie createRefFullMovie(SeekableByteChannel input, String url) throws IOException {
        Movie movie = parseFullMovieChannel(input);

        TrakBox[] tracks = movie.moov.getTracks();
        for (int i = 0; i < tracks.length; i++) {
            TrakBox trakBox = tracks[i];
            trakBox.setDataRef(url);
        }
        return movie;
    }

    public static Movie parseFullMovieChannel(SeekableByteChannel input) throws IOException {
        FileTypeBox ftyp = null;
        for (Atom atom : getRootAtoms(input)) {
            if ("ftyp".equals(atom.getHeader().getFourcc())) {
                ftyp = (FileTypeBox) atom.parseBox(input);
            } else if ("moov".equals(atom.getHeader().getFourcc())) {
                return new Movie(ftyp, (MovieBox) atom.parseBox(input));
            }
        }
        return null;
    }

    public static List<MovieFragmentBox> parseMovieFragments(SeekableByteChannel input) throws IOException {
        MovieBox moov = null;
        LinkedList<MovieFragmentBox> fragments = new LinkedList<MovieFragmentBox>();
        for (Atom atom : getRootAtoms(input)) {
            if ("moov".equals(atom.getHeader().getFourcc())) {
                moov = (MovieBox) atom.parseBox(input);
            } else if ("moof".equalsIgnoreCase(atom.getHeader().getFourcc())) {
                fragments.add((MovieFragmentBox) atom.parseBox(input));
            }
        }
        for (MovieFragmentBox fragment : fragments) {
            fragment.setMovie(moov);
        }
        return fragments;
    }

    public static List<Atom> getRootAtoms(SeekableByteChannel input) throws IOException {
        input.setPosition(0);
        List<Atom> result = new ArrayList<Atom>();
        long off = 0;
        Header atom;
        while (off < input.size()) {
            input.setPosition(off);
            atom = Header.read(NIOUtils.fetchFromChannel(input, 16));
            if (atom == null)
                break;
            result.add(new Atom(atom, off));
            off += atom.getSize();
        }

        return result;
    }

    public static Atom findFirstAtomInFile(String fourcc, File input) throws IOException {
        SeekableByteChannel c = new AutoFileChannelWrapper(input);
        try {
            return findFirstAtom(fourcc, c);
        } finally {
            IOUtils.closeQuietly(c);
        }
    }

    public static Atom findFirstAtom(String fourcc, SeekableByteChannel input) throws IOException {
        List<Atom> rootAtoms = getRootAtoms(input);
        for (Atom atom : rootAtoms) {
            if (fourcc.equals(atom.getHeader().getFourcc()))
                return atom;
        }
        return null;
    }

    public static Atom atom(SeekableByteChannel input) throws IOException {
        long off = input.position();
        Header atom = Header.read(NIOUtils.fetchFromChannel(input, 16));

        return atom == null ? null : new Atom(atom, off);
    }

    public static class Atom {
        private long offset;
        private Header header;

        public Atom(Header header, long offset) {
            this.header = header;
            this.offset = offset;
        }

        public long getOffset() {
            return offset;
        }

        public Header getHeader() {
            return header;
        }

        public Box parseBox(SeekableByteChannel input) throws IOException {
            input.setPosition(offset + header.headerSize());
            return BoxUtil.parseBox(NIOUtils.fetchFromChannel(input, (int) header.getBodySize()), header,
                    BoxFactory.getDefault());
        }

        public void copyContents(SeekableByteChannel input, WritableByteChannel out) throws IOException {
            input.setPosition(offset + header.headerSize());
            NIOUtils.copy(input, out, header.getBodySize());
        }

        public void copy(SeekableByteChannel input, WritableByteChannel out) throws IOException {
            input.setPosition(offset);
            NIOUtils.copy(input, out, header.getSize());
        }
    }

    public static MovieBox parseMovie(File source) throws IOException {
        SeekableByteChannel input = null;
        try {
            input = readableChannel(source);
            return parseMovieChannel(input);
        } finally {
            if (input != null)
                input.close();
        }
    }

    public static MovieBox createRefMovieFromFile(File source) throws IOException {
        SeekableByteChannel input = null;
        try {
            input = readableChannel(source);
            return createRefMovie(input, "file://" + source.getCanonicalPath());
        } finally {
            if (input != null)
                input.close();
        }
    }

    public static void writeMovieToFile(File f, MovieBox movie) throws IOException {
        SeekableByteChannel out = null;
        try {
            out = NIOUtils.writableChannel(f);
            writeMovie(out, movie);
        } finally {
            closeQuietly(out);
        }
    }

    public static void writeMovie(SeekableByteChannel out, MovieBox movie) throws IOException {
        doWriteMovieToChannel(out, movie, 0);
    }

    public static void doWriteMovieToChannel(SeekableByteChannel out, MovieBox movie, int additionalSize)
            throws IOException {
        int sizeHint = estimateMoovBoxSize(movie) + additionalSize;
        Logger.debug("Using " + sizeHint + " bytes for MOOV box");

        ByteBuffer buf = ByteBuffer.allocate(sizeHint * 4);
        movie.write(buf);
        ((java.nio.Buffer)buf).flip();
        out.write(buf);
    }

    public static Movie parseFullMovie(File source) throws IOException {
        SeekableByteChannel input = null;
        try {
            input = readableChannel(source);
            return parseFullMovieChannel(input);
        } finally {
            if (input != null)
                input.close();
        }
    }

    public static Movie createRefFullMovieFromFile(File source) throws IOException {
        SeekableByteChannel input = null;
        try {
            input = readableChannel(source);
            return createRefFullMovie(input, "file://" + source.getCanonicalPath());
        } finally {
            if (input != null)
                input.close();
        }
    }

    public static void writeFullMovieToFile(File f, Movie movie) throws IOException {
        SeekableByteChannel out = null;
        try {
            out = NIOUtils.writableChannel(f);
            writeFullMovie(out, movie);
        } finally {
            closeQuietly(out);
        }
    }

    public static void writeFullMovie(SeekableByteChannel out, Movie movie) throws IOException {
        doWriteFullMovieToChannel(out, movie, 0);
    }

    public static void doWriteFullMovieToChannel(SeekableByteChannel out, Movie movie, int additionalSize)
            throws IOException {
        int sizeHint = estimateMoovBoxSize(movie.getMoov()) + additionalSize;
        Logger.debug("Using " + sizeHint + " bytes for MOOV box");

        ByteBuffer buf = ByteBuffer.allocate(sizeHint + 128);
        movie.getFtyp().write(buf);
        movie.getMoov().write(buf);
        ((java.nio.Buffer)buf).flip();
        out.write(buf);
    }

    /**
     * Estimate buffer size needed to write MOOV box based on the amount of stuff in
     * there
     * 
     * @param movie
     * @return
     */
    public static int estimateMoovBoxSize(MovieBox movie) {
        return movie.estimateSize() + (4 << 10);
    }

    public static String getFourcc(Codec codec) {
        return codecMapping.get(codec);
    }

    public static ByteBuffer writeBox(Box box, int approxSize) {
        ByteBuffer buf = ByteBuffer.allocate(approxSize);
        box.write(buf);
        buf.flip();

        return buf;
    }

    public static void writeMdat(FileChannelWrapper out, long mdatPos, long mdatSize) throws IOException {
        out.setPosition(mdatPos);
        Header mdat = Header.createHeader("mdat", mdatSize + 16);
        if (mdat.headerSize() != 16) {
            mdat = Header.createHeader("mdat", mdatSize + 8);
            Header.createHeader("free", 8).writeChannel(out);
        }
        mdat.writeChannel(out);
    }

    public static long mdatPlaceholder(FileChannelWrapper out) throws IOException {
        long mdatPos = out.position();
        out.write(ByteBuffer.wrap(new byte[16]));
        return mdatPos;
    }

    public static void traceBox(Box box) {
        traceBoxR(box, 0);
    }

    public static void traceBoxR(Box b, int level) {
        if (b instanceof NodeBox) {
            NodeBox box = (NodeBox) b;
            for (Box box2 : box.getBoxes()) {
                traceBoxR(box2, level + 1);
            }
        } else {
            StringBuilder bld = new StringBuilder();
            for (int i = 0; i < level; i++)
                bld.append(' ');
            System.out.println(bld.toString() + b.getHeader().getFourcc());
        }
    }
    
    public static Atom getMdat(List<Atom> rootAtoms) {
        for (Atom atom : rootAtoms) {
            if ("mdat".equals(atom.getHeader().getFourcc())) {
                return atom;
            }
        }
        return null;
    }

    public static Atom getMoov(List<Atom> rootAtoms) {
        for (Atom atom : rootAtoms) {
            if ("moov".equals(atom.getHeader().getFourcc())) {
                return atom;
            }
        }
        return null;
    }
}