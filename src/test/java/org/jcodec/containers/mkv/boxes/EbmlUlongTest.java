package org.jcodec.containers.mkv.boxes;

import static org.jcodec.containers.mkv.MKVType.CueClusterPosition;

import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;

public class EbmlUlongTest {

    @Test
    public void test() {
        EbmlUlong z = new EbmlUlong(CueClusterPosition.id);
        z.setUlong(2);
        Assert.assertArrayEquals("THIS IS PARTA!!!", new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02}, z.data.array());
    }
    
    @Test
    public void test1() {
        EbmlUlong z = new EbmlUlong(CueClusterPosition.id);
        z.setBuf(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02}));
        Assert.assertEquals("THIS IS PARTA!!!", 2L, z.getUlong());
    }

}
