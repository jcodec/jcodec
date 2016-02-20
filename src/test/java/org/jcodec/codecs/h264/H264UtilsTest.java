package org.jcodec.codecs.h264;

import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class H264UtilsTest {

    @Test
    public void testEscapeNAL() {
        short[] src = new short[] { 0x64, 0x00, 0x15, 0xac, 0xb2, 0x01, 0x00, 0x4b, 0x7f, 0xe0, 0x00, 0x60, 0x00, 0x82,
                0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x64, 0x1e, 0x2c, 0x5c, 0x90 };
        short[] tgt = new short[] { 0x64, 0x00, 0x15, 0xac, 0xb2, 0x01, 0x00, 0x4b, 0x7f, 0xe0, 0x00, 0x60, 0x00, 0x82,
                0x00, 0x00, 0x03, 0x00, 0x02, 0x00, 0x00, 0x03, 0x00, 0x64, 0x1e, 0x2c, 0x5c, 0x90 };
        
        byte[] b = Arrays.copyOf(asByteArray(src), src.length + 2);
        ByteBuffer bb = ByteBuffer.wrap(b);
        bb.limit(bb.limit() - 2);

        H264Utils.escapeNAL(bb);
        
        for (byte c : b) {
            System.out.println(String.format("%02x", c & 0xff));
        }
        
        Assert.assertArrayEquals(asByteArray(tgt), b);
    }

    byte[] asByteArray(short[] src) {
        byte[] result = new byte[src.length];
        for (int i = 0; i < src.length; i++) {
            result[i] = (byte) src[i];
        }
        return result;
    }
    
    @Test
    public void testAvcCToAnnexB() {
        ArrayList<ByteBuffer> spsList = new ArrayList<ByteBuffer>();
        spsList.add(ByteBuffer.wrap(new byte[] {'s', 't', 'a', 'n'}));
        spsList.add(ByteBuffer.wrap(new byte[] {'t', 'h', 'e'}));
        ArrayList<ByteBuffer> ppsList = new ArrayList<ByteBuffer>();
        ppsList.add(ByteBuffer.wrap(new byte[] {'m', 'a', 'n'}));
        ppsList.add(ByteBuffer.wrap(new byte[] {'c', 'o', 'o', 'l'}));
        AvcCBox avcCBox = new AvcCBox(66, 0, 42, 0, spsList, ppsList);
        byte[] res = H264Utils.avcCToAnnexB(avcCBox);
        Assert.assertArrayEquals(new byte[] { 0, 0, 0, 1, 0x67, 's', 't', 'a', 'n', 0, 0, 0, 1, 0x67, 't', 'h', 'e', 0,
                0, 0, 1, 0x68, 'm', 'a', 'n', 0, 0, 0, 1, 0x68, 'c', 'o', 'o', 'l' }, res);
        
    }
}
