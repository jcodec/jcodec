package org.jcodec.containers.mp4.boxes;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jcodec.common.io.ByteBufferSeekableByteChannel;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Atom;
import org.junit.Test;

import js.nio.ByteBuffer;
import js.util.List;

public class FileTypeBoxTest {
    @Test
    public void testParse() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(0x1c + 8);
        buf.putInt(0x1c);
        buf.putArr("ftyp".getBytes());
        buf.putArr("mp42".getBytes());
        buf.putInt(1);
        buf.putArr("mp41".getBytes());
        buf.putArr("mp42".getBytes());
        buf.putArr("isom".getBytes());
        buf.putInt(8);
        buf.putArr("free".getBytes());
        buf.clear();

        SeekableByteChannel input = new ByteBufferSeekableByteChannel(buf);
        List<Atom> rootAtoms = MP4Util.getRootAtoms(input);
        assertEquals(2, rootAtoms.size());
        Atom atom = rootAtoms.get(0);
        assertEquals("ftyp", atom.getHeader().getFourcc());
        Box box = atom.parseBox(input);
        assertTrue(FileTypeBox.class.isInstance(box));

        FileTypeBox ftyp = (FileTypeBox) box;
        assertEquals("mp42", ftyp.getMajorBrand());
        assertArrayEquals(new String[] { "mp41", "mp42", "isom" }, ftyp.getCompBrands().toArray(new String[0]));
    }

}
