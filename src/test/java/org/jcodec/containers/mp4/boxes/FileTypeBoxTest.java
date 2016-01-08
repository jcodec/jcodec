package org.jcodec.containers.mp4.boxes;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.List;

import org.jcodec.common.io.ByteBufferSeekableByteChannel;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Atom;
import org.junit.Test;

public class FileTypeBoxTest {
    @Test
    public void testParse() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(0x1c + 8);
        buf.putInt(0x1c);
        buf.put("ftyp".getBytes());
        buf.put("mp42".getBytes());
        buf.putInt(1);
        buf.put("mp41".getBytes());
        buf.put("mp42".getBytes());
        buf.put("isom".getBytes());
        buf.putInt(8);
        buf.put("free".getBytes());
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
