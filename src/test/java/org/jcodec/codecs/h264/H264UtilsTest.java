package org.jcodec.codecs.h264;
import org.jcodec.platform.Platform;
import org.junit.Assert;
import org.junit.Test;

import java.lang.System;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class H264UtilsTest {

    @Test
    public void testAvcCToAnnexB() {
        ArrayList<ByteBuffer> spsList = new ArrayList<ByteBuffer>();
        spsList.add(ByteBuffer.wrap(new byte[] {'s', 't', 'a', 'n'}));
        spsList.add(ByteBuffer.wrap(new byte[] {'t', 'h', 'e'}));
        ArrayList<ByteBuffer> ppsList = new ArrayList<ByteBuffer>();
        ppsList.add(ByteBuffer.wrap(new byte[] {'m', 'a', 'n'}));
        ppsList.add(ByteBuffer.wrap(new byte[] {'c', 'o', 'o', 'l'}));
        AvcCBox avcCBox = AvcCBox.createAvcCBox(66, 0, 42, 0, spsList, ppsList);
        byte[] res = H264Utils.avcCToAnnexB(avcCBox);
        Assert.assertArrayEquals(new byte[] { 0, 0, 0, 1, 0x67, 's', 't', 'a', 'n', 0, 0, 0, 1, 0x67, 't', 'h', 'e', 0,
                0, 0, 1, 0x68, 'm', 'a', 'n', 0, 0, 0, 1, 0x68, 'c', 'o', 'o', 'l' }, res);

    }
}
