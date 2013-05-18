package org.jcodec.movtool;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.Assert;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Atom;
import org.jcodec.containers.mp4.boxes.BoxFactory;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.NodeBox;

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
        SeekableByteChannel fi = null;
        try {
            fi = NIOUtils.rwFileChannel(f);
            Atom moov = getMoov(fi);
            Assert.assertNotNull(moov);

            fi.position(moov.getOffset());
            ByteBuffer oldMov = NIOUtils.fetchFrom(fi, (int) moov.getHeader().getSize());
            Header header = Header.read(oldMov);
            MovieBox movBox = (MovieBox) NodeBox.parseBox(oldMov, header, BoxFactory.getDefault());

            apply(movBox);

            oldMov.position(0);
            oldMov.limit(oldMov.capacity());

            try {
                movBox.write(oldMov);
            } catch (Exception e) {
                return false;
            }
            if (oldMov.hasRemaining()) {
                if (oldMov.remaining() < 8)
                    return false;
                oldMov.putInt(oldMov.remaining());
                oldMov.put(new byte[] { 'f', 'r', 'e', 'e' });
            }
            fi.position(moov.getOffset());
            fi.write(oldMov);

            return true;
        } finally {
            if (fi != null)
                fi.close();
        }
    }

    private Atom getMoov(SeekableByteChannel f) throws IOException {
        for (Atom atom : MP4Util.getRootAtoms(f)) {
            if ("moov".equals(atom.getHeader().getFourcc())) {
                return atom;
            }
        }
        return null;
    }
}
