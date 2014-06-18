package org.jcodec.codecs.vp8;

import java.io.File;
import java.io.IOException;

import org.jcodec.common.IOUtils;
import org.junit.Assert;
import org.junit.Before;

public class VP8DecoderTest {

    private byte[] bb;
    private VP8Decoder dec;

//    public void testKF() throws Exception {
//        dec.decode(ByteBuffer.wrap(bb));
//        
//        ImageIO.write(dec.getBufferedImage(), "png", MKVMuxerTest.tildeExpand("~/decoded.png"));
//    }
    
//    public void testKFToPicture() throws Exception {
//        dec.decode(ByteBuffer.wrap(bb));
//        Picture p = dec.getPicture();
//        ImageIO.write(JCodecUtil.toBufferedImage(p), "png", MKVMuxerTest.tildeExpand("~/decoded.pic.png"));
//    }
    
    public void pysch() throws Exception {
        int mbWidth = 4;
        int mbHeight = 2;
        int stride = mbWidth*16;
        
        int size = mbWidth*mbHeight*16*16;
        for (int mbRow = 0; mbRow < mbWidth; mbRow++)
            for (int mbCol = 0; mbCol < mbHeight; mbCol++) 

                for (int lumaRow=0;lumaRow<4;lumaRow++)
                    for(int lumaCol=0;lumaCol<4;lumaCol++)
                        for (int lumaPRow=0;lumaPRow<4;lumaPRow++)
                            for (int lumaPCol=0;lumaPCol<4;lumaPCol++){
                                int lIdx = 16*stride*mbRow + 4*stride*lumaRow + stride*lumaPRow + 16*mbCol + 4*lumaCol + lumaPCol;
                                System.out.println("mbRow: "+mbRow+" mbCol: "+mbCol+" lumaRow: "+lumaRow+" lumaCol: "+lumaCol+" lumaPRow: "+lumaPRow+" lumaPCol: "+lumaPCol+" = "+lIdx);
                                Assert.assertTrue("Must me smaller then "+size+" but was "+lIdx, (lIdx < size));
                            }
        System.out.println(size);
    }

    @Before
    public void setUp() throws IOException {
        String path = "src/test/resources/fr.vp8";
        bb = IOUtils.readFileToByteArray(new File(path));
        System.out.println("byte array length: " + bb.length);
        dec = new VP8Decoder();
    }

}
