package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

import org.jcodec.containers.mp4.boxes.Box.LeafBox;
import org.junit.Assert;
import org.junit.Test;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class EditsBoxTest {
    @Test
    public void testShit() {
        LeafBox box = new LeafBox(Header.createHeader("free", 0),
                ByteBuffer.wrap(new byte[] { 0, 0, 0, 16, 'e', 'l', 's', 't', 0, 0, 0, 0, 0, 0, 0, 50, 0 }));
        Assert.assertFalse(EditsBox.isLookingLikeEdits(box));
    }

}
