package org.jcodec.containers.mp4;

import static org.jcodec.HexDump.dump;
import static org.jcodec.HexDump.hexdump0;
import static org.junit.Assert.*;

import java.io.File;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

import org.jcodec.HexDump;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.AutoFileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.containers.mp4.MP4Util.Atom;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.MediaInfoBox;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class MP4UtilTest {
    @Test
    @Ignore
    public void testName() throws Exception {
        File f = new File("src/test/resources/zhuker/1D158634-69DF-4C7F-AB6F-CCC83F04FEDB/1.mp4");
        MovieBox moov = MP4Util.parseMovie(f);
        MediaInfoBox minf = moov.getVideoTrack().getMdia().getMinf();
        AvcCBox avcCBox = Box.findFirstPath(minf, AvcCBox.class, Box.path("stbl.stsd.avc1.avcC"));
        long size = avcCBox.getHeader().getSize();
        ByteBuffer buf = ByteBuffer.allocate(128);
        avcCBox.write(buf);
        buf.flip();
        System.out.println(hexdump0(buf));
        Assert.assertEquals(size, buf.remaining());
    }

    @Test
    @Ignore
    //this test fails if avcc box is parsed and then written
    public void testReadWriteIphoneMp4() throws Exception {
        File f = new File("src/test/resources/zhuker/1D158634-69DF-4C7F-AB6F-CCC83F04FEDB/1.mp4");
        ByteBuffer read = ByteBuffer.allocate(64 * 1024);
        MP4Util.parseMovie(f).write(read);
        read.flip();

        Atom atom = MP4Util.findFirstAtom("moov", new AutoFileChannelWrapper(f));
        MappedByteBuffer written = NIOUtils.mapFile(f);
        written.position((int) atom.getOffset());
        written.limit((int) (written.position() + atom.getHeader().getSize()));

        boolean equals = read.equals(written);
        if (!equals) {
            System.out.println(read + " " + read.remaining());
            System.out.println(dump(read, -read.position(), new StringBuilder()));
            System.out.println(written + " " + written.remaining());
            System.out.println(dump(written, -written.position(), new StringBuilder()));
        }
        assertTrue(equals);
    }
}
