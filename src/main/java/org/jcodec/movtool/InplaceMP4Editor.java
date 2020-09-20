package org.jcodec.movtool;

import org.jcodec.common.Tuple;
import org.jcodec.common.Tuple._2;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mp4.BoxFactory;
import org.jcodec.containers.mp4.BoxUtil;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Atom;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.MovieFragmentBox;

import java.io.File;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.jcodec.common.Preconditions.checkNotNull;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Parses MP4 header and allows custom MP4Editor to modify it, then tries to put
 * the resulting header into the same place relatively to a file.
 * 
 * This might not work out, for example if the resulting header is bigger then
 * the original.
 * 
 * Use this class to make blazing fast changes to MP4 files when you know your
 * are not adding anything new to the header, perhaps only patching some values
 * or removing stuff from the header.
 * 
 * @author The JCodec project
 * 
 */
public class InplaceMP4Editor {

    /**
     * Tries to modify movie header in place according to what's implemented in
     * the edit, the file gets pysically modified if the operation is
     * successful. No temporary file is created.
     * 
     * @param file
     *            A file to be modified
     * @param edit
     *            An edit to be carried out on a movie header
     * @return Whether or not edit was successful, i.e. was there enough place
     *         to put the new header
     * @throws IOException
     * @throws Exception
     */
    public boolean modify(File file, MP4Edit edit) throws IOException {
        SeekableByteChannel fi = null;
        try {
            fi = NIOUtils.rwChannel(file);

            List<Tuple._2<Atom, ByteBuffer>> fragments = doTheFix(fi, edit);
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

    /**
     * Tries to modify movie header in place according to what's implemented in
     * the edit. Copies modified contents to a new file.
     * 
     * Note: The header is still edited in-place, so the new file will have
     * all-the-same sample offsets.
     * 
     * Note: Still subject to the same limitations as 'modify', i.e. the new
     * header must 'fit' into an old place.
     * 
     * This method is useful when you can't write to the original file, for ex.
     * you don't have permission.
     * 
     * @param src
     *            An original file
     * @param dst
     *            A file to store the modified copy
     * @param edit
     *            An edit logic to apply
     * @return
     * @throws IOException
     */
    public boolean copy(File src, File dst, MP4Edit edit) throws IOException {
        SeekableByteChannel fi = null;
        SeekableByteChannel fo = null;
        try {
            fi = NIOUtils.readableChannel(src);
            fo = NIOUtils.writableChannel(dst);

            List<Tuple._2<Atom, ByteBuffer>> fragments = doTheFix(fi, edit);
            if (fragments == null)
                return false;

            List<_2<Long, ByteBuffer>> fragOffsets = Tuple._2map0(fragments, new Tuple.Mapper<Atom, Long>() {
                public Long map(Atom t) {
                    return t.getOffset();
                }
            });

            // If everything is clean, only then actually start writing file
            Map<Long, ByteBuffer> rewrite = Tuple.asMap(fragOffsets);
            for (Atom atom : MP4Util.getRootAtoms(fi)) {
                ByteBuffer byteBuffer = rewrite.get(atom.getOffset());
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

    /**
     * Tries to modify movie header in place according to what's implemented in
     * the edit. Copies modified contents to a new file with the same name
     * erasing the original file if successful.
     * 
     * This is a shortcut for 'copy' when you want the new file to have the same
     * name but for some reason can not modify the original file in place. Maybe
     * modifications of files are expensive or not supported on your filesystem.
     * 
     * @param src
     *            A source and destination file
     * @param edit
     *            An edit to be applied
     * @return
     * @throws IOException
     */
    public boolean replace(File src, MP4Edit edit) throws IOException {
        File tmp = new File(src.getParentFile(), "." + src.getName());
        if (copy(src, tmp, edit)) {
            tmp.renameTo(src);
            return true;
        }
        return false;
    }

    private List<Tuple._2<Atom, ByteBuffer>> doTheFix(SeekableByteChannel fi, MP4Edit edit) throws IOException {
        Atom moovAtom = getMoov(fi);
        checkNotNull(moovAtom);

        ByteBuffer moovBuffer = fetchBox(fi, moovAtom);
        MovieBox moovBox = (MovieBox) parseBox(moovBuffer);

        List<Tuple._2<Atom, ByteBuffer>> fragments = new LinkedList<Tuple._2<Atom, ByteBuffer>>();
        if (BoxUtil.containsBox(moovBox, "mvex")) {
            List<Tuple._2<ByteBuffer, MovieFragmentBox>> temp = new LinkedList<Tuple._2<ByteBuffer, MovieFragmentBox>>();
            for (Atom fragAtom : getFragments(fi)) {
                ByteBuffer fragBuffer = fetchBox(fi, fragAtom);
                fragments.add(Tuple.pair(fragAtom, fragBuffer));
                MovieFragmentBox fragBox = (MovieFragmentBox) parseBox(fragBuffer);
                fragBox.setMovie(moovBox);
                temp.add(Tuple.pair(fragBuffer, fragBox));
            }

            edit.applyToFragment(moovBox, Tuple._2_project1(temp).toArray(new MovieFragmentBox[0]));

            for (Tuple._2<ByteBuffer, ? extends Box> frag : temp) {
                if (!rewriteBox(frag.v0, frag.v1))
                    return null;
            }
        } else
            edit.apply(moovBox);

        if (!rewriteBox(moovBuffer, moovBox))
            return null;
        fragments.add(Tuple.pair(moovAtom, moovBuffer));
        return fragments;
    }

    private void replaceBox(SeekableByteChannel fi, Atom atom, ByteBuffer buffer) throws IOException {
        fi.setPosition(atom.getOffset());
        fi.write(buffer);
    }

    private boolean rewriteBox(ByteBuffer buffer, Box box) {
        try {
            ((java.nio.Buffer)buffer).clear();
            box.write(buffer);
            if (buffer.hasRemaining()) {
                if (buffer.remaining() < 8)
                    return false;
                buffer.putInt(buffer.remaining());
                buffer.put(new byte[] { 'f', 'r', 'e', 'e' });
            }
            ((java.nio.Buffer)buffer).flip();
            return true;
        } catch (BufferOverflowException e) {
            return false;
        }
    }

    private ByteBuffer fetchBox(SeekableByteChannel fi, Atom moov) throws IOException {
        fi.setPosition(moov.getOffset());
        ByteBuffer oldMov = NIOUtils.fetchFromChannel(fi, (int) moov.getHeader().getSize());
        return oldMov;
    }

    private Box parseBox(ByteBuffer oldMov) {
        Header header = Header.read(oldMov);
        Box box = BoxUtil.parseBox(oldMov, header, BoxFactory.getDefault());
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
