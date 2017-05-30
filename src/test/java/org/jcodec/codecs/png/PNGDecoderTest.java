package org.jcodec.codecs.png;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;
import org.junit.Assert;
import org.junit.Test;

public class PNGDecoderTest {
    @Test
    public void testPNG() throws IOException {
        String dir = "src/test/resources/png/img%d.png";
        String raw = "src/test/resources/png/img%d.raw";

        PNGDecoder pngDec = new PNGDecoder();
        for (int i = 0;; i++) {
            File f = new File(String.format(dir, i));
            if (!f.exists())
                break;
            ByteBuffer buf = NIOUtils.fetchFromFile(f);
            VideoCodecMeta codecMeta = pngDec.getCodecMeta(buf);
            Picture8Bit pic = Picture8Bit.create(codecMeta.getSize().getWidth(), codecMeta.getSize().getHeight(),
                    ColorSpace.RGB);
            Picture8Bit dec = pngDec.decodeFrame8Bit(buf, pic.getData());
            ByteBuffer refB = NIOUtils.fetchFromFile(new File(String.format(raw, i)));

            byte[] array = NIOUtils.toArray(refB);
            for(int j = 0; j < array.length; j++) {
                array[j] = (byte)((array[j] & 0xff) - 128);
                
            }
            Assert.assertArrayEquals("" + i, array, dec.getPlaneData(0));
        }
    }

}
