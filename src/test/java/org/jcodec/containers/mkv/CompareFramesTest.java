package org.jcodec.containers.mkv;

import static org.jcodec.common.io.IOUtils.readFileToByteArray;
import static org.jcodec.containers.mkv.MKVType.Cluster;
import static org.jcodec.containers.mkv.MKVType.Segment;
import static org.jcodec.containers.mkv.MKVType.findAllTree;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.IOUtils;
import org.jcodec.containers.mkv.boxes.EbmlMaster;
import org.jcodec.containers.mkv.boxes.MkvBlock;
import org.junit.Assert;
import org.junit.Test;

public class CompareFramesTest {

    @Test
    public void testReadFrames() throws IOException {
        byte[] rawFrame = readFileToByteArray(new File("src/test/resources/mkv/single-frame01.vp8"));
        FileChannel channel = new FileInputStream("src/test/resources/mkv/single-frame.webm").getChannel();
        MKVParser p = new MKVParser(new FileChannelWrapper(channel));
        List<EbmlMaster> tree = p.parse();
        MKVType[] path = { Segment, Cluster };
        EbmlMaster[] me = findAllTree(tree, EbmlMaster.class, path);
        Assert.assertNotNull(me);
        Assert.assertEquals(1, me.length);
        List<MkvBlock> bs = MKVMuxerTest.getBlocksByTrackNumber(me[0], 1);
        Assert.assertNotNull(bs);
        Assert.assertEquals(1, bs.size());
        MkvBlock videoBlock = bs.get(0);
        ByteBuffer source = ByteBuffer.allocate((int) videoBlock.size());
        channel.position(videoBlock.dataOffset);
        channel.read(source);
        source.flip();

        ByteBuffer[] frames = videoBlock.getFrames(source);
        ByteBuffer byteBuffer = frames[0];
        byte[] frameBytes = MKVMuxerTest.bufferToArray(byteBuffer);
        Assert.assertNotNull(frames);
        Assert.assertEquals(1, frames.length);

        Assert.assertArrayEquals(rawFrame, frameBytes);
    }

    @Test
    public void testFramesByTrack() throws IOException {
        FileChannel c = new FileInputStream("src/test/resources/mkv/single-frame.webm").getChannel();
        try {
            MKVParser p = new MKVParser(new FileChannelWrapper(c));
            List<EbmlMaster> tree = p.parse();
            MKVType[] path = { Segment, Cluster };
            EbmlMaster[] me = findAllTree(tree, EbmlMaster.class, path);
            Assert.assertNotNull(me);
            Assert.assertEquals(1, me.length);
            List<MkvBlock> bs = MKVMuxerTest.getBlocksByTrackNumber(me[0], 1);
            Assert.assertNotNull(bs);
            Assert.assertEquals(1, bs.size());
        } finally {
            IOUtils.closeQuietly(c);
        }
    }

}
