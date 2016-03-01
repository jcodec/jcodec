package org.jcodec.containers.mp4;

import static org.jcodec.common.io.NIOUtils.readableChannel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jcodec.common.AutoFileChannelWrapper;
import org.jcodec.common.Codec;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.logging.Logger;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsets64Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsetsBox;
import org.jcodec.containers.mp4.boxes.CompositionOffsetsBox;
import org.jcodec.containers.mp4.boxes.Edit;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.MovieFragmentBox;
import org.jcodec.containers.mp4.boxes.SampleSizesBox;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox;
import org.jcodec.containers.mp4.boxes.SyncSamplesBox;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox;
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
        codecMapping.put(Codec.J2K, "mjp2");
    }

    public static MovieBox createRefMovie(SeekableByteChannel input, String url) throws IOException {
        MovieBox movie = parseMovieChannel(input);

        for (TrakBox trakBox : movie.getTracks()) {
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
            return BoxUtil.parseBox(NIOUtils.fetchFromChannel(input, (int) header.getBodySize()), header, BoxFactory.getDefault());
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
            out.close();
        }
    }

    public static void writeMovie(SeekableByteChannel out, MovieBox movie) throws IOException {
        doWriteMovieToChannel(out, movie, 0);
    }

    public static void doWriteMovieToChannel(SeekableByteChannel out, MovieBox movie, int additionalSize) throws IOException {
        int sizeHint = estimateMoovBoxSize(movie) + additionalSize;
        Logger.debug("Using " + sizeHint + " bytes for MOOV box");

        ByteBuffer buf = ByteBuffer.allocate(sizeHint);
        movie.write(buf);
        buf.flip();
        out.write(buf);
    }

    /**
     * Estimate buffer size needed to write MOOV box based on the amount of
     * stuff in there
     * 
     * @param movie
     * @return
     */
    public static int estimateMoovBoxSize(MovieBox movie) {
        int sizeHint = 4 << 10; // 4K plus
        for (TrakBox trak : movie.getTracks()) {
            sizeHint += 4 << 10; // 4K per track
            List<Edit> edits = trak.getEdits();
            sizeHint += edits != null ? (edits.size() << 3) + (edits.size() << 2) : 0;
            ChunkOffsetsBox stco = trak.getStco();
            sizeHint += stco != null ? (stco.getChunkOffsets().length << 2) : 0;
            ChunkOffsets64Box co64 = trak.getCo64();
            sizeHint += co64 != null ? (co64.getChunkOffsets().length << 3) : 0;
            SampleSizesBox stsz = trak.getStsz();
            sizeHint += stsz != null ? (stsz.getDefaultSize() != 0 ? 0 : (stsz.getCount() << 2)) : 0;
            TimeToSampleBox stts = trak.getStts();
            sizeHint += stts != null ? (stts.getEntries().length << 3) : 0;
            SyncSamplesBox stss = trak.getStss();
            sizeHint += stss != null ? (stss.getSyncSamples().length << 2) : 0;
            CompositionOffsetsBox ctts = trak.getCtts();
            sizeHint += ctts != null ? (ctts.getEntries().length << 3) : 0;
            SampleToChunkBox stsc = trak.getStsc();
            sizeHint += stsc != null ? (stsc.getSampleToChunk().length << 3) + (stsc.getSampleToChunk().length << 2)
                    : 0;
        }
        return sizeHint;
    }

    public static Box cloneBox(Box box, int approxSize) {
        return doCloneBox(box, approxSize, BoxFactory.getDefault());
    }

    public static Box doCloneBox(Box box, int approxSize, IBoxFactory bf) {
        ByteBuffer buf = ByteBuffer.allocate(approxSize);
        box.write(buf);
        buf.flip();
        return BoxUtil.parseChildBox(buf, bf);
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
}