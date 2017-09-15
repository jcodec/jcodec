package org.jcodec.movtool;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.logging.Logger;
import org.jcodec.containers.mp4.BoxFactory;
import org.jcodec.containers.mp4.BoxUtil;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Atom;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.MovieBox;

/**
 * Parses MP4 file, applies the edit and saves the result in a new file.
 *
 * Relocates the movie header to the end of the file if necessary.
 * 
 * @author The JCodec project
 * 
 */
public class RelocateMP4Editor {
    
    public void modifyOrRelocate(File src, MP4Edit edit) throws IOException {
        boolean modify = new InplaceMP4Editor().modify(src, edit);
        if (!modify)
            relocate(src, edit);
    }

    public void relocate(File src, MP4Edit edit) throws IOException {
        SeekableByteChannel f = null;
        try {
            f = NIOUtils.rwChannel(src);
            Atom moovAtom = getMoov(f);
            ByteBuffer moovBuffer = fetchBox(f, moovAtom);
            MovieBox moovBox = (MovieBox) parseBox(moovBuffer);

            edit.apply(moovBox);

            if (moovAtom.getOffset() + moovAtom.getHeader().getSize() < f.size()) {
                Logger.info("Relocating movie header to the end of the file.");
                f.setPosition(moovAtom.getOffset() + 4);
                f.write(ByteBuffer.wrap(Header.FOURCC_FREE));
                f.setPosition(f.size());
            } else {
                f.setPosition(moovAtom.getOffset());
            }
            MP4Util.writeMovieBox(f, moovBox);
        } finally {
            NIOUtils.closeQuietly(f);
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
}
