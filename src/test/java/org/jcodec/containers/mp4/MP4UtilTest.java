package org.jcodec.containers.mp4;
import static org.jcodec.HexDump.hexdump0;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.AutoFileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.containers.mp4.MP4Util.Atom;
import org.jcodec.containers.mp4.MP4Util.Movie;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.MediaInfoBox;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.lang.System;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

public class MP4UtilTest {
    @Test
    public void testSimple() throws Exception {
        File f = new File("./src/test/resources/video/seq_h264_1.mp4");
        MovieBox moov = MP4Util.parseMovie(f);
        assertNotNull(moov);
        assertNotNull(moov.getVideoTrack());
        assertNotNull(moov.getAudioTracks().get(0));
        SampleEntry[] sampleEntries = moov.getVideoTrack().getSampleEntries();
        VideoSampleEntry vse = (VideoSampleEntry) sampleEntries[0];
        assertNotNull(vse);
        AvcCBox avcc = (AvcCBox) vse.getBoxes().get(0);
        assertNotNull(avcc);
        System.out.println(sampleEntries);
        
        Box box = moov.getAudioTracks().get(0).getSampleEntries()[0].getBoxes().get(0);
        assertEquals("esds", box.getFourcc());
        System.out.println(box);
    }
    @Test
    @Ignore
    public void _testName() throws Exception {
        File f = new File("src/test/resources/zhuker/1D158634-69DF-4C7F-AB6F-CCC83F04FEDB/1.mp4");
        MovieBox moov = MP4Util.parseMovie(f);
        MediaInfoBox minf = moov.getVideoTrack().getMdia().getMinf();
        AvcCBox avcCBox = (AvcCBox) NodeBox.findFirstPath(minf, Box.path("stbl.stsd.avc1.avcC"));
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
    public void _testReadWriteIphoneMp4() throws Exception {
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
//            System.out.println(read + " " + read.remaining());
//            System.out.println(dump(read, -read.position(), new StringBuilder()));
//            System.out.println(written + " " + written.remaining());
//            System.out.println(dump(written, -written.position(), new StringBuilder()));
        }
        assertTrue(equals);
    }
}
