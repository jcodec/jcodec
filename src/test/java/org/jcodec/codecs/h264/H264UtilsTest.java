package org.jcodec.codecs.h264;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.jcodec.codecs.h264.H264Utils.Mv;
import org.jcodec.codecs.h264.H264Utils.MvList;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Size;
import org.jcodec.common.tools.Debug;
import org.jcodec.platform.Platform;
import org.junit.Assert;
import org.junit.Test;

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
    public void testGotoNALUnit() {
        ByteBuffer buf = ByteBuffer.wrap(new byte[]{
                0x00, 0x00, 0x03, 0x00, 0x01, 0x00, 0x00, 0x00, 0x03, 0x00, 0x00, 0x65,
                0x00, 0x00, 0x01,
                0x00, 0x00, 0x00, 0x01, 0x65,
                0x00, 0x00, 0x01, 0x42, 0x00, 0x00, 0x03, 0x00, 0x49 });
        ByteBuffer bufA = buf.duplicate();

        byte[] res0 = NIOUtils.toArray(H264Utils.gotoNALUnit(buf));
        byte[] res0A = NIOUtils.toArray(H264Utils.gotoNALUnitWithArray(bufA));

        buf.position(buf.position() + 3);
        bufA.position(bufA.position() + 3);
        byte[] res1 = NIOUtils.toArray(H264Utils.gotoNALUnit(buf));
        byte[] res1A = NIOUtils.toArray(H264Utils.gotoNALUnitWithArray(bufA));

        buf.position(buf.position() + 4);
        bufA.position(bufA.position() + 4);
        byte[] res2 = NIOUtils.toArray(H264Utils.gotoNALUnit(buf));
        byte[] res2A = NIOUtils.toArray(H264Utils.gotoNALUnitWithArray(bufA));

        buf.position(buf.position() + 3);
        bufA.position(bufA.position() + 3);
        byte[] res3 = NIOUtils.toArray(H264Utils.gotoNALUnit(buf));
        byte[] res3A = NIOUtils.toArray(H264Utils.gotoNALUnitWithArray(bufA));

        Assert.assertArrayEquals(new byte[]{0x00, 0x00, 0x03, 0x00, 0x01, 0x00, 0x00, 0x00, 0x03, 0x00, 0x00, 0x65}, res0);
        Assert.assertArrayEquals(new byte[]{}, res1);
        Assert.assertArrayEquals(new byte[]{0x65}, res2);
        Assert.assertArrayEquals(new byte[]{0x42, 0x00, 0x00, 0x03, 0x00, 0x49}, res3);

        Assert.assertArrayEquals(res0, res0A);
        Assert.assertArrayEquals(res1, res1A);
        Assert.assertArrayEquals(res2, res2A);
        Assert.assertArrayEquals(res3, res3A);
    }
    
    
    @Test
    public void testCanGetSizeCorrectlyFromValidSps()
    {
    	byte[] data = { (byte)0x4D, (byte)0x40, (byte)0x1F, 
    			        (byte)0x9A, (byte)0x64, (byte)0x02, (byte)0x00, (byte)0x24, (byte)0xFF, (byte)0xFF, (byte)0x80, 
    			        (byte)0x5E, (byte)0x80, (byte)0x5F, (byte)0x37, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x40, 
    			        (byte)0x00, (byte)0x00, (byte)0xFA, (byte)0x00, (byte)0x00, (byte)0x1D, (byte)0x4C, (byte)0x25, 
    			         };

    	Debug.debug = true;
    	
    	final SeqParameterSet sps = H264Utils.readSPS(ByteBuffer.wrap(data));

    	final Size size = H264Utils.getPicSize(sps);
    	Assert.assertEquals(1024, size.getWidth());
    	Assert.assertEquals(576, size.getHeight());
    }
    
    // test with nal stream bytes
    
}
