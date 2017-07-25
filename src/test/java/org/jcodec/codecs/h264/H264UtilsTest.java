package org.jcodec.codecs.h264;
import org.jcodec.codecs.h264.H264Utils.Mv;
import org.jcodec.codecs.h264.H264Utils.MvList;
import org.jcodec.codecs.h264.H264Utils.Ui2Vector;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.platform.Platform;
import org.junit.Assert;
import org.junit.Test;

import java.lang.System;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class H264UtilsTest {

    @Test
    public void testEscapeNAL() {
        short[] src = new short[] { 0x64, 0x00, 0x15, 0xac, 0xb2, 0x01, 0x00, 0x4b, 0x7f, 0xe0, 0x00, 0x60, 0x00, 0x82,
                0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x64, 0x1e, 0x2c, 0x5c, 0x90 };
        short[] tgt = new short[] { 0x64, 0x00, 0x15, 0xac, 0xb2, 0x01, 0x00, 0x4b, 0x7f, 0xe0, 0x00, 0x60, 0x00, 0x82,
                0x00, 0x00, 0x03, 0x00, 0x02, 0x00, 0x00, 0x03, 0x00, 0x64, 0x1e, 0x2c, 0x5c, 0x90 };
        
        byte[] b = Platform.copyOfByte(asByteArray(src), src.length + 2);
        ByteBuffer bb = ByteBuffer.wrap(b);
        bb.limit(bb.limit() - 2);

        H264Utils.escapeNALinplace(bb);
        
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
        AvcCBox avcCBox = AvcCBox.createAvcCBox(66, 0, 42, 0, spsList, ppsList);
        byte[] res = NIOUtils.toArray(H264Utils.avcCToAnnexB(avcCBox));
        Assert.assertArrayEquals(new byte[] { 0, 0, 0, 1, 0x67, 's', 't', 'a', 'n', 0, 0, 0, 1, 0x67, 't', 'h', 'e', 0,
                0, 0, 1, 0x68, 'm', 'a', 'n', 0, 0, 0, 1, 0x68, 'c', 'o', 'o', 'l' }, res);
        
    }
    
    @Test
    public void testMv() {
        int mv = 0;
        Assert.assertEquals(0, Mv.mvX(mv));
        Assert.assertEquals(0, Mv.mvY(mv));
        mv = Mv.packMv(42, 143, 11);
        Assert.assertEquals(42, Mv.mvX(mv));
        Assert.assertEquals(143, Mv.mvY(mv));
        Assert.assertEquals(42, Mv.mvC(mv, 0));
        Assert.assertEquals(143, Mv.mvC(mv, 1));
        Assert.assertEquals(11, Mv.mvRef(mv));
        mv = Mv.packMv(-42, 143, 11);
        Assert.assertEquals(-42, Mv.mvX(mv));
        Assert.assertEquals(143, Mv.mvY(mv));
        Assert.assertEquals(11, Mv.mvRef(mv));
        mv = Mv.packMv(42, -143, 11);
        Assert.assertEquals(42, Mv.mvX(mv));
        Assert.assertEquals(-143, Mv.mvY(mv));
        Assert.assertEquals(11, Mv.mvRef(mv));
        mv = Mv.packMv(-42, -143, 11);
        Assert.assertEquals(-42, Mv.mvX(mv));
        Assert.assertEquals(-143, Mv.mvY(mv));
        Assert.assertEquals(-42, Mv.mvC(mv, 0));
        Assert.assertEquals(-143, Mv.mvC(mv, 1));
        Assert.assertEquals(11, Mv.mvRef(mv));
        mv = Mv.packMv(-42, -143, -11);
        Assert.assertEquals(-42, Mv.mvX(mv));
        Assert.assertEquals(-143, Mv.mvY(mv));
        Assert.assertEquals(-11, Mv.mvRef(mv));
    }
    
    @Test
    public void testMvList() {
        MvList pair = new MvList(2);
        Assert.assertEquals(0, pair.mv0X(0));
        Assert.assertEquals(0, pair.mv0Y(0));
        Assert.assertEquals(-1, pair.mv0R(0));
        Assert.assertEquals(0, pair.mv1X(0));
        Assert.assertEquals(0, pair.mv1Y(0));
        Assert.assertEquals(-1, pair.mv1R(0));

        pair.setMv(0, 0, Mv.packMv(42, 143, 11));
        Assert.assertEquals(42, pair.mv0X(0));
        Assert.assertEquals(143, pair.mv0Y(0));
        Assert.assertEquals(11, pair.mv0R(0));
        Assert.assertEquals(0, pair.mv1X(0));
        Assert.assertEquals(0, pair.mv1Y(0));
        Assert.assertEquals(-1, pair.mv1R(0));
        
        pair.setMv(1, 1, Mv.packMv(-42, -43, 11));
        Assert.assertEquals(0, pair.mv0X(1));
        Assert.assertEquals(0, pair.mv0Y(1));
        Assert.assertEquals(-1, pair.mv0R(1));
        Assert.assertEquals(-42, pair.mv1X(1));
        Assert.assertEquals(-43, pair.mv1Y(1));
        Assert.assertEquals(11, pair.mv1R(1));
        
        pair.clear();
        Assert.assertEquals(0, pair.mv0X(0));
        Assert.assertEquals(0, pair.mv0Y(0));
        Assert.assertEquals(-1, pair.mv0R(0));
        Assert.assertEquals(0, pair.mv1X(0));
        Assert.assertEquals(0, pair.mv1Y(0));
        Assert.assertEquals(-1, pair.mv1R(0));
    }
    
    @Test
    public void testMvList2D() {
        H264Utils.MvList2D pair = new H264Utils.MvList2D(2, 2);
        Assert.assertEquals(0, pair.mv0X(1, 1));
        Assert.assertEquals(0, pair.mv0Y(1, 1));
        Assert.assertEquals(-1, pair.mv0R(1, 1));
        Assert.assertEquals(0, pair.mv1X(1, 1));
        Assert.assertEquals(0, pair.mv1Y(1, 1));
        Assert.assertEquals(-1, pair.mv1R(1, 1));

        pair.setMv(1, 0, 0, Mv.packMv(42, 143, 11));
        Assert.assertEquals(42, pair.mv0X(1, 0));
        Assert.assertEquals(143, pair.mv0Y(1, 0));
        Assert.assertEquals(11, pair.mv0R(1, 0));
        Assert.assertEquals(0, pair.mv1X(1, 0));
        Assert.assertEquals(0, pair.mv1Y(1, 0));
        Assert.assertEquals(-1, pair.mv1R(1, 0));
        
        pair.setMv(1, 1, 1, Mv.packMv(-42, -43, 11));
        Assert.assertEquals(0, pair.mv0X(1, 1));
        Assert.assertEquals(0, pair.mv0Y(1, 1));
        Assert.assertEquals(-1, pair.mv0R(1, 1));
        Assert.assertEquals(-42, pair.mv1X(1, 1));
        Assert.assertEquals(-43, pair.mv1Y(1, 1));
        Assert.assertEquals(11, pair.mv1R(1, 1));
        
        pair.clear();
        Assert.assertEquals(0, pair.mv0X(1, 1));
        Assert.assertEquals(0, pair.mv0Y(1, 1));
        Assert.assertEquals(-1, pair.mv0R(1, 1));
        Assert.assertEquals(0, pair.mv1X(1, 1));
        Assert.assertEquals(0, pair.mv1Y(1, 1));
        Assert.assertEquals(-1, pair.mv1R(1, 1));
    }
    
    @Test
    public void testUi2Vector() {
        int vect = 0;
        
        for (int i = 0; i < 16; i++) {
            Assert.assertEquals(0, Ui2Vector.get(vect, i));
            vect = Ui2Vector.set(vect, i, i);
            Assert.assertEquals(i & 0x3, Ui2Vector.get(vect, i));
        }
            
    }
}
