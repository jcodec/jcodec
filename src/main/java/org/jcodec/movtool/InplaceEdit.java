package org.jcodec.movtool;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.jcodec.common.io.RandomAccessFileInputStream;
import org.jcodec.common.io.RandomAccessFileOutputStream;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Atom;
import org.jcodec.containers.mp4.boxes.MovieBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public abstract class InplaceEdit {

    protected abstract void apply(MovieBox mov);

    public boolean save(File f) throws IOException, Exception {
        RandomAccessFileInputStream fi = null;
        RandomAccessFileOutputStream out = null;
        try {
            fi = new RandomAccessFileInputStream(f);
            Atom moov = getMoov(fi);
            Assert.assertNotNull(moov);
            MovieBox movBox = (MovieBox) moov.parseBox(fi);
            apply(movBox);
            if (movBox.calcSize() <= moov.getHeader().getSize())
                return false;
            out = new RandomAccessFileOutputStream(f);
            out.seek(moov.getOffset());
            movBox.write(out);
            
            return true;
        } finally {
            if (fi != null)
                fi.close();
            if (out != null)
                out.close();
        }
    }

    private Atom getMoov(RandomAccessFileInputStream f) throws IOException {
        for (Atom atom : MP4Util.getRootAtoms(f)) {
            if ("moov".equals(atom.getHeader().getFourcc())) {
                return atom;
            }
        }
        return null;
    }
}
