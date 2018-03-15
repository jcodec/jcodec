package org.jcodec.containers.mp4.boxes;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jcodec.common.io.ByteBufferSeekableByteChannel;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Atom;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.List;

public class SegmentTypeBoxTest {
    @Test
    public void testParse() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(0x18 + 8);
        buf.putInt(0x18);
        buf.put("stypmsdh".getBytes());
        buf.putInt(0);
        buf.put("msdhmsix".getBytes());
        buf.putInt(8);
        buf.put("free".getBytes());
        buf.clear();

        SeekableByteChannel input = ByteBufferSeekableByteChannel.readFromByteBuffer(buf);
        List<Atom> rootAtoms = MP4Util.getRootAtoms(input);
        assertEquals(2, rootAtoms.size());
        Atom atom = rootAtoms.get(0);
        assertEquals("styp", atom.getHeader().getFourcc());
        Box box = atom.parseBox(input);
        assertTrue(box instanceof SegmentTypeBox);

        SegmentTypeBox ftyp = (SegmentTypeBox) box;
        assertEquals("msdh", ftyp.getMajorBrand());
        assertArrayEquals(new String[] { "msdh", "msix" }, ftyp.getCompBrands().toArray(new String[0]));

    }

}
