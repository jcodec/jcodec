package org.jcodec.containers.mkv;

import static org.jcodec.common.IOUtils.readFileToByteArray;
import static org.jcodec.containers.mkv.Type.Cluster;
import static org.jcodec.containers.mkv.Type.Segment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.List;

import org.jcodec.containers.mkv.ebml.MasterElement;
import org.jcodec.containers.mkv.elements.BlockElement;
import org.jcodec.containers.mkv.elements.Cluster;
import org.junit.Assert;
import org.junit.Test;

public class CompareFramesTest {

    @Test
    public void test() throws IOException {
        byte[] rawFrame = readFileToByteArray(new File("src/test/resources/mkv/single-frame01.vp8"));
        FileChannel channel = new FileInputStream("src/test/resources/mkv/single-frame.webm").getChannel();
        SimpleEBMLParser p = new SimpleEBMLParser(channel);
        p.parse();
        List<MasterElement> tree = p.getTree();
        Cluster[] me  = Type.findAll(tree,  Cluster.class, Segment, Cluster);
        Assert.assertNotNull(me);
        Assert.assertEquals(1, me.length);
        List<BlockElement> bs = me[0].getBlocksByTrackNumber(1);
        Assert.assertNotNull(bs);
        Assert.assertEquals(1, bs.size());
        BlockElement videoBlock = bs.get(0);
        byte[][]frames = videoBlock.getFrames(channel);
        Assert.assertNotNull(frames);
        Assert.assertEquals(1, frames.length);
        System.out.println(Reader.printAsHex(frames[0]));
        Assert.assertArrayEquals(rawFrame, frames[0]);
    }
    
    @Test
    public void testFramesByTrack() throws IOException {
        SimpleEBMLParser p = new SimpleEBMLParser(new FileInputStream("src/test/resources/mkv/single-frame.webm").getChannel());
        p.parse();
        List<MasterElement> tree = p.getTree();
        Cluster[] me  = Type.findAll(tree,  Cluster.class, Segment, Cluster);
        Assert.assertNotNull(me);
        Assert.assertEquals(1, me.length);
        List<BlockElement> bs = me[0].getBlocksByTrackNumber(1);
        Assert.assertNotNull(bs);
        Assert.assertEquals(1, bs.size());
    }

}
