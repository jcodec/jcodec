package org.jcodec.codecs.png;

import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class PNGEncoderTest {
    @Test
    public void testPNG() throws IOException {
        Picture original = AWTUtil.decodePNG0(new File("src/test/resources/png/img5.png"));

        PNGEncoder encoder = new PNGEncoder();
        ByteBuffer encoded = encoder.encodeFrame(original, ByteBuffer.allocate(encoder.estimateBufferSize(original))).getData();

        Picture decoded = decode(encoded);
        Assert.assertArrayEquals(original.getPlaneData(0), decoded.getPlaneData(0));
    }

    private static Picture decode(ByteBuffer encoded) {
        PNGDecoder pngDec = new PNGDecoder();
        VideoCodecMeta codecMeta = pngDec.getCodecMeta(encoded);
        Picture pic = Picture.create(codecMeta.getSize().getWidth(), codecMeta.getSize().getHeight(),
                ColorSpace.RGB);
        return pngDec.decodeFrame(encoded, pic.getData());
    }
}