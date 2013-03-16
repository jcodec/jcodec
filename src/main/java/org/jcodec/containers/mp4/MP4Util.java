package org.jcodec.containers.mp4;

import static org.jcodec.common.JCodecUtil.bufin;

import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.JCodecUtil;
import org.jcodec.common.io.FileRAInputStream;
import org.jcodec.common.io.RAInputStream;
import org.jcodec.common.io.WindowInputStream;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.BoxFactory;
import org.jcodec.containers.mp4.boxes.Edit;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.MovieBox;
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

    public static MovieBox createRefMovie(RAInputStream input, String url) throws IOException {
        MovieBox movie = parseMovie(input);

        for (TrakBox trakBox : movie.getTracks()) {
            trakBox.setDataRef(url);
        }
        return movie;
    }

    public static MovieBox parseMovie(RAInputStream input) throws IOException {
        List<Atom> rootAtoms = getRootAtoms(input);
        for (Atom atom : rootAtoms) {
            if ("moov".equals(atom.getHeader().getFourcc())) {
                return (MovieBox) atom.parseBox(input);
            }
        }
        return null;
    }

    public static List<Atom> getRootAtoms(RAInputStream input) throws IOException {
        input.seek(0);
        List<Atom> result = new ArrayList<Atom>();
        long off = 0;
        Header atom;
        do {
            atom = Header.read(input);
            if (atom == null)
                break;
            result.add(new Atom(atom, off));
            if (atom.getSize() < 8) {
                System.out.println("Broken atom '" + atom.getFourcc() + "': size " + atom.getSize());
                break;
            }
            off += atom.getSize();
            input.seek(off);
        } while (true);

        return result;
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

        public Box parseBox(RAInputStream input) throws IOException {
            input.seek(offset + header.headerSize());
            return NodeBox.parseBox(input, header, BoxFactory.getDefault());
        }

        public void copy(RAInputStream input, DataOutput out) throws IOException {
            input.seek(offset);
            WindowInputStream wnd = new WindowInputStream(input, header.getSize());
            byte[] buf = new byte[8096];
            int read;
            while ((read = wnd.read(buf)) != -1)
                out.write(buf, 0, read);
        }
    }

    public static MovieBox parseMovie(File source) throws IOException {
        RAInputStream input = null;
        try {
            input = bufin(source);
            return parseMovie(input);
        } finally {
            if (input != null)
                input.close();
        }
    }

    public static MovieBox createRefMovie(File source) throws IOException {
        RAInputStream input = null;
        try {
            input = bufin(source);
            return createRefMovie(input, "file://" + source.getCanonicalPath());
        } finally {
            if (input != null)
                input.close();
        }
    }
}