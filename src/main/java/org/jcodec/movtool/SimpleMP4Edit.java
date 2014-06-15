package org.jcodec.movtool;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jcodec.common.Assert;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.Tuple;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Atom;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.BoxFactory;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.MovieFragmentBox;
import org.jcodec.containers.mp4.boxes.NodeBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public abstract class SimpleMP4Edit {

    /**
     * Operation performed on a movie header and fragments
     * 
     * @param mov
     */
    protected void apply(MovieBox mov, MovieFragmentBox[] fragmentBox) {
    }

    /**
     * Operation performed on a movie header
     * 
     * @param mov
     */
    protected void apply(MovieBox mov) {
    }

    public boolean inplace(File f) throws IOException, Exception {
        SeekableByteChannel fi = null;
        try {
            fi = NIOUtils.rwFileChannel(f);

            List<Tuple._2<Atom, ByteBuffer>> fragments = doTheFix(fi);
            if (fragments == null)
                return false;

            // If everything is clean, only then actually writing stuff to the
            // file
            for (Tuple._2<Atom, ByteBuffer> fragment : fragments) {
                replaceBox(fi, fragment.v0, fragment.v1);
            }

            return true;
        } finally {
            NIOUtils.closeQuietly(fi);
        }
    }

    public boolean copy(File f, File dst) throws IOException {
        SeekableByteChannel fi = null;
        SeekableByteChannel fo = null;
        try {
            fi = NIOUtils.readableFileChannel(f);
            fo = NIOUtils.writableFileChannel(dst);

            List<Tuple._2<Atom, ByteBuffer>> fragments = doTheFix(fi);
            if (fragments == null)
                return false;

            // If everything is clean, only then actually start writing file
            Map<Atom, ByteBuffer> rewrite = Tuple.asMap(fragments);
            for (Atom atom : MP4Util.getRootAtoms(fi)) {
                ByteBuffer byteBuffer = rewrite.get(atom);
                if (byteBuffer != null)
                    fo.write(byteBuffer);
                else
                    atom.copy(fi, fo);
            }

            return true;
        } finally {
            NIOUtils.closeQuietly(fi);
            NIOUtils.closeQuietly(fo);
        }
    }

    private List<Tuple._2<Atom, ByteBuffer>> doTheFix(SeekableByteChannel fi) throws IOException {
        Atom moovAtom = getMoov(fi);
        Assert.assertNotNull(moovAtom);

        ByteBuffer moovBuffer = fetchBox(fi, moovAtom);
        MovieBox moovBox = (MovieBox) parseBox(moovBuffer);

        List<Tuple._2<Atom, ByteBuffer>> fragments = new LinkedList<Tuple._2<Atom, ByteBuffer>>();
        if (Box.findFirst(moovBox, "mvex") != null) {
            List<Tuple._2<ByteBuffer, MovieFragmentBox>> temp = new LinkedList<Tuple._2<ByteBuffer, MovieFragmentBox>>();
            for (Atom fragAtom : getFragments(fi)) {
                ByteBuffer fragBuffer = fetchBox(fi, fragAtom);
                fragments.add(Tuple._2(fragAtom, fragBuffer));
                MovieFragmentBox fragBox = (MovieFragmentBox) parseBox(fragBuffer);
                fragBox.setMovie(moovBox);
                temp.add(Tuple._2(fragBuffer, fragBox));
            }

            apply(moovBox, Tuple._2_project1(temp).toArray(new MovieFragmentBox[0]));

            for (Tuple._2<ByteBuffer, ? extends Box> frag : temp) {
                if (!rewriteBox(frag.v0, frag.v1))
                    return null;
            }
        } else
            apply(moovBox);

        if (!rewriteBox(moovBuffer, moovBox))
            return null;
        fragments.add(Tuple._2(moovAtom, moovBuffer));
        return fragments;
    }

    private void replaceBox(SeekableByteChannel fi, Atom atom, ByteBuffer buffer) throws IOException {
        fi.position(atom.getOffset());
        fi.write(buffer);
    }

    private boolean rewriteBox(ByteBuffer buffer, Box box) {
        buffer.clear();
        box.write(buffer);
        if (buffer.hasRemaining()) {
            if (buffer.remaining() < 8)
                return false;
            buffer.putInt(buffer.remaining());
            buffer.put(new byte[] { 'f', 'r', 'e', 'e' });
        }
        buffer.flip();

        return true;
    }

    private ByteBuffer fetchBox(SeekableByteChannel fi, Atom moov) throws IOException {
        fi.position(moov.getOffset());
        ByteBuffer oldMov = NIOUtils.fetchFrom(fi, (int) moov.getHeader().getSize());
        return oldMov;
    }

    private Box parseBox(ByteBuffer oldMov) {
        Header header = Header.read(oldMov);
        Box box = NodeBox.parseBox(oldMov, header, BoxFactory.getDefault());
        return box;
    }

    private Atom getMoov(SeekableByteChannel f) throws IOException {
        for (Atom atom : MP4Util.getRootAtoms(f)) {
            if ("moov".equals(atom.getHeader().getFourcc())) {
                return atom;
            }
        }
        return null;
    }

    private List<Atom> getFragments(SeekableByteChannel f) throws IOException {
        List<Atom> result = new LinkedList<Atom>();
        for (Atom atom : MP4Util.getRootAtoms(f)) {
            if ("moof".equals(atom.getHeader().getFourcc())) {
                result.add(atom);
            }
        }
        return result;
    }
}
